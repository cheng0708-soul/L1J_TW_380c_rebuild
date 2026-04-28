package l1j.server.server.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.Opcodes;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.model.L1PcInventory;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PShopNpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.shop.PShopAnnouncer;
import l1j.server.server.model.shop.PShopCurrencyUtil;
// PShopManager remains imported for legacy references but is unused in our custom stall logic.
// import l1j.server.server.model.shop.PShopManager;
import l1j.server.server.model.shop.PShopSpawnUtil;
import l1j.server.server.serverpackets.S_PShopItemList;
// 為動態攤位封包內部類引用所需
import l1j.server.server.serverpackets.ServerBasePacket;
import l1j.server.server.templates.L1Item;
import l1j.server.server.templates.L1PrivateShopBuyList;
import l1j.server.server.templates.L1PrivateShopSellList;

public class C_PShopBypass {
    // 全域 PSHOP TRACE 靜音開關（僅本檔可控訊息）
    private static final boolean SUPPRESS_PSHOP_TRACE = true;

    private static int encodeAttr(int kind, int level) {
        if (kind <= 0 && level <= 0) {
            return 0;
        }
        return ((kind & 0xFF) << 8) | (level & 0xFF);
    }

    private static int decodeAttrKind(int encoded) {
        if (encoded <= 0) {
            return 0;
        }
        return (encoded >> 8) & 0xFF;
    }

    private static int decodeAttrLevel(int encoded) {
        if (encoded <= 0) {
            return 0;
        }
        return encoded & 0xFF;
    }

