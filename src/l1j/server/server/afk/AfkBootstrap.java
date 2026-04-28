package l1j.server.server.afk;

import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkBootstrap {
	private static volatile boolean _inited = false;
	private AfkBootstrap() {}

	public static synchronized void init() {
		if (_inited) return;
		l1j.server.server.afk.AfkConfig.load();
		long period = 500L;
		AfkScheduler.scheduleAtFixedRate(new Runnable(){
			@Override public void run() {
				for (L1PcInstance pc : L1World.getInstance().getAllPlayers()) {
					try { l1j.server.server.afk.AfkRegistry.get(pc).tick(); } catch (Throwable ignore){}
				}
			}
		}, period, period);
		_inited = true;
	}
}