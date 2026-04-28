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
import l1j.server.server.datatables.PetItemTable;
import l1j.server.server.datatables.PetTable;
import l1j.server.server.datatables.PetTypeTable;
import l1j.server.server.model.L1Inventory;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1PcInventory;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1DollInstance;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.Instance.L1PetInstance;
import l1j.server.server.model.Instance.L1SummonInstance;
import l1j.server.server.serverpackets.S_ItemName;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.templates.L1Npc;
import l1j.server.server.templates.L1Pet;
import l1j.server.server.templates.L1PetItem;
import l1j.server.server.templates.L1PetType;
import l1j.server.server.utils.Random;

/**
 * 處理收到由客戶端傳來給道具的封包
 */
public class C_GiveItem extends ClientBasePacket {
	private static final String C_GIVE_ITEM = "[C] C_GiveItem";

	public C_GiveItem(byte decrypt[], ClientThread client) {
		super(decrypt);
		
		L1PcInstance pc = client.getActiveChar();
		if ((pc == null) || pc.isGhost()) {
			return;
		}
		int targetId = readD();
		readH();
		readH();
		int itemId = readD();
		int count = readD();
		
		L1Object object = L1World.getInstance().findObject(targetId);
		if ((object == null) || !(object instanceof L1NpcInstance)) {
			return;
		}
		L1NpcInstance target = (L1NpcInstance) object;
		final int npcId = target.getNpcTemplate().get_npcId();
		final boolean isPetOrSummon = (target instanceof L1PetInstance) || (target instanceof L1SummonInstance);
		final boolean isRefineOrTrialMonster = isRefineOrTrialMonster(target);

		// 先取得要給予的物品，才能判斷是否為「馴養寵物」用途。
		L1Inventory inv = pc.getInventory();
		L1ItemInstance item = inv.getItem(itemId);
		if (item == null) {
			return;
		}
		// 允許「可馴養寵物」的怪物接收其指定馴養物品（肉/漂浮之眼肉/胡蘿蔔...）。
		// 這是寵物機制的核心：玩家必須把馴養物品拖曳給怪物。
		final boolean isTamingItemForThisMonster = isTamingItemForMonster(npcId, item.getItemId());

		if (!isNpcItemReceivable(target.getNpcTemplate(), isPetOrSummon, isRefineOrTrialMonster, isTamingItemForThisMonster)) {
			return;
		}
		L1Inventory targetInv = target.getInventory();
		// 只能給以下物品 by mca 20081013
		// 注意：煉化/試煉怪需能接受任務材料，因此對這些怪物不套用「可給予物品白名單」。
		if (pc.isGm() || isPetOrSummon || isRefineOrTrialMonster || item.getItemId() == 40010 || // 治癒藥水
			item.getItemId() == 40011 ||          // 強力治癒藥水
			item.getItemId() == 40012 ||          // 終極治癒藥水
			item.getItemId() == 40013 ||          // 自我加速藥水
			item.getItemId() == 40018 ||          // 強化 自我加速藥水
			item.getItemId() == 40056 ||          // 肉
			item.getItemId() == 40057 ||          // 漂浮之眼肉
			item.getItemId() == 40070 ||          // 進化果實
			item.getItemId() == 88 ||             // 潘的角
			item.getItemId() == 40507 ||          // 安特之樹枝
			item.getItemId() == 40505 ||          // 安特之樹皮
			item.getItemId() == 40499 ||          // 蘑菇汁
			item.getItemId() == 40494 ||          // 純粹的米索莉塊
			item.getItemId() == 40495 ||          // 米索莉線
			item.getItemId() == 40520 ||          // 精靈粉末
			item.getItemId() == 40521 ||          // 精靈羽翼
			item.getItemId() == 40508 ||          // 奧里哈魯根
			item.getItemId() == 49302 ||          // 終極果實
			item.getItemId() == 40060 ||          // 胡蘿蔔
			item.getItemId() == 41310) {          // 勝利果實
                } else {
                  pc.sendPackets(new S_ServerMessage(942)); // 對方的負重太重，無法再給予。
                  return;
		}  // end
		if (item.isEquipped()) {
			pc.sendPackets(new S_ServerMessage(141)); // \f1你不能夠將轉移已經裝備的物品。
			return;
		}
		if (!item.getItem().isTradable()) {
			pc.sendPackets(new S_ServerMessage(210, item.getItem().getName())); // \f1%0%d是不可轉移的…
			return;
		}
		if (item.getBless() >= 128) { // 封印的裝備
			// \f1%0%d是不可轉移的…
			pc.sendPackets(new S_ServerMessage(210, item.getItem().getName()));
			return;
		}
		// 使用中的寵物項鍊 - 無法給予
		for (L1NpcInstance petNpc : pc.getPetList().values()) {
			if (petNpc instanceof L1PetInstance) {
				L1PetInstance pet = (L1PetInstance) petNpc;
				if (item.getId() == pet.getItemObjId()) {
					pc.sendPackets(new S_ServerMessage(1187)); // 寵物項鍊正在使用中。
					return;
				}
			}
		}
		// 使用中的魔法娃娃 - 無法給予
		for (L1DollInstance doll : pc.getDollList().values()) {
			if (doll.getItemObjId() == item.getId()) {
				pc.sendPackets(new S_ServerMessage(1181)); // 這個魔法娃娃目前正在使用中。
				return;
			}
		}
		// 煉化/試煉怪：允許玩家交付任務材料，且不受怪物負重/容量限制影響
		if (!isRefineOrTrialMonster) {
			if (targetInv.checkAddItem(item, count) != L1Inventory.OK) {
				pc.sendPackets(new S_ServerMessage(942)); // 對方的負重太重，無法再給予。
				return;
			}
		}
		item = inv.tradeItem(item, count, targetInv);
		target.onGetItem(item);
		target.turnOnOffLight();
		pc.turnOnOffLight();

		L1PetType petType = PetTypeTable.getInstance().get(
				target.getNpcTemplate().get_npcId());
		// 某些專案/資料庫可能未完整建立 pettypes 資料，
		// 但仍需要支援「用肉/漂浮之眼肉/胡蘿蔔馴養」的基礎寵物怪。
		// 若 petType 缺失，改用白名單與可馴養物品做保底判斷。
		if ((petType == null) || target.isDead()) {
			if (!target.isDead() && isWhitelistedTameMonster(npcId)
					&& isBasicTamingFood(item.getItemId())) {
				// 直接走馴養流程（不依賴 pettypes 資料）
				tamePet(pc, target);
			}
			return;
		}

		// 捕抓寵物
		if (item.getItemId() == petType.getItemIdForTaming()) {
			tamePet(pc, target);
		}
		// 進化寵物
		else if (item.getItemId() == petType.getEvolvItemId()) {
			evolvePet(pc, target, item.getItemId());
		}

		if (item.getItem().getType2() == 0) { // 道具類
			// 食物類
			if (item.getItem().getType() == 7) {
				eatFood(pc, target, item, count);
			}
			// 寵物裝備類
			else if ((item.getItem().getType() == 11)
					&& (petType.canUseEquipment())) { // 判斷是否可用寵物裝備
				usePetWeaponArmor(target, item);
			}
		}

	}

