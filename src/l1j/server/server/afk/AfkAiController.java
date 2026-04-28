package l1j.server.server.afk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import l1j.server.server.model.L1Location;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1MonsterInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.serverpackets.S_MoveCharPacket;
import l1j.server.server.utils.MoveUtil;

/**
 * AfkAiController v8-fix
 * - Keeps v6 anti-oscillation & wall-follow, v7 corner-pop & wake, v4 recenter
 * - Adds v8 chase watchdog (drop target if no progress in 3s)
 */
public class AfkAiController {

// --- HP stall detection (airwall fix) ---
private static final Map<Integer, Integer> _hpMemo = new HashMap<>();
private static final Map<Integer, Long> _hpMemoMs = new HashMap<>();
private static final int HP_STALL_MS = 1500;

// 8-direction deltas (avoid MoveUtil dependency)
private static final int[] DX = {0, 1, 1, 1, 0, -1, -1, -1};
private static final int[] DY = {-1, -1, 0, 1, 1, 1, 0, -1};

    private static final Logger _log = Logger.getLogger(AfkAiController.class.getName());
    private static final boolean DEBUG = false;
    private static void dbg(L1PcInstance pc, String s) { if (!DEBUG || pc==null) return; try { pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("[AFK] "+s)); } catch (Throwable ignore) {} }


    private final L1PcInstance _pc;
    public AfkAiController(L1PcInstance pc) { this._pc = pc; }

    // --- lifecycle hooks (kept for compatibility) ---
    public void tick() {}
    public void toggle() { try { AfkService.toggleRunning(_pc); } catch (Throwable ignore) {} }
    public void onDeath() { try { AfkController.forceOff(_pc); } catch (Throwable ignore) {} }
    public void onDisconnect() { try { AfkService.onDisconnect(_pc); } catch (Throwable ignore) {} }
    public void onReconnect() {}
    public void onTownWarp() { try { AfkController.forceOff(_pc); } catch (Throwable ignore) {} }
    public void onSuccessfulHit() { try { AfkCruiseEngine.markCombat(_pc); } catch (Throwable ignore) {} try { Integer tid = _chaseTargetId.get(_pc.getId()); if (tid != null) { l1j.server.server.afk.AfkWallIgnoreList.clearIgnore(_pc, tid.intValue()); } } catch (Throwable ignore) {} }

    // --- state ---
	// 牆後阻擋累積 tick 計數（pcId -> 次數）
	private static final java.util.Map<Integer, Integer> _blockedTicks = new java.util.HashMap<Integer, Integer>();
	// 追擊重複路徑計數：pcId -> (tileHash -> 次數)
	private static final java.util.Map<Integer, java.util.concurrent.ConcurrentHashMap<Integer, Integer>> _tileRepeatCounts = new java.util.HashMap<Integer, java.util.concurrent.ConcurrentHashMap<Integer, Integer>>();
    private static final Map<Integer, Long> _lastStepMs = new HashMap<Integer, Long>();
    private static final Map<Integer, Long> _lastAttackMs = new HashMap<Integer, Long>();
    private static final Map<Integer, Integer> _lastHeading = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> _bounceDir = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> _lastPosPacked = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> _histA = new HashMap<Integer, Integer>(); // 最新
    private static final Map<Integer, Integer> _histB = new HashMap<Integer, Integer>(); // 次新
    private static final Map<Integer, Integer> _histC = new HashMap<Integer, Integer>(); // 第三新
    private static final Map<Integer, Integer> _lastXY = new HashMap<Integer, Integer>();
    private static final Map<Integer, Long> _lastXYMs = new HashMap<Integer, Long>();

    // 追怪狀態
    private static final Map<Integer, Integer> _chaseTargetId = new HashMap<Integer, Integer>();
    private static final Map<Integer, Integer> _chaseLastDist = new HashMap<Integer, Integer>();
    private static final Map<Integer, Long> _chaseLastImprovedMs = new HashMap<Integer, Long>();
    private static final Map<Integer, Integer> _pathRetryCounts = new HashMap<Integer, Integer>();

    // 貼牆模式
    private static final Map<Integer, Long> _wallUntil = new HashMap<Integer, Long>();
    private static final Map<Integer, Integer> _wallSide = new HashMap<Integer, Integer>(); // +1=順時針/-1=逆時針

    // --- constants ---
    private static final int STEP_INTERVAL_MS = 500; // legacy fallback (實際節拍改用 AfkSpeedUtil 依 gfx/狀態動態計算)
    private static final int STALL_MS = 3000;
    private static final int SCAN_RANGE = 12;
    private static final int WALL_MS = 2500;
    private static final int CORNER_POP_TRIES = 2;
    private static final int CHASE_STALL_MS = 3000;
    private static final int CHASE_RESET_COOLDOWN_MS = 800;

    /** 外部喚醒：模式切換/重新開啟時呼叫，重置節拍與避讓狀態 */
    public static void nudgeWake(final L1PcInstance pc) {
        if (pc == null) return;
        final int id = pc.getId();
        _lastStepMs.put(id, 0L);
        _lastAttackMs.put(id, 0L);
        _histA.remove(id); _histB.remove(id); _histC.remove(id);
        _wallUntil.remove(id); _wallSide.remove(id);
        _lastPosPacked.remove(id);
        _chaseTargetId.remove(id); _chaseLastDist.remove(id); _chaseLastImprovedMs.remove(id);
        _pathRetryCounts.remove(id);
        AfkPathService.clear(pc);
    }

    // --- main tick entry ---
    public static void tick(L1PcInstance pc, int originX, int originY, int originMap) {
        tick(pc, originX, originY, originMap, -1);
    }

    public static void tick(L1PcInstance pc, int originX, int originY, int originMap, int patrolRadiusArg) {
        if (isOffline(pc)) {
            try { AfkService.onDisconnect(pc); } catch (Throwable ignore) {}
            try { AfkCruiseEngine.stop(pc); } catch (Throwable ignore) {}
            try { AfkMagicRegistry.setEnabled(pc, false); } catch (Throwable ignore) {}
            return;
        }

        if (pc == null) return;
        if (!AfkService.isRunning(pc)) return;

        // 跟隨協同系統：若已進入跟隨協同模式，交由 CoopFollowService 處理本 tick
        try {
            if (l1j.server.server.afk.CoopFollowService.tickCoop(pc)) {
                return;
            }
        } catch (Throwable ignore) {}
        try {
            final int pcId = pc.getId();
            final long now = System.currentTimeMillis();

            // 置中：若觸發則暫停本 tick
            if (AfkCenterEngine.process(pc)) return;

            // Watchdog：3 秒無移動 → 解鎖步進
            int packNow = pack(pc.getX(), pc.getY());
            Integer lastPack = _lastXY.get(pcId);
            if (lastPack == null || lastPack.intValue() != packNow) {
                _lastXY.put(pcId, packNow);
                _lastXYMs.put(pcId, now);
            } else {
                Long t0 = _lastXYMs.get(pcId);
                if (t0 != null && now - t0.longValue() > STALL_MS) {
                    _lastStepMs.put(pcId, 0L);
                    _lastXYMs.put(pcId, now);
                }
            }

            final int __moveIntervalMs = AfkSpeedUtil.getMoveIntervalMs(pc);
            final boolean canStep = (_lastStepMs.get(pcId) == null) || (now - _lastStepMs.get(pcId) >= __moveIntervalMs);

            // 搜怪
            boolean ranged = false;
            try {
                l1j.server.server.model.Instance.L1ItemInstance w = pc.getWeapon();
                if (w != null && w.getItem() instanceof l1j.server.server.templates.L1Weapon) {
                    int wt = ((l1j.server.server.templates.L1Weapon) w.getItem()).getType();
                    ranged = (wt == 4 || wt == 13|| wt ==62);
                }
            } catch (Throwable ignore) {}
            final int rangeCap = (AfkConfig.RANGED_MAX_RANGE > 0 ? AfkConfig.RANGED_MAX_RANGE : 8);
final int attackRange = ranged ? rangeCap : 1;

            List<L1Object> objs = L1World.getInstance().getVisibleObjects(pc, SCAN_RANGE);
            L1MonsterInstance nearest = null;
            int best = Integer.MAX_VALUE;
            for (L1Object o : objs) {
                if (o instanceof L1MonsterInstance) {
                    L1MonsterInstance m = (L1MonsterInstance)o;
                    if (m.isDead() || m.getCurrentHp() <= 0) continue;
                    int d = pc.getLocation().getTileLineDistance(m.getLocation());
                    if (d < best) { best = d; nearest = m; }
                }
            }

            
            // 目標選擇：維持既有目標直到死亡，否則最近者
            L1MonsterInstance target = nearest;
            // retaliation: prefer aggressor that is targeting me / has hate on me
            try {
                if (target == null) {
                    L1MonsterInstance bestAgg = null; int bestD = 9999;
                    for (L1Object __o : objs) {
                        if (!(__o instanceof L1MonsterInstance)) continue;
                        L1MonsterInstance m = (L1MonsterInstance)__o;
                        boolean aggro = false;
                        if (!aggro) {
                            try {
                                Object hl = m.getHateList();
                                if (hl != null) {
                                    try { java.lang.reflect.Method gm = hl.getClass().getMethod("get", Object.class); Object v = gm.invoke(hl, pc); if (v instanceof Integer && ((Integer)v).intValue() > 0) aggro = true; } catch (Throwable __2) {}
                                }
                            } catch (Throwable __1) {}
                        }
                        if (aggro) {
                            int d = pc.getLocation().getTileLineDistance(m.getLocation());
                            if (d < bestD) { bestD = d; bestAgg = m; }
                        }
                    }
                    if (bestAgg != null) target = bestAgg;
                }
            } catch (Throwable __agg) {}

            try {
                Integer keepId = _chaseTargetId.get(pcId);
                if (keepId != null) {
                    L1Object keepObj = L1World.getInstance().findObject(keepId);
                    if (keepObj instanceof L1MonsterInstance) {
                        L1MonsterInstance km = (L1MonsterInstance) keepObj;
                        if (!km.isDead() && km.getCurrentHp() > 0 && km.getMapId() == pc.getMapId() &&
                            pc.getLocation().getTileLineDistance(km.getLocation()) <= SCAN_RANGE) {
                            target = km;
                        }
                    }
                }
            } catch (Throwable ignore) {}

            if (target != null) {
                try { CoopFollowService.onLeaderAttack(pc, target); } catch (Throwable ignore) {}
                int dist = pc.getLocation().getTileLineDistance(target.getLocation());
                boolean hasLos = true;
                try { hasLos = l1j.server.server.afk.LosUtil.hasLineOfSight(pc.getMap(), pc.getX(), pc.getY(), target.getX(), target.getY()); } catch (Throwable ignore) {}
                boolean wallOk = true;
                try { wallOk = l1j.server.server.afk.AfkWallJudge.shouldAttack(pc, target); } catch (Throwable ignore) {}
                // 牆後/阻擋累積：若連續多次被擋，清除鎖定並暫忽略
                {
                    Integer __btObj = _blockedTicks.get(pcId);
                    int __bt = __btObj == null ? 0 : __btObj.intValue();
                    if (!wallOk) { __bt++; } else { __bt = 0; }
                    _blockedTicks.put(pcId, Integer.valueOf(__bt));
                    if (__bt >= 4 && !AfkConfig.USE_PATHFINDING) { // 無尋路時才直接忽略
                        try { l1j.server.server.afk.AfkWallIgnoreList.markIgnore(pc, target.getId(), System.currentTimeMillis()); } catch (Throwable ignore) {}
                        _chaseTargetId.remove(pcId);
                        _tileRepeatCounts.remove(pcId);
                        dbg(pc, "阻擋超時 -> 釋放鎖定與暫忽略");
                        _lastStepMs.put(pcId, 0L); // 開閘，允許立刻巡航
                        cruiseStep(pc);
                        return;
                    }
                    // 若非常接近（<=4格）且仍被牆阻擋，立即釋放鎖定並暫忽略
                    if (!wallOk && dist <= 4 && !AfkConfig.USE_PATHFINDING) {
                        try { l1j.server.server.afk.AfkWallIgnoreList.markIgnore(pc, target.getId(), System.currentTimeMillis()); } catch (Throwable ignore) {}
                        _chaseTargetId.remove(pcId);
                        _tileRepeatCounts.remove(pcId);
                        dbg(pc, "近距離阻擋 -> 立即釋放鎖定與暫忽略");
                        _lastStepMs.put(pcId, 0L); // 開閘，允許立刻巡航
                        cruiseStep(pc);
                        return;
                    }
                }


                // 更新鎖定
                Integer cur = _chaseTargetId.get(pcId);
                if (cur == null || cur.intValue() != target.getId()) {
                    _chaseTargetId.put(pcId, target.getId()); _tileRepeatCounts.remove(pcId);
                    _chaseLastDist.put(pcId, dist);
                    _chaseLastImprovedMs.put(pcId, now);
                    _pathRetryCounts.remove(pcId);
                    AfkPathService.clear(pc);
                    dbg(pc, "鎖定目標 id="+target.getId()+" dist="+dist+" LoS="+hasLos);
                }

                // 武器射程（區別名稱避免重複宣告）
                int atkRange = 1;
                try {
                    l1j.server.server.model.Instance.L1ItemInstance w = pc.getWeapon();
                    if (w != null && w.getItem() instanceof l1j.server.server.templates.L1Weapon) {
                        int wt = ((l1j.server.server.templates.L1Weapon) w.getItem()).getType();
                        atkRange = (wt == 4 || wt == 13 || wt == 62) ? 10 : 1;
                    }
                } catch (Throwable ignore) {}

                // 1) 優先嘗試施法（有視線且≤10格）
                boolean magicPossible = false;
                try {
                    java.util.LinkedHashSet<Integer> __regs = l1j.server.server.afk.AfkMagicRegistry.getRegistered(pc);
                    magicPossible = __regs != null && !__regs.isEmpty() && l1j.server.server.afk.AfkMagicRegistry.isEnabled(pc);
                } catch (Throwable __e) { magicPossible = false; }
                if (!magicPossible) { dbg(pc, "no registered magic -> use physical first"); }
                boolean acted = false;
                // HIDDEN/Fly guard: if target is hidden (sink/fly), drop lock and patrol
                try {
                    int __hs = ((l1j.server.server.model.Instance.L1NpcInstance) target).getHiddenStatus();
                    if (__hs == l1j.server.server.model.Instance.L1NpcInstance.HIDDEN_STATUS_SINK ||
                        __hs == l1j.server.server.model.Instance.L1NpcInstance.HIDDEN_STATUS_FLY) {
                        dbg(pc, "目標隱藏/飛天 -> 解除鎖定並巡邏");
                        _chaseTargetId.remove(pcId);
                        _chaseLastDist.remove(pcId);
                        _chaseLastImprovedMs.remove(pcId);
                        cruiseStep(pc);
                        return;
                    }
                } catch (Throwable __ignore) {}

                final int __rangeCap = resolveMagicRangeCap(pc, rangeCap);
                final Integer __sid = __selectedSkill(pc);
                final boolean __triple = (__sid != null && __sid.intValue() == 132);
                final boolean canCastNow = magicPossible && hasLos && wallOk && (
                    __triple ? (ranged && dist <= rangeCap)
                             : (dist <= __rangeCap)
                );
                if (canCastNow) {
// --- HP stall guard: if target HP hasn't changed while in LOS/range, try a small sidestep to fix airwall ---
try {
    int hpNow = target.getCurrentHp();
    Integer hpPrev = _hpMemo.get(pcId);
    Long hpTs = _hpMemoMs.get(pcId);
    long nowMs = System.currentTimeMillis();
    if (hpPrev != null && hpTs != null && hpPrev.intValue() == hpNow && (nowMs - hpTs.longValue()) >= HP_STALL_MS) {
        if (sideStep(pc, target.getX(), target.getY())) {
            _hpMemoMs.put(pcId, nowMs); // cooldown few ms after step
            _lastStepMs.put(pcId, nowMs);
            return; // moved this tick; attack next tick
        }
    }
    _hpMemo.put(pcId, hpNow);
    _hpMemoMs.put(pcId, nowMs);
} catch (Throwable ignore) {}

                    if (l1j.server.server.afk.AfkMagicEngine.tryCastOne(pc, target)) {
                        AfkCruiseEngine.markCombat(pc);
                        dbg(pc, "施法攻擊 dist="+dist);
                        acted = true; _tileRepeatCounts.remove(pcId);
                    }
                }

                // 2) 物理攻擊（有視線且在射程內；遠程不保持距離）
                if (!acted && hasLos && ((ranged && dist <= rangeCap) || (!ranged && wallOk && dist <= atkRange))) {
                    // 依照當前形象(gfx) + 狀態(加速/勇水/精餅...) 的攻擊間隔節流
                    final long __nowAtk = System.currentTimeMillis();
                    final int __atkIntervalMs = AfkSpeedUtil.getAttackIntervalMs(pc);
                    final Long __lastAtk = _lastAttackMs.get(pcId);
                    if (__lastAtk != null && (__nowAtk - __lastAtk.longValue()) < __atkIntervalMs) {
                        // 攻擊尚未冷卻：不要亂走造成抖動，也不要重複送攻擊封包
                        acted = true;
                    } else {
                        try {
                            l1j.server.server.afk.AfkCombatUtil.physicalAttack(pc, target);
                            _lastAttackMs.put(pcId, __nowAtk);
                            AfkCruiseEngine.markCombat(pc);
                            dbg(pc, "物理攻擊 dist=" + dist + " iv=" + __atkIntervalMs);
                        } catch (Throwable ignore) {}
                        acted = true; _tileRepeatCounts.remove(pcId);
                    }
                }

                // 3) 不在射程或無視線 → 一定嘗試靠近（放寬步法），不受節拍限制（開閘）
                if (!acted) {
                    // 追擊路徑重複計數（僅在未出手且不在有效攻擊狀態時啟動）
                    try {
                        boolean inRange = hasLos && wallOk && (magicPossible ? (__triple ? (ranged && dist <= rangeCap) : (dist <= __rangeCap)) : ((ranged && dist <= rangeCap) || (!ranged && dist <= atkRange)));
                        if (!inRange) {
                            java.util.concurrent.ConcurrentHashMap<Integer, Integer> tm = _tileRepeatCounts.get(pcId);
                            if (tm == null) { tm = new java.util.concurrent.ConcurrentHashMap<Integer, Integer>(); _tileRepeatCounts.put(pcId, tm); }
                            int th = (pc.getX() << 16) ^ pc.getY();
                            int c = tm.getOrDefault(th, 0) + 1;
                            tm.put(th, c);
                            if (c > 3) {
                                _tileRepeatCounts.remove(pcId);
                                AfkPathService.clear(pc);
                                int retry = nextPathRetry(pcId);
                                dbg(pc, "重複路徑>3次 -> 重算短路徑(" + retry + ")");
                                if (retry > Math.max(1, AfkConfig.PF_RETRY_LIMIT)) {
                                    l1j.server.server.afk.AfkWallIgnoreList.markIgnore(pc, target.getId(), System.currentTimeMillis());
                                    _chaseTargetId.remove(pcId);
                                    _pathRetryCounts.remove(pcId);
                                    dbg(pc, "短路徑重試超限 -> 暫忽略並釋放鎖定");
                                    _lastStepMs.put(pcId, 0L);
                                    cruiseStep(pc);
                                    return;
                                }
                            }
                        } else {
                            _tileRepeatCounts.remove(pcId);
                        }
                    } catch (Throwable ignore) {}

                    _lastStepMs.put(pcId, 0L); // 開閘
                    int h = headingToward(pc.getX(), pc.getY(), target.getX(), target.getY());
                    _lastHeading.put(pcId, h);
                    boolean moved = stepTowardChaseRelaxed(pc, target.getX(), target.getY());
                    if (!moved) moved = popCorner(pc, h);
                    if (moved) {
                        _lastStepMs.put(pcId, now);
                        try {
                            int newDist = pc.getLocation().getTileLineDistance(target.getLocation());
                            Integer lastD = _chaseLastDist.get(pcId);
                            if (lastD == null || newDist < lastD.intValue()) {
                                _chaseLastDist.put(pcId, newDist);
                                _chaseLastImprovedMs.put(pcId, now);
                            }
                        } catch (Throwable ignore) {}
                        dbg(pc, "靠近一步");
                        _pathRetryCounts.remove(pcId);
                        acted = true; _tileRepeatCounts.remove(pcId);
                    } else {
                        int retry = nextPathRetry(pcId);
                        if (retry > Math.max(1, AfkConfig.PF_RETRY_LIMIT)) {
                            try { l1j.server.server.afk.AfkWallIgnoreList.markIgnore(pc, target.getId(), System.currentTimeMillis()); } catch (Throwable ignore) {}
                            _chaseTargetId.remove(pcId);
                            _pathRetryCounts.remove(pcId);
                            _tileRepeatCounts.remove(pcId);
                            dbg(pc, "短路徑失敗超限 -> 暫忽略並巡航");
                            cruiseStep(pc);
                            return;
                        }
                        dbg(pc, "靠近失敗 -> 巡航保底");
                    }
                }

                // 4) 若此 tick 仍無動作，強制巡航一步保底
                if (!acted) {
                    cruiseStep(pc);
                    return;
                }

                // 5) 若目標死亡則解除鎖定
                if (target.isDead() || target.getCurrentHp() <= 0) {
                    dbg(pc, "目標死亡，解除鎖定");
                    _chaseTargetId.remove(pcId);
                    _chaseLastDist.remove(pcId);
                    _chaseLastImprovedMs.remove(pcId);
                    _pathRetryCounts.remove(pcId);
                    AfkPathService.clear(pc);
                }

                return; // 本 tick 追怪已行動
            }

            // 巡航

            if (canStep) cruiseStep(pc);

            // 半徑外瞬回（允許跨界，跨出才瞬回）
            int radius = AfkService.getPatrolRadius(pc);
            if (radius > 0) {
                int dx = Math.abs(pc.getX() - originX);
                int dy = Math.abs(pc.getY() - originY);
                if (Math.max(dx, dy) > radius || pc.getMapId() != originMap) {
                    l1j.server.server.model.L1Teleport.teleport(pc, originX, originY, (short)originMap, pc.getHeading(), true);
                    _lastStepMs.put(pcId, 0L);
                    AfkCenterEngine.resetRef(pc);
                    return;
                }
            }
        } catch (Throwable t) {
            // swallow
        }
    }

    // --- helpers ---
    private static boolean isOffline(final L1PcInstance pc) {
        try {
            if (pc == null) return true;
            if (pc.getNetConnection() == null) return true;
            try {
                final Object nc = pc.getNetConnection();
                if (nc.getClass().getMethod("isClosed") != null) {
                    boolean closed = ((Boolean)nc.getClass().getMethod("isClosed").invoke(nc)).booleanValue();
                    if (closed) return true;
                }
            } catch (Throwable ignore) {}
            try {
                if (pc.getOnlineStatus() != 1) return true;
            } catch (Throwable ignore) {}
            return false;
        } catch (Throwable e) {
            return true;
        }
    }

    private static void setPassableSafe(final L1Map map, final int x, final int y, final boolean passable) {
        try { map.setPassable(x, y, passable); } catch (Throwable ignore) {}
        try { map.setPassable(new L1Location(x, y, map.getId()), passable); } catch (Throwable ignore) {}
    }

    private static boolean isRecent(final int pcId, final int px, final int py) {
        int p = pack(px, py);
        Integer a = _histA.get(pcId), b = _histB.get(pcId), c = _histC.get(pcId);
        if (a != null && a == p) return true;
        if (b != null && b == p) return true;
        if (c != null && c == p) return true;
        return false;
    }

    private static void pushHistory(final int pcId, final int px, final int py) {
        int p = pack(px, py);
        Integer a = _histA.get(pcId);
        Integer b = _histB.get(pcId);
        _histC.put(pcId, (b == null ? -1 : b.intValue()));
        _histB.put(pcId, (a == null ? -1 : a.intValue()));
        _histA.put(pcId, p);
    }

    private static void enableWallFollow(final int pcId, final L1Map map, final int x, final int y, final int heading) {
        boolean rightOk = map.isPassable(MoveUtil.MoveLocX(x, (heading+2)&7), MoveUtil.MoveLocY(y, (heading+2)&7));
        boolean leftOk  = map.isPassable(MoveUtil.MoveLocX(x, (heading+6)&7), MoveUtil.MoveLocY(y, (heading+6)&7));
        int side = rightOk && !leftOk ? +1 : (!rightOk && leftOk ? -1 : ((heading & 1) == 0 ? +1 : -1));
        _wallSide.put(pcId, side);
        _wallUntil.put(pcId, System.currentTimeMillis() + WALL_MS);
    }

    private static boolean inWallFollow(final int pcId) {
        Long until = _wallUntil.get(pcId);
        return until != null && System.currentTimeMillis() < until.longValue();
    }

    /** 角落脫困：試「側一步→前一步」，忽略近期踩踏限制 */
    private static boolean popCorner(final L1PcInstance pc, final int heading) {
        final L1Map map = pc.getMap();
        final int x = pc.getX(), y = pc.getY();
        int[] sides = new int[] { +2, -2 };
        for (int i = 0; i < CORNER_POP_TRIES && i < sides.length; i++) {
            int side = sides[i];
            int hSide = (heading + (side > 0 ? 2 : 6)) & 7;
            int sx = MoveUtil.MoveLocX(x, hSide), sy = MoveUtil.MoveLocY(y, hSide);
            if (!map.isPassable(sx, sy)) continue;
            int hFwd = heading;
            int fx = MoveUtil.MoveLocX(sx, hFwd), fy = MoveUtil.MoveLocY(sy, hFwd);
            // 側一步
            try {
                setPassableSafe(map, x, y, true);
                pc.setHeading(hSide);
                pc.getLocation().set(sx, sy);
                pc.sendPackets(new S_MoveCharPacket(pc));
                pc.broadcastPacket(new S_MoveCharPacket(pc));
                setPassableSafe(map, sx, sy, false);
            } catch (Throwable ignore) {}
            // 若可，前一步
            if (map.isPassable(fx, fy)) {
                try {
                    setPassableSafe(map, sx, sy, true);
                    pc.setHeading(hFwd);
                    pc.getLocation().set(fx, fy);
                    pc.sendPackets(new S_MoveCharPacket(pc));
                    pc.broadcastPacket(new S_MoveCharPacket(pc));
                    setPassableSafe(map, fx, fy, false);
                } catch (Throwable ignore) {}
            }
            return true;
        }
        return false;
    }

    private static void cruiseStep(final L1PcInstance pc) {
        final int pcId = pc.getId();
        final long now = System.currentTimeMillis();
        Long last = _lastStepMs.get(pcId);
        final int __moveIntervalMs = AfkSpeedUtil.getMoveIntervalMs(pc);
        if (last != null && now - last < __moveIntervalMs) return;

        final L1Map map = pc.getMap();
        final int x = pc.getX(), y = pc.getY();

        int heading = _lastHeading.containsKey(pcId) ? _lastHeading.get(pcId) : pc.getHeading();
        int reverse = (heading + 4) & 7;
        int dir = _bounceDir.containsKey(pcId) ? _bounceDir.get(pcId) : 1;
        if (dir == 0) dir = 1;

        int chosen = -1;
        int nx = x, ny = y;
        int lastPack = _lastPosPacked.containsKey(pcId) ? _lastPosPacked.get(pcId) : -1;

        // 1) 貼牆模式
        if (inWallFollow(pcId)) {
            int side = _wallSide.get(pcId) == null ? 1 : _wallSide.get(pcId);
            int slide = (heading + (side > 0 ? 2 : 6)) & 7; // 90°
            int[] order = new int[] { slide, (slide + side) & 7, (slide - side) & 7, heading, (heading + side) & 7, (heading - side) & 7, reverse };
            for (int h : order) {
                int cx = MoveUtil.MoveLocX(x, h), cy = MoveUtil.MoveLocY(y, h);
                if (!map.isPassable(cx, cy)) continue;
                int p = pack(cx, cy);
                if (p == lastPack || isRecent(pcId, cx, cy)) continue;
                chosen = h; nx = cx; ny = cy; break;
            }
        }

        // 2) 一般模式
        if (chosen == -1) {
            int[] order = new int[] { heading, (heading + 1*dir) & 7, (heading + 2*dir) & 7, (heading - 1*dir) & 7, (heading - 2*dir) & 7, (heading + 3*dir) & 7 };
            for (int h : order) {
                if (h == reverse) continue;
                int cx = MoveUtil.MoveLocX(x, h), cy = MoveUtil.MoveLocY(y, h);
                if (!map.isPassable(cx, cy)) continue;
                int p = pack(cx, cy);
                if (p == lastPack || isRecent(pcId, cx, cy)) continue;
                chosen = h; nx = cx; ny = cy; break;
            }
        }

        // 3) 退一步：允許回頭，但仍避開最近 3 格
        if (chosen == -1) {
            int[] order = new int[] { (heading + 1) & 7, (heading + 7) & 7, (heading + 2) & 7, (heading + 6) & 7, (heading + 3) & 7, (heading + 5) & 7, reverse };
            for (int h : order) {
                int cx = MoveUtil.MoveLocX(x, h), cy = MoveUtil.MoveLocY(y, h);
                if (!map.isPassable(cx, cy)) continue;
                int p = pack(cx, cy);
                if (p == lastPack || isRecent(pcId, cx, cy)) continue;
                chosen = h; nx = cx; ny = cy; break;
            }
        }

        // 4) 仍不行 → 嘗試角落脫困
        if (chosen == -1) {
            if (popCorner(pc, heading)) {
                _lastStepMs.put(pcId, now);
                pushHistory(pcId, pc.getX(), pc.getY());
                enableWallFollow(pcId, map, pc.getX(), pc.getY(), heading);
                return;
            }
        }

        // 5) 保底任選可走格，並重置歷史
        if (chosen == -1) {
            for (int h = 0; h < 8; h++) {
                int cx = MoveUtil.MoveLocX(x, h), cy = MoveUtil.MoveLocY(y, h);
                if (!map.isPassable(cx, cy)) continue;
                chosen = h; nx = cx; ny = cy; break;
            }
            _histA.remove(pcId); _histB.remove(pcId); _histC.remove(pcId);
        }

        // ABAB 震盪檢測
        Integer histB = _histB.get(pcId);
        if (histB != null && histB.intValue() == pack(nx, ny)) {
            enableWallFollow(pcId, map, x, y, heading);
        }

        // 移動（釋放舊格→佔用新格）
        try {
            setPassableSafe(map, x, y, true);
            pc.setHeading(chosen == -1 ? heading : chosen);
            pc.getLocation().set(nx, ny);
            pc.sendPackets(new S_MoveCharPacket(pc));
            pc.broadcastPacket(new S_MoveCharPacket(pc));
            setPassableSafe(map, nx, ny, false);
        } catch (Throwable ignore) {}

        if (chosen != -1) {
            if (chosen == reverse) {
                _bounceDir.put(pcId, -dir);
                enableWallFollow(pcId, map, nx, ny, chosen);
            }
            _lastHeading.put(pcId, chosen);
        }

        try { _lastPosPacked.put(pcId, pack(x, y)); } catch (Throwable ignore) {}
        pushHistory(pcId, nx, ny);
        _lastStepMs.put(pcId, now);
    }

    /** 追怪步進：回傳是否成功移動 */
    private static boolean stepTowardSmart(final L1PcInstance pc, final int tx, final int ty) {
        final L1Map map = pc.getMap();
        final int x = pc.getX(), y = pc.getY();
        final int baseHeading = headingToward(x, y, tx, ty);

        int[] order = new int[] { baseHeading, (baseHeading+1)&7, (baseHeading+7)&7, (baseHeading+2)&7, (baseHeading+6)&7, (baseHeading+3)&7, (baseHeading+5)&7, (baseHeading+4)&7 };
        int chosen = -1, nx = x, ny = y, best = Math.max(Math.abs(x - tx), Math.abs(y - ty));

        for (int h : order) {
            int cx = MoveUtil.MoveLocX(x, h);
            int cy = MoveUtil.MoveLocY(y, h);
            if (!map.isPassable(cx, cy)) continue;
            if (isRecent(pc.getId(), cx, cy)) continue;
            int d = Math.max(Math.abs(cx - tx), Math.abs(cy - ty));
            if (d <= best) { best = d; chosen = h; nx = cx; ny = cy; }
            if (chosen != -1 && d == best) break;
        }
        if (chosen == -1) return false;

        try {
            setPassableSafe(map, x, y, true);
            pc.setHeading(chosen);
            pc.getLocation().set(nx, ny);
            pc.sendPackets(new S_MoveCharPacket(pc));
            pc.broadcastPacket(new S_MoveCharPacket(pc));
            setPassableSafe(map, nx, ny, false);
        } catch (Throwable ignore) {}

        pushHistory(pc.getId(), nx, ny);
        return true;
    }

    
    /** 追怪用步法（放寬版）*/
    private static boolean stepTowardChaseRelaxed(final L1PcInstance pc, final int tx, final int ty) {
        final L1Map map = pc.getMap();
        final int x = pc.getX(), y = pc.getY();
        final int base = headingToward(x, y, tx, ty);
        final int d0 = Math.max(Math.abs(x - tx), Math.abs(y - ty));
        int chosen = -1, nx = x, ny = y, best = d0;
        int[] order = new int[] { base, (base+1)&7, (base+7)&7, (base+2)&7, (base+6)&7, (base+3)&7, (base+5)&7, (base+4)&7 };
        for (int h : order) {
            int cx = l1j.server.server.utils.MoveUtil.MoveLocX(x, h);
            int cy = l1j.server.server.utils.MoveUtil.MoveLocY(y, h);
            if (!map.isPassable(cx, cy)) continue;
            int d = Math.max(Math.abs(cx - tx), Math.abs(cy - ty));
            if (d <= best) { best = d; chosen = h; nx = cx; ny = cy; }
        }
        if (chosen == -1) {
            AfkPathService.stepToward(pc, tx, ty);
            return pc.getX() != x || pc.getY() != y;
        }
        try {
            setPassableSafe(map, x, y, true);
            pc.setHeading(chosen);
            pc.getLocation().set(nx, ny);
            pc.sendPackets(new l1j.server.server.serverpackets.S_MoveCharPacket(pc));
            pc.broadcastPacket(new l1j.server.server.serverpackets.S_MoveCharPacket(pc));
            setPassableSafe(map, nx, ny, false);
        } catch (Throwable ignore) {}
        return true;
    }

    private static int headingToward(final int x, final int y, final int tx, final int ty) {
        final int dx = Integer.signum(tx - x);
        final int dy = Integer.signum(ty - y);
        if (dx==0 && dy<0) return 0;
        if (dx>0 && dy<0) return 1;
        if (dx>0 && dy==0) return 2;
        if (dx>0 && dy>0) return 3;
        if (dx==0 && dy>0) return 4;
        if (dx<0 && dy>0) return 5;
        if (dx<0 && dy==0) return 6;
        if (dx<0 && dy<0) return 7;
        return 0;
    }

    private static int pack(int x, int y) { return (x << 16) ^ (y & 0xFFFF); }

    // 瞬回後清除追擊目標，避免立即再度追出界
    public static void clearChaseTarget(final l1j.server.server.model.Instance.L1PcInstance pc) {
        if (pc == null) return;
        final int id = pc.getId();
        try { _chaseTargetId.remove(id); } catch (Throwable ignore) {}
        try { _chaseLastDist.remove(id); } catch (Throwable ignore) {}
        try { _chaseLastImprovedMs.remove(id); } catch (Throwable ignore) {}
        try { _pathRetryCounts.remove(id); } catch (Throwable ignore) {}
        try { AfkPathService.clear(pc); } catch (Throwable ignore) {}
        try { _lastXYMs.remove(id); } catch (Throwable ignore) {}
        try { dbg(pc, "清除追擊目標(瞬回)"); } catch (Throwable ignore) {}
    }



