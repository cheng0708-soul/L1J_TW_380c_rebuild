package l1j.server.server.afk;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

public final class AfkScheduler {
	private static Timer _fallbackTimer;
	private AfkScheduler() {}

	public static void scheduleAtFixedRate(final Runnable r, long delayMs, long periodMs) {
		try {
			Class<?> gtp = Class.forName("l1j.server.server.GeneralThreadPool");
			Method gi = gtp.getMethod("getInstance");
			Method schedule = gtp.getMethod("scheduleAtFixedRate", Runnable.class, long.class, long.class);
			Object inst = gi.invoke(null);
			schedule.invoke(inst, r, delayMs, periodMs);
			return;
		} catch (Throwable t) {}
		if (_fallbackTimer == null) _fallbackTimer = new Timer("AFK-Scheduler", true);
		_fallbackTimer.scheduleAtFixedRate(new TimerTask(){ @Override public void run(){ try{ r.run(); }catch(Throwable ignore){} }}, delayMs, periodMs);
	}
}