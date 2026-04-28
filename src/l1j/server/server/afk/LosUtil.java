package l1j.server.server.afk;

import l1j.server.server.model.map.L1Map;
import l1j.server.server.utils.IntRange;

public final class LosUtil {
	private LosUtil() {}

	public static boolean hasLineOfSight(L1Map map, int x0, int y0, int x1, int y1) {
		if (!AfkConfig.CHECK_LOS) return true;
		return sample(map, x0, y0, x1, y1);
	}

	public static boolean hasClearTrajectory(L1Map map, int x0, int y0, int x1, int y1) {
		if (!AfkConfig.CHECK_TRAJECTORY) return true;
		return sample(map, x0, y0, x1, y1);
	}

	private static boolean sample(L1Map map, int x0, int y0, int x1, int y1) {
		int dx = Math.abs(x1 - x0);
		int dy = Math.abs(y1 - y0);
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		int err = dx - dy;
		int steps = 0;
		while (true) {
			if (steps++ > AfkConfig.LOS_STEP_LIMIT) return false;
			if (!(x0 == x1 && y0 == y1)) {
				if (!map.isPassable(x0, y0)) return false;
			}
			if (x0 == x1 && y0 == y1) break;
			int e2 = 2 * err;
			if (e2 > -dy) { err -= dy; x0 += sx; }
			if (e2 <  dx) { err += dx; y0 += sy; }
		}
		return true;
	}

	public static int clampRange(int v, int min, int max) {
		return IntRange.ensure(v, min, max);
	}
}