package com.multiflame.passiveregen.api;

import java.util.UUID;

public final class PassiveRegenAPI {
    private static IPassiveRegenInternals internals;

    public static void register(IPassiveRegenInternals impl) {
        if (internals == null) {
            internals = impl;
        }
    }

    private PassiveRegenAPI() {
    }

    public static boolean isAvailable() {
        return internals != null;
    }

    public static void clearDamageCooldown(UUID playerUUID) {
        if (internals != null) {
            internals.clearDamageCooldown(playerUUID);
        }
    }

    public static void applyRegenBoost(UUID playerUUID, double multiplier, int durationTicks) {
        if (internals != null) {
            internals.applyRegenBoost(playerUUID, multiplier, durationTicks);
        }
    }

    public static void reduceCooldown(UUID playerUUID, int percentReduction) {
        if (internals != null) {
            internals.reduceCooldown(playerUUID, percentReduction);
        }
    }

    public static boolean isRegenReady(UUID playerUUID) {
        return internals != null && internals.isRegenReady(playerUUID);
    }

    public static boolean isHungerBlocked(UUID playerUUID) {
        return internals != null && internals.isHungerBlocked(playerUUID);
    }

    public static int getRemainingCooldownTicks(UUID playerUUID) {
        return internals != null ? internals.getRemainingCooldownTicks(playerUUID) : 0;
    }

    public static float getCurrentHealRate(UUID playerUUID) {
        return internals != null ? internals.getCurrentHealRate(playerUUID) : 0.0F;
    }

    public static void applyRegenPenalty(UUID playerUUID, double multiplier, int durationTicks) {
        if (internals != null) {
            internals.applyRegenPenalty(playerUUID, multiplier, durationTicks);
        }
    }

    public static void blockRegen(UUID playerUUID, int durationTicks) {
        if (internals != null) {
            internals.blockRegen(playerUUID, durationTicks);
        }
    }

    public static void overrideHungerRestrictions(UUID playerUUID, int durationTicks) {
        if (internals != null) {
            internals.overrideHungerRestrictions(playerUUID, durationTicks);
        }
    }
}
