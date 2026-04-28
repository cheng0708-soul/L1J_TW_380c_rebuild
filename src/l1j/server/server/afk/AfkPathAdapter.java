package l1j.server.server.afk;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * Thin static wrapper around AfkPathService.
 * NOTE: The underlying service methods are {@code void}; do not 'return' them.
 */
public final class AfkPathAdapter {
    private AfkPathAdapter() {}

    public static void clear(final L1PcInstance pc) {
        // was: return AfkPathService.clear(pc);
        AfkPathService.clear(pc);
    }

    public static void stepToward(final L1PcInstance pc, final int tx, final int ty) {
        // was: return AfkPathService.stepToward(pc, tx, ty);
        AfkPathService.stepToward(pc, tx, ty);
    }
}
