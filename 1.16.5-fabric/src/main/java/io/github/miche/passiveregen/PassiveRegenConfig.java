package io.github.miche.passiveregen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

public final class PassiveRegenConfig {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("passive-health-regen.json");

    public boolean enabled = true;
    public int damageCooldownTicks = 100;
    public int minimumHungerPercent = 50;
    public double minimumSaturationLevel = 0.0D;
    public int updateIntervalTicks = 20;
    public int baseHealIntervalTicks = 100;
    public boolean rampUpEnabled = false;
    public int fullStrengthHealIntervalTicks = 50;
    public int rampFullStrengthTicks = 600;
    public double healAmountPerTrigger = 0.5D;

    public boolean scaleWithMaxHealth = false;
    public double maxHealthScalingExponent = 0.5D;
    public double maxHealthScalingCap = 2.0D;
    public int maxRegenHealthPercent = 100;
    public String[] blockedEffects = new String[0];
    public String[] dimensionBlacklist = new String[0];
    public int pvpDamageCooldownTicks = -1;
    public boolean disableNaturalRegen = false;
    public boolean regenWhileSprinting = true;

    public boolean hungerBonusEnabled = false;
    public int hungerBonusThresholdPercent = 75;
    public double hungerBonusHealMultiplier = 1.5D;
    public double hungerBonusSpeedMultiplier = 1.5D;
    public int hungerBonusCooldownReduction = 25;
    public boolean hungerPenaltyEnabled = false;
    public double hungerPenaltySpeedMultiplier = 0.25D;
    public double hungerPenaltyHealMultiplier = 1.0D;
    public boolean hungerFullBonusEnabled = false;
    public double hungerFullBonusHealMultiplier = 2.0D;
    public double hungerFullBonusSpeedMultiplier = 2.0D;

    public boolean saturationBonusEnabled = true;
    public double saturationBonusThreshold = 10.0D;

    public double saturationBonusDeactivateThreshold = 10.0D;
    public double saturationBonusSpeedMultiplier = 2.0D;
    public double saturationBonusHealMultiplier = 2.0D;

    public double saturationBonusCostPerHp = 1.0D;

    public double saturationBonusIdleDrainPerTick = 0.0D;

    public double saturationBonusMinSaturationFloor = 0.0D;

    public double saturationBonusFlatHealBonus = 0.25D;

    public boolean saturationBonusScaleByExcess = false;

    public boolean hungerDrainEnabled = false;
    public double hungerDrainSpeedMultiplier = 1.0D;
    public double hungerDrainCostPerHp = 0.6D;
    public double hungerDrainIdleDrainPerTick = 0.0D;
    public double hungerDrainMinFloor = 0.0D;

    public boolean disableHealingDuringPoison = true;
    public boolean disableHealingDuringWither = true;

    public boolean regenOnKillEnabled = false;
    public int regenOnKillCooldownReduction = 50;
    public boolean regenOnKillHostileOnly = false;
    public String[] regenOnKillBlacklist = new String[0];
    public boolean regenOnKillComboEnabled = false;
    public int regenOnKillComboWindowTicks = 200;
    public int regenOnKillComboMaxStacks = 5;
    public int regenOnKillComboReductionPerStack = 10;

    public BonusStackingMode bonusStackingMode = BonusStackingMode.MULTIPLICATIVE;
    public boolean crouchBonusEnabled = false;
    public double crouchSpeedMultiplier = 1.5D;
    public double crouchHealMultiplier = 1.0D;
    public boolean lightLevelBonusEnabled = false;
    public double lightLevelMinMultiplier = 0.75D;
    public double lightLevelMaxMultiplier = 1.25D;
    public boolean dayNightMultiplierEnabled = false;
    public double dayMultiplier = 1.25D;
    public double nightMultiplier = 0.75D;
    public boolean difficultyScalingEnabled = false;
    public double peacefulMultiplier = 2.0D;
    public double easyMultiplier = 1.25D;
    public double normalMultiplier = 1.0D;
    public double hardMultiplier = 0.75D;

    public boolean largeDamagePenaltyEnabled = false;
    public int largeDamageThresholdPercent = 50;
    public double largeDamageCooldownMultiplier = 1.5D;

