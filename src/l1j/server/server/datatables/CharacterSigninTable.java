package l1j.server.server.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.utils.SQLUtil;

public class CharacterSigninTable {
    private static final Logger _log = Logger.getLogger(CharacterSigninTable.class.getName());
    private static final CharacterSigninTable _instance = new CharacterSigninTable();
    public static CharacterSigninTable getInstance() { return _instance; }

    private CharacterSigninTable() {}

    public static class Record {
        public int charId;
        public String accountName;
        public int signCount;
        public Integer levelAtSignin; // nullable
        public Timestamp cooldownUntil;
        public Timestamp lastSignedAt;
    }

    public Record loadOrCreate(int charId, String accountName) {
        Record r = null;
        Connection con = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement("SELECT char_id, account_name, sign_count, level_at_signin, cooldown_until, last_signed_at FROM character_signin WHERE char_id=?");
            pstm.setInt(1, charId);
            rs = pstm.executeQuery();
            if (rs.next()) {
                r = new Record();
                r.charId = rs.getInt(1);
                r.accountName = rs.getString(2);
                r.signCount = rs.getInt(3);
                int lvl = rs.getInt(4);
                r.levelAtSignin = rs.wasNull() ? null : Integer.valueOf(lvl);
                r.cooldownUntil = rs.getTimestamp(5);
                r.lastSignedAt = rs.getTimestamp(6);
                return r;
            }
        } catch (Exception e) {
            _log.log(Level.SEVERE, "loadOrCreate failed: " + charId, e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
        saveNew(charId, accountName);
        r = new Record();
        r.charId = charId;
        r.accountName = accountName;
        r.signCount = 0;
        r.levelAtSignin = null;
        r.cooldownUntil = null;
        r.lastSignedAt = null;
        return r;
    }

    public void saveNew(int charId, String accountName) {
        Connection con = null;
        PreparedStatement pstm = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement(
                "INSERT INTO character_signin (char_id, account_name, sign_count, level_at_signin, cooldown_until, last_signed_at) VALUES (?, ?, 0, NULL, NULL, NULL)");
            pstm.setInt(1, charId);
            pstm.setString(2, accountName);
            pstm.execute();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "saveNew failed: " + charId, e);
        } finally {
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
    }

    public void update(int charId, int signCount, Integer levelAtSignin, Timestamp cooldownUntil, Timestamp lastSignedAt) {
        Connection con = null;
        PreparedStatement pstm = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement(
                "UPDATE character_signin SET sign_count=?, level_at_signin=?, cooldown_until=?, last_signed_at=? WHERE char_id=?");
            pstm.setInt(1, signCount);
            if (levelAtSignin == null) {
                pstm.setNull(2, java.sql.Types.TINYINT);
            } else {
                pstm.setInt(2, levelAtSignin.intValue());
            }
            pstm.setTimestamp(3, cooldownUntil);
            pstm.setTimestamp(4, lastSignedAt);
            pstm.setInt(5, charId);
            pstm.execute();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "update failed: " + charId, e);
        } finally {
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
    }
}
