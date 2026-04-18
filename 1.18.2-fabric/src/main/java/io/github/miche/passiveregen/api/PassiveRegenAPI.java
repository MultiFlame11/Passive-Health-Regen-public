package io.github.miche.passiveregen.api;

import java.util.UUID;

/**
 * Public API for interacting with passive-health-regen from addon mods.
 *
 * <p>All methods silently do nothing if the host mod is not loaded.
 * Check {@link #isAvailable()} before relying on effects in optional-dependency scenarios.
 *
 * <p>Call all methods from the server thread (e.g. during item {@code use()} or {@code finishUsingItem()}).
 */
public final class PassiveRegenAPI {

    /**
     * Internal hook — set by PassiveRegenHandler on construction.
     * Package-private so only classes in this package can write it.
     */
    private static IPassiveRegenInternals internals;

    /** Called by PassiveRegenHandler at mod load. Do not call from addon mods. */
    public static void register(IPassiveRegenInternals impl) {
        if (internals == null) internals = impl;
    }

    private PassiveRegenAPI() {}

    /**
     * Returns {@code true} if passive-health-regen is loaded and the API hook is registered.
     * When {@code false}, all other methods are no-ops.
     */
    public static boolean isAvailable() {
        return internals != null;
    }

    /**
     * Immediately clears the damage cooldown for a player so passive regen
     * can start on the very next handler tick.
     *
     * <p>If the player has never taken damage this session (no cooldown entry yet),
     * this call seeds the entry so regen will begin.  Safe to call at full health.
     *
     * @param playerUUID UUID of the server-side player to affect.
     */
    public static void clearDamageCooldown(UUID playerUUID) {
        if (internals != null) {
            internals.clearDamageCooldown(playerUUID);
        }
    }

    /**
     * Applies a temporary regen speed multiplier to a player.
     *
     * <p>If a boost is already active, the new one replaces it only when its
     * {@code multiplier} is greater than or equal to the active boost's multiplier
     * (highest wins — boosts do <em>not</em> stack additively).
     * Applying the same multiplier refreshes the duration.
     *
     * @param playerUUID    UUID of the server-side player to affect.
     * @param multiplier    Speed multiplier. {@code 1.5} = 50% faster, {@code 2.0} = double.
     *                      Values below {@code 1.0} are clamped to {@code 1.0}.
     * @param durationTicks Duration in game ticks (20 ticks = 1 second).
     */
    public static void applyRegenBoost(UUID playerUUID, double multiplier, int durationTicks) {
        if (internals != null) {
            internals.applyRegenBoost(playerUUID, multiplier, durationTicks);
        }
    }
}