    /**
     * 建立用於顯示名稱的暫時物品，套用強化 / 屬性 / 祝福後，使用 getViewName()
     * 來產生與實際物品相同的顯示字串。
     */
    private static String buildPShopDisplayName(int itemId, int bless, int enchant, int attrEncoded, int count) {
        L1Item template = null;
        try {
            template = ItemTable.getInstance().getTemplate(itemId);
        } catch (Throwable ignore) {}
        if (template == null) {
            return String.valueOf(itemId);
        }
        try {
            L1ItemInstance tmp = new L1ItemInstance();
            tmp.setItem(template);
            tmp.setBless(bless);
            tmp.setEnchantLevel(enchant);
            int kind = decodeAttrKind(attrEncoded);
            int level = decodeAttrLevel(attrEncoded);
            if (kind > 0) {
                tmp.setAttrEnchantKind(kind);
            }
            if (level > 0) {
                tmp.setAttrEnchantLevel(level);
            }
            tmp.setIdentified(true);
            return tmp.getNumberedViewName(count);
        } catch (Throwable t) {
            StringBuilder sb = new StringBuilder();
            if (enchant != 0) {
                if (enchant > 0) {
                    sb.append("+" + enchant + " ");
                } else {
                    sb.append(enchant + " ");
                }
            }
            sb.append(template.getName());
            return sb.toString();
        }
    }
    private static void sendMsg(l1j.server.server.model.Instance.L1PcInstance pc, String text) {
        if (text != null && text.startsWith("PSHOP_") && SUPPRESS_PSHOP_TRACE) {
            return; // 避免 PSHOP_TRACE / PSHOP_* 類訊息噴出
        }
        try { pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage(text)); } catch (Throwable ignore) {}
    }


    /**
     * 存放每個攤位 NPC 的發話器。開攤時會創建並啟動對應的 PShopAnnouncer，
     * 收攤時須將其停止並移除。key 為 npcObjId。
     */
    private static final Map<Integer, PShopAnnouncer> _announcers = new ConcurrentHashMap<>();

    /*
     * 以下為動態商店所使用的暫存資料結構。
     * _pendingSellOrders：存放每位買家可購買攤位商品的清單，key 為玩家 ID。
     * _pendingBuyOrders：存放每位買家可賣給攤位的清單，key 為玩家 ID。
     * PShopSellOrder 與 PShopBuyOrder 封裝了各列項目及其索引，便於後續結算。
     */
    private static final Map<Integer, PShopSellOrder> _pendingSellOrders = new ConcurrentHashMap<>();
    private static final Map<Integer, PShopBuyOrder>  _pendingBuyOrders  = new ConcurrentHashMap<>();

    /**
     * 攤位販售條目：包含 pshop_items.id(rowId)、物品 ID、販售單價、可售數量及物品屬性。
     */
    public static class PShopSellItem {
        int index;      // 送往客戶端的索引；對應 pshop_items 的 id
        int itemId;     // 物品 ID
        int price;      // 單件售價
        int count;      // 可售數量
        int bless;      // 祝福屬性
        int enchant;    // 強化等級
        int attr;       // 屬性等級
        String name;    // 顯示名稱（含強化等級）
        int gfxId;      // 圖示 ID
        byte[] status;  // 物品狀態 bytes
    }

    /**
     * 買家在攤位購買時的清單，包含多個 PShopSellItem。
     */
    public static class PShopSellOrder {
        java.util.List<PShopSellItem> items = new java.util.ArrayList<>();
    }

    /**
     * 攤位收購條目：包含 pshop_items.id(rowId)、物品 ID、收購單價、可收購數量。
     */
    public static class PShopBuyItem {
        /**
         * 資料庫中 pshop_items.id (rowId)。
         */
        int index;
        /**
         * 玩家背包中物品的 objectId，用於從背包取得具體實例並刪除。
         */
        int objId;
        /**
         * 物品 ID (模板 ID)。
         */
        int itemId;
        /**
         * 收購單價。
         */
        int price;
        /**
         * 可收購的剩餘數量。
         */
        int count;
        /**
         * 收購條目的祝福狀態。
         */
        int bless;
        /**
         * 收購條目的強化等級。
         */
        int enchant;
        /**
         * 收購條目的屬性強化等級（等級，不含種類）。
         */
        int attr;
    }

    /**
     * 買家在攤位販售時的清單，包含多個 PShopBuyItem。
     */
    public static class PShopBuyOrder {
        java.util.List<PShopBuyItem> items = new java.util.ArrayList<>();
    }

    // ===== 動態攤位清單封包類 =====
    /**
     * 攤位販售清單封包，模仿一般商店的 S_ShopSellList。用於顯示攤位可販售的物品，
     * 讓客戶端以商店介面選購。條目包含索引、圖示 ID、價格、名稱與狀態 bytes。
     */
    public static class S_PShopSellList extends ServerBasePacket {
        public static class Entry {
            public int index;
            public int gfxId;
            public int price;
            public String name;
            public byte[] status;
        }

        public S_PShopSellList(int objId, java.util.List<Entry> list) {
            writeC(Opcodes.S_OPCODE_SHOWSHOPBUYLIST);
            writeD(objId);
            if (list == null) {
                writeH(0);
            } else {
                writeH(list.size());
                for (Entry e : list) {
                    writeD(e.index);
                    writeH(e.gfxId);
                    writeD(e.price);
                    writeS(e.name);
                    if (e.status != null) {
                        writeC(e.status.length);
                        for (byte b : e.status) {
                            writeC(b);
                        }
                    } else {
                        writeC(0);
                    }
                }
            }
            // 固定使用 0x0007 表示以金幣/元寶為單位，由客戶端自行判斷顯示
            writeH(0x0007);
        }

        @Override
        public String getType() {
            return "[S] S_PShopSellList";
        }
    
        @Override
        public byte[] getContent() {
            return getBytes();
        }
}


    /**
     * 攤位收購清單封包，模仿一般商店的 S_ShopBuyList。用於顯示攤位願意收購的物品，
     * 客戶端選擇要賣的物品與數量後以一般商店流程結算。
     */
    public static class S_PShopBuyList extends ServerBasePacket {        public static class Entry { public int objId; public int price; }

        public S_PShopBuyList(int objId, java.util.List<Entry> list) {
            writeC(Opcodes.S_OPCODE_SHOWSHOPSELLLIST);
            writeD(objId);
            if (list == null) {
                writeH(0);
            } else {
                writeH(list.size());
                for (Entry e : list) {
                    writeD(e.objId);
                    writeD(e.price);
                }
            }
            writeH(0x0007);
        }

        @Override
        public String getType() {
            return "[S] S_PShopBuyList";
        }
    
        @Override
        public byte[] getContent() {
            return getBytes();
        }
}


    // ===== 設定、取得、清除暫存清單的方法 =====
    public static void setPendingSellOrder(int pcId, PShopSellOrder order) {
        if (order == null) return;
        _pendingSellOrders.put(pcId, order);
    }

    public static PShopSellOrder getPendingSellOrder(int pcId) {
        return _pendingSellOrders.get(pcId);
    }

    public static void clearPendingSellOrder(int pcId) {
        _pendingSellOrders.remove(pcId);
    }

    public static void setPendingBuyOrder(int pcId, PShopBuyOrder order) {
        if (order == null) return;
        _pendingBuyOrders.put(pcId, order);
    }

    public static PShopBuyOrder getPendingBuyOrder(int pcId) {
        return _pendingBuyOrders.get(pcId);
    }

    public static void clearPendingBuyOrder(int pcId) {
        _pendingBuyOrders.remove(pcId);
    }

    /**
     * 處理玩家從攤位購買商品的結果。將依照買家選擇的項目與數量進行扣款、發貨及更新資料庫。
     *
     * @param pc      買家
     * @param npc     攤位 NPC
     * @param order   買家當前的販售清單
     * @param indices 選擇的索引陣列，對應於 order.items 的順序
     * @param counts  對應索引的購買數量
     */
    public static void processPShopBuy(L1PcInstance pc, L1PShopNpcInstance npc, PShopSellOrder order, int[] indices, int[] counts) {
        if (pc == null || npc == null || order == null || indices == null || counts == null) {
            return;
        }
        int npcObjId = npc.getId();
        // 先取得攤位資訊：stall_id、mode_item_id、攤主帳號與角色
        int stallId = 0;
        int modeItemId = 0;
        String ownerAccount = "";
        int ownerCharId = 0;
        long escrow = 0;
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT stall_id,mode_item_id,account_name,char_id,escrow_amount FROM pshop_stalls WHERE npc_objid=? AND status=1")) {
            ps.setInt(1, npcObjId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stallId = rs.getInt("stall_id");
                    modeItemId = rs.getInt("mode_item_id");
                    ownerAccount = rs.getString("account_name");
                    ownerCharId = rs.getInt("char_id");
                    escrow = rs.getLong("escrow_amount");
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        if (stallId == 0 || modeItemId == 0) {
            sendMsg(pc, "交易失敗：攤位不存在。");
            return;
        }
        // 計算所選商品的總價與檢查數量
        long totalCost = 0;
        java.util.List<PShopSellItem> items = order.items;
        java.util.Map<PShopSellItem, Integer> buyMap = new java.util.HashMap<>();
        for (int i = 0; i < indices.length && i < counts.length; i++) {
            int idx = indices[i];
            int qty = counts[i];
            if (idx < 0 || idx >= items.size()) continue;
            PShopSellItem it = items.get(idx);
            if (it == null) continue;
            int available = it.count;
            if (available <= 0) continue;
            int buyQty = qty;
            if (buyQty > available) buyQty = available;
            if (buyQty <= 0) continue;
            long cost = (long) it.price * (long) buyQty;
            totalCost += cost;
            buyMap.put(it, buyMap.getOrDefault(it, 0) + buyQty);
        }
        if (buyMap.isEmpty()) {
            sendMsg(pc, "沒有選擇任何商品。");
            return;
        }
        if (totalCost <= 0) {
            sendMsg(pc, "交易金額為零。");
            return;
        }
        // 檢查玩家持有的貨幣是否足夠
        if (!pc.getInventory().checkItem(modeItemId, (int) totalCost)) {
            String currencyName = PShopCurrencyUtil.currencyName(modeItemId);
            sendMsg(pc, "您身上的" + currencyName + "不足，無法購買。");
            return;
        }
        // 扣除玩家貨幣
        pc.getInventory().consumeItem(modeItemId, (int) totalCost);
        // 將金額記入攤主的 holdings
        try (Connection con = L1DatabaseFactory.getInstance().getConnection()) {
            // 更新或插入 holdings
            for (java.util.Map.Entry<PShopSellItem, Integer> entry : buyMap.entrySet()) {
                PShopSellItem it = entry.getKey();
                int buyQty = entry.getValue();
                // 1. 從 pshop_items 減少販售數量
                try (PreparedStatement ps = con.prepareStatement(
                        "UPDATE pshop_items SET sell_count = GREATEST(sell_count - ?,0), "
                      + "owner_item_objid = CASE WHEN sell_count - ? <= 0 THEN 0 ELSE owner_item_objid END "
                      + "WHERE id=?")) {
                    ps.setInt(1, buyQty);
                    ps.setInt(2, buyQty);
                    ps.setInt(3, it.index);
                    ps.executeUpdate();
                }
                // 2. 給玩家物品（按件給予，設定屬性）
                for (int x = 0; x < buyQty; x++) {
                    L1ItemInstance newItem = pc.getInventory().storeItem(it.itemId, 1);
                    if (newItem != null) {
                        try {
                            newItem.setBless(it.bless);
                            newItem.setEnchantLevel(it.enchant);
                            int attrKind = decodeAttrKind(it.attr);
                            int attrLevel = decodeAttrLevel(it.attr);
                            if (attrKind > 0) {
                                newItem.setAttrEnchantKind(attrKind);
                            }
                            if (attrLevel > 0) {
                                newItem.setAttrEnchantLevel(attrLevel);
                            }
                            newItem.setIdentified(true);
                            pc.getInventory().updateItem(newItem, L1PcInventory.COL_IS_ID);
                        } catch (Throwable ignore) {}
                    }
                }
                // 3. 若此販售條目已經賣完，將該筆 pshop_items 標記為失效，
                //    避免攤主收攤或查看已獲得物品時又被當成尚有存貨返還。
                if (buyQty >= it.count) {
                    try (PreparedStatement ps2 = con.prepareStatement(
                            "UPDATE pshop_items SET item_id=0, sell_count=0, owner_item_objid=0 WHERE id=?")) {
                        ps2.setInt(1, it.index);
                        ps2.executeUpdate();
                    } catch (Throwable ignore) {}
                }
            }
            // 3. 計算攤主應得的金額，累加於 holdings 中
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO pshop_holdings(account_name,char_id,mode_item_id,item_id,item_count) " +
                    "VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE item_count=item_count+?"
            );
            ps.setString(1, ownerAccount);
            ps.setInt(2, ownerCharId);
            ps.setInt(3, modeItemId);
            ps.setInt(4, modeItemId);
            ps.setInt(5, (int) totalCost);
            ps.setInt(6, (int) totalCost);
            ps.executeUpdate();
            ps.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        // 發送簡單提示訊息
        sendMsg(pc, "購買完成，共花費 " + totalCost + ".");
    }

    /**
     * 處理玩家對攤位販售物品的結果。依照玩家選擇的索引與數量，扣除攤位保證金並回饋對應貨幣、收走玩家物品。
     *
     * @param pc      買家（實際上賣給攤位）
     * @param npc     攤位 NPC
     * @param order   買家當前的收購清單
     * @param indices 選擇的索引陣列，對應於 order.items 的順序
     * @param counts  對應索引的販售數量
     */
    public static void processPShopSell(L1PcInstance pc, L1PShopNpcInstance npc, PShopBuyOrder order, int[] indices, int[] counts) throws Exception {
        if (pc == null || npc == null || order == null || indices == null || counts == null) {
            return;
        }
        int npcObjId = npc.getId();

        // 找到攤位 / 幣別
        int stallId = 0;
        int modeItemId = 40308;
        try (java.sql.Connection con = l1j.server.L1DatabaseFactory.getInstance().getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(
                     "SELECT stall_id,mode_item_id FROM pshop_stalls WHERE npc_objid=? AND status=1")) {
            ps.setInt(1, npcObjId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stallId = rs.getInt("stall_id");
                    try { modeItemId = rs.getInt("mode_item_id"); } catch (Throwable ignore) {}
                }
            }
        }
        if (stallId == 0) {
            pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("攤位不存在或已關閉。"));
            return;
        }

        // 算要賣的每一列
        java.util.LinkedHashMap<PShopBuyItem, Integer> sellMap = new java.util.LinkedHashMap<>();
        long total = 0;
        
        // 客戶端在 SHOWSHOPSELLLIST 介面回傳的是「物品物件ID(objId)」與數量，非陣列索引
        // 先建立 objId -> 條目 的對照表
        java.util.Map<Integer, PShopBuyItem> byObj = new java.util.HashMap<>();
        for (PShopBuyItem it : order.items) {
            byObj.put(it.objId, it);
        }
        for (int iIdx = 0; iIdx < indices.length && iIdx < counts.length; iIdx++) {
            int objIdSel = indices[iIdx];
            int want = counts[iIdx];
            if (want <= 0) continue;
            PShopBuyItem bi = byObj.get(objIdSel);
            if (bi == null) {
                // 安全起見：若未找到（例如堆疊品只顯示其中一個 objId），嘗試以 itemId 比對第一筆
                for (PShopBuyItem cand : order.items) {
                    if (cand.itemId == objIdSel) { // 幾乎不會發生，預防性處理
                        bi = cand; break;
                    }
                }
            }
            if (bi == null) continue;
            int allow = Math.min(want, Math.max(1, bi.count));
            if (allow <= 0) continue;
            sellMap.put(bi, allow);
            total += (long) bi.price * allow;
        }

        if (sellMap.isEmpty()) {
            pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("無商品可收購。"));
            return;
        }

        // 檢查並扣攤位託管幣
        long escrow = 0;
        try (java.sql.Connection con = l1j.server.L1DatabaseFactory.getInstance().getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement("SELECT escrow_amount FROM pshop_stalls WHERE stall_id=? FOR UPDATE")) {
            ps.setInt(1, stallId);
            try (java.sql.ResultSet rs = ps.executeQuery()) { if (rs.next()) escrow = rs.getLong(1); }
            if (escrow < total) {
                pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("商店保證金不足，無法收購。"));
                return;
            }
            try (java.sql.PreparedStatement upd = con.prepareStatement("UPDATE pshop_stalls SET escrow_amount=escrow_amount-? WHERE stall_id=?")) {
                upd.setLong(1, total);
                upd.setInt(2, stallId);
                upd.executeUpdate();
            }
        }

        // 付款給玩家
        pc.getInventory().storeItem(modeItemId, (int) total);

        // 逐列處理移物 & 託管 & 下修 buy_count
        for (java.util.Map.Entry<PShopBuyItem, Integer> ent : sellMap.entrySet()) {
            PShopBuyItem bi = ent.getKey();
            int sellQty = ent.getValue();

            l1j.server.server.templates.L1Item tpl = l1j.server.server.datatables.ItemTable.getInstance().getTemplate(bi.itemId);
            boolean stackable = tpl != null && tpl.isStackable();

            if (stackable) {
                // 堆疊：扣玩家數量 → 合併到 sell_count
                pc.getInventory().consumeItem(bi.itemId, sellQty);
                try (java.sql.Connection con = l1j.server.L1DatabaseFactory.getInstance().getConnection()) {
                    int updated = 0;
                    try (java.sql.PreparedStatement ps = con.prepareStatement(
                            "UPDATE pshop_items SET sell_count=sell_count+? WHERE stall_id=? AND item_id=? AND buy_price=0 AND sell_price=0")) {
                        ps.setInt(1, sellQty);
                        ps.setInt(2, stallId);
                        ps.setInt(3, bi.itemId);
                        updated = ps.executeUpdate();
                    }
                    if (updated == 0) {
                        try (java.sql.PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO pshop_items(stall_id,item_id,bless,enchant_lvl,attr_enchant," +
                            "sell_price,sell_count,buy_price,buy_count,owner_item_objid) VALUES(?,?,?,?,?, 0,?, 0,0, 0)")) {
                            ps.setInt(1, stallId);
                            ps.setInt(2, bi.itemId);
                            ps.setInt(3, 0);
                            ps.setInt(4, 0);
                            ps.setInt(5, 0);
                            ps.setInt(6, sellQty);
                            ps.executeUpdate();
                        }
                    }
                }
            } else {
                // 非堆疊：按 objId 逐顆移轉
                for (int c = 0; c < sellQty; c++) {
                    l1j.server.server.model.Instance.L1ItemInstance inst = pc.getInventory().getItem(bi.objId);
                    if (inst == null) break;
                    int ownerObj = inst.getId();
                    pc.getInventory().removeItem(inst, 1);
                    try (java.sql.Connection con = l1j.server.L1DatabaseFactory.getInstance().getConnection();
                         java.sql.PreparedStatement ps = con.prepareStatement(
                            "INSERT INTO pshop_items(stall_id,item_id,bless,enchant_lvl,attr_enchant," +
                            "sell_price,sell_count,buy_price,buy_count,owner_item_objid) VALUES(?,?,?,?,?, 0,1, 0,0, ?)")) {
                        ps.setInt(1, stallId);
                        ps.setInt(2, bi.itemId);
                        ps.setInt(3, inst.getBless());
                        ps.setInt(4, inst.getEnchantLevel());
                        int attrKind = 0;
                        int attrLevel = 0;
                        try {
                            attrKind = inst.getAttrEnchantKind();
                            attrLevel = inst.getAttrEnchantLevel();
                        } catch (Throwable ignoreAttr) {}
                        ps.setInt(5, encodeAttr(attrKind, attrLevel));
                        ps.setInt(6, ownerObj);
                        ps.executeUpdate();
                    }
                }
            }

            // 下修 buy_count（用該收購行的 id）
            try (java.sql.Connection con = l1j.server.L1DatabaseFactory.getInstance().getConnection();
                 java.sql.PreparedStatement ps = con.prepareStatement(
                    "UPDATE pshop_items SET buy_count=GREATEST(buy_count-?,0) WHERE id=?")) {
                ps.setInt(1, sellQty);
                ps.setInt(2, bi.index);
                ps.executeUpdate();
            }
        }

        pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("交易完成。"));



}
    public static void handle(L1PcInstance pc, String cmd) {
        if (pc == null || cmd == null) {
            return;
        }
        cmd = cmd.trim();
        try {
            // ===== 玩家在個人商店UI按OK後選幣別 =====
            if (cmd.equalsIgnoreCase("pshop_mode coin")) {
                createStallFromCurrentShop(pc, 40308); // 金幣
                return;
            } else if (cmd.equalsIgnoreCase("pshop_mode yuan")) {
                createStallFromCurrentShop(pc, 240107); // 元寶
                return;
            }

            // ===== 攤主三個選項 =====
            if (cmd.startsWith("pshop_owner list") || cmd.startsWith("shop_owner list")) {
                showOwnerList(pc);
            } else if (cmd.startsWith("pshop_owner escrow") || cmd.startsWith("shop_owner escrow")) {
                showOwnerEscrow(pc);
            } else if (cmd.startsWith("pshop_owner close") || cmd.startsWith("shop_owner close")) {
                closeShop(pc);
            // ===== 一般玩家查看攤位 =====
            } else if (cmd.startsWith("pshop_shop buy")) {
                showBuyerSaleList(pc);
            } else if (cmd.startsWith("pshop_shop sell")) {
                // 非攤主按「賣」時，直接顯示收購清單 UI，不再僅以系統訊息列出
                try {
                    showBuyerBuyList(pc);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    sendMsg(pc, "無法顯示收購清單。");
                }
            } else {
                sendMsg(pc, "PSHOP: 未知指令 " + cmd);
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendMsg(pc, "PSHOP 錯誤: " + e.getMessage());
        }
    }

    /**
     * 按 OK → 選幣別後真正跑的流程
     */
    private static void createStallFromCurrentShop(L1PcInstance pc, int modeItemId) throws Exception {
        // 檢查是否已有同帳號、同角色、同幣別的攤位在運作
        if (hasStallOfMode(pc, modeItemId)) {
            sendMsg(pc, "此幣別已經有攤位，請先收回原有的攤位。");
            return;
        }

        // 取得玩家在個人商店介面設置的販售與收購清單
        List<L1PrivateShopSellList> sellList = pc.getSellList();
        List<L1PrivateShopBuyList>  buyList  = pc.getBuyList();

        // 讀取玩家輸入的商店敘述並轉為字串；若無字串則為空
        String desc = "";
        try {
            byte[] chatBytes = pc.getShopChat();
            if (chatBytes != null) {
                try {
                    desc = new String(chatBytes, "utf8");
                } catch (Exception ex) {
                    desc = new String(chatBytes);
                }
            }
        } catch (Throwable ignore) {}
        // 移除換行及尾端的問號符號，避免 NPC 顯示問號
        if (desc != null) {
            // 移除換行符號並替換所有問號為空白，避免問號出現在 NPC 對話中
            desc = desc.replace('\r', ' ').replace('\n', ' ');
            desc = desc.replace('?', ' ').replace('？', ' ').trim();
        }

        // 計算收購所需的保證金（buyPrice * buyCount 的總和）
        long needEscrow = 0;
        if (buyList != null) {
            for (L1PrivateShopBuyList b : buyList) {
                if (b == null) continue;
                needEscrow += (long) b.getBuyPrice() * (long) b.getBuyTotalCount();
            }
        }

        // 檢查玩家是否擁有足夠的貨幣，如不足則取消開店
        if (needEscrow > 0) {
            if (!pc.getInventory().checkItem(modeItemId, (int) needEscrow)) {
                String currencyName = (modeItemId == 240107 ? "元寶" : "金幣");
                sendMsg(pc, "身上" + currencyName + "不足，無法開店。");
                return;
            }
            // 扣除保證金
            pc.getInventory().consumeItem(modeItemId, (int) needEscrow);
        }

        // 使用 PShopSpawnUtil 生成互動式攤位 NPC
        L1PShopNpcInstance npc = PShopSpawnUtil.spawn(pc, 0, modeItemId, pc.getHeading());
        if (npc == null) {
            sendMsg(pc, "無法生成攤位NPC。");
            return;
        }

        // 設定 NPC 的顯示名稱：[幣別] + 玩家名稱 + "的商店"（名稱顯示於頭頂）
        String currencyName = PShopCurrencyUtil.currencyName(modeItemId);
        String shopName = "[" + currencyName + "]" + pc.getName() + "的商店";
        try {
            npc.setCustomName(shopName);
            // 將 nameId 設為自訂名稱，讓 S_NPCPack 在頭頂顯示
            npc.setNameId(shopName);
            npc.setTitle("");
        } catch (Throwable ignore) {}

        // 將攤位資訊寫入 DB，stall_desc 不寫入中文避免亂碼，escrow_amount 記錄保證金
        int stallId = insertStallRow(pc, npc.getId(), modeItemId, needEscrow, "");

        // 將玩家欲販售的物品托管進 DB，並從玩家背包移除
        if (sellList != null) {
            for (L1PrivateShopSellList s : sellList) {
                if (s == null) continue;
                int objId      = s.getItemObjectId();
                int totalCount = s.getSellTotalCount();
                int soldCount  = s.getSellCount();
                int remain     = totalCount - soldCount;
                if (remain <= 0) {
                    remain = totalCount;
                }
                int price      = s.getSellPrice();
                // 取得物品實例
                L1ItemInstance item = pc.getInventory().getItem(objId);
                if (item == null) continue;
                int itemId  = item.getItemId();
                int bless   = item.getBless();
                int enchant = item.getEnchantLevel();
                int attrKind = 0;
                int attrLevel = 0;
                try {
                    attrKind = item.getAttrEnchantKind();
                    attrLevel = item.getAttrEnchantLevel();
                } catch (Throwable ignore) {}
                int attr = encodeAttr(attrKind, attrLevel);
                int ownerObjId = item.getId();
                // 將此物品登記到 pshop_items 表中，包含祝福與強化屬性資訊（含屬性種類與等級）
                insertItemRow(stallId, itemId, price, remain, bless, enchant, attr, ownerObjId);
                // 從玩家背包移除
                pc.getInventory().removeItem(item, remain);
                // 不寫入 pshop_holdings。未售出的物品僅記錄在 pshop_items 中，
                // 方便收攤時返還；收購所得才寫入 pshop_holdings。
            }
        }

        // 將玩家欲收購的物品條件寫入 pshop_items 表
        if (buyList != null) {
            for (L1PrivateShopBuyList b : buyList) {
                if (b == null) continue;
                int objId = b.getItemObjectId();
                int total = b.getBuyTotalCount();
                int price = b.getBuyPrice();
                int itemId;
                L1ItemInstance invItem = pc.getInventory().getItem(objId);
                if (invItem != null) {
                    itemId = invItem.getItemId();
                } else {
                    itemId = objId;
                }
                insertBuyRowWithSample(pc, stallId, itemId, price, total);
            }
        }

        // 建立並啟動攤位公告器，每 12 秒說話一次
        String say = "[" + currencyName + "]" + (desc == null ? "" : desc.trim());
        PShopAnnouncer announcer = new PShopAnnouncer(npc, say);
        announcer.start();
        _announcers.put(npc.getId(), announcer);

        // 立即顯示 NPC 給玩家及附近其他人
        try {
            pc.sendPackets(new l1j.server.server.serverpackets.S_NPCPack(npc));
            npc.broadcastPacket(new l1j.server.server.serverpackets.S_NPCPack(npc));
        } catch (Throwable ignore) {}

        // 回饋玩家訊息
        sendMsg(pc, "已生成" + currencyName + "攤位：[" + currencyName + "]" + desc);
        return;
    }

    private static boolean hasStallOfMode(L1PcInstance pc, int modeItemId) throws Exception {
        // 檢查是否有同帳號、同角色、同幣別的活躍攤位。如果是「殭屍攤位」(NPC 不存在)，
        // 則自動將其中的物品、貨幣返還給玩家並清除資料庫。
        Connection con = L1DatabaseFactory.getInstance().getConnection();
        PreparedStatement ps = con.prepareStatement(
            "SELECT stall_id,npc_objid,escrow_amount FROM pshop_stalls WHERE account_name=? AND char_id=? AND mode_item_id=? AND status=1");
        ps.setString(1, pc.getAccountName());
        ps.setInt(2, pc.getId());
        ps.setInt(3, modeItemId);
        ResultSet rs = ps.executeQuery();
        boolean activeExist = false;
        while (rs.next()) {
            int stallId    = rs.getInt("stall_id");
            int npcObjId   = rs.getInt("npc_objid");
            long escrow    = rs.getLong("escrow_amount");
            // 判斷該攤位的 NPC 是否還存在於世界中
            boolean npcAlive = false;
            try {
                l1j.server.server.model.L1Object obj = l1j.server.server.model.L1World.getInstance().findObject(npcObjId);
                if (obj instanceof l1j.server.server.model.Instance.L1NpcInstance) {
                    npcAlive = true;
                }
            } catch (Throwable ignore) {}
            if (npcAlive) {
                activeExist = true;
            } else {
                // 這是一個殭屍攤位：自動返還物品與貨幣並清除資料庫
                try {
                    recoverGhostStall(pc, stallId, npcObjId, escrow, modeItemId);
                } catch (Throwable ignore) {}
            }
        }
        rs.close();
        ps.close();
        con.close();
        return activeExist;
    }

    /**
     * 將沒有 NPC 的攤位視為殭屍攤位，自動返還物品與貨幣並清除資料庫記錄。
     *
     * @param pc        攤主
     * @param stallId   攤位 ID
     * @param npcObjId  NPC 物件 ID
     * @param escrow    保證金
     * @param modeItemId 幣別 ID
     */
    private static void recoverGhostStall(L1PcInstance pc, int stallId, int npcObjId, long escrow, int modeItemId) throws Exception {
        // 步驟 1：返還已收購的物品 (pshop_holdings)
        Connection con = L1DatabaseFactory.getInstance().getConnection();
        PreparedStatement ps = con.prepareStatement(
            "SELECT item_id,item_count FROM pshop_holdings WHERE account_name=? AND char_id=? AND mode_item_id=?");
        ps.setString(1, pc.getAccountName());
        ps.setInt(2, pc.getId());
        ps.setInt(3, modeItemId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int itemId = rs.getInt("item_id");
            int count  = rs.getInt("item_count");
            // 直接返還到玩家背包 (不保留祝福/強化資訊，因 pshop_holdings 未存此資訊)
            pc.getInventory().storeItem(itemId, count);
        }
        rs.close();
        ps.close();
        // 刪除收購紀錄
        ps = con.prepareStatement(
            "DELETE FROM pshop_holdings WHERE account_name=? AND char_id=? AND mode_item_id=?");
        ps.setString(1, pc.getAccountName());
        ps.setInt(2, pc.getId());
        ps.setInt(3, modeItemId);
        ps.executeUpdate();
        ps.close();

        // 步驟 2：返還未售出的販售品，包含祝福/強化資訊
        ps = con.prepareStatement(
            "SELECT item_id,sell_count,bless,enchant_lvl,attr_enchant FROM pshop_items WHERE stall_id=? AND (sell_count>0 OR owner_item_objid>0)");
        ps.setInt(1, stallId);
        rs = ps.executeQuery();
        while (rs.next()) {
            int itemId  = rs.getInt("item_id");
            if (itemId <= 0) {
                // 已標記失效的紀錄，殭屍攤位自動回收時也不再返還
                continue;
            }
            int count   = rs.getInt("sell_count");
            int bless   = rs.getInt("bless");
            int enchant = rs.getInt("enchant_lvl");
            int encodedAttr = rs.getInt("attr_enchant");
            int attrKind = decodeAttrKind(encodedAttr);
            int attrLevel = decodeAttrLevel(encodedAttr);
            for (int i = 0; i < count; i++) {
                L1ItemInstance newItem = pc.getInventory().storeItem(itemId, 1);
                if (newItem != null) {
                    try {
                        newItem.setBless(bless);
                        newItem.setEnchantLevel(enchant);
                        if (attrKind > 0) {
                            newItem.setAttrEnchantKind(attrKind);
                        }
                        if (attrLevel > 0) {
                            newItem.setAttrEnchantLevel(attrLevel);
                        }
                        newItem.setIdentified(true);
                        pc.getInventory().updateItem(newItem, L1PcInventory.COL_IS_ID);
                    } catch (Throwable ignore) {}
                }
            }
        }
        rs.close();
        ps.close();
        // 刪除 pshop_items 紀錄
        ps = con.prepareStatement("DELETE FROM pshop_items WHERE stall_id=?");
        ps.setInt(1, stallId);
        ps.executeUpdate();
        ps.close();

        // 步驟 3：返還保證金
        if (escrow > 0) {
            pc.getInventory().storeItem(modeItemId, (int) escrow);
        }

        // 步驟 4：刪除 pshop_stalls 記錄
        ps = con.prepareStatement(
            "DELETE FROM pshop_stalls WHERE stall_id=? OR npc_objid=? OR (account_name=? AND char_id=? AND mode_item_id=? AND status=1)");
        ps.setInt(1, stallId);
        ps.setInt(2, npcObjId);
        ps.setString(3, pc.getAccountName());
        ps.setInt(4, pc.getId());
        ps.setInt(5, modeItemId);
        ps.executeUpdate();
        ps.close();
        con.close();
        // 給予玩家提示，告知原攤位已自動關閉
        sendMsg(pc, "偵測到遺失的攤位，已自動將物品與保證金返還並關閉。");
    }

    private static int insertStallRow(L1PcInstance pc, int npcObjId, int modeItemId, long escrow, String desc) throws Exception {
        Connection con = L1DatabaseFactory.getInstance().getConnection();
        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO pshop_stalls(account_name,char_id,mode_item_id,npc_objid,stall_desc,escrow_amount,status) "
                        + "VALUES(?,?,?,?,?,?,1)", Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, pc.getAccountName());
        ps.setInt(2, pc.getId());
        ps.setInt(3, modeItemId);
        ps.setInt(4, npcObjId);
        ps.setString(5, ""); // 先不寫中文
        ps.setLong(6, escrow);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        int id = 0;
        if (rs.next()) {
            id = rs.getInt(1);
        }
        rs.close();
        ps.close();
        con.close();
        return id;
    }

    /**
     * 將販售品寫入 pshop_items 表。包含祝福、強化與屬性資訊。
     *
     * @param stallId       攤位 ID
     * @param itemId        物品 ID
     * @param price         售價
     * @param count         未售出的個數
     * @param bless         祝福狀態 (0=普通,1=受咀咒,2=受祝福)
     * @param enchant       強化等級
     * @param attr          屬性強化等級（僅儲存等級，不包含屬性種類）
     * @param ownerItemObj  原物品的 objectId
     */
    private static void insertItemRow(int stallId, int itemId, int price, int count,
            int bless, int enchant, int attr, int ownerItemObj) throws Exception {
        Connection con = L1DatabaseFactory.getInstance().getConnection();
        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO pshop_items(stall_id,item_id,bless,enchant_lvl,attr_enchant,sell_price,sell_count,owner_item_objid) "
                        + "VALUES(?,?,?,?,?,?,?,?)");
        ps.setInt(1, stallId);
        ps.setInt(2, itemId);
        ps.setInt(3, bless);
        ps.setInt(4, enchant);
        ps.setInt(5, attr);
        ps.setInt(6, price);
        ps.setInt(7, count);
        ps.setInt(8, ownerItemObj);
        ps.executeUpdate();
        ps.close();
        con.close();
    }

    /**
     * 將收購需求寫入 pshop_items 表。祝福與強化資訊固定為 0。
     *
     * @param stallId 攤位 ID
     * @param itemId  物品 ID
     * @param price   收購價格
     * @param count   收購數量
     */
    private static void insertBuyRow(int stallId, int itemId, int price, int count) throws Exception {
        Connection con = L1DatabaseFactory.getInstance().getConnection();
        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO pshop_items(stall_id,item_id,bless,enchant_lvl,attr_enchant,buy_price,buy_count) "
                        + "VALUES(?,?,?,?,?,?,?)");
        ps.setInt(1, stallId);
        ps.setInt(2, itemId);
        ps.setInt(3, 1);      // bless 預設為普通
        ps.setInt(4, 0);      // enchant 等級為 0
        ps.setInt(5, 0);      // 屬性強化等級為 0
        ps.setInt(6, price);
        ps.setInt(7, count);
        ps.executeUpdate();
        ps.close();
        con.close();
    }

    private static void insertHoldingRow(L1PcInstance pc, int modeItemId, int itemId, int count) throws Exception {
        Connection con = L1DatabaseFactory.getInstance().getConnection();
        PreparedStatement ps = con.prepareStatement(
                "INSERT INTO pshop_holdings(account_name,char_id,mode_item_id,item_id,item_count) "
                        + "VALUES(?,?,?,?,?) ON DUPLICATE KEY UPDATE item_count=item_count+VALUES(item_count)");
        ps.setString(1, pc.getAccountName());
        ps.setInt(2, pc.getId());
        ps.setInt(3, modeItemId);
        ps.setInt(4, itemId);
        ps.setInt(5, count);
        ps.executeUpdate();
        ps.close();
        con.close();
    }

    // ============== 攤主：查看攤位商品 ==============
    private static void showOwnerList(L1PcInstance pc) throws Exception {
        StallInfo stall = findMyActiveStall(pc);
        if (stall == null) {
            sendMsg(pc, "你目前沒有啟用中的攤位。");
            return;
        }
        // 不顯示額外的系統訊息，直接顯示列表
        // 查詢未售出的商品並組裝為列表
        Connection con = L1DatabaseFactory.getInstance().getConnection();
        PreparedStatement ps = con.prepareStatement(
                "SELECT item_id,sell_count,enchant_lvl,attr_enchant,bless,owner_item_objid "
              + "FROM pshop_items WHERE stall_id=? AND (sell_count>0 OR owner_item_objid>0)");
        ps.setInt(1, stall.stallId);
        ResultSet rs = ps.executeQuery();
        java.util.List<S_PShopItemList.Entry> entries = new java.util.ArrayList<>();
        while (rs.next()) {
            int itemId    = rs.getInt("item_id");
            if (itemId <= 0) {
                // 已被標記為失效的紀錄（例如已完全賣出的販售條目），不再顯示給攤主
                continue;
            }
            int count     = rs.getInt("sell_count");
            int enchant   = rs.getInt("enchant_lvl");
            int attr      = rs.getInt("attr_enchant");
            int bless     = rs.getInt("bless");
            int ownerObj  = rs.getInt("owner_item_objid");
            l1j.server.server.templates.L1Item template = null;
            try {
                template = ItemTable.getInstance().getTemplate(itemId);
            } catch (Throwable ignore) {}
            if (template != null) {
                // 組合顯示名稱（強化值 + 名稱）
                S_PShopItemList.Entry e = new S_PShopItemList.Entry();
                e.objectId   = ownerObj > 0 ? ownerObj : itemId;
                e.useType    = template.getUseType();
                e.gfxId      = template.getGfxId();
                e.bless      = bless;
                e.count      = count;
                e.identified = 1; // 以已鑑定狀態顯示，名稱由 getViewName() 決定
                e.name       = buildPShopDisplayName(itemId, bless, enchant, attr, count);
                entries.add(e);
            }
        }
        rs.close();
        ps.close();
        con.close();
        if (entries.isEmpty()) {
            // 若列表為空仍發送空的檢索包顯示空白畫面
            pc.sendPackets(new S_PShopItemList(stall.npcObjId, entries));
        } else {
            pc.sendPackets(new S_PShopItemList(stall.npcObjId, entries));
        }
    }

    // ============== 攤主：已獲得的物品(託管) ==============
    private 
	/**
	 * 攤主查看「已獲得的物品」。
	 * 顯示：
	 * 1) 已託管的貨幣(holdings)，依照攤位幣別 mode_item_id 過濾。
	 * 2) 尚未售出的販售品。
	 * 3) 攤位保證金（使用與攤位相同幣別的圖示）。
	 */
	static void showOwnerEscrow(L1PcInstance pc) throws Exception {
		StallInfo stall = findMyActiveStall(pc);
		if (stall == null) {
			sendMsg(pc, "你目前沒有啟用中的攤位。");
			return;
		}
		// 組裝所有託管物品與未售商品的列表
		java.util.List<S_PShopItemList.Entry> entries = new java.util.ArrayList<>();
		try (java.sql.Connection con = L1DatabaseFactory.getInstance().getConnection()) {
			// 1) 攤主帳上的貨幣（holdings）
			try (java.sql.PreparedStatement ps = con.prepareStatement(
					"SELECT item_id,item_count FROM pshop_holdings WHERE account_name=? AND char_id=? AND mode_item_id=?")) {
				ps.setString(1, pc.getAccountName());
				ps.setInt(2, pc.getId());
				ps.setInt(3, stall.modeItemId);
				try (java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						int itemId = rs.getInt("item_id");
						int count  = rs.getInt("item_count");
						l1j.server.server.templates.L1Item template = null;
						try {
							template = ItemTable.getInstance().getTemplate(itemId);
						} catch (Throwable ignore) {}
						if (template == null) {
							continue;
						}
						S_PShopItemList.Entry e = new S_PShopItemList.Entry();
						e.objectId   = itemId;
						e.useType    = template.getUseType();
						e.gfxId      = template.getGfxId();
						e.bless      = 0;
						e.count      = count;
						e.identified = 1;
						e.name       = template.getName();
						entries.add(e);
					}
				}
			}

			// 2) 未售出的販售品 (含強化及祝福資訊)
			try (java.sql.PreparedStatement ps = con.prepareStatement(
					"SELECT item_id,sell_count,enchant_lvl,attr_enchant,bless,owner_item_objid " +
					"FROM pshop_items WHERE stall_id=? AND (sell_count>0 OR owner_item_objid>0)")) {
				ps.setInt(1, stall.stallId);
				try (java.sql.ResultSet rs = ps.executeQuery()) {
					while (rs.next()) {
						int itemId    = rs.getInt("item_id");
						int count     = rs.getInt("sell_count");
						int enchant   = rs.getInt("enchant_lvl");
						int attr      = rs.getInt("attr_enchant");
						int bless     = rs.getInt("bless");
						int ownerObj  = rs.getInt("owner_item_objid");
						l1j.server.server.templates.L1Item template = null;
						try {
							template = ItemTable.getInstance().getTemplate(itemId);
						} catch (Throwable ignore) {}
						if (template == null) {
							continue;
						}
						S_PShopItemList.Entry e = new S_PShopItemList.Entry();
						e.objectId   = ownerObj > 0 ? ownerObj : itemId;
						e.useType    = template.getUseType();
						e.gfxId      = template.getGfxId();
						e.bless      = bless;
						e.count      = count;
						e.identified = 1;
						e.name       = buildPShopDisplayName(itemId, bless, enchant, attr, count);
						entries.add(e);
					}
				}
			}

			// 3) 攤位保證金/貨幣顯示 — 嚴格依照攤位幣別
			if (stall.escrow > 0 && stall.modeItemId > 0) {
				try {
					l1j.server.server.templates.L1Item tmp = ItemTable.getInstance().getTemplate(stall.modeItemId);
					if (tmp != null) {
						S_PShopItemList.Entry e = new S_PShopItemList.Entry();
						e.objectId   = stall.modeItemId;
						e.useType    = tmp.getUseType();
						e.gfxId      = tmp.getGfxId();
						e.bless      = 0;
						e.count      = (int) stall.escrow;
						e.identified = 1;
						e.name       = tmp.getName();
						entries.add(e);
					}
				} catch (Throwable ignore) {}
			}
		}

		pc.sendPackets(new S_PShopItemList(stall.npcObjId, entries));
	}
