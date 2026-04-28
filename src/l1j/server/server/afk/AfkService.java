package l1j.server.server.afk;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;
import l1j.server.server.datatables.SkillsTable;
import l1j.server.server.templates.L1Skills;

/**
 * AfkService (clean rebuild)
 * - 保留原有 API 名稱，避免改動其他檔案：
 *   toggleRunning / toggleRunningMsg / isRunning / stop / onDisconnect
 *   setPatrol / togglePatrol / setTeleportMode
 *   beginSkillRegister / clearSkills / listSkills / registerSkillId
 *   captureSkillFromCast (供 C_UseSkill 反射呼叫)
 * - 僅以 try-catch 方式呼叫 AfkCruiseEngine / AfkAiController / AfkMagicRegistry，
 *   即使那些類在你的分支有差異也不會編譯失敗。
 */
public final class AfkService {

    private static final Logger _log = Logger.getLogger(AfkService.class.getName());
    private AfkService() {}

    public static final int UNLIMITED = -1;

    private static final Map<Integer, State> _states = new ConcurrentHashMap<Integer, State>();

    private static final class State {
        boolean running = false;
        boolean tpMode = false;
        int patrolRadius = UNLIMITED;
        boolean registeringSkill = false;
        Set<Integer> skillIds = new LinkedHashSet<Integer>(); // 容量只用 1，保留 Set 結構兼容舊 UI
    }

    private static State st(final L1PcInstance pc) {
        State s = _states.get(pc.getId());
        if (s == null) { s = new State(); _states.put(pc.getId(), s); }
        return s;
    }

    /* =========================== 基本查詢 =========================== */

    public static boolean isRunning(final L1PcInstance pc) {
        if (pc == null) return false;
        try { return st(pc).running; } catch (Throwable ignore) { return false; }
    }