// Try a one-tile sidestep (left/right) to correct projectile line without pathing loops
private static boolean sideStep(final L1PcInstance pc, final int tx, final int ty) {
    try {
        final int x = pc.getX(), y = pc.getY();
        final int heading = headingToward(x, y, tx, ty);
        final int left = (heading + 7) & 7;
        final int right = (heading + 1) & 7;
        final int[] dirs = new int[] { left, right };
        l1j.server.server.model.map.L1Map map = pc.getMap();
        for (int h : dirs) {
            int nx = l1j.server.server.utils.MoveUtil.MoveLocX(x, h);
            int ny = l1j.server.server.utils.MoveUtil.MoveLocY(y, h);
            if (!map.isPassable(nx, ny)) continue;
            pc.setHeading(h);
            pc.getLocation().set(nx, ny);
            l1j.server.server.serverpackets.S_MoveCharPacket pkt = new l1j.server.server.serverpackets.S_MoveCharPacket(pc);
            pc.sendPackets(pkt);
            pc.broadcastPacket(pkt);
            return true;
        }
    } catch (Throwable ignore) {}
    return false;
}


private static boolean arrowOk(final l1j.server.server.model.Instance.L1PcInstance pc,
                               final l1j.server.server.model.L1Character target,
                               final boolean ranged) {
    try {
        if (!ranged) return true;
        final l1j.server.server.model.map.L1Map map = pc.getMap();
        final int tx = target.getX(), ty = target.getY();
        int x = pc.getX(), y = pc.getY();
        int guard = 64;
        while (guard-- > 0 && (x != tx || y != ty)) {
            final int h = (headingToward(x, y, tx, ty) & 7);
            try {
                if (!map.isArrowPassable(x, y, h)) return false;
                x = x + DX[h]; y = y + DY[h];
            } catch (Throwable ignore) {
                int nx = x + DX[h], ny = y + DY[h];
                if (!map.isPassable(nx, ny)) return false;
                x = nx; y = ny;
            }
        }
        return true;
    } catch (Throwable ignore) { return true; }
}

