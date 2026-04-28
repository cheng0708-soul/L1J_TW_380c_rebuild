
package l1j.server.server.datatables;

import java.sql.*;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.L1DatabaseFactory;

public class PShopStallTable {
    private static final PShopStallTable _instance = new PShopStallTable();
    public static PShopStallTable getInstance() { return _instance; }

    public int upsertOpen(L1PcInstance owner, int modeItemId, int templateId, int npcObjId, String desc) {
        try (Connection con = L1DatabaseFactory.getInstance().getConnection()) {
            String sql = "INSERT INTO pshop_stalls (account_name,char_id,mode_item_id,npc_objid,npc_templateid,stall_desc,status) "
                       + "VALUES (?,?,?,?,?, ?,1) "
                       + "ON DUPLICATE KEY UPDATE npc_objid=VALUES(npc_objid), npc_templateid=VALUES(npc_templateid), stall_desc=VALUES(stall_desc), status=1";
            try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, owner.getAccountName());
                ps.setInt(2, owner.getId());
                ps.setInt(3, modeItemId);
                ps.setInt(4, npcObjId);
                ps.setInt(5, templateId);
                ps.setString(6, desc);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
            String q = "SELECT stall_id FROM pshop_stalls WHERE account_name=? AND mode_item_id=?";
            try (PreparedStatement ps2 = con.prepareStatement(q)) {
                ps2.setString(1, owner.getAccountName());
                ps2.setInt(2, modeItemId);
                try (ResultSet rs2 = ps2.executeQuery()) {
                    if (rs2.next()) return rs2.getInt(1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int closeAndGetNpcObjId(L1PcInstance owner, int modeItemId) {
        int npcObjId = 0;
        try (Connection con = L1DatabaseFactory.getInstance().getConnection()) {
            try (PreparedStatement ps = con.prepareStatement("SELECT npc_objid FROM pshop_stalls WHERE account_name=? AND mode_item_id=? AND status=1")) {
                ps.setString(1, owner.getAccountName());
                ps.setInt(2, modeItemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) npcObjId = rs.getInt(1);
                }
            }
            try (PreparedStatement ps2 = con.prepareStatement("UPDATE pshop_stalls SET status=0, npc_objid=NULL WHERE account_name=? AND mode_item_id=?")) {
                ps2.setString(1, owner.getAccountName());
                ps2.setInt(2, modeItemId);
                ps2.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return npcObjId;
    }
}
