package io.github.miche.passiveregen.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.miche.passiveregen.PassiveRegenMod;
import io.github.miche.passiveregen.client.PassiveRegenClientMod;
import io.github.miche.passiveregen.client.RegenHudState;
import io.github.miche.passiveregen.config.RegenHudConfig;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import com.mojang.blaze3d.vertex.PoseStack;

@Environment(EnvType.CLIENT)
public final class RegenHudRenderer extends GuiComponent implements HudRenderCallback {
    private static final ResourceLocation HEART_TEXTURE = new ResourceLocation(PassiveRegenMod.MODID, "textures/gui/regen_heart.png");
    private static final int TEX_W = 16;
    private static final int TEX_H = 64;
    private static final float TIMER_TEXT_SCALE = 0.75F;

    @Override
    public void onHudRender(PoseStack matrices, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }

        RegenHudConfig config = PassiveRegenClientMod.CONFIG;
        if (config == null) {
            return;
        }

        RegenHudState state = RegenHudState.get();
        boolean visible = shouldRender(config, state);
        float fadeAlpha = config.hudFadeEnabled
            ? state.updateAndGetFadeAlpha(visible, config.hudFadeInMs, config.hudFadeOutMs)
            : (visible ? 1.0F : 0.0F);
        if (fadeAlpha <= 0.0F) {
            return;
        }

        float scale = (float) config.hudScale;
        float opacity = (float) config.hudOpacity * fadeAlpha;
        float fillOpacity = (float) config.hudOpacity * (visible ? Math.max(fadeAlpha, 0.35F) : fadeAlpha);
        String timerText = getTimerText(state);
        int timerWidth = config.showTimer ? client.font.width(timerText) : 0;
        int[] pos = HudPositionPreset.calculate(config, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight(), config.showTimer, timerWidth, scale);

        boolean withered = state.isWithered() && config.hudWitherEffectEnabled;
        boolean poisoned = state.isPoisoned() && !withered && config.hudPoisonEffectEnabled;
        boolean freezing = false;
        boolean freezingDamaging = false;
        if (!withered && !poisoned && config.hudFreezingEnabled && client.player.getTicksRequiredToFreeze() > 0) {
            float frozenPct = (float) client.player.getTicksFrozen() / (float) client.player.getTicksRequiredToFreeze();
            freezing = frozenPct >= (float) config.hudFreezingThresholdPercent;
            freezingDamaging = frozenPct >= 1.0F;
        }
        boolean hungerBlocked = state.isHungerBlocked() && !withered && !poisoned && !freezing;
        boolean critical = state.isCriticalHealth() && !withered && !poisoned && !hungerBlocked && !freezing;
        boolean campfireActive = state.isNearCampfire() && !withered && !poisoned && !hungerBlocked && !freezing;
        boolean saturationBonus = state.isSaturationBonus() && !withered && !poisoned && !freezing && !campfireActive && config.hudSaturationSheenEnabled;

