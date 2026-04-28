package l1j.server.server.datatables;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.utils.Random;
import l1j.server.server.utils.SQLUtil;

/**
 * 每日任務：依玩家等級派發 4 種怪物，擊殺後回報換元寶。
 *
 * 需求整理：
 * - 單一對話檔，透過對話選項觸發：
 *   - 接取每日任務：顯示 4 種怪物與目標擊殺數量 (新接取時為 0/x)。
 *   - 完成回報任務：確認是否完成，發放元寶 60 個。
 * - 接取條件：
 *   - 角色等級 >= 40。
 *   - 一天只能接一次（以伺服器日期為準）。
 * - 任務內容：
 *   - 4 種怪物，依角色等級從 npc 表挑選可擊殺怪物。
 *   - 每種怪物擊殺數量隨機 10 ~ 80。
 *   - 不能是 BOSS（spawnlist_boss 的 npc_id 視為 BOSS）。
 * - 擊殺進度：
 *   - 每擊殺一次對應怪物即更新計數。
 *   - 每次更新都以系統訊息顯示四種怪物的當前進度。
 *   - 全部達成時，標記為已完成。
 * - 提示文字：
 *   - 接取成功時顯示 4 行：例如「食人妖精 0/40」。
 *   - 擊殺途中每次更新顯示 4 行：例如「食人妖精 1/40」。
 *   - 任務完成後，再次點「接取」，
 *     顯示「你已完成每日任務，下一次接取任務 HH時mm分ss秒」。
 *   - 完成回報後，再點「完成回報任務」，顯示「已完成每日任務，請次日繼續努力」。
 *   - 未完成任務就點「完成回報任務」，顯示「你還未完成任務。」。
 */
public class DailyQuestTable {

    private static final Logger _log = Logger.getLogger(DailyQuestTable.class.getName());
    private static final DailyQuestTable _instance = new DailyQuestTable();

    public static DailyQuestTable getInstance() {
        return _instance;
    }

    private static final int REQUIRED_LEVEL = 40;
    private static final int MIN_KILL = 10;
    private static final int MAX_KILL = 80;
    private static final int TASKS_PER_DAY = 4;

    private static final int REWARD_ITEM_ID = 240140; // 元寶福袋
    private static final int REWARD_COUNT = 1;

    private DailyQuestTable() {
    }

    /**
     * 玩家點選「接取每日任務」時由 C_NPCTalkAction 呼叫。
     */
    public void attemptAccept(L1PcInstance pc) {
        if (pc == null) {
            return;
        }
        if (pc.getLevel() < REQUIRED_LEVEL) {
            pc.sendPackets(new S_SystemMessage("你的等級尚未達到 40 級，無法接取每日任務。"));
            return;
        }

        int charId = pc.getId();
        java.sql.Date today = currentSqlDate();
        QuestState state = loadQuestState(charId, today);

        if (state != null) {
            // 今天已經有紀錄
        	if (state.completed && state.rewarded) {
        	    // 目前時間
        	    Calendar now = Calendar.getInstance();
        	    // 明天 00:00:00（你原本 calcNextAvailable(today) 的邏輯）
        	    Calendar next = calcNextAvailable(today);

        	    long diffMillis = next.getTimeInMillis() - now.getTimeInMillis();
        	    if (diffMillis < 0) {
        	        diffMillis = 0; // 理論上不會 <0，但保險一下
        	    }

        	    long diffSec = diffMillis / 1000;
        	    long hours   = diffSec / 3600;
        	    long minutes = (diffSec % 3600) / 60;
        	    long seconds = (diffSec % 60);

        	    String msg = String.format(
        	            "你已完成每日任務，下一次接取任務還需 %02d時%02d分%02d秒",
        	            hours, minutes, seconds);

        	    pc.sendPackets(new S_SystemMessage(msg));
        	    return;
        	}
 else {
                // 已接但未完成，提醒玩家先完成
                pc.sendPackets(new S_SystemMessage("你今天已接取每日任務，請先完成後再回來。"));
                // 同時顯示目前進度
                List<Task> tasks = loadTasks(charId, today);
                if (!tasks.isEmpty()) {
                    sendProgress(pc, tasks);
                }
                return;
            }
        }

        // 尚未接取今日任務 → 建立新任務
        List<Task> tasks = createRandomTasksForLevel(pc.getLevel());
        if (tasks.isEmpty()) {
            pc.sendPackets(new S_SystemMessage("目前無法為你產生每日任務，請稍後再試。"));
            return;
        }

        saveNewQuest(charId, today, tasks);

        // 顯示 4 行 0/x 的目標
        sendProgress(pc, tasks);
    }

