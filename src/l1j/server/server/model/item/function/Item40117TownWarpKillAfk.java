package l1j.server.server.model.item.function;

import java.util.logging.Logger;

import l1j.server.server.afk.AfkController;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;

/**
 * 40117：固定傳點 + 立即停掛 + 不消耗
 * - 綁定 etcitem.class_name 為本類別即可，不用改 C_ItemUse。
 * - 若你的 L1Teleport 方法簽名不同，請把下方呼叫替換成你專案的實際方法。
 */
public class Item40117TownWarpKillAfk {
	private static final Logger _log = Logger.getLogger(Item40117TownWarpKillAfk.class.getName());

	public boolean use(final L1PcInstance pc, final L1ItemInstance item) {
		if (pc == null || item == null) {
			return false;
		}
		// 不消耗：保持 1 張
		try {
			if (item.getCount() != 1) {
				item.setCount(1);
			}
		} catch (Throwable ignore) {}

		// 立即停掛（總開關 OFF）
		try {
			AfkController.forceOff(pc);
		} catch (Throwable t) {
			_log.warning("40117 KillSwitch: AfkController.forceOff failed: " + t);
		}

		// 固定傳點（若你專案簽名不同，請替換成正確方法）
		final int x = 33082;
		final int y = 33389;
		final short mapId = 4;
		final int heading = pc.getHeading();
		try {
			L1Teleport.teleport(pc, x, y, mapId, heading, true);
		} catch (Throwable t) {
			_log.warning("40117 Fixed Teleport failed: " + t);
			return false;
		}
		return true;
	}
}
