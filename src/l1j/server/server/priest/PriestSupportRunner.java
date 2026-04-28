package l1j.server.server.priest;

import static l1j.server.server.model.skill.L1SkillId.ADVANCE_SPIRIT;
import static l1j.server.server.model.skill.L1SkillId.AQUA_PROTECTER;
import static l1j.server.server.model.skill.L1SkillId.BERSERKERS;
import static l1j.server.server.model.skill.L1SkillId.BLESSED_ARMOR;
import static l1j.server.server.model.skill.L1SkillId.BLESS_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.BRAVE_AURA;
import static l1j.server.server.model.skill.L1SkillId.BURNING_SPIRIT;
import static l1j.server.server.model.skill.L1SkillId.BURNING_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.CLEAR_MIND;
import static l1j.server.server.model.skill.L1SkillId.CONCENTRATION;
import static l1j.server.server.model.skill.L1SkillId.COUNTER_MAGIC;
import static l1j.server.server.model.skill.L1SkillId.DECREASE_WEIGHT;
import static l1j.server.server.model.skill.L1SkillId.DRESS_EVASION;
import static l1j.server.server.model.skill.L1SkillId.EARTH_SKIN;
import static l1j.server.server.model.skill.L1SkillId.ELEMENTAL_FIRE;
import static l1j.server.server.model.skill.L1SkillId.ENCHANT_VENOM;
import static l1j.server.server.model.skill.L1SkillId.ENCHANT_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.EXTRA_HEAL;
import static l1j.server.server.model.skill.L1SkillId.FIRE_WEAPON;
import static l1j.server.server.model.skill.L1SkillId.FULL_HEAL;
import static l1j.server.server.model.skill.L1SkillId.GLOWING_AURA;
import static l1j.server.server.model.skill.L1SkillId.GREATER_HEAL;
import static l1j.server.server.model.skill.L1SkillId.HEAL;
import static l1j.server.server.model.skill.L1SkillId.IMMUNE_TO_HARM;
import static l1j.server.server.model.skill.L1SkillId.INSIGHT;
import static l1j.server.server.model.skill.L1SkillId.IRON_SKIN;
import static l1j.server.server.model.skill.L1SkillId.LIFE_STREAM;
import static l1j.server.server.model.skill.L1SkillId.MIRROR_IMAGE;
import static l1j.server.server.model.skill.L1SkillId.NATURES_BLESSING;
import static l1j.server.server.model.skill.L1SkillId.PATIENCE;
import static l1j.server.server.model.skill.L1SkillId.PHYSICAL_ENCHANT_DEX;
import static l1j.server.server.model.skill.L1SkillId.PHYSICAL_ENCHANT_STR;
import static l1j.server.server.model.skill.L1SkillId.RESIST_ELEMENTAL;
import static l1j.server.server.model.skill.L1SkillId.RESIST_MAGIC;
import static l1j.server.server.model.skill.L1SkillId.SOUL_OF_FLAME;
import static l1j.server.server.model.skill.L1SkillId.STORM_SHOT;
import static l1j.server.server.model.skill.L1SkillId.WIND_SHOT;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;

import l1j.server.server.GeneralThreadPool;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.model.L1Object;
import l1j.server.server.model.L1World;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1NpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_Dexup;
import l1j.server.server.serverpackets.S_OwnCharAttrDef;
import l1j.server.server.serverpackets.S_SPMR;
import l1j.server.server.serverpackets.S_ServerMessage;
import l1j.server.server.serverpackets.S_SkillIconAura;
import l1j.server.server.serverpackets.S_SkillIconShield;
import l1j.server.server.serverpackets.S_SkillSound;
import l1j.server.server.serverpackets.S_Strup;
import l1j.server.server.templates.L1Skills;

public final class PriestSupportRunner implements Runnable {

    private static final int[] PRIEST_IDS = { 961123, 961124, 961125, 961126, 961127 };

    private static final Map<Integer, ScheduledFuture<?>> TASKS = new ConcurrentHashMap<>();

    private final int _charId;

