package com.multiflame.passiveregen;

import java.util.List;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = PassiveRegenMod.MODID)
public class PassiveRegenConfig {
    public static class General {
        @Config.Comment("Set to false to completely disable passive regeneration.")
        public boolean enabled = true;

        @Config.Comment({
            "How many ticks after taking damage before passive regen can start.",
            "20 ticks = 1 second. Default 100 = 5 seconds.",
            "Examples: 40 = 2s, 100 = 5s (default), 200 = 10s, 600 = 30s"
        })
        @Config.RangeInt(min = 0, max = 12000)
        public int damageCooldownTicks = 100;

        @Config.Comment({
            "Minimum hunger bar % required before passive regen can occur.",
            "0 = regen works even on an empty hunger bar.",
            "50 = at least half hunger required (default).",
            "100 = hunger bar must be completely full."
        })
        @Config.RangeInt(min = 0, max = 100)
        public int minimumHungerPercent = 50;

        @Config.Comment({
            "Minimum saturation level required before passive regen can occur.",
            "0.0 = disabled (saturation is not checked).",
            "Saturation depletes before hunger. Max vanilla saturation is ~20.0."
        })
        @Config.RangeDouble(min = 0.0D, max = 20.0D)
        public double minimumSaturationLevel = 0.0D;

        @Config.Comment({
            "How often (in ticks) players are checked for passive regen.",
            "20 ticks = 1 second. Default 20 = every second."
        })
        @Config.RangeInt(min = 1, max = 200)
        public int updateIntervalTicks = 20;

        @Config.Comment({
            "Ticks between each heal trigger at the base regen speed.",
            "20 ticks = 1 second. Default 100 = heal once every 5 seconds."
        })
        @Config.RangeInt(min = 1, max = 12000)
        public int baseHealIntervalTicks = 100;

        @Config.Comment({
            "If true, regen starts slow and speeds up the longer you stay out of combat.",
            "If false, regen always uses baseHealIntervalTicks."
        })
        public boolean rampUpEnabled = false;

        @Config.Comment({
            "Ticks between each heal trigger at peak ramp-up speed.",
            "Only used if rampUpEnabled = true. Default 50 = once every 2.5 seconds."
        })
        @Config.RangeInt(min = 1, max = 12000)
        public int fullStrengthHealIntervalTicks = 50;

        @Config.Comment({
            "Total out-of-combat ticks required to reach peak ramp-up speed.",
            "20 ticks = 1 second. Default 600 = 30 seconds."
        })
        @Config.RangeInt(min = 1, max = 12000)
        public int rampFullStrengthTicks = 600;

        @Config.Comment({
            "How much health to restore each time regen triggers.",
            "0.5 = quarter heart (default), 1.0 = half heart, 2.0 = full heart."
        })
        @Config.RangeDouble(min = 0.01D, max = 100.0D)
        public double healAmountPerTrigger = 0.5D;
    }

    public static class Limits {
        @Config.Comment({
            "If true, regen heals slightly more for players with more than 20 max HP."
        })
        public boolean scaleWithMaxHealth = false;

        @Config.Comment({
            "Controls how steeply regen scales with max HP when scaleWithMaxHealth = true.",
            "0.5 = square-root scaling (default)."
        })
        @Config.RangeDouble(min = 0.1D, max = 4.0D)
        public double maxHealthScalingExponent = 0.5D;

        @Config.Comment({
            "Maximum heal multiplier from max-health scaling."
        })
        @Config.RangeDouble(min = 1.0D, max = 100.0D)
        public double maxHealthScalingCap = 2.0D;

        @Config.Comment({
            "Regen stops when health reaches this percentage of max health.",
            "100 = full regen (default)."
        })
        @Config.RangeInt(min = 0, max = 100)
        public int maxRegenHealthPercent = 100;

        @Config.Comment({
            "List of potion/mob effect IDs that prevent passive regen while active.",
            "Use namespaced IDs like minecraft:poison."
        })
        public String[] blockedEffects = new String[0];

        @Config.Comment({
            "List of dimension IDs where passive regen is disabled entirely.",
            "For 1.12.2 use dimension integers as strings, e.g. -1 Nether, 0 Overworld, 1 End."
        })
        public String[] dimensionBlacklist = new String[0];

        @Config.Comment({
            "Separate damage cooldown when a player hits you. -1 = use normal cooldown."
        })
        @Config.RangeInt(min = -1, max = 12000)
        public int pvpDamageCooldownTicks = -1;