        float progress = (withered || poisoned) ? 1.0F : (state.isRegenActive() ? 1.0F : state.getCooldownProgress());
        int tint = getHeartTint(config, withered, poisoned, freezing, hungerBlocked, campfireActive, saturationBonus);
        float t = (System.currentTimeMillis() % 3_600_000L) / 1000.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, HEART_TEXTURE);

        matrices.pushPose();
        matrices.translate(pos[0], pos[1], 0.0F);
        matrices.scale(scale, scale, 1.0F);

        if (hungerBlocked) {
            float shake = 0.0F;
            long cyclePosMs = System.currentTimeMillis() % 3500L;
            if (cyclePosMs < 250L) {
                float phase = cyclePosMs / 250.0F;
                shake = (float) Math.sin(phase * Math.PI * 6.0) * 0.6F;
            }
            matrices.translate(shake, 0.0F, 0.0F);
        }

        if (withered) {
            drawGlow(matrices, 0.18F + 0.10F * pulse(t, 1.6F), 0xFF501050, opacity, 1.35F);
        } else if (poisoned) {
            drawGlow(matrices, 0.14F + 0.08F * pulse(t, 1.1F), 0xFF5FD43B, opacity, 1.25F);
        } else if (freezing) {
            float frostPulse = 0.16F + 0.10F * pulse(t, 1.3F);
            if (freezingDamaging) {
                frostPulse *= 1.3F;
            }
            drawGlow(matrices, frostPulse, config.getHudFreezingArgb(), opacity, 1.30F);
        } else if (campfireActive) {
            drawGlow(matrices, 0.16F + 0.10F * pulse(t, 2.5F), 0xFFFFA020, opacity, 1.30F);
        } else if (saturationBonus) {
            drawGlow(matrices, 0.10F + 0.06F * pulse(t, 1.1F), 0xFFFFCC22, opacity, 1.15F);
        } else if (critical) {
            drawGlow(matrices, 0.18F + 0.18F * pulse(t, 4.2F), 0xFFFF3232, opacity, 1.18F);
        } else if (hungerBlocked) {
            drawGlow(matrices, 0.10F + 0.16F * pulse(t, 0.9F), 0xFFDC2828, opacity, 1.20F);
        } else if (!state.isGlowSuppressed() && state.isRegenActive()) {
            float glowT = state.getGlowPhaseSeconds();
            float pulse = config.hudRichAnimations
                ? 0.05F + 0.18F * ((float) Math.sin(glowT * 2.2F) * 0.30F + (float) Math.sin(glowT * 3.7F) * 0.15F + 0.55F)
                : 0.05F + 0.18F * (0.5F + 0.5F * (float) Math.sin(glowT * 2.2F - (float) (Math.PI / 2)));
            drawGlow(matrices, pulse, tint, opacity, 1.25F);
        }

        float thumpScale = state.getHealThumpScale();
        if (thumpScale != 1.0F) {
            matrices.pushPose();
            matrices.translate(8.0F, 8.0F, 0.0F);
            matrices.scale(thumpScale, thumpScale, 1.0F);
            matrices.translate(-8.0F, -8.0F, 0.0F);
        }
        drawHeart(matrices, progress, tint, opacity, fillOpacity);
        if (thumpScale != 1.0F) {
            matrices.popPose();
        }

        float flashAlpha = state.getHealFlashAlpha();
        if (flashAlpha > 0.0F) {
            drawHealFlash(matrices, flashAlpha, opacity);
        }

        drawSparkle(matrices, state, opacity);

        if (config.showTimer && !timerText.isEmpty()) {
            int timerArgb = ((int) (opacity * 255.0F) << 24) | 0xFFFFFF;
            drawTimer(matrices, client.font, timerText, 20, 4, timerArgb);
        }

        matrices.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static int getHeartTint(RegenHudConfig config, boolean withered, boolean poisoned, boolean freezing, boolean hungerBlocked, boolean campfireActive, boolean saturationBonus) {
        if (withered) return 0xFF1A121A;
        if (poisoned) return 0xFF4E9A2A;
        if (freezing && config.hudFreezingTintEnabled) return config.getHudFreezingArgb();
        if (hungerBlocked) return config.getHudBlockedArgb();
        if (campfireActive) return 0xFFFF8C2A;
        if (saturationBonus) return 0xFFFFCC22;
        return config.getHudArgb();
    }

    private static float pulse(float time, float speed) {
        return 0.5F + 0.5F * (float) Math.sin(time * speed);
    }

    private static void drawGlow(PoseStack matrices, float alpha, int tint, float opacity, float scale) {
        float a = (((tint >>> 24) & 0xFF) / 255.0F) * alpha * opacity;
        float r = ((tint >>> 16) & 0xFF) / 255.0F;
        float g = ((tint >>> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, a);
        matrices.pushPose();
        matrices.translate(8.0F, 8.0F, 0.0F);
        matrices.scale(scale, scale, 1.0F);
        matrices.translate(-8.0F, -8.0F, 0.0F);
        blit(matrices, 0, 0, 0, 16, 16, 16, TEX_W, TEX_H);
        matrices.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawHeart(PoseStack matrices, float progress, int tint, float opacity, float fillOpacity) {
        int fillHeight = Mth.ceil(16.0F * Mth.clamp(progress, 0.0F, 1.0F));
        if (fillHeight > 0) {
            float a = (((tint >>> 24) & 0xFF) / 255.0F) * fillOpacity;
            float r = ((tint >>> 16) & 0xFF) / 255.0F;
            float g = ((tint >>> 8) & 0xFF) / 255.0F;
            float b = (tint & 0xFF) / 255.0F;
            int drawY = 16 - fillHeight;
            RenderSystem.setShaderColor(r, g, b, a);
            blit(matrices, 0, drawY, 0, 16 + drawY, 16, fillHeight, TEX_W, TEX_H);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity);
        blit(matrices, 0, 0, 0, 0, 16, 16, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawHealFlash(PoseStack matrices, float alpha, float opacity) {
        float flashScale = 1.10F + (1.0F - alpha) * 0.10F;
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * opacity);
        matrices.pushPose();
        matrices.translate(8.0F, 8.0F, 0.0F);
        matrices.scale(flashScale, flashScale, 1.0F);
        matrices.translate(-8.0F, -8.0F, 0.0F);
        blit(matrices, 0, 0, 0, 32, 16, 16, TEX_W, TEX_H);
        matrices.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawSparkle(PoseStack matrices, RegenHudState state, float opacity) {
        float alpha = state.getSparkleAlpha() * opacity;
        if (alpha <= 0.0F) {
            return;
        }

        float progress = state.getSparkleProgress();
        float ease = 1.0F - (1.0F - progress) * (1.0F - progress);
        float innerR = 2.2F + ease * 2.2F;
        float outerR = innerR + 0.8F + ease * 0.5F;
        int lineColor = ((int) (alpha * 0.9F * 255.0F) << 24) | 0xFFF0C8;
        int dotColor = ((int) (alpha * 255.0F) << 24) | 0xFFFFFF;

        for (int i = 0; i < 8; i++) {
            double angle = (i / 8.0D) * Math.PI * 2.0D + Math.PI * 0.125D;
            int x1 = Math.round(8.0F + (float) Math.cos(angle) * innerR);
            int y1 = Math.round(8.0F + (float) Math.sin(angle) * innerR);
            int x2 = Math.round(8.0F + (float) Math.cos(angle) * outerR);
            int y2 = Math.round(8.0F + (float) Math.sin(angle) * outerR);
            fill(matrices, x1, y1, x2 == x1 ? x1 + 1 : x2, y2 == y1 ? y1 + 1 : y2, lineColor);
            fill(matrices, x2, y2, x2 + 1, y2 + 1, dotColor);
        }
    }

    private static void drawTimer(PoseStack matrices, Font font, String text, int x, int y, int argb) {
        matrices.pushPose();
        matrices.translate(x, y, 0.0F);
        matrices.scale(TIMER_TEXT_SCALE, TIMER_TEXT_SCALE, 1.0F);
        font.drawShadow(matrices, text, 0.0F, 0.0F, argb);
        matrices.popPose();
    }

    private static boolean shouldRender(RegenHudConfig config, RegenHudState state) {
        if (config == null || !config.showRegenHud) {
            return false;
        }

        boolean fullHealth = state.getMaxHealth() > 0.0F && state.getCurrentHealth() >= state.getMaxHealth();
        if (config.hideAtFullHealth && fullHealth && !state.isRegenActive() && state.isReady()) {
            return false;
        }

        String condition = config.showCondition == null ? "injured" : config.showCondition.trim().toLowerCase(Locale.ROOT);
        if ("always".equals(condition)) {
            return true;
        }
        if ("out_of_combat".equals(condition)) {
            return state.isCooldownCounting() || state.isRegenActive() || state.isHungerBlocked() || state.getCurrentHealth() < state.getMaxHealth();
        }
        return state.getCurrentHealth() < state.getMaxHealth() || state.isRegenActive() || state.isHungerBlocked();
    }

    private static String getTimerText(RegenHudState state) {
        if (state.isRegenActive() || state.isReady()) {
            return "";
        }
        return String.format(Locale.ROOT, "%.1fs", state.getSecondsRemaining());
    }
}