    private PriestSupportRunner(int charId) {
        _charId = charId;
    }

    public static void startFor(L1PcInstance owner) {
        if (owner == null) return;
        int cid = owner.getId();
        ScheduledFuture<?> f = TASKS.get(cid);
        if (f == null || f.isCancelled()) {
            f = GeneralThreadPool.getInstance().scheduleAtFixedRate(new PriestSupportRunner(cid), 1000, 1000);
            TASKS.put(cid, f);
        }
    }

    public static void stopFor(int charId) {
        ScheduledFuture<?> f = TASKS.remove(charId);
        if (f != null) f.cancel(false);
    }

    @Override
    public void run() {
        L1PcInstance owner = findOwnerById(_charId);
        if (owner == null || owner.getOnlineStatus() == 0) {
            stopFor(_charId);
            return;
        }

        L1NpcInstance priest = findOwnedPriest(owner);
        if (priest == null) return;

        // 每秒回魔
        PriestSettingsStore.regenTick(_charId);

        // 自動補血
        PriestSettingsStore.Settings st = PriestSettingsStore.get(_charId);
        if (st == null || !st.autoSupport) return;
        int hpPercent = owner.getCurrentHp() * 100 / Math.max(1, owner.getMaxHp());
        if (hpPercent <= st.healThreshold) {
            castHealIfPossible(owner, priest);
        }

        // 自動 buff（如果有需要可以打開）
        applySupportBuffs(owner, priest);
    }

    private L1PcInstance findOwnerById(int charId) {
        for (L1PcInstance p : L1World.getInstance().getAllPlayers()) {
            if (p.getId() == charId) return p;
        }
        return null;
    }

    private L1NpcInstance findOwnedPriest(L1PcInstance owner) {
        for (L1Object obj : L1World.getInstance().getVisibleObjects(owner, 15)) {
            if (!(obj instanceof L1NpcInstance)) continue;
            L1NpcInstance npc = (L1NpcInstance) obj;
            if (npc.getMaster() != owner) continue;
            if (npc.getNpcTemplate() == null) continue;
            int id = npc.getNpcTemplate().get_npcId();
            for (int v : PRIEST_IDS) if (v == id) return npc;
        }
        return null;
    }

    private int healSkillFor(int npcId) {
        switch (npcId) {
            case 961123: return HEAL;
            case 961124: return EXTRA_HEAL;
            case 961125: return GREATER_HEAL;
            case 961126: return FULL_HEAL;
            case 961127: return NATURES_BLESSING;
        }
        return HEAL;
    }

    private int[] supportBuffsFor(int npcId) {
        switch (npcId) {
            case 961123:
                return new int[]{ PHYSICAL_ENCHANT_DEX, PHYSICAL_ENCHANT_STR, BLESS_WEAPON };
            case 961124:
                return new int[]{ PHYSICAL_ENCHANT_DEX, PHYSICAL_ENCHANT_STR, BLESS_WEAPON,
                        DECREASE_WEIGHT, EARTH_SKIN, WIND_SHOT, FIRE_WEAPON, LIFE_STREAM, BLESSED_ARMOR };
            case 961125:
                return new int[]{ PHYSICAL_ENCHANT_DEX, PHYSICAL_ENCHANT_STR, BLESS_WEAPON,
                        DECREASE_WEIGHT, IRON_SKIN, STORM_SHOT, BURNING_WEAPON, LIFE_STREAM, BERSERKERS, BLESSED_ARMOR };
            case 961126:
                return new int[]{ PHYSICAL_ENCHANT_DEX, PHYSICAL_ENCHANT_STR, BLESS_WEAPON,
                        DECREASE_WEIGHT, IRON_SKIN, STORM_SHOT, BURNING_WEAPON, LIFE_STREAM,
                        IMMUNE_TO_HARM, RESIST_MAGIC, RESIST_ELEMENTAL, CLEAR_MIND, ADVANCE_SPIRIT, BERSERKERS, ENCHANT_VENOM, BLESSED_ARMOR };
            case 961127:
                return new int[]{ PHYSICAL_ENCHANT_DEX, PHYSICAL_ENCHANT_STR, BLESS_WEAPON,
                        DECREASE_WEIGHT, IRON_SKIN, STORM_SHOT, BURNING_WEAPON, LIFE_STREAM,
                        IMMUNE_TO_HARM, RESIST_MAGIC, RESIST_ELEMENTAL, CLEAR_MIND, ADVANCE_SPIRIT,
                        COUNTER_MAGIC, ELEMENTAL_FIRE, SOUL_OF_FLAME, ENCHANT_WEAPON, AQUA_PROTECTER,
                        BERSERKERS, ENCHANT_VENOM, BURNING_SPIRIT, DRESS_EVASION, MIRROR_IMAGE,
                        CONCENTRATION, PATIENCE, INSIGHT, BLESSED_ARMOR };
            default:
                return new int[0];
        }
    }

