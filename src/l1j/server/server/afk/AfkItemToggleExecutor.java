package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkItemToggleExecutor {
	public AfkItemToggleExecutor() {}
	public static void handle(L1PcInstance pc){ AfkRegistry.get(pc).toggle(); }
	public void execute(L1PcInstance pc, Object item){ handle(pc); }
}