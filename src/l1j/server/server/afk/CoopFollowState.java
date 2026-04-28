package l1j.server.server.afk;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * Per-player state for 跨系統.
 * Lightweight global registry keyed by pcId, so we do not need to modify L1PcInstance.
 */
public final class CoopFollowState {

    public enum HealSkill {
        LESSER_HEAL("初級治癒術", 1),
        EXTRA_HEAL("中級治癒術", 19),
        GREATER_HEAL("高級治癒術", 35),
        FULL_HEAL("全部治癒術", 57);

        public final String displayName;
        public final int skillId;

        HealSkill(String displayName, int skillId) {
            this.displayName = displayName;
            this.skillId = skillId;
        }

        public HealSkill next() {
            HealSkill[] vals = values();
            return vals[(this.ordinal() + 1) % vals.length];
        }
    }

    private static final Map<Integer, CoopFollowState> STATES = new ConcurrentHashMap<Integer, CoopFollowState>();

    public static CoopFollowState of(final L1PcInstance pc) {
        if (pc == null) return null;
        final int id = pc.getId();
        CoopFollowState s = STATES.get(id);
        if (s == null) {
            s = new CoopFollowState();
            STATES.put(id, s);
        }
        return s;
    }

    public static void clear(final L1PcInstance pc) {
        if (pc == null) return;
        STATES.remove(pc.getId());
    }

    private boolean followSystemOn = false;
    private boolean coopBattleOn = false;

    private int followTargetId = 0;
    // 次 100% 主
    private int coopTargetId = 0;

    private HealSkill healSkill = HealSkill.LESSER_HEAL;
    private int healHpPercent = 10;

    // 正輩泻
    private boolean buffRegistering = false;

    private final LinkedHashSet<Integer> buffSkills = new LinkedHashSet<Integer>();

    // === runtime throttles (ms) ===
    // Used to respect sprite-based move / attack speeds while CoopFollowService
    // runs on a fixed scheduler tick.
    private volatile long lastCoopMoveMs = 0L;
    private volatile long lastCoopAttackMs = 0L;

    public boolean isFollowSystemOn() { return followSystemOn; }
    public void toggleFollowSystem() { this.followSystemOn = !this.followSystemOn; }

    public boolean isCoopBattleOn() { return coopBattleOn; }
    public void toggleCoopBattle() { this.coopBattleOn = !this.coopBattleOn; }

    public int getFollowTargetId() { return followTargetId; }
    public void setFollowTargetId(int followTargetId) { this.followTargetId = followTargetId; }

    public int getCoopTargetId() { return coopTargetId; }
    public void setCoopTargetId(int coopTargetId) { this.coopTargetId = coopTargetId; }

    public HealSkill getHealSkill() { return healSkill; }
    public void cycleHealSkill() { this.healSkill = this.healSkill.next(); }

    public int getHealHpPercent() { return healHpPercent; }
    public void addHealHpPercent(int delta) {
        this.healHpPercent += delta;
        if (this.healHpPercent < 10) this.healHpPercent = 10;
        if (this.healHpPercent > 90) this.healHpPercent = 90;
    }

    public boolean isBuffRegistering() { return buffRegistering; }
    public void setBuffRegistering(boolean v) { this.buffRegistering = v; }

    public Set<Integer> getBuffSkills() { return buffSkills; }

    public long getLastCoopMoveMs() { return lastCoopMoveMs; }
    public void setLastCoopMoveMs(long v) { this.lastCoopMoveMs = v; }

    public long getLastCoopAttackMs() { return lastCoopAttackMs; }
    public void setLastCoopAttackMs(long v) { this.lastCoopAttackMs = v; }

    public void toggleBuffSkill(int skillId) {
        if (buffSkills.contains(skillId)) buffSkills.remove(skillId);
        else buffSkills.add(skillId);
    }

    public String buildBuffNamesString() {
        if (buffSkills.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Integer id : buffSkills) {
            if (!first) sb.append("");
            first = false;
            sb.append(id);
        }
        return sb.toString();
    }
}
