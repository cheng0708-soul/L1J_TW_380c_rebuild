package l1j.server.server.afk;

import static l1j.server.server.model.skill.L1SkillId.BLESS_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.ENCHANT_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.HOLY_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.SHADOW_FANG;

import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.model.L1Location;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Party;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1MonsterInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.model.skill.L1SkillUse;
import l1j.server.server.serverpackets.S_Message_YN;
import l1j.server.server.serverpackets.S_MoveCharPacket;
import l1j.server.server.serverpackets.S_NPCTalkReturn;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.templates.L1Skills;
import l1j.server.server.utils.MoveUtil;
import l1j.server.server.datatables.SprTable;
/**
 * High-level operations for 跨系統.
 * This class is referenced from AfkUiRouter + item executor.
 *
 * NOTE: The movement / attack AI hooks are intentionally conservative here.
 * You can extend them to fully integrate with your existing AFK engines.
 */
public final class CoopFollowService {

    private CoopFollowService() {}

    private static final String TALK_ID = "cooperate";

    // Speed multiplier constants (match AcceleratorChecker)
    private static final double HASTE_RATE = 0.75;
    private static final double WAFFLE_RATE = 0.87;
    private static final double DOUBLE_HASTE_RATE = 0.37;

    private enum ActType { MOVE, ATTACK }

    /**
     * Compute the action interval (ms) based on current polymorph sprite and
     * speed buffs/debuffs (haste/brave/third-speed/etc).
     *
     * This mirrors the server's accelerator checker logic so the coop follower
     * does not move/attack faster than the character form allows.
     */
    private static int getRightIntervalMs(final L1PcInstance pc, final ActType type) {
        if (pc == null) return 0;
        int interval;
        try {
            switch (type) {
            case ATTACK:
                interval = SprTable.getInstance().getAttackSpeed(pc.getTempCharGfx(), pc.getCurrentWeapon() + 1);
                break;
            case MOVE:
            default:
                interval = SprTable.getInstance().getMoveSpeed(pc.getTempCharGfx(), pc.getCurrentWeapon());
                break;
            }
        } catch (Throwable t) {
            // Safe fallbacks to avoid division by zero / super fast loops
            interval = (type == ActType.ATTACK) ? 600 : 400;
        }

        // 1st speed: haste/slow
        try {
            switch (pc.getMoveSpeed()) {
            case 1:
                interval = (int) Math.max(1, Math.round(interval * HASTE_RATE));
                break;
            case 2:
                interval = (int) Math.max(1, Math.round(interval / HASTE_RATE));
                break;
            default:
                break;
            }
        } catch (Throwable ignore) {}

        // 2nd speed: brave/waffle/windwalk/etc
        try {
            switch (pc.getBraveSpeed()) {
            case 1: // brave
                interval = (int) Math.max(1, Math.round(interval * HASTE_RATE));
                break;
            case 3: // waffle
                interval = (int) Math.max(1, Math.round(interval * WAFFLE_RATE));
                break;
            case 4: // wind walk / holy walk / etc (move only)
                if (type == ActType.MOVE) {
                    interval = (int) Math.max(1, Math.round(interval * HASTE_RATE));
                }
                break;
            case 5: // double haste
                interval = (int) Math.max(1, Math.round(interval * DOUBLE_HASTE_RATE));
                break;
            case 6: // bloodlust (attack only)
                if (type == ActType.ATTACK) {
                    interval = (int) Math.max(1, Math.round(interval * HASTE_RATE));
                }
                break;
            default:
                break;
            }
        } catch (Throwable ignore) {}

        // Fruit of life
        try {
            if (pc.isRibrave() && type == ActType.MOVE) {
                interval = (int) Math.max(1, Math.round(interval * WAFFLE_RATE));
            }
        } catch (Throwable ignore) {}

        // 3rd speed
        try {
            if (pc.isThirdSpeed()) {
                interval = (int) Math.max(1, Math.round(interval * WAFFLE_RATE));
            }
        } catch (Throwable ignore) {}

        // Wind shackle (attack/cast)
        try {
            if (pc.isWindShackle() && type != ActType.MOVE) {
                interval = (int) Math.max(1, Math.round(interval / 2.0));
            }
        } catch (Throwable ignore) {}

        // Clamp: never allow 0 or negative
        if (interval < 1) interval = 1;
        return interval;
    }

