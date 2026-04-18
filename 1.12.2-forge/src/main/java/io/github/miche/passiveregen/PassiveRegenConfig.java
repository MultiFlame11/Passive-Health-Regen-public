package io.github.miche.passiveregen;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = PassiveRegenMod.MODID)
public class PassiveRegenConfig {
    @Config.Comment("Set to false to completely disable passive regeneration.")
    public static boolean enabled = true;

    @Config.Comment({
        "If true, regen starts slow and speeds up the longer you stay out of combat.",
        "At rampFullStrengthTicks out-of-combat ticks you reach peak regen speed.",
        "If false, regen always runs at the baseHealIntervalTicks rate."
    })
    public static boolean rampUpEnabled = false;

    @Config.Comment({
        "How many ticks after taking damage before passive regen can start.",
        "20 ticks = 1 second. Default 100 = 5 seconds.",
        "Examples: 40 = 2s,  100 = 5s (default),  200 = 10s,  600 = 30s"
    })
    @Config.RangeInt(min = 0, max = 12000)
    public static int damageCooldownTicks = 100;

    @Config.Comment({
        "Minimum hunger bar % required before passive regen can occur.",
        "0 = regen works even on an empty hunger bar.",
        "50 = at least half hunger required (default).",
        "100 = hunger bar must be completely full.",
        "Examples: 0 (any hunger), 25 (quarter+), 50 (half+, default), 75 (three-quarter+), 100 (full only)"
    })
    @Config.RangeInt(min = 0, max = 100)
    public static int minimumHungerPercent = 50;

    @Config.Comment({
        "How often (in ticks) players are checked for passive regen.",
        "20 ticks = 1 second. Lower = more precise; higher = cheaper.",
        "Default 20 (every second). Rarely needs changing.",
        "Examples: 10 = 0.5s, 20 = 1s (default), 40 = 2s"
    })
    @Config.RangeInt(min = 1, max = 200)
    public static int updateIntervalTicks = 20;

    @Config.Comment({
        "Ticks between each heal trigger at the base (starting) regen speed.",
        "Lower = faster regen. Vanilla Regeneration I is approximately 50 ticks.",
        "20 ticks = 1 second. Default 100 = heal once every 5 seconds.",
        "Examples: 20 = 1s (fast), 50 = 2.5s (vanilla regen I), 100 = 5s (default), 200 = 10s (slow)"
    })
    @Config.RangeInt(min = 1, max = 12000)
    public static int baseHealIntervalTicks = 100;

    @Config.Comment({
        "Ticks between each heal trigger at peak ramp-up speed (only used if rampUpEnabled = true).",
        "This is the FASTEST the regen will ever go, reached after rampFullStrengthTicks out-of-combat.",
        "Must be less than baseHealIntervalTicks for ramp-up to have any effect.",
        "Default 50 = heal once every 2.5 seconds at peak.",
        "Examples: 20 = 1s (very fast), 50 = 2.5s (default), 75 = 3.75s"
    })
    @Config.RangeInt(min = 1, max = 12000)
    public static int fullStrengthHealIntervalTicks = 50;

    @Config.Comment({
        "Total out-of-combat ticks to reach peak regen speed (only used if rampUpEnabled = true).",
        "20 ticks = 1 second. Default 600 = 30 seconds out of combat to reach full speed.",
        "Examples: 200 = 10s, 400 = 20s, 600 = 30s (default), 1200 = 1 minute"
    })
    @Config.RangeInt(min = 1, max = 12000)
    public static int rampFullStrengthTicks = 600;

    @Config.Comment({
        "How much health to restore each time regen triggers.",
        "0.5 = quarter heart (default),  1.0 = half heart,  2.0 = full heart.",
        "This is before any max-health scaling."
    })
    @Config.RangeDouble(min = 0.01D, max = 100.0D)
    public static double healAmountPerTrigger = 0.5D;

    @Config.Comment({
        "If true, regen heals slightly more for players with more than 20 max HP.",
        "Uses a soft curve (controlled by maxHealthScalingExponent) so it does not become extreme.",
        "Useful for modpacks with high-HP bosses or modded players with extra hearts."
    })
    public static boolean scaleWithMaxHealth = false;

    @Config.Comment({
        "Controls how steeply regen scales with max HP when scaleWithMaxHealth = true.",
        "0.5 = square-root scaling (gentle, default).  1.0 = linear.  2.0 = quadratic (steep).",
        "Lower values keep scaling gentle even at very high HP pools.",
        "Example at 40 max HP (2x base): exponent 0.5 gives ~1.41x heal, 1.0 gives 2.0x heal."
    })
    @Config.RangeDouble(min = 0.1D, max = 4.0D)
    public static double maxHealthScalingExponent = 0.5D;

