package l1j.server.server.model;

import java.util.concurrent.ConcurrentHashMap;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * BOSS 重生查詢 - 分頁狀態暫存（依玩家 ObjId）。
 * <p>
 * 只存「目前頁碼」，避免在 C_ItemUSe / C_NPCAction 內塞大量狀態。
 */
public final class BossStatusRegistry {

    private static final ConcurrentHashMap<Integer, Integer> PAGE = new ConcurrentHashMap<Integer, Integer>();

    private BossStatusRegistry() {
    }

    public static int getPage(L1PcInstance pc) {
        if (pc == null) {
            return 0;
        }
        final Integer p = PAGE.get(Integer.valueOf(pc.getId()));
        return (p == null) ? 0 : p.intValue();
    }

    public static void setPage(L1PcInstance pc, int page) {
        if (pc == null) {
            return;
        }
        if (page < 0) {
            page = 0;
        }
        PAGE.put(Integer.valueOf(pc.getId()), Integer.valueOf(page));
    }
}
