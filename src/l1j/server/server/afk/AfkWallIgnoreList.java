package l1j.server.server.afk;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 牆後怪暫時忽略清單（無定時重試版）。
 * - 只要條目存在且與玩家距離未超過 ignoreRadius，就持續忽略。
 * - 超過 ignoreRadius（預設 25）或命中回饋會清除。
 */
public class AfkWallIgnoreList {
	private static final Map<Object, ConcurrentHashMap<Integer, Ignore>> _map = new WeakHashMap<Object, ConcurrentHashMap<Integer, Ignore>>();

	public static boolean shouldIgnore(Object playerKey, int targetId, int distance, long now) {
		ConcurrentHashMap<Integer, Ignore> m = _map.get(playerKey);
		if (m == null) return false;
		Ignore ig = m.get(Integer.valueOf(targetId));
		if (ig == null) return false;
		// 距離過大直接清除（自動解除忽略）
		if (distance > AfkWallConfig.ignoreRadius()) {
			m.remove(Integer.valueOf(targetId));
			return false;
		}
		// 不使用時間重試；只要還在距離內就維持忽略
		return true;
	}

	public static void markIgnore(Object playerKey, int targetId, long now) {
		ConcurrentHashMap<Integer, Ignore> m = _map.get(playerKey);
		if (m == null) {
			m = new ConcurrentHashMap<Integer, Ignore>();
			_map.put(playerKey, m);
		}
		m.put(Integer.valueOf(targetId), new Ignore());
	}

	public static void clearIgnore(Object playerKey, int targetId) {
		ConcurrentHashMap<Integer, Ignore> m = _map.get(playerKey);
		if (m != null) m.remove(Integer.valueOf(targetId));
	}

	private static class Ignore {
		// 目前無額外欄位（保留擴充）
	}
}