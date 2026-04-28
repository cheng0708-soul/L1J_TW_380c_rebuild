package l1j.server.server.utils;

import java.util.concurrent.ConcurrentHashMap;

import l1j.server.server.model.Instance.L1PcInstance;

public final class AutofarmRegistry {
	private static final ConcurrentHashMap<Integer, Boolean> _afk = new ConcurrentHashMap<Integer, Boolean>();

	private AutofarmRegistry() {}

	public static boolean isEnabled(final int objId) {
		Boolean v = _afk.get(objId);
		return v != null && v.booleanValue();
	}

	public static boolean enable(final int objId) {
		_afk.put(objId, Boolean.TRUE);
		return true;
	}

	public static boolean disable(final int objId) {
		_afk.remove(objId);
		return true;
	}

	public static boolean toggle(final int objId) {
		if (isEnabled(objId)) {
			disable(objId);
			return false;
		} else {
			enable(objId);
			return true;
		}
	}

	public static boolean isEnabled(final L1PcInstance pc) {
		return pc != null && isEnabled(pc.getId());
	}

	public static void stopFor(final L1PcInstance pc) {
		if (pc == null) return;
		disable(pc.getId());
	}
}