        @Config.Comment("If true, vanilla natural regeneration is disabled.")
        public boolean disableNaturalRegen = false;

        @Config.Comment("If false, passive regen is paused while sprinting.")
        public boolean regenWhileSprinting = true;

        @Config.Comment("If true, passive regen is blocked while poison is active.")
        public boolean disableHealingDuringPoison = true;

        @Config.Comment("If true, passive regen is blocked while wither is active.")
        public boolean disableHealingDuringWither = true;
    }

    public static class HungerBonus {
        @Config.Comment("Master toggle for the hunger bonus system.")
        public boolean hungerBonusEnabled = false;

        @Config.Comment("Hunger percent threshold to trigger the hunger bonus.")
        @Config.RangeInt(min = 0, max = 100)
        public int hungerBonusThresholdPercent = 75;

        @Config.Comment("Heal amount multiplier when hunger exceeds the threshold.")
        @Config.RangeDouble(min = 1.0D, max = 100.0D)
        public double hungerBonusHealMultiplier = 1.5D;

        @Config.Comment("Heal speed multiplier when hunger exceeds the threshold.")
        @Config.RangeDouble(min = 1.0D, max = 100.0D)
        public double hungerBonusSpeedMultiplier = 1.5D;

        @Config.Comment("Cooldown reduction percent when hunger exceeds the threshold.")
        @Config.RangeInt(min = 0, max = 100)
        public int hungerBonusCooldownReduction = 25;

        @Config.Comment({
            "If true, regen still fires below minimumHungerPercent/minimumSaturationLevel",
            "but at a reduced rate instead of being fully blocked.",
            "The HUD still shows orange to indicate the penalized state."
        })
        public boolean hungerPenaltyEnabled = false;

        @Config.Comment({
            "Speed multiplier applied when hunger/saturation is below minimum threshold.",
            "Only used when hungerPenaltyEnabled = true.",
            "0.25 = 4x slower (default). 1.0 = same speed as normal (effectively disables the penalty)."
        })
        @Config.RangeDouble(min = 0.01D, max = 1.0D)
        public double hungerPenaltySpeedMultiplier = 0.25D;

        @Config.Comment({
            "Heal amount multiplier when hunger/saturation is below minimum threshold.",
            "Only used when hungerPenaltyEnabled = true.",
            "1.0 = same amount (default), 0.5 = half heals."
        })
        @Config.RangeDouble(min = 0.01D, max = 1.0D)
        public double hungerPenaltyHealMultiplier = 1.0D;

        @Config.Comment("Second-tier bonus at full hunger.")
        public boolean hungerFullBonusEnabled = false;

        @Config.Comment("Heal amount multiplier at full hunger.")
        @Config.RangeDouble(min = 1.0D, max = 100.0D)
        public double hungerFullBonusHealMultiplier = 2.0D;

        @Config.Comment("Heal speed multiplier at full hunger.")
        @Config.RangeDouble(min = 1.0D, max = 100.0D)
        public double hungerFullBonusSpeedMultiplier = 2.0D;

        @Config.Comment("Master toggle for the saturation bonus system.")
        public boolean saturationBonusEnabled = true;

        @Config.Comment("Minimum saturation needed to trigger the saturation bonus.")
        @Config.RangeDouble(min = 0.0D, max = 20.0D)
        public double saturationBonusThreshold = 10.0D;

        @Config.Comment({
            "Once active, the saturation bonus stays on until saturation falls below this level.",
            "Set equal to saturationBonusThreshold to disable hysteresis."
        })
        @Config.RangeDouble(min = 0.0D, max = 20.0D)
        public double saturationBonusDeactivateThreshold = 10.0D;

        @Config.Comment("Heal speed multiplier when saturation meets the bonus threshold.")
        @Config.RangeDouble(min = 1.0D, max = 10.0D)
        public double saturationBonusSpeedMultiplier = 2.0D;

        @Config.Comment("Heal amount multiplier when saturation meets the bonus threshold.")
        @Config.RangeDouble(min = 1.0D, max = 10.0D)
        public double saturationBonusHealMultiplier = 2.0D;

        @Config.Comment({
            "Saturation consumed per 1 HP healed while the bonus is active.",
            "0.0 = no saturation cost."
        })
        @Config.RangeDouble(min = 0.0D, max = 10.0D)
        public double saturationBonusCostPerHp = 1.0D;

