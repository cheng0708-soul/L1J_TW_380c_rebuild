package l1j.server.server.priest;

import static l1j.server.server.model.skill.L1SkillId.EARTH_SKIN;
import static l1j.server.server.model.skill.L1SkillId.IRON_SKIN;
import static l1j.server.server.model.skill.L1SkillId.PHYSICAL_ENCHANT_DEX;
import static l1j.server.server.model.skill.L1SkillId.PHYSICAL_ENCHANT_STR;
import static l1j.server.server.model.skill.L1SkillId.RESIST_ELEMENTAL;
import static l1j.server.server.model.skill.L1SkillId.RESIST_MAGIC;
import static l1j.server.server.model.skill.L1SkillId.BLESSED_ARMOR;

import java.util.HashMap;
import java.util.Map;

import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_Dexup;
import l1j.server.server.serverpackets.S_OwnCharAttrDef;
import l1j.server.server.serverpackets.S_SPMR;
import l1j.server.server.serverpackets.S_SkillIconAura;
import l1j.server.server.serverpackets.S_SkillIconShield;
import l1j.server.server.serverpackets.S_Strup;
import l1j.server.server.templates.L1Skills;

/**
 * 以 DB 的 skills.castgfx 為基準，對應到客戶端 BUFF 圖示封包。
 * 未具專用圖示的技能維持數值更新，不造圖示。
 */
public final class PriestBuffIconResolver {

    /** castgfx -> AuraIconId 對應（以 DB 內容掃描補齊） */
    private static final Map<Integer, Integer> AURA_BY_CASTGFX = new HashMap<Integer, Integer>() {{
        // 火焰武器(148): castgfx=2182 -> Aura 147
        put(2182, 147);
        // 烈炎武器(163): castgfx=2242 -> Aura 162
        put(2242, 162);
        // 風之神射(149): castgfx=2246 -> Aura 148
        put(2246, 148);
        // 暴風神射(166): castgfx=2248 -> Aura 165
        put(2248, 165);
        // 激勵士氣(114): castgfx=2277 -> Aura 113
        put(2277, 113);
        // 衝擊士氣(117): castgfx=3942 -> Aura 116
        put(3942, 116);
    }};

    /** 直接以技能 ID 對應 ShieldIconId */
    private static final Map<Integer, Integer> SHIELD_BY_SKILL = new HashMap<Integer, Integer>() {{
        // 大地防護(151) -> 6
        put(EARTH_SKIN, 6);
        // 鋼鐵防護(168) -> 10
        put(IRON_SKIN, 10);
        // 鎧甲護持(21) -> 3
        put(BLESSED_ARMOR, 3);
    }};

    private PriestBuffIconResolver() {}

    public static void sendIcon(L1PcInstance pc, int skillId, int durationSec) {
        // DEX/STR 類型：專用封包
        if (skillId == PHYSICAL_ENCHANT_DEX) { pc.sendPackets(new S_Dexup(pc, 5, durationSec)); return; }
        if (skillId == PHYSICAL_ENCHANT_STR) { pc.sendPackets(new S_Strup(pc, 5, durationSec)); return; }

        // 屬防/抗魔：更新數值 UI
        if (skillId == RESIST_ELEMENTAL) { pc.sendPackets(new S_OwnCharAttrDef(pc)); return; }
        if (skillId == RESIST_MAGIC) { pc.sendPackets(new S_SPMR(pc)); return; }

        // 盾 icon：照技能 ID 對應
        Integer shield = SHIELD_BY_SKILL.get(skillId);
        if (shield != null) {
            pc.sendPackets(new S_SkillIconShield(shield, durationSec));
            return;
        }

        // Aura icon：利用 DB 的 castgfx 對應
        L1Skills sk = SkillsTable.getInstance().getTemplate(skillId);
        if (sk != null) {
            Integer auraId = AURA_BY_CASTGFX.get(sk.getCastGfx());
            if (auraId != null) {
                pc.sendPackets(new S_SkillIconAura(auraId, durationSec));
                return;
            }
        }

        // 其餘（例：水之防護(160), 毒性抵抗(104), 暗影閃避(106)）客戶端沒有預設圖示：不送。
    }
}