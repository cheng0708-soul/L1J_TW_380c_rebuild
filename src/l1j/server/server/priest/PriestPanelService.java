
package l1j.server.server.priest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.utils.SQLUtil;

/**
 * 控制面板用：計算「當前智力」= NPC 基本智力 + character_priest_iq.iq_bonus
 * 取得 iq_bonus 的優先順序：
 *   1) 有綁 item_objid 的資料列：以 item_objid 查找
 *   2) 找不到時，用 (owner_objid, priest_item_id) 回退
 */
public final class PriestPanelService {

    private PriestPanelService() {}

    /** 取得面板用的當前智力 */
    public static int getCurrentInt(L1PcInstance pc, L1NpcInstance priest, L1ItemInstance boundItem) {
        int baseInt = getNpcBaseInt(priest);
        int bonus = getIqBonus(pc, priest, boundItem);
        return Math.max(0, baseInt + bonus);
    }

    /** 讀 NPC 基本智力（你的 NpcTable 可能不同，這裡用 DB 直接查詢做為通用法） */
    private static int getNpcBaseInt(L1NpcInstance priest) {
        if (priest != null) {
            try {
                // 多數核心會在 L1NpcInstance.getNpcTemplate().getIntel() 或 getAbility().getInt()
                Object tpl = priest.getNpcTemplate();
                try { 
                    // 嘗試 getIntel()
                    java.lang.reflect.Method m = tpl.getClass().getMethod("getIntel");
                    Object v = m.invoke(tpl);
                    if (v instanceof Integer) return ((Integer)v).intValue();
                } catch (NoSuchMethodException ignore) {}
                try { 
                    // 嘗試 getAbility().getInt()
                    java.lang.reflect.Method m = tpl.getClass().getMethod("getAbility");
                    Object ab = m.invoke(tpl);
                    java.lang.reflect.Method m2 = ab.getClass().getMethod("getInt");
                    Object v = m2.invoke(ab);
                    if (v instanceof Integer) return ((Integer)v).intValue();
                } catch (Throwable ignore) {}
            } catch (Throwable ignore) {}
        }
        // 退而求其次：直接查 DB（需要 npcId）。你的 L1NpcInstance 若能取 getNpcId() 就可用此路徑。
        try {
            int npcId = (int) priest.getClass().getMethod("getNpcId").invoke(priest);
            Connection con = L1DatabaseFactory.getInstance().getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT `intel` FROM `npc` WHERE `npcid`=?");
            ps.setInt(1, npcId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            SQLUtil.close(rs); SQLUtil.close(ps); SQLUtil.close(con);
        } catch (Throwable ignore) {}
        return 0;
    }

    /** 取得智力加成 */
    public static int getIqBonus(L1PcInstance pc, L1NpcInstance priest, L1ItemInstance boundItem) {
        PriestIqDAO2.Row row = null;
        if (boundItem != null) {
            row = PriestIqDAO2.byItemObjId(boundItem.getId());
            if (row != null) return row.iqBonus;
        }
        // 回退：用 owner + priest_item_id
        int itemId = inferPriestItemId(priest);
        if (itemId > 0) {
            row = PriestIqDAO2.byOwnerAndItem(pc.getId(), itemId);
            if (row != null) return row.iqBonus;
        }
        return 0;
    }

    /** 嘗試從 priest 實例推斷 item_id（若你有 stage map 可改寫成查表） */
    private static int inferPriestItemId(L1NpcInstance priest) {
        try {
            // 你若有「召喚時把 itemId 記在 NPC 身上」，可以在這裡讀出自訂欄位；
            // 這裡先嘗試讀 getTransformItemId() / getSpawnItemId() 類方法；沒有就回 0。
            for (String mn : new String[]{"getItemId", "getSpawnItemId", "getTransformItemId"}) {
                try {
                    java.lang.reflect.Method m = priest.getClass().getMethod(mn);
                    Object v = m.invoke(priest);
                    if (v instanceof Integer) {
                        int id = ((Integer)v).intValue();
                        if (id >= 240123 && id <= 240127) return id;
                    }
                } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
        return 0;
    }
}
