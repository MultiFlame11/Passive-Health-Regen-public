package io.github.miche.passiveregen;

import net.minecraftforge.common.ForgeConfigSpec;

public class PassiveRegenConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.BooleanValue ENABLED;
    public static final ForgeConfigSpec.BooleanValue RAMP_UP_ENABLED;
    public static final ForgeConfigSpec.IntValue DAMAGE_COOLDOWN_TICKS;
    public static final ForgeConfigSpec.IntValue MINIMUM_HUNGER_PERCENT;
    public static final ForgeConfigSpec.IntValue UPDATE_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue BASE_HEAL_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue FULL_STRENGTH_HEAL_INTERVAL_TICKS;
    public static final ForgeConfigSpec.IntValue RAMP_FULL_STRENGTH_TICKS;
    public static final ForgeConfigSpec.DoubleValue HEAL_AMOUNT_PER_TRIGGER;
    public static final ForgeConfigSpec.BooleanValue SCALE_WITH_MAX_HEALTH;
    public static final ForgeConfigSpec.DoubleValue MAX_HEALTH_SCALING_EXPONENT;
    public static final ForgeConfigSpec.DoubleValue MAX_HEALTH_SCALING_CAP;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        ENABLED = builder
            .comment("Set to false to disable passive regeneration entirely.")
            .define("enabled", true);

        RAMP_UP_ENABLED = builder
            .comment("If true, the heal rate ramps from the base rate up to the full-strength rate over time out of combat.")
            .define("rampUpEnabled", false);

        DAMAGE_COOLDOWN_TICKS = builder
            .comment("How long after taking damage before passive regeneration can begin.")
            .defineInRange("damageCooldownTicks", 100, 0, 12000);

        MINIMUM_HUNGER_PERCENT = builder
            .comment(
                "Minimum hunger fullness percent required before passive regeneration can occur.",
                "0 means passive regeneration can still work at an empty hunger bar.",
                "50 means at least half full, and 100 means a completely full hunger bar is required."
            )
            .defineInRange("minimumHungerPercent", 50, 0, 100);

        UPDATE_INTERVAL_TICKS = builder
            .comment("How often players are checked for passive regeneration. Lower is more precise; higher is cheaper.")
            .defineInRange("updateIntervalTicks", 20, 1, 200);

        BASE_HEAL_INTERVAL_TICKS = builder
            .comment("Ticks per 1 health healed at the base passive rate. Vanilla Regeneration I is about 50.")
            .defineInRange("baseHealIntervalTicks", 100, 1, 12000);

        FULL_STRENGTH_HEAL_INTERVAL_TICKS = builder
            .comment("Ticks per 1 health healed at full ramp strength.")
            .defineInRange("fullStrengthHealIntervalTicks", 50, 1, 12000);

        RAMP_FULL_STRENGTH_TICKS = builder
            .comment("Total out-of-combat ticks required to reach the full-strength heal rate when ramp-up is enabled.")
            .defineInRange("rampFullStrengthTicks", 600, 1, 12000);

        HEAL_AMOUNT_PER_TRIGGER = builder
            .comment("How much health to heal each time passive regeneration triggers. 0.5 = quarter heart, 1.0 = half a heart, 2.0 = full heart.")
            .defineInRange("healAmountPerTrigger", 0.5D, 0.01D, 100.0D);

        SCALE_WITH_MAX_HEALTH = builder
            .comment("If true, passive regeneration scales gently with max health above 20 HP.")
            .define("scaleWithMaxHealth", false);

        MAX_HEALTH_SCALING_EXPONENT = builder
            .comment("Exponent used for max-health scaling when enabled. 0.5 is square-root scaling.")
            .defineInRange("maxHealthScalingExponent", 0.5D, 0.1D, 4.0D);

        MAX_HEALTH_SCALING_CAP = builder
            .comment("Maximum multiplier allowed from max-health scaling.")
            .defineInRange("maxHealthScalingCap", 2.0D, 1.0D, 100.0D);

        builder.pop();
        SPEC = builder.build();
    }

    public static float getHealAmountPerUpdate(long outOfCombatTicks, float maxHealth) {
        if (!ENABLED.get()) {
            return 0.0F;
        }

        double healAmount = Math.max(0.01D, HEAL_AMOUNT_PER_TRIGGER.get());
        return (float) (healAmount * getMaxHealthScaleMultiplier(maxHealth));
    }

    public static int getMinimumFoodLevel() {
        return (int) Math.ceil((Math.max(0, Math.min(100, MINIMUM_HUNGER_PERCENT.get())) / 100.0D) * 20.0D);
    }

    private static double getMaxHealthScaleMultiplier(float maxHealth) {
        if (!SCALE_WITH_MAX_HEALTH.get() || maxHealth <= 20.0F) {
            return 1.0D;
        }

        double normalized = Math.max(1.0D, maxHealth / 20.0D);
        double exponent = Math.max(0.1D, MAX_HEALTH_SCALING_EXPONENT.get());
        double multiplier = Math.pow(normalized, exponent);
        double cap = Math.max(1.0D, MAX_HEALTH_SCALING_CAP.get());
        return Math.min(cap, multiplier);
    }
}
