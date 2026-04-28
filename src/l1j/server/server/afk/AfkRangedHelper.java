package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SkillSound;

/**
 * Utilities related to ranged-attack feedback while AFK.
 */
public final class AfkRangedHelper {
    private AfkRangedHelper() {}

    /**
     * Plays a client-side skill/attack sound on the player and broadcasts it.
     * Replaces the incorrect call to AfkPacketUtil.send1(pc, new C_SkillSound(...)).
     */
    public static void playHitFx(final L1PcInstance pc, final int gfxId) {
        final S_SkillSound pkt = new S_SkillSound(pc.getId(), gfxId);
        pc.sendPackets(pkt);
        pc.broadcastPacket(pkt);
    }
}
