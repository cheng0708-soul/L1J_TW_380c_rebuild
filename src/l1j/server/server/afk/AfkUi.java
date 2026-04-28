
package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;

/**
 * 直接由 C_NPCAction 呼叫的 UI 路由（對話檔按鈕 -> 後台行為）
 * 與 AfkUiRouter 內容一致，保留雙路徑以相容舊分支。
 */
public final class AfkUi {

    private AfkUi() {}

    public static boolean onAction(L1PcInstance pc, String action) {
        if (pc == null || action == null) return false;

        // 標準格式：sum_1..sum_9
        if (action.startsWith("sum_")) {
            int code = -1;
            try { code = Integer.parseInt(action.substring(4)); } catch (Exception ignore) {}

            switch (code) {
                case 1: AfkService.toggleRunning(pc); return true;
                case 2: AfkService.toggleTeleportMode(pc); pc.sendPackets(new S_SystemMessage("瞬移模式：已登錄")); return true;

                case 3: AfkService.setPatrol(pc, 10); pc.sendPackets(new S_SystemMessage("巡邏模式：已登錄 (10格)")); return true;
                case 4: AfkService.setPatrol(pc, 30); pc.sendPackets(new S_SystemMessage("巡邏模式：已登錄 (30格)")); return true;
                case 5: AfkService.setPatrol(pc, 50); pc.sendPackets(new S_SystemMessage("巡邏模式：已登錄 (50格)")); return true;
                case 6: AfkService.setPatrol(pc, 100); pc.sendPackets(new S_SystemMessage("巡邏模式：已登錄 (100格)")); return true;

                case 7: AfkService.beginSkillRegister(pc); return true;
                case 8: AfkService.listSkills(pc); return true;
                case 9: AfkService.clearSkills(pc); return true;
            }
        }

        // 兼容 afk:toggle / afk:tp 短指令
        if (action.startsWith("afk:")) {
            String rest = action.substring(4);
            if ("toggle".equals(rest)) { AfkService.toggleRunning(pc); return true; }
            if ("tp".equals(rest)) { AfkService.toggleTeleportMode(pc); pc.sendPackets(new S_SystemMessage("瞬移模式：已登錄")); return true; }
        }
        return false;
    }
}
