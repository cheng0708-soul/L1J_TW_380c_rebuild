
package l1j.server.server.priest;

import java.sql.*;
import l1j.server.L1DatabaseFactory;

public final class NpcIntelResolver {
    private NpcIntelResolver() {}

    public static int getBaseIntByItemAndName(int itemId, String baseName) {
        Integer i = byMap(itemId);
        if (i != null) return i.intValue();
        i = byName(baseName);
        return i != null ? i.intValue() : 0;
    }

    private static Integer byMap(int itemId) {
        String sql = "SELECT n.intel FROM npc n JOIN priest_item_npc_map m ON n.npcid=m.npc_id WHERE m.item_id=?";
        try (Connection c = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignore) {}
        return null;
    }

    private static Integer byName(String baseName) {
        String sql = "SELECT intel FROM npc WHERE name=? LIMIT 1";
        try (Connection c = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, baseName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ignore) {}
        return null;
    }
}
