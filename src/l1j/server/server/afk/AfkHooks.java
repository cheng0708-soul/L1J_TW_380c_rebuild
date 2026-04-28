package l1j.server.server.afk;

import java.lang.reflect.Method;
import java.util.logging.Logger;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * AFK Kill-Switch helper (v5 - targets AfkController.forceOff)
 *
 * Primary path:
 *   AfkController.forceOff(pc)
 *
 * Fallbacks:
 *   AfkService.toggleRunning(pc,false) / toggle(pc,false) / stop(pc) / setEnabledFor(pc,false)
 *   plus the previous generic attempts.
 */
public final class AfkHooks {
	private static final Logger _log = Logger.getLogger(AfkHooks.class.getName());

	private AfkHooks() {}

	public static void killNow(final L1PcInstance pc) {
		if (pc == null) return;
		boolean ok = false;
		// 1) AfkController.forceOff(pc)
		try {
			final Class<?> ctrl = Class.forName("l1j.server.server.afk.AfkController");
			final Method m = ctrl.getMethod("forceOff", L1PcInstance.class);
			m.invoke(null, pc); // most likely static
			ok = true;
			_log.info("[AFK] Kill-switch via AfkController.forceOff(pc).");
		} catch (Throwable ignore) {}

		// 2) Service fallbacks
		if (!ok) {
			try {
				final Class<?> svcClazz = Class.forName("l1j.server.server.afk.AfkService");
				final Object svc = getServiceInstance(svcClazz);
				ok |= tryInvokeBoolean(svcClazz, svc, "toggleRunning", pc, false);
				ok |= tryInvokeBoolean(svcClazz, svc, "toggle", pc, false);
				ok |= tryInvoke(svcClazz, svc, "stop", pc);
				ok |= tryInvokeBoolean(svcClazz, svc, "setEnabledFor", pc, false);
				ok |= tryInvoke(svcClazz, svc, "disableFor", pc);
				if (ok) _log.info("[AFK] Kill-switch via AfkService.*");
			} catch (Throwable ignore) {}
		}

		if (!ok) {
			_log.info("[AFK] Kill-switch: no direct API matched. Please wire AfkController.forceOff in your 40117 handler directly.");
		}
	}

	// =========== helpers ===========
	private static Object getServiceInstance(Class<?> svcClazz) {
		try { return svcClazz.getMethod("getInstance").invoke(null); } catch (Throwable ignore) {}
		try { return svcClazz.getField("INSTANCE").get(null); } catch (Throwable ignore) {}
		try { return svcClazz.getDeclaredConstructor().newInstance(); } catch (Throwable ignore) {}
		return null;
	}

	private static boolean tryInvoke(Class<?> svcClazz, Object svc, String name, L1PcInstance pc) {
		try { svcClazz.getMethod(name, L1PcInstance.class).invoke(svc, pc); return true; } catch (Throwable ignore) {}
		return false;
	}
	private static boolean tryInvokeBoolean(Class<?> svcClazz, Object svc, String name, L1PcInstance pc, boolean flag) {
		try { svcClazz.getMethod(name, L1PcInstance.class, boolean.class).invoke(svc, pc, flag); return true; } catch (Throwable ignore) {}
		return false;
	}
}
