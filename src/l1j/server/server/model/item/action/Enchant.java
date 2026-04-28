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
package l1j.server.server.model.item.action;

import l1j.server.Config;
import l1j.server.server.ClientThread;
import l1j.server.server.datatables.LogEnchantTable;
import l1j.server.server.model.L1PcInventory;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.identity.L1ItemId;
import l1j.server.server.serverpackets.S_ItemStatus;
import l1j.server.server.serverpackets.S_OwnCharAttrDef;
import l1j.server.server.serverpackets.S_SPMR;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.templates.L1Armor;
import l1j.server.server.utils.Random;

public class Enchant {

	// 對武器施法的卷軸
	public static void scrollOfEnchantWeapon(L1PcInstance pc, L1ItemInstance l1iteminstance
			, L1ItemInstance l1iteminstance1, ClientThread client) {
		int itemId = l1iteminstance.getItem().getItemId();
		int safe_enchant = l1iteminstance1.getItem().get_safeenchant();
		int weaponId = l1iteminstance1.getItem().getItemId();
		if ((l1iteminstance1 == null) || (l1iteminstance1.getItem().getType2() != 1)
				|| (safe_enchant < 0) || (l1iteminstance1.getBless() >= 128)) {
			pc.sendPackets(new S_ServerMessage(79));
			return;
		}
		
		if ((weaponId == 7) || (weaponId == 35) || (weaponId == 48) || (weaponId == 73) || (weaponId == 105)
				|| (weaponId == 120) || (weaponId == 147) || (weaponId == 156)
				|| (weaponId == 174) || (weaponId == 175) || (weaponId == 224)) { // 象牙塔裝備
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
			return;
		}
		if ((weaponId >= 246) && (weaponId <= 249)) { // 試煉之劍
			if (itemId != L1ItemId.SCROLL_OF_ENCHANT_QUEST_WEAPON) { // 非試煉卷軸
				pc.sendPackets(new S_ServerMessage(79));
				return;
			}
		}
		if (itemId == L1ItemId.SCROLL_OF_ENCHANT_QUEST_WEAPON) { // 試煉卷軸
			if ((weaponId < 246) || (weaponId > 249)) { // 非試煉之劍
				pc.sendPackets(new S_ServerMessage(79));
				return;
			}
		}
		if ((weaponId == 36) || (weaponId == 183) || ((weaponId >= 250) && (weaponId <= 255))) { // 幻象武器
			if (itemId != 40128) { // 非對武器施法的幻象卷軸
				pc.sendPackets(new S_ServerMessage(79));
				return;
			}
		}
		if (itemId == 40128) { // 對武器施法的幻象卷軸
			if ((weaponId != 36) && (weaponId != 183) && ((weaponId < 250) || (weaponId > 255))) { // 非幻象武器
				pc.sendPackets(new S_ServerMessage(79)); // \f1何も起きませんでした。
				return;
			}
		}

		int enchant_level = l1iteminstance1.getEnchantLevel();
		// 自訂：武器最高強化 +30，達到上限後不可再強化
		if (enchant_level >= 30) {
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
			return;
		}

		if (itemId == L1ItemId.C_SCROLL_OF_ENCHANT_WEAPON) { // 受咀咒的 對武器施法的卷軸
			pc.getInventory().removeItem(l1iteminstance, 1);
			if (enchant_level < -6) {
				// -7以上失敗。
				FailureEnchant(pc, l1iteminstance1);
			} else {
				SuccessEnchant(pc, l1iteminstance1, client, -1);
			}
		} else if (enchant_level < safe_enchant) { // 強化等級小於安定值
			pc.getInventory().removeItem(l1iteminstance, 1);
			SuccessEnchant(pc, l1iteminstance1, client, RandomELevel(l1iteminstance1, itemId));
		} else {
			pc.getInventory().removeItem(l1iteminstance, 1);

			int rnd = Random.nextInt(100) + 1;
			int enchant_chance_wepon;
			if (enchant_level >= 9) {
				enchant_chance_wepon = (100 + 3 * Config.ENCHANT_CHANCE_WEAPON) / 6;
			}
			else {
				enchant_chance_wepon = (100 + 3 * Config.ENCHANT_CHANCE_WEAPON) / 3;
			}

			// 自訂：15% 對武器施法的卷軸 (240128)
			if (itemId == 240128) {
				enchant_chance_wepon += 15;
			}
			// 上限保護，避免成功率過高
			if (enchant_chance_wepon > 95) {
				enchant_chance_wepon = 95;
			}

			if (rnd < enchant_chance_wepon) {
				int randomEnchantLevel = RandomELevel(l1iteminstance1, itemId);
				SuccessEnchant(pc, l1iteminstance1, client, randomEnchantLevel);
			} else if ((enchant_level >= 9) && (rnd < (enchant_chance_wepon * 2))) {
			    pc.sendPackets(new S_ServerMessage(160, l1iteminstance1.getLogName(), "$245", "$248"));
			} else {
			    // 自訂：防爆卷效果判斷（武器）
			    int protectType = l1iteminstance1.getEnchantProtectType();
			    if (protectType == 1) { // 歸0防爆：強化失敗時裝備不爆、強化值歸 0
			        l1iteminstance1.setEnchantLevel(0);
			        l1iteminstance1.setEnchantProtectType(0);
			        pc.getInventory().updateItem(l1iteminstance1, L1PcInventory.COL_ENCHANTLVL);
			        pc.sendPackets(new S_ItemStatus(l1iteminstance1));

			        // ★ 提示訊息
			        pc.sendPackets(new S_SystemMessage("強化失敗，但裝備受到『歸0防爆卷』保護，裝備未毀損，強化值已歸 0。"));

			    } else if (protectType == 2) { // 完全防爆：強化失敗時裝備不爆、強化值不變
			        l1iteminstance1.setEnchantProtectType(0);
			        pc.getInventory().updateItem(l1iteminstance1, L1PcInventory.COL_ENCHANTLVL);
			        pc.sendPackets(new S_ItemStatus(l1iteminstance1));

			        // ★ 提示訊息
			        pc.sendPackets(new S_SystemMessage("強化失敗，但裝備受到『完全防爆卷』保護，裝備未毀損，強化值沒有變化。"));

			    } else {
			        FailureEnchant(pc, l1iteminstance1);
			    }
			}

		}
	}