    private void applySupportBuffs(L1PcInstance owner, L1NpcInstance priest) {
        int npcId = priest.getNpcTemplate().get_npcId();
        int[] buffs = supportBuffsFor(npcId);
        for (int skillId : buffs) {
            // 依主人武器類型過濾家族：遠程 → 只施弓系；近戰 → 只施近戰系
            boolean ranged = isRangedWeapon(owner);
            if (isRangedFamily(skillId) && !ranged) continue;
            if (isMeleeFamily(skillId) && ranged) continue;

            // 避免低階覆蓋高階：若已存在高階 buff，跳過低階
            if (skillId == FIRE_WEAPON && owner.hasSkillEffect(BURNING_WEAPON)) continue;
            if (skillId == WIND_SHOT && owner.hasSkillEffect(STORM_SHOT)) continue;

            if (owner.hasSkillEffect(skillId)) continue;
            castBuffNoMp(priest, owner, skillId);
        }
    }

    /**
     * 判斷主人的武器是否屬於遠程（弓/鋼拳）。
     */
    private boolean isRangedWeapon(L1PcInstance owner) {
        L1ItemInstance wep = owner.getWeapon();
        if (wep == null) return false;
        if (wep.getItem().getType2() != 1) return false; // 非武器
        int t = wep.getItem().getType();
        // 4: bow, 11: gauntlet（含弩/射擊器）
        return t == 4 || t == 11;
    }

    private boolean isRangedFamily(int skillId) {
        return (skillId == STORM_SHOT || skillId == WIND_SHOT);
    }

    private boolean isMeleeFamily(int skillId) {
        return (skillId == BURNING_WEAPON || skillId == FIRE_WEAPON || skillId == BURNING_SPIRIT || skillId == SOUL_OF_FLAME);
    }

    private void castBuffNoMp(L1NpcInstance priest, L1PcInstance owner, int skillId) {
        L1Skills sk = SkillsTable.getInstance().getTemplate(skillId);
        if (sk == null) return;

        if (skillId == BLESSED_ARMOR) {
            L1ItemInstance armor = null;
            for (L1ItemInstance it : owner.getInventory().getItems()) {
                if (it.getItem().getType2() == 2 && it.getItem().getType() == 2) { armor = it; break; }
            }
            if (armor != null) {
                armor.setSkillArmorEnchant(owner, skillId, sk.getBuffDuration() * 1000);
                owner.sendPackets(new S_ServerMessage(161, armor.getLogName(), "$245", "$247"));
            }
            // 設定角色身上的技能效果以便 UI 倒數與 AI 判斷
            owner.setSkillEffect(BLESSED_ARMOR, sk.getBuffDuration() * 1000);
            // 推送對應圖示（Shield 類）
            PriestBuffIconResolver.sendIcon(owner, BLESSED_ARMOR, sk.getBuffDuration());
            return;
        }

        owner.setSkillEffect(skillId, sk.getBuffDuration() * 1000);
        int gfx = sk.getCastGfx();
        if (gfx > 0) {
            owner.sendPackets(new S_SkillSound(owner.getId(), gfx));
            owner.broadcastPacket(new S_SkillSound(owner.getId(), gfx));
        }
        PriestBuffIconResolver.sendIcon(owner, skillId, sk.getBuffDuration());
    }