private static boolean smartNudge(final l1j.server.server.model.Instance.L1PcInstance pc,
                                  final l1j.server.server.model.L1Character target) {
    try {
        final l1j.server.server.model.map.L1Map map = pc.getMap();
        final int x = pc.getX(), y = pc.getY();
        final int h = (headingToward(x, y, target.getX(), target.getY()) & 7);
        final int left = (h + 7) & 7, right = (h + 1) & 7, lf = (h + 6) & 7, rf = (h + 2) & 7;
        int[] order = new int[]{ left, right, lf, rf };
        for (int dir : order) {
            int nx = x + DX[dir], ny = y + DY[dir];
            if (!map.isPassable(nx, ny)) continue;
            pc.setHeading(dir);
            pc.getLocation().set(nx, ny);
            l1j.server.server.serverpackets.S_MoveCharPacket pkt =
                new l1j.server.server.serverpackets.S_MoveCharPacket(pc);
            pc.sendPackets(pkt);
            pc.broadcastPacket(pkt);
            return true;
        }
    } catch (Throwable ignore) {}
    return false;
}

    private static int nextPathRetry(final int pcId) {
        final Integer cur = _pathRetryCounts.get(pcId);
        final int next = (cur == null ? 1 : cur.intValue() + 1);
        _pathRetryCounts.put(pcId, Integer.valueOf(next));
        return next;
    }