	// 對盔甲施法的卷軸
	public static void scrollOfEnchantArmor(L1PcInstance pc, L1ItemInstance l1iteminstance
			, L1ItemInstance l1iteminstance1, ClientThread client) {
		int itemId = l1iteminstance.getItem().getItemId();
		int safe_enchant = ((L1Armor) l1iteminstance1.getItem()).get_safeenchant();
		int armorId = l1iteminstance1.getItem().getItemId();
		if ((l1iteminstance1 == null) || (l1iteminstance1.getItem().getType2() != 2)
				|| (safe_enchant < 0) || (l1iteminstance1.getBless() >= 128)) {
			pc.sendPackets(new S_ServerMessage(79)); // \f1何も起きませんでした。
			return;
		}
		if (armorId == 20028 || armorId == 20082 || armorId == 20126 || armorId == 20173
				|| armorId == 20206 || armorId == 20232 || armorId == 21138
				|| armorId == 21051 || armorId == 21052 || armorId== 21053
				|| armorId == 21054 || armorId == 21055 || armorId == 21056
				|| armorId == 21140 || armorId == 21141) { // 象牙塔裝備
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
			return;
		}
		if ((armorId == 20161) || ((armorId >= 21035) && (armorId <= 21038))) { // 幻象裝備
			if (itemId != 40127) { // 非對盔甲施法的幻象卷軸
				pc.sendPackets(new S_ServerMessage(79));
				return;
			}
		}
		if (itemId == 40127) { // 對盔甲施法的幻象卷軸
			if ((armorId != 20161) && ((armorId < 21035) || (armorId > 21038))) { // 非幻象裝備
				pc.sendPackets(new S_ServerMessage(79)); // \f1何も起きませんでした。
				return;
			}
		}

		int enchant_level = l1iteminstance1.getEnchantLevel();
		// 自訂：防具最高強化 +30，達到上限後不可再強化
		if (enchant_level >= 30) {
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
			return;
		}
		if (itemId == L1ItemId.C_SCROLL_OF_ENCHANT_ARMOR) { // 受咀咒的 對盔甲施法的卷軸
			pc.getInventory().removeItem(l1iteminstance, 1);
			if (enchant_level < -6) {
				// -7以上失敗。
				FailureEnchant(pc, l1iteminstance1);
			} else {
				SuccessEnchant(pc, l1iteminstance1, client, -1);
			}
		} else if (enchant_level < safe_enchant) { // 強化等級小於安定值
			pc.getInventory().removeItem(l1iteminstance, 1);
			SuccessEnchant(pc, l1iteminstance1, client, RandomELevel(l1iteminstance1, itemId));
		} else {
			pc.getInventory().removeItem(l1iteminstance, 1);
			int rnd = Random.nextInt(100) + 1;
			int enchant_chance_armor;
			int enchant_level_tmp;
			if (safe_enchant == 0) { // 骨、ブラックミスリル用補正
				enchant_level_tmp = enchant_level + 2;
			} else {
				enchant_level_tmp = enchant_level;
			}
			if (enchant_level >= 9) {
				enchant_chance_armor = (100 + enchant_level_tmp * Config.ENCHANT_CHANCE_ARMOR) / (enchant_level_tmp * 2);
			} else {
				enchant_chance_armor = (100 + enchant_level_tmp * Config.ENCHANT_CHANCE_ARMOR) / enchant_level_tmp;
			}

			// 自訂：15% 對盔甲施法的卷軸 (240129)
			if (itemId == 240129) {
				enchant_chance_armor += 15;
			}
			// 上限保護，避免成功率過高
			if (enchant_chance_armor > 95) {
				enchant_chance_armor = 95;
			}

			if (rnd < enchant_chance_armor) {
			    int randomEnchantLevel = RandomELevel(l1iteminstance1, itemId);
			    SuccessEnchant(pc, l1iteminstance1, client, randomEnchantLevel);
			}
			else if ((enchant_level >= 9) && (rnd < (enchant_chance_armor * 2))) {
			    pc.sendPackets(new S_ServerMessage(160, l1iteminstance1.getLogName(), "$252", "$248"));
			} else {
			    // 自訂：防爆卷效果判斷（盔甲）
			    int protectType = l1iteminstance1.getEnchantProtectType();
			    if (protectType == 1) { // 歸0防爆
			        l1iteminstance1.setEnchantLevel(0);
			        l1iteminstance1.setEnchantProtectType(0);
			        pc.getInventory().updateItem(l1iteminstance1, L1PcInventory.COL_ENCHANTLVL);
			        pc.sendPackets(new S_ItemStatus(l1iteminstance1));

			        // ★ 提示訊息
			        pc.sendPackets(new S_SystemMessage("強化失敗，但裝備受到『歸0防爆卷』保護，裝備未毀損，強化值已歸 0。"));

			    } else if (protectType == 2) { // 完全防爆
			        l1iteminstance1.setEnchantProtectType(0);
			        pc.getInventory().updateItem(l1iteminstance1, L1PcInventory.COL_ENCHANTLVL);
			        pc.sendPackets(new S_ItemStatus(l1iteminstance1));

			        // ★ 提示訊息
			        pc.sendPackets(new S_SystemMessage("強化失敗，但裝備受到『完全防爆卷』保護，裝備未毀損，強化值沒有變化。"));

			    } else {
			        FailureEnchant(pc, l1iteminstance1);
			    }
			}

		}
	}

