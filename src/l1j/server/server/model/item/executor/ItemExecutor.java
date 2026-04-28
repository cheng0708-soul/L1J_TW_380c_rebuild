package l1j.server.server.model.item.executor;

import l1j.server.server.model.Instance.L1ItemInstance;
import l1j.server.server.model.Instance.L1PcInstance;

/**
 * Canonical ItemExecutor interface for L1J_TW_3.80c.
 * Single required method. Does NOT extend any other interface.
 */
public interface ItemExecutor {
    void execute(L1PcInstance pc, L1ItemInstance item);
}