        @Config.Comment({
            "Small extra saturation drain per server tick while the bonus is active.",
            "0.0 = no idle drain."
        })
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public double saturationBonusIdleDrainPerTick = 0.0D;

        @Config.Comment({
            "Saturation drain will not push the player below this floor.",
            "0.0 = no floor."
        })
        @Config.RangeDouble(min = 0.0D, max = 20.0D)
        public double saturationBonusMinSaturationFloor = 0.0D;

        @Config.Comment("Extra flat HP added to each heal tick while the saturation bonus is active.")
        @Config.RangeDouble(min = 0.0D, max = 10.0D)
        public double saturationBonusFlatHealBonus = 0.25D;

        @Config.Comment({
            "If true, saturation bonus strength scales with how far above threshold saturation is.",
            "If false, the full bonus applies as soon as the threshold is met."
        })
        public boolean saturationBonusScaleByExcess = false;
    }

    public static class HungerDrain {
        @Config.Comment("Master toggle for optional hunger drain while passive regen is active.")
        public boolean hungerDrainEnabled = false;

        @Config.Comment("Extra multiplier applied to all hunger-drain costs.")
        @Config.RangeDouble(min = 0.0D, max = 10.0D)
        public double hungerDrainSpeedMultiplier = 1.0D;

        @Config.Comment("Hunger consumed per 1 HP healed while hunger drain is enabled.")
        @Config.RangeDouble(min = 0.0D, max = 10.0D)
        public double hungerDrainCostPerHp = 0.6D;

        @Config.Comment("Small extra hunger drain applied each server tick while regen is ready at full health.")
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public double hungerDrainIdleDrainPerTick = 0.0D;

        @Config.Comment("Hunger drain will not push the player below this food level.")
        @Config.RangeDouble(min = 0.0D, max = 20.0D)
        public double hungerDrainMinFloor = 0.0D;
    }

    public static class RegenOnKill {
        @Config.Comment("If true, killing an entity reduces the player's damage cooldown.")
        public boolean regenOnKillEnabled = false;

        @Config.Comment("Percent of remaining cooldown removed on kill.")
        @Config.RangeInt(min = 0, max = 100)
        public int regenOnKillCooldownReduction = 50;

        @Config.Comment("If true, only hostile mob kills grant the cooldown reduction.")
        public boolean regenOnKillHostileOnly = false;

        @Config.Comment("Entity IDs that never grant kill-based cooldown reduction.")
        public String[] regenOnKillBlacklist = new String[0];

        @Config.Comment("If true, repeated kills within the combo window stack extra reduction.")
        public boolean regenOnKillComboEnabled = false;

        @Config.Comment("How long the kill combo window lasts, in ticks.")
        @Config.RangeInt(min = 20, max = 1200)
        public int regenOnKillComboWindowTicks = 200;

        @Config.Comment("Maximum combo stacks before extra reduction caps out.")
        @Config.RangeInt(min = 1, max = 20)
        public int regenOnKillComboMaxStacks = 5;

        @Config.Comment("Additional cooldown reduction percent per combo stack.")
        @Config.RangeInt(min = 0, max = 100)
        public int regenOnKillComboReductionPerStack = 10;
    }

    public static class Bonuses {
        @Config.Comment({
            "Controls how multiple active bonus multipliers combine.",
            "MULTIPLICATIVE, ADDITIVE, or STRONGEST_ONLY."
        })
        public BonusStackingMode bonusStackingMode = BonusStackingMode.MULTIPLICATIVE;

        @Config.Comment("Master toggle for the sneaking/crouching regen bonus.")
        public boolean crouchBonusEnabled = false;

        @Config.Comment("Heal speed multiplier while sneaking.")
        @Config.RangeDouble(min = 1.0D, max = 10.0D)
        public double crouchSpeedMultiplier = 1.5D;

        @Config.Comment("Heal amount multiplier while sneaking.")
        @Config.RangeDouble(min = 1.0D, max = 10.0D)
        public double crouchHealMultiplier = 1.0D;

        @Config.Comment("Master toggle for light-level-based regen scaling.")
        public boolean lightLevelBonusEnabled = false;

        @Config.Comment("Multiplier applied at block light level 0.")
        @Config.RangeDouble(min = 0.1D, max = 2.0D)
        public double lightLevelMinMultiplier = 0.75D;

        @Config.Comment("Multiplier applied at block light level 15.")
        @Config.RangeDouble(min = 0.1D, max = 2.0D)
        public double lightLevelMaxMultiplier = 1.25D;