	// 飾品強化卷軸
	public static void scrollOfEnchantAccessory(L1PcInstance pc, L1ItemInstance l1iteminstance
			, L1ItemInstance l1iteminstance1, ClientThread client) {
		if ((l1iteminstance1 == null) || (l1iteminstance1.getBless() >= 128)
				|| (l1iteminstance1.getItem().getType2() != 2
				|| l1iteminstance1.getItem().getType() < 8
				|| l1iteminstance1.getItem().getType() > 12
				|| l1iteminstance1.getItem().getGrade() == -1)) { // 封印中
			pc.sendPackets(new S_ServerMessage(79));
			return;
		}
		int enchant_level = l1iteminstance1.getEnchantLevel();

		if (enchant_level < 0 || enchant_level >= 10) { // 強化上限 + 10
			pc.sendPackets(new S_ServerMessage(79));
			return;
		}

		int rnd = Random.nextInt(100) + 1;
		int enchant_chance_accessory;
		int enchant_level_tmp = enchant_level;
		int itemStatus = 0;
		// +6 時額外獎勵效果判斷
		boolean award = false;
		// 成功率： +0-85% ~ +9-40%
		enchant_chance_accessory = (50 + enchant_level_tmp) / (enchant_level_tmp + 1) + 35;

		if (rnd < enchant_chance_accessory) { // 成功
			if (l1iteminstance1.getEnchantLevel() == 5) {
				award = true;
			}
			switch (l1iteminstance1.getItem().getGrade()) {
				case 0: // 上等
					// 四屬性 +1
					l1iteminstance1.setEarthMr(l1iteminstance1.getEarthMr() + 1);
					itemStatus += L1PcInventory.COL_EARTHMR;
					l1iteminstance1.setFireMr(l1iteminstance1.getFireMr() + 1);
					itemStatus += L1PcInventory.COL_FIREMR;
					l1iteminstance1.setWaterMr(l1iteminstance1.getWaterMr() + 1);
					itemStatus += L1PcInventory.COL_WATERMR;
					l1iteminstance1.setWindMr(l1iteminstance1.getWindMr() + 1);
					itemStatus += L1PcInventory.COL_WINDMR;
					// LV6 額外獎勵：體力與魔力回復量 +1
					if (award) {
						l1iteminstance1.setHpr(l1iteminstance1.getHpr() + 1);
						itemStatus += L1PcInventory.COL_HPR;
						l1iteminstance1.setMpr(l1iteminstance1.getMpr() + 1);
						itemStatus += L1PcInventory.COL_MPR;
					}
					// 裝備中
					if (l1iteminstance1.isEquipped()) {
						pc.addFire(1);
						pc.addWater(1);
						pc.addEarth(1);
						pc.addWind(1);
					}
					break;
				case 1: // 中等
					// HP +2
					l1iteminstance1.setaddHp(l1iteminstance1.getaddHp() + 2);
					itemStatus += L1PcInventory.COL_ADDHP;
					// LV6 額外獎勵：魔防 +1
					if (award) {
						l1iteminstance1.setM_Def(l1iteminstance1.getM_Def() + 1);
						itemStatus += L1PcInventory.COL_M_DEF;
					}
					// 裝備中
					if (l1iteminstance1.isEquipped()) {
						pc.addMaxHp(2);
						if (award) {
							pc.addMr(1);
							pc.sendPackets(new S_SPMR(pc));
						}
					}
					break;
				case 2: // 初等
					// MP +1
					l1iteminstance1.setaddMp(l1iteminstance1.getaddMp() + 1);
					itemStatus += L1PcInventory.COL_ADDMP;
					// LV6 額外獎勵：魔攻 +1
					if (award) {
						l1iteminstance1.setaddSp(l1iteminstance1.getaddSp() + 1);
						itemStatus += L1PcInventory.COL_ADDSP;
					}
					// 裝備中
					if (l1iteminstance1.isEquipped()) {
						pc.addMaxMp(1);
						if (award) {
							pc.addSp(1);
							pc.sendPackets(new S_SPMR(pc));
						}
					}
					break;
				case 3: // 特等
					// 功能台版未實裝。
					break;
				default:
					pc.sendPackets(new S_ServerMessage(79));
					return;
			}
		} else { // 飾品強化失敗
			FailureEnchant(pc, l1iteminstance1);
			pc.getInventory().removeItem(l1iteminstance, 1);
			return;
		}
		SuccessEnchant(pc, l1iteminstance1, client, 1);
		// 更新
		pc.sendPackets(new S_ItemStatus(l1iteminstance1));
		pc.getInventory().saveEnchantAccessory(l1iteminstance1, itemStatus);
		// 刪除
		pc.getInventory().removeItem(l1iteminstance, 1);
	}