    public boolean campfireRegenEnabled = true;
    public int campfireRadius = 8;
    public double campfireSpeedMultiplier = 2.0D;
    public double campfireHealMultiplier = 1.0D;
    public boolean campfireCooldownReductionEnabled = false;
    public int campfireCooldownReductionPercent = 20;

    public boolean freezingPenaltyEnabled = false;
    public double freezingPenaltyThresholdPercent = 0.0D;
    public double freezingSpeedMultiplier = 0.5D;
    public double freezingHealMultiplier = 0.75D;
    public double freezingCooldownMultiplier = 1.75D;
    public boolean freezingBlocksRegen = false;

    public enum BonusStackingMode {
        MULTIPLICATIVE,
        ADDITIVE,
        STRONGEST_ONLY
    }

    public static PassiveRegenConfig load() {
        PassiveRegenConfig config = null;
        if (Files.exists(CONFIG_PATH)) {
            try (JsonReader reader = new JsonReader(Files.newBufferedReader(CONFIG_PATH))) {
                reader.setLenient(true);
                config = GSON.fromJson(reader, PassiveRegenConfig.class);
            } catch (IOException ignored) {
            }
        }

        if (config == null) {
            config = new PassiveRegenConfig();
        }

        config.clamp();
        config.save();
        return config;
    }