        @Config.Comment("Master toggle for day/night regen scaling.")
        public boolean dayNightMultiplierEnabled = false;

        @Config.Comment("Multiplier applied during the day.")
        @Config.RangeDouble(min = 0.1D, max = 3.0D)
        public double dayMultiplier = 1.25D;

        @Config.Comment("Multiplier applied during the night.")
        @Config.RangeDouble(min = 0.1D, max = 3.0D)
        public double nightMultiplier = 0.75D;

        @Config.Comment("Master toggle for difficulty-based regen scaling.")
        public boolean difficultyScalingEnabled = false;

        @Config.Comment("Multiplier applied on Peaceful difficulty.")
        @Config.RangeDouble(min = 0.1D, max = 5.0D)
        public double peacefulMultiplier = 2.0D;

        @Config.Comment("Multiplier applied on Easy difficulty.")
        @Config.RangeDouble(min = 0.1D, max = 5.0D)
        public double easyMultiplier = 1.25D;

        @Config.Comment("Multiplier applied on Normal difficulty.")
        @Config.RangeDouble(min = 0.1D, max = 5.0D)
        public double normalMultiplier = 1.0D;

        @Config.Comment("Multiplier applied on Hard difficulty.")
        @Config.RangeDouble(min = 0.1D, max = 5.0D)
        public double hardMultiplier = 0.75D;
    }

    public static class LargeDamagePenalty {
        @Config.Comment("If true, very large hits apply a longer cooldown than normal.")
        public boolean largeDamagePenaltyEnabled = false;

        @Config.Comment("Percent of max health a hit must deal to count as large damage.")
        @Config.RangeInt(min = 1, max = 100)
        public int largeDamageThresholdPercent = 50;

        @Config.Comment("Cooldown multiplier applied for large hits.")
        @Config.RangeDouble(min = 1.0D, max = 5.0D)
        public double largeDamageCooldownMultiplier = 1.5D;
    }

    public static class Hud {
        @Config.Comment("Master toggle for the passive regen HUD widget.")
        public boolean showRegenHud = true;

        @Config.Comment("If true, shows the remaining cooldown in seconds next to the heart.")
        public boolean showTimer = false;

        @Config.Comment({
            "Global opacity multiplier for the entire HUD (0.0 = invisible, 1.0 = fully opaque)."
        })
        @Config.RangeDouble(min = 0.0D, max = 1.0D)
        public double hudOpacity = 1.0D;

        @Config.Comment({
            "Heart fill color as a hex RGB string. Default FF69B4 (pink)."
        })
        public String hudColor = "FF69B4";

        @Config.Comment({
            "Heart color when hunger is too low to regen. Default FF9F1A (orange)."
        })
        public String hudBlockedColor = "FF9F1A";

        @Config.Comment("HUD scale. 1.0 = normal size.")
        @Config.RangeDouble(min = 0.5D, max = 4.0D)
        public double hudScale = 1.0D;

        @Config.Comment("HUD position preset.")
        public HudPosition hudPosition = HudPosition.LEFT_OF_HEALTH;

        @Config.Comment("Extra horizontal offset after the preset anchor is applied.")
        @Config.RangeInt(min = -2000, max = 2000)
        public int hudOffsetX = 0;

        @Config.Comment("Extra vertical offset after the preset anchor is applied.")
        @Config.RangeInt(min = -2000, max = 2000)
        public int hudOffsetY = 0;

        @Config.Comment("Anchor used only when hudPosition = CUSTOM.")
        public HudAnchor hudCustomAnchor = HudAnchor.TOP_LEFT;

        @Config.Comment("When the HUD should appear.")
        public HudShowCondition showCondition = HudShowCondition.injured;

        @Config.Comment("If true, hide the HUD when you are already full health and regen is ready.")
        public boolean hideAtFullHealth = true;

        @Config.Comment("If true, the HUD fades in and out instead of appearing instantly.")
        public boolean hudFadeEnabled = true;

        @Config.Comment("How long the HUD takes to fade in, in milliseconds.")
        @Config.RangeInt(min = 0, max = 3000)
        public int hudFadeInMs = 500;

        @Config.Comment("How long the HUD takes to fade out, in milliseconds.")
        @Config.RangeInt(min = 0, max = 3000)
        public int hudFadeOutMs = 400;