	public static void scrollOfEnchantWeaponAttr(L1PcInstance pc, L1ItemInstance scroll,
            L1ItemInstance weapon, ClientThread client) {
int itemId = scroll.getItem().getItemId();

// 只能對武器、且非詛咒
if (weapon == null || weapon.getItem().getType2() != 1 || weapon.getBless() >= 128) {
pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
return;
}
if (weapon.getItem().get_safeenchant() < 0) { // 強化不可
pc.sendPackets(new S_ServerMessage(1453)); // 此裝備無法使用強化。
return;
}

// 區分屬性 kind（0:無 1:地 2:火 4:水 8:風）
int newAttrKind;
switch (itemId) {
case 41429: // 舊：風
case 49144: // 新：風之武器強化卷軸
newAttrKind = 8;
break;
case 41430: // 舊：地
case 49145: // 新：地之武器強化卷軸
newAttrKind = 1;
break;
case 41431: // 舊：水
case 49146: // 新：水之武器強化卷軸
newAttrKind = 4;
break;
case 41432: // 舊：火
case 49147: // 新：火之武器強化卷軸
newAttrKind = 2;
break;
default:
pc.sendPackets(new S_ServerMessage(79));
return;
}

int oldKind  = weapon.getAttrEnchantKind();
int oldLevel = weapon.getAttrEnchantLevel();
boolean isSameAttr = (oldKind == newAttrKind && oldLevel > 0);

// ★ 最高階防誤點：同屬性且已經 5 階 → 不動 & 不吃卷
if (isSameAttr && oldLevel >= 5) {
pc.sendPackets(new S_SystemMessage("屬性已為最高階段，無法再強化。"));
return;
}

// ★ 二次確認：只有「變更成不同屬性」時才需要
if (oldLevel > 0 && oldKind > 0 && oldKind != newAttrKind) {
    int pendingObjId = pc.getPendingAttrEnchantObjId();

    if (pendingObjId != weapon.getId()) {
        // 第一次嘗試把此武器改成其他屬性 → 只警告，不吃卷
        pc.setPendingAttrEnchantObjId(weapon.getId());
        pc.sendPackets(new S_SystemMessage(
                "此武器目前已有屬性，使用此卷軸將變更為其他屬性。若要繼續，請再對同一把武器使用一次卷軸以確認。"));
        return;
    } else {
        // 第二次點同一把武器 → 視為已確認
        pc.setPendingAttrEnchantObjId(0);
    }
}


// ★ 正式進入 85 / 9 / 6 機率流程
int rnd = Random.nextInt(100) + 1;
boolean isSuccess = false;
boolean isFailure = false;

if (oldLevel <= 0) {
// 武器無屬性 → 不吃失敗機率
if (rnd <= 9) {           // 9% 成功
isSuccess = true;
} // 其餘 91% 什麼都不發生
} else {
// 武器已有屬性 → 9% 成功、6% 失敗、其餘沒事
if (rnd <= 9) {           // 9% 成功升階 / 換屬性
isSuccess = true;
} else if (rnd <= 15) {   // 6% 失敗降階
isFailure = true;
}
// 16~100 → 沒事發生
}

if (isSuccess) {
int newLevel;

if (oldLevel <= 0) {
// 無屬性 → 直接 1 階
newLevel = 1;
} else if (isSameAttr) {
// 同屬性 → 升 1 階（最多 5）
newLevel = oldLevel + 1;
if (newLevel > 5) {
newLevel = 5;
}
} else {
// 換屬性 → 1 階重新開始
newLevel = 1;
}

weapon.setAttrEnchantKind(newAttrKind);
weapon.setAttrEnchantLevel(newLevel);

pc.getInventory().updateItem(weapon, L1PcInventory.COL_ATTR_ENCHANT_KIND);
pc.getInventory().saveItem(weapon, L1PcInventory.COL_ATTR_ENCHANT_KIND);
pc.getInventory().updateItem(weapon, L1PcInventory.COL_ATTR_ENCHANT_LEVEL);
pc.getInventory().saveItem(weapon, L1PcInventory.COL_ATTR_ENCHANT_LEVEL);

pc.sendPackets(new S_ServerMessage(1410, weapon.getLogName())); // 對\f1%0附加強大的魔法力量成功。
pc.sendPackets(new S_ItemStatus(weapon));

} else if (isFailure) {
int newLevel = oldLevel - 1;
int newKindAfter = oldKind;

if (newLevel <= 0) {
newLevel = 0;
newKindAfter = 0; // 降到 0 → 屬性清空
}

weapon.setAttrEnchantKind(newKindAfter);
weapon.setAttrEnchantLevel(newLevel);

pc.getInventory().updateItem(weapon, L1PcInventory.COL_ATTR_ENCHANT_KIND);
pc.getInventory().saveItem(weapon, L1PcInventory.COL_ATTR_ENCHANT_KIND);
pc.getInventory().updateItem(weapon, L1PcInventory.COL_ATTR_ENCHANT_LEVEL);
pc.getInventory().saveItem(weapon, L1PcInventory.COL_ATTR_ENCHANT_LEVEL);

pc.sendPackets(new S_ServerMessage(1411, weapon.getLogName())); // 對\f1%0附加魔法失敗。
pc.sendPackets(new S_ItemStatus(weapon));

} else {
// 光芒但沒任何效果
	 pc.sendPackets(new S_SystemMessage("武器發出光芒，但沒有發生任何事。"));
}

// 只有真正進入機率判定時才會走到這裡 → scroll 一定消耗
pc.getInventory().removeItem(scroll, 1);
}


