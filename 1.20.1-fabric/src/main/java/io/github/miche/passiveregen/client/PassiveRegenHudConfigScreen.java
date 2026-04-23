package io.github.miche.passiveregen.client;

import io.github.miche.passiveregen.config.RegenHudConfig;
import java.util.Arrays;
import java.util.stream.Collectors;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PassiveRegenHudConfigScreen {
    private static final PositionPreset[] POSITION_PRESETS = PositionPreset.values();
    private static final AnchorPreset[] ANCHOR_PRESETS = AnchorPreset.values();
    private static final ShowCondition[] SHOW_CONDITIONS = ShowCondition.values();

    private PassiveRegenHudConfigScreen() {
    }

    public static Screen create(Screen parent) {
        RegenHudConfig working = PassiveRegenClientMod.CONFIG.copy();
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("title.passiveregen.hud_config"));

        ConfigEntryBuilder entries = builder.entryBuilder();

        ConfigCategory hud = builder.getOrCreateCategory(Component.translatable("config.passiveregen.category.hud"));
        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.show_hud"), working.showRegenHud)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.showRegenHud = value)
            .build());
        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.show_timer"), working.showTimer)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.showTimer = value)
            .build());
        hud.addEntry(entries.startDoubleField(Component.translatable("config.passiveregen.hud_opacity"), working.hudOpacity)
            .setDefaultValue(1.0D)
            .setSaveConsumer(value -> working.hudOpacity = value)
            .build());
        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.hud_fade_enabled"), working.hudFadeEnabled)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.hudFadeEnabled = value)
            .build());
        hud.addEntry(entries.startIntField(Component.translatable("config.passiveregen.hud_fade_in_ms"), working.hudFadeInMs)
            .setDefaultValue(500)
            .setSaveConsumer(value -> working.hudFadeInMs = value)
            .build());
        hud.addEntry(entries.startIntField(Component.translatable("config.passiveregen.hud_fade_out_ms"), working.hudFadeOutMs)
            .setDefaultValue(400)
            .setSaveConsumer(value -> working.hudFadeOutMs = value)
            .build());
        hud.addEntry(entries.startStrField(Component.translatable("config.passiveregen.hud_color"), working.hudColor)
            .setDefaultValue("FF69B4")
            .setSaveConsumer(value -> working.hudColor = value)
            .build());
        hud.addEntry(entries.startStrField(Component.translatable("config.passiveregen.hud_blocked_color"), working.hudBlockedColor)
            .setDefaultValue("FF9F1A")
            .setSaveConsumer(value -> working.hudBlockedColor = value)
            .build());
        hud.addEntry(entries.startDoubleField(Component.translatable("config.passiveregen.hud_scale"), working.hudScale)
            .setDefaultValue(1.0D)
            .setSaveConsumer(value -> working.hudScale = value)
            .build());
        hud.addEntry(entries.startSelector(Component.translatable("config.passiveregen.hud_position"), POSITION_PRESETS, PositionPreset.fromConfig(working.hudPosition))
            .setDefaultValue(PositionPreset.LEFT_OF_HEALTH)
            .setNameProvider(value -> Component.translatable(value.translationKey))
            .setSaveConsumer(value -> working.hudPosition = value.configValue)
            .build());
        hud.addEntry(entries.startIntField(Component.translatable("config.passiveregen.hud_offset_x"), working.hudOffsetX)
            .setDefaultValue(0)
            .setSaveConsumer(value -> working.hudOffsetX = value)
            .build());
        hud.addEntry(entries.startIntField(Component.translatable("config.passiveregen.hud_offset_y"), working.hudOffsetY)
            .setDefaultValue(0)
            .setSaveConsumer(value -> working.hudOffsetY = value)
            .build());
        hud.addEntry(entries.startSelector(Component.translatable("config.passiveregen.hud_custom_anchor"), ANCHOR_PRESETS, AnchorPreset.fromConfig(working.hudCustomAnchor))
            .setDefaultValue(AnchorPreset.TOP_LEFT)
            .setNameProvider(value -> Component.translatable(value.translationKey))
            .setSaveConsumer(value -> working.hudCustomAnchor = value.configValue)
            .build());
        hud.addEntry(entries.startSelector(Component.translatable("config.passiveregen.show_condition"), SHOW_CONDITIONS, ShowCondition.fromConfig(working.showCondition))
            .setDefaultValue(ShowCondition.INJURED)
            .setNameProvider(value -> Component.translatable(value.translationKey))
            .setSaveConsumer(value -> working.showCondition = value.configValue)
            .build());
        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.hide_at_full_health"), working.hideAtFullHealth)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.hideAtFullHealth = value)
            .build());
        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.hud_rich_animations"), working.hudRichAnimations)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.hudRichAnimations = value)
            .build());

        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.hud_freezing_enabled"), working.hudFreezingEnabled)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.hudFreezingEnabled = value)
            .build());
        hud.addEntry(entries.startDoubleField(Component.translatable("config.passiveregen.hud_freezing_threshold"), working.hudFreezingThresholdPercent)
            .setDefaultValue(0.4D)
            .setSaveConsumer(value -> working.hudFreezingThresholdPercent = value)
            .build());
        hud.addEntry(entries.startStrField(Component.translatable("config.passiveregen.hud_freezing_color"), working.hudFreezingColor)
            .setDefaultValue("88DDFF")
            .setSaveConsumer(value -> working.hudFreezingColor = value)
            .build());
        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.hud_freezing_tint_enabled"), working.hudFreezingTintEnabled)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.hudFreezingTintEnabled = value)
            .build());
        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.hud_freezing_frost_enabled"), working.hudFreezingFrostEnabled)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.hudFreezingFrostEnabled = value)
            .build());
        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.hud_freezing_shake_enabled"), working.hudFreezingShakeEnabled)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.hudFreezingShakeEnabled = value)
            .build());
        hud.addEntry(entries.startDoubleField(Component.translatable("config.passiveregen.hud_freezing_shake_intensity"), working.hudFreezingShakeIntensity)
            .setDefaultValue(0.2D)
            .setSaveConsumer(value -> working.hudFreezingShakeIntensity = value)
            .build());
        hud.addEntry(entries.startDoubleField(Component.translatable("config.passiveregen.hud_freezing_shake_speed"), working.hudFreezingShakeSpeedHz)
            .setDefaultValue(2.25D)
            .setSaveConsumer(value -> working.hudFreezingShakeSpeedHz = value)
            .build());
        hud.addEntry(entries.startBooleanToggle(Component.translatable("config.passiveregen.hud_freezing_snow_enabled"), working.hudFreezingSnowEnabled)
            .setDefaultValue(true)
            .setSaveConsumer(value -> working.hudFreezingSnowEnabled = value)
            .build());
        hud.addEntry(entries.startIntField(Component.translatable("config.passiveregen.hud_freezing_snow_count"), working.hudFreezingSnowCount)
            .setDefaultValue(4)
            .setSaveConsumer(value -> working.hudFreezingSnowCount = value)
            .build());

        ConfigCategory feedback = builder.getOrCreateCategory(Component.translatable("config.passiveregen.category.feedback"));
        feedback.addEntry(entries.startStrField(Component.translatable("config.passiveregen.particles"), particlesToString(working))
            .setDefaultValue("minecraft:heart;3;0.3")
            .setSaveConsumer(value -> working.particles = parseParticles(value))
            .build());
        feedback.addEntry(entries.startStrField(Component.translatable("config.passiveregen.sounds"), soundsToString(working))
            .setDefaultValue("minecraft:entity.player.levelup;0.25;1.9")
            .setSaveConsumer(value -> working.sounds = parseSounds(value))
            .build());

        builder.setSavingRunnable(() -> {
            working.sanitize();
            PassiveRegenClientMod.CONFIG.copyFrom(working);
            PassiveRegenClientMod.CONFIG.save();
        });

        return builder.build();
    }

    private static String particlesToString(RegenHudConfig config) {
        if (config.particles == null || config.particles.length == 0) return "";
        return Arrays.stream(config.particles)
            .filter(entry -> entry != null)
            .map(entry -> entry.id + ";" + entry.count + ";" + entry.spread)
            .collect(Collectors.joining(", "));
    }

    private static String soundsToString(RegenHudConfig config) {
        if (config.sounds == null || config.sounds.length == 0) return "";
        return Arrays.stream(config.sounds)
            .filter(entry -> entry != null)
            .map(entry -> entry.id + ";" + entry.volume + ";" + entry.pitch)
            .collect(Collectors.joining(", "));
    }

    private static RegenHudConfig.ParticleEntry[] parseParticles(String value) {
        if (value == null || value.trim().isEmpty()) return new RegenHudConfig.ParticleEntry[0];
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .map(part -> {
                String[] split = part.split(";");
                String id = split.length > 0 ? split[0].trim() : "minecraft:heart";
                int count = split.length > 1 ? parseInt(split[1], 3) : 3;
                double spread = split.length > 2 ? parseDouble(split[2], 0.3D) : 0.3D;
                return new RegenHudConfig.ParticleEntry(id, count, spread);
            })
            .toArray(RegenHudConfig.ParticleEntry[]::new);
    }

    private static RegenHudConfig.SoundEntry[] parseSounds(String value) {
        if (value == null || value.trim().isEmpty()) return new RegenHudConfig.SoundEntry[0];
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .map(part -> {
                String[] split = part.split(";");
                String id = split.length > 0 ? split[0].trim() : "minecraft:entity.player.levelup";
                float volume = split.length > 1 ? (float) parseDouble(split[1], 0.25D) : 0.25F;
                float pitch = split.length > 2 ? (float) parseDouble(split[2], 1.9D) : 1.9F;
                return new RegenHudConfig.SoundEntry(id, volume, pitch);
            })
            .toArray(RegenHudConfig.SoundEntry[]::new);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private enum PositionPreset {
        RIGHT_OF_HEALTH("RIGHT_OF_HEALTH", "config.passiveregen.hud_position.right_of_health"),
        LEFT_OF_HEALTH("LEFT_OF_HEALTH", "config.passiveregen.hud_position.left_of_health"),
        ABOVE_HUNGER("ABOVE_HUNGER", "config.passiveregen.hud_position.above_hunger"),
        BELOW_HUNGER("BELOW_HUNGER", "config.passiveregen.hud_position.below_hunger"),
        TOP_LEFT("TOP_LEFT", "config.passiveregen.hud_position.top_left"),
        TOP_RIGHT("TOP_RIGHT", "config.passiveregen.hud_position.top_right"),
        BOTTOM_LEFT("BOTTOM_LEFT", "config.passiveregen.hud_position.bottom_left"),
        BOTTOM_RIGHT("BOTTOM_RIGHT", "config.passiveregen.hud_position.bottom_right"),
        CUSTOM("CUSTOM", "config.passiveregen.hud_position.custom");

        private final String configValue;
        private final String translationKey;

        PositionPreset(String configValue, String translationKey) {
            this.configValue = configValue;
            this.translationKey = translationKey;
        }

        private static PositionPreset fromConfig(String value) {
            if (value != null) {
                for (PositionPreset preset : values()) {
                    if (preset.configValue.equalsIgnoreCase(value.trim())) {
                        return preset;
                    }
                }
            }
            return RIGHT_OF_HEALTH;
        }
    }

    private enum AnchorPreset {
        CENTER("CENTER", "config.passiveregen.hud_anchor.center"),
        TOP("TOP", "config.passiveregen.hud_anchor.top"),
        BOTTOM("BOTTOM", "config.passiveregen.hud_anchor.bottom"),
        LEFT("LEFT", "config.passiveregen.hud_anchor.left"),
        RIGHT("RIGHT", "config.passiveregen.hud_anchor.right"),
        TOP_LEFT("TOP_LEFT", "config.passiveregen.hud_anchor.top_left"),
        TOP_RIGHT("TOP_RIGHT", "config.passiveregen.hud_anchor.top_right"),
        BOTTOM_LEFT("BOTTOM_LEFT", "config.passiveregen.hud_anchor.bottom_left"),
        BOTTOM_RIGHT("BOTTOM_RIGHT", "config.passiveregen.hud_anchor.bottom_right");

        private final String configValue;
        private final String translationKey;

        AnchorPreset(String configValue, String translationKey) {
            this.configValue = configValue;
            this.translationKey = translationKey;
        }

        private static AnchorPreset fromConfig(String value) {
            if (value != null) {
                for (AnchorPreset preset : values()) {
                    if (preset.configValue.equalsIgnoreCase(value.trim())) {
                        return preset;
                    }
                }
            }
            return TOP_LEFT;
        }
    }

    private enum ShowCondition {
        INJURED("injured", "config.passiveregen.show_condition.injured"),
        OUT_OF_COMBAT("out_of_combat", "config.passiveregen.show_condition.out_of_combat"),
        ALWAYS("always", "config.passiveregen.show_condition.always");

        private final String configValue;
        private final String translationKey;

        ShowCondition(String configValue, String translationKey) {
            this.configValue = configValue;
            this.translationKey = translationKey;
        }

        private static ShowCondition fromConfig(String value) {
            if (value != null) {
                for (ShowCondition condition : values()) {
                    if (condition.configValue.equalsIgnoreCase(value.trim())) {
                        return condition;
                    }
                }
            }
            return INJURED;
        }
    }
}
