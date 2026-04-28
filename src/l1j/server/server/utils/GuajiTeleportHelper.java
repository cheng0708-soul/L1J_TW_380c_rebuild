package l1j.server.server.utils;

import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.ConfigGuaji;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;

public final class GuajiTeleportHelper {
	private static final Logger _log = Logger.getLogger(GuajiTeleportHelper.class.getName());

	private GuajiTeleportHelper() {}

	public static boolean doTeleport(final L1PcInstance pc) {
		if (pc == null) return false;
		try {
			if (pc.getInventory() == null) return false;
			final int need = Math.max(1, ConfigGuaji.GUAJI_TELE_ITEMCOUNT);
			final int itemId = resolveTeleportItemId(pc, ConfigGuaji.GUAJI_TELE_ITEM, need);
			if (itemId <= 0) {
				pc.sendPackets(new S_SystemMessage("\fR缺少瞬移道具(" + ConfigGuaji.GUAJI_TELE_ITEM + "/140100)"));
				return false;
			}
			pc.getInventory().consumeItem(itemId, need);
			L1Teleport.randomTeleport(pc, true);
			return true;
		} catch (Throwable e) {
			_log.log(Level.SEVERE, e.getLocalizedMessage(), e);
			return false;
		}
	}

	private static int resolveTeleportItemId(final L1PcInstance pc, final int preferredItemId, final int need) {
		if (pc == null || pc.getInventory() == null) return 0;
		if (preferredItemId > 0 && pc.getInventory().checkItem(preferredItemId, need)) {
			return preferredItemId;
		}
		if (preferredItemId == 40100 && pc.getInventory().checkItem(140100, need)) {
			return 140100;
		}
		if (preferredItemId == 140100 && pc.getInventory().checkItem(40100, need)) {
			return 40100;
		}
		if (pc.getInventory().checkItem(40100, need)) {
			return 40100;
		}
		if (pc.getInventory().checkItem(140100, need)) {
			return 140100;
		}
		return 0;
	}
}
