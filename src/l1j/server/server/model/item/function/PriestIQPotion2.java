
package l1j.server.server.model.item.function;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.executor.ItemExecutorAdapter;
import l1j.server.server.priest.PriestIqDAO2;
import l1j.server.server.priest.PriestGrowthService2;
import l1j.server.server.priest.NpcIntelResolver;
import l1j.server.server.priest.InventoryRefresh;
import l1j.server.server.serverpackets.S_SystemMessage;

public final class PriestIQPotion2 extends ItemExecutorAdapter {
    private static final PriestIQPotion2 _inst = new PriestIQPotion2();
    public static PriestIQPotion2 get() { return _inst; }

    @Override
    public void execute(int targetObjId, L1PcInstance pc, L1ItemInstance potion) {
        handle(pc, potion, targetObjId);
    }
    public void execute(L1PcInstance pc, L1ItemInstance potion) { handle(pc, potion, 0); }

    private static String stripBrackets(String s) {
        if (s == null) return "";
        return s.replaceAll("\\\\[.*?\\\\]", "").trim();
    }

    private void handle(L1PcInstance pc, L1ItemInstance potion, int targetObjId) {
        if (pc == null) return;
        L1ItemInstance target = (targetObjId > 0) ? pc.getInventory().getItem(targetObjId) : null;
        if (target == null) { pc.sendPackets(new S_SystemMessage("請選擇背包內的祭司召喚道具。")); return; }
        if (!PriestGrowthService2.isPriestItem(target.getItemId())) {
            pc.sendPackets(new S_SystemMessage("只能對祭司召喚道具使用。")); return;
        }

        String baseName = stripBrackets(target.getItem().getName());
        if (baseName.isEmpty()) baseName = "祭司";

        // 建立 / 取行
        PriestIqDAO2.Row row = PriestIqDAO2.ensure(pc.getId(), target.getId(), target.getItemId(), baseName, 0);
        if (row == null) { pc.sendPackets(new S_SystemMessage("【系統】character_priest_iq 建立失敗。")); return; }

        // === 上限判定：已達 +100 就不消耗、不顯示 +1 ===
        if (row.iqBonus >= PriestIqDAO2.MAX_IQ_BONUS) {
            pc.sendPackets(new S_SystemMessage("智力增加失敗，最多只能喝100罐"));
            return;
        }
        final int beforeBonus = row.iqBonus;


        // +1 智力
        row = PriestIqDAO2.addIq(pc.getId(), target.getId(), target.getItemId(), 1);
        if (row == null) { pc.sendPackets(new S_SystemMessage("【系統】character_priest_iq 更新失敗。")); return; }
        if (row.iqBonus <= beforeBonus) {
            pc.sendPackets(new S_SystemMessage("智力增加失敗，最多只能喝100罐"));
            return;
        }


        // 計算 base_iq（先依名稱階級，失敗再回退 NPC DB），並以 priest DB 為主
            int baseInt = 0;
            // 這裡沿用前面 stripBrackets 處理過的 baseName，避免重新宣告導致 Duplicate local variable
            if (baseName != null) {
                if (baseName.contains("低階")) baseInt = 10;
                else if (baseName.contains("中階")) baseInt = 12;
                else if (baseName.contains("高階")) baseInt = 14;
                else if (baseName.contains("頂階")) baseInt = 16;
                else if (baseName.contains("神話")) baseInt = 18;
            }
            if (baseInt <= 0) {
                baseInt = NpcIntelResolver.getBaseIntByItemAndName(target.getItemId(), baseName);
            }
            int total = Math.max(0, baseInt + row.iqBonus);

            java.sql.Connection __con = null;
            java.sql.PreparedStatement __ps = null;
            try {
                __con = l1j.server.L1DatabaseFactory.getInstance().getConnection();
                __ps = __con.prepareStatement("UPDATE character_priest_iq SET base_iq=?, iq_total=? WHERE priest_unique_id=?");
                __ps.setInt(1, baseInt);
                __ps.setInt(2, total);
                __ps.setInt(3, row.priestUniqueId);
                __ps.executeUpdate();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                l1j.server.server.utils.SQLUtil.close(__ps);
                l1j.server.server.utils.SQLUtil.close(__con);
            }

            // 立即刷新道具名稱
            InventoryRefresh.push(pc, target);

        pc.sendPackets(new S_SystemMessage("祭司編號 " + row.priestUniqueId + " 智力 +1。"));
        pc.getInventory().removeItem(potion, 1);
    }
}
