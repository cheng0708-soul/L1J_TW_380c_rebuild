package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.serverpackets.ServerBasePacket;

/** Backward-compatible shim. */
public final class AkPacketUtil {
    private AkPacketUtil() {}

    public static void send(L1PcInstance pc, ServerBasePacket p) {
        if (pc == null || p == null) return;
        try { pc.sendPackets(p); } catch (Throwable ignore) {}
    }

    public static void debug(L1PcInstance pc, String text) {
        if (pc == null || text == null) return;
        try { pc.sendPackets(new S_SystemMessage(text)); } catch (Throwable ignore) {}
    }
}