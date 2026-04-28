package l1j.server.server.model.shop;

import l1j.server.server.model.Instance.L1PShopNpcInstance;

public final class PShopCurrencyUtil {
    private PShopCurrencyUtil() {}
    public static final int COIN = 40308;   // Adena
    public static final int YUAN = 240107;  // 元寶

    /**
     * 決定交易幣別：若對象是攤位 NPC，使用其模式；否則回傳金幣預設。
     */
    public static int resolveCurrencyId(Object partner) {
        try {
            if (partner instanceof L1PShopNpcInstance) {
                return ((L1PShopNpcInstance) partner).getModeItemId();
            }
        } catch (Throwable ignore) {}
        return COIN;
    }

    public static String currencyName(int itemId) {
        return itemId == YUAN ? "元寶" : "金幣";
    }
}
