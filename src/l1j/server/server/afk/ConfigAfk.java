
package l1j.server.server.afk;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * AFK 專用設定載入（獨立，不依賴全域 Config）。
 * - 來源：config/afk.properties
 * - 支援白名單：Guaji_magic_allowed=1,2,3
 */
public final class ConfigAfk {
    private static final String PATH = "config/afk.properties";
    private static volatile long _lastLoaded = 0L;
    private static volatile Set<Integer> _GUAJI_MAGIC_ALLOWED = new HashSet<Integer>();

    static { load(); }

    /** 取得允許登錄的攻擊技能白名單（不可修改的快照）。 */
    public static Set<Integer> GUAJI_MAGIC_ALLOWED() {
        // 回傳不可變副本，避免外部修改
        return new java.util.HashSet<Integer>(_GUAJI_MAGIC_ALLOWED);
    }

    /** 供外部存取（保留舊呼叫風格 ConfigAfk.GUAJI_MAGIC_ALLOWED） */
    public static final Set<Integer> GUAJI_MAGIC_ALLOWED = new java.util.HashSet<Integer>();

    /** 外部可主動觸發重新載入（或每次進入時呼叫 reloadIfNeeded()）。 */
    public static synchronized void load() {
        Properties p = new Properties();
        File f = new File(PATH);
        if (f.exists()) {
            try (FileInputStream fis = new FileInputStream(f)) {
                p.load(fis);
            } catch (IOException ignored) {}
            _lastLoaded = f.lastModified();
        }
        // 解析白名單
        Set<Integer> tmp = new HashSet<Integer>();
        String v = p.getProperty("Guaji_magic_allowed", "").trim();
        if (!v.isEmpty()) {
            for (String s : v.split(",")) {
                try { tmp.add(Integer.parseInt(s.trim())); } catch (Exception ignored) {}
            }
        }
        _GUAJI_MAGIC_ALLOWED = tmp;
        synchronized (GUAJI_MAGIC_ALLOWED) {
            GUAJI_MAGIC_ALLOWED.clear();
            GUAJI_MAGIC_ALLOWED.addAll(tmp);
        }
    }

    /** 若檔案有變動則重載。 */
    public static void reloadIfNeeded() {
        File f = new File(PATH);
        if (f.exists()) {
            long lm = f.lastModified();
            if (lm > _lastLoaded) {
                load();
            }
        }
    }

    private ConfigAfk() {}
}
