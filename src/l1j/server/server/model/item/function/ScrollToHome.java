// CONSOLIDATED PATCH v3 2025-09-08 00:06:27
package l1j.server.server.model.item.function;

import l1j.server.server.afk.AfkTeleportUtil;
import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;

/** 兼容版回村卷（暫不 implements 介面，先讓專案編譯過） */
public class ScrollToHome {
    public void execute(int objectId, L1PcInstance pc, L1ItemInstance item) {
        try { AfkTeleportUtil.reposition(pc); } catch (Throwable ignore) {}
    }
}
