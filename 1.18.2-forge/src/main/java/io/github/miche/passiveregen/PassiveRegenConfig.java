package io.github.miche.passiveregen;

import java.util.Collections;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;

public final class PassiveRegenConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue RAMP_UP_ENABLED;
    public static final ForgeConfigSpec.IntValue DAMAGE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MINIMUM_HUNGER_PERCENT;
    public static final ForgeConfigSpec.DoubleValue MINIMUM_SATURATION_LEVEL;
    public static final ForgeConfigSpec.IntValue UPDATE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue BASE_HEAL_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue FULL_STRENGTH_HEAL_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue RAMP_FULL_STRENGTH_TICKS;
    public static final ForgeConfigSpec.DoubleValue HEAL_AMOUNT_PER_TRIGGER;

    public static final ForgeConfigSpec.BooleanValue SCALE_WITH_MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue MAX_HEALTH_SCALING_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue MAX_HEALTH_SCALING_CAP;
    public static final ForgeConfigSpec.IntValue MAX_REGEN_HEALTH_PERCENT;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCKED_EFFECTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DIMENSION_BLACKLIST;
    public static final ForgeConfigSpec.IntValue PVP_DAMAGE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.BooleanValue DISABLE_NATURAL_REGEN;
    public static final ForgeConfigSpec.BooleanValue REGEN_WHILE_SPRINTING;

    public static final ForgeConfigSpec.BooleanValue HUNGER_BONUS_ENABLED;
    public static final ForgeConfigSpec.IntValue HUNGER_BONUS_THRESHOLD_PERCENT;
    public static final ForgeConfigSpec.DoubleValue HUNGER_BONUS_HEAL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HUNGER_BONUS_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.IntValue HUNGER_BONUS_COOLDOWN_REDUCTION;
    public static final ForgeConfigSpec.BooleanValue HUNGER_PENALTY_ENABLED;
    public static final ForgeConfigSpec.DoubleValue HUNGER_PENALTY_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HUNGER_PENALTY_HEAL_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue HUNGER_FULL_BONUS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue HUNGER_FULL_BONUS_HEAL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HUNGER_FULL_BONUS_SPEED_MULTIPLIER;

    public static final ForgeConfigSpec.BooleanValue SATURATION_BONUS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue SATURATION_BONUS_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SATURATION_BONUS_DEACTIVATE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue SATURATION_BONUS_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SATURATION_BONUS_HEAL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue SATURATION_BONUS_COST_PER_HP;
    public static final ForgeConfigSpec.DoubleValue SATURATION_BONUS_IDLE_DRAIN_PER_TICK;
    public static final ForgeConfigSpec.DoubleValue SATURATION_BONUS_MIN_SATURATION_FLOOR;
    public static final ForgeConfigSpec.DoubleValue SATURATION_BONUS_FLAT_HEAL_BONUS;
    public static final ForgeConfigSpec.BooleanValue SATURATION_BONUS_SCALE_BY_EXCESS;

    public static final ForgeConfigSpec.BooleanValue DISABLE_HEALING_DURING_POISON;
    public static final ForgeConfigSpec.BooleanValue DISABLE_HEALING_DURING_WITHER;

    public static final ForgeConfigSpec.BooleanValue REGEN_ON_KILL_ENABLED;
    public static final ForgeConfigSpec.IntValue REGEN_ON_KILL_COOLDOWN_REDUCTION;
    public static final ForgeConfigSpec.BooleanValue REGEN_ON_KILL_HOSTILE_ONLY;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> REGEN_ON_KILL_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue REGEN_ON_KILL_COMBO_ENABLED;
    public static final ForgeConfigSpec.IntValue REGEN_ON_KILL_COMBO_WINDOW_TICKS;
    public static final ForgeConfigSpec.IntValue REGEN_ON_KILL_COMBO_MAX_STACKS;
    public static final ForgeConfigSpec.IntValue REGEN_ON_KILL_COMBO_REDUCTION_PER_STACK;

    public static final ForgeConfigSpec.EnumValue<BonusStackingMode> BONUS_STACKING_MODE;
    public static final ForgeConfigSpec.BooleanValue CROUCH_BONUS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue CROUCH_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CROUCH_HEAL_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue LIGHT_LEVEL_BONUS_ENABLED;
    public static final ForgeConfigSpec.DoubleValue LIGHT_LEVEL_MIN_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue LIGHT_LEVEL_MAX_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue DAY_NIGHT_MULTIPLIER_ENABLED;
    public static final ForgeConfigSpec.DoubleValue DAY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue NIGHT_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue DIFFICULTY_SCALING_ENABLED;
    public static final ForgeConfigSpec.DoubleValue PEACEFUL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue EASY_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue NORMAL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue HARD_MULTIPLIER;

    public static final ForgeConfigSpec.BooleanValue LARGE_DAMAGE_PENALTY_ENABLED;
    public static final ForgeConfigSpec.IntValue LARGE_DAMAGE_THRESHOLD_PERCENT;
    public static final ForgeConfigSpec.DoubleValue LARGE_DAMAGE_COOLDOWN_MULTIPLIER;

    public static final ForgeConfigSpec.BooleanValue CAMPFIRE_REGEN_ENABLED;
    public static final ForgeConfigSpec.IntValue CAMPFIRE_RADIUS;
    public static final ForgeConfigSpec.DoubleValue CAMPFIRE_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue CAMPFIRE_HEAL_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue CAMPFIRE_COOLDOWN_REDUCTION_ENABLED;
    public static final ForgeConfigSpec.IntValue CAMPFIRE_COOLDOWN_REDUCTION_PERCENT;

    public static final ForgeConfigSpec.BooleanValue FREEZING_PENALTY_ENABLED;
    public static final ForgeConfigSpec.DoubleValue FREEZING_PENALTY_THRESHOLD_PERCENT;
    public static final ForgeConfigSpec.DoubleValue FREEZING_SPEED_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FREEZING_HEAL_MULTIPLIER;
    public static final ForgeConfigSpec.DoubleValue FREEZING_COOLDOWN_MULTIPLIER;
    public static final ForgeConfigSpec.BooleanValue FREEZING_BLOCKS_REGEN;

    public enum BonusStackingMode {
        MULTIPLICATIVE,
        ADDITIVE,
        STRONGEST_ONLY
    }

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");
        ENABLED = builder.define("enabled", true);
        DAMAGE_COOLDOWN_TICKS = builder.defineInRange("damageCooldownTicks", 100, 0, 12000);
        MINIMUM_HUNGER_PERCENT = builder.defineInRange("minimumHungerPercent", 50, 0, 100);
        MINIMUM_SATURATION_LEVEL = builder.defineInRange("minimumSaturationLevel", 0.0D, 0.0D, 20.0D);
        UPDATE_INTERVAL_TICKS = builder.defineInRange("updateIntervalTicks", 20, 1, 200);
        BASE_HEAL_INTERVAL_TICKS = builder.defineInRange("baseHealIntervalTicks", 100, 1, 12000);
        RAMP_UP_ENABLED = builder.define("rampUpEnabled", false);
        FULL_STRENGTH_HEAL_INTERVAL_TICKS = builder.defineInRange("fullStrengthHealIntervalTicks", 50, 1, 12000);
        RAMP_FULL_STRENGTH_TICKS = builder.defineInRange("rampFullStrengthTicks", 600, 1, 12000);
        HEAL_AMOUNT_PER_TRIGGER = builder.defineInRange("healAmountPerTrigger", 0.5D, 0.01D, 100.0D);
        builder.pop();

        builder.push("limits");
        SCALE_WITH_MAX_HEALTH = builder.define("scaleWithMaxHealth", false);
        MAX_HEALTH_SCALING_EXPONENT = builder.defineInRange("maxHealthScalingExponent", 0.5D, 0.1D, 4.0D);
        MAX_HEALTH_SCALING_CAP = builder.defineInRange("maxHealthScalingCap", 2.0D, 1.0D, 100.0D);
        MAX_REGEN_HEALTH_PERCENT = builder.defineInRange("maxRegenHealthPercent", 100, 0, 100);
        BLOCKED_EFFECTS = builder.defineList("blockedEffects", Collections.emptyList(), PassiveRegenConfig::isString);
        DIMENSION_BLACKLIST = builder.defineList("dimensionBlacklist", Collections.emptyList(), PassiveRegenConfig::isString);
        PVP_DAMAGE_COOLDOWN_TICKS = builder.defineInRange("pvpDamageCooldownTicks", -1, -1, 12000);
        DISABLE_NATURAL_REGEN = builder.define("disableNaturalRegen", false);
        REGEN_WHILE_SPRINTING = builder.define("regenWhileSprinting", true);
        builder.pop();

        builder.push("hungerBonus");
        HUNGER_BONUS_ENABLED = builder.define("hungerBonusEnabled", false);
        HUNGER_BONUS_THRESHOLD_PERCENT = builder.defineInRange("hungerBonusThresholdPercent", 75, 0, 100);
        HUNGER_BONUS_HEAL_MULTIPLIER = builder.defineInRange("hungerBonusHealMultiplier", 1.5D, 1.0D, 100.0D);
        HUNGER_BONUS_SPEED_MULTIPLIER = builder.defineInRange("hungerBonusSpeedMultiplier", 1.5D, 1.0D, 100.0D);
        HUNGER_BONUS_COOLDOWN_REDUCTION = builder.defineInRange("hungerBonusCooldownReduction", 25, 0, 100);
        HUNGER_PENALTY_ENABLED = builder.define("hungerPenaltyEnabled", false);
        HUNGER_PENALTY_SPEED_MULTIPLIER = builder.defineInRange("hungerPenaltySpeedMultiplier", 0.25D, 0.01D, 1.0D);
        HUNGER_PENALTY_HEAL_MULTIPLIER = builder.defineInRange("hungerPenaltyHealMultiplier", 1.0D, 0.01D, 1.0D);
        HUNGER_FULL_BONUS_ENABLED = builder.define("hungerFullBonusEnabled", false);
        HUNGER_FULL_BONUS_HEAL_MULTIPLIER = builder.defineInRange("hungerFullBonusHealMultiplier", 2.0D, 1.0D, 100.0D);
        HUNGER_FULL_BONUS_SPEED_MULTIPLIER = builder.defineInRange("hungerFullBonusSpeedMultiplier", 2.0D, 1.0D, 100.0D);
        builder.pop();

        builder.push("saturationBonus");
        SATURATION_BONUS_ENABLED = builder.define("saturationBonusEnabled", true);
        SATURATION_BONUS_THRESHOLD = builder.defineInRange("saturationBonusThreshold", 10.0D, 0.0D, 20.0D);
        SATURATION_BONUS_DEACTIVATE_THRESHOLD = builder.defineInRange("saturationBonusDeactivateThreshold", 10.0D, 0.0D, 20.0D);
        SATURATION_BONUS_SPEED_MULTIPLIER = builder.defineInRange("saturationBonusSpeedMultiplier", 2.0D, 1.0D, 10.0D);
        SATURATION_BONUS_HEAL_MULTIPLIER = builder.defineInRange("saturationBonusHealMultiplier", 2.0D, 1.0D, 10.0D);
        SATURATION_BONUS_COST_PER_HP = builder.defineInRange("saturationBonusCostPerHp", 1.0D, 0.0D, 10.0D);
        SATURATION_BONUS_IDLE_DRAIN_PER_TICK = builder.defineInRange("saturationBonusIdleDrainPerTick", 0.0D, 0.0D, 1.0D);
        SATURATION_BONUS_MIN_SATURATION_FLOOR = builder.defineInRange("saturationBonusMinSaturationFloor", 0.0D, 0.0D, 20.0D);
        SATURATION_BONUS_FLAT_HEAL_BONUS = builder.defineInRange("saturationBonusFlatHealBonus", 0.25D, 0.0D, 10.0D);
        SATURATION_BONUS_SCALE_BY_EXCESS = builder.define("saturationBonusScaleByExcess", false);
        builder.pop();

        builder.push("statusEffects");
        DISABLE_HEALING_DURING_POISON = builder.define("disableHealingDuringPoison", true);
        DISABLE_HEALING_DURING_WITHER = builder.define("disableHealingDuringWither", true);
        builder.pop();

        builder.push("regenOnKill");
        REGEN_ON_KILL_ENABLED = builder.define("regenOnKillEnabled", false);
        REGEN_ON_KILL_COOLDOWN_REDUCTION = builder.defineInRange("regenOnKillCooldownReduction", 50, 0, 100);
        REGEN_ON_KILL_HOSTILE_ONLY = builder.define("regenOnKillHostileOnly", false);
        REGEN_ON_KILL_BLACKLIST = builder.defineList("regenOnKillBlacklist", Collections.emptyList(), PassiveRegenConfig::isString);
        REGEN_ON_KILL_COMBO_ENABLED = builder.define("regenOnKillComboEnabled", false);
        REGEN_ON_KILL_COMBO_WINDOW_TICKS = builder.defineInRange("regenOnKillComboWindowTicks", 200, 20, 1200);
        REGEN_ON_KILL_COMBO_MAX_STACKS = builder.defineInRange("regenOnKillComboMaxStacks", 5, 1, 20);
        REGEN_ON_KILL_COMBO_REDUCTION_PER_STACK = builder.defineInRange("regenOnKillComboReductionPerStack", 10, 0, 100);
        builder.pop();

        builder.push("bonuses");
        BONUS_STACKING_MODE = builder.defineEnum("bonusStackingMode", BonusStackingMode.MULTIPLICATIVE);
        CROUCH_BONUS_ENABLED = builder.define("crouchBonusEnabled", false);
        CROUCH_SPEED_MULTIPLIER = builder.defineInRange("crouchSpeedMultiplier", 1.5D, 1.0D, 10.0D);
        CROUCH_HEAL_MULTIPLIER = builder.defineInRange("crouchHealMultiplier", 1.0D, 1.0D, 10.0D);
        LIGHT_LEVEL_BONUS_ENABLED = builder.define("lightLevelBonusEnabled", false);
        LIGHT_LEVEL_MIN_MULTIPLIER = builder.defineInRange("lightLevelMinMultiplier", 0.75D, 0.1D, 2.0D);
        LIGHT_LEVEL_MAX_MULTIPLIER = builder.defineInRange("lightLevelMaxMultiplier", 1.25D, 0.1D, 2.0D);
        DAY_NIGHT_MULTIPLIER_ENABLED = builder.define("dayNightMultiplierEnabled", false);
        DAY_MULTIPLIER = builder.defineInRange("dayMultiplier", 1.25D, 0.1D, 3.0D);
        NIGHT_MULTIPLIER = builder.defineInRange("nightMultiplier", 0.75D, 0.1D, 3.0D);
        DIFFICULTY_SCALING_ENABLED = builder.define("difficultyScalingEnabled", false);
        PEACEFUL_MULTIPLIER = builder.defineInRange("peacefulMultiplier", 2.0D, 0.1D, 5.0D);
        EASY_MULTIPLIER = builder.defineInRange("easyMultiplier", 1.25D, 0.1D, 5.0D);
        NORMAL_MULTIPLIER = builder.defineInRange("normalMultiplier", 1.0D, 0.1D, 5.0D);
        HARD_MULTIPLIER = builder.defineInRange("hardMultiplier", 0.75D, 0.1D, 5.0D);
        builder.pop();

        builder.push("largeDamagePenalty");
        LARGE_DAMAGE_PENALTY_ENABLED = builder.define("largeDamagePenaltyEnabled", false);
        LARGE_DAMAGE_THRESHOLD_PERCENT = builder.defineInRange("largeDamageThresholdPercent", 50, 1, 100);
        LARGE_DAMAGE_COOLDOWN_MULTIPLIER = builder.defineInRange("largeDamageCooldownMultiplier", 1.5D, 1.0D, 5.0D);
        builder.pop();

        builder.push("campfire");
        CAMPFIRE_REGEN_ENABLED = builder.define("campfireRegenEnabled", true);
        CAMPFIRE_RADIUS = builder.defineInRange("campfireRadius", 8, 1, 32);
        CAMPFIRE_SPEED_MULTIPLIER = builder.defineInRange("campfireSpeedMultiplier", 2.0D, 1.0D, 10.0D);
        CAMPFIRE_HEAL_MULTIPLIER = builder.defineInRange("campfireHealMultiplier", 1.0D, 1.0D, 10.0D);
        CAMPFIRE_COOLDOWN_REDUCTION_ENABLED = builder.define("campfireCooldownReductionEnabled", false);
        CAMPFIRE_COOLDOWN_REDUCTION_PERCENT = builder.defineInRange("campfireCooldownReductionPercent", 20, 0, 100);
        builder.pop();

        builder.push("freezing");
        FREEZING_PENALTY_ENABLED = builder.define("freezingPenaltyEnabled", true);
        FREEZING_PENALTY_THRESHOLD_PERCENT = builder.defineInRange("freezingPenaltyThresholdPercent", 0.0D, 0.0D, 1.0D);
        FREEZING_SPEED_MULTIPLIER = builder.defineInRange("freezingSpeedMultiplier", 0.5D, 0.01D, 1.0D);
        FREEZING_HEAL_MULTIPLIER = builder.defineInRange("freezingHealMultiplier", 0.75D, 0.01D, 1.0D);
        FREEZING_COOLDOWN_MULTIPLIER = builder.defineInRange("freezingCooldownMultiplier", 1.75D, 1.0D, 10.0D);
        FREEZING_BLOCKS_REGEN = builder.define("freezingBlocksRegen", false);
        builder.pop();

        SPEC = builder.build();
    }

    private PassiveRegenConfig() {
    }

    public static int getMinimumFoodLevel() {
        return (int) Math.ceil(clampPercent(MINIMUM_HUNGER_PERCENT.get()) / 100.0D * 20.0D);
    }

    public static int getHungerBonusThresholdFoodLevel() {
        return (int) Math.ceil(clampPercent(HUNGER_BONUS_THRESHOLD_PERCENT.get()) / 100.0D * 20.0D);
    }

    public static int getEffectiveDamageCooldown(int foodLevel) {
        int base = Math.max(0, DAMAGE_COOLDOWN_TICKS.get());
        if (!HUNGER_BONUS_ENABLED.get()) {
            return base;
        }
        if (foodLevel < getHungerBonusThresholdFoodLevel()) {
            return base;
        }
        int reduction = clampInt(HUNGER_BONUS_COOLDOWN_REDUCTION.get(), 0, 100);
        return (int) (base * (1.0D - reduction / 100.0D));
    }

    public static double getHungerHealMultiplier(int foodLevel) {
        if (!HUNGER_BONUS_ENABLED.get()) {
            return 1.0D;
        }
        if (HUNGER_FULL_BONUS_ENABLED.get() && foodLevel >= 20) {
            return Math.max(1.0D, HUNGER_FULL_BONUS_HEAL_MULTIPLIER.get());
        }
        if (foodLevel >= getHungerBonusThresholdFoodLevel()) {
            return Math.max(1.0D, HUNGER_BONUS_HEAL_MULTIPLIER.get());
        }
        return 1.0D;
    }

    public static double getHungerSpeedMultiplier(int foodLevel) {
        if (!HUNGER_BONUS_ENABLED.get()) {
            return 1.0D;
        }
        if (HUNGER_FULL_BONUS_ENABLED.get() && foodLevel >= 20) {
            return Math.max(1.0D, HUNGER_FULL_BONUS_SPEED_MULTIPLIER.get());
        }
        if (foodLevel >= getHungerBonusThresholdFoodLevel()) {
            return Math.max(1.0D, HUNGER_BONUS_SPEED_MULTIPLIER.get());
        }
        return 1.0D;
    }

    public static double combineBonusMultipliers(List<Double> multipliers) {
        if (multipliers == null || multipliers.isEmpty()) {
            return 1.0D;
        }

        return switch (BONUS_STACKING_MODE.get()) {
            case ADDITIVE -> {
                double result = 1.0D;
                for (double multiplier : multipliers) {
                    result += multiplier - 1.0D;
                }
                yield Math.max(0.0D, result);
            }
            case STRONGEST_ONLY -> {
                double strongest = 1.0D;
                for (double multiplier : multipliers) {
                    strongest = Math.max(strongest, multiplier);
                }
                yield strongest;
            }
            case MULTIPLICATIVE -> {
                double result = 1.0D;
                for (double multiplier : multipliers) {
                    result *= multiplier;
                }
                yield Math.max(0.0D, result);
            }
        };
    }

    public static double getCurrentHealIntervalTicks(long outOfCombatTicks) {
        int baseTicks = Math.max(1, BASE_HEAL_INTERVAL_TICKS.get());
        if (!RAMP_UP_ENABLED.get()) {
            return baseTicks;
        }
        int fullTicks = Math.max(1, FULL_STRENGTH_HEAL_INTERVAL_TICKS.get());
        int rampTicks = Math.max(1, RAMP_FULL_STRENGTH_TICKS.get());
        double progress = Math.min(1.0D, (double) outOfCombatTicks / rampTicks);
        return baseTicks + (fullTicks - baseTicks) * progress;
    }

    public static double getMaxHealthScaleMultiplier(float maxHealth) {
        if (!SCALE_WITH_MAX_HEALTH.get() || maxHealth <= 20.0F) {
            return 1.0D;
        }
        double normalized = Math.max(1.0D, maxHealth / 20.0D);
        double exponent = Math.max(0.1D, MAX_HEALTH_SCALING_EXPONENT.get());
        double multiplier = Math.pow(normalized, exponent);
        double cap = Math.max(1.0D, MAX_HEALTH_SCALING_CAP.get());
        return Math.min(cap, multiplier);
    }

    private static boolean isString(Object value) {
        return value instanceof String;
    }

    private static int clampPercent(int value) {
        return clampInt(value, 0, 100);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
