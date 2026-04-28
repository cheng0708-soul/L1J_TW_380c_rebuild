package l1j.server.server.model.item.function;

import java.util.logging.Logger;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
// 正確引入抽象基底
import l1j.server.server.model.item.executor.ItemExecutorAdapter;

public class ScrollOfHome extends ItemExecutorAdapter {
	private static final Logger _log = Logger.getLogger(ScrollOfHome.class.getName());

	@Override
	public void execute(int objectId, L1PcInstance pc, L1ItemInstance item) {
		if (pc == null) {
			return;
		}
		// TODO: 實際傳送邏輯請接到 L1Teleport / L1TownLocation
		_log.fine("ScrollOfHome used by " + pc.getName() + " (skeleton; please wire teleport logic).");
	}
}
