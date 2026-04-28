package l1j.server.server.afk;

import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.model.L1Character;
import l1j.server.server.model.Instance.L1PcInstance;

/**
 * 物理攻擊相容層：各分支可能有 attack(...) / onAction(...) / 其它。
 * 這裡會嘗試多種常見方法，找不到就嘗試 target.onAction(pc)。
 */
public final class AfkCombatUtil {
	private static final Logger _log = Logger.getLogger(AfkCombatUtil.class.getName());
	private AfkCombatUtil() {}

	public static void physicalAttack(L1PcInstance pc, L1Character target) {
        // AFK_HIDDEN_GUARD: if target is hidden (sink/fly), notify and drop
        try {
            if (target instanceof l1j.server.server.model.Instance.L1NpcInstance) {
                l1j.server.server.model.Instance.L1NpcInstance tn =
                    (l1j.server.server.model.Instance.L1NpcInstance) target;
                int hs = tn.getHiddenStatus();
                if (hs == l1j.server.server.model.Instance.L1NpcInstance.HIDDEN_STATUS_SINK ||
                    hs == l1j.server.server.model.Instance.L1NpcInstance.HIDDEN_STATUS_FLY) {
                    try {
                        if (target instanceof l1j.server.server.model.Instance.L1MonsterInstance) {
                            l1j.server.server.afk.AfkEventHub.onMobHidden(
                                (l1j.server.server.model.Instance.L1MonsterInstance) target);
                        }
                    } catch (Throwable ignore) {}
                    return;
                }
            }
        } catch (Throwable ignore) {}
		if (pc == null || target == null) return;
		try {
			// 1) pc.attack(L1Character)
			tryInvoke(pc, "attack", new Class<?>[]{ L1Character.class }, new Object[]{ target });
			return;
		} catch (Throwable ignore) {}
		try {
			// 2) pc.onAction(L1Character)
			tryInvoke(pc, "onAction", new Class<?>[]{ L1Character.class }, new Object[]{ target });
			return;
		} catch (Throwable ignore) {}
		try {
			// 3) target.onAction(L1PcInstance) 由目標處理被攻擊
			tryInvoke(target, "onAction", new Class<?>[]{ L1PcInstance.class }, new Object[]{ pc });
			return;
		} catch (Throwable ignore) {}

		_log.log(Level.FINE, "[AFK] physicalAttack: no compatible attack/onAction method found");
	}

	private static void tryInvoke(Object obj, String name, Class<?>[] types, Object[] args) throws Exception {
		Method m = obj.getClass().getMethod(name, types);
		m.invoke(obj, args);
	}
}