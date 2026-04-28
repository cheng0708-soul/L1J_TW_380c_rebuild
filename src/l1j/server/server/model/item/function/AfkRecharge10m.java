package l1j.server.server.model.item.function;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.executor.ItemExecutorAdapter;

public class AfkRecharge10m extends ItemExecutorAdapter {
    public static AfkRecharge10m get() { return new AfkRecharge10m(); }
    @Override
    public void execute(int data, L1PcInstance pc, L1ItemInstance item) {
        // Stub
    }
}
