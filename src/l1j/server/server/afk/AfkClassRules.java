// 新增檔：src/l1j/server/server/afk/AfkClassRules.java
// 目的：取得玩家職業（以通用方式，兼容不同分支），並提供簡單判斷。
package l1j.server.server.afk;

import java.lang.reflect.Method;

import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkClassRules {
	private AfkClassRules() {}

	public static String getClassKey(L1PcInstance pc) {
		if (pc == null) return "Unknown";
		// 優先嘗試常見的布林方法
		try { if (invokeBool(pc, "isElf")) return "Elf"; } catch (Throwable ignore) {}
		try { if (invokeBool(pc, "isKnight")) return "Knight"; } catch (Throwable ignore) {}
		try { if (invokeBool(pc, "isDragonKnight")) return "DragonKnight"; } catch (Throwable ignore) {}
		try { if (invokeBool(pc, "isDarkelf")) return "DarkElf"; } catch (Throwable ignore) {}
		try { if (invokeBool(pc, "isCrown")) return "Crown"; } catch (Throwable ignore) {}
		try { if (invokeBool(pc, "isWizard")) return "Wizard"; } catch (Throwable ignore) {}
		try { if (invokeBool(pc, "isIllusionist")) return "Illusionist"; } catch (Throwable ignore) {}
		try { if (invokeBool(pc, "isWarrior")) return "Warrior"; } catch (Throwable ignore) {}
		try { if (invokeBool(pc, "isFencer")) return "Fencer"; } catch (Throwable ignore) {}
		try { if (invokeBool(pc, "isLancer")) return "Lancer"; } catch (Throwable ignore) {}

		// 後備：某些分支有 getType() / getClassId()
		try {
			Method m = pc.getClass().getMethod("getType"); // 常見：0王 1騎 2妖 3法 4黑 5龍 6幻...
			Object r = m.invoke(pc);
			if (r instanceof Integer) {
				int t = ((Integer) r).intValue();
				switch (t) {
					case 0: return "Crown";
					case 1: return "Knight";
					case 2: return "Elf";
					case 3: return "Wizard";
					case 4: return "DarkElf";
					case 5: return "DragonKnight";
					case 6: return "Illusionist";
					case 7: return "Warrior";
					default: return "Unknown";
				}
			}
		} catch (Throwable ignore) {}
		return "Unknown";
	}

	private static boolean invokeBool(L1PcInstance pc, String name) throws Exception {
		Method m = pc.getClass().getMethod(name);
		Object r = m.invoke(pc);
		return (r instanceof Boolean) && ((Boolean) r).booleanValue();
	}
}
