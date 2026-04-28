package l1j.server.server.priest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.utils.SQLUtil;

/** character_priest_iq DAO：沿用你的資料表風格（以 owner_objid + priest_item_id 唯一鍵）。 */
public final class PriestIqDAO {

    public static final class Row {
        public int uniqueId;
        public int priestItemId;
        public int upgradeStage;
        public int iqBonus;
        public int iqTotal;
        public int ownerObjId;
    }

    private PriestIqDAO() {}

    public static Row getOrCreate(int ownerObjId, int priestItemId, int baseIq) {
        Row r = get(ownerObjId, priestItemId);
        if (r != null) return r;
        insert(ownerObjId, priestItemId, baseIq);
        return get(ownerObjId, priestItemId);
    }

    public static Row get(int ownerObjId, int priestItemId) {
        Connection con = null; PreparedStatement ps = null; ResultSet rs = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            ps = con.prepareStatement("SELECT priest_unique_id,priest_item_id,upgrade_stage,iq_bonus,iq_total,owner_objid FROM character_priest_iq WHERE owner_objid=? AND priest_item_id=?");
            ps.setInt(1, ownerObjId);
            ps.setInt(2, priestItemId);
            rs = ps.executeQuery();
            if (rs.next()) {
                Row r = new Row();
                r.uniqueId = rs.getInt(1);
                r.priestItemId = rs.getInt(2);
                r.upgradeStage = rs.getInt(3);
                r.iqBonus = rs.getInt(4);
                r.iqTotal = rs.getInt(5);
                r.ownerObjId = rs.getInt(6);
                return r;
            }
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(ps);
            SQLUtil.close(con);
        }
    }

    public static void insert(int ownerObjId, int priestItemId, int baseIq) {
        Connection con = null; PreparedStatement ps = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            ps = con.prepareStatement("INSERT INTO character_priest_iq (priest_item_id, upgrade_stage, iq_bonus, iq_total, auto_support_enabled, heal_threshold_pct, owner_objid) VALUES (?,?,?,?,1,50,?)");
            ps.setInt(1, priestItemId);
            ps.setInt(2, 1);
            ps.setInt(3, 0);
            ps.setInt(4, baseIq);
            ps.setInt(5, ownerObjId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(con);
        }
    }

    public static void addIq(int ownerObjId, int priestItemId, int baseIq, int delta) {
        Connection con = null; PreparedStatement ps = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            // 若無記錄 → 先建立
            Row r = get(ownerObjId, priestItemId);
            if (r == null) insert(ownerObjId, priestItemId, baseIq);
            ps = con.prepareStatement("UPDATE character_priest_iq SET iq_bonus=iq_bonus+?, iq_total=iq_total+? WHERE owner_objid=? AND priest_item_id=?");
            ps.setInt(1, delta);
            ps.setInt(2, delta);
            ps.setInt(3, ownerObjId);
            ps.setInt(4, priestItemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(con);
        }
    }
}
