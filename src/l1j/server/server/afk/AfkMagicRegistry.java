package l1j.server.server.afk;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * AfkMagicRegistry — strong-key version (v2)
 * - 強鍵 ConcurrentHashMap，避免 WeakHashMap 清除
 * - getRegistered/registeredSkillIds 回傳 LinkedHashSet<Integer>（相容舊程式宣告）
 */
public final class AfkMagicRegistry {
	private static final Logger _log = Logger.getLogger(AfkMagicRegistry.class.getName());

	private AfkMagicRegistry() {}

	private static final ConcurrentMap<Integer, Boolean> _enabled = new ConcurrentHashMap<Integer, Boolean>();
	private static final ConcurrentMap<Integer, Set<Integer>> _skills = new ConcurrentHashMap<Integer, Set<Integer>>();
	/** key = (pcId<<16) ^ (skillId & 0xFFFF) */
	private static final ConcurrentMap<Integer, Long> _lastCastAtMs = new ConcurrentHashMap<Integer, Long>();

	public static void setEnabled(final L1PcInstance pc, final boolean on) {
		if (pc == null) return;
		_enabled.put(pc.getId(), Boolean.valueOf(on));
		if (AfkSettings.DEBUG()) {
			_log.log(Level.INFO, "[AFK] magic enabled pcId={0} on={1}", new Object[]{pc.getId(), on});
		}
	}

	public static boolean isEnabled(final L1PcInstance pc) {
		if (pc == null) return false;
		final Boolean b = _enabled.get(pc.getId());
		return b != null && b.booleanValue();
	}

	public static void clear(final L1PcInstance pc) {
		if (pc == null) return;
		_skills.remove(pc.getId());
		if (AfkSettings.DEBUG()) {
			_log.log(Level.INFO, "[AFK] magic clear pcId={0}", pc.getId());
		}
	}

	public static void registerSingleSkill(final L1PcInstance pc, final int skillId) {
		if (pc == null) return;
		final LinkedHashSet<Integer> set = new LinkedHashSet<Integer>();
		set.add(Integer.valueOf(skillId));
		_skills.put(pc.getId(), set);
		if (AfkSettings.DEBUG()) {
			_log.log(Level.INFO, "[AFK] magic register single pcId={0} skillId={1}", new Object[]{pc.getId(), skillId});
		}
	}

	public static void register(final L1PcInstance pc, final Collection<Integer> ids) {
		if (pc == null || ids == null || ids.isEmpty()) return;
		final LinkedHashSet<Integer> set = new LinkedHashSet<Integer>(ids);
		_skills.put(pc.getId(), set);
		if (AfkSettings.DEBUG()) {
			_log.log(Level.INFO, "[AFK] magic register multi pcId={0} size={1}", new Object[]{pc.getId(), set.size()});
		}
	}

	public static LinkedHashSet<Integer> registeredSkillIds(final L1PcInstance pc) {
		if (pc == null) return new LinkedHashSet<Integer>();
		final Set<Integer> s = _skills.get(pc.getId());
		return (s == null) ? new LinkedHashSet<Integer>() : new LinkedHashSet<Integer>(s);
	}

	public static LinkedHashSet<Integer> getRegistered(final L1PcInstance pc) {
		final Set<Integer> s = _skills.get(pc.getId());
		return (s == null) ? new LinkedHashSet<Integer>() : new LinkedHashSet<Integer>(s);
	}

	public static boolean canCastNow(final L1PcInstance pc, final int skillId, final long nowMs) {
		if (pc == null) return false;
		if (!isEnabled(pc)) return false;
		final long cooldownMs = 4000L;
		final int key = (pc.getId() << 16) ^ (skillId & 0xFFFF);
		final Long last = _lastCastAtMs.get(key);
		if (last == null) return true;
		return (nowMs - last.longValue()) >= cooldownMs;
	}

	public static void markCast(final L1PcInstance pc, final int skillId, final long nowMs) {
		if (pc == null) return;
		final int key = (pc.getId() << 16) ^ (skillId & 0xFFFF);
		_lastCastAtMs.put(key, Long.valueOf(nowMs));
	}
}
