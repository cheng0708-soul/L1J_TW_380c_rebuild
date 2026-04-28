package l1j.server.server.afk;

import java.util.Locale;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.afk.CoopFollowService;
import l1j.server.server.serverpackets.S_SystemMessage;

public class AfkUiRouter {

    public static boolean tryHandleNpcAction(L1PcInstance pc, String htmlId, String action, String param) {
        if (action == null) return false;
        switch (action) {
            case "sum_1": AfkService.toggleRunning(pc); return true;
            case "sum_2": AfkService.toggleTeleportMode(pc); return true;
            case "sum_3": AfkService.togglePatrol(pc, 10, "巡邏模式10秒"); return true;
            case "sum_4": AfkService.togglePatrol(pc, 30, "巡邏模式30秒"); return true;
            case "sum_5": AfkService.togglePatrol(pc, 50, "巡邏模式50秒"); return true;
            case "sum_6": AfkService.togglePatrol(pc, 100, "巡邏模式100秒"); return true;
            case "sum_7": AfkService.beginSkillRegister(pc); return true;
            case "sum_8": AfkService.listSkills(pc); return true;
            case "sum_9": AfkService.clearSkills(pc); return true;
            case "follow_system_toggle": CoopFollowService.toggleFollowSystem(pc); return true;
            case "follow_target": CoopFollowService.requestFollowTarget(pc); return true;
            case "coop_battle_toggle": CoopFollowService.toggleCoopBattle(pc); return true;
            case "follow_buff_register": CoopFollowService.beginBuffRegister(pc); return true;
            case "follow_buff_query": CoopFollowService.listBuffs(pc); return true;
            case "follow_buff_clear": CoopFollowService.clearBuffs(pc); return true;
            case "follow_heal_skill_cycle": CoopFollowService.cycleHealSkill(pc); return true;
            case "follow_heal_percent_minus": CoopFollowService.adjustHealPercent(pc, -10); return true;
            case "follow_heal_percent_plus": CoopFollowService.adjustHealPercent(pc, +10); return true;
        }

        if (action.toLowerCase(Locale.ROOT).startsWith("afk:")) {
            String rest = action.substring(4);
            if ("toggle".equals(rest)) { AfkService.toggleRunning(pc); return true; }
            if ("tp".equals(rest)) { AfkService.toggleTeleportMode(pc); return true; }
        }
        return false;
    }
}
