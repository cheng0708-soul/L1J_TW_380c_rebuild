package l1j.server.server.utils;

import l1j.server.server.ClientThread;
import l1j.server.server.ActionCodes;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_DoActionShop;
import l1j.server.server.serverpackets.S_ServerMessage;

public final class PShopBypass {

    private PShopBypass() {}

    public static void tryHandle(ClientThread client, String arg) {
        if (client == null) return;
        L1PcInstance pc = client.getActiveChar();
        if (pc == null) return;

        int currency = "yuan".equalsIgnoreCase(arg) ? 240107 : 40308;

        try { pc.setPrivateShopCurrencyItemId(currency); } catch (Throwable ignore) {}
        try { pc.setShopModeChosen(true); } catch (Throwable ignore) {}

        boolean auto = false;
        try { auto = pc.isShopModePendingOpen(); } catch (Throwable ignore) {}
        try { pc.setShopModePendingOpen(false); } catch (Throwable ignore) {}

        if (!auto) {
            try { pc.sendPackets(new S_ServerMessage(166, "已設定為 " + (currency == 240107 ? "[元寶]" : "[金幣]") + " 模式。")); } catch (Throwable ignore) {}
            return;
        }

        byte[] chat = null;
        try { chat = pc.getShopChat(); } catch (Throwable ignore) {}

        byte[] prefix;
        try { prefix = (currency == 240107) ? "[元寶] ".getBytes("utf8") : "[金幣] ".getBytes("utf8"); }
        catch (Exception e) { prefix = new byte[]{}; }

        byte[] sendChat = (chat != null) ? new byte[prefix.length + chat.length] : prefix;
        if (chat != null) {
            System.arraycopy(prefix, 0, sendChat, 0, prefix.length);
            System.arraycopy(chat, 0, sendChat, prefix.length, chat.length);
        }

        try { pc.setPrivateShop(true); } catch (Throwable ignore) {}
        try {
            pc.sendPackets(new S_DoActionShop(pc.getId(), ActionCodes.ACTION_Shop, sendChat));
            pc.broadcastPacket(new S_DoActionShop(pc.getId(), ActionCodes.ACTION_Shop, sendChat));
        } catch (Throwable ignore) {}
    }
}
