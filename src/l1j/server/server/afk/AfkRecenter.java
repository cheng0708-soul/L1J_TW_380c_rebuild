package l1j.server.server.afk;

import java.util.List;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_OwnCharPack;

/**
 * 以「客戶端重繪」達成置中：
 * - 僅對自身送 S_OwnCharPack（不移動、不廣播）→ 客戶端鏡頭會回到玩家
 * - 立即對當前畫面內所有物件呼叫 onPerceive(pc)，避免 NPC/怪物消失
 * - 不做 teleport、不改座標、不改 passable → 不會「亂飄」
 */
public final class AfkRecenter {
    private AfkRecenter() {}

    public static void strongRecenter(final L1PcInstance pc) {
        if (pc == null) return;
        try {
            // 1) 讓客戶端以玩家為中心重新繪製
            pc.sendPackets(new S_OwnCharPack(pc));
        } catch (Throwable ignore) {}

        try {
            // 2) 立即補發畫面內物件，避免空畫面
            List<L1Object> list = L1World.getInstance().getVisibleObjects(pc, 20);
            for (L1Object o : list) {
                try { o.onPerceive(pc); } catch (Throwable ignored) {}
            }
        } catch (Throwable ignore) {}
    }
}