        @Config.Comment({
            "If true, adds a heartbeat thump on each heal tick and an organic multi-sine glow pulse.",
            "Set to false for a simpler, flat animation (smooth sine glow, no scale effect).",
            "Useful for players sensitive to motion or who prefer a cleaner look."
        })
        public boolean hudRichAnimations = true;

        @Config.Comment("If true, shows the gold saturation-bonus sheen when that server-side bonus is active.")
        public boolean hudSaturationSheenEnabled = true;

        @Config.Comment("If true, shows the warm sparkle burst when a saturation-bonus heal tops you off.")
        public boolean hudSaturationSparkleEnabled = true;

        @Config.Comment("If true, shows the poison HUD state while poison is active.")
        public boolean hudPoisonEffectEnabled = true;

        @Config.Comment("If true, shows the wither HUD state while wither is active.")
        public boolean hudWitherEffectEnabled = true;

        @Config.Comment("Optional heal feedback particles. Format: modid:particle;count;spread")
        public String[] particles = new String[0];

        @Config.Comment("Optional heal feedback sounds. Format: modid:sound;volume;pitch")
        public String[] sounds = new String[0];
    }

    public enum HudPosition {
        RIGHT_OF_HEALTH,
        LEFT_OF_HEALTH,
        ABOVE_HUNGER,
        BELOW_HUNGER,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CUSTOM
    }

    public enum HudAnchor {
        CENTER,
        TOP,
        BOTTOM,
        LEFT,
        RIGHT,
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT
    }

    public enum HudShowCondition {
        injured,
        out_of_combat,
        always
    }

    public enum BonusStackingMode {
        MULTIPLICATIVE,
        ADDITIVE,
        STRONGEST_ONLY
    }

    public static General general = new General();
    public static Limits limits = new Limits();
    public static HungerBonus hungerBonus = new HungerBonus();
    public static HungerDrain hungerDrain = new HungerDrain();
    public static RegenOnKill regenOnKill = new RegenOnKill();
    public static Bonuses bonuses = new Bonuses();
    public static LargeDamagePenalty largeDamagePenalty = new LargeDamagePenalty();
    public static Hud hud = new Hud();

