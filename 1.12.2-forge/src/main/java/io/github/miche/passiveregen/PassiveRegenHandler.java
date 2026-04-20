package io.github.miche.passiveregen;

import io.github.miche.passiveregen.api.IPassiveRegenInternals;
import io.github.miche.passiveregen.api.PassiveRegenAPI;
import io.github.miche.passiveregen.event.PassiveRegenCooldownStartEvent;
import io.github.miche.passiveregen.event.PassiveRegenTickEvent;
import io.github.miche.passiveregen.network.RegenHudPacket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public class PassiveRegenHandler implements IPassiveRegenInternals {
    private static final String PERSISTED_OUT_OF_COMBAT_TICKS_TAG = PassiveRegenMod.MODID + ":savedOutOfCombatTicks";
    private static final String PERSISTED_COOLDOWN_DURATION_TICKS_TAG = PassiveRegenMod.MODID + ":savedCooldownDurationTicks";

    private final Map<UUID, Long> lastDamageTicks = new HashMap<>();
    private final Map<UUID, Integer> cooldownDurations = new HashMap<>();
    private final Map<UUID, RegenBoost> activeBoosts = new HashMap<>();
    private final Map<UUID, RegenPenalty> activePenalties = new HashMap<>();
    private final Map<UUID, Long> hungerOverrides = new HashMap<>();
    private final Map<UUID, Long> lastKillTicks = new HashMap<>();
    private final Map<UUID, Integer> killComboStacks = new HashMap<>();
    private final Map<UUID, HudSyncState> lastHudStates = new HashMap<>();
    static volatile long serverTick = 0;

    public PassiveRegenHandler() {
        PassiveRegenAPI.register(this);
    }

    @Override
    public void clearDamageCooldown(UUID playerUUID) {
        lastDamageTicks.put(playerUUID, 0L);
        cooldownDurations.put(playerUUID, 0);
    }

    @Override
    public void applyRegenBoost(UUID playerUUID, double multiplier, int durationTicks) {
        double clampedMultiplier = Math.max(1.0D, multiplier);
        long expiresAt = serverTick + Math.max(0, durationTicks);
        RegenBoost existing = activeBoosts.get(playerUUID);
        if (existing == null || clampedMultiplier >= existing.multiplier) {
            activeBoosts.put(playerUUID, new RegenBoost(clampedMultiplier, expiresAt));
        }
    }

    @Override
    public void reduceCooldown(UUID playerUUID, int percentReduction) {
        Long lastDamageTick = lastDamageTicks.get(playerUUID);
        if (lastDamageTick == null) return;

        int cooldownDuration = getCooldownDuration(playerUUID, resolvePlayer(playerUUID));
        long remaining = lastDamageTick + cooldownDuration - serverTick;
        if (remaining <= 0) return;

        int reduction = Math.max(0, Math.min(100, percentReduction));
        long cut = (long)(remaining * (reduction / 100.0D));
        lastDamageTicks.put(playerUUID, lastDamageTick - cut);
    }

    @Override
    public boolean isRegenReady(UUID playerUUID) {
        EntityPlayer player = resolvePlayer(playerUUID);
        if (player == null || player.world.isRemote) return false;

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.world.getTotalWorldTime());
        int cooldownDuration = getCooldownDuration(playerUUID, player);
        return outOfCombatTicks >= cooldownDuration && computeHealAmount(player, outOfCombatTicks) > 0.0F;
    }

    @Override
    public boolean isHungerBlocked(UUID playerUUID) {
        EntityPlayer player = resolvePlayer(playerUUID);
        if (player == null || player.world.isRemote) return false;

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.world.getTotalWorldTime());
        return isHungerBlocked(player, player.getFoodStats().getFoodLevel(), outOfCombatTicks);
    }

    @Override
    public int getRemainingCooldownTicks(UUID playerUUID) {
        EntityPlayer player = resolvePlayer(playerUUID);
        if (player == null || player.world.isRemote) return 0;

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.world.getTotalWorldTime());
        int cooldownDuration = getCooldownDuration(playerUUID, player);
        return Math.max(0, cooldownDuration - (int)Math.min(Integer.MAX_VALUE, outOfCombatTicks));
    }

    @Override
    public float getCurrentHealRate(UUID playerUUID) {
        EntityPlayer player = resolvePlayer(playerUUID);
        if (player == null || player.world.isRemote) return 0.0F;

        long outOfCombatTicks = getOutOfCombatTicks(playerUUID, player.world.getTotalWorldTime());
        return computeHealAmount(player, outOfCombatTicks);
    }

    @Override
    public void applyRegenPenalty(UUID playerUUID, double multiplier, int durationTicks) {
        double clampedMultiplier = Math.max(0.0D, Math.min(1.0D, multiplier));
        long expiresAt = serverTick + Math.max(0, durationTicks);
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
        long expiresAt = serverTick + Math.max(0, durationTicks);
        Long existing = hungerOverrides.get(playerUUID);
        if (existing == null || expiresAt > existing) {
            hungerOverrides.put(playerUUID, expiresAt);
        }
    }

    @SubscribeEvent
    public void onLivingDamage(LivingDamageEvent event) {
        if (!PassiveRegenConfig.enabled || event.getAmount() <= 0.0F) {
            return;
        }

        if (!(event.getEntityLiving() instanceof EntityPlayer) || event.getEntityLiving().world.isRemote) {
            return;
        }

        EntityPlayer player = (EntityPlayer) event.getEntityLiving();
        long now = player.world.getTotalWorldTime();
        int cooldownTicks = computeDamageCooldownTicks(player, event.getSource(), event.getAmount());
        PassiveRegenCooldownStartEvent cooldownEvent = new PassiveRegenCooldownStartEvent(player, event.getSource(), cooldownTicks);
        MinecraftForge.EVENT_BUS.post(cooldownEvent);
        cooldownTicks = Math.max(0, cooldownEvent.getCooldownTicks());

        UUID playerId = player.getUniqueID();
        lastDamageTicks.put(playerId, now);
        cooldownDurations.put(playerId, cooldownTicks);
        setSavedCooldownState(player, 0L, cooldownTicks);
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (!PassiveRegenConfig.enabled || !PassiveRegenConfig.regenOnKillEnabled) {
            return;
        }
        if (!(event.getSource().getTrueSource() instanceof EntityPlayer)) {
            return;
        }

        EntityPlayer killer = (EntityPlayer) event.getSource().getTrueSource();
        if (killer.world.isRemote || isKillIgnored(event.getEntityLiving())) {
            return;
        }

        UUID killerId = killer.getUniqueID();
        int comboStacks = updateKillCombo(killerId, killer.world.getTotalWorldTime());
        int totalReduction = Math.max(0, Math.min(100,
            PassiveRegenConfig.regenOnKillCooldownReduction
                + comboStacks * Math.max(0, PassiveRegenConfig.regenOnKillComboReductionPerStack)));
        reduceCooldown(killerId, totalReduction);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote) {
            return;
        }

        EntityPlayer player = event.player;
        if (!(player instanceof EntityPlayerMP)) {
            return;
        }

        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        long now = player.world.getTotalWorldTime();
        serverTick = now;
        UUID playerId = player.getUniqueID();
        boolean justHealed = false;
        Long lastDamageTick = lastDamageTicks.get(playerId);
        long outOfCombatTicks = getOutOfCombatTicks(playerId, now);
        syncPersistedCooldownState(player, lastDamageTick, outOfCombatTicks);

        if (!PassiveRegenConfig.enabled) {
            syncHudState(serverPlayer, outOfCombatTicks, getCooldownDuration(playerId, player), false, false, false, PassiveRegenConfig.maxRegenHealthPercent);
            return;
        }

        int cooldownDuration = getCooldownDuration(playerId, player);
        boolean hungerBlocked = isHungerBlocked(player, player.getFoodStats().getFoodLevel(), outOfCombatTicks);

        if (PassiveRegenConfig.disableNaturalRegen) {
            if (player.getFoodStats().getFoodLevel() >= 18 && player.getFoodStats().getSaturationLevel() > 0) {
                player.addExhaustion(0.1F);
            }
        }

        if (lastDamageTick == null) {
            syncHudState(serverPlayer, 0L, 0, false, false, false, PassiveRegenConfig.maxRegenHealthPercent);
            return;
        }

        if (!shouldProcessPlayer(player)) {
            syncHudState(serverPlayer, outOfCombatTicks, cooldownDuration, false, hungerBlocked, false, PassiveRegenConfig.maxRegenHealthPercent);
            return;
        }

        int updateTicks = Math.max(1, PassiveRegenConfig.updateIntervalTicks);
        if ((now + player.getEntityId()) % updateTicks != 0L) {
            HudSyncState previousHudState = lastHudStates.get(playerId);
            boolean keepActivePulse = outOfCombatTicks >= cooldownDuration
                && previousHudState != null
                && previousHudState.regenActive;
            syncHudState(serverPlayer, outOfCombatTicks, cooldownDuration, keepActivePulse, hungerBlocked, false, PassiveRegenConfig.maxRegenHealthPercent);
            return;
        }

        if (outOfCombatTicks < cooldownDuration) {
            syncHudState(serverPlayer, outOfCombatTicks, cooldownDuration, false, hungerBlocked, false, PassiveRegenConfig.maxRegenHealthPercent);
            return;
        }

        float healAmount = computeHealAmount(player, outOfCombatTicks);
        boolean regenActive = healAmount > 0.0F;

        if (healAmount > 0.0F) {
            PassiveRegenTickEvent.Pre preEvent = new PassiveRegenTickEvent.Pre(player, healAmount);
            if (!MinecraftForge.EVENT_BUS.post(preEvent)) {
                healAmount = preEvent.getHealAmount();
                if (healAmount > 0.0F) {
                    player.heal(healAmount);
                    MinecraftForge.EVENT_BUS.post(new PassiveRegenTickEvent.Post(player, healAmount));
                    justHealed = true;
                    regenActive = true;
                } else {
                    regenActive = false;
                }
            } else {
                regenActive = false;
            }
        }

        syncHudState(serverPlayer, outOfCombatTicks, cooldownDuration, regenActive, hungerBlocked, justHealed, PassiveRegenConfig.maxRegenHealthPercent);
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID playerId = event.player.getUniqueID();
        activeBoosts.remove(playerId);
        activePenalties.remove(playerId);
        hungerOverrides.remove(playerId);
        lastHudStates.remove(playerId);
        lastKillTicks.remove(playerId);
        killComboStacks.remove(playerId);
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        UUID playerId = event.player.getUniqueID();
        activeBoosts.remove(playerId);
        activePenalties.remove(playerId);
        hungerOverrides.remove(playerId);
        lastHudStates.remove(playerId);
        restoreCooldownStateAfterReconnect(event.player);

        if (event.player instanceof EntityPlayerMP) {
            EntityPlayerMP serverPlayer = (EntityPlayerMP) event.player;
            Long lastDamageTick = lastDamageTicks.get(playerId);
            if (lastDamageTick == null) {
                syncHudState(serverPlayer, 0L, 0, false, false, false, PassiveRegenConfig.maxRegenHealthPercent);
            } else {
                long outOfCombatTicks = getOutOfCombatTicks(playerId, serverPlayer.world.getTotalWorldTime());
                int cooldownDuration = getCooldownDuration(playerId, serverPlayer);
                boolean hungerBlocked = isHungerBlocked(serverPlayer, serverPlayer.getFoodStats().getFoodLevel(), outOfCombatTicks);
                syncHudState(serverPlayer, outOfCombatTicks, cooldownDuration, false, hungerBlocked, false, PassiveRegenConfig.maxRegenHealthPercent);
            }
        }
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        UUID playerId = event.player.getUniqueID();
        lastDamageTicks.remove(playerId);
        cooldownDurations.remove(playerId);
        activeBoosts.remove(playerId);
        activePenalties.remove(playerId);
        hungerOverrides.remove(playerId);
        lastHudStates.remove(playerId);
        clearSavedCooldownState(event.player);
        if (event.player instanceof EntityPlayerMP) {
            syncHudState((EntityPlayerMP) event.player, 0L, 0, false, false, false, PassiveRegenConfig.maxRegenHealthPercent);
        }
    }

    private float computeHealAmount(EntityPlayer player, long outOfCombatTicks) {
        if (!PassiveRegenConfig.enabled || !shouldProcessPlayer(player)) {
            return 0.0F;
        }

        UUID playerId = player.getUniqueID();
        int foodLevel = player.getFoodStats().getFoodLevel();
        int cooldownDuration = getCooldownDuration(playerId, player);
        if (outOfCombatTicks < cooldownDuration) {
            return 0.0F;
        }

        boolean hungerPenalized = !isHungerOverrideActive(playerId)
                && PassiveRegenConfig.hungerPenaltyEnabled
                && (foodLevel < PassiveRegenConfig.getMinimumFoodLevel()
                    || (PassiveRegenConfig.minimumSaturationLevel > 0.0D
                        && player.getFoodStats().getSaturationLevel() < PassiveRegenConfig.minimumSaturationLevel));

        double healAmount = Math.max(0.01D, PassiveRegenConfig.healAmountPerTrigger);
        double scaledHeal = healAmount * getMaxHealthScaleMultiplier(player.getMaxHealth());
        double healBonusMultiplier = PassiveRegenConfig.combineBonusMultipliers(getHealBonusMultipliers(player, foodLevel, hungerPenalized));
        double speedBonusMultiplier = PassiveRegenConfig.combineBonusMultipliers(getSpeedBonusMultipliers(player, foodLevel, hungerPenalized));

        int updateTicks = Math.max(1, PassiveRegenConfig.updateIntervalTicks);
        double currentHealInterval = getCurrentHealIntervalTicks(outOfCombatTicks) / Math.max(0.0001D, speedBonusMultiplier);
        double finalHeal = scaledHeal * healBonusMultiplier * updateTicks / currentHealInterval;
        finalHeal *= getTemporaryRateMultiplier(playerId, player.world.getTotalWorldTime());
        return (float)Math.max(0.0D, finalHeal);
    }

    private List<Double> getHealBonusMultipliers(EntityPlayer player, int foodLevel, boolean hungerPenalized) {
        List<Double> multipliers = new ArrayList<>();
        float saturationLevel = player.getFoodStats().getSaturationLevel();

        if (hungerPenalized) {
            multipliers.add(Math.max(0.01D, PassiveRegenConfig.hungerPenaltyHealMultiplier));
        } else {
            double hungerHeal = PassiveRegenConfig.getHungerHealMultiplier(foodLevel);
            if (hungerHeal != 1.0D) multipliers.add(hungerHeal);
        }

        if (PassiveRegenConfig.saturationBonusEnabled
                && saturationLevel >= PassiveRegenConfig.saturationBonusThreshold
                && PassiveRegenConfig.saturationBonusHealMultiplier != 1.0D) {
            multipliers.add(Math.max(1.0D, PassiveRegenConfig.saturationBonusHealMultiplier));
        }

        if (PassiveRegenConfig.crouchBonusEnabled && player.isSneaking() && PassiveRegenConfig.crouchHealMultiplier != 1.0D) {
            multipliers.add(Math.max(1.0D, PassiveRegenConfig.crouchHealMultiplier));
        }

        double sharedBonus = getSharedEnvironmentalMultiplier(player);
        if (sharedBonus != 1.0D) multipliers.add(sharedBonus);

        return multipliers;
    }

    private List<Double> getSpeedBonusMultipliers(EntityPlayer player, int foodLevel, boolean hungerPenalized) {
        List<Double> multipliers = new ArrayList<>();
        float saturationLevel = player.getFoodStats().getSaturationLevel();

        if (hungerPenalized) {
            multipliers.add(Math.max(0.01D, PassiveRegenConfig.hungerPenaltySpeedMultiplier));
        } else {
            double hungerSpeed = PassiveRegenConfig.getHungerSpeedMultiplier(foodLevel);
            if (hungerSpeed != 1.0D) multipliers.add(hungerSpeed);
        }

        if (PassiveRegenConfig.saturationBonusEnabled
                && saturationLevel >= PassiveRegenConfig.saturationBonusThreshold
                && PassiveRegenConfig.saturationBonusSpeedMultiplier != 1.0D) {
            multipliers.add(Math.max(1.0D, PassiveRegenConfig.saturationBonusSpeedMultiplier));
        }

        if (PassiveRegenConfig.crouchBonusEnabled && player.isSneaking() && PassiveRegenConfig.crouchSpeedMultiplier != 1.0D) {
            multipliers.add(Math.max(1.0D, PassiveRegenConfig.crouchSpeedMultiplier));
        }

        double sharedBonus = getSharedEnvironmentalMultiplier(player);
        if (sharedBonus != 1.0D) multipliers.add(sharedBonus);

        return multipliers;
    }

    private double getSharedEnvironmentalMultiplier(EntityPlayer player) {
        List<Double> multipliers = new ArrayList<>();

        if (PassiveRegenConfig.lightLevelBonusEnabled) {
            int blockLight = player.world.getLightFor(EnumSkyBlock.BLOCK, player.getPosition());
            double t = Math.max(0.0D, Math.min(1.0D, blockLight / 15.0D));
            double minMultiplier = PassiveRegenConfig.lightLevelMinMultiplier;
            double maxMultiplier = PassiveRegenConfig.lightLevelMaxMultiplier;
            multipliers.add(minMultiplier + (maxMultiplier - minMultiplier) * t);
        }

        if (PassiveRegenConfig.dayNightMultiplierEnabled) {
            multipliers.add(player.world.isDaytime() ? PassiveRegenConfig.dayMultiplier : PassiveRegenConfig.nightMultiplier);
        }

        if (PassiveRegenConfig.difficultyScalingEnabled) {
            EnumDifficulty difficulty = player.world.getDifficulty();
            switch (difficulty) {
                case PEACEFUL:
                    multipliers.add(PassiveRegenConfig.peacefulMultiplier);
                    break;
                case EASY:
                    multipliers.add(PassiveRegenConfig.easyMultiplier);
                    break;
                case NORMAL:
                    multipliers.add(PassiveRegenConfig.normalMultiplier);
                    break;
                case HARD:
                default:
                    multipliers.add(PassiveRegenConfig.hardMultiplier);
                    break;
            }
        }

        return PassiveRegenConfig.combineBonusMultipliers(multipliers);
    }

    private double getCurrentHealIntervalTicks(long outOfCombatTicks) {
        int baseTicks = Math.max(1, PassiveRegenConfig.baseHealIntervalTicks);
        if (!PassiveRegenConfig.rampUpEnabled) {
            return baseTicks;
        }

        int fullTicks = Math.max(1, PassiveRegenConfig.fullStrengthHealIntervalTicks);
        int rampTicks = Math.max(1, PassiveRegenConfig.rampFullStrengthTicks);
        double progress = Math.min(1.0D, (double) outOfCombatTicks / rampTicks);
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

    private int computeDamageCooldownTicks(EntityPlayer player, DamageSource source, float amount) {
        int cooldownTicks;
        if (PassiveRegenConfig.pvpDamageCooldownTicks >= 0 && source.getTrueSource() instanceof EntityPlayer) {
            cooldownTicks = PassiveRegenConfig.pvpDamageCooldownTicks;
        } else {
            cooldownTicks = PassiveRegenConfig.getEffectiveDamageCooldown(player.getFoodStats().getFoodLevel());
        }

        if (PassiveRegenConfig.largeDamagePenaltyEnabled && player.getMaxHealth() > 0.0F) {
            double thresholdPercent = Math.max(1, PassiveRegenConfig.largeDamageThresholdPercent);
            double damagePercent = amount / player.getMaxHealth() * 100.0D;
            if (damagePercent >= thresholdPercent) {
                cooldownTicks = (int)Math.ceil(cooldownTicks * Math.max(1.0D, PassiveRegenConfig.largeDamageCooldownMultiplier));
            }
        }

        return Math.max(0, cooldownTicks);
    }

    private int updateKillCombo(UUID playerId, long now) {
        if (!PassiveRegenConfig.regenOnKillComboEnabled) {
            return 0;
        }

        long comboWindow = Math.max(20, PassiveRegenConfig.regenOnKillComboWindowTicks);
        int previousStacks = killComboStacks.getOrDefault(playerId, 0);
        Long lastKillTick = lastKillTicks.get(playerId);
        int comboStacks;
        if (lastKillTick != null && now - lastKillTick <= comboWindow) {
            comboStacks = Math.min(Math.max(1, PassiveRegenConfig.regenOnKillComboMaxStacks), previousStacks + 1);
        } else {
            comboStacks = 1;
        }

        lastKillTicks.put(playerId, now);
        killComboStacks.put(playerId, comboStacks);
        return comboStacks;
    }

    private boolean shouldProcessPlayer(EntityPlayer player) {
        if (player.isDead
                || player.isSpectator()
                || player.capabilities.disableDamage
                || player.getHealth() >= player.getMaxHealth() * (PassiveRegenConfig.maxRegenHealthPercent / 100.0f)
                || hasBlockedEffect(player, PassiveRegenConfig.blockedEffects)
                || isDimensionBlacklisted(player, PassiveRegenConfig.dimensionBlacklist)
                || (!PassiveRegenConfig.regenWhileSprinting && player.isSprinting())) {
            return false;
        }

        // Hunger/saturation override lets regen fire regardless of food state
        if (isHungerOverrideActive(player.getUniqueID())) {
            return true;
        }

        // Penalty mode: regen still fires below threshold, just at reduced rates
        if (PassiveRegenConfig.hungerPenaltyEnabled) {
            return true;
        }

        // Hard block: must meet both hunger and saturation minimums
        int foodLevel = player.getFoodStats().getFoodLevel();
        if (foodLevel < PassiveRegenConfig.getMinimumFoodLevel()) {
            return false;
        }
        if (PassiveRegenConfig.minimumSaturationLevel > 0.0D
                && player.getFoodStats().getSaturationLevel() < PassiveRegenConfig.minimumSaturationLevel) {
            return false;
        }
        return true;
    }

    private boolean isHungerBlocked(EntityPlayer player, int foodLevel, long outOfCombatTicks) {
        if (player.isDead || player.isSpectator() || player.capabilities.disableDamage) {
            return false;
        }
        if (player.getHealth() >= player.getMaxHealth() * (PassiveRegenConfig.maxRegenHealthPercent / 100.0f)) {
            return false;
        }
        if (hasBlockedEffect(player, PassiveRegenConfig.blockedEffects) || isDimensionBlacklisted(player, PassiveRegenConfig.dimensionBlacklist)) {
            return false;
        }
        if (!PassiveRegenConfig.regenWhileSprinting && player.isSprinting()) {
            return false;
        }
        // Override bypasses the blocked/penalized state entirely
        if (isHungerOverrideActive(player.getUniqueID())) {
            return false;
        }
        boolean belowHunger = foodLevel < PassiveRegenConfig.getMinimumFoodLevel();
        boolean belowSaturation = PassiveRegenConfig.minimumSaturationLevel > 0.0D
                && player.getFoodStats().getSaturationLevel() < PassiveRegenConfig.minimumSaturationLevel;
        return belowHunger || belowSaturation;
    }

    private boolean isHungerOverrideActive(UUID playerId) {
        Long expiresAt = hungerOverrides.get(playerId);
        if (expiresAt == null) return false;
        if (serverTick >= expiresAt) {
            hungerOverrides.remove(playerId);
            return false;
        }
        return true;
    }

    private static boolean hasBlockedEffect(EntityPlayer player, String[] blockedEffects) {
        if (blockedEffects == null || blockedEffects.length == 0) return false;
        for (PotionEffect effect : player.getActivePotionEffects()) {
            ResourceLocation id = ForgeRegistries.POTIONS.getKey(effect.getPotion());
            if (id == null) continue;
            for (String blockedEffect : blockedEffects) {
                if (blockedEffect != null && blockedEffect.equalsIgnoreCase(id.toString())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isKillIgnored(EntityLivingBase victim) {
        if (victim == null) return true;
        if (PassiveRegenConfig.regenOnKillHostileOnly && !(victim instanceof IMob)) {
            return true;
        }
        String[] blacklist = PassiveRegenConfig.regenOnKillBlacklist;
        if (blacklist == null || blacklist.length == 0) return false;
        ResourceLocation id = EntityList.getKey(victim);
        if (id == null) return false;
        String idString = id.toString();
        for (String entry : blacklist) {
            if (entry != null && entry.equalsIgnoreCase(idString)) return true;
        }
        return false;
    }

    private static boolean isDimensionBlacklisted(EntityPlayer player, String[] dimensionBlacklist) {
        if (dimensionBlacklist == null || dimensionBlacklist.length == 0) return false;
        String dimensionId = String.valueOf(player.dimension);
        for (String entry : dimensionBlacklist) {
            if (entry != null && entry.equals(dimensionId)) return true;
        }
        return false;
    }

    private long getOutOfCombatTicks(UUID playerId, long now) {
        Long lastDamageTick = lastDamageTicks.get(playerId);
        return lastDamageTick == null ? 0L : Math.max(0L, now - lastDamageTick);
    }

    private int getCooldownDuration(UUID playerId, EntityPlayer player) {
        Integer stored = cooldownDurations.get(playerId);
        if (stored != null) return Math.max(0, stored);
        if (player == null) return 0;
        return PassiveRegenConfig.getEffectiveDamageCooldown(player.getFoodStats().getFoodLevel());
    }

    private void syncPersistedCooldownState(EntityPlayer player, Long lastDamageTick, long outOfCombatTicks) {
        if (lastDamageTick == null) {
            clearSavedCooldownState(player);
            return;
        }

        int cooldownDuration = getCooldownDuration(player.getUniqueID(), player);
        setSavedCooldownState(player, outOfCombatTicks, cooldownDuration);
    }

    private void restoreCooldownStateAfterReconnect(EntityPlayer player) {
        NBTTagCompound persistedData = getPersistedData(player);
        UUID playerId = player.getUniqueID();
        if (!persistedData.hasKey(PERSISTED_OUT_OF_COMBAT_TICKS_TAG) || !persistedData.hasKey(PERSISTED_COOLDOWN_DURATION_TICKS_TAG)) {
            lastDamageTicks.remove(playerId);
            cooldownDurations.remove(playerId);
            return;
        }

        long savedOutOfCombatTicks = Math.max(0L, persistedData.getLong(PERSISTED_OUT_OF_COMBAT_TICKS_TAG));
        int savedCooldownDuration = Math.max(0, persistedData.getInteger(PERSISTED_COOLDOWN_DURATION_TICKS_TAG));
        lastDamageTicks.put(playerId, player.world.getTotalWorldTime() - savedOutOfCombatTicks);
        cooldownDurations.put(playerId, savedCooldownDuration);
    }

    private static void clearSavedCooldownState(EntityPlayer player) {
        NBTTagCompound persistedData = getPersistedData(player);
        persistedData.removeTag(PERSISTED_OUT_OF_COMBAT_TICKS_TAG);
        persistedData.removeTag(PERSISTED_COOLDOWN_DURATION_TICKS_TAG);
    }

    private static void setSavedCooldownState(EntityPlayer player, long outOfCombatTicks, int cooldownDuration) {
        NBTTagCompound persistedData = getPersistedData(player);
        persistedData.setLong(PERSISTED_OUT_OF_COMBAT_TICKS_TAG, Math.max(0L, outOfCombatTicks));
        persistedData.setInteger(PERSISTED_COOLDOWN_DURATION_TICKS_TAG, Math.max(0, cooldownDuration));
    }

    private static NBTTagCompound getPersistedData(EntityPlayer player) {
        NBTTagCompound entityData = player.getEntityData();
        if (!entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG, 10)) {
            entityData.setTag(EntityPlayer.PERSISTED_NBT_TAG, new NBTTagCompound());
        }
        return entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG);
    }

    private void syncHudState(EntityPlayerMP player, long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, boolean justHealed, int maxRegenHealthPercent) {
        UUID playerId = player.getUniqueID();
        HudSyncState current = new HudSyncState(outOfCombatTicks, damageCooldownTicks, regenActive, hungerBlocked, player.getHealth(), player.getMaxHealth(), maxRegenHealthPercent);
        HudSyncState previous = lastHudStates.get(playerId);
        if (justHealed || !current.equals(previous)) {
            PassiveRegenMod.NETWORK.sendTo(new RegenHudPacket(outOfCombatTicks, damageCooldownTicks, regenActive, hungerBlocked, justHealed, player.getHealth(), player.getMaxHealth(), maxRegenHealthPercent), player);
            lastHudStates.put(playerId, current);
        }
    }

    private EntityPlayer resolvePlayer(UUID playerUUID) {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        return server != null ? server.getPlayerList().getPlayerByUUID(playerUUID) : null;
    }

    private static double getMaxHealthScaleMultiplier(float maxHealth) {
        if (!PassiveRegenConfig.scaleWithMaxHealth || maxHealth <= 20.0F) {
            return 1.0D;
        }

        double normalized = Math.max(1.0D, maxHealth / 20.0D);
        double exponent = Math.max(0.1D, PassiveRegenConfig.maxHealthScalingExponent);
        double multiplier = Math.pow(normalized, exponent);
        double cap = Math.max(1.0D, PassiveRegenConfig.maxHealthScalingCap);
        return Math.min(cap, multiplier);
    }

    private static final class RegenBoost {
        final double multiplier;
        final long expiresAt;

        RegenBoost(double multiplier, long expiresAt) {
            this.multiplier = multiplier;
            this.expiresAt = expiresAt;
        }
    }

    private static final class RegenPenalty {
        final double multiplier;
        final long expiresAt;

        RegenPenalty(double multiplier, long expiresAt) {
            this.multiplier = multiplier;
            this.expiresAt = expiresAt;
        }
    }

    private static final class HudSyncState {
        private final long outOfCombatTicks;
        private final int damageCooldownTicks;
        private final boolean regenActive;
        private final boolean hungerBlocked;
        private final float health;
        private final float maxHealth;
        private final int maxRegenHealthPercent;

        private HudSyncState(long outOfCombatTicks, int damageCooldownTicks, boolean regenActive, boolean hungerBlocked, float health, float maxHealth, int maxRegenHealthPercent) {
            this.outOfCombatTicks = outOfCombatTicks;
            this.damageCooldownTicks = damageCooldownTicks;
            this.regenActive = regenActive;
            this.hungerBlocked = hungerBlocked;
            this.health = health;
            this.maxHealth = maxHealth;
            this.maxRegenHealthPercent = maxRegenHealthPercent;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof HudSyncState)) return false;
            HudSyncState other = (HudSyncState) obj;
            return outOfCombatTicks == other.outOfCombatTicks
                && damageCooldownTicks == other.damageCooldownTicks
                && regenActive == other.regenActive
                && hungerBlocked == other.hungerBlocked
                && Float.compare(health, other.health) == 0
                && Float.compare(maxHealth, other.maxHealth) == 0
                && maxRegenHealthPercent == other.maxRegenHealthPercent;
        }

        @Override
        public int hashCode() {
            int result = Long.hashCode(outOfCombatTicks);
            result = 31 * result + damageCooldownTicks;
            result = 31 * result + (regenActive ? 1 : 0);
            result = 31 * result + (hungerBlocked ? 1 : 0);
            result = 31 * result + Float.floatToIntBits(health);
            result = 31 * result + Float.floatToIntBits(maxHealth);
            result = 31 * result + maxRegenHealthPercent;
            return result;
        }
    }
}
