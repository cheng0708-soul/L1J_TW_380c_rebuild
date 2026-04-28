package l1j.server.server.model;

import java.util.Calendar;
import java.util.Map;

import l1j.server.server.datatables.BossSpawnTable;
import l1j.server.server.model.Instance.L1NpcInstance;

/**
 * 產生所有 BOSS 的狀態列表（給吃檔程式用）
 * 每一行格式：
 * 巴風特-------已重生<br>
 * 死亡騎士-------0天2時43分50秒重生<br>
 */
public class BossStatusBuilder {

    /**
     * 回傳全部 BOSS 的簡易狀態字串（給吃檔程式用）
     */
    public static String buildAll() {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<Integer, L1BossSpawn> e : BossSpawnTable.getBossSpawns().entrySet()) {
            L1BossSpawn spawn = e.getValue();
            sb.append(buildLine(spawn)).append("<br>\n");
        }

        return sb.toString();
    }

    /**
     * 建立某隻 BOSS 的簡易狀態
     * 格式：
     * 巴風特-------已重生
     * 死亡騎士-------0天2時43分50秒重生
     */
    private static String buildLine(L1BossSpawn spawn) {
        String name = spawn.getName(); // spawnlist_boss 的中文名稱
        int npcId = spawn.getNpcId();

        // 判斷是否活著
        if (isAlive(npcId)) {
            return name + "-------已重生";
        }

        // 取得排程時間
        Calendar next = spawn.getScheduleTime();
        if (next == null) {
            return name + "-------時間未知";
        }

        long diff = next.getTimeInMillis() - System.currentTimeMillis();
        if (diff < 0) {
            diff = 0;
        }

        long sec = diff / 1000;
        long day = sec / 86400;
        sec %= 86400;
        long hour = sec / 3600;
        sec %= 3600;
        long min = sec / 60;
        sec %= 60;

        return name + "-------" + day + "天" + hour + "時" + min + "分" + sec + "秒重生";
    }

    /**
     * 世界上是否有這個 npcId 的活體
     */
    private static boolean isAlive(int npcId) {
        for (L1Object obj : L1World.getInstance().getAllVisibleObjects().values()) {
            if (obj instanceof L1NpcInstance) {
                L1NpcInstance npc = (L1NpcInstance) obj;
                if (npc.getNpcTemplate().get_npcId() == npcId) {
                    return true;
                }
            }
        }
        return false;
    }
}
