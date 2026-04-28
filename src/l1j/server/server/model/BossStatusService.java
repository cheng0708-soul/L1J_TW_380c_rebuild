package l1j.server.server.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;

import l1j.server.L1DatabaseFactory;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_NPCTalkReturn;
import l1j.server.server.utils.SQLUtil;

/**
 * BOSS 重生查詢（240142）
 * <p>
 * - 以 boss_status.html 的 #0~#82 做一頁（83 筆）
 * - 支援 bypass: boss_page_prev / boss_page_next 進行翻頁
 */
public final class BossStatusService {

    /** 對應 boss_status.html 裡的 <var src="#0"> ~ <var src="#82"> */
    private static final int PAGE_SIZE = 83;

    private BossStatusService() {
    }

    public static void open(L1PcInstance pc, int page) {
        if (pc == null) {
            return;
        }

        Connection con = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;

        final String[] args = new String[PAGE_SIZE];
        for (int i = 0; i < args.length; i++) {
            args[i] = "";
        }

        try {
            con = L1DatabaseFactory.getInstance().getConnection();

            // ---- 計算總筆數 & 夾頁碼 ----
            int total = 0;
            PreparedStatement pCount = null;
            ResultSet rsCount = null;
            try {
                // 依照 DB：count 代表同名 BOSS 同時生成的數量，不應拿來當過濾條件。
                // 例如「獨角獸」在提供的 spawnlist_boss.sql 中 count=3。
                pCount = con.prepareStatement("SELECT COUNT(*) FROM spawnlist_boss WHERE npc_id > 0");
                rsCount = pCount.executeQuery();
                if (rsCount.next()) {
                    total = rsCount.getInt(1);
                }
            } finally {
                SQLUtil.close(rsCount);
                SQLUtil.close(pCount);
            }

            int pageCount = (total + PAGE_SIZE - 1) / PAGE_SIZE;
            if (pageCount <= 0) {
                pageCount = 1;
            }
            if (page < 0) {
                page = 0;
            }
            if (page >= pageCount) {
                page = pageCount - 1;
            }
            BossStatusRegistry.setPage(pc, page);

            // ---- 查當頁 ----
            final int offset = page * PAGE_SIZE;
            pstm = con.prepareStatement(
                    "SELECT location, next_spawn_time FROM spawnlist_boss "
                            + "WHERE npc_id > 0 ORDER BY id LIMIT ? OFFSET ?");
            pstm.setInt(1, PAGE_SIZE);
            pstm.setInt(2, offset);
            rs = pstm.executeQuery();

            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            final java.util.Date now = new java.util.Date();

            int idx = 0;
            while (rs.next() && idx < args.length) {
                final String name = rs.getString(1); // location
                final String nextTimeStr = rs.getString(2); // next_spawn_time

                String status;
                try {
                    final java.util.Date next = sdf.parse(nextTimeStr);
                    if (now.getTime() >= next.getTime()) {
                        status = "已重生";
                    } else {
                        long diff = (next.getTime() - now.getTime()) / 1000L;
                        long day = diff / 86400;
                        diff %= 86400;
                        long hour = diff / 3600;
                        diff %= 3600;
                        long min = diff / 60;
                        long sec = diff % 60;
                        status = "剩餘" + day + "天" + hour + "時" + min + "分" + sec + "秒";
                    }
                } catch (Exception e) {
                    status = "時間未知";
                }

                args[idx] = name + "-----[" + status + "]";
                idx++;
            }

        } catch (Exception e) {
            args[0] = "BOSS 重生資料讀取失敗";
        } finally {
            SQLUtil.close(rs);
            SQLUtil.close(pstm);
            SQLUtil.close(con);
        }

        pc.sendPackets(new S_NPCTalkReturn(pc.getId(), "boss_status", args));
    }

    public static void pagePrev(L1PcInstance pc) {
        open(pc, BossStatusRegistry.getPage(pc) - 1);
    }

    public static void pageNext(L1PcInstance pc) {
        open(pc, BossStatusRegistry.getPage(pc) + 1);
    }
}