static void closeShop(L1PcInstance pc) throws Exception {

        StallInfo stall = findMyActiveStall(pc);
        if (stall == null) {
            sendMsg(pc, "你目前沒有啟用中的攤位。");
            return;
        }

        // 1) 返還攤位託管的存貨與樣品（pshop_items）
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT item_id, sell_count, bless, enchant_lvl, attr_enchant, owner_item_objid " +
                 "FROM pshop_items WHERE stall_id=? AND (sell_count>0 OR owner_item_objid>0)")) {
            ps.setInt(1, stall.stallId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    final int itemId = rs.getInt("item_id");
                    if (itemId <= 0) {
                        // 已失效的紀錄（例如全部售出的販售條目），不再返還任何物品
                        continue;
                    }
                    final int count  = Math.max(1, rs.getInt("sell_count"));
                    final int bless  = rs.getInt("bless");
                    final int ench   = rs.getInt("enchant_lvl");
                    final int attrLv = rs.getInt("attr_enchant");
                    final int ownerObj = rs.getInt("owner_item_objid");

                    l1j.server.server.templates.L1Item template =
                        l1j.server.server.datatables.ItemTable.getInstance().getTemplate(itemId);
                    if (template == null) continue;

                    if (template.isStackable()) {
                        L1ItemInstance newItem = pc.getInventory().storeItem(itemId, count);
                        if (newItem != null) {
                            try {
                                newItem.setBless(bless);
                                newItem.setEnchantLevel(ench);
                                int attrKind = decodeAttrKind(attrLv);
                                int attrLevel = decodeAttrLevel(attrLv);
                                if (attrKind > 0) {
                                    newItem.setAttrEnchantKind(attrKind);
                                }
                                if (attrLevel > 0) {
                                    newItem.setAttrEnchantLevel(attrLevel);
                                }
                                newItem.setIdentified(true);
                                pc.getInventory().updateItem(newItem, L1PcInventory.COL_IS_ID);
                            } catch (Throwable ignore) {}
                        }
                    } else {
                        for (int i = 0; i < count; i++) {
                            L1ItemInstance newItem = new L1ItemInstance(template, 1);
                            if (ownerObj > 0) newItem.setId(ownerObj);
                            try {
                                newItem.setBless(bless);
                                newItem.setEnchantLevel(ench);
                                int attrKind = decodeAttrKind(attrLv);
                                int attrLevel = decodeAttrLevel(attrLv);
                                if (attrKind > 0) {
                                    newItem.setAttrEnchantKind(attrKind);
                                }
                                if (attrLevel > 0) {
                                    newItem.setAttrEnchantLevel(attrLevel);
                                }
                                newItem.setIdentified(true);
                                pc.getInventory().updateItem(newItem, L1PcInventory.COL_IS_ID);
                            } catch (Throwable ignore) {}
                            L1World.getInstance().storeObject(newItem);
                            pc.getInventory().storeItem(newItem);
                        }
                    }
                }
            }
        }

        // 2) 清除 pshop_items
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM pshop_items WHERE stall_id=?")) {
            ps.setInt(1, stall.stallId);
            ps.executeUpdate();
        }
        // 2.5) 返還保證金與（若有）累積收益
        try (Connection con2 = L1DatabaseFactory.getInstance().getConnection()) {
            long escrowAmt = 0L;
            int modeItem = 0;
            try (PreparedStatement q = con2.prepareStatement(
                    "SELECT escrow_amount, mode_item_id FROM pshop_stalls WHERE stall_id=?")) {
                q.setInt(1, stall.stallId);
                try (ResultSet r = q.executeQuery()) {
                    if (r.next()) {
                        escrowAmt = r.getLong("escrow_amount");
                        modeItem = r.getInt("mode_item_id");
                    }
                }
            } catch (Throwable ignore) {}

            long holdingAmt = 0L;
            try (PreparedStatement qh = con2.prepareStatement(
                    "SELECT COALESCE(SUM(amount),0) FROM pshop_holdings WHERE stall_id=?")) {
                qh.setInt(1, stall.stallId);
                try (ResultSet rh = qh.executeQuery()) {
                    if (rh.next()) holdingAmt = rh.getLong(1);
                }
            } catch (Throwable ignore) {
                // 沒有 holdings 表則忽略
            }

            long refund = escrowAmt + holdingAmt;
            if (modeItem > 0 && refund > 0) {
                long remaining = refund;
                while (remaining > 0) {
                    int chunk = (int)Math.min(remaining, Integer.MAX_VALUE);
                    pc.getInventory().storeItem(modeItem, chunk);
                    remaining -= chunk;
                }
            }

            // 清空 escrow 並刪除 holdings 資料
            try (PreparedStatement u = con2.prepareStatement("UPDATE pshop_stalls SET escrow_amount=0 WHERE stall_id=?")) {
                u.setInt(1, stall.stallId);
                u.executeUpdate();
            } catch (Throwable ignore) {}
            try (PreparedStatement d = con2.prepareStatement("DELETE FROM pshop_holdings WHERE stall_id=?")) {
                d.setInt(1, stall.stallId);
                d.executeUpdate();
            } catch (Throwable ignore) {}
        }


        // 3) 移除 NPC 與標記攤位關店
        try {
} catch (Throwable ignore) {}
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement("DELETE FROM pshop_stalls WHERE stall_id=?")) {
            ps.setInt(1, stall.stallId);
            ps.executeUpdate();
        }
        sendMsg(pc, "攤位已關閉，原物返還完成。");

        // 停止並移除攤位的喊話器，避免NPC收攤後仍持續喊話
        try {
            PShopAnnouncer ann = _announcers.remove(stall.npcObjId);
            if (ann != null) {
                ann.stop();
            }
        } catch (Throwable ignore) {}




        // 嘗試讓 NPC 立即消失（不影響返還）
        try {
            l1j.server.server.model.L1Object obj = l1j.server.server.model.L1World.getInstance().findObject(stall.npcObjId);
            if (obj instanceof l1j.server.server.model.Instance.L1NpcInstance) {
                ((l1j.server.server.model.Instance.L1NpcInstance)obj).deleteMe();
            }
        } catch (Throwable ignore) {}

}


    // ============== 買家：查看販售清單 ==============
    private static void showBuyerSaleList(L1PcInstance pc) throws Exception {
        // 使用動態商店列表顯示攤位販售商品，仿照一般商店UI
        int npcObjId = pc.getTempID();
        if (npcObjId == 0) {
            sendMsg(pc, "未選擇攤位。");
            return;
        }
        // 查詢攤位 ID
        int stallId = 0;
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT stall_id FROM pshop_stalls WHERE npc_objid=? AND status=1")) {
            ps.setInt(1, npcObjId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stallId = rs.getInt(1);
                }
            }
        }
        if (stallId == 0) {
            sendMsg(pc, "此攤位已經不存在。");
            return;
        }
        // 讀取可販售的物品
        PShopSellOrder order = new PShopSellOrder();
        java.util.List<S_PShopSellList.Entry> entries = new java.util.ArrayList<>();
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT id,item_id,sell_count,sell_price,bless,enchant_lvl,attr_enchant FROM pshop_items " +
                     "WHERE stall_id=? AND sell_count>0 AND sell_price>0")) {
            ps.setInt(1, stallId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int rowId  = rs.getInt("id");
                    int itemId = rs.getInt("item_id");
                    int count  = rs.getInt("sell_count");
                    int price  = rs.getInt("sell_price");
                    int bless  = rs.getInt("bless");
                    int enchant= rs.getInt("enchant_lvl");
                    int attr   = rs.getInt("attr_enchant");
                    l1j.server.server.templates.L1Item template = null;
                    try { template = ItemTable.getInstance().getTemplate(itemId); } catch (Throwable ignore) {}
                    if (template == null) continue;
                    // 建立賣家條目
                    PShopSellItem sellItem = new PShopSellItem();
                    sellItem.index   = rowId;
                    sellItem.itemId  = itemId;
                    sellItem.price   = price;
                    sellItem.count   = count;
                    sellItem.bless   = bless;
                    sellItem.enchant = enchant;
                    sellItem.attr    = attr;
                    sellItem.gfxId   = template.getGfxId();
                    // 組合名稱，包含強化等級及價格
                    StringBuilder sb = new StringBuilder();
                    if (enchant != 0) {
                        if (enchant > 0) sb.append("+" + enchant + " ");
                        else sb.append(enchant + " ");
                    }
                    sb.append(template.getName());
                    sellItem.name = buildPShopDisplayName(itemId, bless, enchant, attr, count);
                    // 取得狀態 bytes
                    byte[] status = null;
                    try {
                        l1j.server.server.model.Instance.L1ItemInstance dummy = new l1j.server.server.model.Instance.L1ItemInstance();
                        dummy.setItem(template);
                        status = dummy.getStatusBytes();
                    } catch (Throwable ignore) {}
                    sellItem.status = status;
                    order.items.add(sellItem);
                    // 建立封包條目
                    S_PShopSellList.Entry entry = new S_PShopSellList.Entry();
                    // 使用 rowId 直接作為索引，以便後續結算
                    entry.index = order.items.size() - 1;
                    entry.gfxId = template.getGfxId();
                    entry.price = price;
                    entry.name  = sellItem.name;
                    entry.status = status;
                    entries.add(entry);
                }
            }
        }
        // 將暫存清單加入 map
        setPendingSellOrder(pc.getId(), order);
        // 傳送封包
        pc.sendPackets(new S_PShopSellList(npcObjId, entries));
    }

    // ============== 買家：查看收購清單 ==============
    