	private void eatFood(L1PcInstance pc, L1NpcInstance target,
			L1ItemInstance item, int count) {
		if (!(target instanceof L1PetInstance)) {
			return;
		}
		L1PetInstance pet = (L1PetInstance) target;
		L1Pet _l1pet = PetTable.getInstance().getTemplate(item.getId());
		int food = 0;
		int foodCount = 0;
		boolean isFull = false;

		if (pet.get_food() == 100) { // 非常飽
			return;
		}
		// 食物營養度判斷
		if (item.getItem().getFoodVolume() != 0) {
			// 吃掉食物的數量判斷
			for (int i = 0; i < count; i++) {
				food = item.getItem().getFoodVolume() / 10;
				food += pet.get_food();
				if (!isFull) {
					if (food >= 100) {
						isFull = true;
						pet.set_food(100);
						foodCount++;
					} else {
						pet.set_food(food);
						foodCount++;
					}
				} else {
					break;
				}
			}
			if (foodCount != 0) {
				pet.getInventory().consumeItem(item.getItemId(), foodCount); // 吃掉食物
				// 紀錄寵物飽食度
				_l1pet.set_food(pet.get_food());
				PetTable.getInstance().storePetFood(_l1pet);
			}
		}
	}

	private void usePetWeaponArmor(L1NpcInstance target, L1ItemInstance item) {
		if (!(target instanceof L1PetInstance)) {
			return;
		}
		L1PetInstance pet = (L1PetInstance) target;
		L1PetItem petItem = PetItemTable.getInstance().getTemplate(
				item.getItemId());
		if (petItem.getUseType() == 1) { // 牙齒
			pet.usePetWeapon(pet, item);
		} else if (petItem.getUseType() == 0) { // 盔甲
			pet.usePetArmor(pet, item);
		}
	}

