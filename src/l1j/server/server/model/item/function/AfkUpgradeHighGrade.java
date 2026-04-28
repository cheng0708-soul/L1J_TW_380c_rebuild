package l1j.server.server.model.item.function;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.executor.ItemExecutorAdapter;

public class AfkUpgradeHighGrade extends ItemExecutorAdapter {
    public static AfkUpgradeHighGrade get() { return new AfkUpgradeHighGrade(); }
    @Override
    public void execute(int data, L1PcInstance pc, L1ItemInstance item) {
        // Stub
    }
}
