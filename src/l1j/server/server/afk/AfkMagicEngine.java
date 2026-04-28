package l1j.server.server.afk;

import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.ConfigGuaji;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.skill.L1SkillUse;

public final class AfkMagicEngine {
	private static final Logger _log = Logger.getLogger(AfkMagicEngine.class.getName());

	private AfkMagicEngine() {}

	public static boolean tryCastOne(final L1PcInstance pc, final L1Character target) {
		if (pc == null) return false;
		if (!AfkMagicRegistry.isEnabled(pc)) return false;

		final LinkedHashSet<Integer> regs = AfkMagicRegistry.getRegistered(pc);
		if (regs.isEmpty()) return false;

		final long now = System.currentTimeMillis();
		// AFK_HIDDEN_GUARD: skip hidden/fly targets (sink/fly) before any cast
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
                    return false;
                }
            }
        } catch (Throwable ignore) {}
        for (Integer skillId : regs) {
			if (!AfkMagicRegistry.canCastNow(pc, skillId.intValue(), now)) continue;
			if (performCast(pc, target, skillId.intValue())) {
				AfkMagicRegistry.markCast(pc, skillId.intValue(), now);
				return true;
			}
		}
		return false;
	}

	private static boolean performCast(final L1PcInstance pc, final L1Character target, final int skillId) {
		try {
			if (pc == null || target == null) return false;
			if (pc.getMapId() != target.getMapId()) return false;

			final int dist = pc.getLocation().getTileLineDistance(target.getLocation());
			int range = ConfigGuaji.GUAJI_MAGIC_CAST_RANGE;
			try {
				Integer rv = ConfigGuaji.GUAJI_MAGIC_CAST_RANGE_MAP.get(Integer.valueOf(skillId));
				if (rv != null && rv.intValue() > 0) range = rv.intValue();
			} catch (Throwable __ignore) {}
			if (dist > range) return false;

			boolean losOk = false;
			try {
				losOk = pc.glanceCheck(target.getX(), target.getY());
			} catch (Throwable __e) {
				losOk = pc.getLocation().isInScreen(target.getLocation());
			}
			if (!losOk) return false;

			final L1SkillUse su = new L1SkillUse();
			final int targetId = target.getId();
			su.handleCommands(pc, skillId, targetId, pc.getX(), pc.getY(), null, 0, L1SkillUse.TYPE_NORMAL);
			return true;
		} catch (Throwable e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			return false;
		}
	}
}