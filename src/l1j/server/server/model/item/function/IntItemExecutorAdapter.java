package l1j.server.server.model.item.function;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.model.item.executor.ItemExecutor; // 使用 executor 套件的介面

/**
 * IntItemExecutorAdapter
 * - 修正：補上介面需要的 execute(L1PcInstance, L1ItemInstance)
 * - 修正：int[] 多載不能標 @Override，僅作相容轉接
 * - 保留 protected abstract execute(int, ...) 讓子類覆寫
 */
public abstract class IntItemExecutorAdapter implements ItemExecutor {

    /** 介面簽章（必要） */
    @Override
    public void execute(final L1PcInstance pc, final L1ItemInstance item) {
        // 預設把 data 視為 0，交給整數多載處理
        execute(0, pc, item);
    }

    /** 相容多載：提供 int[] 版本，取首元素。不可標 @Override。 */
    public final void execute(final int[] data, final L1PcInstance pc, final L1ItemInstance item) {
        final int d = (data != null && data.length > 0) ? data[0] : 0;
        execute(d, pc, item);
    }

    /** 子類真正在意的實作點 */
    protected abstract void execute(int data, L1PcInstance pc, L1ItemInstance item);
}