private static boolean arrowProbeEnabled() {
    try {
        java.lang.reflect.Field f = AfkConfig.class.getField("RANGED_ARROW_PROBE");
        Object v = f.get(null);
        if (v instanceof Boolean) return ((Boolean) v).booleanValue();
        if (v instanceof Integer) return ((Integer) v).intValue() != 0;
    } catch (Throwable ignore) {}
    return true;
}



// --- helper: hidden / invisible NPC detection (reflection-safe, no direct calls) ---
private static boolean _callBool(Object obj, String methodName) {
    try {
        java.lang.reflect.Method m = obj.getClass().getMethod(methodName);
        Object v = m.invoke(obj);
        if (v instanceof java.lang.Boolean) return ((java.lang.Boolean)v).booleanValue();
    } catch (Throwable ignore) {}
    return false;
}
private static int _callInt(Object obj, String methodName) {
    try {
        java.lang.reflect.Method m = obj.getClass().getMethod(methodName);
        Object v = m.invoke(obj);
        if (v instanceof java.lang.Integer) return ((java.lang.Integer)v).intValue();
    } catch (Throwable ignore) {}
    return 0;
}
private static boolean isHiddenOrInvisible(final l1j.server.server.model.Instance.L1MonsterInstance m) {
    if (m == null) return true;
    try { if (_callBool(m, "isDead") || _callInt(m, "getCurrentHp") <= 0) return true; } catch (Throwable ignore) {}
    if (_callBool(m, "isInvis") || _callBool(m, "isInvisble")) return true;
    try { if (_callInt(m, "getHiddenStatus") != 0) return true; } catch (Throwable ignore) {}
    return false;
}



