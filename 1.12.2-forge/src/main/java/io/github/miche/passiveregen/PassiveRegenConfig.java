package io.github.miche.passiveregen;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = PassiveRegenMod.MODID)
public class PassiveRegenConfig {
    @Config.Comment("Set to false to disable passive regeneration entirely.")
    public static boolean enabled = true;

    @Config.Comment("If true, the heal rate ramps from the base rate up to the full-strength rate over time out of combat.")
    public static boolean rampUpEnabled = false;

    @Config.Comment("How long after taking damage before passive regeneration can begin.")
    @Config.RangeInt(min = 0, max = 12000)
    public static int damageCooldownTicks = 100;

    @Config.Comment({
        "Minimum hunger fullness percent required before passive regeneration can occur.",
        "0 means passive regeneration can still work at an empty hunger bar.",
        "50 means at least half full, and 100 means a completely full hunger bar is required."
    })
    @Config.RangeInt(min = 0, max = 100)
    public static int minimumHungerPercent = 50;

    @Config.Comment("How often players are checked for passive regeneration. Lower is more precise; higher is cheaper.")
    @Config.RangeInt(min = 1, max = 200)
    public static int updateIntervalTicks = 20;

    @Config.Comment("Ticks per 1 health healed at the base passive rate. Vanilla Regeneration I is about 50.")
    @Config.RangeInt(min = 1, max = 12000)
    public static int baseHealIntervalTicks = 100;

    @Config.Comment("Ticks per 1 health healed at full ramp strength.")
    @Config.RangeInt(min = 1, max = 12000)
    public static int fullStrengthHealIntervalTicks = 50;

    @Config.Comment("Total out-of-combat ticks required to reach the full-strength heal rate when ramp-up is enabled.")
    @Config.RangeInt(min = 1, max = 12000)
    public static int rampFullStrengthTicks = 600;

    @Config.Comment("How much health to heal each time passive regeneration triggers. 0.5 = quarter heart, 1.0 = half a heart, 2.0 = full heart.")
    @Config.RangeDouble(min = 0.01D, max = 100.0D)
    public static double healAmountPerTrigger = 0.5D;

    @Config.Comment("If true, passive regeneration scales gently with max health above 20 HP.")
    public static boolean scaleWithMaxHealth = false;

    @Config.Comment("Exponent used for max-health scaling when enabled. 0.5 is square-root scaling.")
    @Config.RangeDouble(min = 0.1D, max = 4.0D)
    public static double maxHealthScalingExponent = 0.5D;

    @Config.Comment("Maximum multiplier allowed from max-health scaling.")
    @Config.RangeDouble(min = 1.0D, max = 100.0D)
    public static double maxHealthScalingCap = 2.0D;

    public static float getHealAmountPerUpdate(long outOfCombatTicks, float maxHealth) {
        if (!enabled) {
            return 0.0F;
        }

        double healAmount = Math.max(0.01D, healAmountPerTrigger);
        return (float) (healAmount * getMaxHealthScaleMultiplier(maxHealth));
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
