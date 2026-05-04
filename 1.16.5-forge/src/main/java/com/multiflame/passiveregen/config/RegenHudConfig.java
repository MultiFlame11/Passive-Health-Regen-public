package com.multiflame.passiveregen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLPaths;

@OnlyIn(Dist.CLIENT)
public final class RegenHudConfig {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("passive-health-regen-hud.json");

    public boolean showRegenHud = true;
    public boolean showTimer = false;
    public double hudOpacity = 1.0D;
    public boolean hudFadeEnabled = true;
    public int hudFadeInMs = 500;
    public int hudFadeOutMs = 400;
    public String hudColor = "FF69B4";
    public String hudBlockedColor = "FF9F1A";
    public double hudScale = 1.0D;
    public String hudPosition = "LEFT_OF_HEALTH";
    public int hudOffsetX = 0;
    public int hudOffsetY = 0;
    public String hudCustomAnchor = "TOP_LEFT";
    public String showCondition = "injured";
    public boolean hideAtFullHealth = true;
    public boolean hudRichAnimations = true;

    public boolean hudFreezingEnabled = true;
    public double hudFreezingThresholdPercent = 0.4D;
    public String hudFreezingColor = "88DDFF";
    public boolean hudFreezingTintEnabled = true;
    public boolean hudFreezingFrostEnabled = true;
    public boolean hudFreezingShakeEnabled = true;
    public double hudFreezingShakeIntensity = 0.2D;
    public double hudFreezingShakeSpeedHz = 2.25D;
    public boolean hudFreezingSnowEnabled = true;
    public int hudFreezingSnowCount = 4;

    public boolean hudSaturationSheenEnabled = true;
    public boolean hudSaturationSparkleEnabled = true;

    public boolean hudPoisonEffectEnabled = true;
    public boolean hudWitherEffectEnabled = true;

    public ParticleEntry[] particles = new ParticleEntry[0];

    public SoundEntry[] sounds = new SoundEntry[0];

    public static RegenHudConfig load() {
        RegenHudConfig config = null;
        if (Files.exists(CONFIG_PATH)) {
            try (JsonReader reader = new JsonReader(Files.newBufferedReader(CONFIG_PATH))) {
                reader.setLenient(true);
                config = GSON.fromJson(reader, RegenHudConfig.class);
            } catch (IOException ignored) {
            }
        }

        if (config == null) {
            config = new RegenHudConfig();
        }

        config.sanitize();
        config.save();
        return config;
    }

    public RegenHudConfig copy() {
        RegenHudConfig copy = new RegenHudConfig();
        copy.showRegenHud = showRegenHud;
        copy.showTimer = showTimer;
        copy.hudOpacity = hudOpacity;
        copy.hudFadeEnabled = hudFadeEnabled;
        copy.hudFadeInMs = hudFadeInMs;
        copy.hudFadeOutMs = hudFadeOutMs;
        copy.hudColor = hudColor;
        copy.hudBlockedColor = hudBlockedColor;
        copy.hudScale = hudScale;
        copy.hudPosition = hudPosition;
        copy.hudOffsetX = hudOffsetX;
        copy.hudOffsetY = hudOffsetY;
        copy.hudCustomAnchor = hudCustomAnchor;
        copy.showCondition = showCondition;
        copy.hideAtFullHealth = hideAtFullHealth;
        copy.hudRichAnimations = hudRichAnimations;
        copy.hudFreezingEnabled = hudFreezingEnabled;
        copy.hudFreezingThresholdPercent = hudFreezingThresholdPercent;
        copy.hudFreezingColor = hudFreezingColor;
        copy.hudFreezingTintEnabled = hudFreezingTintEnabled;
        copy.hudFreezingFrostEnabled = hudFreezingFrostEnabled;
        copy.hudFreezingShakeEnabled = hudFreezingShakeEnabled;
        copy.hudFreezingShakeIntensity = hudFreezingShakeIntensity;
        copy.hudFreezingShakeSpeedHz = hudFreezingShakeSpeedHz;
        copy.hudFreezingSnowEnabled = hudFreezingSnowEnabled;
        copy.hudFreezingSnowCount = hudFreezingSnowCount;
        copy.hudSaturationSheenEnabled = hudSaturationSheenEnabled;
        copy.hudSaturationSparkleEnabled = hudSaturationSparkleEnabled;
        copy.hudPoisonEffectEnabled = hudPoisonEffectEnabled;
        copy.hudWitherEffectEnabled = hudWitherEffectEnabled;
        copy.particles = copyParticles(particles);
        copy.sounds = copySounds(sounds);
        return copy;
    }

