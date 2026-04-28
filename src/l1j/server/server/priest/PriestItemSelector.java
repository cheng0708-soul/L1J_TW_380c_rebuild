
package l1j.server.server.priest;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;

/** 根據 npc 名稱判斷階級，選出玩家背包中相同階級的祭司道具。 */
public final class PriestItemSelector {
    private PriestItemSelector() {}

    public static L1ItemInstance findSameTier(L1PcInstance pc, L1NpcInstance npc) {
        if (pc == null || npc == null) return null;
        String name = null;
        try { name = npc.getNpcTemplate().get_name(); } catch (Throwable ignore) {}
        if (name == null || name.isEmpty()) {
            try { name = npc.getName(); } catch (Throwable ignore) {}
        }
        int itemId = tierToItemId(name);
        if (itemId == 0) return null;
        for (L1ItemInstance it : pc.getInventory().getItems()) {
            if (it.getItem().getItemId() == itemId) return it;
        }
        return null;
    }

    private static int tierToItemId(String name) {
        if (name == null) return 0;
        if (name.contains("神話")) return 240127;
        if (name.contains("頂級")) return 240126;
        if (name.contains("高階")) return 240125;
        if (name.contains("中階")) return 240124;
        if (name.contains("低階")) return 240123;
        return 0;
    }
}
