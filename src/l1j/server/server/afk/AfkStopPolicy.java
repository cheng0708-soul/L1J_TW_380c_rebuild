package l1j.server.server.afk;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 最小版停掛策略（可編譯、可工作）
 * - 用來被 C_ItemUSe.java 呼叫：
 *   * shouldStopForUseTypeInt(int use_type): 針對 use_type 判定是否應停掛
 *   * isStopItem(int itemId): 針對特定道具 ID 判定是否應停掛
 *
 * 你可以依照實測再把名單補齊，這裡先含：
 * - 40117 主城傳送（指定地點） → 停掛
 * - 40079 一般回家、40095 象牙回家 → 停掛
 * - 40100、40099 隨機瞬移 → 排除（不停掛）
 */
public final class AfkStopPolicy {

    // 「nomal」對應 51（你先前的說明）
    private static final int USE_TYPE_NOMAL = 51;

    // 需要停掛的特定卷軸（可自行擴充）
    private static final Set<Integer> STOP_ITEM_IDS = new HashSet<>(
        Arrays.asList(
            40117, // 主城傳送
            40079, // 回家卷
            40095  // 象牙回家
        )
    );

    // 明確排除（不應停掛）
    private static final Set<Integer> EXCLUDE_ITEM_IDS = new HashSet<>(
        Arrays.asList(
            40100, // 隨機瞬移（非指定）
            40099  // 隨機瞬移（非指定）
        )
    );

    private AfkStopPolicy() {}

    /** 依 use_type 判斷是否應停掛 */
    public static boolean shouldStopForUseTypeInt(final int use_type) {
        return use_type == USE_TYPE_NOMAL;
    }

    /** 依特定道具 ID 判斷是否應停掛（排除隨機瞬移） */
    public static boolean isStopItem(final int itemId) {
        if (EXCLUDE_ITEM_IDS.contains(itemId)) return false;
        return STOP_ITEM_IDS.contains(itemId);
    }
}