    public void copyFrom(RegenHudConfig other) {
        showRegenHud = other.showRegenHud;
        showTimer = other.showTimer;
        hudOpacity = other.hudOpacity;
        hudFadeEnabled = other.hudFadeEnabled;
        hudFadeInMs = other.hudFadeInMs;
        hudFadeOutMs = other.hudFadeOutMs;
        hudColor = other.hudColor;
        hudBlockedColor = other.hudBlockedColor;
        hudScale = other.hudScale;
        hudPosition = other.hudPosition;
        hudOffsetX = other.hudOffsetX;
        hudOffsetY = other.hudOffsetY;
        hudCustomAnchor = other.hudCustomAnchor;
        showCondition = other.showCondition;
        hideAtFullHealth = other.hideAtFullHealth;
        hudRichAnimations = other.hudRichAnimations;
        hudFreezingEnabled = other.hudFreezingEnabled;
        hudFreezingThresholdPercent = other.hudFreezingThresholdPercent;
        hudFreezingColor = other.hudFreezingColor;
        hudFreezingTintEnabled = other.hudFreezingTintEnabled;
        hudFreezingFrostEnabled = other.hudFreezingFrostEnabled;
        hudFreezingShakeEnabled = other.hudFreezingShakeEnabled;
        hudFreezingShakeIntensity = other.hudFreezingShakeIntensity;
        hudFreezingShakeSpeedHz = other.hudFreezingShakeSpeedHz;
        hudFreezingSnowEnabled = other.hudFreezingSnowEnabled;
        hudFreezingSnowCount = other.hudFreezingSnowCount;
        hudSaturationSheenEnabled = other.hudSaturationSheenEnabled;
        hudSaturationSparkleEnabled = other.hudSaturationSparkleEnabled;
        hudPoisonEffectEnabled = other.hudPoisonEffectEnabled;
        hudWitherEffectEnabled = other.hudWitherEffectEnabled;
        particles = copyParticles(other.particles);
        sounds = copySounds(other.sounds);
    }

