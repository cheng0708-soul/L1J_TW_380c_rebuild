package l1j.server.server.afk;

import java.util.concurrent.ConcurrentHashMap;
import l1j.server.server.model.Instance.L1MonsterInstance;
import l1j.server.server.model.Instance.L1PcInstance;

public final class AfkEventHub {
    private AfkEventHub() {}

    private static final ConcurrentHashMap<Integer, AfkAiController> MAP = new ConcurrentHashMap<>();

    public static void register(L1PcInstance pc, AfkAiController ai) {
        if (pc != null && ai != null) MAP.put(pc.getId(), ai);
    }
    public static void unregister(L1PcInstance pc) {
        if (pc != null) MAP.remove(pc.getId());
    }

    public static void onMobHidden(L1MonsterInstance mob) {
        if (mob == null) return;
        final int mobId = mob.getId();
        for (AfkAiController ai : MAP.values()) {
            try { ai.onMobHidden(mobId); } catch (Throwable ignore) {}
        }
    }
}
