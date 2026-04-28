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
import l1j.server.server.model.L1Trade;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_TradeStatus;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * 處理收到由客戶端傳來交易OK的封包
 */
public class C_TradeOK extends ClientBasePacket {

	private static final String C_TRADE_CANCEL = "[C] C_TradeOK";

	public C_TradeOK(byte abyte0[], ClientThread clientthread) throws Exception {
		super(abyte0);

		L1PcInstance player = clientthread.getActiveChar();
		if (player == null) {
			return;
		}
		
		final int tradeId = player.getTradeID();
		// NPC 單向交易（變形怪外觀偽裝造成客戶端改走交易流程）
		if (tradeId < 0) {
			L1NpcInstance npc = null;
			if (L1World.getInstance().findObject(-tradeId) instanceof L1NpcInstance) {
				npc = (L1NpcInstance) L1World.getInstance().findObject(-tradeId);
			}
			if (npc == null || npc.isDead() || !isRefineOrTrialMonster(npc)) {
				// 目標不存在/不允許時，退回並結束交易
				new L1Trade().TradeCancel(player);
				return;
			}
			// 將交易視窗內的物品交付給 NPC（不受 NPC 負重/容量限制影響）
			player.setTradeOk(true);
			while (player.getTradeWindowInventory().getSize() > 0) {
				L1ItemInstance tradeItem = player.getTradeWindowInventory().getItems().get(0);
				player.getTradeWindowInventory().tradeItem(tradeItem, tradeItem.getCount(), npc.getInventory());
				npc.onGetItem(tradeItem);
			}
			player.sendPackets(new S_TradeStatus(0));
			player.setTradeOk(false);
			player.setTradeID(0);
			player.turnOnOffLight();
			npc.turnOnOffLight();
			return;
		}

		L1PcInstance trading_partner = (L1PcInstance) L1World.getInstance().findObject(tradeId);
		if (trading_partner != null) {
			player.setTradeOk(true);

			if (player.getTradeOk() && trading_partner.getTradeOk()) // 同時都壓OK
			{
				// 檢查身上的空間是否還有 (180 - 16)
				if ((player.getInventory().getSize() < (180 - 16)) && (trading_partner.getInventory().getSize() < (180 - 16))) // お互いのアイテムを相手に渡す
				{
					L1Trade trade = new L1Trade();
					trade.TradeOK(player);
				}
				else // お互いのアイテムを手元に戻す
				{
					player.sendPackets(new S_ServerMessage(263)); // \f1一人のキャラクターが持って歩けるアイテムは最大180個までです。
					trading_partner.sendPackets(new S_ServerMessage(263)); // \f1一人のキャラクターが持って歩けるアイテムは最大180個までです。
					L1Trade trade = new L1Trade();
					trade.TradeCancel(player);
				}
			}
		}
	}

	@Override
	public String getType() {
		return C_TRADE_CANCEL;
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

}