    /**
     * 玩家點選「完成回報任務」時呼叫。
     */
    public void attemptComplete(L1PcInstance pc) {
        if (pc == null) {
            return;
        }
        int charId = pc.getId();
        java.sql.Date today = currentSqlDate();

        // 讀主表狀態
        QuestState state = loadQuestState(charId, today);
        if (state == null) {
            pc.sendPackets(new S_SystemMessage("你還未接取每日任務。"));
            return;
        }

        // 讀今天的任務明細
        List<Task> tasks = loadTasks(charId, today);
        if (tasks.isEmpty()) {
            pc.sendPackets(new S_SystemMessage("你今天沒有每日任務可以回報。"));
            return;
        }

        // 以實際 kill_count / need_count 判斷是否已完成
        boolean allDone = true;
        for (Task t : tasks) {
            if (t.killCount < t.needCount) {
                allDone = false;
                break;
            }
        }

        if (!allDone) {
            pc.sendPackets(new S_SystemMessage("你還未完成任務。"));
            // 順便顯示目前進度
            sendProgress(pc, tasks);
            return;
        }

        // 如果實際已完成，但主表沒標 completed，就補標一次
        if (!state.completed) {
            markCompleted(charId, today);
            state.completed = true; // 更新記憶中的狀態
        }

        // 防止重複領獎
        if (state.rewarded) {
            pc.sendPackets(new S_SystemMessage("已完成每日任務，請次日繼續努力"));
            return;
        }

        // 發獎
        try {
            pc.getInventory().storeItem(REWARD_ITEM_ID, REWARD_COUNT);
            markRewarded(charId, today);
            pc.sendPackets(new S_SystemMessage("你獲得了 元寶福袋。"));
        } catch (Exception e) {
            _log.log(Level.SEVERE, "give reward failed for char " + charId, e);
            pc.sendPackets(new S_SystemMessage("發放每日任務獎勵時發生錯誤，請稍後再試。"));
        }
    }


    /**
     * 由 L1MonsterInstance 在怪物死亡時呼叫。
     */
    public void onMonsterKilled(L1PcInstance pc, int mobNpcId) {
        if (pc == null) {
            return;
        }
        int charId = pc.getId();
        java.sql.Date today = currentSqlDate();

        QuestState state = loadQuestState(charId, today);
        if (state == null || state.completed) {
            return; // 今日沒有任務或已完成
        }

        List<Task> tasks = loadTasks(charId, today);
        if (tasks.isEmpty()) {
            return;
        }

        boolean updated = false;
        boolean changed = false;
        for (Task t : tasks) {
            if (t.mobId == mobNpcId && t.killCount < t.needCount) {
                t.killCount++;
                changed = true;
            }
            if (t.killCount > t.needCount) {
                t.killCount = t.needCount;
            }
        }

        if (changed) {
            // 更新 DB
            updateTasks(charId, today, tasks);
            updated = true;
        }

        if (updated) {
            // 檢查是否全部完成
            boolean allDone = true;
            for (Task t : tasks) {
                if (t.killCount < t.needCount) {
                    allDone = false;
                    break;
                }
            }
            if (allDone && !state.completed) {
                markCompleted(charId, today);
                pc.sendPackets(new S_SystemMessage("你已完成今日每日任務，請回任務 NPC 回報。"));
            }

            // 每次擊殺都顯示四行進度
            sendProgress(pc, tasks);
        }
    }

    // ------------------------------------------------------------------------
    // 內部結構與 DB 存取
    // ------------------------------------------------------------------------

    private static class QuestState {
        boolean completed;
        boolean rewarded;
    }

    private static class Task {
        int index;
        int mobId;
        String mobName;
        String mapName;
        int needCount;
        int killCount;
    }

    private java.sql.Date currentSqlDate() {
        long now = System.currentTimeMillis();
        return new java.sql.Date(now);
    }

