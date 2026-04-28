package l1j.server.server.model;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import l1j.server.server.datatables.CharacterSigninTable;
import l1j.server.server.datatables.CharacterSigninTable.Record;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_SystemMessage;

public class SignInService {
    private static final Logger _log = Logger.getLogger(SignInService.class.getName());

    private static final int REQUIRED_LEVEL = 60;
    private static final int COOLDOWN_HOURS = 20;
    private static final int YUANBAO_ITEM_ID = 240107;
    private static final int[] REWARDS = new int[] { 10, 20, 30, 40, 50, 60, 70 };

    private static String formatHMS(long totalSeconds) {
        if (totalSeconds < 0) totalSeconds = 0;
        long h = totalSeconds / 3600;
        long m = (totalSeconds % 3600) / 60;
        long s = totalSeconds % 60;
        return new StringBuilder().append(h).append("小時 ").append(m).append("分 ").append(s).append("秒").toString();
    }

    public static void attemptSignIn(L1PcInstance pc) {
        try {
            if (pc == null || pc.isGhost() || pc.isDead()) {
                return;
            }

            int effectiveLevel = Math.max(pc.getLevel(), pc.getHighLevel());
            // 等級判定：本尊等級或過去最高等級，只要有一個達到門檻即可簽到（支援轉生後 1 級的角色）
            if (effectiveLevel < REQUIRED_LEVEL) {
                pc.sendPackets(new S_SystemMessage("\fR等級不足，需達到 " + REQUIRED_LEVEL + " 級才能簽到。"));
                return;
            }


            final int charId = pc.getId();
            final String account = pc.getAccountName();
            final int currentLevel = effectiveLevel; // 紀錄簽到時有效等級（含轉生前）

            final CharacterSigninTable dao = CharacterSigninTable.getInstance();
            final Record r = dao.loadOrCreate(charId, account);

            final Instant now = Instant.now();
            if (r.cooldownUntil != null && r.cooldownUntil.toInstant().isAfter(now)) {
                long remainSec = Duration.between(now, r.cooldownUntil.toInstant()).getSeconds();
                pc.sendPackets(new S_SystemMessage("\fY簽到冷卻中，剩餘 " + formatHMS(remainSec) + "。"));
                return;
            }

            int index = r.signCount;
            if (index >= 7) index = 0;
            int reward = REWARDS[index];

            pc.getInventory().storeItem(YUANBAO_ITEM_ID, reward);
            pc.sendPackets(new S_SystemMessage("\fY簽到成功！第 " + (index + 1) + " 天，獲得元寶 " + reward + "。"));

            int newCount = index + 1;
            if (newCount >= 7) {
                newCount = 0;
                pc.sendPackets(new S_SystemMessage("\fY恭喜完成第7天簽到，週期已重置；下次從第1天開始。"));
            }

            Timestamp nextCooldown = Timestamp.from(now.plus(COOLDOWN_HOURS, ChronoUnit.HOURS));
            dao.update(charId, newCount, Integer.valueOf(currentLevel), nextCooldown, Timestamp.from(now));

            long remainSec = Duration.between(now, nextCooldown.toInstant()).getSeconds();
            pc.sendPackets(new S_SystemMessage("\fY下次可簽到時間冷卻：" + formatHMS(remainSec) + " 後。"));
        } catch (Exception e) {
            try { pc.sendPackets(new S_SystemMessage("\fR簽到發生錯誤，請聯繫管理員。")); } catch (Throwable ignore) {}
            _log.log(Level.SEVERE, "attemptSignIn error", e);
        }
    }
}