// === Helpers: choose single registered skill and resolve its cast range ===
private static Integer __selectedSkill(final l1j.server.server.model.Instance.L1PcInstance pc) {
    try {
        java.util.LinkedHashSet<Integer> s = l1j.server.server.afk.AfkMagicRegistry.getRegistered(pc);
        if (s != null) {
            for (Integer id : s) { if (id != null) return id; }
        }
    } catch (Throwable ignore) {}
    return null;
}
private static int __skillRangeFromCore(final int skillId) {
    try {
        l1j.server.server.templates.L1Skills sk = l1j.server.server.datatables.SkillsTable.getInstance().getTemplate(skillId);
        if (sk != null) {
            int r = 0;
            try { r = sk.getRanged(); } catch (Throwable ignore) {}
            if (r > 0) return r;
        }
    } catch (Throwable ignoreAll) {}
    return 0;
}
private static int resolveMagicRangeCap(final l1j.server.server.model.Instance.L1PcInstance pc, final int fallback) {
    try {
        Integer sid = __selectedSkill(pc);
        if (sid != null) {
            Integer ov = l1j.server.server.afk.AfkConfig.SKILL_RANGE.get(sid);
            int r = (ov != null && ov.intValue() > 0) ? ov.intValue() : __skillRangeFromCore(sid.intValue());
            if (r <= 0) r = 1;
            if (r > 99) r = 99;
            return r;
        }
    } catch (Throwable ignore) {}
    return fallback;
}


