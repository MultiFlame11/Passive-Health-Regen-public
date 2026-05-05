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
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PassiveRegenHandler implements IPassiveRegenInternals {
    private static final String SAVED_ROOT_TAG = PassiveRegenMod.MODID + "Cooldown";
    private static final String SAVED_OUT_OF_COMBAT_TAG = "outOfCombatTicks";
    private static final String SAVED_DURATION_TAG = "cooldownDurationTicks";

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

    private MinecraftServer currentServer;
    static volatile long serverTick = 0L;

    public PassiveRegenHandler() {
        PassiveRegenAPI.register(this);
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
        if (lastDamageTick == null) {
            return;
        }

        ServerPlayer player = resolvePlayer(playerUUID);
        int cooldownDuration = getCooldownDuration(playerUUID, player);
        long now = getCurrentTick(playerUUID);
        long remaining = lastDamageTick + cooldownDuration - now;
        if (remaining <= 0L) {
            return;
        }

        int reduction = Math.max(0, Math.min(100, percentReduction));
        long cut = (long) (remaining * (reduction / 100.0D));
        long adjustedLastDamageTick = lastDamageTick - cut;
        lastDamageTicks.put(playerUUID, adjustedLastDamageTick);
        if (player != null) {
            long outOfCombatTicks = Math.max(0L, player.serverLevel().getGameTime() - adjustedLastDamageTick);
            setSavedCooldownState(player, outOfCombatTicks, cooldownDuration);
        }
    }

    @Override
    public boolean isRegenReady(UUID playerUUID) {
        ServerPlayer player = resolvePlayer(playerUUID);
        if (player == null || !PassiveRegenConfig.ENABLED.get()) {
            return false;
        }

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.serverLevel().getGameTime());
        int cooldownDuration = getCooldownDuration(playerUUID, player);
        return outOfCombatTicks >= cooldownDuration && computeHealAmount(player, outOfCombatTicks) > 0.0F;
    }

    @Override
    public boolean isHungerBlocked(UUID playerUUID) {
        ServerPlayer player = resolvePlayer(playerUUID);
        if (player == null || !PassiveRegenConfig.ENABLED.get()) {
            return false;
        }

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.serverLevel().getGameTime());
        return isHungerBlocked(player, player.getFoodData().getFoodLevel(), outOfCombatTicks);
    }

    @Override
    public int getRemainingCooldownTicks(UUID playerUUID) {
        ServerPlayer player = resolvePlayer(playerUUID);
        if (player == null || !PassiveRegenConfig.ENABLED.get()) {
            return 0;
        }

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.serverLevel().getGameTime());
        int cooldownDuration = getCooldownDuration(playerUUID, player);
        return Math.max(0, cooldownDuration - (int) Math.min(Integer.MAX_VALUE, outOfCombatTicks));
    }

    @Override
    public float getCurrentHealRate(UUID playerUUID) {
        ServerPlayer player = resolvePlayer(playerUUID);
        if (player == null || !PassiveRegenConfig.ENABLED.get()) {
            return 0.0F;
        }

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

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!PassiveRegenConfig.ENABLED.get() || event.getAmount() <= 0.0F) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide) {
            return;
        }

        currentServer = player.server;
        long now = player.serverLevel().getGameTime();
        serverTick = now;

        int cooldownTicks = computeDamageCooldownTicks(player, event.getSource(), event.getAmount());
        UUID playerId = player.getUUID();
        lastDamageTicks.put(playerId, now);
        cooldownDurations.put(playerId, cooldownTicks);
        frozenMaxDuringCooldown.remove(playerId);
        campfireCooldownApplied.remove(playerId);
        setSavedCooldownState(player, 0L, cooldownTicks);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player rawPlayer = event.player;
        if (event.phase != TickEvent.Phase.END || rawPlayer.level().isClientSide || !(rawPlayer instanceof ServerPlayer player)) {
            return;
        }

        currentServer = player.server;
        long now = player.serverLevel().getGameTime();
        serverTick = now;

        if (!PassiveRegenConfig.ENABLED.get()) {
            return;
        }
        syncNaturalRegenGameRule(player.server);

        UUID playerId = player.getUUID();
        boolean justHealed = false;
        Long lastDamageTick = lastDamageTicks.get(playerId);
        if (lastDamageTick == null) {
            syncHudState(player, 0L, 0, false, false, false, PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get());
            return;
        }

        long outOfCombatTicks = Math.max(0L, now - lastDamageTick);
        int cooldownDuration = getCooldownDuration(playerId, player);
        boolean hungerBlocked = isHungerBlocked(player, player.getFoodData().getFoodLevel(), outOfCombatTicks);

        if (!shouldProcessPlayer(player)) {
            applyHungerDrainIdle(player, playerId, outOfCombatTicks, cooldownDuration, hungerBlocked);
            syncHudState(player, outOfCombatTicks, cooldownDuration, false, hungerBlocked, false, PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get());
            return;
        }

        int updateTicks = Math.max(1, PassiveRegenConfig.UPDATE_INTERVAL_TICKS.get());
        if ((now + player.getId()) % updateTicks != 0L) {
            HudSyncState previousHudState = lastHudStates.get(playerId);
            boolean keepActivePulse = outOfCombatTicks >= cooldownDuration
                && previousHudState != null
                && previousHudState.regenActive();
            syncHudState(player, outOfCombatTicks, cooldownDuration, keepActivePulse, hungerBlocked, false, PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get());
            return;
        }

        if (outOfCombatTicks < cooldownDuration
                && PassiveRegenConfig.CAMPFIRE_REGEN_ENABLED.get()
                && PassiveRegenConfig.CAMPFIRE_COOLDOWN_REDUCTION_ENABLED.get()
                && PassiveRegenConfig.CAMPFIRE_COOLDOWN_REDUCTION_PERCENT.get() > 0
                && !campfireCooldownApplied.contains(playerId)
                && isNearCampfireCached(player)) {
            campfireCooldownApplied.add(playerId);
            reduceCooldown(playerId, PassiveRegenConfig.CAMPFIRE_COOLDOWN_REDUCTION_PERCENT.get());
            outOfCombatTicks = getOutOfCombatTicks(playerId, now);
            cooldownDuration = getCooldownDuration(playerId, player);
        }

        if (outOfCombatTicks < cooldownDuration) {
            syncHudState(player, outOfCombatTicks, cooldownDuration, false, false, false, PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get());
            return;
        }

        float healAmount = computeHealAmount(player, outOfCombatTicks);
        boolean regenActive = healAmount > 0.0F;
        if (healAmount > 0.0F) {
            float beforeHp = player.getHealth();
            player.heal(healAmount);
            float actualHealed = Math.max(0.0F, player.getHealth() - beforeHp);

            if (actualHealed > 0.0F
                    && PassiveRegenConfig.SATURATION_BONUS_ENABLED.get()
                    && !hungerBlocked
                    && PassiveRegenConfig.SATURATION_BONUS_COST_PER_HP.get() > 0.0D
                    && (PassiveRegenConfig.SATURATION_BONUS_HEAL_MULTIPLIER.get() > 1.0D
                        || PassiveRegenConfig.SATURATION_BONUS_SPEED_MULTIPLIER.get() > 1.0D
                        || PassiveRegenConfig.SATURATION_BONUS_FLAT_HEAL_BONUS.get() > 0.0D)
                    && saturationBonusActive.contains(playerId)) {
                float currentSat = player.getFoodData().getSaturationLevel();
                float floor = PassiveRegenConfig.SATURATION_BONUS_MIN_SATURATION_FLOOR.get().floatValue();
                float headroom = Math.max(0.0F, currentSat - floor);
                float rawCost = actualHealed * PassiveRegenConfig.SATURATION_BONUS_COST_PER_HP.get().floatValue();
                float chargedCost = Math.min(rawCost, headroom);
                if (chargedCost > 0.0F) {
                    player.causeFoodExhaustion(chargedCost * 4.0F);
                }
            }

            if (actualHealed > 0.0F) {
                applyHungerDrainHealCost(player, playerId, actualHealed);
            }

            justHealed = actualHealed > 0.0F;
            regenActive = true;
        }

        if (PassiveRegenConfig.SATURATION_BONUS_ENABLED.get()
                && !hungerBlocked
                && PassiveRegenConfig.SATURATION_BONUS_IDLE_DRAIN_PER_TICK.get() > 0.0D
                && (PassiveRegenConfig.SATURATION_BONUS_HEAL_MULTIPLIER.get() > 1.0D
                    || PassiveRegenConfig.SATURATION_BONUS_SPEED_MULTIPLIER.get() > 1.0D
                        || PassiveRegenConfig.SATURATION_BONUS_FLAT_HEAL_BONUS.get() > 0.0D)
                    && saturationBonusActive.contains(playerId)) {
            float currentSat = player.getFoodData().getSaturationLevel();
            float floor = PassiveRegenConfig.SATURATION_BONUS_MIN_SATURATION_FLOOR.get().floatValue();
            float headroom = Math.max(0.0F, currentSat - floor);
            float idleCost = Math.min(PassiveRegenConfig.SATURATION_BONUS_IDLE_DRAIN_PER_TICK.get().floatValue(), headroom);
            if (idleCost > 0.0F) {
                player.causeFoodExhaustion(idleCost * 4.0F);
            }
        }

        syncHudState(player, outOfCombatTicks, cooldownDuration, regenActive, hungerBlocked, justHealed, PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get());
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!PassiveRegenConfig.ENABLED.get() || !PassiveRegenConfig.REGEN_ON_KILL_ENABLED.get()) {
            return;
        }

        if (!(event.getSource().getEntity() instanceof ServerPlayer killer) || killer.level().isClientSide) {
            return;
        }

        currentServer = killer.server;
        if (isKillIgnored(event.getEntity())) {
            return;
        }

        UUID killerId = killer.getUUID();
        int comboStacks = updateKillCombo(killerId, killer.serverLevel().getGameTime());
        int totalReduction = Math.max(0, Math.min(100,
            PassiveRegenConfig.REGEN_ON_KILL_COOLDOWN_REDUCTION.get()
                + comboStacks * Math.max(0, PassiveRegenConfig.REGEN_ON_KILL_COMBO_REDUCTION_PER_STACK.get())));
        reduceCooldown(killerId, totalReduction);
    }
    private static void syncNaturalRegenGameRule(MinecraftServer server) {
        if (server == null || !PassiveRegenConfig.ENABLED.get() || !PassiveRegenConfig.DISABLE_NATURAL_REGEN.get()) {
            return;
        }
        GameRules.BooleanValue rule = server.getGameRules().getRule(GameRules.RULE_NATURAL_REGENERATION);
        if (rule.get()) {
            rule.set(false, server);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        currentServer = player.server;
        UUID playerId = player.getUUID();
        activeBoosts.remove(playerId);
        activePenalties.remove(playerId);
        hungerOverrides.remove(playerId);
        lastHudStates.remove(playerId);
        lastKillTicks.remove(playerId);
        killComboStacks.remove(playerId);
        hungerDrainRemainders.remove(playerId);
        restoreCooldownStateAfterReconnect(player);

        Long restoredLastDamageTick = lastDamageTicks.get(playerId);
        if (restoredLastDamageTick == null) {
            syncHudState(player, 0L, 0, false, false, false, PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get());
            return;
        }

        long outOfCombatTicks = getOutOfCombatTicks(playerId, player.serverLevel().getGameTime());
        int cooldownDuration = getCooldownDuration(playerId, player);
        boolean hungerBlocked = isHungerBlocked(player, player.getFoodData().getFoodLevel(), outOfCombatTicks);
        syncHudState(player, outOfCombatTicks, cooldownDuration, false, hungerBlocked, false, PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get());
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

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

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        currentServer = player.server;
        UUID playerId = player.getUUID();
        lastDamageTicks.remove(playerId);
        cooldownDurations.remove(playerId);
        activeBoosts.remove(playerId);
        activePenalties.remove(playerId);
        hungerOverrides.remove(playerId);
        lastHudStates.remove(playerId);
        lastKillTicks.remove(playerId);
        killComboStacks.remove(playerId);
        campfireCache.remove(playerId);
        campfireNextCheckTick.remove(playerId);
        frozenMaxDuringCooldown.remove(playerId);
        campfireCooldownApplied.remove(playerId);
        saturationBonusActive.remove(playerId);
        hungerDrainRemainders.remove(playerId);
        clearSavedCooldownState(player);
        syncHudState(player, 0L, 0, false, false, false, PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get());
    }

    private float computeHealAmount(ServerPlayer player, long outOfCombatTicks) {
        if (!PassiveRegenConfig.ENABLED.get() || !shouldProcessPlayer(player)) {
            return 0.0F;
        }

        UUID playerId = player.getUUID();
        int foodLevel = player.getFoodData().getFoodLevel();
        float freezeProgress = getFreezingProgress(player);

        int cooldownDuration = getCooldownDuration(playerId, player);
        if (freezeProgress > 0.0F && outOfCombatTicks < cooldownDuration) {
            Float previous = frozenMaxDuringCooldown.get(playerId);
            if (previous == null || freezeProgress > previous) {
                frozenMaxDuringCooldown.put(playerId, freezeProgress);
            }
        }

        Float stickyProgress = frozenMaxDuringCooldown.get(playerId);
        if (stickyProgress != null
                && PassiveRegenConfig.FREEZING_PENALTY_ENABLED.get()
                && PassiveRegenConfig.FREEZING_COOLDOWN_MULTIPLIER.get() > 1.0D) {
            double cooldownMultiplier = 1.0D
                + (PassiveRegenConfig.FREEZING_COOLDOWN_MULTIPLIER.get() - 1.0D) * stickyProgress;
            cooldownDuration = (int) Math.ceil(cooldownDuration * cooldownMultiplier);
        }

        if (outOfCombatTicks < cooldownDuration) {
            return 0.0F;
        }
        frozenMaxDuringCooldown.remove(playerId);

        boolean hungerPenalized = !isHungerOverrideActive(playerId)
            && PassiveRegenConfig.HUNGER_PENALTY_ENABLED.get()
            && (foodLevel < PassiveRegenConfig.getMinimumFoodLevel()
                || (PassiveRegenConfig.MINIMUM_SATURATION_LEVEL.get() > 0.0D
                    && player.getFoodData().getSaturationLevel() < PassiveRegenConfig.MINIMUM_SATURATION_LEVEL.get()));

        if (freezeProgress >= 1.0F && PassiveRegenConfig.FREEZING_BLOCKS_REGEN.get()) {
            return 0.0F;
        }
        if (PassiveRegenConfig.DISABLE_HEALING_DURING_WITHER.get() && player.hasEffect(MobEffects.WITHER)) {
            return 0.0F;
        }
        if (PassiveRegenConfig.DISABLE_HEALING_DURING_POISON.get() && player.hasEffect(MobEffects.POISON)) {
            return 0.0F;
        }

        double baseHeal = Math.max(0.01D, PassiveRegenConfig.HEAL_AMOUNT_PER_TRIGGER.get());
        double scaledHeal = baseHeal * PassiveRegenConfig.getMaxHealthScaleMultiplier(player.getMaxHealth());
        double healBonusMultiplier = PassiveRegenConfig.combineBonusMultipliers(getHealBonusMultipliers(player, foodLevel, hungerPenalized));
        double speedBonusMultiplier = PassiveRegenConfig.combineBonusMultipliers(getSpeedBonusMultipliers(player, foodLevel, hungerPenalized));

        if (freezeProgress > 0.0F && PassiveRegenConfig.FREEZING_PENALTY_ENABLED.get()) {
            double healMultiplier = 1.0D
                + (Math.max(0.01D, PassiveRegenConfig.FREEZING_HEAL_MULTIPLIER.get()) - 1.0D) * freezeProgress;
            double speedMultiplier = 1.0D
                + (Math.max(0.01D, PassiveRegenConfig.FREEZING_SPEED_MULTIPLIER.get()) - 1.0D) * freezeProgress;
            healBonusMultiplier *= Math.max(0.01D, healMultiplier);
            speedBonusMultiplier *= Math.max(0.01D, speedMultiplier);
        }

        double perTriggerHeal = scaledHeal * healBonusMultiplier;
        if (PassiveRegenConfig.SATURATION_BONUS_FLAT_HEAL_BONUS.get() > 0.0D) {
            double saturationStrength = hungerPenalized ? 0.0D : getSaturationBonusStrength(player);
            if (saturationStrength > 0.0D) {
                perTriggerHeal += PassiveRegenConfig.SATURATION_BONUS_FLAT_HEAL_BONUS.get() * saturationStrength;
            }
        }

        long rampUpTicks = Math.max(0L, outOfCombatTicks - cooldownDuration);
        int updateTicks = Math.max(1, PassiveRegenConfig.UPDATE_INTERVAL_TICKS.get());
        double currentHealInterval = PassiveRegenConfig.getCurrentHealIntervalTicks(rampUpTicks) / Math.max(0.0001D, speedBonusMultiplier);
        double finalHeal = perTriggerHeal * updateTicks / currentHealInterval;
        finalHeal *= getTemporaryRateMultiplier(playerId, player.serverLevel().getGameTime());
        return (float) Math.max(0.0D, finalHeal);
    }

    private double getSaturationBonusStrength(ServerPlayer player) {
        if (!isSaturationBonusActive(player)) {
            return 0.0D;
        }
        if (!PassiveRegenConfig.SATURATION_BONUS_SCALE_BY_EXCESS.get()) {
            return 1.0D;
        }
        double saturation = player.getFoodData().getSaturationLevel();
        double threshold = PassiveRegenConfig.SATURATION_BONUS_THRESHOLD.get();
        double range = Math.max(0.01D, 20.0D - threshold);
        return Math.max(0.0D, Math.min(1.0D, (saturation - threshold) / range));
    }

    private void applyHungerDrainHealCost(ServerPlayer player, UUID playerId, float actualHealed) {
        if (!PassiveRegenConfig.HUNGER_DRAIN_ENABLED.get()
                || actualHealed <= 0.0F
                || PassiveRegenConfig.HUNGER_DRAIN_COST_PER_HP.get() <= 0.0D
                || PassiveRegenConfig.HUNGER_DRAIN_SPEED_MULTIPLIER.get() <= 0.0D) {
            return;
        }

        double rawDrain = actualHealed
            * PassiveRegenConfig.HUNGER_DRAIN_COST_PER_HP.get()
            * PassiveRegenConfig.HUNGER_DRAIN_SPEED_MULTIPLIER.get();
        applyHungerDrain(player, playerId, rawDrain);
    }

    private void applyHungerDrainIdle(ServerPlayer player, UUID playerId, long outOfCombatTicks, int cooldownDuration, boolean hungerBlocked) {
        if (!PassiveRegenConfig.HUNGER_DRAIN_ENABLED.get()
                || PassiveRegenConfig.HUNGER_DRAIN_IDLE_DRAIN_PER_TICK.get() <= 0.0D
                || PassiveRegenConfig.HUNGER_DRAIN_SPEED_MULTIPLIER.get() <= 0.0D
                || hungerBlocked
                || outOfCombatTicks < cooldownDuration
                || !player.isAlive()
                || player.isSpectator()
                || player.getAbilities().invulnerable
                || hasBlockedEffect(player, PassiveRegenConfig.BLOCKED_EFFECTS.get())
                || isDimensionBlacklisted(player, PassiveRegenConfig.DIMENSION_BLACKLIST.get())
                || (!PassiveRegenConfig.REGEN_WHILE_SPRINTING.get() && player.isSprinting())
                || player.getHealth() < player.getMaxHealth() * (PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get() / 100.0F)) {
            return;
        }

        double rawDrain = PassiveRegenConfig.HUNGER_DRAIN_IDLE_DRAIN_PER_TICK.get()
            * PassiveRegenConfig.HUNGER_DRAIN_SPEED_MULTIPLIER.get();
        applyHungerDrain(player, playerId, rawDrain);
    }

    private void applyHungerDrain(ServerPlayer player, UUID playerId, double rawDrain) {
        if (rawDrain <= 0.0D) {
            return;
        }

        int floorFood = (int) Math.ceil(Math.max(0.0D, Math.min(20.0D, PassiveRegenConfig.HUNGER_DRAIN_MIN_FLOOR.get())));
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
        if (!PassiveRegenConfig.SATURATION_BONUS_ENABLED.get()) {
            saturationBonusActive.remove(player.getUUID());
            return false;
        }

        float saturation = player.getFoodData().getSaturationLevel();
        UUID playerId = player.getUUID();
        boolean wasActive = saturationBonusActive.contains(playerId);
        boolean active;
        if (saturation >= PassiveRegenConfig.SATURATION_BONUS_THRESHOLD.get().floatValue()) {
            active = true;
        } else if (wasActive && saturation >= PassiveRegenConfig.SATURATION_BONUS_DEACTIVATE_THRESHOLD.get().floatValue()) {
            active = true;
        } else {
            active = false;
        }

        if (active) {
            saturationBonusActive.add(playerId);
        } else {
            saturationBonusActive.remove(playerId);
        }
        return active;
    }

    private List<Double> getHealBonusMultipliers(ServerPlayer player, int foodLevel, boolean hungerPenalized) {
        List<Double> multipliers = new ArrayList<>();

        if (hungerPenalized) {
            multipliers.add(Math.max(0.01D, PassiveRegenConfig.HUNGER_PENALTY_HEAL_MULTIPLIER.get()));
        } else {
            double hungerHeal = PassiveRegenConfig.getHungerHealMultiplier(foodLevel);
            if (hungerHeal != 1.0D) {
                multipliers.add(hungerHeal);
            }
        }

        if (!hungerPenalized) {
            double saturationStrength = getSaturationBonusStrength(player);
            if (saturationStrength > 0.0D && PassiveRegenConfig.SATURATION_BONUS_HEAL_MULTIPLIER.get() != 1.0D) {
                double rawMultiplier = Math.max(1.0D, PassiveRegenConfig.SATURATION_BONUS_HEAL_MULTIPLIER.get());
                multipliers.add(1.0D + (rawMultiplier - 1.0D) * saturationStrength);
            }
        }

        if (PassiveRegenConfig.CROUCH_BONUS_ENABLED.get()
                && player.isShiftKeyDown()
                && PassiveRegenConfig.CROUCH_HEAL_MULTIPLIER.get() != 1.0D) {
            multipliers.add(Math.max(1.0D, PassiveRegenConfig.CROUCH_HEAL_MULTIPLIER.get()));
        }

        if (PassiveRegenConfig.CAMPFIRE_REGEN_ENABLED.get()
                && PassiveRegenConfig.CAMPFIRE_HEAL_MULTIPLIER.get() != 1.0D
                && isNearCampfireCached(player)) {
            multipliers.add(Math.max(1.0D, PassiveRegenConfig.CAMPFIRE_HEAL_MULTIPLIER.get()));
        }

        double sharedMultiplier = getSharedEnvironmentalMultiplier(player);
        if (sharedMultiplier != 1.0D) {
            multipliers.add(sharedMultiplier);
        }

        return multipliers;
    }

    private List<Double> getSpeedBonusMultipliers(ServerPlayer player, int foodLevel, boolean hungerPenalized) {
        List<Double> multipliers = new ArrayList<>();

        if (hungerPenalized) {
            multipliers.add(Math.max(0.01D, PassiveRegenConfig.HUNGER_PENALTY_SPEED_MULTIPLIER.get()));
        } else {
            double hungerSpeed = PassiveRegenConfig.getHungerSpeedMultiplier(foodLevel);
            if (hungerSpeed != 1.0D) {
                multipliers.add(hungerSpeed);
            }
        }

        if (!hungerPenalized) {
            double saturationStrength = getSaturationBonusStrength(player);
            if (saturationStrength > 0.0D && PassiveRegenConfig.SATURATION_BONUS_SPEED_MULTIPLIER.get() != 1.0D) {
                double rawMultiplier = Math.max(1.0D, PassiveRegenConfig.SATURATION_BONUS_SPEED_MULTIPLIER.get());
                multipliers.add(1.0D + (rawMultiplier - 1.0D) * saturationStrength);
            }
        }

        if (PassiveRegenConfig.CROUCH_BONUS_ENABLED.get()
                && player.isShiftKeyDown()
                && PassiveRegenConfig.CROUCH_SPEED_MULTIPLIER.get() != 1.0D) {
            multipliers.add(Math.max(1.0D, PassiveRegenConfig.CROUCH_SPEED_MULTIPLIER.get()));
        }

        if (PassiveRegenConfig.CAMPFIRE_REGEN_ENABLED.get()
                && PassiveRegenConfig.CAMPFIRE_SPEED_MULTIPLIER.get() != 1.0D
                && isNearCampfireCached(player)) {
            multipliers.add(Math.max(1.0D, PassiveRegenConfig.CAMPFIRE_SPEED_MULTIPLIER.get()));
        }

        double sharedMultiplier = getSharedEnvironmentalMultiplier(player);
        if (sharedMultiplier != 1.0D) {
            multipliers.add(sharedMultiplier);
        }

        return multipliers;
    }

    private float getFreezingProgress(ServerPlayer player) {
        if (!PassiveRegenConfig.FREEZING_PENALTY_ENABLED.get()) {
            return 0.0F;
        }
        int required = player.getTicksRequiredToFreeze();
        if (required <= 0) {
            return 0.0F;
        }
        float frozenPercent = (float) player.getTicksFrozen() / (float) required;
        float threshold = PassiveRegenConfig.FREEZING_PENALTY_THRESHOLD_PERCENT.get().floatValue();
        if (frozenPercent <= threshold) {
            return 0.0F;
        }
        if (threshold >= 1.0F) {
            return frozenPercent >= 1.0F ? 1.0F : 0.0F;
        }
        float progress = (frozenPercent - threshold) / (1.0F - threshold);
        return Math.max(0.0F, Math.min(1.0F, progress));
    }

    private boolean isNearCampfireCached(ServerPlayer player) {
        UUID playerId = player.getUUID();
        long now = player.serverLevel().getGameTime();
        Long nextCheck = campfireNextCheckTick.get(playerId);
        if (nextCheck == null || now >= nextCheck) {
            boolean result = isNearCampfire(player, PassiveRegenConfig.CAMPFIRE_RADIUS.get());
            campfireCache.put(playerId, result);
            campfireNextCheckTick.put(playerId, now + 40L);
            return result;
        }
        Boolean cached = campfireCache.get(playerId);
        return cached != null && cached;
    }

    private double getSharedEnvironmentalMultiplier(ServerPlayer player) {
        List<Double> multipliers = new ArrayList<>();

        if (PassiveRegenConfig.LIGHT_LEVEL_BONUS_ENABLED.get()) {
            int blockLight = player.serverLevel().getBrightness(LightLayer.BLOCK, player.blockPosition());
            double t = Math.max(0.0D, Math.min(1.0D, blockLight / 15.0D));
            double minMultiplier = PassiveRegenConfig.LIGHT_LEVEL_MIN_MULTIPLIER.get();
            double maxMultiplier = PassiveRegenConfig.LIGHT_LEVEL_MAX_MULTIPLIER.get();
            multipliers.add(minMultiplier + (maxMultiplier - minMultiplier) * t);
        }

        if (PassiveRegenConfig.DAY_NIGHT_MULTIPLIER_ENABLED.get()) {
            multipliers.add(player.serverLevel().isDay()
                ? PassiveRegenConfig.DAY_MULTIPLIER.get()
                : PassiveRegenConfig.NIGHT_MULTIPLIER.get());
        }

        if (PassiveRegenConfig.DIFFICULTY_SCALING_ENABLED.get()) {
            Difficulty difficulty = player.serverLevel().getDifficulty();
            switch (difficulty) {
                case PEACEFUL -> multipliers.add(PassiveRegenConfig.PEACEFUL_MULTIPLIER.get());
                case EASY -> multipliers.add(PassiveRegenConfig.EASY_MULTIPLIER.get());
                case NORMAL -> multipliers.add(PassiveRegenConfig.NORMAL_MULTIPLIER.get());
                case HARD -> multipliers.add(PassiveRegenConfig.HARD_MULTIPLIER.get());
            }
        }

        return PassiveRegenConfig.combineBonusMultipliers(multipliers);
    }

    private double getTemporaryRateMultiplier(UUID playerId, long now) {
        double multiplier = 1.0D;

        RegenBoost boost = activeBoosts.get(playerId);
        if (boost != null) {
            if (now >= boost.expiresAt) {
                activeBoosts.remove(playerId);
            } else {
                multiplier *= boost.multiplier;
            }
        }

        RegenPenalty penalty = activePenalties.get(playerId);
        if (penalty != null) {
            if (now >= penalty.expiresAt) {
                activePenalties.remove(playerId);
            } else {
                multiplier *= penalty.multiplier;
            }
        }

        return multiplier;
    }

    private int computeDamageCooldownTicks(ServerPlayer player, DamageSource source, float amount) {
        int cooldownTicks;
        if (PassiveRegenConfig.PVP_DAMAGE_COOLDOWN_TICKS.get() >= 0 && source.getEntity() instanceof ServerPlayer) {
            cooldownTicks = PassiveRegenConfig.PVP_DAMAGE_COOLDOWN_TICKS.get();
        } else {
            cooldownTicks = PassiveRegenConfig.getEffectiveDamageCooldown(player.getFoodData().getFoodLevel());
        }

        if (PassiveRegenConfig.LARGE_DAMAGE_PENALTY_ENABLED.get() && player.getMaxHealth() > 0.0F) {
            double damagePercent = amount / player.getMaxHealth() * 100.0D;
            if (damagePercent >= Math.max(1, PassiveRegenConfig.LARGE_DAMAGE_THRESHOLD_PERCENT.get())) {
                cooldownTicks = (int) Math.ceil(cooldownTicks * Math.max(1.0D, PassiveRegenConfig.LARGE_DAMAGE_COOLDOWN_MULTIPLIER.get()));
            }
        }

        return Math.max(0, cooldownTicks);
    }

    private int updateKillCombo(UUID playerId, long now) {
        if (!PassiveRegenConfig.REGEN_ON_KILL_COMBO_ENABLED.get()) {
            return 0;
        }

        long comboWindow = Math.max(20, PassiveRegenConfig.REGEN_ON_KILL_COMBO_WINDOW_TICKS.get());
        int previousStacks = killComboStacks.getOrDefault(playerId, 0);
        Long lastKillTick = lastKillTicks.get(playerId);
        int comboStacks;
        if (lastKillTick != null && now - lastKillTick <= comboWindow) {
            comboStacks = Math.min(Math.max(1, PassiveRegenConfig.REGEN_ON_KILL_COMBO_MAX_STACKS.get()), previousStacks + 1);
        } else {
            comboStacks = 1;
        }

        lastKillTicks.put(playerId, now);
        killComboStacks.put(playerId, comboStacks);
        return comboStacks;
    }

    private boolean shouldProcessPlayer(ServerPlayer player) {
        if (!player.isAlive()
                || player.isSpectator()
                || player.getAbilities().invulnerable
                || player.getHealth() >= player.getMaxHealth() * (PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get() / 100.0F)
                || hasBlockedEffect(player, PassiveRegenConfig.BLOCKED_EFFECTS.get())
                || isDimensionBlacklisted(player, PassiveRegenConfig.DIMENSION_BLACKLIST.get())
                || (!PassiveRegenConfig.REGEN_WHILE_SPRINTING.get() && player.isSprinting())) {
            return false;
        }

        if (isHungerOverrideActive(player.getUUID())) {
            return true;
        }

        if (PassiveRegenConfig.HUNGER_PENALTY_ENABLED.get()) {
            return true;
        }

        if (player.getFoodData().getFoodLevel() < PassiveRegenConfig.getMinimumFoodLevel()) {
            return false;
        }

        return PassiveRegenConfig.MINIMUM_SATURATION_LEVEL.get() <= 0.0D
            || player.getFoodData().getSaturationLevel() >= PassiveRegenConfig.MINIMUM_SATURATION_LEVEL.get();
    }

    private boolean isHungerBlocked(ServerPlayer player, int foodLevel, long outOfCombatTicks) {
        if (!player.isAlive()
                || player.isSpectator()
                || player.getAbilities().invulnerable
                || player.getHealth() >= player.getMaxHealth() * (PassiveRegenConfig.MAX_REGEN_HEALTH_PERCENT.get() / 100.0F)
                || hasBlockedEffect(player, PassiveRegenConfig.BLOCKED_EFFECTS.get())
                || isDimensionBlacklisted(player, PassiveRegenConfig.DIMENSION_BLACKLIST.get())
                || (!PassiveRegenConfig.REGEN_WHILE_SPRINTING.get() && player.isSprinting())
                || isHungerOverrideActive(player.getUUID())) {
            return false;
        }

        boolean belowHunger = foodLevel < PassiveRegenConfig.getMinimumFoodLevel();
        boolean belowSaturation = PassiveRegenConfig.MINIMUM_SATURATION_LEVEL.get() > 0.0D
            && player.getFoodData().getSaturationLevel() < PassiveRegenConfig.MINIMUM_SATURATION_LEVEL.get();
        return belowHunger || belowSaturation;
    }

    private boolean isHungerOverrideActive(UUID playerId) {
        Long expiresAt = hungerOverrides.get(playerId);
        if (expiresAt == null) {
            return false;
        }
        if (getCurrentTick(playerId) >= expiresAt) {
            hungerOverrides.remove(playerId);
            return false;
        }
        return true;
    }

    private static boolean hasBlockedEffect(ServerPlayer player, List<? extends String> blockedEffects) {
        if (blockedEffects == null || blockedEffects.isEmpty()) {
            return false;
        }
        for (MobEffectInstance effect : player.getActiveEffects()) {
            ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value());
            if (id != null && blockedEffects.contains(id.toString())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isDimensionBlacklisted(ServerPlayer player, List<? extends String> dimensionBlacklist) {
        if (dimensionBlacklist == null || dimensionBlacklist.isEmpty()) {
            return false;
        }
        return dimensionBlacklist.contains(player.serverLevel().dimension().location().toString());
    }

    private static boolean isKillIgnored(LivingEntity victim) {
        if (victim == null) {
            return true;
        }
        if (PassiveRegenConfig.REGEN_ON_KILL_HOSTILE_ONLY.get() && !(victim instanceof Enemy)) {
            return true;
        }
        List<? extends String> blacklist = PassiveRegenConfig.REGEN_ON_KILL_BLACKLIST.get();
        if (blacklist == null || blacklist.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        return id != null && blacklist.contains(id.toString());
    }

    private long getOutOfCombatTicks(UUID playerId, long now) {
        Long lastDamageTick = lastDamageTicks.get(playerId);
        return lastDamageTick == null ? 0L : Math.max(0L, now - lastDamageTick);
    }

    private int getCooldownDuration(UUID playerId, ServerPlayer player) {
        Integer stored = cooldownDurations.get(playerId);
        if (stored != null) {
            return Math.max(0, stored);
        }
        if (player == null) {
            return 0;
        }
        return PassiveRegenConfig.getEffectiveDamageCooldown(player.getFoodData().getFoodLevel());
    }

    private long getCurrentTick(UUID playerId) {
        ServerPlayer player = resolvePlayer(playerId);
        return player != null ? player.serverLevel().getGameTime() : serverTick;
    }

    private void restoreCooldownStateAfterReconnect(ServerPlayer player) {
        CompoundTag persisted = getPersistedData(player);
        if (!persisted.contains(SAVED_ROOT_TAG, Tag.TAG_COMPOUND)) {
            lastDamageTicks.remove(player.getUUID());
            cooldownDurations.remove(player.getUUID());
            return;
        }

        CompoundTag saved = persisted.getCompound(SAVED_ROOT_TAG);
        long outOfCombatTicks = Math.max(0L, saved.getLong(SAVED_OUT_OF_COMBAT_TAG));
        int cooldownDuration = Math.max(0, saved.getInt(SAVED_DURATION_TAG));
        lastDamageTicks.put(player.getUUID(), player.serverLevel().getGameTime() - outOfCombatTicks);
        cooldownDurations.put(player.getUUID(), cooldownDuration);
    }

    private void clearSavedCooldownState(ServerPlayer player) {
        getPersistedData(player).remove(SAVED_ROOT_TAG);
    }

    private void setSavedCooldownState(ServerPlayer player, long outOfCombatTicks, int cooldownDuration) {
        CompoundTag saved = new CompoundTag();
        saved.putLong(SAVED_OUT_OF_COMBAT_TAG, Math.max(0L, outOfCombatTicks));
        saved.putInt(SAVED_DURATION_TAG, Math.max(0, cooldownDuration));
        getPersistedData(player).put(SAVED_ROOT_TAG, saved);
    }

    private void syncHudState(ServerPlayer player, long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, int maxRegenHealthPercent) {
        UUID playerId = player.getUUID();
        boolean nearCampfire = false;
        if (PassiveRegenConfig.CAMPFIRE_REGEN_ENABLED.get()) {
            long now = player.serverLevel().getGameTime();
            Long nextCheck = campfireNextCheckTick.get(playerId);
            if (nextCheck == null || now >= nextCheck) {
                nearCampfire = isNearCampfire(player, PassiveRegenConfig.CAMPFIRE_RADIUS.get());
                campfireCache.put(playerId, nearCampfire);
                campfireNextCheckTick.put(playerId, now + 40L);
            } else {
                Boolean cached = campfireCache.get(playerId);
                nearCampfire = cached != null && cached;
            }
        }

        boolean saturationBonus = !hungerBlocked && isSaturationBonusActive(player);
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
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radiusSq) {
                    continue;
                }
                for (int dy = -2; dy <= 4; dy++) {
                    mutablePos.set(playerPos.getX() + dx, playerPos.getY() + dy, playerPos.getZ() + dz);
                    BlockState state = player.serverLevel().getBlockState(mutablePos);
                    if ((state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE))
                            && state.getValue(CampfireBlock.LIT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private ServerPlayer resolvePlayer(UUID playerId) {
        return currentServer != null ? currentServer.getPlayerList().getPlayer(playerId) : null;
    }

    private static CompoundTag getPersistedData(Player player) {
        return player.getPersistentData();
    }

    private record RegenBoost(double multiplier, long expiresAt) {
    }

    private record RegenPenalty(double multiplier, long expiresAt) {
    }

    private record HudSyncState(long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean nearCampfire, float health, float maxHealth, int maxRegenHealthPercent, boolean saturationBonus, boolean poisoned, boolean withered) {
    }
}