    @Config.Comment({
        "Maximum heal multiplier from max-health scaling (when scaleWithMaxHealth = true).",
        "Default 2.0 means scaling can at most double the base heal amount, no matter how high HP gets.",
        "Examples: 1.5 = 50% more at most, 2.0 = double at most (default), 3.0 = triple at most"
    })
    @Config.RangeDouble(min = 1.0D, max = 100.0D)
    public static double maxHealthScalingCap = 2.0D;

    @Config.Comment({
        "Regen stops when health reaches this percentage of max health.",
        "100 = regen all the way to full (default). 80 = stops at 80% health.",
        "Examples: 60 = stops at 60%, 80 = stops at 80%, 100 = full regen (default)"
    })
    @Config.RangeInt(min = 0, max = 100)
    public static int maxRegenHealthPercent = 100;

    @Config.Comment({
        "List of potion/mob effect IDs that prevent passive regen while the player has them active.",
        "Empty list = no effects block regen (default).",
        "Use namespaced IDs like minecraft:poison, minecraft:wither.",
        "Examples: [] = nothing blocks, [\"minecraft:poison\"] = paused while poisoned"
    })
    public static String[] blockedEffects = new String[0];

    @Config.Comment({
        "List of dimension IDs where passive regen is disabled entirely.",
        "For 1.12.2, use the dimension integer as a string: \"-1\" = Nether, \"0\" = Overworld, \"1\" = End.",
        "Empty list = regen works in all dimensions (default).",
        "Examples: [] = all dimensions, [\"-1\"] = disabled in Nether"
    })
    public static String[] dimensionBlacklist = new String[0];

    @Config.Comment({
        "Separate damage cooldown (ticks) when a player hits you instead of a mob. -1 = same as damageCooldownTicks (default).",
        "20 ticks = 1 second. Set higher to delay regen longer after PvP hits.",
        "Examples: -1 = same as regular (default), 200 = 10s, 400 = 20s, 600 = 30s"
    })
    @Config.RangeInt(min = -1, max = 12000)
    public static int pvpDamageCooldownTicks = -1;

    // ── Hunger Bonus ──────────────────────────────────────────────────────────

    @Config.Comment("Master toggle for the hunger bonus system. When enabled, high hunger boosts regen.")
    public static boolean hungerBonusEnabled = false;

    @Config.Comment({
        "Hunger % threshold to trigger the hunger bonus (0-100). Default 75.",
        "Example: 75 = bonus kicks in when hunger bar is at least 75% full."
    })
    @Config.RangeInt(min = 0, max = 100)
    public static int hungerBonusThresholdPercent = 75;

    @Config.Comment({
        "Heal amount multiplier applied when hunger exceeds hungerBonusThresholdPercent.",
        "Default 1.5 = 50% more healing."
    })
    @Config.RangeDouble(min = 1.0D, max = 100.0D)
    public static double hungerBonusHealMultiplier = 1.5D;

    @Config.Comment({
        "Heal speed multiplier (reduces effective heal interval) when hunger exceeds threshold.",
        "Default 1.5 = regen ticks 50% faster."
    })
    @Config.RangeDouble(min = 1.0D, max = 100.0D)
    public static double hungerBonusSpeedMultiplier = 1.5D;

    @Config.Comment({
        "Percentage to reduce the damage cooldown by when hunger exceeds threshold (0-100).",
        "Default 25 = cooldown is 25% shorter. 0 = no cooldown reduction."
    })
    @Config.RangeInt(min = 0, max = 100)
    public static int hungerBonusCooldownReduction = 25;

    @Config.Comment("Second-tier bonus at 100% hunger (full bar). Off by default.")
    public static boolean hungerFullBonusEnabled = false;

    @Config.Comment({
        "Heal amount multiplier at full hunger (overrides hungerBonusHealMultiplier when hunger = 20/20).",
        "Default 2.0 = double healing at full hunger."
    })
    @Config.RangeDouble(min = 1.0D, max = 100.0D)
    public static double hungerFullBonusHealMultiplier = 2.0D;

    @Config.Comment({
        "Heal speed multiplier at full hunger (overrides hungerBonusSpeedMultiplier when hunger = 20/20).",
        "Default 2.0 = regen ticks twice as fast at full hunger."
    })
    @Config.RangeDouble(min = 1.0D, max = 100.0D)
    public static double hungerFullBonusSpeedMultiplier = 2.0D;

    // ── Regen on Kill ─────────────────────────────────────────────────────────

    @Config.Comment("If true, killing an entity reduces the player's damage cooldown.")
    public static boolean regenOnKillEnabled = false;

