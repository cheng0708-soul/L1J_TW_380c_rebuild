package l1j.server.server.model.item.function;

import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.executor.ItemExecutorAdapter;
import l1j.server.server.serverpackets.S_NPCTalkReturn;
import l1j.server.server.serverpackets.S_SystemMessage;

/**
 * 240152 跟隨協同系統：打開 cooperate 對話檔。
 * 將此 executor 掛到道具 ID=240152 即可。
 */
public class CoopFollowToggle extends ItemExecutorAdapter {

    private static final Logger _log = Logger.getLogger(CoopFollowToggle.class.getName());

    public static CoopFollowToggle get() {
        return new CoopFollowToggle();
    }

    @Override
    public void execute(int objectId, L1PcInstance pc, L1ItemInstance item) {
        if (pc == null) return;
        try {
            l1j.server.server.afk.CoopFollowService.openMainUi(pc);
        } catch (Exception e) {
            _log.log(Level.WARNING, "CoopFollowToggle open talk failed: cooperate", e);
            try {
                pc.sendPackets(new S_NPCTalkReturn(pc.getId(), "cooperate"));
            } catch (Exception ignore) {}
            try {
                pc.sendPackets(new S_SystemMessage("無法開啟跟隨協同系統對話檔。"));
            } catch (Exception ignore) {}
        }
    }
}
