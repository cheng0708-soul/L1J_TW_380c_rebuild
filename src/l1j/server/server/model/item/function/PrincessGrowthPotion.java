
package l1j.server.server.model.item.function;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.executor.ItemExecutorAdapter;
import l1j.server.server.priest.PriestGrowthService2;
import l1j.server.server.serverpackets.S_SystemMessage;

public final class PrincessGrowthPotion extends ItemExecutorAdapter {
    private static final PrincessGrowthPotion _inst = new PrincessGrowthPotion();
    public static PrincessGrowthPotion get() { return _inst; }

    @Override
    public void execute(int targetObjId, L1PcInstance pc, L1ItemInstance potion) {
        handle(pc, potion, targetObjId);
    }

    // Some cores call the 2-arg path for use_type=choice; don't mark as @Override
    public void execute(L1PcInstance pc, L1ItemInstance potion) {
        handle(pc, potion, 0);
    }

    private void handle(L1PcInstance pc, L1ItemInstance potion, int targetObjId) {
        if (pc == null) return;
        L1ItemInstance target = (targetObjId > 0) ? pc.getInventory().getItem(targetObjId) : null;
        if (target == null) { pc.sendPackets(new S_SystemMessage("請選擇背包內的公主娃娃道具。")); return; }
        if (!PriestGrowthService2.isPrincessItem(target.getItemId())) {
            pc.sendPackets(new S_SystemMessage("目標不是可升階的公主娃娃。")); return;
        }
        PriestGrowthService2.applyGrowthPotion(PriestGrowthService2.Category.PRINCESS, pc, target);
        pc.getInventory().removeItem(potion, 1);
    }
}