    public void save() {
        try {
            sanitize();
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                writer.write("{"); writer.newLine();
                writer.newLine();
                writer.write("  // General"); writer.newLine();
                writer.write("  \"enabled\": " + enabled + ","); writer.newLine();
                writer.write("  \"damageCooldownTicks\": " + damageCooldownTicks + ","); writer.newLine();
                writer.write("  \"minimumHungerPercent\": " + minimumHungerPercent + ","); writer.newLine();
                writer.write("  \"minimumSaturationLevel\": " + minimumSaturationLevel + ","); writer.newLine();
                writer.write("  \"updateIntervalTicks\": " + updateIntervalTicks + ","); writer.newLine();
                writer.write("  \"baseHealIntervalTicks\": " + baseHealIntervalTicks + ","); writer.newLine();
                writer.write("  \"rampUpEnabled\": " + rampUpEnabled + ","); writer.newLine();
                writer.write("  \"fullStrengthHealIntervalTicks\": " + fullStrengthHealIntervalTicks + ","); writer.newLine();
                writer.write("  \"rampFullStrengthTicks\": " + rampFullStrengthTicks + ","); writer.newLine();
                writer.write("  \"healAmountPerTrigger\": " + healAmountPerTrigger + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Limits"); writer.newLine();
                writer.write("  \"scaleWithMaxHealth\": " + scaleWithMaxHealth + ","); writer.newLine();
                writer.write("  \"maxHealthScalingExponent\": " + maxHealthScalingExponent + ","); writer.newLine();
                writer.write("  \"maxHealthScalingCap\": " + maxHealthScalingCap + ","); writer.newLine();
                writer.write("  \"maxRegenHealthPercent\": " + maxRegenHealthPercent + ","); writer.newLine();
                writer.write("  \"blockedEffects\": " + toJsonArray(blockedEffects) + ","); writer.newLine();
                writer.write("  \"dimensionBlacklist\": " + toJsonArray(dimensionBlacklist) + ","); writer.newLine();
                writer.write("  \"pvpDamageCooldownTicks\": " + pvpDamageCooldownTicks + ","); writer.newLine();
                writer.write("  \"disableNaturalRegen\": " + disableNaturalRegen + ","); writer.newLine();
                writer.write("  \"regenWhileSprinting\": " + regenWhileSprinting + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Hunger bonus"); writer.newLine();
                writer.write("  \"hungerBonusEnabled\": " + hungerBonusEnabled + ","); writer.newLine();
                writer.write("  \"hungerBonusThresholdPercent\": " + hungerBonusThresholdPercent + ","); writer.newLine();
                writer.write("  \"hungerBonusHealMultiplier\": " + hungerBonusHealMultiplier + ","); writer.newLine();
                writer.write("  \"hungerBonusSpeedMultiplier\": " + hungerBonusSpeedMultiplier + ","); writer.newLine();
                writer.write("  \"hungerBonusCooldownReduction\": " + hungerBonusCooldownReduction + ","); writer.newLine();
                writer.write("  \"hungerPenaltyEnabled\": " + hungerPenaltyEnabled + ","); writer.newLine();
                writer.write("  \"hungerPenaltySpeedMultiplier\": " + hungerPenaltySpeedMultiplier + ","); writer.newLine();
                writer.write("  \"hungerPenaltyHealMultiplier\": " + hungerPenaltyHealMultiplier + ","); writer.newLine();
                writer.write("  \"hungerFullBonusEnabled\": " + hungerFullBonusEnabled + ","); writer.newLine();
                writer.write("  \"hungerFullBonusHealMultiplier\": " + hungerFullBonusHealMultiplier + ","); writer.newLine();
                writer.write("  \"hungerFullBonusSpeedMultiplier\": " + hungerFullBonusSpeedMultiplier + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Saturation bonus"); writer.newLine();
                writer.write("  \"saturationBonusEnabled\": " + saturationBonusEnabled + ","); writer.newLine();
                writer.write("  \"saturationBonusThreshold\": " + saturationBonusThreshold + ","); writer.newLine();
                writer.write("  // Once active, bonus stays active until sat drops to this value (hysteresis). Equal to threshold = off."); writer.newLine();
                writer.write("  \"saturationBonusDeactivateThreshold\": " + saturationBonusDeactivateThreshold + ","); writer.newLine();
                writer.write("  \"saturationBonusSpeedMultiplier\": " + saturationBonusSpeedMultiplier + ","); writer.newLine();
                writer.write("  \"saturationBonusHealMultiplier\": " + saturationBonusHealMultiplier + ","); writer.newLine();
                writer.write("  // Saturation consumed per 1 HP healed while bonus active. Vanilla saturation-heal uses 1.5."); writer.newLine();
                writer.write("  \"saturationBonusCostPerHp\": " + saturationBonusCostPerHp + ","); writer.newLine();
                writer.write("  // Optional idle wick: extra per-tick drain while bonus active. 0.005 â‰ˆ 0.1 sat/sec. 0 = vanilla model."); writer.newLine();
                writer.write("  \"saturationBonusIdleDrainPerTick\": " + saturationBonusIdleDrainPerTick + ","); writer.newLine();
                writer.write("  // Drain cannot push saturation below this level. 0 = no floor."); writer.newLine();
                writer.write("  \"saturationBonusMinSaturationFloor\": " + saturationBonusMinSaturationFloor + ","); writer.newLine();
                writer.write("  // Flat HP added per heal tick (on top of multipliers). Makes ticks feel chunkier."); writer.newLine();
                writer.write("  \"saturationBonusFlatHealBonus\": " + saturationBonusFlatHealBonus + ","); writer.newLine();
                writer.write("  // When true, bonus strength scales linearly with sat above threshold (0% at threshold, 100% at 20)."); writer.newLine();
                writer.write("  \"saturationBonusScaleByExcess\": " + saturationBonusScaleByExcess + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Hunger drain"); writer.newLine();
                writer.write("  \"hungerDrainEnabled\": " + hungerDrainEnabled + ","); writer.newLine();
                writer.write("  \"hungerDrainSpeedMultiplier\": " + hungerDrainSpeedMultiplier + ","); writer.newLine();
                writer.write("  \"hungerDrainCostPerHp\": " + hungerDrainCostPerHp + ","); writer.newLine();
                writer.write("  \"hungerDrainIdleDrainPerTick\": " + hungerDrainIdleDrainPerTick + ","); writer.newLine();
                writer.write("  \"hungerDrainMinFloor\": " + hungerDrainMinFloor + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Status effect blockers (visuals always play; toggle only affects regen)"); writer.newLine();
                writer.write("  \"disableHealingDuringPoison\": " + disableHealingDuringPoison + ","); writer.newLine();
                writer.write("  \"disableHealingDuringWither\": " + disableHealingDuringWither + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Regen on kill"); writer.newLine();
                writer.write("  \"regenOnKillEnabled\": " + regenOnKillEnabled + ","); writer.newLine();
                writer.write("  \"regenOnKillCooldownReduction\": " + regenOnKillCooldownReduction + ","); writer.newLine();
                writer.write("  \"regenOnKillHostileOnly\": " + regenOnKillHostileOnly + ","); writer.newLine();
                writer.write("  \"regenOnKillBlacklist\": " + toJsonArray(regenOnKillBlacklist) + ","); writer.newLine();
                writer.write("  \"regenOnKillComboEnabled\": " + regenOnKillComboEnabled + ","); writer.newLine();
                writer.write("  \"regenOnKillComboWindowTicks\": " + regenOnKillComboWindowTicks + ","); writer.newLine();
                writer.write("  \"regenOnKillComboMaxStacks\": " + regenOnKillComboMaxStacks + ","); writer.newLine();
                writer.write("  \"regenOnKillComboReductionPerStack\": " + regenOnKillComboReductionPerStack + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Bonuses"); writer.newLine();
                writer.write("  \"bonusStackingMode\": \"" + bonusStackingMode.name() + "\","); writer.newLine();
                writer.write("  \"crouchBonusEnabled\": " + crouchBonusEnabled + ","); writer.newLine();
                writer.write("  \"crouchSpeedMultiplier\": " + crouchSpeedMultiplier + ","); writer.newLine();
                writer.write("  \"crouchHealMultiplier\": " + crouchHealMultiplier + ","); writer.newLine();
                writer.write("  \"lightLevelBonusEnabled\": " + lightLevelBonusEnabled + ","); writer.newLine();
                writer.write("  \"lightLevelMinMultiplier\": " + lightLevelMinMultiplier + ","); writer.newLine();
                writer.write("  \"lightLevelMaxMultiplier\": " + lightLevelMaxMultiplier + ","); writer.newLine();
                writer.write("  \"dayNightMultiplierEnabled\": " + dayNightMultiplierEnabled + ","); writer.newLine();
                writer.write("  \"dayMultiplier\": " + dayMultiplier + ","); writer.newLine();
                writer.write("  \"nightMultiplier\": " + nightMultiplier + ","); writer.newLine();
                writer.write("  \"difficultyScalingEnabled\": " + difficultyScalingEnabled + ","); writer.newLine();
                writer.write("  \"peacefulMultiplier\": " + peacefulMultiplier + ","); writer.newLine();
                writer.write("  \"easyMultiplier\": " + easyMultiplier + ","); writer.newLine();
                writer.write("  \"normalMultiplier\": " + normalMultiplier + ","); writer.newLine();
                writer.write("  \"hardMultiplier\": " + hardMultiplier + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Large damage penalty"); writer.newLine();
                writer.write("  \"largeDamagePenaltyEnabled\": " + largeDamagePenaltyEnabled + ","); writer.newLine();
                writer.write("  \"largeDamageThresholdPercent\": " + largeDamageThresholdPercent + ","); writer.newLine();
                writer.write("  \"largeDamageCooldownMultiplier\": " + largeDamageCooldownMultiplier + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Campfire aura. Warm up near a lit campfire/soul campfire."); writer.newLine();
                writer.write("  \"campfireRegenEnabled\": " + campfireRegenEnabled + ","); writer.newLine();
                writer.write("  \"campfireRadius\": " + campfireRadius + ","); writer.newLine();
                writer.write("  // Faster ticks while near a campfire. 1.0 disables the speed bump."); writer.newLine();
                writer.write("  \"campfireSpeedMultiplier\": " + campfireSpeedMultiplier + ","); writer.newLine();
                writer.write("  // Bigger heal per tick while near a campfire. 1.0 disables the heal bump."); writer.newLine();
                writer.write("  \"campfireHealMultiplier\": " + campfireHealMultiplier + ","); writer.newLine();
                writer.write("  // One-shot cooldown reduction when player first sits near a campfire after taking damage."); writer.newLine();
                writer.write("  // Stacks on top of bandages / regenOnKill / API reduceCooldown calls."); writer.newLine();
                writer.write("  \"campfireCooldownReductionEnabled\": " + campfireCooldownReductionEnabled + ","); writer.newLine();
                writer.write("  \"campfireCooldownReductionPercent\": " + campfireCooldownReductionPercent + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Freezing penalty (1.17+). Slows/blocks regen while the player is frozen in powder snow."); writer.newLine();
                writer.write("  \"freezingPenaltyEnabled\": " + freezingPenaltyEnabled + ","); writer.newLine();
                writer.write("  // Fraction of getTicksRequiredToFreeze before the penalty kicks in. 0.4 = 40% frozen."); writer.newLine();
                writer.write("  \"freezingPenaltyThresholdPercent\": " + freezingPenaltyThresholdPercent + ","); writer.newLine();
                writer.write("  // Tick speed multiplier while frozen past threshold. <1.0 = slower. 1.0 = no change."); writer.newLine();
                writer.write("  \"freezingSpeedMultiplier\": " + freezingSpeedMultiplier + ","); writer.newLine();
                writer.write("  // Heal amount multiplier while frozen past threshold. <1.0 = smaller heals."); writer.newLine();
                writer.write("  \"freezingHealMultiplier\": " + freezingHealMultiplier + ","); writer.newLine();
                writer.write("  // Extends the out-of-combat wait while frozen. 1.5 = 50% longer cooldown before regen starts."); writer.newLine();
                writer.write("  \"freezingCooldownMultiplier\": " + freezingCooldownMultiplier + ","); writer.newLine();
                writer.write("  // If true, no regen at all while frozen past threshold. Overrides the multipliers."); writer.newLine();
                writer.write("  \"freezingBlocksRegen\": " + freezingBlocksRegen); writer.newLine();
                writer.write("}"); writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    public int getMinimumFoodLevel() {
        return (int)Math.ceil((Math.max(0, Math.min(100, minimumHungerPercent)) / 100.0D) * 20.0D);
    }

    public int getHungerBonusThresholdFoodLevel() {
        return (int)Math.ceil((Math.max(0, Math.min(100, hungerBonusThresholdPercent)) / 100.0D) * 20.0D);
    }

    public float getHealAmountPerUpdate(long outOfCombatTicks, float maxHealth, int foodLevel) {
        if (!enabled) return 0.0F;

        double healAmount = Math.max(0.01D, healAmountPerTrigger);
        double scaledHeal = healAmount * getMaxHealthScaleMultiplier(maxHealth);
        int updateTicks = Math.max(1, updateIntervalTicks);
        double currentHealInterval = getCurrentHealIntervalTicks(outOfCombatTicks);
        double healMult = getHungerHealMultiplier(foodLevel);
        double speedMult = getHungerSpeedMultiplier(foodLevel);
        return (float)(scaledHeal * healMult * updateTicks / (currentHealInterval / speedMult));
    }

    public int getEffectiveDamageCooldown(int foodLevel) {
        int base = Math.max(0, damageCooldownTicks);
        if (!hungerBonusEnabled) return base;
        if (foodLevel < getHungerBonusThresholdFoodLevel()) return base;
        int reduction = Math.max(0, Math.min(100, hungerBonusCooldownReduction));
        return (int)(base * (1.0D - reduction / 100.0D));
    }

    public double getHungerHealMultiplier(int foodLevel) {
        if (!hungerBonusEnabled) return 1.0D;
        if (hungerFullBonusEnabled && foodLevel >= 20) return Math.max(1.0D, hungerFullBonusHealMultiplier);
        if (foodLevel >= getHungerBonusThresholdFoodLevel()) return Math.max(1.0D, hungerBonusHealMultiplier);
        return 1.0D;
    }

    public double getHungerSpeedMultiplier(int foodLevel) {
        if (!hungerBonusEnabled) return 1.0D;
        if (hungerFullBonusEnabled && foodLevel >= 20) return Math.max(1.0D, hungerFullBonusSpeedMultiplier);
        if (foodLevel >= getHungerBonusThresholdFoodLevel()) return Math.max(1.0D, hungerBonusSpeedMultiplier);
        return 1.0D;
    }

    public double combineBonusMultipliers(List<Double> multipliers) {
        if (multipliers == null || multipliers.isEmpty()) return 1.0D;
        switch (bonusStackingMode) {
            case ADDITIVE:
                double additiveResult = 1.0D;
                for (double multiplier : multipliers) additiveResult += (multiplier - 1.0D);
                return Math.max(0.0D, additiveResult);
            case STRONGEST_ONLY:
                double strongest = 1.0D;
                for (double multiplier : multipliers) strongest = Math.max(strongest, multiplier);
                return strongest;
            case MULTIPLICATIVE:
            default:
                double multiplicativeResult = 1.0D;
                for (double multiplier : multipliers) multiplicativeResult *= multiplier;
                return Math.max(0.0D, multiplicativeResult);
        }
    }

    private double getCurrentHealIntervalTicks(long outOfCombatTicks) {
        int baseTicks = Math.max(1, baseHealIntervalTicks);
        if (!rampUpEnabled) return baseTicks;
        int fullTicks = Math.max(1, fullStrengthHealIntervalTicks);
        int rampTicks = Math.max(1, rampFullStrengthTicks);
        double progress = Math.min(1.0D, (double)outOfCombatTicks / rampTicks);
        return baseTicks + (fullTicks - baseTicks) * progress;
    }

    private double getMaxHealthScaleMultiplier(float maxHealth) {
        if (!scaleWithMaxHealth || maxHealth <= 20.0F) return 1.0D;
        double normalized = Math.max(1.0D, maxHealth / 20.0D);
        double exponent = Math.max(0.1D, maxHealthScalingExponent);
        double multiplier = Math.pow(normalized, exponent);
        double cap = Math.max(1.0D, maxHealthScalingCap);
        return Math.min(cap, multiplier);
    }

    private void clamp() {
        sanitize();
    }

    public void sanitize() {
        damageCooldownTicks = clampInt(damageCooldownTicks, 0, 12000);
        minimumHungerPercent = clampInt(minimumHungerPercent, 0, 100);
        minimumSaturationLevel = clampDouble(minimumSaturationLevel, 0.0D, 20.0D);
        updateIntervalTicks = clampInt(updateIntervalTicks, 1, 200);
        baseHealIntervalTicks = clampInt(baseHealIntervalTicks, 1, 12000);
        fullStrengthHealIntervalTicks = clampInt(fullStrengthHealIntervalTicks, 1, 12000);
        rampFullStrengthTicks = clampInt(rampFullStrengthTicks, 1, 12000);
        if (updateIntervalTicks > baseHealIntervalTicks) {
            updateIntervalTicks = baseHealIntervalTicks;
        }
        if (updateIntervalTicks > fullStrengthHealIntervalTicks) {
            updateIntervalTicks = fullStrengthHealIntervalTicks;
        }
        healAmountPerTrigger = clampDouble(healAmountPerTrigger, 0.01D, 100.0D);

        maxHealthScalingExponent = clampDouble(maxHealthScalingExponent, 0.1D, 4.0D);
        maxHealthScalingCap = clampDouble(maxHealthScalingCap, 1.0D, 100.0D);
        maxRegenHealthPercent = clampInt(maxRegenHealthPercent, 0, 100);
        pvpDamageCooldownTicks = clampInt(pvpDamageCooldownTicks, -1, 12000);
        if (blockedEffects == null) blockedEffects = new String[0];
        if (dimensionBlacklist == null) dimensionBlacklist = new String[0];

        hungerBonusThresholdPercent = clampInt(hungerBonusThresholdPercent, 0, 100);
        hungerBonusHealMultiplier = clampDouble(hungerBonusHealMultiplier, 1.0D, 100.0D);
        hungerBonusSpeedMultiplier = clampDouble(hungerBonusSpeedMultiplier, 1.0D, 100.0D);
        hungerBonusCooldownReduction = clampInt(hungerBonusCooldownReduction, 0, 100);
        hungerPenaltySpeedMultiplier = clampDouble(hungerPenaltySpeedMultiplier, 0.01D, 1.0D);
        hungerPenaltyHealMultiplier = clampDouble(hungerPenaltyHealMultiplier, 0.01D, 1.0D);
        hungerFullBonusHealMultiplier = clampDouble(hungerFullBonusHealMultiplier, 1.0D, 100.0D);
        hungerFullBonusSpeedMultiplier = clampDouble(hungerFullBonusSpeedMultiplier, 1.0D, 100.0D);
        saturationBonusThreshold = clampDouble(saturationBonusThreshold, 0.0D, 20.0D);
        saturationBonusSpeedMultiplier = clampDouble(saturationBonusSpeedMultiplier, 1.0D, 10.0D);
        saturationBonusHealMultiplier = clampDouble(saturationBonusHealMultiplier, 1.0D, 10.0D);
        saturationBonusCostPerHp = clampDouble(saturationBonusCostPerHp, 0.0D, 10.0D);
        saturationBonusDeactivateThreshold = clampDouble(saturationBonusDeactivateThreshold, 0.0D, saturationBonusThreshold);
        saturationBonusIdleDrainPerTick = clampDouble(saturationBonusIdleDrainPerTick, 0.0D, 1.0D);
        saturationBonusMinSaturationFloor = clampDouble(saturationBonusMinSaturationFloor, 0.0D, 20.0D);
        saturationBonusFlatHealBonus = clampDouble(saturationBonusFlatHealBonus, 0.0D, 10.0D);
        hungerDrainSpeedMultiplier = clampDouble(hungerDrainSpeedMultiplier, 0.0D, 10.0D);
        hungerDrainCostPerHp = clampDouble(hungerDrainCostPerHp, 0.0D, 10.0D);
        hungerDrainIdleDrainPerTick = clampDouble(hungerDrainIdleDrainPerTick, 0.0D, 1.0D);
        hungerDrainMinFloor = clampDouble(hungerDrainMinFloor, 0.0D, 20.0D);

        regenOnKillCooldownReduction = clampInt(regenOnKillCooldownReduction, 0, 100);
        if (regenOnKillBlacklist == null) regenOnKillBlacklist = new String[0];
        regenOnKillComboWindowTicks = clampInt(regenOnKillComboWindowTicks, 20, 1200);
        regenOnKillComboMaxStacks = clampInt(regenOnKillComboMaxStacks, 1, 20);
        regenOnKillComboReductionPerStack = clampInt(regenOnKillComboReductionPerStack, 0, 100);

        if (bonusStackingMode == null) bonusStackingMode = BonusStackingMode.MULTIPLICATIVE;
        crouchSpeedMultiplier = clampDouble(crouchSpeedMultiplier, 1.0D, 10.0D);
        crouchHealMultiplier = clampDouble(crouchHealMultiplier, 1.0D, 10.0D);
        lightLevelMinMultiplier = clampDouble(lightLevelMinMultiplier, 0.1D, 2.0D);
        lightLevelMaxMultiplier = clampDouble(lightLevelMaxMultiplier, 0.1D, 2.0D);
        dayMultiplier = clampDouble(dayMultiplier, 0.1D, 3.0D);
        nightMultiplier = clampDouble(nightMultiplier, 0.1D, 3.0D);
        peacefulMultiplier = clampDouble(peacefulMultiplier, 0.1D, 5.0D);
        easyMultiplier = clampDouble(easyMultiplier, 0.1D, 5.0D);
        normalMultiplier = clampDouble(normalMultiplier, 0.1D, 5.0D);
        hardMultiplier = clampDouble(hardMultiplier, 0.1D, 5.0D);

        largeDamageThresholdPercent = clampInt(largeDamageThresholdPercent, 1, 100);
        largeDamageCooldownMultiplier = clampDouble(largeDamageCooldownMultiplier, 1.0D, 5.0D);

        campfireRadius = clampInt(campfireRadius, 1, 32);
        campfireSpeedMultiplier = clampDouble(campfireSpeedMultiplier, 1.0D, 10.0D);
        campfireHealMultiplier = clampDouble(campfireHealMultiplier, 1.0D, 10.0D);
        campfireCooldownReductionPercent = clampInt(campfireCooldownReductionPercent, 0, 100);
        freezingPenaltyThresholdPercent = clampDouble(freezingPenaltyThresholdPercent, 0.0D, 1.0D);
        freezingSpeedMultiplier = clampDouble(freezingSpeedMultiplier, 0.01D, 1.0D);
        freezingHealMultiplier = clampDouble(freezingHealMultiplier, 0.01D, 1.0D);
        freezingCooldownMultiplier = clampDouble(freezingCooldownMultiplier, 1.0D, 10.0D);
    }

    private static String toJsonArray(String[] values) {
        if (values == null || values.length == 0) return "[]";
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            if (i > 0) builder.append(", ");
            builder.append("\"").append(escape(values[i])).append("\"");
        }
        builder.append("]");
        return builder.toString();
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
