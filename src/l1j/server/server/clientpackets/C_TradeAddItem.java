/**
 *                            License
 * THE WORK (AS DEFINED BELOW) IS PROVIDED UNDER THE TERMS OF THIS  
 * CREATIVE COMMONS PUBLIC LICENSE ("CCPL" OR "LICENSE"). 
 * THE WORK IS PROTECTED BY COPYRIGHT AND/OR OTHER APPLICABLE LAW.  
 * ANY USE OF THE WORK OTHER THAN AS AUTHORIZED UNDER THIS LICENSE OR  
 * COPYRIGHT LAW IS PROHIBITED.
 * 
 * BY EXERCISING ANY RIGHTS TO THE WORK PROVIDED HERE, YOU ACCEPT AND  
 * AGREE TO BE BOUND BY THE TERMS OF THIS LICENSE. TO THE EXTENT THIS LICENSE  
 * MAY BE CONSIDERED TO BE A CONTRACT, THE LICENSOR GRANTS YOU THE RIGHTS CONTAINED 
 * HERE IN CONSIDERATION OF YOUR ACCEPTANCE OF SUCH TERMS AND CONDITIONS.
 * 
 */
package l1j.server.server.clientpackets;

import l1j.server.server.ClientThread;
import l1j.server.server.model.L1Inventory;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1DollInstance;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.serverpackets.S_TradeAddItem;
import l1j.server.server.serverpackets.S_Trade;
import l1j.server.server.serverpackets.S_ServerMessage;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * 處理收到由客戶端傳來增加交易物品的封包
 */
public class C_TradeAddItem extends ClientBasePacket {
	private static final String C_TRADE_ADD_ITEM = "[C] C_TradeAddItem";

	public C_TradeAddItem(byte abyte0[], ClientThread client) throws Exception {
		super(abyte0);

		L1PcInstance pc = client.getActiveChar();
		if (pc == null) {
			return;
		}
		
		int itemid = readD();
		int itemcount = readD();
		
		L1ItemInstance item = pc.getInventory().getItem(itemid);
		if (item == null) {
			return;
		}

		itemcount = Math.abs(itemcount);
		itemcount = Math.min(itemcount, item.getCount());
		if (itemcount <= 0) {
			return;
		}
		if (!item.getItem().isTradable()) {
			pc.sendPackets(new S_ServerMessage(210, item.getItem().getName())); // \f1%0は捨てたりまたは他人に讓ることができません。
			return;
		}
		if (item.getBless() >= 128) { // 封印的裝備
			// \f1%0は捨てたりまたは他人に讓ることができません。
			pc.sendPackets(new S_ServerMessage(210, item.getItem().getName()));
			return;
		}
		// 使用中的寵物項鍊 - 無法交易
		for (L1NpcInstance petNpc : pc.getPetList().values()) {
			if (petNpc instanceof L1PetInstance) {
				L1PetInstance pet = (L1PetInstance) petNpc;
				if (item.getId() == pet.getItemObjId()) {
					pc.sendPackets(new S_ServerMessage(1187)); // 寵物項鍊正在使用中。
					return;
				}
			}
		}
		// 使用中的魔法娃娃 - 無法交易
		for (L1DollInstance doll : pc.getDollList().values()) {
			if (doll.getItemObjId() == item.getId()) {
				pc.sendPackets(new S_ServerMessage(1181)); // 這個魔法娃娃目前正在使用中。
				return;
			}
		}

		// 交易對象
		final int tradeId = pc.getTradeID();
		// 變形怪（如ドッペルゲンガー）外觀偽裝成玩家時，有些客戶端在「拖曳道具到目標」會直接送出
		// C_TradeAddItem，而不會先送 C_Trade（因此 tradeId 仍為 0，造成沒有任何反應）。
		// 這裡若尚未建立交易，且面前一格是允許收材料的煉化/試煉怪，則自動建立 NPC 單向交易。
		int resolvedTradeId = tradeId;
		if (resolvedTradeId == 0 && !pc.getTradeOk()) {
			L1NpcInstance npc = faceToFaceNpc(pc);
			if (npc == null) {
				// 拖曳物品到目標時不一定是正面一格；改用附近最近的候選者。
				npc = findNearestRefineOrTrialNpc(pc, 4);
			}
			if (npc != null && isRefineOrTrialMonster(npc)) {
				pc.setTradeID(-npc.getId());
				pc.sendPackets(new S_Trade(npc.getName()));
				resolvedTradeId = pc.getTradeID();
			}
		}
		if (resolvedTradeId == 0 || pc.getTradeOk()) {
			return;
		}
		// 對 NPC 的「單向交易」：避免變形怪外觀偽裝造成客戶端改走交易流程
		if (resolvedTradeId < 0) {
			// 交易對象必須仍存在且為允許收任務/煉化材料的怪物
			if (!(L1World.getInstance().findObject(-resolvedTradeId) instanceof L1NpcInstance)) {
				return;
			}
			L1NpcInstance npc = (L1NpcInstance) L1World.getInstance().findObject(-resolvedTradeId);
			if (npc == null || npc.isDead()) {
				return;
			}
			if (!isRefineOrTrialMonster(npc)) {
				return;
			}
			// 不檢查 NPC 負重/容量；先把物品放進自己的交易視窗，等按 OK 再交付給怪物
			pc.getInventory().tradeItem(item, itemcount, pc.getTradeWindowInventory());
			pc.sendPackets(new S_TradeAddItem(item, itemcount, 0));
			return;
		}

		L1PcInstance tradingPartner = (L1PcInstance) L1World.getInstance().findObject(resolvedTradeId);
		if (tradingPartner == null) {
			return;
		}
		if (tradingPartner.getInventory().checkAddItem(item, itemcount) != L1Inventory.OK) { // 檢查容量與重量
			tradingPartner.sendPackets(new S_ServerMessage(270)); // \f1持っているものが重くて取引できません。
			pc.sendPackets(new S_ServerMessage(271)); // \f1相手が物を持ちすぎていて取引できません。
			return;
		}

		// 一般玩家交易
		new l1j.server.server.model.L1Trade().TradeAddItem(pc, itemid, itemcount);
	}

