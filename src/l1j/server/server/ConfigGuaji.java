package l1j.server.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConfigGuaji {
	private static final Logger _log = Logger.getLogger(ConfigGuaji.class.getName());
	private static final String _path = "config/COM_掛機_整合.properties";

	public static boolean GUAJI_ACTION = true;
	public static String GUAJI_CMD = "9520851a";

	public static Set<Integer> GUAJI_MAP_STOPSKILL = new HashSet<Integer>();

	public static boolean GUAJI_TELE = true;
	public static int GUAJI_TELE_ITEM = 40100;
	public static int GUAJI_TELE_ITEMCOUNT = 1;

	public static int GUAJI_TOGGLE_ITEM = 240101;
	public static boolean GUAJI_TOGGLE_ITEM_CONSUME = false;

	public static boolean GUAJI_RETURN_USE_TOWNAPI = true;
	public static short GUAJI_RETURN_MAP = 4;
	public static int GUAJI_RETURN_X = 33437;
	public static int GUAJI_RETURN_Y = 32809;
	public static int GUAJI_RETURN_HEAD = 5;

	public static boolean GUAJI_MAGIC_ENABLE = true;
	public static LinkedHashSet<Integer> GUAJI_MAGIC_ALLOWED = new LinkedHashSet<Integer>();
	public static LinkedHashSet<Integer> GUAJI_MAGIC_REGISTERED_DEFAULT = new LinkedHashSet<Integer>();
	public static int GUAJI_MAGIC_CAST_SEC = 2;
	public static int GUAJI_MAGIC_CAST_RANGE = 12;
	
	public static java.util.Map<Integer, Integer> GUAJI_MAGIC_CAST_RANGE_MAP = new java.util.HashMap<Integer, Integer>();
public static Map<Integer, Integer> GUAJI_MAGIC_CAST_SEC_MAP = new HashMap<Integer, Integer>();

	private ConfigGuaji() {}

	public static void load() {
		final Properties p = new Properties();
		InputStream is = null;
		try {
			is = new FileInputStream(_path);
			p.load(is);

			GUAJI_ACTION = getBoolean(p, "Guaji_action", GUAJI_ACTION);
			GUAJI_CMD = p.getProperty("guaji", GUAJI_CMD);

			GUAJI_MAP_STOPSKILL = parseIntSet(p.getProperty("Guaji_map_stopskill", ""));

			GUAJI_TELE = getBoolean(p, "Guaji_tele", GUAJI_TELE);
			GUAJI_TELE_ITEM = getInt(p, "Guaji_tele_item", GUAJI_TELE_ITEM);
			GUAJI_TELE_ITEMCOUNT = getInt(p, "Guaji_tele_itemcount", GUAJI_TELE_ITEMCOUNT);

			GUAJI_TOGGLE_ITEM = getInt(p, "Guaji_toggle_item", GUAJI_TOGGLE_ITEM);
			GUAJI_TOGGLE_ITEM_CONSUME = getBoolean(p, "Guaji_toggle_item_consume", GUAJI_TOGGLE_ITEM_CONSUME);

			GUAJI_RETURN_USE_TOWNAPI = getBoolean(p, "Guaji_return_use_townapi", GUAJI_RETURN_USE_TOWNAPI);
			GUAJI_RETURN_MAP = (short) getInt(p, "Guaji_return_map", GUAJI_RETURN_MAP);
			GUAJI_RETURN_X = getInt(p, "Guaji_return_x", GUAJI_RETURN_X);
			GUAJI_RETURN_Y = getInt(p, "Guaji_return_y", GUAJI_RETURN_Y);
			GUAJI_RETURN_HEAD = getInt(p, "Guaji_return_head", GUAJI_RETURN_HEAD);

			GUAJI_MAGIC_ENABLE = getBoolean(p, "Guaji_magic_enable", GUAJI_MAGIC_ENABLE);
			GUAJI_MAGIC_ALLOWED = parseIntSetOrdered(p.getProperty("Guaji_magic_allowed", ""));
			GUAJI_MAGIC_REGISTERED_DEFAULT = parseIntSetOrdered(p.getProperty("Guaji_magic_registered_default", ""));
			GUAJI_MAGIC_CAST_SEC = getInt(p, "Guaji_magic_cast_sec", GUAJI_MAGIC_CAST_SEC);
			GUAJI_MAGIC_CAST_SEC_MAP = parseIntIntMap(p.getProperty("Guaji_magic_cast_sec_map", ""));

			_log.info("Loaded COM_掛機_整合.properties (AFK integrated).");
		} catch (Exception e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			_log.warning("Use default guaji settings.");
		} finally {
			if (is != null) try { is.close(); } catch (Exception ignored) {}
		}
	}

	private static boolean getBoolean(final Properties p, final String key, final boolean def) {
		final String v = p.getProperty(key);
		if (v == null) return def;
		return "true".equalsIgnoreCase(v.trim()) || "1".equals(v.trim());
	}

	private static int getInt(final Properties p, final String key, final int def) {
		try {
			return Integer.parseInt(p.getProperty(key).trim());
		} catch (Exception e) {
			return def;
		}
	}

	private static Set<Integer> parseIntSet(final String csv) {
		final Set<Integer> s = new HashSet<Integer>();
		if (csv == null || csv.trim().isEmpty()) return s;
		for (String t : csv.split(",")) {
			try { s.add(Integer.parseInt(t.trim())); } catch (Exception ignored) {}
		}
		return s;
	}

	private static LinkedHashSet<Integer> parseIntSetOrdered(final String csv) {
		final LinkedHashSet<Integer> s = new LinkedHashSet<Integer>();
		if (csv == null || csv.trim().isEmpty()) return s;
		for (String t : csv.split(",")) {
			try { s.add(Integer.parseInt(t.trim())); } catch (Exception ignored) {}
		}
		return s;
	}

	private static Map<Integer, Integer> parseIntIntMap(final String csv) {
		final Map<Integer, Integer> m = new HashMap<Integer, Integer>();
		if (csv == null || csv.trim().isEmpty()) return m;
		for (String t : csv.split(",")) {
			final String[] kv = t.split(":");
			if (kv.length != 2) continue;
			try {
				final int k = Integer.parseInt(kv[0].trim());
				final int v = Integer.parseInt(kv[1].trim());
				m.put(k, v);
			} catch (Exception ignored) {}
		}
		return m;
	}
}