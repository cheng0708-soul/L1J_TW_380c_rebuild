
package l1j.server.server.model.item.function;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.executor.ItemExecutorAdapter;
import l1j.server.server.priest.PriestGrowthService2;
import l1j.server.server.serverpackets.S_SystemMessage;

public final class PriestGrowthPotion extends ItemExecutorAdapter {
    private static final PriestGrowthPotion _inst = new PriestGrowthPotion();
    public static PriestGrowthPotion get() { return _inst; }

    @Override
    public void execute(int targetObjId, L1PcInstance pc, L1ItemInstance potion) {
        handle(pc, potion, targetObjId);
    }

    // 2-arg path tolerance (not @Override)
    public void execute(L1PcInstance pc, L1ItemInstance potion) {
        handle(pc, potion, 0);
    }

    private void handle(L1PcInstance pc, L1ItemInstance potion, int targetObjId) {
        if (pc == null) return;
        L1ItemInstance target = (targetObjId > 0) ? pc.getInventory().getItem(targetObjId) : null;
        if (target == null) { pc.sendPackets(new S_SystemMessage("請選擇背包內的祭司召喚道具。")); return; }
        if (!PriestGrowthService2.isPriestItem(target.getItemId())) {
            pc.sendPackets(new S_SystemMessage("目標不是可升階的祭司道具。")); return;
        }
        PriestGrowthService2.applyGrowthPotion(PriestGrowthService2.Category.PRIEST, pc, target);
        pc.getInventory().removeItem(potion, 1);
    }
}
