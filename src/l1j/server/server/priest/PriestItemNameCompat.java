package l1j.server.server.priest;

import java.lang.reflect.Method;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;

/** 兼容工具：嘗試以反射修改道具顯示名稱與儲存，避免不同核心 API 差異造成編譯錯誤。 */
public final class PriestItemNameCompat {
    private PriestItemNameCompat() {}

    public static void tryRenameAndSave(L1PcInstance pc, L1ItemInstance item, String newName) {
        if (pc == null || item == null) return;
        try {
            // 嘗試多種命名
            tryInvoke(item, "setViewName", new Class<?>[]{String.class}, new Object[]{newName});
            tryInvoke(item, "setViewNameEx", new Class<?>[]{boolean.class}, new Object[]{true});
            tryInvoke(item, "setItemName", new Class<?>[]{String.class}, new Object[]{newName});
            tryInvoke(item, "setCustomName", new Class<?>[]{String.class}, new Object[]{newName});

            // 儲存：saveItem / updateItem 多種簽名
            if (!tryInvoke(pc.getInventory(), "saveItem",
                    new Class<?>[]{item.getClass(), int.class},
                    new Object[]{item, 0})) {
                if (!tryInvoke(pc.getInventory(), "updateItem",
                        new Class<?>[]{item.getClass()},
                        new Object[]{item})) {
                    for (int col : new int[]{1,2,4,8,16,32}) {
                        if (tryInvoke(pc.getInventory(), "updateItem",
                                new Class<?>[]{item.getClass(), int.class},
                                new Object[]{item, col})) break;
                    }
                }
            }
        } catch (Throwable ignore) {}
    }

    private static boolean tryInvoke(Object target, String name, Class<?>[] sig, Object[] args) {
        try {
            Method m = target.getClass().getMethod(name, sig);
            m.setAccessible(true);
            m.invoke(target, args);
            return true;
        } catch (Throwable t) { return false; }
    }
}