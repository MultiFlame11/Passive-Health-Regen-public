package io.github.miche.passiveregen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class PassiveRegenConfig {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("passive-health-regen.json");

    public boolean enabled = true;
    public boolean rampUpEnabled = false;
    public int damageCooldownTicks = 100;
    public int minimumHungerPercent = 50;
    public int updateIntervalTicks = 20;
    public int baseHealIntervalTicks = 100;
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

    // -- Hunger Bonus ---------------------------------------------------------
    public boolean hungerBonusEnabled = false;
    public int hungerBonusThresholdPercent = 75;
    public double hungerBonusHealMultiplier = 1.5D;
    public double hungerBonusSpeedMultiplier = 1.5D;
    public int hungerBonusCooldownReduction = 25;
    public boolean hungerFullBonusEnabled = false;
    public double hungerFullBonusHealMultiplier = 2.0D;
    public double hungerFullBonusSpeedMultiplier = 2.0D;

    // -- Regen on Kill --------------------------------------------------------
    public boolean regenOnKillEnabled = false;
    public int regenOnKillCooldownReduction = 50;

    // -- Natural Regen --------------------------------------------------------
    public boolean disableNaturalRegen = false;

    // -- Sprinting ------------------------------------------------------------
    public boolean regenWhileSprinting = true;

    public static PassiveRegenConfig load() {
        PassiveRegenConfig config = null;
        if (Files.exists(CONFIG_PATH)) {
            try (com.google.gson.stream.JsonReader jr = new com.google.gson.stream.JsonReader(Files.newBufferedReader(CONFIG_PATH))) {
                jr.setLenient(true);
                config = GSON.fromJson(jr, PassiveRegenConfig.class);
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
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(CONFIG_PATH)) {
                w.write("{");
                w.newLine();

                w.write("  // Set to false to completely disable passive regeneration.");
                w.newLine();
                w.write("  \"enabled\": " + enabled + ",");
                w.newLine();
                w.newLine();

                w.write("  // If true, regen starts slow and speeds up the longer you stay out of combat.");
                w.newLine();
                w.write("  // At rampFullStrengthTicks out-of-combat ticks you reach peak regen speed.");
                w.newLine();
                w.write("  // If false, regen always runs at the baseHealIntervalTicks rate.");
                w.newLine();
                w.write("  \"rampUpEnabled\": " + rampUpEnabled + ",");
                w.newLine();
                w.newLine();

                w.write("  // How many ticks after taking damage before passive regen can start.");
                w.newLine();
                w.write("  // 20 ticks = 1 second. Default 100 = 5 seconds.");
                w.newLine();
                w.write("  // Examples: 40 = 2s,  100 = 5s (default),  200 = 10s,  600 = 30s");
                w.newLine();
                w.write("  \"damageCooldownTicks\": " + damageCooldownTicks + ",");
                w.newLine();
                w.newLine();

                w.write("  // Minimum hunger bar % required before passive regen can occur.");
                w.newLine();
                w.write("  // 0 = regen works even on an empty hunger bar.");
                w.newLine();
                w.write("  // 50 = at least half hunger required (default).");
                w.newLine();
                w.write("  // 100 = hunger bar must be completely full.");
                w.newLine();
                w.write("  // Examples: 0 (any hunger), 25 (quarter+), 50 (half+, default), 75 (three-quarter+), 100 (full only)");
                w.newLine();
                w.write("  \"minimumHungerPercent\": " + minimumHungerPercent + ",");
                w.newLine();
                w.newLine();

                w.write("  // How often (in ticks) players are checked for passive regen.");
                w.newLine();
                w.write("  // 20 ticks = 1 second. Lower = more precise; higher = cheaper.");
                w.newLine();
                w.write("  // Default 20 (every second). Rarely needs changing.");
                w.newLine();
                w.write("  // Examples: 10 = 0.5s, 20 = 1s (default), 40 = 2s");
                w.newLine();
                w.write("  \"updateIntervalTicks\": " + updateIntervalTicks + ",");
                w.newLine();
                w.newLine();

                w.write("  // Ticks between each heal trigger at the base (starting) regen speed.");
                w.newLine();
                w.write("  // Lower = faster regen. Vanilla Regeneration I is approximately 50 ticks.");
                w.newLine();
                w.write("  // 20 ticks = 1 second. Default 100 = heal once every 5 seconds.");
                w.newLine();
                w.write("  // Examples: 20 = 1s (fast), 50 = 2.5s (vanilla regen I), 100 = 5s (default), 200 = 10s (slow)");
                w.newLine();
                w.write("  \"baseHealIntervalTicks\": " + baseHealIntervalTicks + ",");
                w.newLine();
                w.newLine();

                w.write("  // Ticks between each heal trigger at peak ramp-up speed (only used if rampUpEnabled = true).");
                w.newLine();
                w.write("  // This is the FASTEST the regen will ever go, reached after rampFullStrengthTicks out-of-combat ticks.");
                w.newLine();
                w.write("  // Must be less than baseHealIntervalTicks for ramp-up to have any effect.");
                w.newLine();
                w.write("  // Default 50 = heal once every 2.5 seconds at peak.");
                w.newLine();
                w.write("  // Examples: 20 = 1s (very fast), 50 = 2.5s (default), 75 = 3.75s");
                w.newLine();
                w.write("  \"fullStrengthHealIntervalTicks\": " + fullStrengthHealIntervalTicks + ",");
                w.newLine();
                w.newLine();

                w.write("  // Total out-of-combat ticks to reach peak regen speed (only used if rampUpEnabled = true).");
                w.newLine();
                w.write("  // 20 ticks = 1 second. Default 600 = 30 seconds out of combat to reach full speed.");
                w.newLine();
                w.write("  // Examples: 200 = 10s, 400 = 20s, 600 = 30s (default), 1200 = 1 minute");
                w.newLine();
                w.write("  \"rampFullStrengthTicks\": " + rampFullStrengthTicks + ",");
                w.newLine();
                w.newLine();

                w.write("  // How much health to restore each time regen triggers.");
                w.newLine();
                w.write("  // 0.5 = quarter heart (default),  1.0 = half heart,  2.0 = full heart.");
                w.newLine();
                w.write("  // This is before any max-health scaling.");
                w.newLine();
                w.write("  \"healAmountPerTrigger\": " + healAmountPerTrigger + ",");
                w.newLine();
                w.newLine();

                w.write("  // If true, regen heals slightly more for players with more than 20 max HP.");
                w.newLine();
                w.write("  // Uses a soft curve (controlled by maxHealthScalingExponent) so it does not become extreme.");
                w.newLine();
                w.write("  // Useful for modpacks with high-HP bosses or modded players with extra hearts.");
                w.newLine();
                w.write("  \"scaleWithMaxHealth\": " + scaleWithMaxHealth + ",");
                w.newLine();
                w.newLine();

                w.write("  // Controls how steeply regen scales with max HP when scaleWithMaxHealth = true.");
                w.newLine();
                w.write("  // 0.5 = square-root scaling (gentle, default).  1.0 = linear.  2.0 = quadratic (steep).");
                w.newLine();
                w.write("  // Lower values keep scaling gentle even at very high HP pools.");
                w.newLine();
                w.write("  // Example at 40 max HP (2x base): exponent 0.5 gives ~1.41x heal, 1.0 gives 2.0x heal.");
                w.newLine();
                w.write("  \"maxHealthScalingExponent\": " + maxHealthScalingExponent + ",");
                w.newLine();
                w.newLine();

                w.write("  // Maximum heal multiplier from max-health scaling (when scaleWithMaxHealth = true).");
                w.newLine();
                w.write("  // Default 2.0 means scaling can at most double the base heal amount, no matter how high HP gets.");
                w.newLine();
                w.write("  // Examples: 1.5 = 50% more at most, 2.0 = double at most (default), 3.0 = triple at most");
                w.newLine();
                w.write("  \"maxHealthScalingCap\": " + maxHealthScalingCap + ",");
                w.newLine();

                w.write("  // Regen stops when health reaches this percentage of max health.");
                w.newLine();
                w.write("  // 100 = regen all the way to full (default). 80 = stops at 80% health.");
                w.newLine();
                w.write("  // Examples: 60 = stops at 60%, 80 = stops at 80%, 100 = full regen (default)");
                w.newLine();
                w.write("  \"maxRegenHealthPercent\": " + maxRegenHealthPercent + ",");
                w.newLine();
                w.newLine();

                w.write("  // List of potion/mob effect IDs that prevent passive regen while the player has them active.");
                w.newLine();
                w.write("  // Empty list = no effects block regen (default).");
                w.newLine();
                w.write("  // Use namespaced IDs like minecraft:poison, minecraft:wither, minecraft:weakness.");
                w.newLine();
                w.write("  // Examples: [] = nothing blocks regen, [\"minecraft:poison\"] = paused while poisoned");
                w.newLine();
                w.write("  \"blockedEffects\": " + toJsonArray(blockedEffects) + ",");
                w.newLine();
                w.newLine();

                w.write("  // List of dimension IDs where passive regen is disabled entirely.");
                w.newLine();
                w.write("  // Empty list = regen works in all dimensions (default).");
                w.newLine();
                w.write("  // Examples: [] = all dimensions, [\"minecraft:the_nether\"] = disabled in Nether");
                w.newLine();
                w.write("  \"dimensionBlacklist\": " + toJsonArray(dimensionBlacklist) + ",");
                w.newLine();
                w.newLine();

                w.write("  // Separate damage cooldown (ticks) when a player hits you. -1 = same as damageCooldownTicks (default).");
                w.newLine();
                w.write("  // 20 ticks = 1 second. Set higher to delay regen longer after PvP hits.");
                w.newLine();
                w.write("  // Examples: -1 = same as regular (default), 200 = 10s, 400 = 20s, 600 = 30s");
                w.newLine();
                w.write("  \"pvpDamageCooldownTicks\": " + pvpDamageCooldownTicks + ",");
                w.newLine();
                w.newLine();

                w.write("  // Master toggle for the hunger bonus system. When enabled, high hunger boosts regen.");
                w.newLine();
                w.write("  \"hungerBonusEnabled\": " + hungerBonusEnabled + ",");
                w.newLine();
                w.newLine();

                w.write("  // Hunger % threshold to trigger the hunger bonus (0-100). Default 75.");
                w.newLine();
                w.write("  \"hungerBonusThresholdPercent\": " + hungerBonusThresholdPercent + ",");
                w.newLine();
                w.newLine();

                w.write("  // Heal amount multiplier applied when hunger exceeds threshold. Default 1.5.");
                w.newLine();
                w.write("  \"hungerBonusHealMultiplier\": " + hungerBonusHealMultiplier + ",");
                w.newLine();
                w.newLine();

                w.write("  // Heal speed multiplier when hunger exceeds threshold. Default 1.5.");
                w.newLine();
                w.write("  \"hungerBonusSpeedMultiplier\": " + hungerBonusSpeedMultiplier + ",");
                w.newLine();
                w.newLine();

                w.write("  // % to reduce damage cooldown when hunger exceeds threshold (0-100). Default 25.");
                w.newLine();
                w.write("  \"hungerBonusCooldownReduction\": " + hungerBonusCooldownReduction + ",");
                w.newLine();
                w.newLine();

                w.write("  // Second-tier bonus at 100% hunger (full bar). Off by default.");
                w.newLine();
                w.write("  \"hungerFullBonusEnabled\": " + hungerFullBonusEnabled + ",");
                w.newLine();
                w.newLine();

                w.write("  // Heal amount multiplier at full hunger. Default 2.0.");
                w.newLine();
                w.write("  \"hungerFullBonusHealMultiplier\": " + hungerFullBonusHealMultiplier + ",");
                w.newLine();
                w.newLine();

                w.write("  // Heal speed multiplier at full hunger. Default 2.0.");
                w.newLine();
                w.write("  \"hungerFullBonusSpeedMultiplier\": " + hungerFullBonusSpeedMultiplier + ",");
                w.newLine();
                w.newLine();

                w.write("  // If true, killing an entity reduces the player's damage cooldown.");
                w.newLine();
                w.write("  \"regenOnKillEnabled\": " + regenOnKillEnabled + ",");
                w.newLine();
                w.newLine();

                w.write("  // % of remaining cooldown to remove on kill (0-100). Default 50.");
                w.newLine();
                w.write("  \"regenOnKillCooldownReduction\": " + regenOnKillCooldownReduction + ",");
                w.newLine();
                w.newLine();

                w.write("  // If true, vanilla natural regeneration is disabled.");
                w.newLine();
                w.write("  \"disableNaturalRegen\": " + disableNaturalRegen + ",");
                w.newLine();
                w.newLine();

                w.write("  // If false, passive regen is paused while the player is sprinting.");
                w.newLine();
                w.write("  \"regenWhileSprinting\": " + regenWhileSprinting);
                w.newLine();

                w.write("}");
                w.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    public float getHealAmountPerUpdate(long outOfCombatTicks, float maxHealth, int foodLevel) {
        if (!enabled) {
            return 0.0F;
        }

        double healAmount = Math.max(0.0D, healAmountPerTrigger);
        double scaledHeal = healAmount * getMaxHealthScaleMultiplier(maxHealth);

        int updateTicks = Math.max(1, updateIntervalTicks);
        double currentHealInterval = getCurrentHealIntervalTicks(outOfCombatTicks, foodLevel);
        double healMult = getHungerHealMultiplier(foodLevel);
        return (float) (scaledHeal * healMult * updateTicks / currentHealInterval);
    }

    public int getEffectiveDamageCooldown(int foodLevel) {
        int base = Math.max(0, damageCooldownTicks);
        if (!hungerBonusEnabled) return base;
        int threshold = (int) Math.ceil((Math.max(0, Math.min(100, hungerBonusThresholdPercent)) / 100.0D) * 20.0D);
        if (foodLevel < threshold) return base;
        int reduction = Math.max(0, Math.min(100, hungerBonusCooldownReduction));
        return (int) (base * (1.0D - reduction / 100.0D));
    }

    private double getHungerHealMultiplier(int foodLevel) {
        if (!hungerBonusEnabled) return 1.0D;
        if (hungerFullBonusEnabled && foodLevel >= 20) return Math.max(1.0D, hungerFullBonusHealMultiplier);
        int threshold = (int) Math.ceil((Math.max(0, Math.min(100, hungerBonusThresholdPercent)) / 100.0D) * 20.0D);
        if (foodLevel >= threshold) return Math.max(1.0D, hungerBonusHealMultiplier);
        return 1.0D;
    }

    private double getHungerSpeedMultiplier(int foodLevel) {
        if (!hungerBonusEnabled) return 1.0D;
        if (hungerFullBonusEnabled && foodLevel >= 20) return Math.max(1.0D, hungerFullBonusSpeedMultiplier);
        int threshold = (int) Math.ceil((Math.max(0, Math.min(100, hungerBonusThresholdPercent)) / 100.0D) * 20.0D);
        if (foodLevel >= threshold) return Math.max(1.0D, hungerBonusSpeedMultiplier);
        return 1.0D;
    }

    private double getCurrentHealIntervalTicks(long outOfCombatTicks, int foodLevel) {
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

    public int getMinimumFoodLevel() {
        int clampedPercent = Math.max(0, Math.min(100, minimumHungerPercent));
        return (int) Math.ceil((clampedPercent / 100.0D) * 20.0D);
    }

    private double getMaxHealthScaleMultiplier(float maxHealth) {
        if (!scaleWithMaxHealth || maxHealth <= 20.0F) {
            return 1.0D;
        }

        double normalized = Math.max(1.0D, maxHealth / 20.0D);
        double exponent = Math.max(0.1D, maxHealthScalingExponent);
        double multiplier = Math.pow(normalized, exponent);
        double cap = Math.max(1.0D, maxHealthScalingCap);
        return Math.min(cap, multiplier);
    }

    private void clamp() {
        damageCooldownTicks = clampInt(damageCooldownTicks, 0, 12000);
        minimumHungerPercent = clampInt(minimumHungerPercent, 0, 100);
        updateIntervalTicks = clampInt(updateIntervalTicks, 1, 200);
        baseHealIntervalTicks = clampInt(baseHealIntervalTicks, 1, 12000);
        fullStrengthHealIntervalTicks = clampInt(fullStrengthHealIntervalTicks, 1, 12000);
        rampFullStrengthTicks = clampInt(rampFullStrengthTicks, 1, 12000);
        if (healAmountPerTrigger <= 0.0D) {
            healAmountPerTrigger = 0.5D;
        }
        maxHealthScalingExponent = clampDouble(maxHealthScalingExponent, 0.1D, 4.0D);
        maxHealthScalingCap = clampDouble(maxHealthScalingCap, 1.0D, 100.0D);
        maxRegenHealthPercent = clampInt(maxRegenHealthPercent, 0, 100);
        pvpDamageCooldownTicks = Math.max(-1, Math.min(12000, pvpDamageCooldownTicks));
        if (blockedEffects == null) blockedEffects = new String[0];
        if (dimensionBlacklist == null) dimensionBlacklist = new String[0];
        hungerBonusThresholdPercent = clampInt(hungerBonusThresholdPercent, 0, 100);
        if (hungerBonusHealMultiplier < 1.0D) hungerBonusHealMultiplier = 1.5D;
        if (hungerBonusSpeedMultiplier < 1.0D) hungerBonusSpeedMultiplier = 1.5D;
        hungerBonusCooldownReduction = clampInt(hungerBonusCooldownReduction, 0, 100);
        if (hungerFullBonusHealMultiplier < 1.0D) hungerFullBonusHealMultiplier = 2.0D;
        if (hungerFullBonusSpeedMultiplier < 1.0D) hungerFullBonusSpeedMultiplier = 2.0D;
        regenOnKillCooldownReduction = clampInt(regenOnKillCooldownReduction, 0, 100);
    }

    private static String toJsonArray(String[] arr) {
        if (arr == null || arr.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < arr.length; i++) {
            sb.append("\"").append(arr[i].replace("\"", "\\\"")).append("\"");
            if (i < arr.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
