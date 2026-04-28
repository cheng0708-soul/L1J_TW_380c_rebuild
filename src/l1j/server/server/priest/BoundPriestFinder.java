package l1j.server.server.priest;

import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;

/**
 * 嘗試找出「目前要使用的那一顆祭司召喚道具」。
 *
 * 優先順序：
 *   1) 若 pc.getTempID() 指向的是祭司 NPC，依該 NPC 推回對應的召喚道具 itemId，
 *      再從 character_priest_iq / 背包中找出那一顆。
 *   2) 找不到時，回退為舊行為：依 owner+priest_item_id 由高階往低階找，
 *      再回退成玩家背包裡的第一顆祭司道具。
 *
 * 這樣在同一角色同時擁有高階、頂階、神話等多顆祭司時，
 * 會依照「目前正在對話的那隻祭司」顯示對應的智力，不會永遠抓到最高等那一顆。
 */
public final class BoundPriestFinder {

    private BoundPriestFinder() {}

    public static L1ItemInstance findFor(L1PcInstance pc) {
        if (pc == null) {
            return null;
        }

        // 1) 先嘗試：依「目前對話中的祭司 NPC」決定綁定哪一顆召喚道具
        L1ItemInstance byNpc = findByNpcContext(pc);
        if (byNpc != null) {
            return byNpc;
        }

        // 2) 找不到時，用舊邏輯：owner + priest_item_id 由高階往低階找
        for (int itemId : new int[]{240127, 240126, 240125, 240124, 240123}) {
            PriestIqDAO2.Row row = PriestIqDAO2.byOwnerAndItem(pc.getId(), itemId);
            if (row != null && row.itemObjId != null) {
                L1ItemInstance it = pc.getInventory().getItem(row.itemObjId.intValue());
                if (it != null) {
                    return it;
                }
            }
        }

        // 3) 最後回退：背包裡第一顆祭司
        for (L1ItemInstance it : pc.getInventory().getItems()) {
            int id = it.getItem().getItemId();
            if (id >= 240123 && id <= 240127) {
                return it;
            }
        }
        return null;
    }

    /**
     * 依照玩家目前對話的 NPC（pc.getTempID()）推回對應的祭司召喚道具。
     */
    private static L1ItemInstance findByNpcContext(L1PcInstance pc) {
        int npcObjId;
        try {
            npcObjId = pc.getTempID();
        } catch (Throwable t) {
            return null;
        }
        if (npcObjId <= 0) {
            return null;
        }

        L1Object obj = L1World.getInstance().findObject(npcObjId);
        if (!(obj instanceof L1NpcInstance)) {
            return null;
        }
        L1NpcInstance priestNpc = (L1NpcInstance) obj;

        int priestItemId = inferPriestItemId(priestNpc);
        if (priestItemId <= 0) {
            return null;
        }

        // 先試著用 owner + priest_item_id 找有綁定 item_objid 的那一筆
        PriestIqDAO2.Row row = PriestIqDAO2.byOwnerAndItem(pc.getId(), priestItemId);
        if (row != null && row.itemObjId != null) {
            L1ItemInstance it = pc.getInventory().getItem(row.itemObjId.intValue());
            if (it != null) {
                return it;
            }
        }

        // 若 DB 還沒建紀錄，就直接從背包找第一顆相同 itemId 的祭司召喚道具
        for (L1ItemInstance it : pc.getInventory().getItems()) {
            if (it.getItem().getItemId() == priestItemId) {
                return it;
            }
        }
        return null;
    }

    /**
     * 從祭司 NPC 推回對應的召喚道具 itemId。
     * 先嘗試呼叫 getItemId / getSpawnItemId / getTransformItemId，
     * 若沒有，則以 npcId 961123~961127 映射到 240123~240127。
     */
    private static int inferPriestItemId(L1NpcInstance priestNpc) {
        if (priestNpc == null) {
            return 0;
        }

        // 1) 反射讀可能存在的 itemId 相關方法
        try {
            for (String mn : new String[]{"getItemId", "getSpawnItemId", "getTransformItemId"}) {
                try {
                    java.lang.reflect.Method m = priestNpc.getClass().getMethod(mn);
                    Object v = m.invoke(priestNpc);
                    if (v instanceof Integer) {
                        int id = ((Integer) v).intValue();
                        if (id >= 240123 && id <= 240127) {
                            return id;
                        }
                    }
                } catch (NoSuchMethodException ignore) {
                    // 換下一個方法名
                }
            }
        } catch (Throwable ignore) {
        }

        // 2) 回退：依 npcId 映射（本服 priest companion ID 範圍：961123~961127）
        try {
            int npcId = (int) priestNpc.getClass().getMethod("getNpcId").invoke(priestNpc);
            switch (npcId) {
                case 961123: return 240123;
                case 961124: return 240124;
                case 961125: return 240125;
                case 961126: return 240126;
                case 961127: return 240127;
                default: break;
            }
        } catch (Throwable ignore) {
        }

        return 0;
    }
}
