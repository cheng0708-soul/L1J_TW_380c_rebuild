package l1j.server.server.afk;

import java.util.logging.Level;
import java.util.logging.Logger;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.model.map.L1WorldMap;

public class AfkWallJudge {
    private static final Logger _log = Logger.getLogger(AfkWallJudge.class.getName());

    public static boolean shouldAttack(L1PcInstance pc, L1Character target) {
        if (!AfkWallConfig.enabled()) return true;
        if (pc == null || target == null) return false;
        if (pc.isDead() || target.isDead()) return false;
        final int dist = distance(pc, target);
        final long now = System.currentTimeMillis();
        // active ignore?
        if (AfkWallIgnoreList.shouldIgnore(pc, target.getId(), dist, now)) {
            // NEW: if path becomes clear within rangeCheck, clear ignore and allow attack
            int chk = Math.max(3, AfkWallConfig.rangeCheck());
            if (dist <= chk && isAttackPathClear(pc, target)) {
                AfkWallIgnoreList.clearIgnore(pc, target.getId());
                debug("path clear now -> clear ignore: t=" + target.getId());
            } else {
                return false;
            }
        }
        if (dist > AfkWallConfig.rangeCheck()) return true;
        if (!isAttackPathClear(pc, target)) {
            AfkWallIgnoreList.markIgnore(pc, target.getId(), now);
            debug("blocked, mark ignore: t=" + target.getId());
            return false;
        }
        return true;
    }

    public static boolean isAttackPathClear(L1Character from, L1Character to) {
        try {
            L1Map map = from.getMap() != null ? from.getMap() : L1WorldMap.getInstance().getMap(from.getMapId());
            if (map == null) return true;
            int x0 = from.getX(), y0 = from.getY(), x1 = to.getX(), y1 = to.getY();
            int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0), sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
            int err = dx - dy, steps = 0, stepLimit = Math.max(8, AfkWallConfig.losStepLimit());
            int cx = x0, cy = y0;
            while (true) {
                if (!(cx == x0 && cy == y0) && !(cx == x1 && cy == y1)) {
                    if (!gridPass(map, cx, cy)) return false;
                }
                if (cx == x1 && cy == y1) return true;
                int e2 = 2 * err;
                if (e2 > -dy) { err -= dy; cx += sx; }
                if (e2 < dx) { err += dx; cy += sy; }
                if (++steps > stepLimit) return false;
            }
        } catch (Throwable t) { debugEx("isAttackPathClear exception", t); return true; }
    }

    private static boolean gridPass(L1Map map, int x, int y) {
        try {
            try { java.lang.reflect.Method m = map.getClass().getMethod("isArrowPassable", int.class, int.class);
                  Object r = m.invoke(map, x, y); if (r instanceof Boolean) return ((Boolean) r).booleanValue(); } catch (NoSuchMethodException ns) {}
            try { java.lang.reflect.Method m2 = map.getClass().getMethod("isPassable", int.class, int.class);
                  Object r2 = m2.invoke(map, x, y); if (r2 instanceof Boolean) return ((Boolean) r2).booleanValue(); } catch (NoSuchMethodException ns2) {}
            return true;
        } catch (Throwable t) { return true; }
    }

    private static int distance(L1Character a, L1Character b) {
        int dx = Math.abs(a.getX() - b.getX()), dy = Math.abs(a.getY() - b.getY());
        return Math.max(dx, dy);
    }

    private static void debug(String s) { if (AfkWallConfig.debug()) _log.info("[WallJudge] " + s); }
    private static void debugEx(String s, Throwable t) { if (AfkWallConfig.debug()) _log.log(Level.INFO, "[WallJudge] " + s, t); }
}
