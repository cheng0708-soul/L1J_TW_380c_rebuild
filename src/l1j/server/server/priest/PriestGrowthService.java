
package l1j.server.server.priest;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.templates.L1Item;
import l1j.server.server.utils.SQLUtil;

/**
 * 祭司/公主娃娃升階服務（資料驅動，失敗不消失）。
 * - priest_item_stage_map / princess_doll_stage_map
 * - growth_rates：成功率（千分比）
 * - 成功：替換模板，但 item_objid 不變；同步通知 PriestIqDAO2 繼承智力綁定。
 */
public final class PriestGrowthService {
    private static final Logger _log = Logger.getLogger(PriestGrowthService.class.getName());
    private static final SecureRandom RNG = new SecureRandom();

    public enum Category { PRIEST, PRINCESS }

    private PriestGrowthService() {}

    public static boolean applyGrowthPotion(final Category cat, final L1PcInstance pc, final L1ItemInstance target) {
        if (pc == null || target == null) return false;
        try (Connection con = L1DatabaseFactory.getInstance().getConnection()) {
            final int itemId = target.getItemId();
            final int stage = getStage(con, cat, itemId);
            if (stage <= 0) { pc.sendPackets(new S_SystemMessage("目標不是可升階的"+ (cat==Category.PRIEST?"祭司":"公主娃娃") +"。")); return false; }
            if (stage >= 5) { pc.sendPackets(new S_SystemMessage("已達最高階。")); return false; }

            final int nextItemId = getNextItemId(con, cat, stage + 1);
            if (nextItemId == 0) { pc.sendPackets(new S_SystemMessage("下一階設定遺失，請通知管理員。")); return false; }

            final int permilles = getRate(con, cat, stage, stage + 1);
            final boolean success = RNG.nextInt(1000) < permilles;

            if (success) {
                L1Item tpl = ItemTable.getInstance().getTemplate(nextItemId);
                if (tpl == null) { pc.sendPackets(new S_SystemMessage("升級模板遺失："+nextItemId)); return false; }
                target.setItem(tpl); // 替換模板，保留 objid 與既有屬性
                pc.sendPackets(new S_SystemMessage("升級成功！已提升至第 " + (stage + 1) + " 階。"));
                // 智力綁定：同步更新 item 對應的階級與模板
             // 智力綁定：同步更新 item 對應的階級與模板，並重算 base / total
                if (cat == Category.PRIEST) {
                    if (PriestIqDAO2.hasItemObjId()) {
                        PriestIqDAO2.onUpgrade(target.getId(), nextItemId, stage + 1);
                    } else {
                        PriestIqDAO2.onUpgrade(pc.getId(), itemId, nextItemId, stage + 1);
                    }
                    // 升階後刷新道具顯示（新 base + bonus）
                    InventoryRefresh.push(pc, target);
                }

            } else {
                pc.sendPackets(new S_SystemMessage("升級失敗，目標道具未消失。"));
            }
            insertLog(con, pc.getId(), target.getId(), cat, stage, success);
            return success;
        } catch (Exception e) {
            _log.log(Level.WARNING, "applyGrowthPotion error", e);
            try { pc.sendPackets(new S_SystemMessage("升級發生例外：" + e.getClass().getSimpleName())); } catch (Exception ignore) {}
            return false;
        }
    }

    private static int getStage(Connection con, Category cat, int itemId) throws SQLException {
        final String table = (cat == Category.PRIEST ? "priest_item_stage_map" : "princess_doll_stage_map");
        final String sql = "SELECT stage FROM " + table + " WHERE item_id=?";
        PreparedStatement ps = null; ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, itemId);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        } finally { SQLUtil.close(rs); SQLUtil.close(ps); }
    }

    private static int getNextItemId(Connection con, Category cat, int nextStage) throws SQLException {
        final String table = (cat == Category.PRIEST ? "priest_item_stage_map" : "princess_doll_stage_map");
        final String sql = "SELECT item_id FROM " + table + " WHERE stage=?";
        PreparedStatement ps = null; ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setInt(1, nextStage);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
            return 0;
        } finally { SQLUtil.close(rs); SQLUtil.close(ps); }
    }

    private static int getRate(Connection con, Category cat, int from, int to) throws SQLException {
        final String sql = "SELECT success_permilles FROM growth_rates WHERE category=? AND from_stage=? AND to_stage=?";
        PreparedStatement ps = null; ResultSet rs = null;
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, cat == Category.PRIEST ? "priest" : "princess");
            ps.setInt(2, from); ps.setInt(3, to);
            rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } finally { SQLUtil.close(rs); SQLUtil.close(ps); }
        return 0;
    }

    private static void insertLog(Connection con, int charId, int itemObjId, Category cat, int fromStage, boolean success) {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(
                "INSERT INTO growth_logs (char_id,item_objid,category,from_stage,to_stage,success,ts) VALUES (?,?,?,?,?,?,CURRENT_TIMESTAMP)");
            ps.setInt(1, charId);
            ps.setInt(2, itemObjId);
            ps.setString(3, cat == Category.PRIEST ? "priest" : "princess");
            ps.setInt(4, fromStage);
            ps.setInt(5, Math.min(5, fromStage + 1));
            ps.setInt(6, success ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException ignore) {
        } finally { SQLUtil.close(ps); }
    }
}