    /**
     * ⭐ 這裡開始：補血量 + MP 消耗都吃祭司智力 ⭐
     */
    private void castHealIfPossible(L1PcInstance owner, L1NpcInstance priest) {
        if (owner == null || priest == null || priest.getNpcTemplate() == null) {
            return;
        }

        int skillId = healSkillFor(priest.getNpcTemplate().get_npcId());
        L1Skills sk = SkillsTable.getInstance().getTemplate(skillId);
        if (sk == null) {
            return;
        }

        PriestSettingsStore.Settings st = PriestSettingsStore.get(_charId);
        if (st == null) {
            return;
        }

        // 目前這隻祭司的「有效智力」（包含吃藥後 iq_total）
        int priestInt = resolvePriestInt(owner, priest);

        // 基礎 MP 消耗 + 智力減免
        int baseMpCost = Math.max(0, sk.getMpConsume());
        int mpCost = calcPriestMpCost(baseMpCost, priestInt);
        if (mpCost <= 0 || st.mp < mpCost) {
            return;
        }

     // 補血量公式：基礎值(隨機) + 祭司智力 * 係數
        int amount = calcPriestHealing(skillId, priestInt);
        if (amount <= 0) {
            return;
        }

        PriestSettingsStore.consumeMp(_charId, mpCost);
        owner.healHp(amount);
        int gfx = sk.getCastGfx();
        if (gfx > 0) {
            owner.sendPackets(new S_SkillSound(owner.getId(), gfx));
            owner.broadcastPacket(new S_SkillSound(owner.getId(), gfx));
        }
    }

    /**
     * 取得這隻祭司目前的「有效智力」：
     * 1) 先從 character_priest_iq 找對應 owner + priest_item_id 的 iq_total
     * 2) 找不到就退回 NPC 原始 intel
     */
    private int resolvePriestInt(L1PcInstance owner, L1NpcInstance priest) {
        try {
            if (owner == null || priest == null || priest.getNpcTemplate() == null) {
                return 0;
            }

            // 1) 最高優先：若祭司 NPC 本身已經綁定了召喚道具的 item_objid，就直接以該道具的智力為準
            try {
                int boundItemObjId = 0;
                try { boundItemObjId = priest.getPriestItemObjId(); } catch (Throwable ignore0) {}
                if (boundItemObjId > 0) {
                    PriestIqDAO2.Row row = PriestIqDAO2.byItemObjId(boundItemObjId);
                    if (row != null && row.iqTotal > 0) {
                        return row.iqTotal;
                    }
                }
            } catch (Throwable ignore) {
            }

            // 2) 次優先：仍然保留 owner + priest_item_id 的查法，給尚未完整轉換的資料使用
            int npcId = priest.getNpcTemplate().get_npcId();
            int itemId = priestItemIdForNpc(npcId);
            if (itemId > 0) {
                PriestIqDAO2.Row row = PriestIqDAO2.byOwnerAndItem(owner.getId(), itemId);
                if (row != null && row.iqTotal > 0) {
                    return row.iqTotal;
                }
            }

            // 3) 最後回退：NPC 原本的 INT
            return priest.getNpcTemplate().get_int();
        } catch (Throwable t) {
            return 0;
        }
    }



    /**
     * 祭司 NPC id 對應召喚道具 itemId（與你現有設定對應）。
     */
    private int priestItemIdForNpc(int npcId) {
        switch (npcId) {
            case 961123: return 240123; // 低階
            case 961124: return 240124; // 中階
            case 961125: return 240125; // 高階
            case 961126: return 240126; // 頂級
            case 961127: return 240127; // 神話
            default:     return 0;
        }
    }

