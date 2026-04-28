package l1j.server.server.model.item.executor;

import l1j.server.server.datatables.TreasureBoxTable;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;

public final class TreasureBoxExecutor {
    private TreasureBoxExecutor() {}
    public static void open(L1PcInstance pc, L1ItemInstance boxItem) {
        TreasureBoxTable.getInstance().openAndGive(pc, boxItem);
    }
}
