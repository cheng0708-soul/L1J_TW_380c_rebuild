package l1j.server.server.priest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;

/**
 * 入口：
 * - 首次召喚即定案 base_iq（10/12/14/16/18），建立列（key=item_objid）。
 * - 當場改名：原名[編號X][智+Y]。
 * - 面板只顯示 base_iq + iq_bonus（不讀 NPC 表）。
 */
public final class PriestHtmlHook {

    private PriestHtmlHook() {}

    public static boolean tryOpen(L1PcInstance pc, L1NpcInstance npc, L1ItemInstance bound) {
        if (pc == null) return false;

        Integer itemObjId = null;
        String baseName = "祭司";
        int itemId = 0;
        if (bound != null) {
            try { itemObjId = (int) bound.getId(); } catch (Throwable ignore) {}
            try { baseName = bound.getItem().getName(); } catch (Throwable ignore) {}
            try { itemId = bound.getItemId(); } catch (Throwable ignore) {}
        }

        int baseFromTier = determineBaseIqByTier(itemId, baseName);
        PriestRow row = ensureAndRead(pc, itemObjId, itemId, baseName, baseFromTier);

        if (bound != null) {
            String newName = String.format("%s[%d][智+%d]", baseName, row.serial, row.iqBonus);
            l1j.server.server.priest.PriestItemNameCompat.tryRenameAndSave(pc, bound, newName);
        }

        int displayIq = Math.max(0, row.baseIq + row.iqBonus);
        int mp = 0;
        try { if (npc != null) mp = npc.getCurrentMp(); } catch (Throwable ignore) {}

        l1j.server.server.priest.PriestDialogController.open(pc, bound, mp, row.healThresholdPct, row.autoSupportEnabled == 1, String.valueOf(displayIq));
        return true;
    }

    private static int determineBaseIqByTier(int itemId, String name) {
        String n = (name == null) ? "" : name;
        if (n.contains("低階")) return 10;
        if (n.contains("中階")) return 12;
        if (n.contains("高階")) return 14;
        if (n.contains("頂階")) return 16;
        if (n.contains("神話")) return 18;
        return 10;
    }

    private static class PriestRow {
        int serial;
        int baseIq;
        int iqBonus;
        int healThresholdPct;
        int autoSupportEnabled;
    }

    private static PriestRow ensureAndRead(L1PcInstance pc, Integer itemObjId, int priestItemId, String priestName, int baseIqFromTier) {
        PriestRow r = new PriestRow();
        if (itemObjId == null) {
            r.baseIq = baseIqFromTier;
            r.iqBonus = 0;
            r.healThresholdPct = 50;
            r.autoSupportEnabled = 1;
            r.serial = 0;
            return r;
        }

        try (Connection con = L1DatabaseFactory.getInstance().getConnection()) {
            try (PreparedStatement ins = con.prepareStatement(
                "INSERT IGNORE INTO character_priest_iq " +
                "(item_objid, owner_objid, priest_item_id, priest_name, upgrade_stage, iq_bonus, iq_total, auto_support_enabled, heal_threshold_pct, base_iq) " +
                "VALUES (?, ?, ?, ?, 0, 0, ?, 1, 50, ?)"
            )) {
                ins.setInt(1, itemObjId);
                ins.setInt(2, pc != null ? pc.getId() : 0);
                ins.setInt(3, priestItemId);
                ins.setString(4, priestName);
                ins.setInt(5, baseIqFromTier);
                ins.setInt(6, baseIqFromTier);
                ins.executeUpdate();
            } catch (Throwable ignore) {}

            try (PreparedStatement q = con.prepareStatement(
                "SELECT priest_unique_id, base_iq, iq_bonus, heal_threshold_pct, auto_support_enabled " +
                "FROM character_priest_iq WHERE item_objid=?"
            )) {
                q.setInt(1, itemObjId);
                try (ResultSet rs = q.executeQuery()) {
                    if (rs.next()) {
                        r.serial = rs.getInt(1);
                        r.baseIq = rs.getInt(2);
                        r.iqBonus = rs.getInt(3);
                        r.healThresholdPct = rs.getInt(4);
                        r.autoSupportEnabled = rs.getInt(5);
                    } else {
                        r.serial = 0;
                        r.baseIq = baseIqFromTier;
                        r.iqBonus = 0;
                        r.healThresholdPct = 50;
                        r.autoSupportEnabled = 1;
                    }
                }
            }

            if (r.baseIq <= 0) {
                try (PreparedStatement up = con.prepareStatement(
                    "UPDATE character_priest_iq SET base_iq=?, iq_total=? WHERE item_objid=?"
                )) {
                    up.setInt(1, baseIqFromTier);
                    up.setInt(2, baseIqFromTier + Math.max(0, r.iqBonus));
                    up.setInt(3, itemObjId);
                    up.executeUpdate();
                } catch (Throwable ignore) {}
                r.baseIq = baseIqFromTier;
            }

        } catch (Throwable ignore) {}

        return r;
    }
}