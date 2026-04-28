package l1j.server.server.clientpackets;

import l1j.server.server.ClientThread;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PShopNpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_NPCTalkReturn;

public class C_NPCTalk extends ClientBasePacket {

    private static final String C_NPCTALK = "[C] C_NPCTalk";

    public C_NPCTalk(byte[] decrypt, ClientThread client) throws Exception {
        super(decrypt);
        int objid = readD();

        L1PcInstance pc = client.getActiveChar();
        if (pc == null) {
            return;
        }

        L1Object obj = L1World.getInstance().findObject(objid);
        if (obj == null) {
            return;
        }

        // 釣魚商店 / 擺攤 NPC 等特殊處理
        if (obj instanceof L1PShopNpcInstance) {
    L1PShopNpcInstance npc = (L1PShopNpcInstance) obj;
    boolean isOwner = false;
    try {
        isOwner = (pc.getId() == npc.getOwnerId());
    } catch (Throwable ignore) {}
    String htmlid = isOwner ? "pshop_owner" : "pshop_shop";
    
        try { pc.setTempID(npc.getId()); } catch (Throwable ignore) {}
// Send both objectId=0 and objectId=npcId for client compatibility
    pc.sendPackets(new S_NPCTalkReturn(0, htmlid));
    pc.sendPackets(new S_NPCTalkReturn(npc.getId(), htmlid));
    return;
}


        if (obj instanceof L1NpcInstance) {
            L1NpcInstance npc = (L1NpcInstance) obj;

            // Priest companion dialog hook (IDs: 961123~961127)
            if (npc.getNpcTemplate() != null) {
                int __nid = npc.getNpcTemplate().get_npcId();
                if (__nid==961123 || __nid==961124 || __nid==961125 || __nid==961126 || __nid==961127) {
                    // 把這隻祭司 NPC 的 objid 記在 pc 上，給 BoundPriestFinder 用
                    try { pc.setTempID(npc.getId()); } catch (Throwable ignore) {}

                    // 綁定：根據這隻祭司 NPC 的階級，找到玩家背包中對應那一顆祭司道具，將其 item_objid 綁到 NPC 上
                    try {
                        l1j.server.server.model.Instance.L1ItemInstance boundItem =
                                l1j.server.server.priest.PriestItemSelector.findSameTier(pc, npc);
                        if (boundItem != null) {
                            try { npc.setPriestItemObjId(boundItem.getId()); } catch (Throwable ignore2) {}
                            try {
                                // 確保這顆祭司道具有一筆獨立的智力成長紀錄
                                l1j.server.server.priest.PriestIqDAO2.ensure(
                                        pc.getId(),
                                        boundItem.getId(),       // item_objid
                                        boundItem.getItemId(),   // priest_item_id
                                        boundItem.getViewName(), // 名稱
                                        npc.getNpcTemplate().get_int()); // 該階祭司基本智力
                            } catch (Throwable ignore3) {}
                        }
                    } catch (Throwable ignore1) {}

                    int __iq = npc.getNpcTemplate().get_int();
                    int __mpInit = npc.getNpcTemplate().get_mp();
                    int __regen = 0;
                    switch (__nid) {
                        case 961123: __regen = 3; break;
                        case 961124: __regen = 4; break;
                        case 961125: __regen = 6; break;
                        case 961126: __regen = 9; break;
                        case 961127: __regen = 12; break;
                    }
                    l1j.server.server.priest.PriestSettingsStore.Settings st =
                        l1j.server.server.priest.PriestSettingsStore.ensureLoaded(pc.getId(), __mpInit, __regen);
                    l1j.server.server.priest.PriestSettingsStore.regenTick(pc.getId());
                    String[] __data = new String[] {
                        String.valueOf(__iq),
                        String.valueOf(st.healThreshold),
                        (st.autoSupport ? "開" : "關"),
                        String.valueOf(st.mp)
                    };
                    if (st.autoSupport) { l1j.server.server.priest.PriestSupportRunner.startFor(pc); }
                    pc.sendPackets(new S_NPCTalkReturn(npc.getId(), "priest_doll", __data));
                    return;
                }
            }

            // 一般 NPC → 走原本對話/商店流程
            npc.onTalkAction(pc);
        }

    }

    @Override
    public String getType() {
        return C_NPCTALK;
    }
}