// 外部事件：怪物切為隱藏／飛天（由 setHiddenStatus／封包保底／spawn 檢查觸發）
// 策略：
// 1) 若當前追擊目標就是它 → 立刻放生＋巡邏
// 2) 否則若它在我附近（<=10 格）且確定處於隱藏/飛天 → 也放生（避免站樁）
public void onMobHidden(int mobId) {
    try {
        final int pcId = _pc.getId();

        // 1) 追擊表命中 → 放生
        try {
            Integer keepId = _chaseTargetId.get(pcId);
            if (keepId != null && keepId.intValue() == mobId) {
                try { _chaseTargetId.remove(pcId); } catch (Throwable ignore) {}
                try { cruiseStep(_pc); } catch (Throwable ignore) {}
                return;
            }
        } catch (Throwable ignore) {}

        // 2) 附近有剛轉隱藏/飛天的怪 → 放生
        try {
            l1j.server.server.model.L1Object obj =
                l1j.server.server.model.L1World.getInstance().findObject(mobId);
            if (obj instanceof l1j.server.server.model.Instance.L1NpcInstance) {
                l1j.server.server.model.Instance.L1NpcInstance tn =
                    (l1j.server.server.model.Instance.L1NpcInstance) obj;
                int hs = tn.getHiddenStatus();
                if (hs == l1j.server.server.model.Instance.L1NpcInstance.HIDDEN_STATUS_SINK ||
                    hs == l1j.server.server.model.Instance.L1NpcInstance.HIDDEN_STATUS_FLY) {
                    // 只在距離合理時才干預，避免無關玩家被影響
                    int dist = _pc.getLocation().getTileLineDistance(tn.getLocation());
                    if (dist <= 10) {
                        try { _chaseTargetId.remove(pcId); } catch (Throwable ignore) {}
                        try { cruiseStep(_pc); } catch (Throwable ignore) {}
                        return;
                    }
                }
            }
        } catch (Throwable ignore) {}
    } catch (Throwable ignore) {}
}

}