	private final static String receivableImpls[] = new String[] { "L1Npc", // NPC
			"L1Guardian", // 妖精森林的守護者
			"L1Teleporter", // 傳送師
			"L1Guard" }; // 警衛

	private boolean isNpcItemReceivable(L1Npc npc, boolean isPetOrSummon, boolean isRefineOrTrialMonster,
			boolean isTamingItemForThisMonster) {
		// 寵物/召喚物一律允許（餵食、裝備、馴養、進化等）
		if (isPetOrSummon) {
			return true;
		}
		// 煉化/試煉怪（包含變形怪等特殊Impl）一律允許收物品
		// 其餘怪物仍維持關閉，避免被濫用
		if (isRefineOrTrialMonster) {
			return true;
		}
// 僅開放「煉化/試煉」用途的特定怪物可收物品（避免其他怪物被濫用）。
		if (npc.getImpl().equals("L1Monster")) {
			// 一般怪物預設不開放，但「可馴養寵物」的怪物必須能接收其指定馴養物品。
			return isRefineOrTrialMonster || isTamingItemForThisMonster;
		}
		for (String impl : receivableImpls) {
			if (npc.getImpl().equals(impl)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 判斷是否為「可馴養寵物」怪物，且玩家給予的是其指定馴養道具。
	 *
	 * 例如：肉/漂浮之眼肉/胡蘿蔔等。
	 */
	private boolean isTamingItemForMonster(int npcId, int itemId) {
		L1PetType petType = PetTypeTable.getInstance().get(npcId);
		if (petType != null) {
			return itemId == petType.getItemIdForTaming();
		}
		// pettypes 缺失時，以白名單 + 基礎馴養食物作保底
		return isWhitelistedTameMonster(npcId) && isBasicTamingFood(itemId);
	}

	/**
	 * 寵物馴養用的基礎食物。
	 *
	 * 40056 肉 / 40057 漂浮之眼肉 / 40060 胡蘿蔔
	 */
	private boolean isBasicTamingFood(int itemId) {
		switch (itemId) {
		case 40056:
		case 40057:
		case 40060:
			return true;
		default:
			return false;
		}
	}

	/**
	 * 只開放指定「可馴養寵物」怪物（避免讓所有怪物都能收物品）。
	 *
	 * 45034 牧羊犬
	 * 45039 貓
	 * 45040 熊
	 * 45042 杜賓狗
	 * 45043 狼
	 * 45044 浣熊
	 * 45046 小獵犬
	 * 45047 聖伯納犬
	 * 45048 狐狸
	 * 45049 暴走兔
	 * 45053 哈士奇
	 * 45054 柯利
	 * 45313 虎男
	 */
	private boolean isWhitelistedTameMonster(int npcId) {
		switch (npcId) {
		case 45034:
		case 45039:
		case 45040:
		case 45042:
		case 45043:
		case 45044:
		case 45046:
		case 45047:
		case 45048:
		case 45049:
		case 45053:
		case 45054:
		case 45313:
			return true;
		default:
			return false;
		}
	}

	/**
	 * 只允許「給材料/道具 -> 怪物死亡或煉化後產出另一個物品」的怪物。
	 *
	 * 來源：L1NpcInstance.refineItem() 中的煉化/試煉怪（會在怪物背包內轉換材料）。
	 */
	private boolean isRefineOrTrialMonster(L1NpcInstance npc) {
		if (npc == null) {
			return false;
		}
		// 變形怪（Doppelganger）在不同資料庫/版本可能有多個 npcId。
		// 只要模板標記為 doppel，就視為「試煉/交付材料」用途，允許收物品。
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

	private void tamePet(L1PcInstance pc, L1NpcInstance target) {
		if ((target instanceof L1PetInstance)
				|| (target instanceof L1SummonInstance)) {
			return;
		}

		int petcost = 0;
		for (L1NpcInstance petNpc : pc.getPetList().values()) {
			petcost += petNpc.getPetcost();
		}
		int charisma = pc.getCha();
		if (pc.isCrown()) { // 王族
			charisma += 6;
		} else if (pc.isElf()) { // 妖精
			charisma += 12;
		} else if (pc.isWizard()) { // 法師
			charisma += 6;
		} else if (pc.isDarkelf()) { // 黑暗妖精
			charisma += 6;
		} else if (pc.isDragonKnight()) { // 龍騎士
			charisma += 6;
		} else if (pc.isIllusionist()) { // 幻術師
			charisma += 6;
		}
		charisma -= petcost;

		L1PcInventory inv = pc.getInventory();
		if ((charisma >= 6) && (inv.getSize() < 180)) {
			if (isTamePet(target)) {
				L1ItemInstance petamu = inv.storeItem(40314, 1); // 漂浮之眼的肉
				if (petamu != null) {
					new L1PetInstance(target, pc, petamu.getId());
					pc.sendPackets(new S_ItemName(petamu));
				}
			} else {
				pc.sendPackets(new S_ServerMessage(324)); // 馴養失敗。
			}
		}
	}

	private void evolvePet(L1PcInstance pc, L1NpcInstance target, int itemId) {
		if (!(target instanceof L1PetInstance)) {
			return;
		}
		L1PcInventory inv = pc.getInventory();
		L1PetInstance pet = (L1PetInstance) target;
		L1ItemInstance petamu = inv.getItem(pet.getItemObjId());
		if (((pet.getLevel() >= 30) || (itemId == 41310)) && // Lv30以上或是使用勝利果實
				(pc == pet.getMaster()) && // 自分のペット
				(petamu != null)) {
			L1ItemInstance highpetamu = inv.storeItem(40316, 1);
			if (highpetamu != null) {
				pet.evolvePet( // 寵物進化
				highpetamu.getId());
				pc.sendPackets(new S_ItemName(highpetamu));
				inv.removeItem(petamu, 1);
			}
		}
	}

	private boolean isTamePet(L1NpcInstance npc) {
		boolean isSuccess = false;
		int npcId = npc.getNpcTemplate().get_npcId();
		if (npcId == 45313) { // タイガー
			if ((npc.getMaxHp() / 3 > npc.getCurrentHp() // HPが1/3未満で1/16の確率
					)
					&& (Random.nextInt(16) == 15)) {
				isSuccess = true;
			}
		} else {
			if (npc.getMaxHp() / 3 > npc.getCurrentHp()) {
				isSuccess = true;
			}
		}

		if ((npcId == 45313) || (npcId == 45044) || (npcId == 45711)) { // タイガー、ラクーン、紀州犬の子犬
			if (npc.isResurrect()) { // RES後はテイム不可
				isSuccess = false;
			}
		}

		return isSuccess;
	}

	@Override
	public String getType() {
		return C_GIVE_ITEM;
	}
}
