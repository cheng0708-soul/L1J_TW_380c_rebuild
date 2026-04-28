package l1j.server.server.utils;

import l1j.server.server.model.Instance.L1PcInstance;
import l1j.server.server.afk.AfkCruiseEngine;
import l1j.server.server.afk.AfkMagicRegistry;
import l1j.server.server.afk.AfkService;

/**
 * Bridge hooks for legacy Autofarm + AFK engines.
 * On death / return-town, hard-stop all automation.
 */
public final class AutofarmHooks {

    private AutofarmHooks() {}

    /** Called from L1PcInstance.death(...) */
    public static void onDeath(final L1PcInstance pc) {
        // Legacy autofarm
        AutofarmRegistry.stopFor(pc);
        // AFK engines
        try { AfkService.stop(pc); } catch (Throwable ignore) {}
        try { AfkCruiseEngine.stop(pc); } catch (Throwable ignore) {}
        try { AfkMagicRegistry.setEnabled(pc, false); } catch (Throwable ignore) {}
    }

    /** Called from return-town flow */
    public static void onReturnTown(final L1PcInstance pc) {
        // Legacy autofarm
        AutofarmRegistry.stopFor(pc);
        // AFK engines
        try { AfkService.stop(pc); } catch (Throwable ignore) {}
        try { AfkCruiseEngine.stop(pc); } catch (Throwable ignore) {}
        try { AfkMagicRegistry.setEnabled(pc, false); } catch (Throwable ignore) {}
    }
}
