// Copyright (c) 2025 AFK helper patch
// Package: l1j.server.server.afk
package l1j.server.server.afk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l1j.server.server.model.Instance.L1PcInstance;

/**
 * Thin facade between game events and the per-player AFK controller.
 * Keeps one controller per player id and forwards lifecycle + combat events.
 */
public final class AfkEventBridge {

    private static final Map<Integer, AfkAiController> CONTROLLERS = new ConcurrentHashMap<>();

    private AfkEventBridge() {}

    private static AfkAiController of(final L1PcInstance pc) {
        return CONTROLLERS.computeIfAbsent(pc.getId(), id -> new AfkAiController(pc));
    }

    /** Toggle AFK on/off for this player. */
    public static void toggle(final L1PcInstance pc) { of(pc).toggle(); }

    /** Player died -> cleanly disable and persist last state. */
    public static void onDeath(final L1PcInstance pc) { of(pc).onDeath(); }

    /** Player used town warp (teleport to safety). */
    public static void onTownWarp(final L1PcInstance pc) { of(pc).onTownWarp(); }

    /** Socket down -> give controller a chance to persist state and clear. */
    public static void onDisconnect(final L1PcInstance pc) {
        try { of(pc).onDisconnect(); } finally { CONTROLLERS.remove(pc.getId()); }
    }

    /** Player reconnected -> restore last state if needed. */
    public static void onReconnect(final L1PcInstance pc) { of(pc).onReconnect(); }

    /**
     * NEW: call this when a physical or magical attack performed by the player
     * has successfully landed (hit/skill).
     * Some AFK implementations use this to reset internal timers and keep the
     * engine awake while in combat.
     */
    public static void onSuccessfulHit(final L1PcInstance pc) {
        of(pc).onSuccessfulHit();
    }

    /** Periodic driver (e.g., scheduled by server loop). */
    public static void tick(final L1PcInstance pc) { of(pc).tick(); }
}
