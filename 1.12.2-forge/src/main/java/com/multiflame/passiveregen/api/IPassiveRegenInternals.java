package com.multiflame.passiveregen.api;

import java.util.UUID;

/**
 * Internal hook implemented by PassiveRegenHandler.
 * Not part of the public API  -- do not reference from addon mods.
 */
public interface IPassiveRegenInternals {
    void clearDamageCooldown(UUID playerUUID);
    void applyRegenBoost(UUID playerUUID, double multiplier, int durationTicks);
    void reduceCooldown(UUID playerUUID, int percentReduction);
    boolean isRegenReady(UUID playerUUID);
    boolean isHungerBlocked(UUID playerUUID);
    int getRemainingCooldownTicks(UUID playerUUID);
    float getCurrentHealRate(UUID playerUUID);
    void applyRegenPenalty(UUID playerUUID, double multiplier, int durationTicks);
    void blockRegen(UUID playerUUID, int durationTicks);
    void overrideHungerRestrictions(UUID playerUUID, int durationTicks);
}
