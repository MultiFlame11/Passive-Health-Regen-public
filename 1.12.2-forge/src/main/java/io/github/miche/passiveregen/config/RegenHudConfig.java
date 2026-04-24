package io.github.miche.passiveregen.config;

import io.github.miche.passiveregen.PassiveRegenConfig;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RegenHudConfig {
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

    public static RegenHudConfig current() {
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
        config.sanitize();
        return config;
    }

    public int getHudArgb() {
        return parseHexColor(hudColor, 0xFFFF69B4);
    }

    public int getHungerBlockedArgb() {
        return parseHexColor(hudBlockedColor, 0xFFFF9F1A);
    }

    private static int parseHexColor(String hex, int fallback) {
        if (hex == null || hex.trim().isEmpty()) return fallback;
        String s = hex.trim().replaceAll("(?i)^(0x|#)", "");
        try {
            return 0xFF000000 | (int) Long.parseLong(s, 16);
        } catch (NumberFormatException e) {
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

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
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