private static void showBuyerBuyList(l1j.server.server.model.Instance.L1PcInstance pc) throws Exception {
    int npcObjId = pc.getTempID();
    if (npcObjId == 0) {
        pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("未選擇攤位。"));
        return;
    }
    int stallId = 0;
    try (java.sql.Connection con = l1j.server.L1DatabaseFactory.getInstance().getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(
             "SELECT stall_id FROM pshop_stalls WHERE npc_objid=? AND status=1")) {
        ps.setInt(1, npcObjId);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) stallId = rs.getInt(1);
        }
    }
    if (stallId == 0) {
        pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("該攤位不存在或已關閉。"));
        return;
    }

    PShopBuyOrder order = new PShopBuyOrder();
    java.util.List<S_PShopBuyList.Entry> entries = new java.util.ArrayList<>();

    try (java.sql.Connection con = l1j.server.L1DatabaseFactory.getInstance().getConnection();
         java.sql.PreparedStatement ps = con.prepareStatement(
             "SELECT b.id, b.item_id, b.buy_price, b.buy_count, b.bless, b.enchant_lvl, b.attr_enchant " +
             "FROM pshop_items b " +
             "WHERE b.stall_id=? AND b.buy_price>0 AND b.buy_count>0 " +
             "AND EXISTS (SELECT 1 FROM pshop_items s " +
             "            WHERE s.stall_id=b.stall_id AND s.item_id=b.item_id " +
             "              AND (s.owner_item_objid>0 OR s.sell_count>0))")) {
        ps.setInt(1, stallId);
        try (java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int rowId  = rs.getInt("id");
                int itemId = rs.getInt("item_id");
                int price  = rs.getInt("buy_price");
                int limit  = rs.getInt("buy_count");
                int blessSpec   = 0;
                int enchantSpec = 0;
                int attrSpec    = 0;
                try {
                    blessSpec   = rs.getInt("bless");
                    enchantSpec = rs.getInt("enchant_lvl");
                    attrSpec    = rs.getInt("attr_enchant");
                } catch (Throwable ignore) {}

                // 買家背包中符合條件的未裝備清單（需同 itemId + bless + enchant + 屬性）
                java.util.List<l1j.server.server.model.Instance.L1ItemInstance> list = new java.util.ArrayList<>();
                for (l1j.server.server.model.Instance.L1ItemInstance it : pc.getInventory().getItems()) {
                    try {
                        if (it.getItemId() != itemId || it.isEquipped()) {
                            continue;
                        }
                        int blessInv   = 0;
                        int enchantInv = 0;
                        int attrInvEnc = 0;
                        try {
                            blessInv   = it.getBless();
                            enchantInv = it.getEnchantLevel();
                            int kindInv  = 0;
                            int levelInv = 0;
                            try {
                                kindInv  = it.getAttrEnchantKind();
                                levelInv = it.getAttrEnchantLevel();
                            } catch (Throwable ignore2) {}
                            attrInvEnc = encodeAttr(kindInv, levelInv);
                        } catch (Throwable ignore3) {}
                        boolean blessMismatch = (blessSpec != 0 && blessInv != blessSpec);
                        boolean enchantMismatch = (enchantSpec != 0 && enchantInv != enchantSpec);
                        boolean attrMismatch = (attrSpec != 0 && attrInvEnc != attrSpec);
                        if (blessMismatch || enchantMismatch || attrMismatch) {
                            continue;
                        }
                        list.add(it);
                    } catch (Throwable ignore) {}
                }
                if (list.isEmpty()) continue;

                l1j.server.server.templates.L1Item template = l1j.server.server.datatables.ItemTable.getInstance().getTemplate(itemId);
                boolean stackable = template != null && template.isStackable();

                if (stackable) {
                    // 堆疊物：一列，數量上限= min(玩家持有, buy_count)
                    int playerCnt = 0;
                    for (l1j.server.server.model.Instance.L1ItemInstance it : list) playerCnt += it.getCount();
                    int maxSell = Math.max(0, Math.min(playerCnt, limit));
                    if (maxSell <= 0) continue;

                    l1j.server.server.model.Instance.L1ItemInstance any = list.get(0);
                    S_PShopBuyList.Entry e = new S_PShopBuyList.Entry();
                    e.objId = any.getId();
                    e.price = price;
                    entries.add(e);

                    PShopBuyItem bi = new PShopBuyItem();
                    bi.index  = rowId;      // 對應 DB 行 id（之後要下修 buy_count）
                    bi.objId  = any.getId();// 客戶端圖示/物品定位
                    bi.itemId = itemId;
                    bi.price  = price;
                    bi.count  = maxSell;    // 允許選擇的最大數量
                    order.items.add(bi);
                } else {
                    // 非堆疊：根據玩家背包件數建立多列，最多不超過 buy_count
                    int build = 0;
                    for (l1j.server.server.model.Instance.L1ItemInstance it : list) {
                        if (build >= limit) break;
                        S_PShopBuyList.Entry e = new S_PShopBuyList.Entry();
                        e.objId = it.getId();
                        e.price = price;
                        entries.add(e);

                        PShopBuyItem bi = new PShopBuyItem();
                        bi.index  = rowId;
                        bi.objId  = it.getId();
                        bi.itemId = itemId;
                        bi.price  = price;
                        bi.count  = 1;       // 單件
                        order.items.add(bi);
                        build++;
                    }
                }
            }
        }
    }

    if (entries.isEmpty()) {
        pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("此攤位目前沒有可收購的物品。"));
        return;
    }

    setPendingBuyOrder(pc.getId(), order);
    pc.sendPackets(new S_PShopBuyList(npcObjId, entries));
}




