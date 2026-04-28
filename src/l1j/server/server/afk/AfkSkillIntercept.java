// 新增/覆蓋檔案：src/l1j/server/server/afk/AfkSkillIntercept.java
// 作用：在玩家從技能視窗點選技能欲施放時，若目前處於「登錄模式」，就把點到的 skillId 記錄下來，並吃掉這次施法。
package l1j.server.server.afk;

import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkSkillIntercept {
	private static final Logger _log = Logger.getLogger(AfkSkillIntercept.class.getName());

	private AfkSkillIntercept() {}

	/**
	 * @return true 表示「已攔截（完成登錄）」；false 表示「不處理，讓原本施法流程繼續」
	 */
	public static boolean onBeforeCast(L1PcInstance pc, int skillId) {
		if (pc == null) return false;
		if (!AfkSkillRegistry.isRegisterMode(pc)) return false;

		AfkSkillRegistry.set(pc, skillId);
		AfkSkillRegistry.setRegisterMode(pc, false);

		if (AfkSettings.DEBUG()) {
			_log.log(Level.INFO, "[AFK] intercept cast for register pcId={0} skillId={1}", new Object[]{pc.getId(), skillId});
			System.out.println("[AFK] intercept: pcId=" + pc.getId() + " skillId=" + skillId);
		}
		// true -> 呼叫端應該 return，不繼續施法
		return true;
	}
}
