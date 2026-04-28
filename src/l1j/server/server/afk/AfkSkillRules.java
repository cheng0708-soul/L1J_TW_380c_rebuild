// 覆蓋檔：src/l1j/server/server/afk/AfkSkillRules.java
// 在既有攻擊技能判斷前，先做「職業專屬白名單」判斷（命中立即通過）。
package l1j.server.server.afk;

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.templates.L1Skills;
import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkSkillRules {
	private static final Logger _log = Logger.getLogger(AfkSkillRules.class.getName());

	private AfkSkillRules() {}

	public static boolean canRegisterAttackSkill(L1PcInstance pc, int skillId) {
		// 1) 職業專屬白名單（命中直接通過）
		String ck = AfkClassRules.getClassKey(pc);
		Set<Integer> wl = AfkSettings.classWhitelist(ck);
		if (wl.contains(Integer.valueOf(skillId))) {
			if (AfkSettings.DEBUG()) {
				_log.log(Level.INFO, "[AFK] class whitelist pass class={0} skillId={1}", new Object[]{ck, skillId});
			}
			return true;
		}

		// 2) 既有攻擊型檢查（你原先的策略）：isDamage / TYPE_ATTACK / TARGET_TO_ENEMY
		L1Skills s = null;
		try {
			s = SkillsTable.getInstance().getTemplate(skillId);
		} catch (Throwable t) {
			if (AfkSettings.DEBUG()) {
				_log.log(Level.FINE, "[AFK] SkillsTable.getTemplate error for skillId=" + skillId, t);
			}
		}
		if (s == null) return false;

		try {
			java.lang.reflect.Method m = L1Skills.class.getMethod("isDamage");
			Object r = m.invoke(s);
			if (r instanceof Boolean && ((Boolean) r).booleanValue()) return true;
		} catch (Throwable ignore) {}

		try {
			java.lang.reflect.Method mType = L1Skills.class.getMethod("getType");
			Object r = mType.invoke(s);
			if (r instanceof Integer) {
				int type = ((Integer) r).intValue();
				try {
					java.lang.reflect.Field f = L1Skills.class.getField("TYPE_ATTACK");
					if (type == f.getInt(null)) return true;
				} catch (Throwable e) {
					if (type == 1) return true;
				}
			}
		} catch (Throwable ignore) {}

		try {
			java.lang.reflect.Method mTarget = L1Skills.class.getMethod("getTarget");
			Object r = mTarget.invoke(s);
			if (r instanceof Integer) {
				int target = ((Integer) r).intValue();
				try {
					java.lang.reflect.Field f = L1Skills.class.getField("TARGET_TO_ENEMY");
					if (target == f.getInt(null)) return true;
				} catch (Throwable e) {
					if (target == 2) return true;
				}
			}
		} catch (Throwable ignore) {}

		return false;
	}
}