    @Config.Ignore public static boolean enabled = true;
    @Config.Ignore public static boolean rampUpEnabled = false;
    @Config.Ignore public static int damageCooldownTicks = 100;
    @Config.Ignore public static int minimumHungerPercent = 50;
    @Config.Ignore public static double minimumSaturationLevel = 0.0D;
    @Config.Ignore public static int updateIntervalTicks = 20;
    @Config.Ignore public static int baseHealIntervalTicks = 100;
    @Config.Ignore public static int fullStrengthHealIntervalTicks = 50;
    @Config.Ignore public static int rampFullStrengthTicks = 600;
    @Config.Ignore public static double healAmountPerTrigger = 0.5D;
    @Config.Ignore public static boolean scaleWithMaxHealth = false;
    @Config.Ignore public static double maxHealthScalingExponent = 0.5D;
    @Config.Ignore public static double maxHealthScalingCap = 2.0D;
    @Config.Ignore public static int maxRegenHealthPercent = 100;
    @Config.Ignore public static String[] blockedEffects = new String[0];
    @Config.Ignore public static String[] dimensionBlacklist = new String[0];
    @Config.Ignore public static int pvpDamageCooldownTicks = -1;
    @Config.Ignore public static boolean disableNaturalRegen = false;
    @Config.Ignore public static boolean regenWhileSprinting = true;
    @Config.Ignore public static boolean disableHealingDuringPoison = true;
    @Config.Ignore public static boolean disableHealingDuringWither = true;
    @Config.Ignore public static boolean hungerBonusEnabled = false;
    @Config.Ignore public static boolean hungerPenaltyEnabled = false;
    @Config.Ignore public static double hungerPenaltySpeedMultiplier = 0.25D;
    @Config.Ignore public static double hungerPenaltyHealMultiplier = 1.0D;
    @Config.Ignore public static int hungerBonusThresholdPercent = 75;
    @Config.Ignore public static double hungerBonusHealMultiplier = 1.5D;
    @Config.Ignore public static double hungerBonusSpeedMultiplier = 1.5D;
    @Config.Ignore public static int hungerBonusCooldownReduction = 25;
    @Config.Ignore public static boolean hungerFullBonusEnabled = false;
    @Config.Ignore public static double hungerFullBonusHealMultiplier = 2.0D;
    @Config.Ignore public static double hungerFullBonusSpeedMultiplier = 2.0D;
    @Config.Ignore public static boolean saturationBonusEnabled = true;
    @Config.Ignore public static double saturationBonusThreshold = 10.0D;
    @Config.Ignore public static double saturationBonusDeactivateThreshold = 10.0D;
    @Config.Ignore public static double saturationBonusSpeedMultiplier = 2.0D;
    @Config.Ignore public static double saturationBonusHealMultiplier = 2.0D;
    @Config.Ignore public static double saturationBonusCostPerHp = 1.0D;
    @Config.Ignore public static double saturationBonusIdleDrainPerTick = 0.0D;
    @Config.Ignore public static double saturationBonusMinSaturationFloor = 0.0D;
    @Config.Ignore public static double saturationBonusFlatHealBonus = 0.25D;
    @Config.Ignore public static boolean saturationBonusScaleByExcess = false;
    @Config.Ignore public static boolean hungerDrainEnabled = false;
    @Config.Ignore public static double hungerDrainSpeedMultiplier = 1.0D;
    @Config.Ignore public static double hungerDrainCostPerHp = 0.6D;
    @Config.Ignore public static double hungerDrainIdleDrainPerTick = 0.0D;
    @Config.Ignore public static double hungerDrainMinFloor = 0.0D;
    @Config.Ignore public static boolean regenOnKillEnabled = false;
    @Config.Ignore public static int regenOnKillCooldownReduction = 50;
    @Config.Ignore public static boolean regenOnKillHostileOnly = false;
    @Config.Ignore public static String[] regenOnKillBlacklist = new String[0];
    @Config.Ignore public static boolean regenOnKillComboEnabled = false;
    @Config.Ignore public static int regenOnKillComboWindowTicks = 200;
    @Config.Ignore public static int regenOnKillComboMaxStacks = 5;
    @Config.Ignore public static int regenOnKillComboReductionPerStack = 10;
    @Config.Ignore public static BonusStackingMode bonusStackingMode = BonusStackingMode.MULTIPLICATIVE;
    @Config.Ignore public static boolean crouchBonusEnabled = false;
    @Config.Ignore public static double crouchSpeedMultiplier = 1.5D;
    @Config.Ignore public static double crouchHealMultiplier = 1.0D;
    @Config.Ignore public static boolean lightLevelBonusEnabled = false;
    @Config.Ignore public static double lightLevelMinMultiplier = 0.75D;
    @Config.Ignore public static double lightLevelMaxMultiplier = 1.25D;
    @Config.Ignore public static boolean dayNightMultiplierEnabled = false;
    @Config.Ignore public static double dayMultiplier = 1.25D;
    @Config.Ignore public static double nightMultiplier = 0.75D;
    @Config.Ignore public static boolean difficultyScalingEnabled = false;
    @Config.Ignore public static double peacefulMultiplier = 2.0D;
    @Config.Ignore public static double easyMultiplier = 1.25D;
    @Config.Ignore public static double normalMultiplier = 1.0D;
    @Config.Ignore public static double hardMultiplier = 0.75D;
    @Config.Ignore public static boolean largeDamagePenaltyEnabled = false;
    @Config.Ignore public static int largeDamageThresholdPercent = 50;
    @Config.Ignore public static double largeDamageCooldownMultiplier = 1.5D;

    static {
        syncAliases();
    }

