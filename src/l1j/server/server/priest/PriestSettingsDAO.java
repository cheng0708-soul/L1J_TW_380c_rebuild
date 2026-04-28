package l1j.server.server.priest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.utils.SQLUtil;

public class PriestSettingsDAO {

    public static class Row {
        public int charId;
        public int threshold;
        public boolean autoSupport;
        public int mp;
    }

    public Row load(int charId) {
        Row r = null;
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            ps = con.prepareStatement("SELECT char_id, heal_threshold, auto_support, mp FROM priest_settings WHERE char_id=?");
            ps.setInt(1, charId);
            rs = ps.executeQuery();
            if (rs.next()) {
                r = new Row();
                r.charId = rs.getInt("char_id");
                r.threshold = rs.getInt("heal_threshold");
                r.autoSupport = rs.getInt("auto_support") != 0;
                r.mp = rs.getInt("mp");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(ps);
            SQLUtil.close(con);
        }
        return r;
    }

    public void upsert(int charId, int threshold, boolean autoSupport, int mp) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            ps = con.prepareStatement(
                "INSERT INTO priest_settings (char_id, heal_threshold, auto_support, mp, updated_at) " +
                "VALUES (?,?,?,?,NOW()) " +
                "ON DUPLICATE KEY UPDATE heal_threshold=VALUES(heal_threshold), auto_support=VALUES(auto_support), mp=VALUES(mp), updated_at=VALUES(updated_at)"
            );
            ps.setInt(1, charId);
            ps.setInt(2, threshold);
            ps.setInt(3, autoSupport ? 1 : 0);
            ps.setInt(4, mp);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(con);
        }
    }
}
