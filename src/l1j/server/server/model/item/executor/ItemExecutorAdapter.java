package l1j.server.server.model.item.executor;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.function.ItemExecutor;

/**
 * 統一版 ItemExecutorAdapter（唯一來源：model.item.executor）
 * 對齊 L1J_TW_3.80c 的 ItemExecutor 介面（model.item.function 套件）。
 * 子類別請覆寫 protected void execute(int data, L1PcInstance pc, L1ItemInstance item)。
 */
public abstract class ItemExecutorAdapter implements ItemExecutor {

    /** 介面規範：無 data 的執行入口，預設轉呼叫 data 版本。 */
    @Override
    public void execute(final L1PcInstance pc, final L1ItemInstance item) {
        execute(0, pc, item);
    }

    /** 子類別主實作：帶 data 參數的版本。 */
    public abstract void execute(int data, L1PcInstance pc, L1ItemInstance item);
}