	/**
	 * 有些操作(例如拖曳物品到目標)不一定要求目標在正前方。
	 * 若附近(距離<=range)僅存在一隻允許收材料的煉化/試煉怪，則將其視為交易對象。
	 */
	private L1NpcInstance findNearestRefineOrTrialNpc(L1PcInstance pc, int range) {
		L1NpcInstance nearest = null;
		int nearestDist = Integer.MAX_VALUE;
		boolean tie = false;
		for (L1Object obj : L1World.getInstance().getVisibleObjects(pc, range)) {
			if (!(obj instanceof L1NpcInstance)) {
				continue;
			}
			L1NpcInstance npc = (L1NpcInstance) obj;
			if (npc == null || npc.isDead()) {
				continue;
			}
			if (!isRefineOrTrialMonster(npc)) {
				continue;
			}
			int dist = pc.getTileLineDistance(npc);
			if (dist < nearestDist) {
				nearest = npc;
				nearestDist = dist;
				tie = false;
			} else if (dist == nearestDist && nearest != null && nearest.getId() != npc.getId()) {
				tie = true;
			}
		}
		return tie ? null : nearest;
	}

	private L1NpcInstance faceToFaceNpc(L1PcInstance pc) {
		int pcX = pc.getX();
		int pcY = pc.getY();
		int pcHeading = pc.getHeading();
		for (L1Object obj : L1World.getInstance().getVisibleObjects(pc, 1)) {
			if (!(obj instanceof L1NpcInstance)) {
				continue;
			}
			L1NpcInstance npc = (L1NpcInstance) obj;
			int x = npc.getX();
			int y = npc.getY();
			if ((pcHeading == 0) && (pcX == x) && (pcY == (y + 1))) {
				return npc;
			} else if ((pcHeading == 1) && (pcX == (x - 1)) && (pcY == (y + 1))) {
				return npc;
			} else if ((pcHeading == 2) && (pcX == (x - 1)) && (pcY == y)) {
				return npc;
			} else if ((pcHeading == 3) && (pcX == (x - 1)) && (pcY == (y - 1))) {
				return npc;
			} else if ((pcHeading == 4) && (pcX == x) && (pcY == (y - 1))) {
				return npc;
			} else if ((pcHeading == 5) && (pcX == (x + 1)) && (pcY == (y - 1))) {
				return npc;
			} else if ((pcHeading == 6) && (pcX == (x + 1)) && (pcY == y)) {
				return npc;
			} else if ((pcHeading == 7) && (pcX == (x + 1)) && (pcY == (y + 1))) {
				return npc;
			}
		}
		return null;
	}

	/** 與 C_GiveItem / C_Trade 的白名單保持一致 */
	private boolean isRefineOrTrialMonster(L1NpcInstance npc) {
		if (npc == null) {
			return false;
		}
		if (npc.getNpcTemplate().is_doppel()) {
			return true;
		}
		switch (npc.getNpcTemplate().get_npcId()) {
		case 45032: // ブロッブ（煉化）
		case 81069: // ドッペルゲンガー（クエスト）
		case 45166: // ジャックオーランタン
		case 45167: // ジャックオーランタン
			return true;
		default:
			return false;
		}
	}

	@Override
	public String getType() {
		return C_TRADE_ADD_ITEM;
	}
}
