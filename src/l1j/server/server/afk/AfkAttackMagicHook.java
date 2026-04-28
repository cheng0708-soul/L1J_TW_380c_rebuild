package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkAttackMagicHook {
	private AfkAttackMagicHook() {}
	public static void onHit(L1PcInstance pc){ if (pc != null) AfkEventBridge.onSuccessfulHit(pc); }
}