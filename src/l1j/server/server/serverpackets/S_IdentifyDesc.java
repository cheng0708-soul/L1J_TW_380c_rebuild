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

import l1j.server.server.Opcodes;
import l1j.server.server.model.Instance.L1ItemInstance;

public class S_IdentifyDesc extends ServerBasePacket {

	private byte[] _byte = null;

	/**
	 * 確認スクロール使用時のメッセージを表示する
	 */
	public S_IdentifyDesc(L1ItemInstance item) {
		buildPacket(item);
	}

	private void buildPacket(L1ItemInstance item) {
		writeC(Opcodes.S_OPCODE_IDENTIFYDESC);
		// ★ 這行很重要，上一版沒送才會閃退
		writeH(item.getItem().getItemDescId());

		StringBuilder name = new StringBuilder();

		if (item.getItem().getBless() == 0) {
			name.append("$227 "); // 祝福された
		}
		else if (item.getItem().getBless() == 2) {
			name.append("$228 "); // 呪われた
		}

		name.append(item.getItem().getIdentifiedNameId());

		// 旅館鑰匙
		if (item.getItem().getItemId() == 40312 && item.getKeyId() != 0) {
			name.append(item.getInnKeyName());
		}

		if (item.getItem().getType2() == 1) { // weapon
			writeH(134); // \f1%0：小さなモンスター打擊%1 大きなモンスター打擊%2
			writeC(3);
			writeS(name.toString());
			int enchant = item.getEnchantLevel();
			StringBuilder small = new StringBuilder();
			small.append(item.getItem().getDmgSmall()).append("+").append(enchant);

			// 高強化武器 +11 以上 暴擊率 / 暴擊傷害 文字（+11~+30）
			int enchantLevel = enchant;
			if (enchantLevel > 30) {
				enchantLevel = 30;
			}
			if (enchantLevel >= 11) {
				int bonusLv = enchantLevel - 10; // +11 時為 1，+30 時為 20
				int critRate = bonusLv; // 暴擊率（％）
				int critDmgPercent = bonusLv * 10; // 暴擊傷害％
				small.append("\n暴擊率+").append(critRate).append("%");
				small.append("\n暴擊傷害+").append(critDmgPercent).append("%");
			}

			writeS(small.toString());
			writeS(item.getItem().getDmgLarge() + "+" + enchant);

		}
		else if (item.getItem().getType2() == 2) { // armor
			if (item.getItem().getItemId() == 20383) { // 騎馬用ヘルム
				writeH(137); // \f1%0：使用可能回数%1［重さ%2］
				writeC(3);
				writeS(name.toString());
				writeS(String.valueOf(item.getChargeCount()));
			}
			else {
				writeH(135); // \f1%0：防御力%1 防御具
				writeC(2);

				int baseAc = Math.abs(item.getItem().get_ac());
				int enchant = item.getEnchantLevel();

				// 第二個參數：原本是 "AC+Enchant"
				StringBuilder value = new StringBuilder();
				value.append(baseAc).append("+").append(enchant);

				// ★ 高強化防具 +11 以上 額外能力文字（+11~+30）
				int enchantLevel = enchant;
				if (enchantLevel > 30) {
					enchantLevel = 30;
				}
				if (enchantLevel >= 11) {
					int bonusLv = enchantLevel - 10; // +11 =1, +30 =20
					int mrBonus = 4 + bonusLv;   // 魔法額外防禦
					int drBonus = bonusLv;       // 傷害減免
					int attrBonus = 3 + bonusLv; // 四屬性防禦（火水風地）

					value.append("\n力量+").append(bonusLv);
					value.append("\n敏捷+").append(bonusLv);
					value.append("\n智力+").append(bonusLv);
					value.append("\n精神+").append(bonusLv);
					value.append("\n魔法防禦+").append(mrBonus);
					value.append("\n傷害減免+").append(drBonus);
					value.append("\n四屬性防禦+").append(attrBonus);
				}
				// ----------------------------------------

				writeS(name.toString());
				writeS(value.toString());
			}

		}
		else if (item.getItem().getType2() == 0) { // etcitem
			if (item.getItem().getType() == 1) { // wand
				writeH(137); // \f1%0：使用可能回数%1［重さ%2］
				writeC(3);
				writeS(name.toString());
				writeS(String.valueOf(item.getChargeCount()));
			}
			else if (item.getItem().getType() == 2) { // light系アイテム
				writeH(138);
				writeC(2);
				name.append(": $231 "); // 残りの燃料
				name.append(String.valueOf(item.getRemainingTime()));
				writeS(name.toString());
			}
			else if (item.getItem().getType() == 7) { // food
				writeH(136); // \f1%0：満腹度%1［重さ%2］
				writeC(3);
				writeS(name.toString());
				writeS(String.valueOf(item.getItem().getFoodVolume()));
			}
			else {
				writeH(138); // \f1%0：［重さ%1］
				writeC(2);
				writeS(name.toString());
			}
			writeS(String.valueOf(item.getWeight()));
		}
	}

	@Override
	public byte[] getContent() {
		if (_byte == null) {
			_byte = getBytes();
		}
		return _byte;
	}
}