    public static void syncAliases() {
        enabled = general.enabled;
        rampUpEnabled = general.rampUpEnabled;
        damageCooldownTicks = general.damageCooldownTicks;
        minimumHungerPercent = general.minimumHungerPercent;
        minimumSaturationLevel = general.minimumSaturationLevel;
        updateIntervalTicks = general.updateIntervalTicks;
        baseHealIntervalTicks = general.baseHealIntervalTicks;
        fullStrengthHealIntervalTicks = general.fullStrengthHealIntervalTicks;
        rampFullStrengthTicks = general.rampFullStrengthTicks;
        healAmountPerTrigger = general.healAmountPerTrigger;

        scaleWithMaxHealth = limits.scaleWithMaxHealth;
        maxHealthScalingExponent = limits.maxHealthScalingExponent;
        maxHealthScalingCap = limits.maxHealthScalingCap;
        maxRegenHealthPercent = limits.maxRegenHealthPercent;
        blockedEffects = limits.blockedEffects;
        dimensionBlacklist = limits.dimensionBlacklist;
        pvpDamageCooldownTicks = limits.pvpDamageCooldownTicks;
        disableNaturalRegen = limits.disableNaturalRegen;
        regenWhileSprinting = limits.regenWhileSprinting;
        disableHealingDuringPoison = limits.disableHealingDuringPoison;
        disableHealingDuringWither = limits.disableHealingDuringWither;

        hungerBonusEnabled = hungerBonus.hungerBonusEnabled;
        hungerPenaltyEnabled = hungerBonus.hungerPenaltyEnabled;
        hungerPenaltySpeedMultiplier = hungerBonus.hungerPenaltySpeedMultiplier;
        hungerPenaltyHealMultiplier = hungerBonus.hungerPenaltyHealMultiplier;
        hungerBonusThresholdPercent = hungerBonus.hungerBonusThresholdPercent;
        hungerBonusHealMultiplier = hungerBonus.hungerBonusHealMultiplier;
        hungerBonusSpeedMultiplier = hungerBonus.hungerBonusSpeedMultiplier;
        hungerBonusCooldownReduction = hungerBonus.hungerBonusCooldownReduction;
        hungerFullBonusEnabled = hungerBonus.hungerFullBonusEnabled;
        hungerFullBonusHealMultiplier = hungerBonus.hungerFullBonusHealMultiplier;
        hungerFullBonusSpeedMultiplier = hungerBonus.hungerFullBonusSpeedMultiplier;
        saturationBonusEnabled = hungerBonus.saturationBonusEnabled;
        saturationBonusThreshold = hungerBonus.saturationBonusThreshold;
        saturationBonusDeactivateThreshold = Math.max(0.0D, Math.min(hungerBonus.saturationBonusThreshold, hungerBonus.saturationBonusDeactivateThreshold));
        saturationBonusSpeedMultiplier = hungerBonus.saturationBonusSpeedMultiplier;
        saturationBonusHealMultiplier = hungerBonus.saturationBonusHealMultiplier;
        saturationBonusCostPerHp = hungerBonus.saturationBonusCostPerHp;
        saturationBonusIdleDrainPerTick = hungerBonus.saturationBonusIdleDrainPerTick;
        saturationBonusMinSaturationFloor = Math.max(0.0D, Math.min(20.0D, hungerBonus.saturationBonusMinSaturationFloor));
        saturationBonusFlatHealBonus = hungerBonus.saturationBonusFlatHealBonus;
        saturationBonusScaleByExcess = hungerBonus.saturationBonusScaleByExcess;
        hungerDrainEnabled = hungerDrain.hungerDrainEnabled;
        hungerDrainSpeedMultiplier = hungerDrain.hungerDrainSpeedMultiplier;
        hungerDrainCostPerHp = hungerDrain.hungerDrainCostPerHp;
        hungerDrainIdleDrainPerTick = hungerDrain.hungerDrainIdleDrainPerTick;
        hungerDrainMinFloor = hungerDrain.hungerDrainMinFloor;

        regenOnKillEnabled = regenOnKill.regenOnKillEnabled;
        regenOnKillCooldownReduction = regenOnKill.regenOnKillCooldownReduction;
        regenOnKillHostileOnly = regenOnKill.regenOnKillHostileOnly;
        regenOnKillBlacklist = regenOnKill.regenOnKillBlacklist;
        regenOnKillComboEnabled = regenOnKill.regenOnKillComboEnabled;
        regenOnKillComboWindowTicks = regenOnKill.regenOnKillComboWindowTicks;
        regenOnKillComboMaxStacks = regenOnKill.regenOnKillComboMaxStacks;
        regenOnKillComboReductionPerStack = regenOnKill.regenOnKillComboReductionPerStack;

        bonusStackingMode = bonuses.bonusStackingMode;
        crouchBonusEnabled = bonuses.crouchBonusEnabled;
        crouchSpeedMultiplier = bonuses.crouchSpeedMultiplier;
        crouchHealMultiplier = bonuses.crouchHealMultiplier;
        lightLevelBonusEnabled = bonuses.lightLevelBonusEnabled;
        lightLevelMinMultiplier = bonuses.lightLevelMinMultiplier;
        lightLevelMaxMultiplier = bonuses.lightLevelMaxMultiplier;
        dayNightMultiplierEnabled = bonuses.dayNightMultiplierEnabled;
        dayMultiplier = bonuses.dayMultiplier;
        nightMultiplier = bonuses.nightMultiplier;
        difficultyScalingEnabled = bonuses.difficultyScalingEnabled;
        peacefulMultiplier = bonuses.peacefulMultiplier;
        easyMultiplier = bonuses.easyMultiplier;
        normalMultiplier = bonuses.normalMultiplier;
        hardMultiplier = bonuses.hardMultiplier;

        largeDamagePenaltyEnabled = largeDamagePenalty.largeDamagePenaltyEnabled;
        largeDamageThresholdPercent = largeDamagePenalty.largeDamageThresholdPercent;
        largeDamageCooldownMultiplier = largeDamagePenalty.largeDamageCooldownMultiplier;
    }

