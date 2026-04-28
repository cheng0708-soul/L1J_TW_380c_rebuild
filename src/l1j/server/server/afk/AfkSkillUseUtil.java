package l1j.server.server.afk;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.model.L1Character;
import l1j.server.server.model.Instance.L1PcInstance;

/**
 * 更穩定的施法工具：
 * - 加入完整防呆（pc / target / skillId / map）
 * - 以反射呼叫 L1SkillUse.handleCommands(...)，自動匹配多種簽名
 * - 任一簽名呼叫成功即返回，不會讓例外中斷 AFK 排程
 */
public final class AfkSkillUseUtil {

    private static final Logger _log = Logger.getLogger(AfkSkillUseUtil.class.getName());

    private AfkSkillUseUtil() {}

    public static void castSkill(final L1PcInstance pc, final int skillId, final L1Character target) {
        // ---- 基本防呆：避免 NPE 把排程砸掉 ----
        if (pc == null || pc.isDead() || pc.isTeleport()) return;
        if (skillId <= 0) return;
        if (target == null) return; // 攻擊/指向型技能沒有目標就不施放（避免進 L1SkillUse NPE）
        try {
            final int targetId = target.getId();
            final int tx = target.getX();
            final int ty = target.getY();
            final short mapId = (short) pc.getMapId();

            // 反射載入 L1SkillUse
            final Class<?> cls = Class.forName("l1j.server.server.model.skill.L1SkillUse");
            final Method[] methods = cls.getDeclaredMethods();

            // 嘗試所有叫做 handleCommands 的方法
            outer:
            for (Method m : methods) {
                if (!"handleCommands".equals(m.getName())) continue;
                Class<?>[] ps = m.getParameterTypes();
                Object[] args = new Object[ps.length];

                // 依參數型別填入對應值
                int[] ints = new int[] { skillId, targetId, tx, ty, 0, 0 };
                int intIdx = 0;
                boolean needsCharacter = false;
                for (int i = 0; i < ps.length; i++) {
                    Class<?> p = ps[i];
                    if (p.getName().equals("l1j.server.server.model.Instance.L1PcInstance")) {
                        args[i] = pc;
                    } else if (p == int.class) {
                        args[i] = ints[intIdx < ints.length ? intIdx : (ints.length - 1)];
                        intIdx++;
                    } else if (p.getName().equals("l1j.server.server.model.L1Character")) {
                        needsCharacter = true;
                        args[i] = target;
                    } else if (p == boolean.class) {
                        args[i] = Boolean.TRUE;
                    } else if (p == short.class) {
                        args[i] = mapId;
                    } else {
                        args[i] = null;
                    }
                }

                // 若該簽名需要 L1Character，卻沒有 target，跳過
                if (needsCharacter && target == null) continue;

                try {
                    Constructor<?> ctor = cls.getConstructor();
                    Object su = ctor.newInstance();
                    m.invoke(su, args);
                    return; // 任一成功即結束
                } catch (Throwable t) {
                    // 換下一個簽名嘗試
                    continue outer;
                }
            }

            _log.log(Level.WARNING, "[AFK] 未能找到可用的 L1SkillUse.handleCommands 簽名");
        } catch (Throwable t) {
            // 不讓排程中斷
            _log.log(Level.WARNING, "[AFK] castSkill 例外（已忽略以維持排程）", t);
        }
    }
}
