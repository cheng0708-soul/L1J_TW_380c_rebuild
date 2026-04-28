
package l1j.server.server.model.shop;

import l1j.server.server.model.L1Object;
import l1j.server.server.model.Instance.L1PShopNpcInstance;
import l1j.server.server.model.Instance.L1PcInstance;

/** Minimal fallback: only set title; avoid Broadcaster & S_DoActionShop to fit all bases */
public final class ShopBroadcastUtil {

    private ShopBroadcastUtil() {}

    public static void open(L1Object who, String title) {
        if (who == null) return;
        if (who instanceof L1PShopNpcInstance) {
            try { ((L1PShopNpcInstance) who).setTitle(title); } catch (Throwable t) {}
        } else if (who instanceof L1PcInstance) {
            try { ((L1PcInstance) who).setTitle(title); } catch (Throwable t) {}
        }
    }
}
