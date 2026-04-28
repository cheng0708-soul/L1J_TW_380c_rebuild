package l1j.server.server.priest;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;

/**
 * 祭司面板控制器（相容舊簽名）。
 * 顯示值一律來自 character_priest_iq：displayIq = base_iq + iq_bonus（以 item_objid 為 key）。
 */
public final class PriestDialogController {

    private PriestDialogController() {}

    public static void open(L1PcInstance pc, L1ItemInstance boundItem,
                            int currentMp, int healPct, boolean supportOn, String displayIq) {
        if (displayIq == null) displayIq = "0";
        render(pc, boundItem, currentMp, healPct, supportOn, displayIq);
    }

    public static void open(L1PcInstance pc, L1ItemInstance boundItem,
                            int currentMp, int healPct, boolean supportOn) {
        String displayIq = String.valueOf(fetchDisplayIqFromDB(boundItem));
        render(pc, boundItem, currentMp, healPct, supportOn, displayIq);
    }

    public static void open(L1PcInstance pc, L1ItemInstance boundItem,
                            int currentMp, boolean supportOn, String displayIq) {
        if (displayIq == null) displayIq = "0";
        render(pc, boundItem, currentMp, 50, supportOn, displayIq);
    }

    public static void open(L1PcInstance pc, L1ItemInstance boundItem,
                            int currentMp, boolean supportOn) {
        String displayIq = String.valueOf(fetchDisplayIqFromDB(boundItem));
        render(pc, boundItem, currentMp, 50, supportOn, displayIq);
    }

    private static void render(L1PcInstance pc, L1ItemInstance boundItem,
                               int currentMp, int healPct, boolean supportOn, String displayIq) {
        if (pc == null) return;
        Map<String,String> vars = new HashMap<String,String>();
        vars.put("#0", displayIq);
        vars.put("#1", String.valueOf(Math.max(0, healPct)));
        vars.put("#2", supportOn ? "開啟" : "關閉");
        vars.put("#3", String.valueOf(Math.max(0, currentMp)));
        sendHtmlWithVars(pc, "priest_doll.html", vars, boundItem);
    }

    private static int fetchDisplayIqFromDB(L1ItemInstance bound) {
        if (bound == null) return 0;
        int itemObjId;
        try { itemObjId = (int) bound.getId(); } catch (Throwable t) { return 0; }
        int base = 0, bonus = 0;
        try (Connection con = L1DatabaseFactory.getInstance().getConnection();
             PreparedStatement ps = con.prepareStatement(
                 "SELECT base_iq, iq_bonus FROM character_priest_iq WHERE item_objid=?")) {
            ps.setInt(1, itemObjId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) { base = rs.getInt(1); bonus = rs.getInt(2); }
            }
        } catch (Throwable ignore) {}
        return Math.max(0, base + bonus);
    }

    private static void sendHtmlWithVars(L1PcInstance pc, String file,
                                         Map<String,String> vars, L1ItemInstance bound) {
        try {
            String[] candidates = new String[] {
                "l1j.server.server.serverpackets.S_NPCTalkReturn",
                "l1j.server.server.serverpackets.S_NPCTalk",
                "l1j.server.server.serverpackets.S_ShowHTML"
            };
            Object packet = null;
            for (String className : candidates) {
                Class<?> cls = null;
                try { cls = Class.forName(className); } catch (Throwable ignore) {}
                if (cls == null) continue;
                try { packet = cls.getConstructor(String.class).newInstance(file); } catch (Throwable ignore) {}
                if (packet == null) {
                    try {
                        int objId = (bound != null) ? (int) bound.getId() : 0;
                        packet = cls.getConstructor(int.class, String.class).newInstance(objId, file);
                    } catch (Throwable ignore) {}
                }
                if (packet != null) break;
            }
            if (packet == null) return;
            for (Map.Entry<String,String> e : vars.entrySet()) {
                String k = e.getKey(), v = e.getValue();
                try { packet.getClass().getMethod("setVariable", String.class, String.class).invoke(packet, k, v); }
                catch (Throwable t1) {
                    try { packet.getClass().getMethod("add", String.class, String.class).invoke(packet, k, v); }
                    catch (Throwable t2) {
                        try { packet.getClass().getMethod("putVar", String.class, String.class).invoke(packet, k, v); }
                        catch (Throwable t3) {
                            try { packet.getClass().getMethod("replace", String.class, String.class).invoke(packet, k, v); }
                            catch (Throwable t4) {}
                        }
                    }
                }
            }
            try {
                pc.getClass().getMethod("sendPackets", Class.forName("l1j.server.server.serverpackets.ServerBasePacket")).invoke(pc, packet);
            } catch (Throwable e) {
                pc.getClass().getMethod("sendPackets", Object.class).invoke(pc, packet);
            }
        } catch (Throwable ignore) {}
    }
}