    /**
     * 類似角色 INT 減免 MP：
     * 以 12 為基準，INT 每高 2 點 MP 消耗 -1（最低 1）。
     */
    private int calcPriestMpCost(int baseMpCost, int priestInt) {
        int cost = baseMpCost;
        if (cost <= 0) {
            return 0;
        }
        if (priestInt > 12) {
            int reduce = (priestInt - 12) / 2; // 14→-1, 16→-2, 18→-3 ...
            cost -= reduce;
        }
        if (cost < 1) {
            cost = 1;
        }
        return cost;
    }

    /**
     * 依智力微調補血量：
     * 以 INT 16 為基準，每高 1 點 +3%，每低 1 點 -3%，限制在 0.5x～2x。
     */
    /**
     * 祭司補血公式（依技能階級）：
     * 基礎補血量 = 隨機(baseMin~baseMax)
     * 最終補血量 = 基礎補血量 + priestInt * k
     * k：HEAL=1、EXTRA_HEAL=2、GREATER_HEAL=3、FULL_HEAL=4、NATURES_BLESSING=5
     */
    private int calcPriestHealing(int skillId, int priestInt) {
        int baseMin;
        int baseMax;
        int k;
        switch (skillId) {
            case HEAL:
                baseMin = 2; baseMax = 8; k = 1;
                break;
            case EXTRA_HEAL:
                baseMin = 5; baseMax = 40; k = 2;
                break;
            case GREATER_HEAL:
                baseMin = 11; baseMax = 88; k = 3;
                break;
            case FULL_HEAL:
                baseMin = 14; baseMax = 168; k = 4;
                break;
            case NATURES_BLESSING:
                baseMin = 13; baseMax = 156; k = 5;
                break;
            default:
                baseMin = 2; baseMax = 8; k = 1;
                break;
        }
        int base = ThreadLocalRandom.current().nextInt(baseMin, baseMax + 1);
        long amount = (long) base + (long) priestInt * k;

        if (amount < 1) {
            amount = 1;
        }
        if (amount > Integer.MAX_VALUE) {
            amount = Integer.MAX_VALUE;
        }
        return (int) amount;
    }


    /** 以下是原本的 buff icon 顯示邏輯，完全沒改動 */

    private void sendBuffIcon(L1PcInstance pc, int skillId, int durationSec) {
        switch (skillId) {
            case PHYSICAL_ENCHANT_DEX: // 通暢氣脈術
                pc.sendPackets(new S_Dexup(pc, 5, durationSec));
                break;
            case PHYSICAL_ENCHANT_STR: // 體魄強健術
                pc.sendPackets(new S_Strup(pc, 5, durationSec));
                break;
            case EARTH_SKIN:           // 大地防護
                pc.sendPackets(new S_SkillIconShield(3, durationSec));
                break;
            case DRESS_EVASION:        // 暗影閃避
                pc.sendPackets(new S_SkillIconShield(6, durationSec));
                break;
            case IRON_SKIN:            // 鋼鐵防護
                pc.sendPackets(new S_SkillIconShield(10, durationSec));
                break;
            case FIRE_WEAPON:          // 火焰武器
                pc.sendPackets(new S_SkillIconAura(147, durationSec));
                break;
            case BURNING_WEAPON:       // 烈炎武器
                pc.sendPackets(new S_SkillIconAura(162, durationSec));
                break;
            case WIND_SHOT:            // 風之神射
                pc.sendPackets(new S_SkillIconAura(148, durationSec));
                break;
            case STORM_SHOT:           // 風之疾走
                pc.sendPackets(new S_SkillIconAura(155, durationSec));
                break;
            case GLOWING_AURA:         // 激勵士氣
                pc.sendPackets(new S_SkillIconAura(113, durationSec));
                break;
            case BRAVE_AURA:           // 衝擊士氣
                pc.sendPackets(new S_SkillIconAura(116, durationSec));
                break;
            case RESIST_MAGIC:         // 魔法相消/抗魔 (僅更新數值)
                pc.sendPackets(new S_SPMR(pc));
                break;
            case RESIST_ELEMENTAL:     // 屬性防禦 (僅更新屬性UI)
                pc.sendPackets(new S_OwnCharAttrDef(pc));
                break;
            default:
                break;
        }
    }
}
