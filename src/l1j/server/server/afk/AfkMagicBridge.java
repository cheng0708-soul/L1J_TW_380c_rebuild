package l1j.server.server.afk;

import java.util.List;

import l1j.server.server.model.L1Character;
import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkMagicBridge {
	private AfkMagicBridge() {}

	public static Integer getPreferredSkillId(L1PcInstance pc, L1Character target) {
		List<Integer> pri = AfkConfig.MAGIC_PRIORITY;
		if (pri == null || pri.isEmpty()) return null;
		return pri.get(0);
	}
}