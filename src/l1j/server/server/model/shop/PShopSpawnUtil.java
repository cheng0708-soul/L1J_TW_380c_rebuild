package l1j.server.server.model.shop;

import l1j.server.server.IdFactory;
import l1j.server.server.datatables.NpcTable;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PShopNpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.templates.L1Npc;

public class PShopSpawnUtil {
    // Mute PSHOP TRACE messages (UI spam)
    private static final boolean MUTE_PSHOP_TRACE = true;



    public static L1PShopNpcInstance spawn(L1PcInstance owner, int templateId, int modeItemId, int heading) {
        try {
            int fixedId = 970004; // 使用DB對應npcaction的NPC ID
            L1Npc tpl = NpcTable.getInstance().getTemplate(fixedId);
            if (tpl == null) {
                System.out.println("PSHOP: cannot find template @970004");
                return null;
            }

            L1PShopNpcInstance npc = new L1PShopNpcInstance(tpl, owner.getId(), modeItemId);
            npc.setId(IdFactory.getInstance().nextId());
            npc.setX(owner.getX());
            npc.setY(owner.getY());
            npc.setMap(owner.getMapId());
            npc.setHeading(heading);

            // 不要套用玩家外觀，確保client送出C_NPCTalk
            // npc.setTempCharGfx(owner.getGfxId());

            L1World.getInstance().storeObject(npc);
            L1World.getInstance().addVisibleObject(npc);
            return npc;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    }
}
