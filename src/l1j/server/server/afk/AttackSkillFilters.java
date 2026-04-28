package l1j.server.server.afk;

import java.lang.reflect.Method;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.templates.L1Skills;

public class AttackSkillFilters {

	private static final Logger _log = Logger.getLogger(AttackSkillFilters.class.getName());
	private static volatile boolean _loaded = false;
	private static volatile Set<Integer> _WL = Collections.emptySet();

	private static void ensureWhitelist() {
		if (_loaded) return;
		synchronized (AttackSkillFilters.class) {
			if (_loaded) return;
			HashSet<Integer> set = new HashSet<Integer>();
			// 預設允許：三重矢/衝擊之暈/屠宰者
			set.add(Integer.valueOf(132));
			set.add(Integer.valueOf(87));
			set.add(Integer.valueOf(187));
			try (InputStreamReader r = new InputStreamReader(new FileInputStream("./config/afk.properties"), StandardCharsets.UTF_8)) {
				java.util.Properties p = new java.util.Properties();
				p.load(r);
				String raw = p.getProperty("Afk_attack_skill_whitelist", "");
				if (raw != null && !raw.trim().isEmpty()) {
					for (String s : raw.split(",")) {
						try {
							int id = Integer.parseInt(s.trim());
							if (id > 0) set.add(Integer.valueOf(id));
						} catch (Throwable ignore) {}
					}
				}
			} catch (Throwable e) {
				_log.log(Level.FINE, "[AFK] read Afk_attack_skill_whitelist failed", e);
			}
			_WL = Collections.unmodifiableSet(set);
			_loaded = true;
		}
	}

	private static Boolean b(Object o, String m) {
		try { Method mm = o.getClass().getMethod(m); Object r = mm.invoke(o);
			if (r instanceof Boolean) return (Boolean) r;
		} catch (Throwable ignored) {}
		return null;
	}
	private static Integer i(Object o, String m) {
		try { Method mm = o.getClass().getMethod(m); Object r = mm.invoke(o);
			if (r instanceof Integer) return (Integer) r;
		} catch (Throwable ignored) {}
		return null;
	}
	private static String n(Object o, String m) {
		try { Method mm = o.getClass().getMethod(m); Object r = mm.invoke(o);
			if (r instanceof String) return (String) r;
		} catch (Throwable ignored) {}
		return null;
	}

	/** 盡量取出技能 ID（反射相容 getSkillId/getId/getNumber） */
	private static int skillIdOf(L1Skills s) {
		Integer id = i(s, "getSkillId");
		if (id == null) id = i(s, "getId");
		if (id == null) id = i(s, "getNumber");
		return id == null ? 0 : id.intValue();
	}

	private static boolean nameHas(L1Skills s, String... keys) {
		String x = n(s, "getName");
		if (x == null) return false;
		x = x.toLowerCase(java.util.Locale.ROOT);
		for (String k : keys) {
			if (x.contains(k)) return true;
		}
		return false;
	}

	private static boolean looksDamage(L1Skills s) {
		Integer type = i(s, "getType"); // TYPE_ATTACK ?
		if (type != null) {
			try {
				int TA = s.getClass().getField("TYPE_ATTACK").getInt(null);
				if (type.intValue() == TA) return true;
			} catch (Throwable ignored) {
				if (type.intValue() == 1) return true; // 常見分支
			}
		}
		Integer target = i(s, "getTarget");
		if (target != null) {
			try {
				int EN = s.getClass().getField("TARGET_TO_ENEMY").getInt(null);
				if (target.intValue() == EN) return true;
			} catch (Throwable ignored) {
				if (target.intValue() == 2) return true;
			}
		}
		Integer act = i(s, "getActionId");
		if (act != null && act.intValue() > 0) return true;
		if (nameHas(s, "arrow","矢","射","擊","衝擊","爆","破","斬","屠","殺")) return true;
		return false;
	}

	public static boolean isAttackMagic(L1Skills s) {
		if (s == null) return false;

		ensureWhitelist();
		try {
			int sid = skillIdOf(s);
			if (sid > 0 && _WL.contains(Integer.valueOf(sid))) return true; // 白名單直通
		} catch (Throwable ignore) {}

		if (!looksDamage(s)) return false;
		Boolean summon = b(s, "isSummon"); if (summon != null && summon.booleanValue()) return false;
		Boolean curse  = b(s, "isCurse");  if (curse  != null && curse.booleanValue())  return false;
		Boolean buff   = b(s, "isBuff");   if (buff   != null && buff.booleanValue())   return false;
		Boolean heal   = b(s, "isHeal");   if (heal   != null && heal.booleanValue())   return false;
		if (nameHas(s, "summon","召喚","curse","詛咒","buff","護盾","強化","加速","heal","治療","治癒","恢復")) return false;
		return true;
	}
}
