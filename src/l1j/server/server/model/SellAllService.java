package l1j.server.server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import l1j.server.server.datatables.ItemTable;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.identity.L1ItemId;
import l1j.server.server.serverpackets.S_Message_YN;
import l1j.server.server.serverpackets.S_NPCTalkReturn;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.william.L1WilliamItemPrice;

/**
 * SellAllService
 * 一鍵販賣的主邏輯（支援分頁與長久記憶）。
 */
public final class SellAllService {

    // 每頁顯示的取消清單數量，對應 sell_all_1.html 裡的 0~19 共 20 個格子。
    private static final int PAGE_SIZE = 20;

    private SellAllService() {
    }

    // 玩家選擇「登陸販售商店的物品」
    public static void handleRegister(L1PcInstance pc) {
        if (pc == null) {
            return;
        }
        SellAllRegistry.setRegisterMode(pc, true);
        pc.sendPackets(new S_SystemMessage("一鍵販賣：請從背包中點擊要登錄販售的物品。"));
    }

    /**
     * 在 C_ItemUSe 中呼叫：
     * if (SellAllService.interceptItemClick(pc, item)) return;
     *
     * @return true 表示已處理點擊，不要再繼續原本的使用行為。
     */
    public static boolean interceptItemClick(L1PcInstance pc, L1ItemInstance item) {
        if (pc == null || item == null) {
            return false;
        }
        if (!SellAllRegistry.isRegisterMode(pc)) {
            return false;
        }

        // 單次登錄結束登錄模式
        SellAllRegistry.setRegisterMode(pc, false);

        int itemId = item.getItem().getItemId();

        // 不允許登錄金幣與一鍵販售道具本身
        if (itemId == L1ItemId.ADENA || itemId == 240153) {
            pc.sendPackets(new S_SystemMessage("一鍵販賣：此物品無法登錄為一鍵販售目標。"));
            return true;
        }

        SellAllRegistry.addItem(pc, itemId);

        String name;
        try {
            name = item.getItem().getName();
        } catch (Throwable t) {
            name = "ID=";
        }

        pc.sendPackets(new S_SystemMessage("一鍵販賣：已登錄物品 " + name + " 。"));
        pc.setTempID(0);
        return true;
    }

    // 玩家選擇「取消販售物品」→ 一律從第 0 頁開始
    public static void handleCancel(L1PcInstance pc) {
        if (pc == null) {
            return;
        }
        SellAllRegistry.setCancelPage(pc, 0);
        openCancelList(pc, 0);
    }

    // 上一頁
    public static void handlePagePrev(L1PcInstance pc) {
        if (pc == null) {
            return;
        }
        int page = SellAllRegistry.getCancelPage(pc);
        page--;
        if (page < 0) {
            page = 0;
        }
        openCancelList(pc, page);
    }

    // 下一頁
    public static void handlePageNext(L1PcInstance pc) {
        if (pc == null) {
            return;
        }
        int page = SellAllRegistry.getCancelPage(pc);
        page++;
        openCancelList(pc, page); // openCancelList 內部會 clamp 範圍
    }

    // 重新打開目前頁數的取消清單（給 C_Attr case 67 使用）
    public static void openCancelList(L1PcInstance pc) {
        if (pc == null) {
            return;
        }
        int page = SellAllRegistry.getCancelPage(pc);
        openCancelList(pc, page);
    }

    // 打開指定頁數的取消清單
    public static void openCancelList(L1PcInstance pc, int page) {
        if (pc == null) {
            return;
        }

        Set<Integer> regSet = SellAllRegistry.getRegisteredItems(pc);
        if (regSet.isEmpty()) {
            pc.sendPackets(new S_SystemMessage("一鍵販賣：目前沒有任何登錄的販售物品。"));
            return;
        }

        List<Integer> all = new ArrayList<Integer>(regSet);
        int total = all.size();
        int pageCount = (total + PAGE_SIZE - 1) / PAGE_SIZE;
        if (pageCount <= 0) {
            pageCount = 1;
        }

        if (page < 0) {
            page = 0;
        } else if (page >= pageCount) {
            page = pageCount - 1;
        }
        SellAllRegistry.setCancelPage(pc, page);

        String[] args = new String[PAGE_SIZE];
        for (int i = 0; i < PAGE_SIZE; i++) {
            args[i] = "";
        }

        int start = page * PAGE_SIZE;
        for (int i = 0; i < PAGE_SIZE; i++) {
            int idx = start + i;
            if (idx >= total) {
                break;
            }
            int itemId = all.get(idx).intValue();
            String name;
            try {
                if (itemId == L1ItemId.ADENA) {
                    name = "金幣";
                } else {
                    name = ItemTable.getInstance().getTemplate(itemId).getName();
                }
            } catch (Throwable t) {
                name = "ID=";
            }
            args[i] = name ;
        }

        pc.sendPackets(new S_NPCTalkReturn(pc.getId(), "sell_all_1", args));
    }

