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
     * Internal hook  -- set by PassiveRegenHandler on construction.
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
     * (highest wins  -- boosts do <em>not</em> stack additively).
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
    /**
     * Reduces the remaining damage cooldown by a percentage of what is left.
     *
     * <p>Example: if 8 seconds remain on a 10-second cooldown and percentReduction is 50,
     * the remaining wait becomes 4 seconds. Has no effect once regen is already active.
     * Values are clamped to 0-100.
     *
     * @param playerUUID      UUID of the server-side player to affect.
     * @param percentReduction Percentage of remaining cooldown to cut (0-100).
     */
    public static void reduceCooldown(UUID playerUUID, int percentReduction) {
        if (internals != null) {
            internals.reduceCooldown(playerUUID, percentReduction);
        }
    }

    /**
     * Returns true if the player is currently past their damage cooldown and passive regen
     * can actually heal them right now.
     */
    public static boolean isRegenReady(UUID playerUUID) {
        return internals != null && internals.isRegenReady(playerUUID);
    }

    /**
     * Returns true if passive regen is specifically blocked by insufficient hunger.
     */
    public static boolean isHungerBlocked(UUID playerUUID) {
        return internals != null && internals.isHungerBlocked(playerUUID);
    }

    /**
     * Returns the remaining cooldown in ticks before passive regen can begin.
     * Returns 0 if the player is already ready or unavailable.
     */
    public static int getRemainingCooldownTicks(UUID playerUUID) {
        return internals != null ? internals.getRemainingCooldownTicks(playerUUID) : 0;
    }

    /**
     * Returns the effective heal amount that would be applied on the current regen update,
     * after all active bonuses and temporary modifiers.
     */
    public static float getCurrentHealRate(UUID playerUUID) {
        return internals != null ? internals.getCurrentHealRate(playerUUID) : 0.0F;
    }

    /**
     * Applies a temporary regen penalty to a player.
     *
     * <p>Values below 1.0 slow regen. The strongest penalty wins and equal values refresh
     * duration. Values are clamped to the 0.0-1.0 range.
     */
    public static void applyRegenPenalty(UUID playerUUID, double multiplier, int durationTicks) {
        if (internals != null) {
            internals.applyRegenPenalty(playerUUID, multiplier, durationTicks);
        }
    }

    /**
     * Completely blocks passive regen for the duration.
     */
    public static void blockRegen(UUID playerUUID, int durationTicks) {
        if (internals != null) {
            internals.blockRegen(playerUUID, durationTicks);
        }
    }

    /**
     * Temporarily bypasses the hunger and saturation minimum thresholds for a player.
     *
     * <p>While the override is active, regen fires at full rate regardless of how low
     * the player's hunger or saturation is.  The hunger-penalty multipliers are also
     * skipped.  Useful for consumable items (bandages, food buffs) that should work
     * even when the player is starving.
     *
     * <p>Calling again while an override is already active refreshes the duration if the
     * new expiry would be later than the current one.
     *
     * @param playerUUID    UUID of the server-side player to affect.
     * @param durationTicks How long the bypass lasts in game ticks (20 ticks = 1 second).
     */
    public static void overrideHungerRestrictions(UUID playerUUID, int durationTicks) {
        if (internals != null) {
            internals.overrideHungerRestrictions(playerUUID, durationTicks);
        }
    }
}
