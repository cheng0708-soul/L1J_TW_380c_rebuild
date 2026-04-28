package l1j.server.server.model;

import java.util.List;

import l1j.server.server.datatables.ShopTable;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_ShopBuyList;
import l1j.server.server.serverpackets.S_ShopSellList;
import l1j.server.server.utils.collections.Lists;

/**
 * Helper for "portable" shop opened via item HTML.
 *
 * This reuses the existing NPC shop system by mapping a virtual objectId
 * to a real npcId that has shop rows in the DB.
 */
public class PortableShop {

    /** NPC ID that owns the shop rows (configured by you in DB, e.g. npc 90000). */
    public static final int PORTABLE_NPC_ID = 90000;

    /**
     * Virtual object id used in shop packets. It must NOT collide with any
     * real object in L1World. We use a negative value to stay safe.
     */
    public static final int PORTABLE_OBJID = -90000;

    /**
     * Open the "buy" list: what the shop sells to the player.
     */
    public static void openBuy(L1PcInstance pc) {
        // Reuse standard shop UI: S_ShopSellList (shop sells, player buys)
        pc.sendPackets(new S_ShopSellList(PORTABLE_OBJID, pc));
    }

    /**
     * Open the "sell" list: what the player can sell to the shop.
     */
    public static void openSell(L1PcInstance pc) {
        pc.sendPackets(new S_ShopBuyList(PORTABLE_OBJID, pc));
    }
}
