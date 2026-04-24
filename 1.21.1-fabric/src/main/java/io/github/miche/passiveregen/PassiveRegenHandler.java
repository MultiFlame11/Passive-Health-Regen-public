package io.github.miche.passiveregen;

import io.github.miche.passiveregen.api.IPassiveRegenInternals;
import io.github.miche.passiveregen.api.PassiveRegenAPI;
import io.github.miche.passiveregen.network.RegenHudPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.LightLayer;

public final class PassiveRegenHandler implements IPassiveRegenInternals {
    private final Map<UUID, Long> lastDamageTicks = new HashMap<>();
    private final Map<UUID, Integer> cooldownDurations = new HashMap<>();
    private final Map<UUID, RegenBoost> activeBoosts = new HashMap<>();
    private final Map<UUID, RegenPenalty> activePenalties = new HashMap<>();
    private final Map<UUID, Long> hungerOverrides = new HashMap<>();
    private final Map<UUID, Long> lastKillTicks = new HashMap<>();
    private final Map<UUID, Integer> killComboStacks = new HashMap<>();
    private final Map<UUID, HudSyncState> lastHudStates = new HashMap<>();
    private final Map<UUID, Boolean> campfireCache = new HashMap<>();
    private final Map<UUID, Long> campfireNextCheckTick = new HashMap<>();
    private final Set<UUID> campfireCooldownApplied = new HashSet<>();
    private final Set<UUID> saturationBonusActive = new HashSet<>();
    private final Map<UUID, Float> frozenMaxDuringCooldown = new HashMap<>();
    private final Map<UUID, Double> hungerDrainRemainders = new HashMap<>();
    private PassiveRegenConfig storedConfig;
    private MinecraftServer currentServer;
    static volatile long serverTick = 0L;

    public PassiveRegenHandler() {
        PassiveRegenAPI.register(this);
    }

    public void setConfig(PassiveRegenConfig config) {
        this.storedConfig = config;
    }

    @Override
    public void clearDamageCooldown(UUID playerUUID) {
        ServerPlayer player = resolvePlayer(playerUUID);
        long now = player != null ? player.serverLevel().getGameTime() : serverTick;
        lastDamageTicks.put(playerUUID, now);
        cooldownDurations.put(playerUUID, 0);
        if (player != null) {
            setSavedCooldownState(player, 0L, 0);
        }
    }

    @Override
    public void applyRegenBoost(UUID playerUUID, double multiplier, int durationTicks) {
        double clampedMultiplier = Math.max(1.0D, multiplier);
        long expiresAt = getCurrentTick(playerUUID) + Math.max(0, durationTicks);
        RegenBoost existing = activeBoosts.get(playerUUID);
        if (existing == null || clampedMultiplier >= existing.multiplier) {
            activeBoosts.put(playerUUID, new RegenBoost(clampedMultiplier, expiresAt));
        }
    }

    @Override
    public void reduceCooldown(UUID playerUUID, int percentReduction) {
        Long lastDamageTick = lastDamageTicks.get(playerUUID);
        if (lastDamageTick == null) return;

        ServerPlayer player = resolvePlayer(playerUUID);
        int cooldownDuration = getCooldownDuration(playerUUID, player);
        long now = getCurrentTick(playerUUID);
        long remaining = lastDamageTick + cooldownDuration - now;
        if (remaining <= 0L) return;

        int reduction = Math.max(0, Math.min(100, percentReduction));
        long cut = (long)(remaining * (reduction / 100.0D));
        long adjustedLastDamageTick = lastDamageTick - cut;
        lastDamageTicks.put(playerUUID, adjustedLastDamageTick);
        if (player != null) {
            long outOfCombatTicks = Math.max(0L, player.serverLevel().getGameTime() - adjustedLastDamageTick);
            setSavedCooldownState(player, outOfCombatTicks, cooldownDuration);
        }
    }

