package l1j.server.server.model.shop;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.utils.SQLUtil;

public class PShopDB {

    public static final PShopDB getInstance() { return new PShopDB(); }

    // ===== stalls =====
    public int createStall(L1PcInstance owner, int modeItemId, int npcObjId, int npcTemplateId, String desc) {
        String sql = "INSERT INTO pshop_stalls(account_name,char_id,mode_item_id,npc_objid,npc_templateid,stall_desc,status,escrow_amount)"
                   + " VALUES(?,?,?,?,?,?,1,0)";
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            ps = con.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, owner.getAccountName());
            ps.setInt(2, owner.getId());
            ps.setInt(3, modeItemId);
            ps.setInt(4, npcObjId);
            ps.setInt(5, npcTemplateId);
            ps.setString(6, desc == null ? "" : desc);
            ps.executeUpdate();
            rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(ps);
            SQLUtil.close(con);
        }
        return 0;
    }

    public void closeStall(int stallId) {
        String sql = "UPDATE pshop_stalls SET status=0 WHERE stall_id=?";
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, stallId);
            ps.executeUpdate();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void addEscrow(int stallId, int delta) {
        String sql = "UPDATE pshop_stalls SET escrow_amount=escrow_amount+? WHERE stall_id=?";
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, Math.max(0, delta));
            ps.setInt(2, stallId);
            ps.executeUpdate();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public boolean subEscrowIfEnough(int stallId, int cost) {
        String sql = "UPDATE pshop_stalls SET escrow_amount=escrow_amount-? WHERE stall_id=? AND escrow_amount>=?";
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, cost);
            ps.setInt(2, stallId);
            ps.setInt(3, cost);
            return ps.executeUpdate() > 0;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }

    public int findStallIdByNpcObjId(int npcObjId) {
        String sql = "SELECT stall_id FROM pshop_stalls WHERE npc_objid=? AND status=1";
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, npcObjId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return 0;
    }

    // ===== items =====
    public void insertSellItem(int stallId, int itemId, int bless, int enchant, int attrEnchant, int price, int count, int ownerItemObjId) {
        String sql = "INSERT INTO pshop_items(stall_id,item_id,bless,enchant_lvl,attr_enchant,sell_price,sell_count,owner_item_objid)"
                   + " VALUES(?,?,?,?,?,?,?,?)";
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i=1;
            ps.setInt(i++, stallId);
            ps.setInt(i++, itemId);
            ps.setInt(i++, bless);
            ps.setInt(i++, enchant);
            ps.setInt(i++, attrEnchant);
            ps.setInt(i++, price);
            ps.setInt(i++, count);
            ps.setInt(i++, ownerItemObjId);
            ps.executeUpdate();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void insertBuyItem(int stallId, int itemId, int bless, int enchant, int attrEnchant, int price, int count) {
        String sql = "INSERT INTO pshop_items(stall_id,item_id,bless,enchant_lvl,attr_enchant,buy_price,buy_count)"
                   + " VALUES(?,?,?,?,?,?,?)";
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            int i=1;
            ps.setInt(i++, stallId);
            ps.setInt(i++, itemId);
            ps.setInt(i++, bless);
            ps.setInt(i++, enchant);
            ps.setInt(i++, attrEnchant);
            ps.setInt(i++, price);
            ps.setInt(i++, count);
            ps.executeUpdate();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static final class SellRow {
        public int id, item_id, bless, enchant, attr, price, count, owner_objid;
    }

    public List<SellRow> listSellItems(int stallId) {
        String sql = "SELECT id,item_id,bless,enchant_lvl,attr_enchant,sell_price,sell_count,owner_item_objid"
                   + " FROM pshop_items WHERE stall_id=? AND sell_count>0";
        List<SellRow> out = new ArrayList<>();
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, stallId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SellRow r = new SellRow();
                    r.id = rs.getInt(1);
                    r.item_id = rs.getInt(2);
                    r.bless = rs.getInt(3);
                    r.enchant = rs.getInt(4);
                    r.attr = rs.getInt(5);
                    r.price = rs.getInt(6);
                    r.count = rs.getInt(7);
                    r.owner_objid = rs.getInt(8);
                    out.add(r);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return out;
    }

    public boolean decSellCount(int rowId, int n) {
        String sql = "UPDATE pshop_items SET sell_count=sell_count-? WHERE id=? AND sell_count>=?";
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, n);
            ps.setInt(2, rowId);
            ps.setInt(3, n);
            return ps.executeUpdate() > 0;
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
    }
}
