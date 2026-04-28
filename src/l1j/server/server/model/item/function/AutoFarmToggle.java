package l1j.server.server.model.item.function;

import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.afk.AfkMagicRegistry;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.executor.ItemExecutorAdapter;
import l1j.server.server.serverpackets.S_NPCTalkReturn;
import l1j.server.server.afk.AfkService;
import l1j.server.server.serverpackets.S_SystemMessage;

/**
 * 掛機啟動器：點擊顯示「autofarm-c」對話檔。
 * - 直接送出 S_NPCTalkReturn，無需 NPC。
 * - 預設用玩家自己的 objId；部分客戶端/分支可用 0 也行。
 */
public class AutoFarmToggle extends ItemExecutorAdapter {
	private static final Logger _log = Logger.getLogger(AutoFarmToggle.class.getName());

	private static final String TALK_ID = "autofarm";

	public static AutoFarmToggle get() {
		return new AutoFarmToggle();
	}

	@Override
	public void execute(int objectId, L1PcInstance pc, L1ItemInstance item) {
        if (pc == null) { return; }
        try {
            // 只顯示使用者要求的提示文字
            l1j.server.server.afk.AfkService.greet(pc);
            // 打開對話檔（使用新版 talk id）
            pc.sendPackets(new S_NPCTalkReturn(pc.getId(), "autofarm"));
        } catch (Exception e) {
            _log.log(Level.WARNING, "AutoFarmToggle open talk failed: autofarm", e);
            try { pc.sendPackets(new S_NPCTalkReturn(0, "autofarm")); } catch (Exception ignore) {}
        }
}
}