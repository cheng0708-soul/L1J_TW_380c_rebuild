package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * 歷史相容入口。
 * 真正狀態只保留在 AfkService，避免 Controller / Service 兩套狀態分裂。
 */
public class AfkController {

    /** 讀取掛機狀態 */
    public static boolean isAfk(final L1PcInstance pc) {
        return AfkService.isRunning(pc);
    }

    /** 切換掛機開關：委派給 AfkService */
    public static void toggle(final L1PcInstance pc) {
        if (pc == null) return;
        AfkService.toggleRunning(pc);
    }

    /** 供死亡/回村/下線等情況統一關閉 */
    public static void forceOff(final L1PcInstance pc) {
        if (pc == null) return;
        AfkService.stop(pc);
    }
}