private static StallInfo findMyActiveStall(L1PcInstance pc) throws Exception {
    StallInfo s = null;
    int npcObjId = 0;
    try {
        // 跟目前正在對話的攤位 NPC 綁定
        npcObjId = pc.getTempID();
    } catch (Throwable ignore) {}

    Connection con = L1DatabaseFactory.getInstance().getConnection();

    // 1) 先嘗試：用「目前對話的 NPC(objid)」+ 帳號 + 角色 找「這一攤」的資料
    if (npcObjId > 0) {
        PreparedStatement ps = con.prepareStatement(
                "SELECT stall_id,mode_item_id,escrow_amount,npc_objid " +
                "FROM pshop_stalls WHERE account_name=? AND char_id=? AND npc_objid=? AND status=1");
        ps.setString(1, pc.getAccountName());
        ps.setInt(2, pc.getId());
        ps.setInt(3, npcObjId);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            s = new StallInfo();
            s.stallId    = rs.getInt("stall_id");
            s.modeItemId = rs.getInt("mode_item_id");
            s.escrow     = rs.getLong("escrow_amount");
            s.npcObjId   = rs.getInt("npc_objid");
        }
        rs.close();
        ps.close();
    }

    // 2) 如果沒找到，就回退成舊行為：抓這個角色的任一個啟用中的攤位（通常是最後開啟的那一個）
    if (s == null) {
        PreparedStatement ps = con.prepareStatement(
                "SELECT stall_id,mode_item_id,escrow_amount,npc_objid " +
                "FROM pshop_stalls WHERE account_name=? AND char_id=? AND status=1 " +
                "ORDER BY stall_id DESC LIMIT 1");
        ps.setString(1, pc.getAccountName());
        ps.setInt(2, pc.getId());
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            s = new StallInfo();
            s.stallId    = rs.getInt("stall_id");
            s.modeItemId = rs.getInt("mode_item_id");
            s.escrow     = rs.getLong("escrow_amount");
            s.npcObjId   = rs.getInt("npc_objid");
        }
        rs.close();
        ps.close();
    }

    con.close();
    return s;
}


    private static class StallInfo {
        int stallId;
        int modeItemId;
        long escrow;
        int npcObjId;
    }

