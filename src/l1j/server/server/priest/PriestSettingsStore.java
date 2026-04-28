package l1j.server.server.priest;

import java.util.concurrent.ConcurrentHashMap;

public final class PriestSettingsStore {

    public static final class Settings {
        public int healThreshold;      // 10..100（以10為單位）
        public boolean autoSupport;    // 開/關
        public int mp;                 // 目前MP
        public long lastMpTickMillis;  // 上次回魔時間（毫秒）
        public int regenPer8sec;       // 每8秒回魔值（依祭司等級）
        public Settings(int healThreshold, boolean autoSupport, int mp, int regenPer8sec) {
            this.healThreshold = healThreshold;
            this.autoSupport = autoSupport;
            this.mp = Math.max(0, mp);
            this.regenPer8sec = Math.max(0, regenPer8sec);
            this.lastMpTickMillis = System.currentTimeMillis();
        }
    }

    private static final ConcurrentHashMap<Integer, Settings> MAP = new ConcurrentHashMap<>();
    private static final PriestSettingsDAO DAO = new PriestSettingsDAO();

    /** 初次載入：若DB無資料，用預設值建立；mpInit與regen由呼叫者依NPC等級提供。 */
    public static Settings ensureLoaded(int charId, int mpInit, int regenPer8sec) {
        Settings s = MAP.get(charId);
        if (s != null) return s;
        PriestSettingsDAO.Row row = DAO.load(charId);
        if (row != null) {
            s = new Settings(snap(row.threshold), row.autoSupport, Math.max(0, row.mp), regenPer8sec);
        } else {
            s = new Settings(50, false, Math.max(0, mpInit), regenPer8sec);
            DAO.upsert(charId, s.healThreshold, s.autoSupport, s.mp);
        }
        MAP.put(charId, s);
        return s;
    }

    private static int snap(int v) {
        if (v < 10) v = 10;
        if (v > 100) v = 100;
        return (v / 10) * 10;
    }

    public static Settings get(int charId) {
        return MAP.get(charId);
    }

    public static void setThreshold(int charId, int value) {
        Settings s = get(charId);
        if (s == null) return;
        s.healThreshold = snap(value);
        DAO.upsert(charId, s.healThreshold, s.autoSupport, s.mp);
    }

    public static void deltaThreshold(int charId, int delta) {
        Settings s = get(charId);
        if (s == null) return;
        setThreshold(charId, s.healThreshold + delta);
    }

    public static void setAutoSupport(int charId, boolean on) {
        Settings s = get(charId);
        if (s == null) return;
        s.autoSupport = on;
        DAO.upsert(charId, s.healThreshold, s.autoSupport, s.mp);
    }

    /** 扣魔（施法時呼叫） */
    public static int consumeMp(int charId, int amount) {
        Settings s = get(charId);
        if (s == null) return 0;
        if (amount <= 0) return s.mp;
        s.mp = Math.max(0, s.mp - amount);
        DAO.upsert(charId, s.healThreshold, s.autoSupport, s.mp);
        return s.mp;
    }

    /** 回魔（以時間差計算），每8秒回 s.regenPer8sec。 */
    public static int regenTick(int charId) {
        Settings s = get(charId);
        if (s == null) return 0;
        long now = System.currentTimeMillis();
        long dt = now - s.lastMpTickMillis;
        if (dt < 1000) return s.mp;
        int ticks = (int)(dt / 8000L);
        if (ticks > 0 && s.regenPer8sec > 0) {
            long add = (long) ticks * (long) s.regenPer8sec;
            s.mp = (int)Math.min(999999, (long)s.mp + add);
            s.lastMpTickMillis += ticks * 8000L;
            DAO.upsert(charId, s.healThreshold, s.autoSupport, s.mp);
        } else if (ticks > 0) {
            s.lastMpTickMillis += ticks * 8000L;
        }
        return s.mp;
    }
}
