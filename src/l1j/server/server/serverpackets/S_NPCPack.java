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
package l1j.server.server.serverpackets;

import l1j.server.Config;
import l1j.server.server.Opcodes;
import l1j.server.server.datatables.NPCTalkDataTable;
import l1j.server.server.model.L1NpcTalkData;
import l1j.server.server.model.Instance.L1FieldObjectInstance;
import l1j.server.server.model.Instance.L1NpcInstance;

// Referenced classes of package l1j.server.server.serverpackets:
// ServerBasePacket

public class S_NPCPack extends ServerBasePacket {

	private static final String S_NPC_PACK = "[S] S_NPCPack";

	private static final int STATUS_POISON = 1;

	private static final int STATUS_PC = 4;

	private static final int HIDDEN_STATUS_FLY = 2;

	private byte[] _byte = null;

	public S_NPCPack(L1NpcInstance npc) {
		writeC(Opcodes.S_OPCODE_CHARPACK);
		writeH(npc.getX());
		writeH(npc.getY());
		writeD(npc.getId());
		if (npc.getTempCharGfx() == 0) {
			writeH(npc.getGfxId());
		}
		else {
			writeH(npc.getTempCharGfx());
		}
		writeC(npc.getStatus());
		writeC(npc.getHeading());
		writeC(npc.getChaLightSize());
		writeC(npc.getMoveSpeed());
		writeExp(npc.getExp());
		writeH(npc.getTempLawful());
        if (Config.SHOW_NPC_ID) {
            writeS(npc.getNameId() + "[" + npc.getNpcId() + "]"
                    + "面向[" + npc.getHeading() + "]" + "圖形["
                    + npc.getGfxId() + "]");
        } else {
            writeS(npc.getNameId());
        }
		if (npc instanceof L1FieldObjectInstance) { // SICの壁字、看板など
			L1NpcTalkData talkdata = NPCTalkDataTable.getInstance().getTemplate(npc.getNpcTemplate().get_npcId());
			if (talkdata != null) {
				writeS(talkdata.getNormalAction()); // タイトルがHTML名として解釈される
			}
			else {
				writeS(null);
			}
		}
		else {
			writeS(npc.getTitle());
		}

		/**
		 * シシニテ - 0:mob,item(atk pointer), 1:poisoned(), 2:invisable(), 4:pc,
		 * 8:cursed(), 16:brave(), 32:??, 64:??(??), 128:invisable but name
		 */
		int status = 0;
		if (npc.getPoison() != null) { // 毒状態
			if (npc.getPoison().getEffectId() == 1) {
				status |= STATUS_POISON;
			}
		}
		if (npc.getNpcTemplate().is_doppel()) {
			// 變形怪 (Doppelganger)
			//
			// 注意：當變形怪化身成玩家外觀時（TempCharGfx 變成玩家職業外觀），
			// 客戶端會把它視為「PC 角色」，玩家拖曳物品時會觸發「交易請求」而不是「給予」封包，
			// 造成試煉/煉化交付材料失敗。
			//
			// 因此：在「已化身為玩家外觀」的狀態下，不送出 STATUS_PC(4) 標記，
			// 讓客戶端維持 NPC 互動行為(可給予物品)。
			boolean isMimicPcShape = (npc.getTempCharGfx() != 0)
					&& (npc.getTempCharGfx() != npc.getNpcTemplate().get_gfxid());
			if (!isMimicPcShape) {
				// 原本邏輯：部分變形怪需要以 PC 狀態標記處理（保留舊例外）。
				if (npc.getGfxId() != 31 && npc.getNpcTemplate().get_npcId() != 81069) {
					status |= STATUS_PC;
				}
			}
		}
		// 二段加速狀態
		status |= npc.getBraveSpeed() * 16;

		writeC(status);

		writeD(0); // 0以外にするとC_27が飛ぶ
		writeS(null);
		writeS(null); // マスター名？
		if (npc.getTempCharGfx() == 1024 || npc.getTempCharGfx() == 2363
				|| npc.getTempCharGfx() == 6697 || npc.getTempCharGfx() == 8180
				 || npc.getTempCharGfx() == 1204 || npc.getTempCharGfx() == 2353
				 || npc.getTempCharGfx() == 3631 || npc.getTempCharGfx() == 2544) { // 飛行系怪物
			writeC(npc.getHiddenStatus() == HIDDEN_STATUS_FLY ? 2 : 1); // 判斷是否飛天中
		} else {
			writeC(0);
		}
		writeC(0xFF); // HP
		writeC(0);
		writeC(npc.getLevel());
		writeC(0xFF);
		writeC(0xFF);
		writeC(0);
	}

	@Override
	public byte[] getContent() {
		if (_byte == null) {
			_byte = _bao.toByteArray();
		}

		return _byte;
	}

	@Override
	public String getType() {
		return S_NPC_PACK;
	}

}
