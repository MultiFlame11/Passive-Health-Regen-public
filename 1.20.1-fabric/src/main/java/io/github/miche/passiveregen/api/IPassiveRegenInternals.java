package io.github.miche.passiveregen.api;

import java.util.UUID;

/**
 * Internal hook implemented by PassiveRegenHandler.
 * Not part of the public API — do not reference from addon mods.
 */
public interface IPassiveRegenInternals {
    void clearDamageCooldown(UUID playerUUID);
    void applyRegenBoost(UUID playerUUID, double multiplier, int durationTicks);
}
