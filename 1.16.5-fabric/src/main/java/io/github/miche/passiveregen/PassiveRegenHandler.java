package io.github.miche.passiveregen;

import io.github.miche.passiveregen.api.IPassiveRegenInternals;
import io.github.miche.passiveregen.api.PassiveRegenAPI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

public final class PassiveRegenHandler implements IPassiveRegenInternals {
    private final Map<UUID, Long> lastDamageTicks = new HashMap<>();
    private final Map<UUID, Float> lastKnownHealth = new HashMap<>();
    private final Map<UUID, RegenBoost> activeBoosts = new HashMap<>();
    private PassiveRegenConfig storedConfig;
    static volatile long serverTick = 0;

    public PassiveRegenHandler() {
        PassiveRegenAPI.register(this);
    }

    // ── PassiveRegenAPI implementation ────────────────────────────────────────

    @Override
    public void clearDamageCooldown(UUID playerUUID) {
        // 0L = "last damaged at world tick zero"  -- cooldown is always expired by the time
        // a player can use an item, so regen starts on the very next handler tick.
        lastDamageTicks.put(playerUUID, 0L);
    }

    @Override
    public void applyRegenBoost(UUID playerUUID, double multiplier, int durationTicks) {
        double m = Math.max(1.0, multiplier);
        long expiresAt = serverTick + durationTicks;
        RegenBoost existing = activeBoosts.get(playerUUID);
        // Highest multiplier wins. Equal multiplier refreshes duration.
        if (existing == null || m >= existing.multiplier) {
            activeBoosts.put(playerUUID, new RegenBoost(m, expiresAt));
        }
    }

    @Override
    public void reduceCooldown(UUID playerUUID, int percentReduction) {
        if (storedConfig == null) return;
        Long last = lastDamageTicks.get(playerUUID);
        if (last == null) return;
        long remaining = last + storedConfig.damageCooldownTicks - serverTick;
        if (remaining <= 0) return;
        int pct = Math.max(0, Math.min(100, percentReduction));
        long cut = (long)(remaining * (pct / 100.0));
        lastDamageTicks.put(playerUUID, last - cut);
    }

    // ── Lifecycle callbacks ───────────────────────────────────────────────────

    public void onServerTick(Iterable<ServerPlayer> players, PassiveRegenConfig config) {
        this.storedConfig = config;
        if (!config.enabled) {
            return;
        }

        for (ServerPlayer player : players) {
            long now = player.level.getGameTime();
            serverTick = now;
            UUID playerId = player.getUUID();

            // Health-delta damage detection (fallback; no ALLOW_DAMAGE in 1.16.5 Fabric API)
            float currentHealth = player.getHealth();
            Float previousHealth = lastKnownHealth.put(playerId, currentHealth);
            if (previousHealth != null && currentHealth < previousHealth) {
                lastDamageTicks.put(playerId, now);
            }

            // Disable vanilla natural regen by draining food exhaustion
            if (config.disableNaturalRegen) {
                if (player.getFoodData().getFoodLevel() >= 18 && player.getFoodData().getSaturationLevel() > 0) {
                    player.causeFoodExhaustion(0.1F);
                }
            }

            if (!shouldProcessPlayer(player, config)) {
                continue;
            }

            int updateTicks = Math.max(1, config.updateIntervalTicks);
            if ((now + player.getId()) % updateTicks != 0L) {
                continue;
            }

            int foodLevel = player.getFoodData().getFoodLevel();

            Long lastDamageTick = lastDamageTicks.get(playerId);
            if (lastDamageTick == null) {
                continue;
            }
            long outOfCombatTicks = now - lastDamageTick;
            int effectiveCooldown = config.getEffectiveDamageCooldown(foodLevel);
            if (outOfCombatTicks < effectiveCooldown) {
                continue;
            }

            float healAmount = config.getHealAmountPerUpdate(outOfCombatTicks, player.getMaxHealth(), foodLevel);

            RegenBoost boost = activeBoosts.get(playerId);
            if (boost != null) {
                if (now >= boost.expiresAt) {
                    activeBoosts.remove(playerId);
                } else {
                    healAmount = (float) (healAmount * boost.multiplier);
                }
            }

            if (healAmount > 0.0F) {
                player.heal(healAmount);
            }
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        UUID playerId = player.getUUID();
        lastDamageTicks.remove(playerId);
        lastKnownHealth.remove(playerId);
        activeBoosts.remove(playerId);
    }

    public void onPlayerRespawn(ServerPlayer player) {
        UUID playerId = player.getUUID();
        lastDamageTicks.remove(playerId);
        lastKnownHealth.remove(playerId);
        activeBoosts.remove(playerId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public void onEntityKilled(ServerPlayer killer, PassiveRegenConfig config, long now) {
        if (!config.regenOnKillEnabled) return;
        UUID killerId = killer.getUUID();
        Long lastDamageTick = lastDamageTicks.get(killerId);
        if (lastDamageTick == null) return;
        int effectiveCooldown = config.getEffectiveDamageCooldown(killer.getFoodData().getFoodLevel());
        long remaining = lastDamageTick + effectiveCooldown - now;
        if (remaining <= 0) return;
        int reduction = Math.max(0, Math.min(100, config.regenOnKillCooldownReduction));
        long reduced = (long) (remaining * (reduction / 100.0D));
        lastDamageTicks.put(killerId, lastDamageTick - reduced);
    }

    private static boolean shouldProcessPlayer(ServerPlayer player, PassiveRegenConfig config) {
        return player.isAlive()
            && !player.isSpectator()
            && !player.abilities.invulnerable
            && player.getFoodData().getFoodLevel() >= config.getMinimumFoodLevel()
            && player.getHealth() < player.getMaxHealth() * (config.maxRegenHealthPercent / 100.0f)
            && !hasBlockedEffect(player, config.blockedEffects)
            && !isDimensionBlacklisted(player, config.dimensionBlacklist)
            && (config.regenWhileSprinting || !player.isSprinting());
    }

    private static boolean hasBlockedEffect(net.minecraft.server.level.ServerPlayer player, String[] blockedEffects) {
        if (blockedEffects == null || blockedEffects.length == 0) return false;
        java.util.Set<String> blocked = new java.util.HashSet<>(java.util.Arrays.asList(blockedEffects));
        for (net.minecraft.world.effect.MobEffectInstance inst : player.getActiveEffects()) {
            net.minecraft.resources.ResourceLocation id = net.minecraft.core.Registry.MOB_EFFECT.getKey(inst.getEffect());
            if (id != null && blocked.contains(id.toString())) return true;
        }
        return false;
    }

    private static boolean isDimensionBlacklisted(net.minecraft.server.level.ServerPlayer player, String[] dimensionBlacklist) {
        if (dimensionBlacklist == null || dimensionBlacklist.length == 0) return false;
        String dimId = player.level.dimension().location().toString();
        return java.util.Arrays.asList(dimensionBlacklist).contains(dimId);
    }

    private static final class RegenBoost {
        final double multiplier;
        final long expiresAt;

        RegenBoost(double multiplier, long expiresAt) {
            this.multiplier = multiplier;
            this.expiresAt = expiresAt;
        }
    }
}