    @Override
    public boolean isRegenReady(UUID playerUUID) {
        if (storedConfig == null) return false;
        ServerPlayer player = resolvePlayer(playerUUID);
        if (player == null) return false;

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.serverLevel().getGameTime());
        int cooldownDuration = getCooldownDuration(playerUUID, player);
        return outOfCombatTicks >= cooldownDuration && computeHealAmount(player, outOfCombatTicks) > 0.0F;
    }

    @Override
    public boolean isHungerBlocked(UUID playerUUID) {
        if (storedConfig == null) return false;
        ServerPlayer player = resolvePlayer(playerUUID);
        if (player == null) return false;

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.serverLevel().getGameTime());
        return isHungerBlocked(player, player.getFoodData().getFoodLevel(), outOfCombatTicks);
    }

    @Override
    public int getRemainingCooldownTicks(UUID playerUUID) {
        if (storedConfig == null) return 0;
        ServerPlayer player = resolvePlayer(playerUUID);
        if (player == null) return 0;

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.serverLevel().getGameTime());
        int cooldownDuration = getCooldownDuration(playerUUID, player);
        return Math.max(0, cooldownDuration - (int)Math.min(Integer.MAX_VALUE, outOfCombatTicks));
    }

    @Override
    public float getCurrentHealRate(UUID playerUUID) {
        if (storedConfig == null) return 0.0F;
        ServerPlayer player = resolvePlayer(playerUUID);
        if (player == null) return 0.0F;

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.serverLevel().getGameTime());
        return computeHealAmount(player, outOfCombatTicks);
    }

    @Override
    public void applyRegenPenalty(UUID playerUUID, double multiplier, int durationTicks) {
        double clampedMultiplier = Math.max(0.0D, Math.min(1.0D, multiplier));
        long expiresAt = getCurrentTick(playerUUID) + Math.max(0, durationTicks);
        RegenPenalty existing = activePenalties.get(playerUUID);
        if (existing == null || clampedMultiplier <= existing.multiplier) {
            activePenalties.put(playerUUID, new RegenPenalty(clampedMultiplier, expiresAt));
        }
    }

    @Override
    public void blockRegen(UUID playerUUID, int durationTicks) {
        applyRegenPenalty(playerUUID, 0.0D, durationTicks);
    }

    @Override
    public void overrideHungerRestrictions(UUID playerUUID, int durationTicks) {
        long expiresAt = getCurrentTick(playerUUID) + Math.max(0, durationTicks);
        Long existing = hungerOverrides.get(playerUUID);
        if (existing == null || expiresAt > existing) {
            hungerOverrides.put(playerUUID, expiresAt);
        }
    }

    public void onPlayerDamaged(ServerPlayer player, DamageSource source, float amount, PassiveRegenConfig config) {
        currentServer = player.server;
        storedConfig = config;
        long now = player.serverLevel().getGameTime();
        serverTick = now;
        int cooldownTicks = computeDamageCooldownTicks(player, source, amount, config);
        UUID playerId = player.getUUID();
        lastDamageTicks.put(playerId, now);
        cooldownDurations.put(playerId, cooldownTicks);
        frozenMaxDuringCooldown.remove(playerId);
        campfireCooldownApplied.remove(playerId);
        setSavedCooldownState(player, 0L, cooldownTicks);
    }

    public void onEntityKilled(ServerPlayer killer, LivingEntity victim, PassiveRegenConfig config) {
        currentServer = killer.server;
        storedConfig = config;
        if (!config.enabled || !config.regenOnKillEnabled || isKillIgnored(victim, config)) return;

        UUID killerId = killer.getUUID();
        int comboStacks = updateKillCombo(killerId, killer.serverLevel().getGameTime(), config);
        int totalReduction = Math.max(0, Math.min(100,
            config.regenOnKillCooldownReduction + comboStacks * Math.max(0, config.regenOnKillComboReductionPerStack)));
        reduceCooldown(killerId, totalReduction);
    }

    public void onServerTick(MinecraftServer server, Iterable<ServerPlayer> players, PassiveRegenConfig config) {
        currentServer = server;
        storedConfig = config;

        for (ServerPlayer player : players) {
            long now = player.serverLevel().getGameTime();
            serverTick = now;
            UUID playerId = player.getUUID();
            boolean justHealed = false;
            Long lastDamageTick = lastDamageTicks.get(playerId);
            long outOfCombatTicks = getOutOfCombatTicks(playerId, now);
            syncPersistedCooldownState(player, lastDamageTick, outOfCombatTicks);

            if (!config.enabled) {
                syncHudState(player, outOfCombatTicks, getCooldownDuration(playerId, player), false, false, false, config.maxRegenHealthPercent);
                continue;
            }

            int cooldownDuration = getCooldownDuration(playerId, player);
            boolean hungerBlocked = isHungerBlocked(player, player.getFoodData().getFoodLevel(), outOfCombatTicks);

            if (config.disableNaturalRegen) {
                if (player.getFoodData().getFoodLevel() >= 18 && player.getFoodData().getSaturationLevel() > 0.0F) {
                    player.causeFoodExhaustion(0.1F);
                }
            }

            if (lastDamageTick == null) {
                syncHudState(player, 0L, 0, false, false, false, config.maxRegenHealthPercent);
                continue;
            }

            if (!shouldProcessPlayer(player, config)) {
                applyHungerDrainIdle(player, playerId, outOfCombatTicks, cooldownDuration, hungerBlocked, config);
                syncHudState(player, outOfCombatTicks, cooldownDuration, false, hungerBlocked, false, config.maxRegenHealthPercent);
                continue;
            }

            int updateTicks = Math.max(1, config.updateIntervalTicks);
            if ((now + player.getId()) % updateTicks != 0L) {
                HudSyncState previousHudState = lastHudStates.get(playerId);
                boolean keepActivePulse = outOfCombatTicks >= cooldownDuration
                    && previousHudState != null
                    && previousHudState.regenActive;
                syncHudState(player, outOfCombatTicks, cooldownDuration, keepActivePulse, hungerBlocked, false, config.maxRegenHealthPercent);
                continue;
            }

            if (outOfCombatTicks < cooldownDuration) {
                if (config.campfireRegenEnabled
                        && config.campfireCooldownReductionEnabled
                        && config.campfireCooldownReductionPercent > 0
                        && !campfireCooldownApplied.contains(playerId)
                        && isNearCampfireCached(player)) {
                    campfireCooldownApplied.add(playerId);
                    reduceCooldown(playerId, config.campfireCooldownReductionPercent);
                    outOfCombatTicks = getOutOfCombatTicks(playerId, now);
                    cooldownDuration = getCooldownDuration(playerId, player);
                }
                if (outOfCombatTicks < cooldownDuration) {
                    syncHudState(player, outOfCombatTicks, cooldownDuration, false, false, false, config.maxRegenHealthPercent);
                    continue;
                }
            }

            float healAmount = computeHealAmount(player, outOfCombatTicks);
            boolean regenActive = healAmount > 0.0F;

            if (healAmount > 0.0F) {
                float beforeHp = player.getHealth();
                player.heal(healAmount);
                float actualHealed = Math.max(0.0F, player.getHealth() - beforeHp);

                if (actualHealed > 0.0F
                        && config.saturationBonusEnabled
                        && config.saturationBonusCostPerHp > 0.0D
                        && (config.saturationBonusHealMultiplier > 1.0D || config.saturationBonusSpeedMultiplier > 1.0D)
                        && saturationBonusActive.contains(playerId)) {
                    float currentSat = player.getFoodData().getSaturationLevel();
                    float floor = (float) config.saturationBonusMinSaturationFloor;
                    float headroom = Math.max(0.0F, currentSat - floor);
                    float rawCost = actualHealed * (float) config.saturationBonusCostPerHp;
                    float chargedCost = Math.min(rawCost, headroom);
                    if (chargedCost > 0.0F) {

                        player.causeFoodExhaustion(chargedCost * 4.0F);
                    }
                }

                if (actualHealed > 0.0F) {
                    applyHungerDrainHealCost(player, playerId, actualHealed, config);
                }

                justHealed = true;
                regenActive = true;
            }

            if (config.saturationBonusEnabled
                    && config.saturationBonusIdleDrainPerTick > 0.0D
                    && (config.saturationBonusHealMultiplier > 1.0D || config.saturationBonusSpeedMultiplier > 1.0D)
                    && saturationBonusActive.contains(playerId)) {
                float currentSat = player.getFoodData().getSaturationLevel();
                float floor = (float) config.saturationBonusMinSaturationFloor;
                float headroom = Math.max(0.0F, currentSat - floor);
                float idleCost = Math.min((float) config.saturationBonusIdleDrainPerTick, headroom);
                if (idleCost > 0.0F) {
                    player.causeFoodExhaustion(idleCost * 4.0F);
                }
            }

            syncHudState(player, outOfCombatTicks, cooldownDuration, regenActive, hungerBlocked, justHealed, config.maxRegenHealthPercent);
        }
    }

    public void onPlayerDisconnect(ServerPlayer player) {
        currentServer = player.server;
        UUID playerId = player.getUUID();
        Long lastDamageTick = lastDamageTicks.get(playerId);
        if (lastDamageTick == null) {
            clearSavedCooldownState(player);
        } else {
            long outOfCombatTicks = Math.max(0L, player.serverLevel().getGameTime() - lastDamageTick);
            int cooldownDuration = getCooldownDuration(playerId, player);
            setSavedCooldownState(player, outOfCombatTicks, cooldownDuration);
        }
        activeBoosts.remove(playerId);
        activePenalties.remove(playerId);
        hungerOverrides.remove(playerId);
        lastHudStates.remove(playerId);
        lastKillTicks.remove(playerId);
        killComboStacks.remove(playerId);
        lastDamageTicks.remove(playerId);
        cooldownDurations.remove(playerId);
        campfireCache.remove(playerId);
        campfireNextCheckTick.remove(playerId);
        frozenMaxDuringCooldown.remove(playerId);
        campfireCooldownApplied.remove(playerId);
        saturationBonusActive.remove(playerId);
        hungerDrainRemainders.remove(playerId);
    }

    public void onPlayerLogin(ServerPlayer player) {
        currentServer = player.server;
        UUID playerId = player.getUUID();
        activeBoosts.remove(playerId);
        activePenalties.remove(playerId);
        hungerOverrides.remove(playerId);
        lastHudStates.remove(playerId);
        hungerDrainRemainders.remove(playerId);
        restoreCooldownStateAfterReconnect(player);

        Long lastDamageTick = lastDamageTicks.get(playerId);
        if (lastDamageTick == null) {
            syncHudState(player, 0L, 0, false, false, false, storedConfig != null ? storedConfig.maxRegenHealthPercent : 100);
            return;
        }

        PassiveRegenConfig config = storedConfig;
        if (config == null) return;
        long outOfCombatTicks = getOutOfCombatTicks(playerId, player.serverLevel().getGameTime());
        int cooldownDuration = getCooldownDuration(playerId, player);
        boolean hungerBlocked = isHungerBlocked(player, player.getFoodData().getFoodLevel(), outOfCombatTicks);
        syncHudState(player, outOfCombatTicks, cooldownDuration, false, hungerBlocked, false, config.maxRegenHealthPercent);
    }

    public void onPlayerRespawn(ServerPlayer player) {
        currentServer = player.server;
        UUID playerId = player.getUUID();
        lastDamageTicks.remove(playerId);
        cooldownDurations.remove(playerId);
        activeBoosts.remove(playerId);
        activePenalties.remove(playerId);
        hungerOverrides.remove(playerId);
        lastHudStates.remove(playerId);
        frozenMaxDuringCooldown.remove(playerId);
        campfireCooldownApplied.remove(playerId);
        saturationBonusActive.remove(playerId);
        hungerDrainRemainders.remove(playerId);
        clearSavedCooldownState(player);
        syncHudState(player, 0L, 0, false, false, false, storedConfig != null ? storedConfig.maxRegenHealthPercent : 100);
    }

    private float computeHealAmount(ServerPlayer player, long outOfCombatTicks) {
        if (storedConfig == null || !storedConfig.enabled || !shouldProcessPlayer(player, storedConfig)) {
            return 0.0F;
        }

        UUID playerId = player.getUUID();
        int foodLevel = player.getFoodData().getFoodLevel();
        float freezeProgress = getFreezingProgress(player);

        int cooldownDuration = getCooldownDuration(playerId, player);
        if (freezeProgress > 0.0F && outOfCombatTicks < cooldownDuration) {
            Float prev = frozenMaxDuringCooldown.get(playerId);
            if (prev == null || freezeProgress > prev) {
                frozenMaxDuringCooldown.put(playerId, freezeProgress);
            }
        }
        Float stickyProgress = frozenMaxDuringCooldown.get(playerId);
        if (stickyProgress != null
                && storedConfig.freezingPenaltyEnabled
                && storedConfig.freezingCooldownMultiplier > 1.0D) {
            double cdMult = 1.0D + (storedConfig.freezingCooldownMultiplier - 1.0D) * stickyProgress;
            cooldownDuration = (int) Math.ceil(cooldownDuration * cdMult);
        }
        if (outOfCombatTicks < cooldownDuration) {
            return 0.0F;
        }
        frozenMaxDuringCooldown.remove(playerId);

        boolean hungerPenalized = !isHungerOverrideActive(playerId)
            && storedConfig.hungerPenaltyEnabled
            && (foodLevel < storedConfig.getMinimumFoodLevel()
                || (storedConfig.minimumSaturationLevel > 0.0D
                    && player.getFoodData().getSaturationLevel() < storedConfig.minimumSaturationLevel));

        if (freezeProgress >= 1.0F && storedConfig.freezingBlocksRegen) {
            return 0.0F;
        }

        if (storedConfig.disableHealingDuringWither && player.hasEffect(MobEffects.WITHER)) {
            return 0.0F;
        }
        if (storedConfig.disableHealingDuringPoison && player.hasEffect(MobEffects.POISON)) {
            return 0.0F;
        }

        double healAmount = Math.max(0.01D, storedConfig.healAmountPerTrigger);
        double scaledHeal = healAmount * getMaxHealthScaleMultiplier(player.getMaxHealth());
        double healBonusMultiplier = storedConfig.combineBonusMultipliers(getHealBonusMultipliers(player, foodLevel, hungerPenalized));
        double speedBonusMultiplier = storedConfig.combineBonusMultipliers(getSpeedBonusMultipliers(player, foodLevel, hungerPenalized));

        if (freezeProgress > 0.0F && storedConfig.freezingPenaltyEnabled) {
            double healMult = 1.0D + (Math.max(0.01D, storedConfig.freezingHealMultiplier) - 1.0D) * freezeProgress;
            double speedMult = 1.0D + (Math.max(0.01D, storedConfig.freezingSpeedMultiplier) - 1.0D) * freezeProgress;
            healBonusMultiplier *= Math.max(0.01D, healMult);
            speedBonusMultiplier *= Math.max(0.01D, speedMult);
        }

        double perTriggerHeal = scaledHeal * healBonusMultiplier;
        if (storedConfig.saturationBonusFlatHealBonus > 0.0D) {
            double satStrengthFlat = getSaturationBonusStrength(player);
            if (satStrengthFlat > 0.0D) {
                perTriggerHeal += storedConfig.saturationBonusFlatHealBonus * satStrengthFlat;
            }
        }

        int updateTicks = Math.max(1, storedConfig.updateIntervalTicks);
        double currentHealInterval = getCurrentHealIntervalTicks(outOfCombatTicks) / Math.max(0.0001D, speedBonusMultiplier);
        double finalHeal = perTriggerHeal * updateTicks / currentHealInterval;
        finalHeal *= getTemporaryRateMultiplier(playerId, player.serverLevel().getGameTime());
        return (float)Math.max(0.0D, finalHeal);
    }

    private double getSaturationBonusStrength(ServerPlayer player) {
        if (!isSaturationBonusActive(player)) return 0.0D;
        if (!storedConfig.saturationBonusScaleByExcess) return 1.0D;
        double sat = player.getFoodData().getSaturationLevel();
        double threshold = storedConfig.saturationBonusThreshold;
        double range = Math.max(0.01D, 20.0D - threshold);
        return Math.max(0.0D, Math.min(1.0D, (sat - threshold) / range));
    }

    private void applyHungerDrainHealCost(ServerPlayer player, UUID playerId, float actualHealed, PassiveRegenConfig config) {
        if (!config.hungerDrainEnabled
                || actualHealed <= 0.0F
                || config.hungerDrainCostPerHp <= 0.0D
                || config.hungerDrainSpeedMultiplier <= 0.0D) {
            return;
        }

        double rawDrain = actualHealed * config.hungerDrainCostPerHp * config.hungerDrainSpeedMultiplier;
        applyHungerDrain(player, playerId, rawDrain, config);
    }

    private void applyHungerDrainIdle(ServerPlayer player, UUID playerId, long outOfCombatTicks, int cooldownDuration, boolean hungerBlocked, PassiveRegenConfig config) {
        if (!config.hungerDrainEnabled
                || config.hungerDrainIdleDrainPerTick <= 0.0D
                || config.hungerDrainSpeedMultiplier <= 0.0D
                || hungerBlocked
                || outOfCombatTicks < cooldownDuration
                || !player.isAlive()
                || player.isSpectator()
                || player.isCreative()
                || hasBlockedEffect(player, config.blockedEffects)
                || isDimensionBlacklisted(player, config.dimensionBlacklist)
                || (!config.regenWhileSprinting && player.isSprinting())
                || player.getHealth() < player.getMaxHealth() * (config.maxRegenHealthPercent / 100.0F)) {
            return;
        }

        double rawDrain = config.hungerDrainIdleDrainPerTick * config.hungerDrainSpeedMultiplier;
        applyHungerDrain(player, playerId, rawDrain, config);
    }

    private void applyHungerDrain(ServerPlayer player, UUID playerId, double rawDrain, PassiveRegenConfig config) {
        if (rawDrain <= 0.0D) {
            return;
        }

        int floorFood = (int) Math.ceil(Math.max(0.0D, Math.min(20.0D, config.hungerDrainMinFloor)));
        int currentFood = player.getFoodData().getFoodLevel();
        if (currentFood <= floorFood) {
            hungerDrainRemainders.remove(playerId);
            return;
        }

        double totalDrain = hungerDrainRemainders.getOrDefault(playerId, 0.0D) + rawDrain;
        int availableWholeDrain = Math.max(0, currentFood - floorFood);
        int wholeDrain = Math.min((int) Math.floor(totalDrain), availableWholeDrain);

        if (wholeDrain > 0) {
            player.getFoodData().setFoodLevel(currentFood - wholeDrain);
        }

        int newFood = player.getFoodData().getFoodLevel();
        if (newFood <= floorFood) {
            hungerDrainRemainders.remove(playerId);
            return;
        }

        double remainder = totalDrain - wholeDrain;
        if (remainder > 0.0D) {
            hungerDrainRemainders.put(playerId, Math.min(remainder, 0.999999D));
        } else {
            hungerDrainRemainders.remove(playerId);
        }
    }

    private boolean isSaturationBonusActive(ServerPlayer player) {
        if (storedConfig == null || !storedConfig.saturationBonusEnabled) {
            saturationBonusActive.remove(player.getUUID());
            return false;
        }
        float sat = player.getFoodData().getSaturationLevel();
        UUID id = player.getUUID();
        boolean wasActive = saturationBonusActive.contains(id);
        boolean active;
        if (sat >= (float) storedConfig.saturationBonusThreshold) {
            active = true;
        } else if (wasActive && sat >= (float) storedConfig.saturationBonusDeactivateThreshold) {
            active = true;
        } else {
            active = false;
        }
        if (active) saturationBonusActive.add(id);
        else saturationBonusActive.remove(id);
        return active;
    }

    private List<Double> getHealBonusMultipliers(ServerPlayer player, int foodLevel, boolean hungerPenalized) {
        List<Double> multipliers = new ArrayList<>();

        if (hungerPenalized) {
            multipliers.add(Math.max(0.01D, storedConfig.hungerPenaltyHealMultiplier));
        } else {
            double hungerHeal = storedConfig.getHungerHealMultiplier(foodLevel);
            if (hungerHeal != 1.0D) multipliers.add(hungerHeal);
        }

        double satStrength = getSaturationBonusStrength(player);
        if (satStrength > 0.0D && storedConfig.saturationBonusHealMultiplier != 1.0D) {
            double rawMult = Math.max(1.0D, storedConfig.saturationBonusHealMultiplier);

            double effective = 1.0D + (rawMult - 1.0D) * satStrength;
            multipliers.add(effective);
        }

        if (storedConfig.crouchBonusEnabled && player.isShiftKeyDown() && storedConfig.crouchHealMultiplier != 1.0D) {
            multipliers.add(Math.max(1.0D, storedConfig.crouchHealMultiplier));
        }

        if (storedConfig.campfireRegenEnabled && storedConfig.campfireHealMultiplier != 1.0D && isNearCampfireCached(player)) {
            multipliers.add(Math.max(1.0D, storedConfig.campfireHealMultiplier));
        }

        double sharedBonus = getSharedEnvironmentalMultiplier(player);
        if (sharedBonus != 1.0D) multipliers.add(sharedBonus);

        return multipliers;
    }

    private List<Double> getSpeedBonusMultipliers(ServerPlayer player, int foodLevel, boolean hungerPenalized) {
        List<Double> multipliers = new ArrayList<>();

        if (hungerPenalized) {
            multipliers.add(Math.max(0.01D, storedConfig.hungerPenaltySpeedMultiplier));
        } else {
            double hungerSpeed = storedConfig.getHungerSpeedMultiplier(foodLevel);
            if (hungerSpeed != 1.0D) multipliers.add(hungerSpeed);
        }

        double satStrengthSpeed = getSaturationBonusStrength(player);
        if (satStrengthSpeed > 0.0D && storedConfig.saturationBonusSpeedMultiplier != 1.0D) {
            double rawMult = Math.max(1.0D, storedConfig.saturationBonusSpeedMultiplier);
            double effective = 1.0D + (rawMult - 1.0D) * satStrengthSpeed;
            multipliers.add(effective);
        }

        if (storedConfig.crouchBonusEnabled && player.isShiftKeyDown() && storedConfig.crouchSpeedMultiplier != 1.0D) {
            multipliers.add(Math.max(1.0D, storedConfig.crouchSpeedMultiplier));
        }

        if (storedConfig.campfireRegenEnabled && storedConfig.campfireSpeedMultiplier != 1.0D && isNearCampfireCached(player)) {
            multipliers.add(Math.max(1.0D, storedConfig.campfireSpeedMultiplier));
        }

        double sharedBonus = getSharedEnvironmentalMultiplier(player);
        if (sharedBonus != 1.0D) multipliers.add(sharedBonus);

        return multipliers;
    }

    private float getFreezingProgress(ServerPlayer player) {
        if (storedConfig == null || !storedConfig.freezingPenaltyEnabled) return 0.0F;
        int required = player.getTicksRequiredToFreeze();
        if (required <= 0) return 0.0F;
        float frozenPct = (float) player.getTicksFrozen() / (float) required;
        float threshold = (float) storedConfig.freezingPenaltyThresholdPercent;
        if (frozenPct <= threshold) return 0.0F;
        if (threshold >= 1.0F) return frozenPct >= 1.0F ? 1.0F : 0.0F;
        float progress = (frozenPct - threshold) / (1.0F - threshold);
        if (progress < 0.0F) return 0.0F;
        if (progress > 1.0F) return 1.0F;
        return progress;
    }

    private boolean isNearCampfireCached(ServerPlayer player) {
        UUID id = player.getUUID();
        long now = player.serverLevel().getGameTime();
        Long nextCheck = campfireNextCheckTick.get(id);
        if (nextCheck == null || now >= nextCheck) {
            boolean result = isNearCampfire(player, storedConfig.campfireRadius);
            campfireCache.put(id, result);
            campfireNextCheckTick.put(id, now + 40L);
            return result;
        }
        Boolean cached = campfireCache.get(id);
        return cached != null && cached;
    }

    private double getSharedEnvironmentalMultiplier(ServerPlayer player) {
        List<Double> multipliers = new ArrayList<>();

        if (storedConfig.lightLevelBonusEnabled) {
            BlockPos pos = player.blockPosition();
            int blockLight = player.serverLevel().getBrightness(LightLayer.BLOCK, pos);
            double t = Math.max(0.0D, Math.min(1.0D, blockLight / 15.0D));
            double minMultiplier = storedConfig.lightLevelMinMultiplier;
            double maxMultiplier = storedConfig.lightLevelMaxMultiplier;
            multipliers.add(minMultiplier + (maxMultiplier - minMultiplier) * t);
        }

        if (storedConfig.dayNightMultiplierEnabled) {
            multipliers.add(player.serverLevel().isDay() ? storedConfig.dayMultiplier : storedConfig.nightMultiplier);
        }

        if (storedConfig.difficultyScalingEnabled) {
            Difficulty difficulty = player.serverLevel().getDifficulty();
            switch (difficulty) {
                case PEACEFUL -> multipliers.add(storedConfig.peacefulMultiplier);
                case EASY -> multipliers.add(storedConfig.easyMultiplier);
                case HARD -> multipliers.add(storedConfig.hardMultiplier);
                case NORMAL -> multipliers.add(storedConfig.normalMultiplier);
            }
        }

        return storedConfig.combineBonusMultipliers(multipliers);
    }

    private double getCurrentHealIntervalTicks(long outOfCombatTicks) {
        int baseTicks = Math.max(1, storedConfig.baseHealIntervalTicks);
        if (!storedConfig.rampUpEnabled) return baseTicks;

        int fullTicks = Math.max(1, storedConfig.fullStrengthHealIntervalTicks);
        int rampTicks = Math.max(1, storedConfig.rampFullStrengthTicks);
        double progress = Math.min(1.0D, (double)outOfCombatTicks / rampTicks);
        return baseTicks + (fullTicks - baseTicks) * progress;
    }

    private double getTemporaryRateMultiplier(UUID playerId, long now) {
        double multiplier = 1.0D;

        RegenBoost boost = activeBoosts.get(playerId);
        if (boost != null) {
            if (now >= boost.expiresAt) activeBoosts.remove(playerId);
            else multiplier *= boost.multiplier;
        }

        RegenPenalty penalty = activePenalties.get(playerId);
        if (penalty != null) {
            if (now >= penalty.expiresAt) activePenalties.remove(playerId);
            else multiplier *= penalty.multiplier;
        }

        return multiplier;
    }

    private int computeDamageCooldownTicks(ServerPlayer player, DamageSource source, float amount, PassiveRegenConfig config) {
        int cooldownTicks;
        if (config.pvpDamageCooldownTicks >= 0 && source.getEntity() instanceof ServerPlayer) {
            cooldownTicks = config.pvpDamageCooldownTicks;
        } else {
            cooldownTicks = config.getEffectiveDamageCooldown(player.getFoodData().getFoodLevel());
        }

        if (config.largeDamagePenaltyEnabled && player.getMaxHealth() > 0.0F) {
            double thresholdPercent = Math.max(1, config.largeDamageThresholdPercent);
            double damagePercent = amount / player.getMaxHealth() * 100.0D;
            if (damagePercent >= thresholdPercent) {
                cooldownTicks = (int)Math.ceil(cooldownTicks * Math.max(1.0D, config.largeDamageCooldownMultiplier));
            }
        }

        return Math.max(0, cooldownTicks);
    }

    private int updateKillCombo(UUID playerId, long now, PassiveRegenConfig config) {
        if (!config.regenOnKillComboEnabled) return 0;

        long comboWindow = Math.max(20, config.regenOnKillComboWindowTicks);
        int previousStacks = killComboStacks.getOrDefault(playerId, 0);
        Long lastKillTick = lastKillTicks.get(playerId);
        int comboStacks;
        if (lastKillTick != null && now - lastKillTick <= comboWindow) {
            comboStacks = Math.min(Math.max(1, config.regenOnKillComboMaxStacks), previousStacks + 1);
        } else {
            comboStacks = 1;
        }

        lastKillTicks.put(playerId, now);
        killComboStacks.put(playerId, comboStacks);
        return comboStacks;
    }

    private boolean shouldProcessPlayer(ServerPlayer player, PassiveRegenConfig config) {
        if (!player.isAlive()
            || player.isSpectator()
            || player.getAbilities().invulnerable
            || player.getHealth() >= player.getMaxHealth() * (config.maxRegenHealthPercent / 100.0F)
            || hasBlockedEffect(player, config.blockedEffects)
            || isDimensionBlacklisted(player, config.dimensionBlacklist)
            || (!config.regenWhileSprinting && player.isSprinting())) {
            return false;
        }

        if (isHungerOverrideActive(player.getUUID())) {
            return true;
        }

        if (config.hungerPenaltyEnabled) {
            return true;
        }

        int foodLevel = player.getFoodData().getFoodLevel();
        if (foodLevel < config.getMinimumFoodLevel()) {
            return false;
        }
        return config.minimumSaturationLevel <= 0.0D
            || player.getFoodData().getSaturationLevel() >= config.minimumSaturationLevel;
    }

    private boolean isHungerBlocked(ServerPlayer player, int foodLevel, long outOfCombatTicks) {
        if (!player.isAlive() || player.isSpectator() || player.getAbilities().invulnerable) return false;
        if (player.getHealth() >= player.getMaxHealth() * (storedConfig.maxRegenHealthPercent / 100.0F)) return false;
        if (hasBlockedEffect(player, storedConfig.blockedEffects) || isDimensionBlacklisted(player, storedConfig.dimensionBlacklist)) return false;
        if (!storedConfig.regenWhileSprinting && player.isSprinting()) return false;
        if (isHungerOverrideActive(player.getUUID())) return false;
        boolean belowHunger = foodLevel < storedConfig.getMinimumFoodLevel();
        boolean belowSaturation = storedConfig.minimumSaturationLevel > 0.0D
            && player.getFoodData().getSaturationLevel() < storedConfig.minimumSaturationLevel;
        return belowHunger || belowSaturation;
    }

    private boolean isHungerOverrideActive(UUID playerId) {
        Long expiresAt = hungerOverrides.get(playerId);
        if (expiresAt == null) return false;
        if (getCurrentTick(playerId) >= expiresAt) {
            hungerOverrides.remove(playerId);
            return false;
        }
        return true;
    }

    private static boolean hasBlockedEffect(ServerPlayer player, String[] blockedEffects) {
        if (blockedEffects == null || blockedEffects.length == 0) return false;
        for (MobEffectInstance effect : player.getActiveEffects()) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            if (id == null) continue;
            String idString = id.toString();
            for (String blockedEffect : blockedEffects) {
                if (blockedEffect != null && blockedEffect.equalsIgnoreCase(idString)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isDimensionBlacklisted(ServerPlayer player, String[] dimensionBlacklist) {
        if (dimensionBlacklist == null || dimensionBlacklist.length == 0) return false;
        String dimensionId = player.serverLevel().dimension().location().toString();
        for (String entry : dimensionBlacklist) {
            if (entry != null && entry.equals(dimensionId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isKillIgnored(LivingEntity victim, PassiveRegenConfig config) {
        if (victim == null) return true;
        if (config.regenOnKillHostileOnly && !(victim instanceof Enemy)) return true;
        String[] blacklist = config.regenOnKillBlacklist;
        if (blacklist == null || blacklist.length == 0) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        if (id == null) return false;
        String idString = id.toString();
        for (String entry : blacklist) {
            if (entry != null && entry.equalsIgnoreCase(idString)) {
                return true;
            }
        }
        return false;
    }

    private long getOutOfCombatTicks(UUID playerId, long now) {
        Long lastDamageTick = lastDamageTicks.get(playerId);
        return lastDamageTick == null ? 0L : Math.max(0L, now - lastDamageTick);
    }

    private int getCooldownDuration(UUID playerId, ServerPlayer player) {
        Integer stored = cooldownDurations.get(playerId);
        if (stored != null) return Math.max(0, stored);
        if (player == null || storedConfig == null) return 0;
        return storedConfig.getEffectiveDamageCooldown(player.getFoodData().getFoodLevel());
    }

    private long getCurrentTick(UUID playerUUID) {
        ServerPlayer player = resolvePlayer(playerUUID);
        if (player != null) return player.serverLevel().getGameTime();
        return serverTick;
    }

    private void syncPersistedCooldownState(ServerPlayer player, Long lastDamageTick, long outOfCombatTicks) {
    }

    private void restoreCooldownStateAfterReconnect(ServerPlayer player) {
        UUID playerId = player.getUUID();
        CooldownPersistenceStore.SavedCooldown saved = CooldownPersistenceStore.load(player.server, playerId);
        if (saved == null) {
            lastDamageTicks.remove(playerId);
            cooldownDurations.remove(playerId);
            return;
        }

        long savedOutOfCombatTicks = Math.max(0L, saved.outOfCombatTicks());
        int savedCooldownDuration = Math.max(0, saved.cooldownDurationTicks());
        lastDamageTicks.put(playerId, player.serverLevel().getGameTime() - savedOutOfCombatTicks);
        cooldownDurations.put(playerId, savedCooldownDuration);
    }

    private void clearSavedCooldownState(ServerPlayer player) {
        CooldownPersistenceStore.clear(player.server, player.getUUID());
    }

    private void setSavedCooldownState(ServerPlayer player, long outOfCombatTicks, int cooldownDuration) {
        CooldownPersistenceStore.save(player.server, player.getUUID(), Math.max(0L, outOfCombatTicks), Math.max(0, cooldownDuration));
    }

    private void syncHudState(ServerPlayer player, long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, int maxRegenHealthPercent) {
        UUID playerId = player.getUUID();
        boolean nearCampfire = false;
        if (storedConfig != null && storedConfig.campfireRegenEnabled) {
            long now = player.serverLevel().getGameTime();
            Long nextCheck = campfireNextCheckTick.get(playerId);
            if (nextCheck == null || now >= nextCheck) {
                nearCampfire = isNearCampfire(player, storedConfig.campfireRadius);
                campfireCache.put(playerId, nearCampfire);
                campfireNextCheckTick.put(playerId, now + 40L);
            } else {
                Boolean cached = campfireCache.get(playerId);
                nearCampfire = cached != null && cached;
            }
        }
        boolean saturationBonus = isSaturationBonusActive(player);
        boolean poisoned = player.hasEffect(MobEffects.POISON);
        boolean withered = player.hasEffect(MobEffects.WITHER);
        HudSyncState current = new HudSyncState(outOfCombatTicks, damageCooldownTicks, regenActive, hungerBlocked, nearCampfire, player.getHealth(), player.getMaxHealth(), maxRegenHealthPercent, saturationBonus, poisoned, withered);
        HudSyncState previous = lastHudStates.get(playerId);
        if (justHealed || !current.equals(previous)) {
            RegenHudPacket.send(player, outOfCombatTicks, damageCooldownTicks, regenActive, hungerBlocked, justHealed, player.getHealth(), player.getMaxHealth(), maxRegenHealthPercent, nearCampfire, saturationBonus, poisoned, withered);
            lastHudStates.put(playerId, current);
        }
    }

    private static boolean isNearCampfire(ServerPlayer player, int radius) {
        BlockPos playerPos = player.blockPosition();
        BlockPos.MutableBlockPos mut = new BlockPos.MutableBlockPos();
        int rsq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > rsq) continue;
                for (int dy = -2; dy <= 4; dy++) {
                    mut.set(playerPos.getX() + dx, playerPos.getY() + dy, playerPos.getZ() + dz);
                    BlockState bs = player.serverLevel().getBlockState(mut);
                    if ((bs.is(Blocks.CAMPFIRE) || bs.is(Blocks.SOUL_CAMPFIRE))
                            && bs.getValue(CampfireBlock.LIT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ServerPlayer resolvePlayer(UUID playerUUID) {
        return currentServer != null ? currentServer.getPlayerList().getPlayer(playerUUID) : null;
    }

    private double getMaxHealthScaleMultiplier(float maxHealth) {
        if (!storedConfig.scaleWithMaxHealth || maxHealth <= 20.0F) return 1.0D;

        double normalized = Math.max(1.0D, maxHealth / 20.0D);
        double exponent = Math.max(0.1D, storedConfig.maxHealthScalingExponent);
        double multiplier = Math.pow(normalized, exponent);
        double cap = Math.max(1.0D, storedConfig.maxHealthScalingCap);
        return Math.min(cap, multiplier);
    }

    private record RegenBoost(double multiplier, long expiresAt) {
    }

    private record RegenPenalty(double multiplier, long expiresAt) {
    }

    private record HudSyncState(long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean nearCampfire, float health, float maxHealth, int maxRegenHealthPercent, boolean saturationBonus, boolean poisoned, boolean withered) {
    }
}
