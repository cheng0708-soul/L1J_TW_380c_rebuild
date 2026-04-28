package l1j.server.server.priest;

import java.lang.reflect.Constructor;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.serverpackets.S_ItemName;

/**
 * 重新要求客戶端更新指定道具的顯示名稱 / icon / itemId。
 *
 * 部分客户端只收 S_ItemName 不會刷新 itemId/icon，
 * 所以這裡額外嘗試送 S_ItemStatus、S_DeleteInventoryItem + S_AddItem。
 * 用反射避免不同分支封包類名差異造成編譯錯誤。
 */
public final class InventoryRefresh {

    private InventoryRefresh() {}

    public static void push(L1PcInstance pc, L1ItemInstance item) {
        if (pc == null || item == null) return;

        // 1) 一定先刷新名字（包含 [編號][智+X] 前綴）
        try {
            pc.sendPackets(new S_ItemName(item));
        } catch (Throwable ignore) {}

        // 2) 嘗試刷新狀態/圖示
        trySend(pc, "l1j.server.server.serverpackets.S_ItemStatus", item);

        // 3) 部分客户端必須刪除再新增才能看到新的 itemId/icon
        boolean deleted = trySend(pc, "l1j.server.server.serverpackets.S_DeleteInventoryItem", item.getId());
        if (!deleted) {
            deleted = trySend(pc, "l1j.server.server.serverpackets.S_DeleteInventoryItem", item);
        }
        if (deleted) {
            if (!trySend(pc, "l1j.server.server.serverpackets.S_AddItem", item)) {
                trySend(pc, "l1j.server.server.serverpackets.S_AddInventoryItem", item);
            }
        }
    }

    private static boolean trySend(L1PcInstance pc, String packetClass, Object arg) {
        try {
            Class<?> cls = Class.forName(packetClass);
            Object pkt = null;

            if (arg != null) {
                try {
                    Constructor<?> c = cls.getConstructor(arg.getClass());
                    pkt = c.newInstance(arg);
                } catch (NoSuchMethodException e1) {
                    if (arg instanceof Integer) {
                        try {
                            Constructor<?> c = cls.getConstructor(int.class);
                            pkt = c.newInstance(((Integer) arg).intValue());
                        } catch (Throwable ignore2) {}
                    }
                    if (pkt == null) {
                        try {
                            Constructor<?> c = cls.getConstructor(Object.class);
                            pkt = c.newInstance(arg);
                        } catch (Throwable ignore3) {}
                    }
                }
            } else {
                try { pkt = cls.getConstructor().newInstance(); } catch (Throwable ignore0) {}
            }

            if (pkt == null) return false;
            sendPacketCompat(pc, pkt);
            return true;
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static void sendPacketCompat(L1PcInstance pc, Object packet) {
        if (pc == null || packet == null) return;
        try {
            pc.getClass()
              .getMethod("sendPackets", Class.forName("l1j.server.server.serverpackets.ServerBasePacket"))
              .invoke(pc, packet);
        } catch (Throwable e) {
            try {
                pc.getClass().getMethod("sendPackets", Object.class).invoke(pc, packet);
            } catch (Throwable ignore) {}
        }
    }
}