    @Config.Comment({
        "Percentage of the remaining damage cooldown to remove on kill (0-100).",
        "100 = fully clears the cooldown (regen starts immediately after a kill).",
        "Default 50 = halves the remaining cooldown."
    })
    @Config.RangeInt(min = 0, max = 100)
    public static int regenOnKillCooldownReduction = 50;

    // ── Natural Regen ─────────────────────────────────────────────────────────

    @Config.Comment("If true, vanilla natural regeneration is disabled.")
    public static boolean disableNaturalRegen = false;

    // ── Sprinting ─────────────────────────────────────────────────────────────

    @Config.Comment("If false, passive regen is paused while the player is sprinting.")
    public static boolean regenWhileSprinting = true;

    public static float getHealAmountPerUpdate(long outOfCombatTicks, float maxHealth, int foodLevel) {
        if (!enabled) {
            return 0.0F;
        }

        double healAmount = Math.max(0.01D, healAmountPerTrigger);
        double scaledHeal = healAmount * getMaxHealthScaleMultiplier(maxHealth);

        int updateTicks = Math.max(1, updateIntervalTicks);
        double currentHealInterval = getCurrentHealIntervalTicks(outOfCombatTicks, foodLevel);

        double healMult = getHungerHealMultiplier(foodLevel);
        return (float) (scaledHeal * healMult * updateTicks / currentHealInterval);
    }

    public static int getEffectiveDamageCooldown(int foodLevel) {
        int base = Math.max(0, damageCooldownTicks);
        if (!hungerBonusEnabled) return base;
        int threshold = (int) Math.ceil((Math.max(0, Math.min(100, hungerBonusThresholdPercent)) / 100.0D) * 20.0D);
        if (foodLevel < threshold) return base;
        int reduction = Math.max(0, Math.min(100, hungerBonusCooldownReduction));
        return (int) (base * (1.0D - reduction / 100.0D));
    }

    private static double getHungerHealMultiplier(int foodLevel) {
        if (!hungerBonusEnabled) return 1.0D;
        if (hungerFullBonusEnabled && foodLevel >= 20) return Math.max(1.0D, hungerFullBonusHealMultiplier);
        int threshold = (int) Math.ceil((Math.max(0, Math.min(100, hungerBonusThresholdPercent)) / 100.0D) * 20.0D);
        if (foodLevel >= threshold) return Math.max(1.0D, hungerBonusHealMultiplier);
        return 1.0D;
    }

    private static double getCurrentHealIntervalTicks(long outOfCombatTicks, int foodLevel) {
        int baseTicks = Math.max(1, baseHealIntervalTicks);
        double interval;
        if (!rampUpEnabled) {
            interval = baseTicks;
        } else {
            int fullTicks = Math.max(1, fullStrengthHealIntervalTicks);
            int rampTicks = Math.max(1, rampFullStrengthTicks);
            double progress = Math.min(1.0D, (double) outOfCombatTicks / rampTicks);
            // interpolate from baseTicks down to fullTicks as progress goes 0->1
            interval = baseTicks + (fullTicks - baseTicks) * progress;
        }
        // Apply hunger speed multiplier (divides interval = faster ticks)
        double speedMult = getHungerSpeedMultiplier(foodLevel);
        return interval / speedMult;
    }

    private static double getHungerSpeedMultiplier(int foodLevel) {
        if (!hungerBonusEnabled) return 1.0D;
        if (hungerFullBonusEnabled && foodLevel >= 20) return Math.max(1.0D, hungerFullBonusSpeedMultiplier);
        int threshold = (int) Math.ceil((Math.max(0, Math.min(100, hungerBonusThresholdPercent)) / 100.0D) * 20.0D);
        if (foodLevel >= threshold) return Math.max(1.0D, hungerBonusSpeedMultiplier);
        return 1.0D;
    }

    private static double getMaxHealthScaleMultiplier(float maxHealth) {
        if (!scaleWithMaxHealth || maxHealth <= 20.0F) {
            return 1.0D;
        }

        double normalized = Math.max(1.0D, maxHealth / 20.0D);
        double exponent = Math.max(0.1D, maxHealthScalingExponent);
        double multiplier = Math.pow(normalized, exponent);
        double cap = Math.max(1.0D, maxHealthScalingCap);
        return Math.min(cap, multiplier);
    }

    @Mod.EventBusSubscriber(modid = PassiveRegenMod.MODID)
    private static class ConfigEvents {
        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (PassiveRegenMod.MODID.equals(event.getModID())) {
                ConfigManager.sync(PassiveRegenMod.MODID, Config.Type.INSTANCE);
            }
        }
    }
}