    // === Coop follow global scheduler ===
    static {
        try {
            AfkScheduler.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    try {
                        tickAllFollowers();
                    } catch (Throwable ignore) {}
                }
            }, 200L, 200L);
        } catch (Throwable ignore) {}
    }

    private static void tickAllFollowers() {
        try {
            for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
                try {
                    tickCoop(pc);
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
    }


    // 跨簡key=(pcId<<16)^skillId
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Long> _supportLastCastMs =
            new java.util.concurrent.ConcurrentHashMap<Integer, Long>();


    
    // 目標身上的輔助魔法預估到期時間 key=(targetId<<16)^skillId
    private static final java.util.concurrent.ConcurrentHashMap<Integer, Long> _supportBuffExpireMs =
            new java.util.concurrent.ConcurrentHashMap<Integer, Long>();

public static final int ATTRCODE_FOLLOW_REQUEST = 1257;

// ＿跨移次觸 onLeaderTeleport 
private static final ThreadLocal<Boolean> _insideFollowerTeleport = new ThreadLocal<Boolean>();

    public static void openMainUi(final L1PcInstance pc) {
        if (pc == null) return;
        CoopFollowState st = CoopFollowState.of(pc);

        String followStatus = st.isFollowSystemOn() ? "開" : "關";
        String coopStatus = st.isCoopBattleOn() ? "開" : "關";

        // 根據技能 ID 從 SkillsTable 取得補血魔法名稱，避免編碼問題
        String healSkillName = "";
        try {
            L1Skills sk = SkillsTable.getInstance().getTemplate(st.getHealSkill().skillId);
            if (sk != null) {
                healSkillName = sk.getName();
            }
        } catch (Throwable ignore) {}
        if (healSkillName == null || healSkillName.isEmpty()) {
            // 後備：用簡單中文名稱
            switch (st.getHealSkill()) {
            case LESSER_HEAL:
                healSkillName = "初級治癒術";
                break;
            case EXTRA_HEAL:
                healSkillName = "中級治癒術";
                break;
            case GREATER_HEAL:
                healSkillName = "高級治癒術";
                break;
            case FULL_HEAL:
                healSkillName = "全部治癒術";
                break;
            default:
                healSkillName = "未設定";
                break;
            }
        }

        String healPercent = String.valueOf(st.getHealHpPercent());
        String buffSummary = st.buildBuffNamesString();

        String[] args = new String[] {
                followStatus,
                coopStatus,
                healSkillName,
                healPercent,
                buffSummary
        };
        pc.sendPackets(new S_NPCTalkReturn(pc.getId(), TALK_ID, args));
    }


    public static void toggleFollowSystem(final L1PcInstance pc) {
        if (pc == null) return;
        CoopFollowState st = CoopFollowState.of(pc);
        if (st == null) return;

        boolean wasOn = st.isFollowSystemOn();
        st.toggleFollowSystem();
        boolean nowOn = st.isFollowSystemOn();

        // 當跟隨協同系統從「開」切換成「關」時，清除跟隨與協同攻擊記憶
        if (wasOn && !nowOn) {
            try {
                st.setFollowTargetId(0);
            } catch (Throwable ignore) {}
            try {
                st.setCoopTargetId(0);
            } catch (Throwable ignore) {}
        }

        openMainUi(pc);
    }

    public static void toggleCoopBattle(final L1PcInstance pc) {
        if (pc == null) return;
        CoopFollowState st = CoopFollowState.of(pc);
        st.toggleCoopBattle();
        openMainUi(pc);
    }

    public static void cycleHealSkill(final L1PcInstance pc) {
        if (pc == null) return;
        CoopFollowState st = CoopFollowState.of(pc);
        st.cycleHealSkill();
        openMainUi(pc);
    }

    public static void adjustHealPercent(final L1PcInstance pc, final int delta) {
        if (pc == null) return;
        CoopFollowState st = CoopFollowState.of(pc);
        st.addHealHpPercent(delta);
        openMainUi(pc);
    }
/**
 * 輩泻次模
 * 系統泻許輩累
 */
public static void beginBuffRegister(final L1PcInstance pc) {
    if (pc == null) return;
    final CoopFollowState st = CoopFollowState.of(pc);
    st.setBuffRegistering(true);
    try {
        pc.sendPackets(new S_SystemMessage("【輔助魔法登錄】模式已啟動，請對隊伍成員施放欲登錄的輔助魔法。"));
    } catch (Throwable ignore) {}
    // 試 AfkService.tryOpenSkillWindow使
    try {
        Class<?> svc = Class.forName("l1j.server.server.afk.AfkService");
        Class<?> pcClazz = Class.forName("l1j.server.server.model.Instance.L1PcInstance");
        java.lang.reflect.Method mm = svc.getDeclaredMethod("tryOpenSkillWindow", pcClazz);
        mm.setAccessible(true);
        mm.invoke(null, pc);
    } catch (Throwable ignore) {}
    openMainUi(pc);
}

/**
 * 詢輩表
 */
public static void listBuffs(final L1PcInstance pc) {
    if (pc == null) return;
    final CoopFollowState st = CoopFollowState.of(pc);
    final java.util.Set<Integer> set = st.getBuffSkills();
    StringBuilder sb = new StringBuilder();
    sb.append("Buffs(").append(set.size()).append(") : ");
    boolean first = true;
    for (Integer id : set) {
        if (id == null) continue;
        if (!first) sb.append(", ");
        first = false;
        String name = null;
        try {
            L1Skills sk = SkillsTable.getInstance().getTemplate(id.intValue());
            if (sk != null) name = sk.getName();
        } catch (Throwable ignore) {}
        if (name == null) name = String.valueOf(id);
        sb.append(name);
    }
    try {
        pc.sendPackets(new S_SystemMessage(sb.toString()));
    } catch (Throwable ignore) {}
}

/**
 *  C_UseSkill 泻模次輩
 * 許家身輩Ｄ強紻
 */
public static void captureSupportSkillFromCast(final L1PcInstance pc, final int skillId, final int targetId) {
    if (pc == null) return;
    final CoopFollowState st = CoopFollowState.of(pc);
    if (st == null || !st.isBuffRegistering()) return;
    st.setBuffRegistering(false); // 次

    final L1Object obj = L1World.getInstance().findObject(targetId);
    if (!(obj instanceof L1PcInstance)) {
        // 許家輩
        try { pc.sendPackets(new S_SystemMessage("【輔助魔法登錄】失敗：目標不是玩家。")); } catch (Throwable ignore) {}
        return;
    }
    final L1PcInstance target = (L1PcInstance) obj;
    if (target.getId() == pc.getId()) {
        // 身
        try { pc.sendPackets(new S_SystemMessage("【輔助魔法登錄】失敗：不能對自己施放。")); } catch (Throwable ignore) {}
        return;
    }

    L1Skills sk = null;
    try {
        sk = SkillsTable.getInstance().getTemplate(skillId);
    } catch (Throwable ignore) {}

    if (sk == null) {
        try { pc.sendPackets(new S_SystemMessage("【輔助魔法登錄】失敗：不支援的技能 ID：" + skillId)); } catch (Throwable ignore) {}
        return;
    }

    boolean ok = true;
    try {
        int targetTo = sk.getTargetTo(); // 0:己 1:PC 2:NPC 4: 8: 16:寵...
        int buffDuration = sk.getBuffDuration();
        int dmgDice = sk.getDamageDice();
        int dmgVal = sk.getDamageValue();

        boolean canSupport = ((targetTo & 1) != 0) || ((targetTo & 4) != 0) || ((targetTo & 8) != 0) || ((targetTo & 16) != 0);
        boolean isBuff = buffDuration > 0;
        boolean isPureDamage = (dmgDice > 0) || (dmgVal > 0);

        ok = canSupport && isBuff && !isPureDamage;
    } catch (Throwable ignore) {}

    if (!ok) {
        try { pc.sendPackets(new S_SystemMessage("【輔助魔法登錄】失敗：此技能不是輔助魔法。")); } catch (Throwable ignore) {}
        return;
    }

    st.getBuffSkills().add(Integer.valueOf(skillId));

    String name = null;
    try {
        if (sk != null) name = sk.getName();
    } catch (Throwable ignore) {}
    if (name == null) name = String.valueOf(skillId);
    try { pc.sendPackets(new S_SystemMessage("已登錄輔助魔法：" + name + " (ID=" + skillId + ")")); } catch (Throwable ignore) {}
}

    public static void clearBuffs(final L1PcInstance pc) {
        if (pc == null) return;
        CoopFollowState st = CoopFollowState.of(pc);
        if (st != null) {
            st.getBuffSkills().clear();
        }

        // 同時清除與此玩家相關的輔助施放記錄，避免舊資料造成後續判斷錯誤
        final int pcId = pc.getId();
        try {
            java.util.Iterator<java.util.Map.Entry<Integer, Long>> it1 = _supportLastCastMs.entrySet().iterator();
            while (it1.hasNext()) {
                java.util.Map.Entry<Integer, Long> e = it1.next();
                int key = e.getKey().intValue();
                int idPart = (key >> 16);
                if (idPart == pcId) {
                    it1.remove();
                }
            }
        } catch (Throwable ignore) {}

        try {
            java.util.Iterator<java.util.Map.Entry<Integer, Long>> it2 = _supportBuffExpireMs.entrySet().iterator();
            while (it2.hasNext()) {
                java.util.Map.Entry<Integer, Long> e = it2.next();
                int key = e.getKey().intValue();
                int idPart = (key >> 16);
                if (idPart == pcId) {
                    it2.remove();
                }
            }
        } catch (Throwable ignore) {}

        try {
            pc.sendPackets(new S_SystemMessage("已清除所有已登錄的輔助魔法。"));
        } catch (Throwable ignore) {}

        openMainUi(pc);
    }


    /**
     * 跨
     * 裡種件檢實家 Yes/No 決
     */
    public static void requestFollowTarget(final L1PcInstance pc) {
        if (pc == null) return;
        CoopFollowState st = CoopFollowState.of(pc);
        if (!st.isFollowSystemOn()) {
            pc.sendPackets(new S_SystemMessage("跟隨協同系統尚未開啟。"));
            return;
        }
        if (!pc.isInParty()) {
            pc.sendPackets(new S_SystemMessage("你目前沒有加入隊伍。"));
            return;
        }
        final L1PcInstance target = findFrontPlayer(pc);
        if (target == null) {
            pc.sendPackets(new S_SystemMessage("面前沒有可以選擇的玩家。"));
            return;
        }
        L1Party party = pc.getParty();
        if (party == null || !party.isMember(target)) {
            pc.sendPackets(new S_SystemMessage("目標不是同一個隊伍成員。"));
            return;
        }

        CoopFollowState targetState = CoopFollowState.of(target);
        if (!targetState.isFollowSystemOn()) {
            pc.sendPackets(new S_SystemMessage("對方尚未開啟跟隨協同系統。"));
            return;
        }

        // 尳 ID 身並 Yes/No 
        try {
            target.setTempID(pc.getId());
            target.sendPackets(new S_Message_YN(ATTRCODE_FOLLOW_REQUEST, pc.getName()));
        } catch (Throwable ignore) {}
        pc.sendPackets(new S_SystemMessage("已送出跟隨請求給 " + target.getName() + "。"));
    }

    /**
     *  C_Attr 家 Yes/No 徼
     *
     * @param leader    Yes/No 佩家被
     * @param followerId 汩家 ID
     * @param accepted true=false=
     */
    public static void onFollowConfirm(final L1PcInstance leader, final int followerId, final boolean accepted) {
        if (leader == null) return;

        final L1Object obj = L1World.getInstance().findObject(followerId);
        if (!(obj instanceof L1PcInstance)) {
            return;
        }
        final L1PcInstance follower = (L1PcInstance) obj;

        // 拒絕
        if (!accepted) {
            follower.sendPackets(new S_SystemMessage("你已拒絕 " + leader.getName() + " 的跟隨請求。"));
            leader.sendPackets(new S_SystemMessage(follower.getName() + " 已拒絕你的跟隨請求。"));
            return;
        }

        // 接受：設定跟隨目標
        CoopFollowState st = CoopFollowState.of(follower);
        st.setFollowTargetId(leader.getId());

        // 兩邊各一行正常中文
        follower.sendPackets(new S_SystemMessage("你已開始跟隨 " + leader.getName() + "。"));
        leader.sendPackets(new S_SystemMessage("玩家 " + follower.getName() + " 以跟隨你。"));
    }


    /**
     * 簡徢丼家
     */
    private static L1PcInstance findFrontPlayer(final L1PcInstance pc) {
        try {
            L1Location loc = pc.getLocation();
            int hx = MoveUtil.MoveLocX(loc.getX(), pc.getHeading());
            int hy = MoveUtil.MoveLocY(loc.getY(), pc.getHeading());
            for (L1Object obj : L1World.getInstance().getVisiblePlayer(pc)) {
                if (obj instanceof L1PcInstance) {
                    L1PcInstance other = (L1PcInstance) obj;
                    if (other.getMapId() != pc.getMapId()) continue;
                    if (other.getX() == hx && other.getY() == hy) {
                        if (other == pc) continue;
                        return other;
                    }
                }
            }
        } catch (Throwable ignore) {}
        return null;
    }


    /**
     *  AFK tick  AfkAiController 跨衺
     *
     * @return true 表示 tick 已AfkAiController 以 return
     */ 
    public static boolean tickCoop(final L1PcInstance pc) {
        if (pc == null) return false;
        if (pc.isDead()) return false;

        final CoopFollowState st = CoopFollowState.of(pc);
        if (st == null) return false;
        if (!st.isFollowSystemOn()) return false;

        final int followId = st.getFollowTargetId();
        if (followId <= 0) return false;

        final L1Object obj = L1World.getInstance().findObject(followId);
        if (!(obj instanceof L1PcInstance)) {
            st.setFollowTargetId(0);
            return false;
        }
        final L1PcInstance leader = (L1PcInstance) obj;
        if (leader.isDead()) {
            st.setFollowTargetId(0);
            return false;
        }

        // 不同地圖：直接傳送到隊長身邊
        if (leader.getMapId() != pc.getMapId()) {
            try {
                L1Teleport.teleport(pc,
                        new L1Location(leader.getX(), leader.getY(), leader.getMapId()),
                        leader.getHeading(), true);
            } catch (Throwable ignore) {}
            return true;
        }

        // 同一張地圖但距離太遠：瞬移靠近隊長，避免掉隊
        try {
            int dist = pc.getLocation().getTileLineDistance(leader.getLocation());
            if (dist > 5) { // 這個數字可以自行調
                L1Teleport.teleport(pc,
                        new L1Location(leader.getX(), leader.getY(), leader.getMapId()),
                        leader.getHeading(), true);
                return true;
            }
        } catch (Throwable ignore) {}

        final long now = System.currentTimeMillis();

        // 判斷目前手上的武器是不是遠程（弓 / 鐵手甲）
        boolean isRangedWeapon = false;
        try {
            L1ItemInstance weapon = pc.getWeapon();
            if (weapon != null && weapon.getItem() != null) {
                int type = weapon.getItem().getType();
                // 4: bow, 11: gauntlet
                if (type == 4 || type == 11) {
                    isRangedWeapon = true;
                }
            }
        } catch (Throwable ignore) {}

        // 先判斷目前是否有「有效」的協同攻擊目標
        boolean hasCoopTarget = false;
        L1MonsterInstance coopMob = null;
        if (st.isCoopBattleOn()) {
            int targetId = st.getCoopTargetId();
            if (targetId > 0) {
                L1Object to = L1World.getInstance().findObject(targetId);
                if (to instanceof L1MonsterInstance) {
                    coopMob = (L1MonsterInstance) to;
                    if (!coopMob.isDead()) {
                        hasCoopTarget = true;
                    } else {
                        // 目標已死就清掉記憶
                        try {
                            st.setCoopTargetId(0);
                        } catch (Throwable ignore) {}
                    }
                } else {
                    // 找不到怪物物件也清掉
                    try {
                        st.setCoopTargetId(0);
                    } catch (Throwable ignore) {}
                }
            }
        }

        // 1) 一般跟隨：只有「沒有協同目標」時才會貼近隊長
        //    （近戰 / 遠程都一樣，這樣遠程在沒打怪時也會乖乖跟著走）
        try {
            if (!hasCoopTarget) {
                keepNearLeader(pc, leader);
            }
        } catch (Throwable ignore) {}

        // 2) 協同輔助治療
        try {
            trySupportHeal(pc, leader, st, now);
        } catch (Throwable ignore) {}

        // 3) 協同輔助 BUFF
        try {
            trySupportBuffs(pc, leader, st, now);
        } catch (Throwable ignore) {}

        // 4) 協同攻擊：只有當有有效目標時才執行
        if (hasCoopTarget && coopMob != null) {
            try {
                tryCoopAttack(pc, coopMob);
            } catch (Throwable ignore) {}
        }

        // 5) 其他 AFK 中心處理
        try {
            AfkCenterEngine.process(pc);
        } catch (Throwable ignore) {}

        return true;
    }


    
    private static void keepNearLeader(final L1PcInstance pc, final L1PcInstance leader) {
        if (pc == null || leader == null) return;
        if (pc.isTeleport() || leader.isTeleport()) return;

        final int dist = pc.getLocation().getTileLineDistance(leader.getLocation());
        if (dist <= 2) return; // 已經很靠近，不需要再走

        // Respect sprite/buff movement interval (only when we are going to move)
        try {
            final CoopFollowState st = CoopFollowState.of(pc);
            final long now = System.currentTimeMillis();
            final int interval = getRightIntervalMs(pc, ActType.MOVE);
            if (st != null && (now - st.getLastCoopMoveMs()) < interval) {
                return;
            }
        } catch (Throwable ignore) {}

        final int x = pc.getX();
        final int y = pc.getY();
        final int lx = leader.getX();
        final int ly = leader.getY();

        final int heading = calcHeading(x, y, lx, ly);
        if (heading < 0) return;

        final int nx = MoveUtil.MoveLocX(x, heading);
        final int ny = MoveUtil.MoveLocY(y, heading);
        final L1Map map = pc.getMap();
        if (map == null) return;
        if (!map.isPassable(nx, ny)) return;

        try {
            map.setPassable(pc.getLocation(), true);
        } catch (Throwable ignore) {}

        pc.setHeading(heading);
        pc.getLocation().set(nx, ny);

        // mark move time after we actually moved
        try {
            final CoopFollowState st = CoopFollowState.of(pc);
            if (st != null) st.setLastCoopMoveMs(System.currentTimeMillis());
        } catch (Throwable ignore) {}

        try {
            map.setPassable(pc.getLocation(), false);
        } catch (Throwable ignore) {}

        // 參考掛機系統：自己與其他玩家都收到同一個移動封包
        try {
            S_MoveCharPacket p = new S_MoveCharPacket(pc);
            pc.sendPackets(p);
            pc.broadcastPacket(p);
        } catch (Throwable ignore) {}

        try {
            final CoopFollowState st = CoopFollowState.of(pc);
            if (st != null) st.setLastCoopMoveMs(System.currentTimeMillis());
        } catch (Throwable ignore) {}
    }

private static int calcHeading(int x, int y, int tx, int ty) {
        int dx = tx - x;
        int dy = ty - y;
        if (dx == 0 && dy == 0) return -1;
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);

        if (ax >= ay) {
            if (dx > 0 && dy <= 0) return 2;    // 
            if (dx > 0 && dy > 0) return 3;     // 
            if (dx < 0 && dy <= 0) return 6;    // 左左
            if (dx < 0 && dy > 0) return 5;     // 左
        } else {
            if (dy > 0 && dx >= 0) return 4;    // 
            if (dy > 0 && dx < 0) return 5;     // 左
            if (dy < 0 && dx >= 0) return 1;    // 
            if (dy < 0 && dx < 0) return 7;     // 左
        }
        return -1;
    }

    // 依照玩家手上的武器來取得攻擊距離，沒武器時至少 1 格
    private static int getAttackRange(final L1PcInstance pc) {
        int range = 1;
        if (pc == null) {
            return range;
        }

        try {
            L1ItemInstance weapon = pc.getWeapon();
            if (weapon != null && weapon.getItem() != null) {
                // 依照武器的「種類」來區分近戰 / 遠程：
                // L1Item.getType():
                //   1:sword, 2:twohandsword, 3:dagger, 4:bow, 5:arrow, 6:spear,
                //   7:blunt, 8:staff, 9:claw, 10:dualsword, 11:gauntlet, 12:sting,
                //   13:chainsword, 14:kiringku
                int type = weapon.getItem().getType();

                // 預設近戰 1 格
                range = 1;

                // 弓與鐵手甲視為遠程武器，協同攻擊時希望保持距離
                if (type == 4 || type == 11) {
                    // 不直接使用資料表的 range，改用固定邏輯距離。
                    // 搭配 L1Character.isAttackPosition(int x, int y, int range)：
                    //   range >= 7 → 遠距離（TileDistance）
                    //   range  < 7 → 近距離（TileLineDistance）
                    range = 7;
                }
            }
        } catch (Throwable ignore) {}

        if (range <= 0) {
            range = 1;
        }
        return range;
    }


    // 朝指定座標移動一步（用在協同攻擊時靠近怪物）
    private static void moveStepTowardsTarget(final L1PcInstance pc, final int tx, final int ty) {
        if (pc == null) return;
        if (pc.isTeleport()) return;

        // Respect sprite/buff movement interval
        try {
            final CoopFollowState st = CoopFollowState.of(pc);
            final long now = System.currentTimeMillis();
            final int interval = getRightIntervalMs(pc, ActType.MOVE);
            if (st != null && (now - st.getLastCoopMoveMs()) < interval) {
                return;
            }
        } catch (Throwable ignore) {}

        final int x = pc.getX();
        final int y = pc.getY();

        final int heading = calcHeading(x, y, tx, ty);
        if (heading < 0) return;

        final int nx = MoveUtil.MoveLocX(x, heading);
        final int ny = MoveUtil.MoveLocY(y, heading);
        final L1Map map = pc.getMap();
        if (map == null) return;
        if (!map.isPassable(nx, ny)) return;

        try {
            map.setPassable(pc.getLocation(), true);
        } catch (Throwable ignore) {}

        // 跟 keepNearLeader 一樣：先設定面向，再更新座標
        pc.setHeading(heading);
        pc.getLocation().set(nx, ny);

        // mark move time after we actually moved
        try {
            final CoopFollowState st = CoopFollowState.of(pc);
            if (st != null) st.setLastCoopMoveMs(System.currentTimeMillis());
        } catch (Throwable ignore) {}

        try {
            map.setPassable(pc.getLocation(), false);
        } catch (Throwable ignore) {}

        // 自己與其他玩家都收到同一個移動封包
        try {
            S_MoveCharPacket p = new S_MoveCharPacket(pc);
            pc.sendPackets(p);
            pc.broadcastPacket(p);
        } catch (Throwable ignore) {}
    }

    private static void trySupportHeal(final L1PcInstance pc, final L1PcInstance leader,
            final CoopFollowState st, final long nowMs) {
        if (pc == null || leader == null || st == null) return;

        final int skillId = resolveHealSkillIdForFollower(pc, st.getHealSkill());
        if (skillId <= 0) return; // follower does not know any configured heal spell
        final int hpNow = leader.getCurrentHp();
        final int hpMax = leader.getMaxHp();
        if (hpMax <= 0) return;
        final int hpPercent = (hpNow * 100) / hpMax;

        if (hpPercent > st.getHealHpPercent()) return;

        castSupportSkill(pc, leader, skillId, nowMs);
    }

    /**
     * Only cast support-heal if the follower has actually learned the heal skill.
     *
     * If the configured heal spell is not mastered, this will fall back to a lower-tier
     * heal spell that *is* mastered (e.g. FULL -> GREATER -> EXTRA -> LESSER).
     * Returns 0 if none are mastered.
     */
    private static int resolveHealSkillIdForFollower(final L1PcInstance pc,
            final CoopFollowState.HealSkill desired) {
        if (pc == null || desired == null) return 0;

        // desired first
        try {
            if (pc.isSkillMastery(desired.skillId)) {
                return desired.skillId;
            }
        } catch (Throwable ignore) {}

        // fallbacks (lower tier)
        try {
            switch (desired) {
            case FULL_HEAL:
                if (pc.isSkillMastery(CoopFollowState.HealSkill.GREATER_HEAL.skillId))
                    return CoopFollowState.HealSkill.GREATER_HEAL.skillId;
                // fallthrough
            case GREATER_HEAL:
                if (pc.isSkillMastery(CoopFollowState.HealSkill.EXTRA_HEAL.skillId))
                    return CoopFollowState.HealSkill.EXTRA_HEAL.skillId;
                // fallthrough
            case EXTRA_HEAL:
                if (pc.isSkillMastery(CoopFollowState.HealSkill.LESSER_HEAL.skillId))
                    return CoopFollowState.HealSkill.LESSER_HEAL.skillId;
                break;
            case LESSER_HEAL:
            default:
                break;
            }
        } catch (Throwable ignore) {}
        return 0;
    }

    private static void trySupportBuffs(final L1PcInstance pc, final L1PcInstance leader,
                                        final CoopFollowState st, final long nowMs) {
        if (pc == null || leader == null || st == null) return;
        if (st.getBuffSkills().isEmpty()) return;

        for (Integer id : st.getBuffSkills()) {
            if (id == null) continue;
            castSupportSkill(pc, leader, id.intValue(), nowMs);
        }
    }

    
    // 判斷目標是否已經擁有指定輔助效果（含武器型 BUFF）
    private static boolean hasSupportBuff(final L1PcInstance target, final int skillId) {
        if (target == null) {
            return false;
        }

        // 一般類型：直接用 skillEffect 判斷
        try {
            if (target.hasSkillEffect(skillId)) {
                return true;
            }
        } catch (Throwable ignore) {}

        // 特例：武器型 buff（神聖武器、武器魔法、祝福魔法武器、暗影之牙等）
        try {
            L1ItemInstance weapon = target.getWeapon();
            if (weapon == null) {
                return false;
            }
            switch (skillId) {
                case HOLY_WEAPON:
                    // 神聖武器：+1 命中 +1 聖屬性
                    return weapon.getHolyDmgByMagic() > 0 || weapon.getHitByMagic() > 0;
                case ENCHANT_WEAPON:
                    // 擬似魔法武器：+2 傷害
                    return weapon.getDmgByMagic() >= 2;
                case BLESS_WEAPON:
                    // 祝福魔法武器：+2 傷害 +2 命中
                    return weapon.getDmgByMagic() >= 2 && weapon.getHitByMagic() >= 2;
                case SHADOW_FANG:
                    // 暗影之牙：+5 傷害
                    return weapon.getDmgByMagic() >= 5;
                default:
                    break;
            }
        } catch (Throwable ignore) {}

        return false;
    }


private static boolean castSupportSkill(final L1PcInstance pc, final L1PcInstance target,
                                            final int skillId, final long nowMs) {
        if (pc == null || target == null) return false;
        if (pc.isDead() || target.isDead()) return false;
        if (pc.getMapId() != target.getMapId()) return false;

        final int key = (pc.getId() << 16) ^ (skillId & 0xFFFF);
        final Long last = _supportLastCastMs.get(key);
        // 固定冷卻約 0.5 秒，實際是否施放由 hasSupportBuff(目標, 技能) + 預估到期時間 決定
        if (last != null && (nowMs - last.longValue()) < 500L) {
            return false;
        }

        final int dist = pc.getLocation().getTileLineDistance(target.getLocation());
        if (dist > 8) return false;

        boolean losOk = false;
        try {
            losOk = pc.glanceCheck(target.getX(), target.getY());
        } catch (Throwable __e) {
            losOk = pc.getLocation().isInScreen(target.getLocation());
        }
        if (!losOk) return false;

        // 如果目標已經有這個輔助效果（含武器 BUFF），就不重複施放
        if (hasSupportBuff(target, skillId)) {
            return false;
        }

        final L1SkillUse su = new L1SkillUse();
        su.handleCommands(pc, skillId, target.getId(), target.getX(), target.getY(), null, 0, L1SkillUse.TYPE_NORMAL);

        _supportLastCastMs.put(key, Long.valueOf(nowMs));
        // 記錄目標此 BUFF 的預估到期時間（若技能表有設定 buffDuration）
        try {
            L1Skills sk = SkillsTable.getInstance().getTemplate(skillId);
            if (sk != null) {
                int sec = sk.getBuffDuration();
                if (sec > 0) {
                    long expMs = nowMs + (sec * 1000L);
                    final int tkey = (target.getId() << 16) ^ (skillId & 0xFFFF);
                    _supportBuffExpireMs.put(tkey, Long.valueOf(expMs));
                }
            }
        } catch (Throwable ignore) {}

        return true;
    }

    private static L1MonsterInstance findAssistTarget(final L1PcInstance leader) {
        if (leader == null || leader.isDead()) return null;
        final int SCAN_RANGE = 12;

        L1MonsterInstance nearest = null;
        int best = Integer.MAX_VALUE;

        java.util.List<L1Object> objs = L1World.getInstance().getVisibleObjects(leader, SCAN_RANGE);
        for (L1Object o : objs) {
            if (!(o instanceof L1MonsterInstance)) continue;
            L1MonsterInstance m = (L1MonsterInstance) o;
            if (m.isDead()) continue;

            int d = leader.getLocation().getTileLineDistance(m.getLocation());
            if (d < best) {
                best = d;
                nearest = m;
            }
        }
        return nearest;
    }

    private static void tryCoopAttack(final L1PcInstance pc, final L1MonsterInstance mob) {
        if (pc == null || mob == null) return;
        if (pc.isDead() || mob.isDead()) return;
        if (pc.getMapId() != mob.getMapId()) return;

        try {
            int dist = pc.getLocation().getTileLineDistance(mob.getLocation());
            int range = getAttackRange(pc);

            if (dist > range) {
                moveStepTowardsTarget(pc, mob.getX(), mob.getY());
                return;
            }

            // Respect sprite/buff attack interval
            try {
                final CoopFollowState st = CoopFollowState.of(pc);
                final long now = System.currentTimeMillis();
                final int interval = getRightIntervalMs(pc, ActType.ATTACK);
                if (st != null && (now - st.getLastCoopAttackMs()) < interval) {
                    return;
                }
            } catch (Throwable ignore) {}

            AfkCombatUtil.physicalAttack(pc, mob);

            try {
                final CoopFollowState st = CoopFollowState.of(pc);
                if (st != null) st.setLastCoopAttackMs(System.currentTimeMillis());
            } catch (Throwable ignore) {}
        } catch (Throwable ignore) {}
    }

    public static void onLeaderAttack(final L1PcInstance leader, final L1MonsterInstance mob) {
        if (leader == null || mob == null) return;
        if (leader.isDead() || mob.isDead()) return;

        final int mobId = mob.getId();
        final int leaderId = leader.getId();

        // 家＾正
        try {
            for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
                if (pc == null) continue;
                if (pc.getId() == leaderId) continue;
                if (pc.isDead()) continue;
                if (pc.getMapId() != leader.getMapId()) continue;

                CoopFollowState st = CoopFollowState.of(pc);
                if (st == null) continue;
                if (!st.isFollowSystemOn()) continue;
                if (!st.isCoopBattleOn()) continue;
                if (st.getFollowTargetId() != leaderId) continue;

                // 跨次佰
                st.setCoopTargetId(mobId);
            }
        } catch (Throwable ignore) {}
    }

    /**
     * 宬移 L1Teleport 跨件移身
     */
        /**
     * Leader teleport disabled.
     * Follower teleport is fully handled by tickCoop() to avoid conflicting teleports.
     */
    public static void onLeaderTeleport(final L1PcInstance leader, final int x, final int y, final short mapId) {
        if (leader == null) {
            return;
        }
        // no-op
    }

}
