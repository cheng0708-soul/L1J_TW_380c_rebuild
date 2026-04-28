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

import static l1j.server.server.model.Instance.L1PcInstance.REGENSTATE_MOVE;
import static l1j.server.server.model.skill.L1SkillId.ABSOLUTE_BARRIER;
import static l1j.server.server.model.skill.L1SkillId.MEDITATION;

import l1j.server.Config;
import l1j.server.server.ClientThread;
import l1j.server.server.model.AcceleratorChecker;
import l1j.server.server.model.Dungeon;
import l1j.server.server.model.DungeonRandom;
import l1j.server.server.model.L1Trade;
import l1j.server.server.model.L1CastleLocation;
import l1j.server.server.model.L1World;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1Character;
import l1j.server.server.model.L1Teleport;
import l1j.server.server.model.Instance.L1DoorInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.trap.L1WorldTraps;
import l1j.server.server.serverpackets.S_MoveCharPacket;
import l1j.server.server.serverpackets.S_SystemMessage;

// Referenced classes of package l1j.server.server.clientpackets:
// ClientBasePacket

/**
 * 處理收到由客戶端傳來移動角色的封包
 */
public class C_MoveChar extends ClientBasePacket {

	private static final byte HEADING_TABLE_X[] =
	{ 0, 1, 1, 1, 0, -1, -1, -1 };

	private static final byte HEADING_TABLE_Y[] =
	{ -1, -1, 0, 1, 1, 1, 0, -1 };

	private static final int CLIENT_LANGUAGE = Config.CLIENT_LANGUAGE;

	// 地圖編號的研究
	@SuppressWarnings("unused")
	private void sendMapTileLog(L1PcInstance pc) {
		pc.sendPackets(new S_SystemMessage(pc.getMap().toString(pc.getLocation())));
	}

	// 移動
	public C_MoveChar(byte decrypt[], ClientThread client) throws Exception {
		super(decrypt);
		
		L1PcInstance pc = client.getActiveChar();
		if ((pc == null) || pc.isTeleport()) { // 傳送中
			return;
		}
		
		// 記錄本次移動前的合法座標（用來撞到人時回朔）
		int oldX = pc.getX();
		int oldY = pc.getY();
		short oldMapId = pc.getMapId();
		
		int locx = readH();
		int locy = readH();
		int heading = readC();

		// 檢查移動的時間間隔
		if (Config.CHECK_MOVE_INTERVAL) {
			int result;
			result = pc.getAcceleratorChecker().checkInterval(AcceleratorChecker.ACT_TYPE.MOVE);
			if (result == AcceleratorChecker.R_DISPOSED) {
				return;
			}
		}
		
		// 移動中, 取消交易
	    if (pc.getTradeID() != 0) {
	    	L1Trade trade = new L1Trade();
	        trade.TradeCancel(pc);
	    }

		if (pc.hasSkillEffect(MEDITATION)) { // 取消冥想效果
			pc.removeSkillEffect(MEDITATION);
		}
		pc.setCallClanId(0); // コールクランを唱えた後に移動すると召喚無効

		if (!pc.hasSkillEffect(ABSOLUTE_BARRIER)) { // 絕對屏障
			pc.setRegenState(REGENSTATE_MOVE);
		}
		pc.getMap().setPassable(pc.getLocation(), true);

		if (heading > 8) { // XOR 加密判定
			heading ^= 0x49;
			locx = pc.getX();
			locy = pc.getY();
		}

		locx += HEADING_TABLE_X[heading];
		locy += HEADING_TABLE_Y[heading];

		// === 角色碰撞判定：如果目標格子已有其他角色，則瞬移回上一個座標（用傳送方式，強制畫面置中） ===
		L1World world = L1World.getInstance();
		for (L1Object obj : world.getVisibleObjects(pc)) {
			if (!(obj instanceof L1Character)) {
				continue;
			}
			L1Character cha = (L1Character) obj;
			if (cha == pc) { // 自己略過
				continue;
			}
			if (cha.getMapId() != oldMapId) {
				continue;
			}
			if (cha.getX() == locx && cha.getY() == locy) {
				// 這一格已經被角色佔據：只有「怪物/NPC 屍體」例外可穿越；玩家/NPC/活著的怪物一律回朔
				// - 門(door) 例外：close 時不可穿越(回朔)，open 時可正常通過
				if (cha instanceof L1DoorInstance) {
					L1DoorInstance door = (L1DoorInstance) cha;
					if (door.isDead()) {
						// 被破壞的門視同屍體/可穿越
						continue;
					}
					// open = 可穿越；close = 視為障礙回朔
					if (door.isOpen()) {
						continue;
					}
					L1Teleport.teleport(pc, oldX, oldY, oldMapId, pc.getHeading(), false);
					return;
				}
				// - 玩家一律不可穿越（不論狀態）
				if (cha instanceof L1PcInstance) {
					L1Teleport.teleport(pc, oldX, oldY, oldMapId, pc.getHeading(), false);
					return;
				}
				// - 非玩家：只有死亡(屍體)才放行
				if (cha.isDead()) {
					continue;
				}
				// - 活著的 NPC/怪物：不可穿越
				L1Teleport.teleport(pc, oldX, oldY, oldMapId, pc.getHeading(), false);
				return;
			}
		}

		if (Dungeon.getInstance().dg(locx, locy, pc.getMap().getId(), pc)) { // 傳點
			return;
		}
		if (DungeonRandom.getInstance().dg(locx, locy, pc.getMap().getId(), pc)) { // 取得隨機傳送地點
			return;
		}

		pc.getLocation().set(locx, locy);
		pc.setHeading(heading);
		if (pc.isGmInvis() || pc.isGhost()) {}
		else if (pc.isInvisble()) {
			pc.broadcastPacketForFindInvis(new S_MoveCharPacket(pc), true);
		}
		else {
			pc.broadcastPacket(new S_MoveCharPacket(pc));
		}

		// sendMapTileLog(pc); //發送信息的目的地瓦（為調查地圖）
		// 寵物競速-判斷圈數
		l1j.server.server.model.game.L1PolyRace.getInstance().checkLapFinish(pc);
		L1WorldTraps.getInstance().onPlayerMoved(pc);

		L1CastleLocation.checkMagicDollInWarArea(pc);


		pc.getMap().setPassable(pc.getLocation(), false);
		// user.UpdateObject(); // 可視範囲内の全オブジェクト更新
	}
}