    public static int getMinimumFoodLevel() {
        return (int) Math.ceil((Math.max(0, Math.min(100, minimumHungerPercent)) / 100.0D) * 20.0D);
    }

    public static int getHungerBonusThresholdFoodLevel() {
        return (int) Math.ceil((Math.max(0, Math.min(100, hungerBonusThresholdPercent)) / 100.0D) * 20.0D);
    }

    public static float getHealAmountPerUpdate(long outOfCombatTicks, float maxHealth, int foodLevel) {
        if (!enabled) return 0.0F;

        double healAmount = Math.max(0.01D, healAmountPerTrigger);
        double scaledHeal = healAmount * getMaxHealthScaleMultiplier(maxHealth);
        int updateTicks = Math.max(1, updateIntervalTicks);
        double currentHealInterval = getCurrentHealIntervalTicks(outOfCombatTicks);
        double healMult = getHungerHealMultiplier(foodLevel);
        double speedMult = getHungerSpeedMultiplier(foodLevel);
        return (float)(scaledHeal * healMult * updateTicks / (currentHealInterval / speedMult));
    }

    public static int getEffectiveDamageCooldown(int foodLevel) {
        int base = Math.max(0, damageCooldownTicks);
        if (!hungerBonusEnabled) return base;
        if (foodLevel < getHungerBonusThresholdFoodLevel()) return base;
        int reduction = Math.max(0, Math.min(100, hungerBonusCooldownReduction));
        return (int)(base * (1.0D - reduction / 100.0D));
    }

    public static double getHungerHealMultiplier(int foodLevel) {
        if (!hungerBonusEnabled) return 1.0D;
        if (hungerFullBonusEnabled && foodLevel >= 20) return Math.max(1.0D, hungerFullBonusHealMultiplier);
        if (foodLevel >= getHungerBonusThresholdFoodLevel()) return Math.max(1.0D, hungerBonusHealMultiplier);
        return 1.0D;
    }

    public static double getHungerSpeedMultiplier(int foodLevel) {
        if (!hungerBonusEnabled) return 1.0D;
        if (hungerFullBonusEnabled && foodLevel >= 20) return Math.max(1.0D, hungerFullBonusSpeedMultiplier);
        if (foodLevel >= getHungerBonusThresholdFoodLevel()) return Math.max(1.0D, hungerBonusSpeedMultiplier);
        return 1.0D;
    }

    public static double combineBonusMultipliers(List<Double> multipliers) {
        if (multipliers == null || multipliers.isEmpty()) return 1.0D;
        switch (bonusStackingMode) {
            case ADDITIVE: {
                double result = 1.0D;
                for (double multiplier : multipliers) result += (multiplier - 1.0D);
                return Math.max(0.0D, result);
            }
            case STRONGEST_ONLY: {
                double strongest = 1.0D;
                for (double multiplier : multipliers) strongest = Math.max(strongest, multiplier);
                return strongest;
            }
            case MULTIPLICATIVE:
            default: {
                double result = 1.0D;
                for (double multiplier : multipliers) result *= multiplier;
                return Math.max(0.0D, result);
            }
        }
    }

    private static double getCurrentHealIntervalTicks(long outOfCombatTicks) {
        int baseTicks = Math.max(1, baseHealIntervalTicks);
        if (!rampUpEnabled) return baseTicks;
        int fullTicks = Math.max(1, fullStrengthHealIntervalTicks);
        int rampTicks = Math.max(1, rampFullStrengthTicks);
        double progress = Math.min(1.0D, (double) outOfCombatTicks / rampTicks);
        return baseTicks + (fullTicks - baseTicks) * progress;
    }

    private static double getMaxHealthScaleMultiplier(float maxHealth) {
        if (!scaleWithMaxHealth || maxHealth <= 20.0F) return 1.0D;
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
                syncAliases();
            }
        }
    }
}
