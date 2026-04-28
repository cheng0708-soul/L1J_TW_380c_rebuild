package l1j.server.server.command.executor;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;

// Safe stub: disables the personal-shop currency command without touching other systems.
public class CommandShopCurrency {
    public void execute(L1PcInstance pc, String cmdName, String arg) {
        if (pc != null) {
            pc.sendPackets(new S_SystemMessage("個人商店幣別系統已停用。"));
        }
    }
}