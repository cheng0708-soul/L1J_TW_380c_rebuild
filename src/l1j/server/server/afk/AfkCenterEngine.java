
package l1j.server.server.afk;

import java.util.HashMap;
import java.util.Map;
import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkCenterEngine {
    private AfkCenterEngine() {}

    private static final int CENTER_DIST = 6;
    private static final int COOLDOWN_MS = 3000;
    private static final int MOVE_WINDOW_MS = 600;

    private static final Map<Integer, Integer> _refPack = new HashMap<Integer, Integer>();
    private static final Map<Integer, Long> _coolUntil = new HashMap<Integer, Long>();
    private static final Map<Integer, Integer> _lastPack = new HashMap<Integer, Integer>();
    private static final Map<Integer, Long> _lastPackMs = new HashMap<Integer, Long>();

    public static boolean process(final L1PcInstance pc) {
        if (pc == null) return false;

        final int id = pc.getId();
        final long now = System.currentTimeMillis();
        final int curPack = pack(pc.getX(), pc.getY());

        boolean moving = false;
        Integer prev = _lastPack.get(id);
        if (prev == null || prev.intValue() != curPack) {
            moving = true;
            _lastPack.put(id, curPack);
            _lastPackMs.put(id, now);
        } else {
            Long lm = _lastPackMs.get(id);
            if (lm != null && (now - lm.longValue()) <= MOVE_WINDOW_MS) moving = true;
        }
        if (!moving) return false;

        Long cd = _coolUntil.get(id);
        if (cd != null && now < cd.longValue()) return false;

        Integer ref = _refPack.get(id);
        if (ref == null) {
            _refPack.put(id, curPack);
            return false;
        }

        final int rx = (ref.intValue() >> 16);
        final int ry = (ref.intValue() & 0xFFFF);
        final int dx = Math.abs(pc.getX() - rx);
        final int dy = Math.abs(pc.getY() - ry);
        final int dist = (dx > dy) ? dx : dy;

        if (dist >= CENTER_DIST) {
            AfkRecenter.strongRecenter(pc);
            _refPack.put(id, curPack);
            _coolUntil.put(id, now + COOLDOWN_MS);
            return true;
        }
        return false;
    }

    public static void resetRef(final L1PcInstance pc) {
        if (pc == null) return;
        final int id = pc.getId();
        final long now = System.currentTimeMillis();
        final int curPack = pack(pc.getX(), pc.getY());
        _refPack.put(id, curPack);
        _lastPack.put(id, curPack);
        _lastPackMs.put(id, now);
        _coolUntil.put(id, now + COOLDOWN_MS);
    }

    private static int pack(final int x, final int y) {
        return (x << 16) ^ (y & 0xFFFF);
    }
}