    public static void stop(final L1PcInstance pc) {
        if (pc == null) return;
        try { Class.forName("l1j.server.server.afk.AfkCruiseEngine").getMethod("stop", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
        try { Class.forName("l1j.server.server.afk.AfkMagicRegistry").getMethod("setEnabled", L1PcInstance.class, boolean.class).invoke(null, pc, false); } catch (Throwable ignore) {}
        try { Class.forName("l1j.server.server.afk.AfkAiController").getMethod("clearChaseTarget", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
        try { Class.forName("l1j.server.server.afk.AfkCenterEngine").getMethod("resetRef", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
        try { st(pc).running = false; } catch (Throwable ignore) {}
    }

    /** 建議在任何登出/斷線時呼叫；也會清掉暫存狀態 */
    public static void onDisconnect(final L1PcInstance pc) {
        stop(pc);
        try { _states.remove(pc.getId()); } catch (Throwable ignore) {}
    }

    /* =========================== 開關（與訊息） =========================== */

    /** 舊 UI 期望的名稱：toggleRunning */
    public static String toggleRunning(final L1PcInstance pc) {
        return toggleRunningMsg(pc);
    }

    /** 切換掛機 + 同步引擎狀態 + 傳送系統訊息 */
    public static String toggleRunningMsg(final L1PcInstance pc) {
        if (pc == null) return "掛機：玩家為空";
        final State s = st(pc);
        s.running = !s.running;
        if (s.running) {
            try {
                // 套用巡航參數
                Class<?> ce = Class.forName("l1j.server.server.afk.AfkCruiseEngine");
                try { ce.getMethod("setTeleportMode", L1PcInstance.class, boolean.class).invoke(null, pc, s.tpMode); } catch (Throwable ignore) {}
                try { ce.getMethod("setPatrolRadius", L1PcInstance.class, int.class).invoke(null, pc, s.patrolRadius); } catch (Throwable ignore) {}
                try { ce.getMethod("setPatrolOriginIfAbsent", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
                try { ce.getMethod("start", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
                try { Class.forName("l1j.server.server.afk.AfkAiController").getMethod("nudgeWake", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
            } catch (Throwable ignore) {}
            try { Class.forName("l1j.server.server.afk.AfkMagicRegistry").getMethod("setEnabled", L1PcInstance.class, boolean.class).invoke(null, pc, true); } catch (Throwable ignore) {}
            try { pc.sendPackets(new S_SystemMessage("掛機：已啟動")); } catch (Throwable ignore) {}
        } else {
            stop(pc);
            try { pc.sendPackets(new S_SystemMessage("掛機：已停止")); } catch (Throwable ignore) {}
        }
        return s.running ? "掛機：已啟動" : "掛機：已停止";
    }

    /* =========================== 巡航 =========================== */

    
    public static String setPatrol(final L1PcInstance pc, final int radius) {
        if (pc == null) return "巡邏模式：玩家為空";
        final State s = st(pc);
        int newRadius = radius;
        // 若點擊同一數值，視為「取消回預設（無限制）」
        if (s.patrolRadius == radius) {
            newRadius = UNLIMITED;
        }
        s.patrolRadius = newRadius;
        try {
            Class<?> ce = Class.forName("l1j.server.server.afk.AfkCruiseEngine");
            try { ce.getMethod("setPatrolRadius", L1PcInstance.class, int.class).invoke(null, pc, newRadius); } catch (Throwable ignore) {}
            // 若改為無限制，清掉原點（避免後續仍有殘留瞬回）
            if (newRadius == UNLIMITED) {
                try { ce.getMethod("clearOrigin", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
            } else {
                // 有限半徑：如掛機正在跑，補記原點+啟動
                boolean running = s.running;
                if (running) {
                    try { ce.getMethod("setPatrolOriginIfAbsent", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
                    try { ce.getMethod("start", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
                    try { Class.forName("l1j.server.server.afk.AfkAiController").getMethod("nudgeWake", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
                }
            }
        } catch (Throwable ignore) {}
        String msg = (newRadius == UNLIMITED) ? "巡邏模式：已取消，回預設（無限制）" : ("巡邏模式：已登錄 (" + newRadius + "格)");
        try { pc.sendPackets(new S_SystemMessage(msg)); } catch (Throwable ignore) {}
        return msg;
    }


    /** 舊 UI 會呼叫的簽名（帶一個無用字串參數）；直接委派到 setPatrol */
    public static String setPatrol(final L1PcInstance pc, final int radius, final String _ignored) { return setPatrol(pc, radius); }
    /** 部分 UI 使用 togglePatrol(...)；行為同 setPatrol(...) */
    public static String togglePatrol(final L1PcInstance pc, final int radius, final String _ignored) { return setPatrol(pc, radius); }

    /** 切換瞬移模式：再按一次即可取消（對齊 10 格模式的行為） */
    public static void toggleTeleportMode(final L1PcInstance pc) {
        if (pc == null) return;
        final State s = st(pc);
        final boolean newOn = !s.tpMode;
        setTeleportMode(pc, newOn);
    }

    /* =========================== 瞬移模式 =========================== */

    public static void setTeleportMode(final L1PcInstance pc, final boolean on) {
        if (pc == null) return;
        final State s = st(pc);
        s.tpMode = on;
        try {
            Class<?> ce = Class.forName("l1j.server.server.afk.AfkCruiseEngine");
            try { ce.getMethod("setTeleportMode", L1PcInstance.class, boolean.class).invoke(null, pc, on); } catch (Throwable ignore) {}
            if (s.running) {
                try { ce.getMethod("start", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
                try { Class.forName("l1j.server.server.afk.AfkAiController").getMethod("nudgeWake", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
            }
        } catch (Throwable ignore) {}
        try { pc.sendPackets(new S_SystemMessage(on ? "瞬移模式：已登錄" : "瞬移模式：已取消")); } catch (Throwable ignore) {}
    }

    
    /* =========================== 其他：舊分支所需的相容方法 =========================== */

    /** 提供給 AfkAiController 舊版相依：回傳目前巡邏半徑（-1 代表無限制） */
    public static int getPatrolRadius(final L1PcInstance pc) {
        if (pc == null) return UNLIMITED;
        return st(pc).patrolRadius;
    }

    /** 提供給 AutoFarmToggle 舊版相依：簡單打招呼並顯示目前狀態 */
    public static void greet(final L1PcInstance pc) {
        if (pc == null) return;
        final State s = st(pc);
        String status = s.running ? "掛機：開" : "掛機：關";
        String patrol = (s.patrolRadius == UNLIMITED) ? "巡邏：無限制" : ("巡邏：" + s.patrolRadius + "格");
        String tp = s.tpMode ? "瞬移：開" : "瞬移：關";
        try { pc.sendPackets(new l1j.server.server.serverpackets.S_SystemMessage("AFK 狀態 → " + status + "，" + patrol + "，" + tp)); } catch (Throwable ignore) {}
    }
/* =========================== 技能登錄（僅 1 個） =========================== */

    public static void beginSkillRegister(final L1PcInstance pc) {
        if (pc == null) return;
        final State s = st(pc);
        s.registeringSkill = true;
        s.skillIds.clear(); // 容量=1，先清空
        try { pc.sendPackets(new S_SystemMessage("技能登錄：請在技能視窗點擊要登錄的『攻擊魔法』。此次點擊不會影響施法。")); } catch (Throwable ignore) {}
        tryOpenSkillWindow(pc);
    }

    public static void clearSkills(final L1PcInstance pc) {
        if (pc == null) return;
        final State s = st(pc);
        s.skillIds.clear();
        try { Class.forName("l1j.server.server.afk.AfkMagicRegistry").getMethod("clear", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
        try { pc.sendPackets(new S_SystemMessage("技能登錄(0)")); } catch (Throwable ignore) {}
    }

    public static void listSkills(final L1PcInstance pc) {
        if (pc == null) return;
        final State s = st(pc);
        StringBuilder sb = new StringBuilder();
        sb.append("技能登錄(").append(s.skillIds.size()).append(") : ");
        boolean first = true;
        for (Integer id : s.skillIds) {
            if (!first) sb.append(", ");
            first = false;
            String name = null;
            try { L1Skills sk = SkillsTable.getInstance().getTemplate(id); if (sk != null) name = sk.getName(); } catch (Throwable ignore) {}
            if (name == null) name = String.valueOf(id);
            sb.append(name);
        }
        try { pc.sendPackets(new S_SystemMessage(sb.toString())); } catch (Throwable ignore) {}
    }

    public static void registerSkillId(final L1PcInstance pc, final int skillId) {
        if (pc == null) return;
        final State s = st(pc);
        s.skillIds.clear();
        s.skillIds.add(Integer.valueOf(skillId));
        try { Class.forName("l1j.server.server.afk.AfkMagicRegistry").getMethod("clear", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
        try { Class.forName("l1j.server.server.afk.AfkMagicRegistry").getMethod("registerSingleSkill", L1PcInstance.class, int.class).invoke(null, pc, skillId); } catch (Throwable ignore) {}
        String name = null; try { L1Skills sk = SkillsTable.getInstance().getTemplate(skillId); if (sk != null) name = sk.getName(); } catch (Throwable ignore) {}
        if (name == null) name = String.valueOf(skillId);
        try { pc.sendPackets(new S_SystemMessage("技能登錄：已登錄 " + name + " (ID=" + skillId + ")")); } catch (Throwable ignore) {}
    }

    /** 供 C_UseSkill 反射呼叫：在登錄模式時接住下一次施法來完成登錄（不中斷當次施法） */
    public static void captureSkillFromCast(final L1PcInstance pc, final int skillId) {
        if (pc == null) return;
        final State s = st(pc);
        if (!s.registeringSkill) return;
        s.registeringSkill = false; // 單次登錄

        // 僅允許攻擊型魔法
        boolean ok = true;
        try {
            ok = l1j.server.server.afk.AttackSkillFilters.isAttackMagic(SkillsTable.getInstance().getTemplate(skillId));
        } catch (Throwable ignore) {}
        if (!ok) {
            try { pc.sendPackets(new S_SystemMessage("技能登錄：非攻擊魔法，取消登錄。")); } catch (Throwable ignore) {}
            return;
        }

        s.skillIds.clear();
        s.skillIds.add(Integer.valueOf(skillId));
        try { Class.forName("l1j.server.server.afk.AfkMagicRegistry").getMethod("clear", L1PcInstance.class).invoke(null, pc); } catch (Throwable ignore) {}
        try { Class.forName("l1j.server.server.afk.AfkMagicRegistry").getMethod("registerSingleSkill", L1PcInstance.class, int.class).invoke(null, pc, skillId); } catch (Throwable ignore) {}
        String name = null; try { L1Skills sk = SkillsTable.getInstance().getTemplate(skillId); if (sk != null) name = sk.getName(); } catch (Throwable ignore) {}
        if (name == null) name = String.valueOf(skillId);
        try { pc.sendPackets(new S_SystemMessage("技能登錄：已登錄 " + name + " (ID=" + skillId + ")")); } catch (Throwable ignore) {}
    }

    /* =========================== 內部：嘗試彈出技能視窗 =========================== */
    private static void tryOpenSkillWindow(final L1PcInstance pc) {
        try {
            Class<?> pkt = Class.forName("l1j.server.server.serverpackets.S_SkillList");

            // 1) S_SkillList(L1PcInstance)
            try {
                java.lang.reflect.Constructor<?> c = pkt.getConstructor(l1j.server.server.model.Instance.L1PcInstance.class);
                Object o = c.newInstance(pc);
                pc.sendPackets((l1j.server.server.serverpackets.ServerBasePacket) o);
                return;
            } catch (Throwable ignore) {}

            // 2) S_SkillList(boolean, L1Skills[])
            try {
                java.lang.reflect.Constructor<?> c = pkt.getConstructor(boolean.class, l1j.server.server.templates.L1Skills[].class);
                Object o = c.newInstance(Boolean.TRUE, new l1j.server.server.templates.L1Skills[0]);
                pc.sendPackets((l1j.server.server.serverpackets.ServerBasePacket) o);
                return;
            } catch (Throwable ignore) {}

            // 3) S_SkillList()
            try {
                java.lang.reflect.Constructor<?> c = pkt.getConstructor();
                Object o = c.newInstance();
                pc.sendPackets((l1j.server.server.serverpackets.ServerBasePacket) o);
                return;
            } catch (Throwable ignore) {}

            // 4) S_SkillList(int,int,int[])
            try {
                java.lang.reflect.Constructor<?> c = pkt.getConstructor(int.class, int.class, int[].class);
                Object o = c.newInstance(0, 0, new int[0]);
                pc.sendPackets((l1j.server.server.serverpackets.ServerBasePacket) o);
                return;
            } catch (Throwable ignore) {}

            // 5) static sendList(pc)
            try {
                java.lang.reflect.Method m = pkt.getMethod("sendList", l1j.server.server.model.Instance.L1PcInstance.class);
                Object o = m.invoke(null, pc);
                if (o instanceof l1j.server.server.serverpackets.ServerBasePacket) {
                    pc.sendPackets((l1j.server.server.serverpackets.ServerBasePacket) o);
                }
                return;
            } catch (Throwable ignore) {}

        } catch (Throwable t) {
            // ignore
        }
        try { pc.sendPackets(new S_SystemMessage("技能登錄：若未自動彈出，請手動開啟技能視窗。")); } catch (Throwable ignore) {}
    }
}
