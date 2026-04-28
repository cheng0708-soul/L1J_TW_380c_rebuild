package l1j.server.server.afk;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AfkConfig {
	public static boolean IGNORE_HIDDEN_MOBS = true;
	public static boolean RANGED_ARROW_PROBE = true;
	public static int SCREEN_CAST_RANGE = 18;
	private static final Logger _log = Logger.getLogger(AfkConfig.class.getName());
	private static final String CONFIG_FILE = "config/afk.properties";

	public static boolean AFK_ENABLED;
	public static boolean AFK_DEBUG;

	public static int MELEE_CHASE_RANGE;
	public static int RANGED_MAX_RANGE;
	public static int RANGED_MIN_RANGE;

	public static boolean CHECK_LOS;
	public static boolean CHECK_TRAJECTORY;
	public static int LOS_STEP_LIMIT;

	public static boolean USE_PATHFINDING;
	public static int PF_MAX_STEPS;
	public static int PF_RETRY_LIMIT;
	public static int PF_STUCK_TICKS;

	public static int COMBAT_TIMEOUT_MS;
	public static int REPOSITION_COOLDOWN_MS;
	public static boolean ALLOW_FREE_TELEPORT;
	public static boolean ALLOW_SCROLL_TELEPORT;
	public static boolean ALLOW_HOME_SCROLL;
	public static int HOMETOWN_ID;

	public static int AFK_TOGGLE_ITEM_ID;

	public static List<Integer> MAGIC_PRIORITY = new ArrayList<Integer>();
	public static Map<Integer, Integer> SKILL_RANGE = new HashMap<Integer, Integer>();

	private AfkConfig() {}

	public static void load() {
		Properties p = new Properties();
		try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
			p.load(fis);
		} catch (IOException e) {
			_log.log(Level.WARNING, "AFK config not found, using defaults: " + CONFIG_FILE, e);
		}

		AFK_ENABLED = getBool(p, "Afk_enabled", true);
		AFK_DEBUG = getBool(p, "Afk_debug", false);
		IGNORE_HIDDEN_MOBS = getBool(p, "Afk_ignore_hidden_mobs", true);
		RANGED_ARROW_PROBE = getBool(p, "RANGED_ARROW_PROBE", true);

		MELEE_CHASE_RANGE = getInt(p, "Afk_melee_chase_range", 4);
		RANGED_MAX_RANGE = getInt(p, "Afk_ranged_max_range", 10);
		RANGED_MIN_RANGE = getInt(p, "Afk_ranged_min_range", 2);

		CHECK_LOS = getBool(p, "Afk_check_los", true);
		CHECK_TRAJECTORY = getBool(p, "Afk_check_trajectory", true);
		LOS_STEP_LIMIT = getInt(p, "Afk_los_step_limit", 24);

		USE_PATHFINDING = getBool(p, "Afk_use_pathfinding", true);
		PF_MAX_STEPS = getInt(p, "Afk_pf_max_steps", 32);
		PF_RETRY_LIMIT = getInt(p, "Afk_pf_retry_limit", 3);
		PF_STUCK_TICKS = getInt(p, "Afk_pf_stuck_ticks", 6);

		COMBAT_TIMEOUT_MS = getInt(p, "Afk_combat_timeout_ms", 6000);
		REPOSITION_COOLDOWN_MS = getInt(p, "Afk_reposition_cooldown_ms", 1500);
		ALLOW_FREE_TELEPORT = getBool(p, "Afk_allow_free_teleport", true);
		ALLOW_SCROLL_TELEPORT = getBool(p, "Afk_allow_scroll_teleport", true);
		ALLOW_HOME_SCROLL = getBool(p, "Afk_allow_home_scroll", true);
		HOMETOWN_ID = getInt(p, "Afk_hometown_id", 0);

		AFK_TOGGLE_ITEM_ID = getInt(p, "Afk_toggle_item_id", 240101);

		parseMagicPriority(p.getProperty("Afk_magic_priority", ""));
		parseSkillRangeMap(p.getProperty("Afk_skill_range_map", ""));

		_log.info("[AFK] Config loaded (v3).");
	}

	private static void parseMagicPriority(String raw) {
		MAGIC_PRIORITY.clear();
		if (raw == null || raw.isEmpty()) return;
		for (String s : raw.split(",")) {
			try { MAGIC_PRIORITY.add(Integer.parseInt(s.trim())); } catch (NumberFormatException ignored) {}
		}
	}

	private static void parseSkillRangeMap(String raw) {
		SKILL_RANGE.clear();
		if (raw == null || raw.isEmpty()) return;
		for (String pair : raw.split(",")) {
			String[] kv = pair.trim().split(":");
			if (kv.length != 2) continue;
			try {
				int key = Integer.parseInt(kv[0].trim());
				int val = Integer.parseInt(kv[1].trim());
				SKILL_RANGE.put(key, val);
			} catch (NumberFormatException ignored) {}
		}
	}

	private static boolean getBool(Properties p, String k, boolean def) {
		String v = p.getProperty(k);
		if (v == null) return def;
		return "true".equalsIgnoreCase(v) || "1".equals(v);
	}

	private static int getInt(Properties p, String k, int def) {
		String v = p.getProperty(k);
		if (v == null) return def;
		try { return Integer.parseInt(v); } catch (NumberFormatException e) { return def; }
	}
}