	// 象牙塔對武器施法的卷軸
	public static void scrollOfEnchantWeaponIvoryTower(L1PcInstance pc, L1ItemInstance l1iteminstance
			, L1ItemInstance l1iteminstance1, ClientThread client) {
		int weaponId = l1iteminstance1.getItem().getItemId();
		if ((l1iteminstance1 == null) || (l1iteminstance1.getItem().getType2() != 1)) {
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
			return;
		}
		if (l1iteminstance1.getBless() >= 128) { // 封印中強化不可
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
			return;
		}
		if (weaponId != 7 && weaponId != 35 && weaponId != 48 && weaponId != 73 && weaponId != 105
				&& weaponId != 120 && weaponId != 147 && weaponId != 156
				&& weaponId != 174 && weaponId != 175 && weaponId != 224) { // 非象牙塔裝備
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
			return;
		}
		int safe_enchant = l1iteminstance1.getItem().get_safeenchant();
		if (l1iteminstance1.getEnchantLevel() < safe_enchant) {
			pc.getInventory().removeItem(l1iteminstance, 1);
			SuccessEnchant(pc, l1iteminstance1, client, 1);
		} else {
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
		}
	}

	// 象牙塔對盔甲施法的卷軸
	public static void scrollOfEnchantArmorIvoryTower(L1PcInstance pc, L1ItemInstance l1iteminstance
			, L1ItemInstance l1iteminstance1, ClientThread client) {
		int armorId = l1iteminstance1.getItem().getItemId();
		if ((l1iteminstance1 == null) || (l1iteminstance1.getItem().getType2() != 2)
				|| (l1iteminstance1.getBless() >= 128)) {
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
			return;
		}
		if (armorId != 20028 && armorId != 20082 && armorId != 20126 && armorId != 20173
				&& armorId != 20206 && armorId != 20232 && armorId != 21138
				&& armorId != 21051 && armorId != 21052 && armorId != 21053
				&& armorId != 21054 && armorId != 21055 && armorId != 21056
				&& armorId != 21140 && armorId != 21141) { // 非象牙塔、泡水裝備
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
			return;
		}
		int safe_enchant = l1iteminstance1.getItem().get_safeenchant();
		if (l1iteminstance1.getEnchantLevel() < safe_enchant) {
			pc.getInventory().removeItem(l1iteminstance, 1);
			SuccessEnchant(pc, l1iteminstance1, client, 1);
		} else {
			pc.sendPackets(new S_ServerMessage(79)); // \f1沒有任何事情發生。
		}
	}

