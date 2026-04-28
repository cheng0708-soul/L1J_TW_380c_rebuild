package l1j.server.server.model.item.function;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.priest.PriestItemNameCompat;

/** 智力成長藥劑：只影響當前祭司（以 item_objid 為鍵），並更新名稱的 [智+N]。 */
public final class PriestIQPotion {

    public static boolean consume(L1PcInstance pc, L1ItemInstance boundSummonItem, int inc) {
        if (pc == null || boundSummonItem == null) return false;
        final int itemObjId = (int) boundSummonItem.getId();
        final String baseName = (boundSummonItem.getItem() != null) ? boundSummonItem.getItem().getName() : "祭司";

        int serial = 0, bonus = 0, baseIq = 0;
        try (Connection con = L1DatabaseFactory.getInstance().getConnection()) {
            try (PreparedStatement ins = con.prepareStatement(
                "INSERT IGNORE INTO character_priest_iq (item_objid, iq_bonus, iq_total, base_iq) VALUES (?,0,0,0)"
            )) {
                ins.setInt(1, itemObjId);
                ins.executeUpdate();
            } catch (Throwable ignore) {}

            try (PreparedStatement up = con.prepareStatement(
                "UPDATE character_priest_iq SET iq_bonus=iq_bonus+?, iq_total=base_iq+iq_bonus WHERE item_objid=?"
            )) {
                up.setInt(1, inc);
                up.setInt(2, itemObjId);
                up.executeUpdate();
            }

            try (PreparedStatement q = con.prepareStatement(
                "SELECT priest_unique_id, iq_bonus, base_iq FROM character_priest_iq WHERE item_objid=?"
            )) {
                q.setInt(1, itemObjId);
                try (ResultSet rs = q.executeQuery()) {
                    if (rs.next()) {
                        serial = rs.getInt(1);
                        bonus  = rs.getInt(2);
                        baseIq = rs.getInt(3);
                    }
                }
            }
        } catch (Throwable ignore) {}

        String newName = String.format("%s[%d][智+%d]", baseName, serial, bonus);
        PriestItemNameCompat.tryRenameAndSave(pc, boundSummonItem, newName);
        return true;
    }

    private PriestIQPotion() {}
}