    public void save() {
        try {
            sanitize();
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                writer.write("{"); writer.newLine();
                writer.newLine();
                writer.write("  // Master toggle for the passive regen HUD widget."); writer.newLine();
                writer.write("  \"showRegenHud\": " + showRegenHud + ","); writer.newLine();
                writer.write("  \"showTimer\": " + showTimer + ","); writer.newLine();
                writer.write("  \"hudOpacity\": " + hudOpacity + ","); writer.newLine();
                writer.write("  \"hudFadeEnabled\": " + hudFadeEnabled + ","); writer.newLine();
                writer.write("  \"hudFadeInMs\": " + hudFadeInMs + ","); writer.newLine();
                writer.write("  \"hudFadeOutMs\": " + hudFadeOutMs + ","); writer.newLine();
                writer.write("  \"hudColor\": \"" + escape(hudColor) + "\","); writer.newLine();
                writer.write("  \"hudBlockedColor\": \"" + escape(hudBlockedColor) + "\","); writer.newLine();
                writer.write("  \"hudScale\": " + hudScale + ","); writer.newLine();
                writer.write("  // Presets: RIGHT_OF_HEALTH, LEFT_OF_HEALTH, ABOVE_HUNGER, BELOW_HUNGER, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CUSTOM"); writer.newLine();
                writer.write("  \"hudPosition\": \"" + escape(hudPosition) + "\","); writer.newLine();
                writer.write("  \"hudOffsetX\": " + hudOffsetX + ","); writer.newLine();
                writer.write("  \"hudOffsetY\": " + hudOffsetY + ","); writer.newLine();
                writer.write("  // Anchors for CUSTOM: CENTER, TOP, BOTTOM, LEFT, RIGHT, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT"); writer.newLine();
                writer.write("  \"hudCustomAnchor\": \"" + escape(hudCustomAnchor) + "\","); writer.newLine();
                writer.write("  // Show conditions: injured, out_of_combat, always"); writer.newLine();
                writer.write("  \"showCondition\": \"" + escape(showCondition) + "\","); writer.newLine();
                writer.write("  \"hideAtFullHealth\": " + hideAtFullHealth + ","); writer.newLine();
                writer.write("  // If true: heartbeat thump on heal + organic multi-sine glow. false = simpler flat animation."); writer.newLine();
                writer.write("  \"hudRichAnimations\": " + hudRichAnimations + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Freezing state (1.17+). Triggers when frozen progress crosses threshold."); writer.newLine();
                writer.write("  \"hudFreezingEnabled\": " + hudFreezingEnabled + ","); writer.newLine();
                writer.write("  \"hudFreezingTintEnabled\": " + hudFreezingTintEnabled + ","); writer.newLine();
                writer.write("  \"hudFreezingFrostEnabled\": " + hudFreezingFrostEnabled + ","); writer.newLine();
                writer.write("  \"hudFreezingShakeEnabled\": " + hudFreezingShakeEnabled + ","); writer.newLine();
                writer.write("  // 0.0 = shows at first frost tick, 1.0 = only when fully frozen and taking damage"); writer.newLine();
                writer.write("  \"hudFreezingThresholdPercent\": " + hudFreezingThresholdPercent + ","); writer.newLine();
                writer.write("  \"hudFreezingColor\": \"" + escape(hudFreezingColor) + "\","); writer.newLine();
                writer.write("  \"hudFreezingShakeIntensity\": " + hudFreezingShakeIntensity + ","); writer.newLine();
                writer.write("  \"hudFreezingShakeSpeedHz\": " + hudFreezingShakeSpeedHz + ","); writer.newLine();
                writer.write("  \"hudFreezingSnowEnabled\": " + hudFreezingSnowEnabled + ","); writer.newLine();
                writer.write("  \"hudFreezingSnowCount\": " + hudFreezingSnowCount + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Shiny gold HUD effect while the food-saturation bonus is active on the server."); writer.newLine();
                writer.write("  \"hudSaturationSheenEnabled\": " + hudSaturationSheenEnabled + ","); writer.newLine();
                writer.write("  \"hudSaturationSparkleEnabled\": " + hudSaturationSparkleEnabled + ","); writer.newLine();
                writer.write("  \"hudPoisonEffectEnabled\": " + hudPoisonEffectEnabled + ","); writer.newLine();
                writer.write("  \"hudWitherEffectEnabled\": " + hudWitherEffectEnabled + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Cosmetic feedback only. Leave arrays empty to disable."); writer.newLine();
                writer.write("  // Example particles: [{ \"id\": \"minecraft:heart\", \"count\": 3, \"spread\": 0.3 }]"); writer.newLine();
                writer.write("  \"particles\": " + particlesToJson(particles) + ","); writer.newLine();
                writer.write("  // Example sounds: [{ \"id\": \"minecraft:entity.player.levelup\", \"volume\": 0.25, \"pitch\": 1.9 }]"); writer.newLine();
                writer.write("  \"sounds\": " + soundsToJson(sounds)); writer.newLine();
                writer.write("}"); writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    public int getHudArgb() {
        String value = hudColor == null ? "FF69B4" : hudColor.trim();
        return parseColor(value, 0xFFFF69B4);
    }

    public int getHudBlockedArgb() {
        String value = hudBlockedColor == null ? "FF9F1A" : hudBlockedColor.trim();
        return parseColor(value, 0xFFFF9F1A);
    }

    public int getHudFreezingArgb() {
        String value = hudFreezingColor == null ? "88DDFF" : hudFreezingColor.trim();
        return parseColor(value, 0xFF88DDFF);
    }

    private static int parseColor(String value, int fallback) {
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        if (value.length() == 6) {
            value = "FF" + value;
        }
        try {
            return (int) Long.parseLong(value, 16);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    public void sanitize() {
        hudOpacity = clampDouble(hudOpacity, 0.0D, 1.0D);
        hudFadeInMs = clampInt(hudFadeInMs, 0, 3000);
        hudFadeOutMs = clampInt(hudFadeOutMs, 0, 3000);
        hudScale = clampDouble(hudScale, 0.5D, 4.0D);
        hudOffsetX = clampInt(hudOffsetX, -2000, 2000);
        hudOffsetY = clampInt(hudOffsetY, -2000, 2000);
        hudFreezingThresholdPercent = clampDouble(hudFreezingThresholdPercent, 0.0D, 1.0D);
        hudFreezingShakeIntensity = clampDouble(hudFreezingShakeIntensity, 0.0D, 5.0D);
        hudFreezingShakeSpeedHz = clampDouble(hudFreezingShakeSpeedHz, 0.0D, 60.0D);
        hudFreezingSnowCount = clampInt(hudFreezingSnowCount, 0, 24);
        if (hudPosition == null || hudPosition.trim().isEmpty()) hudPosition = "LEFT_OF_HEALTH";
        if (hudCustomAnchor == null || hudCustomAnchor.trim().isEmpty()) hudCustomAnchor = "TOP_LEFT";
        if (showCondition == null || showCondition.trim().isEmpty()) showCondition = "injured";
        if (particles == null) particles = new ParticleEntry[0];
        if (sounds == null) sounds = new SoundEntry[0];
        for (ParticleEntry entry : particles) {
            if (entry != null) {
                entry.count = clampInt(entry.count, 0, 64);
                entry.spread = clampDouble(entry.spread, 0.0D, 8.0D);
            }
        }
        for (SoundEntry entry : sounds) {
            if (entry != null) {
                entry.volume = (float) clampDouble(entry.volume, 0.0D, 4.0D);
                entry.pitch = (float) clampDouble(entry.pitch, 0.1D, 4.0D);
            }
        }
    }

    private static ParticleEntry[] copyParticles(ParticleEntry[] source) {
        if (source == null) return new ParticleEntry[0];
        ParticleEntry[] copy = new ParticleEntry[source.length];
        for (int i = 0; i < source.length; i++) {
            ParticleEntry entry = source[i];
            copy[i] = entry == null ? null : new ParticleEntry(entry.id, entry.count, entry.spread);
        }
        return copy;
    }

    private static SoundEntry[] copySounds(SoundEntry[] source) {
        if (source == null) return new SoundEntry[0];
        SoundEntry[] copy = new SoundEntry[source.length];
        for (int i = 0; i < source.length; i++) {
            SoundEntry entry = source[i];
            copy[i] = entry == null ? null : new SoundEntry(entry.id, entry.volume, entry.pitch);
        }
        return copy;
    }

    private static String particlesToJson(ParticleEntry[] entries) {
        if (entries == null || entries.length == 0) return "[]";
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < entries.length; i++) {
            ParticleEntry entry = entries[i];
            if (entry == null) continue;
            if (builder.length() > 1) builder.append(", ");
            builder.append("{ \"id\": \"").append(escape(entry.id)).append("\", \"count\": ").append(entry.count)
                .append(", \"spread\": ").append(entry.spread).append(" }");
        }
        builder.append("]");
        return builder.toString();
    }

    private static String soundsToJson(SoundEntry[] entries) {
        if (entries == null || entries.length == 0) return "[]";
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < entries.length; i++) {
            SoundEntry entry = entries[i];
            if (entry == null) continue;
            if (builder.length() > 1) builder.append(", ");
            builder.append("{ \"id\": \"").append(escape(entry.id)).append("\", \"volume\": ").append(entry.volume)
                .append(", \"pitch\": ").append(entry.pitch).append(" }");
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

    public static final class ParticleEntry {
        public String id;
        public int count;
        public double spread;

        public ParticleEntry(String id, int count, double spread) {
            this.id = id;
            this.count = count;
            this.spread = spread;
        }
    }

    public static final class SoundEntry {
        public String id;
        public float volume;
        public float pitch;

        public SoundEntry(String id, float volume, float pitch) {
            this.id = id;
            this.volume = volume;
            this.pitch = pitch;
        }
    }
}
