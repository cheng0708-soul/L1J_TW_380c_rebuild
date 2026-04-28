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
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_Message_YN;
import l1j.server.server.serverpackets.S_Trade;
import l1j.server.server.utils.FaceToFace;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * 處理收到由客戶端傳來交易的封包
 */
public class C_Trade extends ClientBasePacket {

	private static final String C_TRADE = "[C] C_Trade";

	public C_Trade(byte abyte0[], ClientThread clientthread) throws Exception {
		super(abyte0);

		L1PcInstance player = clientthread.getActiveChar();
		if ((player == null) || player.isGhost()) {
			return;
		}
		L1PcInstance target = FaceToFace.faceToFace(player, false);
		if (target != null) {
			if (!target.isParalyzed()) {
				player.setTradeID(target.getId()); // 相手のオブジェクトIDを保存しておく
				target.setTradeID(player.getId());
				target.sendPackets(new S_Message_YN(252, player.getName())); // %0%sがあなたとアイテムの取引を望んでいます。取引しますか？（Y/N）
			}
			return;
		}

		// 變形怪(如ドッペルゲンガー)等：外觀可能偽裝成玩家，導致客戶端改走「交易」流程。
		// 若玩家面前不是玩家而是可收任務材料/煉化材料的怪物，則直接以「單向交易」開啟視窗，
		// 後續在 C_TradeAddItem / C_TradeOK 內將物品交付給怪物(不受怪物負重影響)。
		L1NpcInstance npc = faceToFaceNpc(player);
		if (npc == null) {
			// 拖曳物品到目標時，客戶端不一定要求「正面一格」。
			// 以附近最近的煉化/試煉怪作為候選，降低「拖曳沒有反應」的機率。
			npc = findNearestRefineOrTrialNpc(player, 4);
		}
		if (npc != null && isRefineOrTrialMonster(npc)) {
			player.setTradeID(-npc.getId()); // 用負數標記「交易對象是NPC」
			player.sendPackets(new S_Trade(npc.getName()));
		}
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
				// 等距離且是不同對象：避免誤選
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
			// 僅需「面前一格」即可，NPC朝向不限制
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

	/**
	 * 只允許「給材料/道具 -> 怪物死亡或煉化後產出另一個物品」的怪物。
	 * 與 C_GiveItem 的白名單保持一致。
	 */
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
		return C_TRADE;
	}
}
