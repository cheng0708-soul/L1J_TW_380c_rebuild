// 覆蓋/新增：src/l1j/server/server/afk/AfkSettings.java
// 讀取職業專屬技能白名單。格式：Afk_class_skill_whitelist.<ClassKey>=逗號分隔的 skillId
package l1j.server.server.afk;

import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public final class AfkSettings {
	private static volatile boolean _loaded;
	private static volatile boolean _DEBUG;
	private static volatile Map<String, Set<Integer>> _CLASS_WL = Collections.emptyMap();

	private AfkSettings() {}

	private static void ensure() {
		if (_loaded) return;
		synchronized (AfkSettings.class) {
			if (_loaded) return;
			Properties p = new Properties();
			try (InputStreamReader r = new InputStreamReader(new FileInputStream("./config/afk.properties"), StandardCharsets.UTF_8)) {
				p.load(r);
			} catch (Exception ignore) {}
			_DEBUG = "true".equalsIgnoreCase(p.getProperty("Afk_debug", "false"));

			// 載入職業白名單（只讀取我們常見的職業鍵；沒有的就空集合）
			String[] keys = new String[]{"Elf","Knight","DragonKnight","Wizard","DarkElf","Crown","Illusionist","Warrior"};
			HashMap<String, Set<Integer>> map = new HashMap<String, Set<Integer>>();
			for (String k : keys) {
				String prop = p.getProperty("Afk_class_skill_whitelist." + k, "");
				HashSet<Integer> set = new HashSet<Integer>();
				if (prop != null && prop.trim().length() > 0) {
					for (String s : prop.split(",")) {
						try {
							int id = Integer.parseInt(s.trim());
							if (id > 0) set.add(id);
						} catch (Exception e) {}
					}
				}
				map.put(k, Collections.unmodifiableSet(set));
			}
			_CLASS_WL = Collections.unmodifiableMap(map);

			_loaded = true;
		}
	}

	public static boolean DEBUG() { ensure(); return _DEBUG; }
	public static Set<Integer> classWhitelist(String classKey) {
		ensure();
		Set<Integer> s = _CLASS_WL.get(classKey);
		return s == null ? Collections.<Integer>emptySet() : s;
	}
}
