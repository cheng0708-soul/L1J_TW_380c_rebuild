package l1j.server.server.priest;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.datatables.ItemTable;
import l1j.server.server.templates.L1Item;
import l1j.server.server.utils.SQLUtil;

/**
 * 祭司智力成長資料 DAO（第二版）
 *
 * 設計重點：
 *   1. 每一顆祭司召喚道具 (item_objid) 對應一筆獨立紀錄，不再以 owner+priest_item_id 共用。
 *   2. 若資料表有 item_objid 欄位，所有查詢與更新都以 item_objid 為主 key。
 *   3. 仍保留 byOwnerAndItem(...) 以兼容舊邏輯，但在有 item_objid 欄位時不再由這裡插入新資料。
 */
public final class PriestIqDAO2 {

    public static final class Row {
        public int priestUniqueId;
        public Integer itemObjId;       // 可能為 null（舊資料）
        public int priestItemId;
        public int ownerObjId;
        public int upgradeStage;
        public int iqBonus;
        public int iqTotal;
        public String priestName;
        public int baseIq;
        public int autoSupportEnabled;
        public int healThresholdPct;
    }

    private static final String T = "character_priest_iq";
    private static final boolean HAS_ITEM_OBJID = hasItemObjIdColumn();
    public static final int MAX_IQ_BONUS = 100;
    public static boolean hasItemObjId() {
        return HAS_ITEM_OBJID;
    }

    private PriestIqDAO2() {
    }

    private static boolean hasItemObjIdColumn() {
        Connection c = null;
        ResultSet rs = null;
        try {
            c = L1DatabaseFactory.getInstance().getConnection();
            DatabaseMetaData md = c.getMetaData();
            rs = md.getColumns(null, null, T, "item_objid");
            return rs.next();
        } catch (SQLException e) {
            return false;
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(c);
        }
    }

    /** 依 item_objid 取得一筆紀錄（每一顆道具一筆）。 */
    public static Row byItemObjId(int itemObjId) {
        if (!HAS_ITEM_OBJID) {
            return null;
        }
        String sql =
            "SELECT priest_unique_id,item_objid,priest_item_id,owner_objid,upgrade_stage,"
          + "iq_bonus,iq_total,priest_name,base_iq,auto_support_enabled,heal_threshold_pct "
          + "FROM " + T + " WHERE item_objid=?";
        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = L1DatabaseFactory.getInstance().getConnection();
            ps = c.prepareStatement(sql);
            ps.setInt(1, itemObjId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(ps);
            SQLUtil.close(c);
        }
        return null;
    }

