package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.map.L1Map;
import l1j.server.server.utils.MoveUtil;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 供 AFK 尋路 / 重置 Path 用；提供 static 與 instance 兩種入口以相容舊碼。
 */
public class AfkPathService {
    private static final int[] DIRS = { 0, 1, 7, 2, 6, 3, 5, 4 };
    private static final Map<Integer, PathState> PATHS = new ConcurrentHashMap<Integer, PathState>();

    // --- static 入口（常見呼叫：AfkPathService.clear(pc)） ---
    public static void clear(final L1PcInstance pc) {
        if (pc == null) return;
        PATHS.remove(pc.getId());
    }

    public static void stepToward(final L1PcInstance pc, final int x, final int y) {
        if (pc == null) return;
        if (!AfkConfig.USE_PATHFINDING) return;

        final L1Map map = pc.getMap();
        final int sx = pc.getX();
        final int sy = pc.getY();
        if (sx == x && sy == y) return;

        final int pcId = pc.getId();
        PathState state = PATHS.get(pcId);
        if (state == null || !state.matches(pc, x, y) || state.steps.isEmpty()) {
            state = buildPathState(pc, x, y);
            if (state == null || state.steps.isEmpty()) {
                PATHS.remove(pcId);
                return;
            }
            PATHS.put(pcId, state);
        }

        while (!state.steps.isEmpty() && state.steps.get(0).packed == pack(sx, sy)) {
            state.steps.remove(0);
        }
        if (state.steps.isEmpty()) {
            PATHS.remove(pcId);
            return;
        }

        final Step next = state.steps.get(0);
        if (!isAdjacent(sx, sy, next.x, next.y) || !map.isPassable(next.x, next.y)) {
            state = buildPathState(pc, x, y);
            if (state == null || state.steps.isEmpty()) {
                PATHS.remove(pcId);
                return;
            }
            PATHS.put(pcId, state);
        }
        final Step actualNext = state.steps.get(0);

        try {
            AfkPacketUtil.moveOutOneBroadcast(pc, actualNext.x, actualNext.y);
            state.steps.remove(0);
            if (state.steps.isEmpty()) {
                PATHS.remove(pcId);
            }
        } catch (Throwable ignore) {}
    }

    // --- instance 入口（若有用 new AfkPathService().clear(pc) 也能編過） ---
    public void clearPath(final L1PcInstance pc) { clear(pc); }

    public void stepToward(final L1PcInstance pc, final int x, final int y, final int dist) {
        stepToward(pc, x, y);
    }

    private static PathState buildPathState(final L1PcInstance pc, final int tx, final int ty) {
        final L1Map map = pc.getMap();
        final int sx = pc.getX();
        final int sy = pc.getY();
        final int maxDepth = Math.max(8, AfkConfig.PF_MAX_STEPS);

        final ArrayDeque<Node> q = new ArrayDeque<Node>();
        final Set<Integer> seen = new HashSet<Integer>();
        final Map<Integer, Integer> prev = new ConcurrentHashMap<Integer, Integer>();

        final int startKey = pack(sx, sy);
        q.add(new Node(sx, sy, 0));
        seen.add(startKey);
        prev.put(startKey, Integer.valueOf(startKey));

        int bestKey = startKey;
        int bestDist = chebyshev(sx, sy, tx, ty);

        while (!q.isEmpty()) {
            final Node cur = q.pollFirst();
            final int curKey = pack(cur.x, cur.y);
            final int curDist = chebyshev(cur.x, cur.y, tx, ty);
            if (curDist < bestDist) {
                bestDist = curDist;
                bestKey = curKey;
                if (bestDist <= 1) break;
            }
            if (cur.depth >= maxDepth) continue;

            for (int dir : DIRS) {
                final int nx = MoveUtil.MoveLocX(cur.x, dir);
                final int ny = MoveUtil.MoveLocY(cur.y, dir);
                final int key = pack(nx, ny);
                if (seen.contains(key)) continue;
                if (!map.isPassable(nx, ny)) continue;
                seen.add(key);
                prev.put(key, Integer.valueOf(curKey));
                q.addLast(new Node(nx, ny, cur.depth + 1));
            }
        }

        if (bestKey == startKey) return null;

        final ArrayDeque<Integer> stack = new ArrayDeque<Integer>();
        int cur = bestKey;
        while (cur != startKey) {
            stack.addFirst(Integer.valueOf(cur));
            final Integer parent = prev.get(cur);
            if (parent == null || parent.intValue() == cur) break;
            cur = parent.intValue();
        }
        if (stack.isEmpty()) return null;

        final List<Step> steps = new ArrayList<Step>();
        for (Integer key : stack) {
            final int px = unpackX(key.intValue());
            final int py = unpackY(key.intValue());
            steps.add(new Step(px, py));
        }
        return new PathState(pc.getMapId(), tx, ty, steps);
    }

    private static int chebyshev(final int x1, final int y1, final int x2, final int y2) {
        return Math.max(Math.abs(x1 - x2), Math.abs(y1 - y2));
    }

    private static int pack(final int x, final int y) {
        return (x << 16) ^ (y & 0xFFFF);
    }

    private static int unpackX(final int packed) {
        return (packed >> 16);
    }

    private static int unpackY(final int packed) {
        return (short) (packed & 0xFFFF);
    }

    private static boolean isAdjacent(final int sx, final int sy, final int tx, final int ty) {
        return Math.max(Math.abs(sx - tx), Math.abs(sy - ty)) == 1;
    }

    private static final class Node {
        private final int x;
        private final int y;
        private final int depth;

        private Node(final int x, final int y, final int depth) {
            this.x = x;
            this.y = y;
            this.depth = depth;
        }
    }

    private static final class Step {
        private final int x;
        private final int y;
        private final int packed;

        private Step(final int x, final int y) {
            this.x = x;
            this.y = y;
            this.packed = pack(x, y);
        }
    }

    private static final class PathState {
        private final short mapId;
        private final int targetX;
        private final int targetY;
        private final List<Step> steps;

        private PathState(final short mapId, final int targetX, final int targetY, final List<Step> steps) {
            this.mapId = mapId;
            this.targetX = targetX;
            this.targetY = targetY;
            this.steps = steps;
        }

        private boolean matches(final L1PcInstance pc, final int tx, final int ty) {
            return pc != null
                && pc.getMapId() == mapId
                && this.targetX == tx
                && this.targetY == ty;
        }
    }
}
