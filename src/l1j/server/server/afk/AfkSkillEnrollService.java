package l1j.server.server.afk;

import java.util.logging.Logger;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.templates.L1Skills;

/**
 * AfkSkillEnrollService
 * 僅提供 3 個行為（對接 autofarm-c.html 的 action=7/8/9）：
 *  - startRegister(pc)  : 進入登錄模式，嘗試彈出技能視窗（失敗則提示手動開啟）
 *  - query(pc)         : 顯示目前已登錄的攻擊技能
 *  - clear(pc)         : 清除已登錄技能
 *
 * ✅ 完全不改動任何既有 AI/巡航/總開關 ！
 */
public class AfkSkillEnrollService {

	private static final Logger _log = Logger.getLogger(AfkSkillEnrollService.class.getName());
	private static final AfkSkillEnrollService _instance = new AfkSkillEnrollService();

	public static AfkSkillEnrollService getInstance() { return _instance; }
	private AfkSkillEnrollService() {}

	public boolean maybeHandleAction(L1PcInstance pc, int actionCode) {
		if (pc == null) return false;
		switch (actionCode) {
			case 7: startRegister(pc); return true;
			case 8: query(pc); return true;
			case 9: clear(pc); return true;
			default: return false;
		}
	}

	public void startRegister(L1PcInstance pc) {
		AfkSkillRegistry.getInstance().setRegisterMode(pc, true);
		pc.sendPackets(new S_SystemMessage("【掛機】技能登錄模式啟動：請選擇一個攻擊魔法，我會把下一次點擊視為登錄（不會真的施法）。"));
		tryOpenSkillWindow(pc);
	}

	public void query(L1PcInstance pc) {
		int id = AfkSkillRegistry.getInstance().getAttackSkill(pc);
		if (id <= 0) {
			pc.sendPackets(new S_SystemMessage("【掛機】尚未登錄攻擊技能。"));
		} else {
			L1Skills s = SkillsTable.getInstance().getTemplate(id);
			String name = (s == null) ? ("ID=" + id) : s.getName();
			pc.sendPackets(new S_SystemMessage("【掛機】已登錄攻擊技能：" + name + " (ID=" + id + ")"));
		}
	}

	public void clear(L1PcInstance pc) {
		AfkSkillRegistry.getInstance().setAttackSkill(pc, 0);
		pc.sendPackets(new S_SystemMessage("【掛機】已清除登錄攻擊技能。"));
	}

	/**
	 * 嘗試彈出技能視窗：因各分支 S_SkillList 建構子不同，這裡用反射多種方案嘗試；
	 * 失敗就只提示使用者手動打開技能視窗（不影響功能）。
	 */
	private void tryOpenSkillWindow(L1PcInstance pc) {
		try {
			// 方案 A：S_SkillList(boolean open, L1Skills[] list)
			Class<?> pkt = Class.forName("l1j.server.server.serverpackets.S_SkillList");
			try {
				java.lang.reflect.Constructor<?> c = pkt.getConstructor(boolean.class, l1j.server.server.templates.L1Skills[].class);
				Object o = c.newInstance(Boolean.TRUE, new l1j.server.server.templates.L1Skills[0]);
				pc.sendPackets((l1j.server.server.serverpackets.ServerBasePacket) o);
				return;
			} catch (Throwable ignore) {}

			// 方案 B：S_SkillList() 無參 → 直接打開現有視窗
			try {
				java.lang.reflect.Constructor<?> c = pkt.getConstructor();
				Object o = c.newInstance();
				pc.sendPackets((l1j.server.server.serverpackets.ServerBasePacket) o);
				return;
			} catch (Throwable ignore) {}

			// 方案 C：其他常見簽名（int type, int level, int[] skills）→ 僅為了觸發開窗
			try {
				java.lang.reflect.Constructor<?> c = pkt.getConstructor(int.class, int.class, int[].class);
				Object o = c.newInstance(0, 0, new int[0]);
				pc.sendPackets((l1j.server.server.serverpackets.ServerBasePacket) o);
				return;
			} catch (Throwable ignore) {}

		} catch (Throwable t) {
			// 找不到 S_SkillList 類別或轉發失敗 → 靠提示即可
		}
		pc.sendPackets(new S_SystemMessage("【掛機】若未自動彈出，請手動打開技能視窗後點擊要登錄的攻擊魔法。"));
	}
}