    /**
     * 依 owner+priest_item_id 找一筆紀錄。
     * 只做兼容用途；在 HAS_ITEM_OBJID=true 的情況下，不再用這個方法來建立新資料。
     */
    public static Row byOwnerAndItem(int ownerObjId, int priestItemId) {
        String sql;
        if (HAS_ITEM_OBJID) {
            sql =
                "SELECT priest_unique_id,item_objid,priest_item_id,owner_objid,upgrade_stage,"
              + "iq_bonus,iq_total,priest_name,base_iq,auto_support_enabled,heal_threshold_pct "
              + "FROM " + T + " WHERE owner_objid=? AND priest_item_id=? "
              + "ORDER BY priest_unique_id ASC LIMIT 1";
        } else {
            sql =
                "SELECT priest_unique_id,null AS item_objid,priest_item_id,owner_objid,upgrade_stage,"
              + "iq_bonus,iq_total,priest_name,base_iq,auto_support_enabled,heal_threshold_pct "
              + "FROM " + T + " WHERE owner_objid=? AND priest_item_id=? "
              + "ORDER BY priest_unique_id ASC LIMIT 1";
        }

        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = L1DatabaseFactory.getInstance().getConnection();
            ps = c.prepareStatement(sql);
            ps.setInt(1, ownerObjId);
            ps.setInt(2, priestItemId);
            rs = ps.executeQuery();
            if (rs.next()) {
                return map(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(ps);
            SQLUtil.close(c);
        }
        return null;
    }

    /**
     * 確保「這一顆祭司道具」有一筆紀錄：
     *  1) 若有 item_objid 欄位 → 只看 item_objid，存在就直接回傳。
     *  2) 若沒有 item_objid 欄位 → 回退為 owner+priest_item_id。
     */
    public static Row ensure(int ownerObjId, int itemObjId, int priestItemId, String priestName, int baseIq) {
        if (HAS_ITEM_OBJID) {
            Row r = byItemObjId(itemObjId);
            if (r != null) {
                return r;
            }
        } else {
            Row r = byOwnerAndItem(ownerObjId, priestItemId);
            if (r != null) {
                return r;
            }
        }

        if (priestName == null) {
            priestName = "";
        }
        if (priestName.length() > 32) {
            priestName = priestName.substring(0, 32);
        }

        String sql;
        if (HAS_ITEM_OBJID) {
            sql =
                "INSERT INTO " + T + " (item_objid,priest_item_id,priest_name,upgrade_stage,"
              + "iq_bonus,iq_total,auto_support_enabled,heal_threshold_pct,owner_objid,base_iq) "
              + "VALUES (?,?,?,?,?,?,1,50,?,?)";
        } else {
            sql =
                "INSERT INTO " + T + " (priest_item_id,priest_name,upgrade_stage,"
              + "iq_bonus,iq_total,auto_support_enabled,heal_threshold_pct,owner_objid,base_iq) "
              + "VALUES (?,?,?,?,?,1,50,?,?)";
        }

        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = L1DatabaseFactory.getInstance().getConnection();
            ps = c.prepareStatement(sql);
            int i = 1;
            if (HAS_ITEM_OBJID) {
                ps.setInt(i++, itemObjId);
            }
            ps.setInt(i++, priestItemId);
            ps.setString(i++, priestName);
            ps.setInt(i++, 1);          // upgrade_stage 初始 1
            ps.setInt(i++, 0);          // iq_bonus 初始 0（尚未吃藥）
            ps.setInt(i++, baseIq);     // iq_total 初始 = baseIq
            ps.setInt(i++, ownerObjId);
            ps.setInt(i++, baseIq);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(c);
        }

        if (HAS_ITEM_OBJID) {
            return byItemObjId(itemObjId);
        } else {
            return byOwnerAndItem(ownerObjId, priestItemId);
        }
    }

    // 相容用 overload
    public static Row ensure(int ownerObjId, int itemObjId, int priestItemId, int baseIq) {
        return ensure(ownerObjId, itemObjId, priestItemId, "祭司", baseIq);
    }

    public static Row ensure(int ownerObjId, int itemObjId, String priestName, int baseIq) {
        return ensure(ownerObjId, itemObjId, 0, priestName, baseIq);
    }

    public static Row ensure(int ownerObjId, int itemObjId, int baseIq) {
        return ensure(ownerObjId, itemObjId, 0, "祭司", baseIq);
    }

    /**
     * 增加指定祭司的智力加成。
     *
     * 若有 item_objid 欄位，UPDATE / SELECT 都以 item_objid 為主；
     * ownerObjId 與 priestItemId 只作為舊資料表的退路。
     */
    /**
     * 增加指定祭司的智力加成（有上限 MAX_IQ_BONUS）。
     *
     * 若有 item_objid 欄位，UPDATE / SELECT 都以 item_objid 為主；
     * ownerObjId 與 priestItemId 只作為舊資料表的退路。
     */
    public static Row addIq(int ownerObjId, int itemObjId, int priestItemId, int delta) {
        if (delta <= 0) {
            return HAS_ITEM_OBJID ? byItemObjId(itemObjId) : byOwnerAndItem(ownerObjId, priestItemId);
        }

        // 先取現有資料
        Row row = HAS_ITEM_OBJID ? byItemObjId(itemObjId) : byOwnerAndItem(ownerObjId, priestItemId);

        // 若沒有資料，先 ensure 一筆（避免 UPDATE 0 rows）
        if (row == null) {
            int baseIq = 10;
            String priestName = "祭司";
            try {
                if (priestItemId != 0) {
                    L1Item tpl = ItemTable.getInstance().getTemplate(priestItemId);
                    if (tpl != null) {
                        priestName = tpl.getName();
                        int b = NpcIntelResolver.getBaseIntByItemAndName(priestItemId, priestName);
                        if (b > 0) baseIq = b;
                    }
                }
            } catch (Throwable ignore) {}

            ensure(ownerObjId, itemObjId, priestItemId, priestName, baseIq);
            row = HAS_ITEM_OBJID ? byItemObjId(itemObjId) : byOwnerAndItem(ownerObjId, priestItemId);
            if (row == null) return null;
        }

        // 已達上限 → 不再增加
        if (row.iqBonus >= MAX_IQ_BONUS) {
            return row;
        }

        // 這次實際能加多少（封頂）
        int applied = Math.min(delta, MAX_IQ_BONUS - row.iqBonus);
        if (applied <= 0) {
            return row;
        }

        String sql;
        if (HAS_ITEM_OBJID) {
            sql = "UPDATE " + T
                + " SET iq_bonus = iq_bonus + ?, iq_total = iq_total + ?"
                + " WHERE item_objid = ?";
        } else {
            sql = "UPDATE " + T
                + " SET iq_bonus = iq_bonus + ?, iq_total = iq_total + ?"
                + " WHERE owner_objid = ? AND priest_item_id = ?";
        }

        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = L1DatabaseFactory.getInstance().getConnection();
            ps = c.prepareStatement(sql);
            int i = 1;
            ps.setInt(i++, applied);
            ps.setInt(i++, applied);
            if (HAS_ITEM_OBJID) {
                ps.setInt(i++, itemObjId);
            } else {
                ps.setInt(i++, ownerObjId);
                ps.setInt(i++, priestItemId);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(c);
        }

        return HAS_ITEM_OBJID ? byItemObjId(itemObjId) : byOwnerAndItem(ownerObjId, priestItemId);
    }


