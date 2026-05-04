package com.multiflame.passiveregen.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.multiflame.passiveregen.PassiveRegenConfig;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RegenHudConfig {
    private static final Gson GSON = new GsonBuilder().create();
    private static final Path CONFIG_PATH = Loader.instance().getConfigDir().toPath().resolve("passive-health-regen-hud.json");

    private static RegenHudConfig current;

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
            config = fromLegacyConfig();
        }

        config.sanitize();
        config.save();
        current = config;
        return config;
    }

    public static RegenHudConfig current() {
        return current != null ? current : load();
    }

    private static RegenHudConfig fromLegacyConfig() {
        PassiveRegenConfig.Hud source = PassiveRegenConfig.hud == null ? new PassiveRegenConfig.Hud() : PassiveRegenConfig.hud;
        RegenHudConfig config = new RegenHudConfig();
        config.showRegenHud = source.showRegenHud;
        config.showTimer = source.showTimer;
        config.hudOpacity = source.hudOpacity;
        config.hudFadeEnabled = source.hudFadeEnabled;
        config.hudFadeInMs = source.hudFadeInMs;
        config.hudFadeOutMs = source.hudFadeOutMs;
        config.hudColor = source.hudColor == null ? "FF69B4" : source.hudColor;
        config.hudBlockedColor = source.hudBlockedColor == null ? "FF9F1A" : source.hudBlockedColor;
        config.hudScale = source.hudScale;
        config.hudPosition = source.hudPosition == null ? "LEFT_OF_HEALTH" : source.hudPosition.name();
        config.hudOffsetX = source.hudOffsetX;
        config.hudOffsetY = source.hudOffsetY;
        config.hudCustomAnchor = source.hudCustomAnchor == null ? "TOP_LEFT" : source.hudCustomAnchor.name();
        config.showCondition = source.showCondition == null ? "injured" : source.showCondition.name().toLowerCase();
        config.hideAtFullHealth = source.hideAtFullHealth;
        config.hudRichAnimations = source.hudRichAnimations;
        config.hudSaturationSheenEnabled = source.hudSaturationSheenEnabled;
        config.hudSaturationSparkleEnabled = source.hudSaturationSparkleEnabled;
        config.hudPoisonEffectEnabled = source.hudPoisonEffectEnabled;
        config.hudWitherEffectEnabled = source.hudWitherEffectEnabled;
        config.particles = parseParticles(source.particles);
        config.sounds = parseSounds(source.sounds);
        return config;
    }

    public void save() {
        try {
            sanitize();
            Files.createDirectories(CONFIG_PATH.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                writer.write("{"); writer.newLine();
                writer.newLine();
                writer.write("  // Client-only HUD settings for Passive Health Regen."); writer.newLine();
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
                writer.write("  \"hudRichAnimations\": " + hudRichAnimations + ","); writer.newLine();
                writer.write("  \"hudSaturationSheenEnabled\": " + hudSaturationSheenEnabled + ","); writer.newLine();
                writer.write("  \"hudSaturationSparkleEnabled\": " + hudSaturationSparkleEnabled + ","); writer.newLine();
                writer.write("  \"hudPoisonEffectEnabled\": " + hudPoisonEffectEnabled + ","); writer.newLine();
                writer.write("  \"hudWitherEffectEnabled\": " + hudWitherEffectEnabled + ","); writer.newLine();
                writer.newLine();
                writer.write("  // Cosmetic feedback only. Leave arrays empty to disable."); writer.newLine();
                writer.write("  // Example particles: [{ \"id\": \"minecraft:heart\", \"count\": 3, \"spread\": 0.3 }]"); writer.newLine();
                writer.write("  \"particles\": " + particlesToJson(particles) + ","); writer.newLine();
                writer.write("  // Example sounds: [{ \"id\": \"minecraft:block.note.harp\", \"volume\": 0.25, \"pitch\": 1.9 }]"); writer.newLine();
                writer.write("  \"sounds\": " + soundsToJson(sounds)); writer.newLine();
                writer.write("}"); writer.newLine();
            }
        } catch (IOException ignored) {
        }
    }

    public int getHudArgb() {
        return parseHexColor(hudColor, 0xFFFF69B4);
    }

    public int getHungerBlockedArgb() {
        return parseHexColor(hudBlockedColor, 0xFFFF9F1A);
    }

    private static int parseHexColor(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        String value = hex.trim().replaceAll("(?i)^(0x|#)", "");
        try {
            return 0xFF000000 | (int) Long.parseLong(value, 16);
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
        if (hudColor == null || hudColor.trim().isEmpty()) hudColor = "FF69B4";
        if (hudBlockedColor == null || hudBlockedColor.trim().isEmpty()) hudBlockedColor = "FF9F1A";
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

    private static ParticleEntry[] parseParticles(String[] values) {
        if (values == null || values.length == 0) {
            return new ParticleEntry[0];
        }

        ParticleEntry[] parsed = new ParticleEntry[values.length];
        int count = 0;
        for (String value : values) {
            ParticleEntry entry = parseParticle(value);
            if (entry != null) {
                parsed[count++] = entry;
            }
        }

        if (count == parsed.length) {
            return parsed;
        }

        ParticleEntry[] trimmed = new ParticleEntry[count];
        System.arraycopy(parsed, 0, trimmed, 0, count);
        return trimmed;
    }

    private static SoundEntry[] parseSounds(String[] values) {
        if (values == null || values.length == 0) {
            return new SoundEntry[0];
        }

        SoundEntry[] parsed = new SoundEntry[values.length];
        int count = 0;
        for (String value : values) {
            SoundEntry entry = parseSound(value);
            if (entry != null) {
                parsed[count++] = entry;
            }
        }

        if (count == parsed.length) {
            return parsed;
        }

        SoundEntry[] trimmed = new SoundEntry[count];
        System.arraycopy(parsed, 0, trimmed, 0, count);
        return trimmed;
    }

    private static ParticleEntry parseParticle(String value) {
        if (value == null) {
            return null;
        }

        String[] parts = value.trim().split(";");
        if (parts.length == 0 || parts[0].trim().isEmpty()) {
            return null;
        }

        int count = 1;
        double spread = 0.0D;
        try {
            if (parts.length > 1) {
                count = Integer.parseInt(parts[1].trim());
            }
            if (parts.length > 2) {
                spread = Double.parseDouble(parts[2].trim());
            }
        } catch (NumberFormatException ignored) {
            return null;
        }

        return new ParticleEntry(parts[0].trim(), count, spread);
    }

    private static SoundEntry parseSound(String value) {
        if (value == null) {
            return null;
        }

        String[] parts = value.trim().split(";");
        if (parts.length == 0 || parts[0].trim().isEmpty()) {
            return null;
        }

        float volume = 1.0F;
        float pitch = 1.0F;
        try {
            if (parts.length > 1) {
                volume = Float.parseFloat(parts[1].trim());
            }
            if (parts.length > 2) {
                pitch = Float.parseFloat(parts[2].trim());
            }
        } catch (NumberFormatException ignored) {
            return null;
        }

        return new SoundEntry(parts[0].trim(), volume, pitch);
    }

    private static String particlesToJson(ParticleEntry[] entries) {
        if (entries == null || entries.length == 0) return "[]";
        StringBuilder builder = new StringBuilder("[");
        for (ParticleEntry entry : entries) {
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
        for (SoundEntry entry : entries) {
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