/**
 * 建立/更新收購設定，並從攤主背包託管一顆樣品到攤位（owner_item_objid）。
 */
private static void insertBuyRowWithSample(l1j.server.server.model.Instance.L1PcInstance owner, int stallId, int itemId, int price, int count) throws Exception {
    try (java.sql.Connection con = l1j.server.L1DatabaseFactory.getInstance().getConnection()) {
        // 1) 建立/更新收購設定
        int updated = 0;
        try (java.sql.PreparedStatement ps = con.prepareStatement(
                "UPDATE pshop_items SET buy_price=?, buy_count=? WHERE stall_id=? AND item_id=? AND buy_price>0")) {
            ps.setInt(1, price);
            ps.setInt(2, count);
            ps.setInt(3, stallId);
            ps.setInt(4, itemId);
            updated = ps.executeUpdate();
        }
        if (updated == 0) {
            try (java.sql.PreparedStatement ps = con.prepareStatement(
                "INSERT INTO pshop_items(stall_id,item_id,bless,enchant_lvl,attr_enchant," +
                "sell_price,sell_count,buy_price,buy_count,owner_item_objid) VALUES(?,?,?,?,?, 0,0, ?,?, 0)")) {
                ps.setInt(1, stallId);
                ps.setInt(2, itemId);
                ps.setInt(3, 0);
                ps.setInt(4, 0);
                ps.setInt(5, 0);
                ps.setInt(6, price);
                ps.setInt(7, count);
                ps.executeUpdate();
            }
        }

        // 2) 從攤主背包託管一顆樣品（未裝備）
        l1j.server.server.model.Instance.L1ItemInstance sample = null;
        for (l1j.server.server.model.Instance.L1ItemInstance it : owner.getInventory().findItemsIdNotEquipped(itemId)) { sample = it; break; }
        if (sample != null) {
            int obj = sample.getId();
            owner.getInventory().removeItem(sample, 1);
            try (java.sql.PreparedStatement ps = con.prepareStatement(
                "INSERT INTO pshop_items(stall_id,item_id,bless,enchant_lvl,attr_enchant," +
                "sell_price,sell_count,buy_price,buy_count,owner_item_objid) VALUES(?,?,?,?,?, 0,0, 0,0, ?)")) {
                ps.setInt(1, stallId);
                ps.setInt(2, itemId);
                ps.setInt(3, sample.getBless());
                ps.setInt(4, sample.getEnchantLevel());
                int attrKind = 0;
                int attrLevel = 0;
                try {
                    attrKind = sample.getAttrEnchantKind();
                    attrLevel = sample.getAttrEnchantLevel();
                } catch (Throwable ignore) {}
                ps.setInt(5, encodeAttr(attrKind, attrLevel));
                ps.setInt(6, obj);
                ps.executeUpdate();
            }
        }
    }
}
}