	// 強化成功
	private static void SuccessEnchant(L1PcInstance pc, L1ItemInstance item, ClientThread client, int i) {
		int itemType2 = item.getItem().getType2();

		String[][] sa = { {"", "", "", "", ""}
						, {"$246", "", "$245", "$245", "$245"}
						, {"$246", "", "$252", "$252", "$252"}};
		String[][] sb = { {"", "", "", "", ""}
						, {"$247", "", "$247", "$248", "$248"}
						, {"$247", "", "$247", "$248", "$248"}};
		String sa_temp = sa[itemType2][i + 1];
		String sb_temp = sb[itemType2][i + 1];

		pc.sendPackets(new S_ServerMessage(161, item.getLogName(), sa_temp, sb_temp));
		int oldEnchantLvl = item.getEnchantLevel();
		int newEnchantLvl = oldEnchantLvl + i;
		int safe_enchant = item.getItem().get_safeenchant();

		// 自訂：強化成功時清除防爆效果
		if (item.getEnchantProtectType() != 0) {
			item.setEnchantProtectType(0);
		}
		item.setEnchantLevel(newEnchantLvl);
		client.getActiveChar().getInventory().updateItem(item, L1PcInventory.COL_ENCHANTLVL);

		if (newEnchantLvl > safe_enchant) {
			client.getActiveChar().getInventory().saveItem(item, L1PcInventory.COL_ENCHANTLVL);
		}
		if ((item.getItem().getType2() == 1) && (Config.LOGGING_WEAPON_ENCHANT != 0)) {
			if ((safe_enchant == 0) || (newEnchantLvl >= Config.LOGGING_WEAPON_ENCHANT)) {
				LogEnchantTable logenchant = new LogEnchantTable();
				logenchant.storeLogEnchant(pc.getId(), item.getId(), oldEnchantLvl, newEnchantLvl);
			}
		} else if ((item.getItem().getType2() == 2) && (Config.LOGGING_ARMOR_ENCHANT != 0)) {
			if ((safe_enchant == 0) || (newEnchantLvl >= Config.LOGGING_ARMOR_ENCHANT)) {
				LogEnchantTable logenchant = new LogEnchantTable();
				logenchant.storeLogEnchant(pc.getId(), item.getId(), oldEnchantLvl, newEnchantLvl);
			}
		}

		if (item.getItem().getType2() == 2) { // 防具類
			if (item.isEquipped()) {
				if ((item.getItem().getType() < 8
						|| item.getItem().getType() > 12)) {
					pc.addAc(-i);
				}
				int armorId = item.getItem().getItemId();
				// 強化等級+1，魔防+1
				int[] i1 = { 20011, 20110, 21123, 21124, 21125, 21126, 120011 };
				// 抗魔法頭盔、抗魔法鏈甲、林德拜爾的XX、受祝福的 抗魔法頭盔
				for (int j = 0; j < i1.length; j ++) {
					if (armorId == i1[j]) {
						pc.addMr(i);
						pc.sendPackets(new S_SPMR(pc));
						break;
					}
				}
				// 強化等級+1，魔防+2
				int[] i2 = { 20056, 120056, 220056 };
				// 抗魔法斗篷
				for (int j = 0; j < i2.length; j ++) {
					if (armorId == i2[j]) {
						pc.addMr(i * 2);
						pc.sendPackets(new S_SPMR(pc));
						break;
					}
				}
			}
			pc.sendPackets(new S_OwnCharAttrDef(pc));
		}
	}

