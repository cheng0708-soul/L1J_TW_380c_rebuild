// 覆蓋檔案：src/l1j/server/server/afk/AfkSkillRegistry.java
// 調整：把 debug log 提升到 INFO，確保在預設 logging 設定下也能看到；
// 並保留先前攻擊技能判定與 getInstance() 兼容方法。
package l1j.server.server.afk;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkSkillRegistry {
	private static final Logger _log = Logger.getLogger(AfkSkillRegistry.class.getName());
	private static final AfkSkillRegistry INSTANCE = new AfkSkillRegistry();
	public static AfkSkillRegistry getInstance() { return INSTANCE; }
	private AfkSkillRegistry() {}

	private static final ConcurrentMap<Integer, Integer> _attackSkillByPcId = new ConcurrentHashMap<Integer, Integer>();
	private static final ConcurrentMap<Integer, Boolean> _registerModeByPcId = new ConcurrentHashMap<Integer, Boolean>();

	public static int get(L1PcInstance pc) {
		if (pc == null) return 0;
		Integer v = _attackSkillByPcId.get(pc.getId());
		if (AfkSettings.DEBUG()) {
			_log.log(Level.INFO, "[AFK] get attack skill pcId={0} -> {1}", new Object[]{pc.getId(), (v == null ? 0 : v)});
		}
		return v == null ? 0 : v.intValue();
	}

	public static void set(L1PcInstance pc, int skillId) {
		if (pc == null) return;
		if (skillId <= 0) {
			_attackSkillByPcId.remove(pc.getId());
			if (AfkSettings.DEBUG()) _log.log(Level.INFO, "[AFK] clear attack skill for pcId={0}", pc.getId());
			return;
		}
		// 若有 AfkSkillRules，檢查是否可登錄（若你的專案沒有 AfkSkillRules，可移除此判定或確保該類已存在）
		try {
			Class.forName("l1j.server.server.afk.AfkSkillRules");
			if (!AfkSkillRules.canRegisterAttackSkill(pc, skillId)) {
				if (AfkSettings.DEBUG()) {
					_log.log(Level.INFO, "[AFK] reject non-attack skill pcId={0} skillId={1}", new Object[]{pc.getId(), skillId});
				}
				return;
			}
		} catch (ClassNotFoundException ignore) {
			// 沒有規則類則直接通過
		}
		_attackSkillByPcId.put(pc.getId(), skillId);
		if (AfkSettings.DEBUG()) {
			_log.log(Level.INFO, "[AFK] set attack skill pcId={0} skillId={1}", new Object[]{pc.getId(), skillId});
		}
	}

	public static boolean isRegisterMode(L1PcInstance pc) {
		if (pc == null) return false;
		Boolean v = _registerModeByPcId.get(pc.getId());
		if (AfkSettings.DEBUG()) {
			_log.log(Level.INFO, "[AFK] isRegisterMode pcId={0} -> {1}", new Object[]{pc.getId(), (v != null && v.booleanValue())});
		}
		return v != null && v.booleanValue();
	}

	public static void setRegisterMode(L1PcInstance pc, boolean on) {
		if (pc == null) return;
		if (on) _registerModeByPcId.put(pc.getId(), Boolean.TRUE);
		else _registerModeByPcId.remove(pc.getId());
		if (AfkSettings.DEBUG()) {
			_log.log(Level.INFO, "[AFK] set registerMode pcId={0} on={1}", new Object[]{pc.getId(), on});
		}
	}

	public static void clear(L1PcInstance pc) {
		if (pc == null) return;
		_attackSkillByPcId.remove(pc.getId());
		_registerModeByPcId.remove(pc.getId());
		if (AfkSettings.DEBUG()) _log.log(Level.INFO, "[AFK] clear all for pcId={0}", pc.getId());
	}
	public static void onLogout(L1PcInstance pc) { clear(pc); }

	// 舊程式相容：instance/別名
	public int getAttackSkill(L1PcInstance pc) { return get(pc); }
	public void setAttackSkill(L1PcInstance pc, int id) { set(pc, id); }
	public boolean isRegisterModeInstance(L1PcInstance pc) { return isRegisterMode(pc); }
	public void setRegisterMode(L1PcInstance pc, boolean on, boolean dummy) { setRegisterMode(pc, on); }
}
