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
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_WhoCharinfo;
import l1j.server.server.serverpackets.S_NPCTalkReturn;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * /who: 直接開 who_main 對話檔
 * 若輸入 /who 角色名, 則顯示該角色的 who 資訊
 */
public class C_Who extends ClientBasePacket {

	private static final String C_WHO = "[C] C_Who";

	public C_Who(byte[] decrypt, ClientThread client) {
		super(decrypt);

		L1PcInstance pc = client.getActiveChar();
		if (pc == null) {
			return;
		}

		// 原本 /who 指令可以帶角色名: /who 某人
		String name = readS();
		if (name != null && name.length() > 0) {
			L1PcInstance find = L1World.getInstance().getPlayer(name);
			if (find != null) {
				S_WhoCharinfo s_whocharinfo = new S_WhoCharinfo(find);
				pc.sendPackets(s_whocharinfo);
				return;
			}
		}

		// 如果沒帶名字, 或找不到該角色 -> 開啟 who_main 對話選單
		pc.sendPackets(new S_NPCTalkReturn(pc.getId(), "who_main"));
	}

	@Override
	public String getType() {
		return C_WHO;
	}
}
