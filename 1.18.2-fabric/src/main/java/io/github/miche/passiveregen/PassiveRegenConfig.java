package io.github.miche.passiveregen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class PassiveRegenConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
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

    public static PassiveRegenConfig load() {
        PassiveRegenConfig config = null;
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
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
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException ignored) {
        }
    }

    public float getHealAmountPerUpdate(long outOfCombatTicks, float maxHealth) {
        if (!enabled) {
            return 0.0F;
        }

        double healAmount = Math.max(0.0D, healAmountPerTrigger);
        return (float) (healAmount * getMaxHealthScaleMultiplier(maxHealth));
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
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
