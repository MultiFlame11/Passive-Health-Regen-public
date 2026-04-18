package io.github.miche.passiveregen;

import net.neoforged.neoforge.common.ModConfigSpec;

public class PassiveRegenConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue RAMP_UP_ENABLED;
    public static final ModConfigSpec.IntValue DAMAGE_COOLDOWN_TICKS;
    public static final ModConfigSpec.IntValue MINIMUM_HUNGER_PERCENT;
    public static final ModConfigSpec.IntValue UPDATE_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue BASE_HEAL_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue FULL_STRENGTH_HEAL_INTERVAL_TICKS;
    public static final ModConfigSpec.IntValue RAMP_FULL_STRENGTH_TICKS;
    public static final ModConfigSpec.DoubleValue HEAL_AMOUNT_PER_TRIGGER;
    public static final ModConfigSpec.BooleanValue SCALE_WITH_MAX_HEALTH;
    public static final ModConfigSpec.DoubleValue MAX_HEALTH_SCALING_EXPONENT;
    public static final ModConfigSpec.DoubleValue MAX_HEALTH_SCALING_CAP;
    public static final ModConfigSpec.IntValue MAX_REGEN_HEALTH_PERCENT;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> BLOCKED_EFFECTS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> DIMENSION_BLACKLIST;
    public static final ModConfigSpec.IntValue PVP_DAMAGE_COOLDOWN_TICKS;

    // ── Hunger Bonus ────────────────────────────────────────────

    public static final ModConfigSpec.BooleanValue HUNGER_BONUS_ENABLED;
    public static final ModConfigSpec.IntValue HUNGER_BONUS_THRESHOLD_PERCENT;
    public static final ModConfigSpec.DoubleValue HUNGER_BONUS_HEAL_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HUNGER_BONUS_SPEED_MULTIPLIER;
    public static final ModConfigSpec.IntValue HUNGER_BONUS_COOLDOWN_REDUCTION;
    public static final ModConfigSpec.BooleanValue HUNGER_FULL_BONUS_ENABLED;
    public static final ModConfigSpec.DoubleValue HUNGER_FULL_BONUS_HEAL_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HUNGER_FULL_BONUS_SPEED_MULTIPLIER;

    // ── Regen on Kill ───────────────────────────────────────────

    public static final ModConfigSpec.BooleanValue REGEN_ON_KILL_ENABLED;
    public static final ModConfigSpec.IntValue REGEN_ON_KILL_COOLDOWN_REDUCTION;

    // ── Natural Regen ───────────────────────────────────────────

    public static final ModConfigSpec.BooleanValue DISABLE_NATURAL_REGEN;

    // ── Sprinting ────────────────────────────────────────────────

    public static final ModConfigSpec.BooleanValue REGEN_WHILE_SPRINTING;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("general");

        ENABLED = builder
            .comment("Set to false to completely disable passive regeneration.")
            .define("enabled", true);

        RAMP_UP_ENABLED = builder
            .comment(
                "If true, regen starts slow and speeds up the longer you stay out of combat.",
                "At rampFullStrengthTicks out-of-combat ticks you reach peak regen speed.",
                "If false, regen always runs at the baseHealIntervalTicks rate."
            )
            .define("rampUpEnabled", false);

        DAMAGE_COOLDOWN_TICKS = builder
            .comment(
                "How many ticks after taking damage before passive regen can start.",
                "20 ticks = 1 second. Default 100 = 5 seconds.",
                "Examples: 40 = 2s,  100 = 5s (default),  200 = 10s,  600 = 30s"
            )
            .defineInRange("damageCooldownTicks", 100, 0, 12000);

        MINIMUM_HUNGER_PERCENT = builder
            .comment(
                "Minimum hunger bar % required before passive regen can occur.",
                "0 = regen works even on an empty hunger bar.",
                "50 = at least half hunger required (default).",
                "100 = hunger bar must be completely full.",
                "Examples: 0 (any hunger), 25 (quarter+), 50 (half+, default), 75 (three-quarter+), 100 (full only)"
            )
            .defineInRange("minimumHungerPercent", 50, 0, 100);

        UPDATE_INTERVAL_TICKS = builder
            .comment(
                "How often (in ticks) players are checked for passive regen.",
                "20 ticks = 1 second. Lower = more precise; higher = cheaper.",
                "Default 20 (every second). Rarely needs changing.",
                "Examples: 10 = 0.5s, 20 = 1s (default), 40 = 2s"
            )
            .defineInRange("updateIntervalTicks", 20, 1, 200);

        BASE_HEAL_INTERVAL_TICKS = builder
            .comment(
                "Ticks between each heal trigger at the base (starting) regen speed.",
                "Lower = faster regen. Vanilla Regeneration I is approximately 50 ticks.",
                "20 ticks = 1 second. Default 100 = heal once every 5 seconds.",
                "Examples: 20 = 1s (fast), 50 = 2.5s (vanilla regen I), 100 = 5s (default), 200 = 10s (slow)"
            )
            .defineInRange("baseHealIntervalTicks", 100, 1, 12000);

        FULL_STRENGTH_HEAL_INTERVAL_TICKS = builder
            .comment(
                "Ticks between each heal trigger at peak ramp-up speed (only used if rampUpEnabled = true).",
                "This is the FASTEST the regen will ever go, reached after rampFullStrengthTicks out-of-combat.",
                "Must be less than baseHealIntervalTicks for ramp-up to have any effect.",
                "Default 50 = heal once every 2.5 seconds at peak.",
                "Examples: 20 = 1s (very fast), 50 = 2.5s (default), 75 = 3.75s"
            )
            .defineInRange("fullStrengthHealIntervalTicks", 50, 1, 12000);

        RAMP_FULL_STRENGTH_TICKS = builder
            .comment(
                "Total out-of-combat ticks to reach peak regen speed (only used if rampUpEnabled = true).",
                "20 ticks = 1 second. Default 600 = 30 seconds out of combat to reach full speed.",
                "Examples: 200 = 10s, 400 = 20s, 600 = 30s (default), 1200 = 1 minute"
            )
            .defineInRange("rampFullStrengthTicks", 600, 1, 12000);

        HEAL_AMOUNT_PER_TRIGGER = builder
            .comment(
                "How much health to restore each time regen triggers.",
                "0.5 = quarter heart (default),  1.0 = half heart,  2.0 = full heart.",
                "This is before any max-health scaling."
            )
            .defineInRange("healAmountPerTrigger", 0.5D, 0.01D, 100.0D);

        SCALE_WITH_MAX_HEALTH = builder
            .comment(
                "If true, regen heals slightly more for players with more than 20 max HP.",
                "Uses a soft curve (controlled by maxHealthScalingExponent) so it does not become extreme.",
                "Useful for modpacks with high-HP bosses or modded players with extra hearts."
            )
            .define("scaleWithMaxHealth", false);

        MAX_HEALTH_SCALING_EXPONENT = builder
            .comment(
                "Controls how steeply regen scales with max HP when scaleWithMaxHealth = true.",
                "0.5 = square-root scaling (gentle, default).  1.0 = linear.  2.0 = quadratic (steep).",
                "Lower values keep scaling gentle even at very high HP pools.",
                "Example at 40 max HP (2x base): exponent 0.5 gives ~1.41x heal, 1.0 gives 2.0x heal."
            )
            .defineInRange("maxHealthScalingExponent", 0.5D, 0.1D, 4.0D);

        MAX_HEALTH_SCALING_CAP = builder
            .comment(
                "Maximum heal multiplier from max-health scaling (when scaleWithMaxHealth = true).",
                "Default 2.0 means scaling can at most double the base heal amount, no matter how high HP gets.",
                "Examples: 1.5 = 50% more at most, 2.0 = double at most (default), 3.0 = triple at most"
            )
            .defineInRange("maxHealthScalingCap", 2.0D, 1.0D, 100.0D);

        MAX_REGEN_HEALTH_PERCENT = builder
            .comment(
                "Regen stops when health reaches this percentage of max health.",
                "100 = regen all the way to full (default). 80 = stops at 80% health.",
                "Examples: 60 = stops at 60%, 80 = stops at 80%, 100 = full regen (default)"
            )
            .defineInRange("maxRegenHealthPercent", 100, 0, 100);

        BLOCKED_EFFECTS = builder
            .comment(
                "List of potion/mob effect IDs that prevent passive regen while the player has them active.",
                "Empty list = no effects block regen (default).",
                "Use namespaced IDs like minecraft:poison, minecraft:wither, minecraft:weakness.",
                "Examples: [] = nothing blocks regen,  [\"minecraft:poison\"] = paused while poisoned"
            )
            .defineList("blockedEffects", java.util.Collections.emptyList(), o -> o instanceof String);

        DIMENSION_BLACKLIST = builder
            .comment(
                "List of dimension IDs where passive regen is disabled entirely.",
                "Empty list = regen works in all dimensions (default).",
                "Use namespaced dimension IDs.",
                "Examples: [] = all dimensions,  [\"minecraft:the_nether\"] = disabled in Nether,  [\"minecraft:the_nether\", \"minecraft:the_end\"] = Nether and End"
            )
            .defineList("dimensionBlacklist", java.util.Collections.emptyList(), o -> o instanceof String);

        PVP_DAMAGE_COOLDOWN_TICKS = builder
            .comment(
                "Separate damage cooldown (in ticks) when a player hits you instead of a mob. -1 = same as damageCooldownTicks (default).",
                "20 ticks = 1 second. Set higher to delay regen longer after PvP hits.",
                "Examples: -1 = same as regular (default),  200 = 10s,  400 = 20s,  600 = 30s"
            )
            .defineInRange("pvpDamageCooldownTicks", -1, -1, 12000);

        builder.pop();

        builder.push("hungerBonus");

        HUNGER_BONUS_ENABLED = builder
            .comment("Master toggle for the hunger bonus system. When enabled, high hunger boosts regen.")
            .define("hungerBonusEnabled", false);

        HUNGER_BONUS_THRESHOLD_PERCENT = builder
            .comment(
                "Hunger % threshold to trigger the hunger bonus (0-100). Default 75.",
                "Example: 75 = bonus kicks in when hunger bar is at least 75% full."
            )
            .defineInRange("hungerBonusThresholdPercent", 75, 0, 100);

        HUNGER_BONUS_HEAL_MULTIPLIER = builder
            .comment(
                "Heal amount multiplier applied when hunger exceeds hungerBonusThresholdPercent.",
                "Default 1.5 = 50% more healing."
            )
            .defineInRange("hungerBonusHealMultiplier", 1.5D, 1.0D, 100.0D);

        HUNGER_BONUS_SPEED_MULTIPLIER = builder
            .comment(
                "Heal speed multiplier (reduces effective heal interval) when hunger exceeds threshold.",
                "Default 1.5 = regen ticks 50% faster."
            )
            .defineInRange("hungerBonusSpeedMultiplier", 1.5D, 1.0D, 100.0D);

        HUNGER_BONUS_COOLDOWN_REDUCTION = builder
            .comment(
                "Percentage to reduce the damage cooldown by when hunger exceeds threshold (0-100).",
                "Default 25 = cooldown is 25% shorter. 0 = no cooldown reduction."
            )
            .defineInRange("hungerBonusCooldownReduction", 25, 0, 100);

        HUNGER_FULL_BONUS_ENABLED = builder
            .comment("Second-tier bonus at 100% hunger (full bar). Off by default.")
            .define("hungerFullBonusEnabled", false);

        HUNGER_FULL_BONUS_HEAL_MULTIPLIER = builder
            .comment(
                "Heal amount multiplier at full hunger (overrides hungerBonusHealMultiplier when hunger = 20/20).",
                "Default 2.0 = double healing at full hunger."
            )
            .defineInRange("hungerFullBonusHealMultiplier", 2.0D, 1.0D, 100.0D);

        HUNGER_FULL_BONUS_SPEED_MULTIPLIER = builder
            .comment(
                "Heal speed multiplier at full hunger (overrides hungerBonusSpeedMultiplier when hunger = 20/20).",
                "Default 2.0 = regen ticks twice as fast at full hunger."
            )
            .defineInRange("hungerFullBonusSpeedMultiplier", 2.0D, 1.0D, 100.0D);

        builder.pop();

        builder.push("regenOnKill");

        REGEN_ON_KILL_ENABLED = builder
            .comment("If true, killing an entity reduces the player's damage cooldown.")
            .define("regenOnKillEnabled", false);

        REGEN_ON_KILL_COOLDOWN_REDUCTION = builder
            .comment(
                "Percentage of the remaining damage cooldown to remove on kill (0-100).",
                "100 = fully clears the cooldown (regen starts immediately after a kill).",
                "Default 50 = halves the remaining cooldown."
            )
            .defineInRange("regenOnKillCooldownReduction", 50, 0, 100);

        builder.pop();

        builder.push("naturalRegen");

        DISABLE_NATURAL_REGEN = builder
            .comment("If true, vanilla natural regeneration is disabled.")
            .define("disableNaturalRegen", false);

        builder.pop();

        builder.push("sprinting");

        REGEN_WHILE_SPRINTING = builder
            .comment("If false, passive regen is paused while the player is sprinting.")
            .define("regenWhileSprinting", true);

        builder.pop();
        SPEC = builder.build();
    }

    public static float getHealAmountPerUpdate(long outOfCombatTicks, float maxHealth, int foodLevel) {
        if (!ENABLED.get()) {
            return 0.0F;
        }

        double healAmount = Math.max(0.01D, HEAL_AMOUNT_PER_TRIGGER.get());
        double scaledHeal = healAmount * getMaxHealthScaleMultiplier(maxHealth);

        int updateTicks = Math.max(1, UPDATE_INTERVAL_TICKS.get());
        double currentHealInterval = getCurrentHealIntervalTicks(outOfCombatTicks, foodLevel);

        double healMult = getHungerHealMultiplier(foodLevel);
        return (float) (scaledHeal * healMult * updateTicks / currentHealInterval);
    }

    public static int getEffectiveDamageCooldown(int foodLevel) {
        int base = Math.max(0, DAMAGE_COOLDOWN_TICKS.get());
        if (!HUNGER_BONUS_ENABLED.get()) return base;
        int threshold = (int) Math.ceil((Math.max(0, Math.min(100, HUNGER_BONUS_THRESHOLD_PERCENT.get())) / 100.0D) * 20.0D);
        if (foodLevel < threshold) return base;
        int reduction = Math.max(0, Math.min(100, HUNGER_BONUS_COOLDOWN_REDUCTION.get()));
        return (int) (base * (1.0D - reduction / 100.0D));
    }

    private static double getHungerHealMultiplier(int foodLevel) {
        if (!HUNGER_BONUS_ENABLED.get()) return 1.0D;
        if (HUNGER_FULL_BONUS_ENABLED.get() && foodLevel >= 20) return Math.max(1.0D, HUNGER_FULL_BONUS_HEAL_MULTIPLIER.get());
        int threshold = (int) Math.ceil((Math.max(0, Math.min(100, HUNGER_BONUS_THRESHOLD_PERCENT.get())) / 100.0D) * 20.0D);
        if (foodLevel >= threshold) return Math.max(1.0D, HUNGER_BONUS_HEAL_MULTIPLIER.get());
        return 1.0D;
    }

    private static double getHungerSpeedMultiplier(int foodLevel) {
        if (!HUNGER_BONUS_ENABLED.get()) return 1.0D;
        if (HUNGER_FULL_BONUS_ENABLED.get() && foodLevel >= 20) return Math.max(1.0D, HUNGER_FULL_BONUS_SPEED_MULTIPLIER.get());
        int threshold = (int) Math.ceil((Math.max(0, Math.min(100, HUNGER_BONUS_THRESHOLD_PERCENT.get())) / 100.0D) * 20.0D);
        if (foodLevel >= threshold) return Math.max(1.0D, HUNGER_BONUS_SPEED_MULTIPLIER.get());
        return 1.0D;
    }

    private static double getCurrentHealIntervalTicks(long outOfCombatTicks, int foodLevel) {
        int baseTicks = Math.max(1, BASE_HEAL_INTERVAL_TICKS.get());
        double interval;
        if (!RAMP_UP_ENABLED.get()) {
            interval = baseTicks;
        } else {
            int fullTicks = Math.max(1, FULL_STRENGTH_HEAL_INTERVAL_TICKS.get());
            int rampTicks = Math.max(1, RAMP_FULL_STRENGTH_TICKS.get());
            double progress = Math.min(1.0D, (double) outOfCombatTicks / rampTicks);
            // interpolate from baseTicks down to fullTicks as progress goes 0->1
            interval = baseTicks + (fullTicks - baseTicks) * progress;
        }
        // Apply hunger speed multiplier (divides interval = faster ticks)
        double speedMult = getHungerSpeedMultiplier(foodLevel);
        return interval / speedMult;
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
