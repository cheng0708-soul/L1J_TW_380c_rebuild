package l1j.server.server.afk;

import l1j.server.server.datatables.SprTable;
import l1j.server.server.model.AcceleratorChecker.ACT_TYPE;
import l1j.server.server.model.Instance.L1PcInstance;

/**
 * AFK 走路/攻擊速度計算工具。
 *
 * 讓掛機系統的移動/攻擊節奏能依照「當前形象(spr/gfx)」與
 * haste/brave/waffle/第三段加速等狀態做一致的速度計算。
 *
 * 計算規則刻意對齊 {@link l1j.server.server.model.AcceleratorChecker} 的 getRightInterval 邏輯。
 */
public final class AfkSpeedUtil {

    // 與 AcceleratorChecker 保持一致
    private static final double HASTE_RATE = 0.75;         // 速度 * 1.33
    private static final double WAFFLE_RATE = 0.87;        // 速度 * 1.15
    private static final double DOUBLE_HASTE_RATE = 0.375; // 速度 * 2.66

    // 兜底（避免 SprTable 取不到或回 0）
    private static final int DEFAULT_MOVE_MS = 500;
    private static final int DEFAULT_ATTACK_MS = 600;

    private AfkSpeedUtil() {}

    public static int getMoveIntervalMs(L1PcInstance pc) {
        return getIntervalMs(pc, ACT_TYPE.MOVE);
    }

    public static int getAttackIntervalMs(L1PcInstance pc) {
        return getIntervalMs(pc, ACT_TYPE.ATTACK);
    }

    /**
     * 依照當前 gfx + 裝備/動作 + 速度狀態，計算理論間隔(ms)。
     */
    public static int getIntervalMs(L1PcInstance pc, ACT_TYPE type) {
        if (pc == null) return (type == ACT_TYPE.MOVE ? DEFAULT_MOVE_MS : DEFAULT_ATTACK_MS);

        int interval = 0;
        try {
            switch (type) {
            case ATTACK:
                interval = SprTable.getInstance().getAttackSpeed(pc.getTempCharGfx(), pc.getCurrentWeapon() + 1);
                break;
            case MOVE:
                interval = SprTable.getInstance().getMoveSpeed(pc.getTempCharGfx(), pc.getCurrentWeapon());
                break;
            case SPELL_DIR:
                interval = SprTable.getInstance().getDirSpellSpeed(pc.getTempCharGfx());
                break;
            case SPELL_NODIR:
                interval = SprTable.getInstance().getNodirSpellSpeed(pc.getTempCharGfx());
                break;
            default:
                interval = 0;
                break;
            }
        } catch (Throwable ignore) {
            interval = 0;
        }

        if (interval <= 0) {
            interval = (type == ACT_TYPE.MOVE ? DEFAULT_MOVE_MS : DEFAULT_ATTACK_MS);
        }

        // 一段加速（haste/slow）
        try {
            switch (pc.getMoveSpeed()) {
            case 1: // 加速術
                interval = (int) Math.round(interval * HASTE_RATE);
                break;
            case 2: // 緩速術
                interval = (int) Math.round(interval / HASTE_RATE);
                break;
            default:
                break;
            }
        } catch (Throwable ignore) {}

        // 二段加速（勇水/精餅/神疾/超級加速/血渴）
        try {
            switch (pc.getBraveSpeed()) {
            case 1: // 勇水
                interval = (int) Math.round(interval * HASTE_RATE);
                break;
            case 3: // 精餅
                interval = (int) Math.round(interval * WAFFLE_RATE);
                break;
            case 4: // 神疾、風走、行走
                if (type == ACT_TYPE.MOVE) {
                    interval = (int) Math.round(interval * HASTE_RATE);
                }
                break;
            case 5: // 超級加速
                interval = (int) Math.round(interval * DOUBLE_HASTE_RATE);
                break;
            case 6: // 血之渴望
                if (type == ACT_TYPE.ATTACK) {
                    interval = (int) Math.round(interval * HASTE_RATE);
                }
                break;
            default:
                break;
            }
        } catch (Throwable ignore) {}

        // 生命之樹果實
        try {
            if (pc.isRibrave() && type == ACT_TYPE.MOVE) {
                interval = (int) Math.round(interval * WAFFLE_RATE);
            }
        } catch (Throwable ignore) {}

        // 三段加速
        try {
            if (pc.isThirdSpeed()) {
                interval = (int) Math.round(interval * WAFFLE_RATE);
            }
        } catch (Throwable ignore) {}

        // 風之枷鎖（非移動）
        try {
            if (pc.isWindShackle() && type != ACT_TYPE.MOVE) {
                interval = (int) Math.round(interval / 2.0);
            }
        } catch (Throwable ignore) {}

        // 寵物競速例外
        try {
            if (pc.getMapId() == 5143) {
                interval = (int) Math.round(interval * 0.1);
            }
        } catch (Throwable ignore) {}

        // 最終保護：避免 0/負值造成無限快
        if (interval < 80) interval = 80;
        if (interval > 3000) interval = 3000;

        return interval;
    }
}