    private Calendar calcNextAvailable(Date today) {
        Calendar c = Calendar.getInstance();
        // 設定為明天的 00:00:00
        c.setTimeInMillis(today.getTime());
        c.add(Calendar.DATE, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private QuestState loadQuestState(int charId, java.sql.Date date) {
        QuestState result = null;
        Connection con = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement(
                    "SELECT completed, rewarded FROM character_dailyquest WHERE char_id=? AND quest_date=?");
            pstm.setInt(1, charId);
            pstm.setDate(2, date);
            rs = pstm.executeQuery();
            if (rs.next()) {
                result = new QuestState();
                result.completed = rs.getBoolean(1);
                result.rewarded = rs.getBoolean(2);
            }
        } catch (Exception e) {
            _log.log(Level.SEVERE, "loadQuestState failed: " + charId, e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
        return result;
    }

    private List<Task> loadTasks(int charId, java.sql.Date date) {
        List<Task> list = new ArrayList<Task>();
        Connection con = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement(
                    "SELECT mob_index, mob_id, mob_name, need_count, kill_count FROM character_dailyquest_mob WHERE char_id=? AND quest_date=? ORDER BY mob_index");
            pstm.setInt(1, charId);
            pstm.setDate(2, date);
            rs = pstm.executeQuery();
            while (rs.next()) {
                Task t = new Task();
                t.index = rs.getInt(1);
                t.mobId = rs.getInt(2);
                t.mobName = rs.getString(3);
                t.needCount = rs.getInt(4);
                t.killCount = rs.getInt(5);
                list.add(t);
            }
        } catch (Exception e) {
            _log.log(Level.SEVERE, "loadTasks failed: " + charId, e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
        // 依 mobId 補上地圖名稱（使用 spawnlist + mapids）
        fillMapNames(list);
        return list;
    }

    /**
     * 查詢每個任務怪物的代表地圖名稱，用於顯示在進度文字後面。
     */
    private void fillMapNames(List<Task> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Connection con = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement(
                    "SELECT MIN(m.locationname) AS locname FROM spawnlist s " +
                    "JOIN mapids m ON m.mapid = s.mapid " +
                    "WHERE s.npc_templateid=?");
            for (Task t : list) {
                try {
                    pstm.setInt(1, t.mobId);
                    rs = pstm.executeQuery();
                    if (rs.next()) {
                        t.mapName = rs.getString(1);
                    }
                } catch (Exception e) {
                    _log.log(Level.WARNING, "fillMapNames single mob failed: " + t.mobId, e);
                } finally {
                    SQLUtil.close(rs);
                    rs = null;
                }
            }
        } catch (Exception e) {
            _log.log(Level.WARNING, "fillMapNames failed", e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
    }

    private void saveNewQuest(int charId, java.sql.Date date, List<Task> tasks) {
        Connection con = null;
        PreparedStatement pstm = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            con.setAutoCommit(false);

            // 建立主表記錄
            pstm = con.prepareStatement(
                    "INSERT INTO character_dailyquest (char_id, quest_date, completed, rewarded, next_available) VALUES (?,?,?,?,?)");
            pstm.setInt(1, charId);
            pstm.setDate(2, date);
            pstm.setBoolean(3, false);
            pstm.setBoolean(4, false);
            Calendar next = calcNextAvailable(date);
            pstm.setTimestamp(5, new Timestamp(next.getTimeInMillis()));
            pstm.execute();
            SQLUtil.close(pstm);
            pstm = null;

            // 建立 4 筆任務明細
            pstm = con.prepareStatement(
                    "INSERT INTO character_dailyquest_mob (char_id, quest_date, mob_index, mob_id, mob_name, need_count, kill_count) VALUES (?,?,?,?,?,?,?)");
            for (Task t : tasks) {
                pstm.setInt(1, charId);
                pstm.setDate(2, date);
                pstm.setInt(3, t.index);
                pstm.setInt(4, t.mobId);
                pstm.setString(5, t.mobName);
                pstm.setInt(6, t.needCount);
                pstm.setInt(7, t.killCount);
                pstm.addBatch();
            }
            pstm.executeBatch();

            con.commit();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "saveNewQuest failed: " + charId, e);
            try {
                if (con != null) {
                    con.rollback();
                }
            } catch (Exception ex) {
                // ignore
            }
        } finally {
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
    }

    private void updateTasks(int charId, java.sql.Date date, List<Task> tasks) {
        Connection con = null;
        PreparedStatement pstm = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement(
                    "UPDATE character_dailyquest_mob SET kill_count=? WHERE char_id=? AND quest_date=? AND mob_index=?");
            for (Task t : tasks) {
                pstm.setInt(1, t.killCount);
                pstm.setInt(2, charId);
                pstm.setDate(3, date);
                pstm.setInt(4, t.index);
                pstm.addBatch();
            }
            pstm.executeBatch();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "updateTasks failed: " + charId, e);
        } finally {
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
    }

    private void markCompleted(int charId, java.sql.Date date) {
        Connection con = null;
        PreparedStatement pstm = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement(
                    "UPDATE character_dailyquest SET completed=1 WHERE char_id=? AND quest_date=?");
            pstm.setInt(1, charId);
            pstm.setDate(2, date);
            pstm.execute();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "markCompleted failed: " + charId, e);
        } finally {
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
    }

    private void markRewarded(int charId, java.sql.Date date) {
        Connection con = null;
        PreparedStatement pstm = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();
            pstm = con.prepareStatement(
                    "UPDATE character_dailyquest SET rewarded=1 WHERE char_id=? AND quest_date=?");
            pstm.setInt(1, charId);
            pstm.setDate(2, date);
            pstm.execute();
        } catch (Exception e) {
            _log.log(Level.SEVERE, "markRewarded failed: " + charId, e);
        } finally {
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
    }

    /**
     * 依等級亂數挑 4 種怪物（排除 BOSS 與非一般怪）。
     */
    private List<Task> createRandomTasksForLevel(int level) {
        List<Task> result = new ArrayList<Task>();
        Connection con = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        try {
            con = L1DatabaseFactory.getInstance().getConnection();

            int minLvl = Math.max(1, level - 5);
            int maxLvl = level + 3;

            // 從 npc 表挑選怪物：
            // impl = 'L1Monster'
            // lvl 在範圍內
            // npcid 不在 spawnlist_boss 的 npc_id 清單（避免 BOSS）
            // 並且從 spawnlist / mapids 取得一個代表性的地圖名稱
            pstm = con.prepareStatement(
                    "SELECT n.npcid, n.name, MIN(m.locationname) AS locname FROM npc n " +
                    "JOIN spawnlist s ON s.npc_templateid = n.npcid " +
                    "JOIN mapids m ON m.mapid = s.mapid " +
                    "WHERE n.impl='L1Monster' AND n.lvl BETWEEN ? AND ? " +
                    "AND n.npcid NOT IN (SELECT DISTINCT npc_id FROM spawnlist_boss) " +
                    "AND s.mapid IN (0, 1, 2, 4, 7, 8, 9, 10, 11, 12, 13, 19, 20, 21, 23, 24, 25, 26, 27, 28, 30, 31, 32, 33, 35, 36, 43, 44, 45, 46, 47, 48, 49, 50, 59, 60, 61, 72, 73, 74, 78, 79, 80, 81, 82, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 240, 241, 242, 243, 244, 248, 249, 250, 251, 252, 253, 254, 257, 258, 259, 303, 304, 307, 308, 309, 310, 320, 410, 420, 430, 440, 441, 442, 443, 444, 450, 451, 452, 453, 454, 455, 456, 457, 460, 461, 462, 463, 464, 465, 466, 467, 468, 470, 471, 472, 473, 474, 475, 476, 477, 478, 480, 521, 522, 523, 524, 604, 605, 606, 607, 780, 781, 783) " +
                    "AND n.name NOT LIKE '%試煉%' AND n.note NOT LIKE '%試煉%' " +
                    "GROUP BY n.npcid, n.name");
            pstm.setInt(1, minLvl);
            pstm.setInt(2, maxLvl);
            rs = pstm.executeQuery();
            List<Integer> ids = new ArrayList<Integer>();
            List<String> names = new ArrayList<String>();
            List<String> mapNames = new ArrayList<String>();
            while (rs.next()) {
                ids.add(rs.getInt(1));
                names.add(rs.getString(2));
                mapNames.add(rs.getString(3));
            }
            SQLUtil.close(rs);
            SQLUtil.close(pstm);
            rs = null;
            pstm = null;

            if (ids.isEmpty()) {
                return result;
            }

            int attempts = 0;
            while (result.size() < TASKS_PER_DAY && attempts < 100) {
                attempts++;
                int idx = Random.nextInt(ids.size());
                int npcId = ids.get(idx);
                String name = names.get(idx);
                String mapName = null;
                if (mapNames != null && idx < mapNames.size()) {
                    mapName = mapNames.get(idx);
                }

                boolean exists = false;
                for (Task t : result) {
                    if (t.mobId == npcId) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    continue;
                }

                Task t = new Task();
                t.index = result.size() + 1;
                t.mobId = npcId;
                t.mobName = name;
                t.mapName = mapName;
                int need = MIN_KILL + Random.nextInt(MAX_KILL - MIN_KILL + 1);
                t.needCount = need;
                t.killCount = 0;
                result.add(t);
            }

        } catch (Exception e) {
            _log.log(Level.SEVERE, "createRandomTasksForLevel failed", e);
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }
        return result;
    }

    private void sendProgress(L1PcInstance pc, List<Task> tasks) {
        for (Task t : tasks) {
            String line = t.mobName + "=(" + t.killCount + "/" + t.needCount + ")";
            if (t.mapName != null && t.mapName.length() > 0) {
                line = line + "---" + t.mapName;
            }
            pc.sendPackets(new S_SystemMessage(line));
        }
    }
}