    // 玩家在取消清單中點了一個物品：sellall_cancel_select <index>
    public static void handleCancelSelect(L1PcInstance pc, String cmd) {
        if (pc == null || cmd == null) {
            return;
        }

        String[] parts = cmd.split(" ");
        if (parts.length < 2) {
            return;
        }
        int index;
        try {
            index = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        if (index < 0 || index >= PAGE_SIZE) {
            return;
        }

        Set<Integer> regSet = SellAllRegistry.getRegisteredItems(pc);
        if (regSet.isEmpty()) {
            return;
        }
        List<Integer> all = new ArrayList<Integer>(regSet);
        int total = all.size();
        int page = SellAllRegistry.getCancelPage(pc);
        int globalIndex = page * PAGE_SIZE + index;
        if (globalIndex < 0 || globalIndex >= total) {
            return;
        }
        int itemId = all.get(globalIndex).intValue();

        String name;
        try {
            if (itemId == L1ItemId.ADENA) {
                name = "金幣";
            } else {
                name = ItemTable.getInstance().getTemplate(itemId).getName();
            }
        } catch (Throwable t) {
            name = "ID=" + itemId;
        }

        pc.setTempID(itemId);
        pc.sendPackets(new S_Message_YN(67, name));
    }

    // 玩家選擇「一鍵販售物品」
    public static void handleExecute(L1PcInstance pc) {
        if (pc == null) {
            return;
        }

        Set<Integer> regSet = SellAllRegistry.getRegisteredItems(pc);
        if (regSet.isEmpty()) {
            pc.sendPackets(new S_SystemMessage("一鍵販賣：目前沒有任何登錄的販售物品。"));
            return;
        }

        long totalGain = 0L;
        int soldCount = 0;

        for (Integer iid : regSet) {
            int itemId = iid.intValue();
            if (itemId == L1ItemId.ADENA || itemId == 240153) {
                continue;
            }

            // 玩家背包中所有該 itemId 的道具
            List<L1ItemInstance> toSell = new ArrayList<L1ItemInstance>();
            for (L1ItemInstance invItem : pc.getInventory().getItems()) {
                if (invItem.getItem().getItemId() == itemId) {
                    // 需求：一鍵販售時排除「已強化(附魔)」以及「裝備中」的物品
                    // - 強化：以 enchant level != 0 判斷（包含 +1、-1 等）
                    // - 裝備中：以 isEquipped() 判斷
                    if (invItem.isEquipped()) {
                        continue;
                    }
                    if (invItem.getEnchantLevel() != 0) {
                        continue;
                    }
                    toSell.add(invItem);
                }
            }

            if (toSell.isEmpty()) {
                continue;
            }

            int price = 0;
            try {
                // 依照你自訂的價格表取價
                price = L1WilliamItemPrice.getItemId(itemId);
            } catch (Throwable t) {
                price = 0;
            }
            if (price <= 0) {
                continue;
            }

            for (L1ItemInstance invItem : toSell) {
                int count = invItem.getCount();
                if (count <= 0) {
                    continue;
                }

                long gain = (long) price * (long) count;
                if (gain <= 0) {
                    continue;
                }

                pc.getInventory().removeItem(invItem, count);
                totalGain += gain;
                soldCount += count;
            }
        }

        if (totalGain <= 0L) {
            pc.sendPackets(new S_SystemMessage("一鍵販賣：沒有可販售的登錄物品。"));
            return;
        }

        // 給予金幣（L1Inventory.storeItem 只接受 int，因此需轉型）
        int gainInt = (int) Math.min(totalGain, Integer.MAX_VALUE);
        pc.getInventory().storeItem(L1ItemId.ADENA, gainInt);
        pc.sendPackets(new S_SystemMessage("一鍵販賣：共販售 " + soldCount + " 個物品，獲得 " + gainInt + " 金幣。"));
    }

    // 開啟主選單 sell_all
    public static void openMainDialog(L1PcInstance pc) {
        if (pc == null) {
            return;
        }
        pc.sendPackets(new S_NPCTalkReturn(pc.getId(), "sell_all"));
    }
}
