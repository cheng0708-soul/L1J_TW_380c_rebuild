
package l1j.server.server.datatables;

import java.sql.*;
import java.util.List;
import l1j.server.L1DatabaseFactory;

public class PShopItemTable {
    private static final PShopItemTable _instance = new PShopItemTable();
    public static PShopItemTable getInstance() { return _instance; }

    public void replaceItems(int stallId, List<?> sellList, List<?> buyList) {
        if (stallId <= 0) return;
        try (Connection con = L1DatabaseFactory.getInstance().getConnection()) {
            try (PreparedStatement del = con.prepareStatement("DELETE FROM pshop_items WHERE stall_id=?")) {
                del.setInt(1, stallId);
                del.executeUpdate();
            }
            try (PreparedStatement ins = con.prepareStatement(
                "INSERT INTO pshop_items (stall_id, item_id, sell_price, sell_count, buy_price, buy_count) VALUES (?,?,?,?,?,?)")) {
                ins.setInt(1, stallId);
                ins.setInt(2, 40000);
                ins.setInt(3, 0);
                ins.setInt(4, 0);
                ins.setInt(5, 0);
                ins.setInt(6, 0);
                ins.executeUpdate();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
