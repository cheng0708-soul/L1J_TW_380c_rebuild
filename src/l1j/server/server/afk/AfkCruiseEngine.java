package l1j.server.server.afk;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.utils.GuajiTeleportHelper;

/** 巡航引擎：排程 tick 並以反射呼叫 AfkAiController */
public class AfkCruiseEngine {
    private static final long TICK_MS = 150L;            // tick 週期（實際走路/攻擊由 AfkSpeedUtil 動態節流）
    private static final long TELEPORT_IDLE_MS = 15_000L;

    private static class State {
        volatile boolean teleportMode = false;
        volatile int patrolRadius = -1; // -1/0 = 無限制
        volatile int originX, originY, originMap;
        volatile boolean originSet = false;
        volatile long lastCombatMs = System.currentTimeMillis();
        ScheduledFuture<?> future;
    }

    private static final Map<Integer, State> STATES = new ConcurrentHashMap<Integer, State>();
    private static final ScheduledExecutorService SCHED = Executors.newScheduledThreadPool(2);

    public static void setTeleportMode(L1PcInstance pc, boolean enable) {
        if (pc == null) return;
        State st = STATES.computeIfAbsent(pc.getId(), k -> new State());
        st.teleportMode = enable;
        if (enable) {
            st.patrolRadius = -1; // 瞬移模式默認無限制巡航
        }
    }

    public static void setPatrolOriginIfAbsent(L1PcInstance pc) {
        if (pc == null) return;
        State st = STATES.computeIfAbsent(pc.getId(), k -> new State());
        if (!st.originSet) {
            st.originX = pc.getX();
            st.originY = pc.getY();
            st.originMap = pc.getMapId();
            st.originSet = true;
        }
    }

    public static void clearOrigin(L1PcInstance pc) {
        if (pc == null) return;
        State st = STATES.computeIfAbsent(pc.getId(), k -> new State());
        st.originSet = false;
    }

    public static void setPatrolRadius(L1PcInstance pc, int radius) {
        if (pc == null) return;
        State st = STATES.computeIfAbsent(pc.getId(), k -> new State());
        if (radius <= 0) { st.patrolRadius = -1; return; } // 無限制
        if (radius < 10) radius = 10;
        if (radius > 100) radius = 100;
        st.patrolRadius = radius;
    }

    public static void markCombat(L1PcInstance pc) {
        if (pc == null) return;
        State st = STATES.computeIfAbsent(pc.getId(), k -> new State());
        st.lastCombatMs = System.currentTimeMillis();
    }

    public static void start(final L1PcInstance pc) {
        if (pc == null) return;
        State st = STATES.computeIfAbsent(pc.getId(), k -> new State());
        if (st.future != null && !st.future.isCancelled()) return;

        // 只有在「有巡邏半徑（10/30/50/100）」時才記錄原點；無限制模式不記原點
        
        // 半徑超界 → 強制瞬回原座標（僅在有限半徑 10/30/50/100 模式）
        if (st.patrolRadius > 0 && st.originSet) {
            try {
                l1j.server.server.model.L1Location origin = new l1j.server.server.model.L1Location(st.originX, st.originY, st.originMap);
                int distFromOrigin = pc.getLocation().getTileLineDistance(origin);
                if (distFromOrigin > st.patrolRadius) {
                    l1j.server.server.model.L1Teleport.teleport(pc, origin, pc.getHeading(), true);
                    try { l1j.server.server.afk.AfkAiController.clearChaseTarget(pc); } catch (Throwable ignore) {}
                    st.lastCombatMs = System.currentTimeMillis(); // 避免馬上觸發隨機瞬移
                }
            } catch (Throwable ignore) {}
        }
        if (st.patrolRadius > 0) setPatrolOriginIfAbsent(pc);

        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    tick(pc);
                } catch (Throwable t) {
                    // swallow
                }
            }
        };
        st.future = SCHED.scheduleAtFixedRate(r, 200L, TICK_MS, TimeUnit.MILLISECONDS);
    }

    public static void stop(L1PcInstance pc) {
        if (pc == null) return;
        State st = STATES.get(pc.getId());
        if (st != null && st.future != null) {
            try { st.future.cancel(false); } catch (Throwable ignore) {}
            st.future = null;
        }
        // 關閉總開關或被強制關閉時，清除原點記憶
        clearOrigin(pc);
    }

    private static void tick(L1PcInstance pc) throws Exception {
        State st = STATES.computeIfAbsent(pc.getId(), k -> new State());
        if (pc.isDead() || pc.isTeleport()) return;
        if (!AfkService.isRunning(pc)) {
            stop(pc);
            return;
        }

        // 15 秒未戰鬥且開啟瞬移模式 → 隨機瞬移並重置原點
        if (st.teleportMode) {
            long idle = System.currentTimeMillis() - st.lastCombatMs;
            if (idle >= TELEPORT_IDLE_MS) {
                if (doRandomTeleport(pc, st)) {
                    st.lastCombatMs = System.currentTimeMillis();
                }
            }
        }

        
        // 半徑超界 → 強制瞬回原座標（僅在有限半徑 10/30/50/100 模式）
        if (st.patrolRadius > 0 && st.originSet) {
            try {
                l1j.server.server.model.L1Location origin = new l1j.server.server.model.L1Location(st.originX, st.originY, st.originMap);
                int distFromOrigin = pc.getLocation().getTileLineDistance(origin);
                if (distFromOrigin > st.patrolRadius) {
                    l1j.server.server.model.L1Teleport.teleport(pc, origin, pc.getHeading(), true);
                    try { l1j.server.server.afk.AfkAiController.clearChaseTarget(pc); } catch (Throwable ignore) {}
                    st.lastCombatMs = System.currentTimeMillis(); // 避免馬上觸發隨機瞬移
                }
            } catch (Throwable ignore) {}
        }

        if (st.patrolRadius > 0) setPatrolOriginIfAbsent(pc);
        invokeAfkAi(pc, st);
    }

    private static void invokeAfkAi(L1PcInstance pc, State st) throws Exception {
        int ox = st.originX, oy = st.originY, omap = st.originMap;
        int radius = st.patrolRadius;
        Class<?> c = Class.forName("l1j.server.server.afk.AfkAiController");
        Method m = null;
        String[] names = new String[] { "tick", "step", "work", "update", "loopOnce" };
        for (String n : names) {
            try {
                m = c.getDeclaredMethod(n, L1PcInstance.class, int.class, int.class, int.class, int.class);
                break;
            } catch (NoSuchMethodException ignore) {}
        }
        if (m == null) {
            // 回退到 4 參數版本
            for (String n : names) {
                try {
                    m = c.getDeclaredMethod(n, L1PcInstance.class, int.class, int.class, int.class);
                    m.setAccessible(true);
                    m.invoke(null, pc, ox, oy, omap);
                    return;
                } catch (NoSuchMethodException ignore) {}
            }
            throw new NoSuchMethodException("AfkAiController 缺少 tick/step/work/update/loopOnce 任一方法");
        }
        m.setAccessible(true);
        m.invoke(null, pc, ox, oy, omap, radius);
    }

    private static boolean doRandomTeleport(L1PcInstance pc, State st) {
        try {
            if (!GuajiTeleportHelper.doTeleport(pc)) {
                return false;
            }
            st.originX = pc.getX();
            st.originY = pc.getY();
            st.originMap = pc.getMapId();
            st.originSet = true;
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