	// 強化失敗
	private static void FailureEnchant(L1PcInstance pc, L1ItemInstance item) {
		String[] sa = {"", "$245", "$252"}; // ""、藍色的、銀色的
		int itemType2 = item.getItem().getType2();

		if (item.getEnchantLevel() < 0) { // 強化等級為負值
			 sa[itemType2] = "$246"; // 黑色的
		}
		pc.sendPackets(new S_ServerMessage(164, item.getLogName(), sa[itemType2])); // \f1%0%s 強烈的發出%1光芒就消失了。
		pc.getInventory().removeItem(item, item.getCount());
	}

	// 隨機強化等級
	private static int RandomELevel(L1ItemInstance item, int itemId) {
		if ((itemId == L1ItemId.B_SCROLL_OF_ENCHANT_ARMOR) || (itemId == L1ItemId.B_SCROLL_OF_ENCHANT_WEAPON)
				|| (itemId == 140129) || (itemId == 140130)) {
			if (item.getEnchantLevel() <= 2) {
				int j = Random.nextInt(100) + 1;
				if (j < 32) {
					return 1;
				} else if ((j >= 33) && (j <= 76)) {
					return 2;
				} else if ((j >= 77) && (j <= 100)) {
					return 3;
				}
			} else if ((item.getEnchantLevel() >= 3) && (item.getEnchantLevel() <= 5)) {
				int j = Random.nextInt(100) + 1;
				if (j < 50) {
					return 2;
				} else {
					return 1;
				}
			}
		}
		return 1;
	}
}