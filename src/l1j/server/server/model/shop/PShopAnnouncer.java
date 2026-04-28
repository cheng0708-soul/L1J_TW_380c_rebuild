package l1j.server.server.model.shop;

import java.util.concurrent.ScheduledFuture;
import l1j.server.server.GeneralThreadPool;
import l1j.server.server.model.Instance.L1PShopNpcInstance;

public class PShopAnnouncer {
    private final L1PShopNpcInstance _npc;
    private final String _say;
    private ScheduledFuture<?> _sf;

    public PShopAnnouncer(L1PShopNpcInstance npc, String say) {
        _npc = npc;
        // 清理字串：去除收尾空白並移除半形／全形問號
        String s = (say == null ? "" : say.trim());
        s = s.replace('?', ' ').replace('？', ' ').trim();
        // 若結尾沒有任何標點，附加全形句號避免客戶端自動加問號
        if (!s.isEmpty()) {
            char last = s.charAt(s.length() - 1);
            if (last != '!' && last != '！' && last != '。' && last != '.' && last != '…') {
                s = s + "。";
            }
        }
        _say = s;
    }

    public void start() {
        stop();
        _sf = GeneralThreadPool.getInstance().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    // 強制使用一般 NPC 說話封包樣式，避免客戶端自動加問號
                    // 在發送前再次清除問號，避免客戶端自動添加
                    String chat = _say;
                    if (chat != null) {
                        chat = chat.replace('?', ' ').replace('？', ' ').trim();
                    }
                    // 若最後沒有標點符號，附加全形句號避免疑問符號
                    if (chat != null && !chat.isEmpty()) {
                        char last = chat.charAt(chat.length() - 1);
                        if (last != '!' && last != '！' && last != '。' && last != '.' && last != '…') {
                            chat = chat + "。";
                        }
                    }
                    l1j.server.server.serverpackets.S_NpcChatPacket pkt =
                        new l1j.server.server.serverpackets.S_NpcChatPacket(_npc, chat, 0);
                    _npc.broadcastPacket(pkt);
                } catch (Throwable ignore) {}
            }
        }, 2000, 12000);
    }

    public void stop() {
        if (_sf != null) {
            try { _sf.cancel(false); } catch (Throwable ignore) {}
            _sf = null;
        }
    }
}