    /** 依 item 或 owner 設定 iq_total（目前主要給補血邏輯使用）。 */
    public static void setTotalByItemOrOwner(int ownerObjId, int itemObjId, int priestItemId, int total) {
        String sql;
        if (HAS_ITEM_OBJID) {
            sql = "UPDATE " + T + " SET iq_total=? WHERE item_objid=?";
        } else {
            sql = "UPDATE " + T + " SET iq_total=? WHERE owner_objid=? AND priest_item_id=?";
        }

        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = L1DatabaseFactory.getInstance().getConnection();
            ps = c.prepareStatement(sql);
            int i = 1;
            ps.setInt(i++, total);
            if (HAS_ITEM_OBJID) {
                ps.setInt(i++, itemObjId);
            } else {
                ps.setInt(i++, ownerObjId);
                ps.setInt(i++, priestItemId);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(c);
        }
    }

    /** 取出 iq_total；若無資料回傳 null。 */
    public static Integer getTotalByItemOrOwner(int ownerObjId, Integer itemObjId, int priestItemId) {
        String sql;
        if (HAS_ITEM_OBJID) {
            sql = "SELECT iq_total FROM " + T + " WHERE item_objid=?";
        } else {
            sql = "SELECT iq_total FROM " + T + " WHERE owner_objid=? AND priest_item_id=?";
        }

        Connection c = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            c = L1DatabaseFactory.getInstance().getConnection();
            ps = c.prepareStatement(sql);
            if (HAS_ITEM_OBJID) {
                if (itemObjId == null) {
                    return null;
                }
                ps.setInt(1, itemObjId.intValue());
            } else {
                ps.setInt(1, ownerObjId);
                ps.setInt(2, priestItemId);
            }
            rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(ps);
            SQLUtil.close(c);
        }
        return null;
    }

    private static Row map(ResultSet rs) throws SQLException {
        Row r = new Row();
        int i = 1;
        r.priestUniqueId = rs.getInt(i++);
        try {
            r.itemObjId = (Integer) rs.getObject(i++);
        } catch (SQLException e) {
            r.itemObjId = null;
            i++;
        }
        r.priestItemId = rs.getInt(i++);
        r.ownerObjId = rs.getInt(i++);
        r.upgradeStage = rs.getInt(i++);
        r.iqBonus = rs.getInt(i++);
        r.iqTotal = rs.getInt(i++);
        r.priestName = rs.getString(i++);
        try {
            r.baseIq = rs.getInt(i++);
        } catch (SQLException e) {
            r.baseIq = 0;
            i++;
        }
        try {
            r.autoSupportEnabled = rs.getInt(i++);
        } catch (SQLException e) {
            r.autoSupportEnabled = 1;
            i++;
        }
        try {
            r.healThresholdPct = rs.getInt(i++);
        } catch (SQLException e) {
            r.healThresholdPct = 50;
            i++;
        }
        return r;
    }


    /**
     * 升階時呼叫的相容用接口。
     *
     * 目前僅作為佔位：在有 item_objid 欄位的情況下，因為同一顆祭司道具
     * 在升階前後仍然使用相同的 item_objid，因此不需要在這裡搬移資料。
     * 如需更精細的升階行為，可以在這裡依照 oldPriestItemId / newPriestItemId
     * 重新設定 base_iq 或其他欄位。
     */
    /**
     * 升階時呼叫（item_objid 欄位存在時）。
     * 會更新 priest_item_id / upgrade_stage / base_iq / iq_total，
     * 並把 iq_bonus 繼承到新階級的基礎智力上。
     */
    public static void onUpgrade(int itemObjId, int newPriestItemId, int newStage) {
        if (!HAS_ITEM_OBJID) {
            // 舊表結構請改呼叫 legacy overload
            return;
        }

        Row row = byItemObjId(itemObjId);
        if (row == null) {
            // 沒有綁定資料通常代表從未吃過智力藥水，留給之後 ensure 再建立
            return;
        }

        int baseIq = determineBaseIqByStage(newStage, newPriestItemId);
        int total = Math.max(0, baseIq + row.iqBonus);

        String newName = null;
        try {
            L1Item tpl = ItemTable.getInstance().getTemplate(newPriestItemId);
            if (tpl != null) newName = tpl.getName();
        } catch (Throwable ignore) {}

        String sql =
            "UPDATE " + T + " SET priest_item_id=?, upgrade_stage=?, base_iq=?, iq_total=?"
          + (newName != null ? ", priest_name=?" : "")
          + " WHERE item_objid=?";

        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = L1DatabaseFactory.getInstance().getConnection();
            ps = c.prepareStatement(sql);
            int i = 1;
            ps.setInt(i++, newPriestItemId);
            ps.setInt(i++, newStage);
            ps.setInt(i++, baseIq);
            ps.setInt(i++, total);
            if (newName != null) {
                ps.setString(i++, newName);
            }
            ps.setInt(i++, itemObjId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(c);
        }
    }

    /**
     * 升階時呼叫（舊表結構：沒有 item_objid 欄位時）。
     * 需要 ownerObjId 與 oldPriestItemId 才能搬移綁定資料。
     */
    public static void onUpgrade(int ownerObjId, int oldPriestItemId, int newPriestItemId, int newStage) {
        if (HAS_ITEM_OBJID) {
            // 新表結構不用走這條
            return;
        }

        Row row = byOwnerAndItem(ownerObjId, oldPriestItemId);
        if (row == null) return;

        int baseIq = determineBaseIqByStage(newStage, newPriestItemId);
        int total = Math.max(0, baseIq + row.iqBonus);

        String newName = null;
        try {
            L1Item tpl = ItemTable.getInstance().getTemplate(newPriestItemId);
            if (tpl != null) newName = tpl.getName();
        } catch (Throwable ignore) {}

        String sql =
            "UPDATE " + T + " SET priest_item_id=?, upgrade_stage=?, base_iq=?, iq_total=?"
          + (newName != null ? ", priest_name=?" : "")
          + " WHERE owner_objid=? AND priest_item_id=?";

        Connection c = null;
        PreparedStatement ps = null;
        try {
            c = L1DatabaseFactory.getInstance().getConnection();
            ps = c.prepareStatement(sql);
            int i = 1;
            ps.setInt(i++, newPriestItemId);
            ps.setInt(i++, newStage);
            ps.setInt(i++, baseIq);
            ps.setInt(i++, total);
            if (newName != null) {
                ps.setString(i++, newName);
            }
            ps.setInt(i++, ownerObjId);
            ps.setInt(i++, oldPriestItemId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            SQLUtil.close(ps);
            SQLUtil.close(c);
        }
    }

    /** 依新階級決定 base_iq（10/12/14/16/18）。 */
    private static int determineBaseIqByStage(int stage, int priestItemId) {
        switch (stage) {
            case 1: return 10; // 低階
            case 2: return 12; // 中階
            case 3: return 14; // 高階
            case 4: return 16; // 頂階
            case 5: return 18; // 神話
            default:
                try {
                    L1Item tpl = ItemTable.getInstance().getTemplate(priestItemId);
                    if (tpl != null) {
                        String n = tpl.getName();
                        if (n != null) {
                            if (n.contains("低階")) return 10;
                            if (n.contains("中階")) return 12;
                            if (n.contains("高階")) return 14;
                            if (n.contains("頂階")) return 16;
                            if (n.contains("神話")) return 18;
                        }
                        int fallback = NpcIntelResolver.getBaseIntByItemAndName(priestItemId, n);
                        if (fallback > 0) return fallback;
                    }
                } catch (Throwable ignore) {}
                return 10;
        }
    }
 }
