package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * 需要時再把真正的瞬移/回村邏輯填進來；先提供同名方法避免掛機流程呼叫失敗。
 */
public final class AfkTeleportUtil {

    private AfkTeleportUtil() {}

    /** 佔位：避免編譯錯誤。實際應改為呼叫你專案的瞬移/回村工具或封包。 */
    public static void reposition(final L1PcInstance pc) {
        // no-op
    }
}