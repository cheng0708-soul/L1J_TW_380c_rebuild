/**
 * 攤位（個人商店 NPC）專用的物品清單封包。
 *
 * 這個封包模仿倉庫取出清單的格式（Opcodes.S_OPCODE_SHOWRETRIEVELIST），
 * 讓客戶端以內建列表介面顯示物品資料（名稱、圖示、數量等）。
 *
 * 注意：此封包僅用來顯示清單，並不提供直接購買/收購功能。
 */
package l1j.server.server.serverpackets;

import java.io.IOException;
import java.util.List;

import l1j.server.server.Opcodes;

public class S_PShopItemList extends ServerBasePacket {
    /**
     * 物品條目結構，用於攜帶攤位物品顯示所需的資訊。
     */
    public static class Entry {
        public int objectId;
        public int useType;
        public int gfxId;
        public int bless;
        public int count;
        public int identified;
        public String name;
    }

    /**
     * 建構攤位物品清單封包。
     *
     * @param objId 對話對象的物件ID（通常為攤位 NPC 的 objectId）
     * @param items 欲顯示的物品條目列表
     */
    public S_PShopItemList(int objId, List<Entry> items) {
        // Opcode for retrieval list (倉庫取出清單)
        writeC(Opcodes.S_OPCODE_SHOWRETRIEVELIST);
        // object id: 讓客戶端知道與哪個 NPC 對話
        writeD(objId);
        // 物品數量
        int size = (items == null) ? 0 : items.size();
        writeH(size);
        // 類型：3 表示個人倉庫、血盟倉庫以外的自定義清單
        writeC(3);
        if (size > 0) {
            for (Entry e : items) {
                if (e == null) continue;
                writeD(e.objectId);
                writeC(e.useType);
                writeH(e.gfxId);
                writeC(e.bless);
                writeD(e.count);
                writeC(e.identified);
                writeS(e.name);
            }
        }
        // 清單顯示費用：此處固定為 0，無須支付費用
        writeD(0);
    }

    @Override
    public byte[] getContent() throws IOException {
        return getBytes();
    }
}