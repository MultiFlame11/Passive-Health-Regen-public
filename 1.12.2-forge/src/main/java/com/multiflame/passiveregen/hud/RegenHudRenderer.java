package com.multiflame.passiveregen.hud;

import com.multiflame.passiveregen.PassiveRegenMod;
import com.multiflame.passiveregen.client.RegenHudState;
import com.multiflame.passiveregen.config.RegenHudConfig;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class RegenHudRenderer extends Gui {
    private static final ResourceLocation HEART_TEXTURE = new ResourceLocation(PassiveRegenMod.MODID, "textures/gui/regen_heart.png");
    private static final float TIMER_TEXT_SCALE = 0.75F;
    private static final int TEX_W = 16;
    private static final int TEX_H = 64;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        Minecraft client = Minecraft.getMinecraft();
        if (client.player == null || client.gameSettings.hideGUI) {
            return;
        }

        RegenHudConfig config = RegenHudConfig.current();
        RegenHudState state = RegenHudState.get();
        boolean logicallyVisible = shouldRender(config, state);

        float fadeAlpha;
        if (config.hudFadeEnabled) {
            fadeAlpha = state.updateAndGetFadeAlpha(logicallyVisible, config.hudFadeInMs, config.hudFadeOutMs);
        } else {
            fadeAlpha = logicallyVisible ? 1.0F : 0.0F;
        }
        if (fadeAlpha <= 0.0F) return;

        float scale = (float) config.hudScale;
        float opacity = (float) config.hudOpacity * fadeAlpha;
        float fillOpacity = (float) config.hudOpacity * (logicallyVisible ? Math.max(fadeAlpha, 0.35F) : fadeAlpha);
        String timerText = getTimerText(state);
        int timerWidth = config.showTimer ? client.fontRenderer.getStringWidth(timerText) : 0;
        ScaledResolution resolution = event.getResolution();
        int[] pos = HudPositionPreset.calculate(config, resolution.getScaledWidth(), resolution.getScaledHeight(), config.showTimer, timerWidth, scale);

        float t = System.currentTimeMillis() / 1000.0F;

        boolean withered = config.hudWitherEffectEnabled && state.isWithered();
        boolean poisoned = !withered && config.hudPoisonEffectEnabled && state.isPoisoned();
        boolean hungerBlocked = state.isHungerBlocked() && !withered && !poisoned;
        boolean saturationBonus = !withered && !poisoned && !hungerBlocked && config.hudSaturationSheenEnabled && state.isSaturationBonus();
        boolean critical = !hungerBlocked && !withered && !poisoned && state.isCriticalHealth();
        float progress = (state.isRegenActive() || withered || poisoned) ? 1.0F : state.getCooldownProgress();
        int tint;
        if (withered) {
            tint = 0xFF1A121A;
        } else if (poisoned) {
            tint = 0xFF4E9A2A;
        } else if (hungerBlocked) {
            tint = config.getHungerBlockedArgb();
        } else {
            tint = config.getHudArgb();
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.translate(pos[0], pos[1], 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);

        if (critical) {
            float shakeX = (float) (Math.sin(t * 25.0) * 0.6 + Math.sin(t * 41.0) * 0.3);
            float shakeY = (float) (Math.sin(t * 29.0) * 0.5 + Math.sin(t * 37.0) * 0.2);
            GlStateManager.translate(shakeX, shakeY, 0.0F);
        }

        if (hungerBlocked) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(8.0F, 8.0F, 0.0F);
            GlStateManager.rotate(-3.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(-8.0F, -8.0F, 0.0F);
        }

        float thumpScale = config.hudRichAnimations ? state.getHealThumpScale() : 1.0F;
        if (thumpScale != 1.0F) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(8.0F, 8.0F, 0.0F);
            GlStateManager.scale(thumpScale, thumpScale, 1.0F);
            GlStateManager.translate(-8.0F, -8.0F, 0.0F);
        }
        drawHeart(client, progress, tint, opacity, fillOpacity);
        if (thumpScale != 1.0F) {
            GlStateManager.popMatrix();
        }

        if (hungerBlocked) {
            GlStateManager.popMatrix();
        }

        float flashAlpha = state.getHealFlashAlpha();
        boolean glowSuppressed = state.isGlowSuppressed();

        if (withered) {
            float witherPulse = 0.14F + 0.08F * (0.5F + 0.5F * (float) Math.sin(t * 1.6F));
            drawGlow(client, witherPulse, 0xFF501050, opacity, 1.35F);
            drawGlow(client, witherPulse * 0.5F, 0xFF200820, opacity, 1.12F);
        } else if (poisoned) {
            float poisonPulse = 0.14F + 0.08F * (0.5F + 0.5F * (float) Math.sin(t * 1.1F));
            drawGlow(client, poisonPulse, 0xFF5FD43B, opacity, 1.30F);
            drawGlow(client, poisonPulse * 0.5F, 0xFF2C6A18, opacity, 1.10F);
        } else if (critical) {
            float critPulse = 0.18F + 0.18F * (0.5F + 0.5F * (float) Math.sin(t * 4.2F));
            drawGlow(client, critPulse * 1.2F, 0xFFFF3232, opacity, 1.10F);
            drawGlow(client, critPulse * 0.75F, 0xFFDC1414, opacity, 1.28F);
        } else if (hungerBlocked) {
            float hungerPulse = 0.03F + 0.06F * (0.5F + 0.5F * (float) Math.sin(t * 0.9F));
            drawGlow(client, hungerPulse * 1.4F, 0xFFDC2828, opacity, 1.15F);
            drawGlow(client, hungerPulse * 0.9F, 0xFFB41414, opacity, 1.35F);
        } else if (saturationBonus && flashAlpha <= 0.0F) {
            float goldPulse = 0.05F + 0.12F * (0.5F + 0.5F * (float) Math.sin(t * 2.0F));
            drawGlow(client, goldPulse, 0xFFFFC24A, opacity, 1.18F);
            drawGlow(client, goldPulse * 0.5F, 0xFFB88300, opacity, 1.32F);
        } else if (flashAlpha <= 0.0F && !glowSuppressed && state.isRegenActive()) {
            float glowT = state.getGlowPhaseSeconds();
            float pulse;
            if (config.hudRichAnimations) {
                float raw = (float) Math.sin(glowT * 2.2F) * 0.60F
                          + (float) Math.sin(glowT * 3.7F) * 0.30F
                          + (float) Math.sin(glowT * 5.1F) * 0.10F;
                pulse = 0.05F + 0.18F * ((raw + 1.0F) * 0.5F);
            } else {
                float phase = glowT * 2.2F - (float) (Math.PI / 2);
                pulse = 0.05F + 0.18F * (0.5F + 0.5F * (float) Math.sin(phase));
            }
            drawGlow(client, pulse, config.getHudArgb(), opacity);
        }

        if (flashAlpha > 0.0F) {
            drawHealFlash(client, flashAlpha, opacity);
        }

        drawSparkle(state, opacity);

        if (config.showTimer && !timerText.isEmpty()) {
            int timerArgb = ((int) (opacity * 255) << 24) | 0xFFFFFF;
            drawTimer(client.fontRenderer, timerText, 20, 4, timerArgb);
        }

        GlStateManager.popMatrix();
    }

    private void drawHeart(Minecraft client, float progress, int tint, float opacity, float fillOpacity) {
        client.getTextureManager().bindTexture(HEART_TEXTURE);
        int fillHeight = MathHelper.ceil(MathHelper.clamp(progress, 0.0F, 1.0F) * 16.0F);
        if (fillHeight > 0) {
            float a = (((tint >>> 24) & 0xFF) / 255.0F) * fillOpacity;
            float r = ((tint >>> 16) & 0xFF) / 255.0F;
            float g = ((tint >>> 8) & 0xFF) / 255.0F;
            float b = (tint & 0xFF) / 255.0F;
            GlStateManager.color(r, g, b, a);
            int drawY = 16 - fillHeight;
            drawModalRectWithCustomSizedTexture(0, drawY, 0.0F, 32.0F - fillHeight, 16, fillHeight, TEX_W, TEX_H);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, opacity * 0.78F);
        drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 0.0F, 16, 16, TEX_W, TEX_H);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawGlow(Minecraft client, float alpha, int tint, float opacity) {
        drawGlow(client, alpha, tint, opacity, 1.25F);
    }

    private void drawGlow(Minecraft client, float alpha, int tint, float opacity, float scale) {
        client.getTextureManager().bindTexture(HEART_TEXTURE);
        float a = (((tint >>> 24) & 0xFF) / 255.0F) * opacity;
        float r = ((tint >>> 16) & 0xFF) / 255.0F;
        float g = ((tint >>> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        GlStateManager.color(r, g, b, alpha * a);
        GlStateManager.pushMatrix();
        GlStateManager.translate(8.0F, 8.0F, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        GlStateManager.translate(-8.0F, -8.0F, 0.0F);
        drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 16.0F, 16, 16, TEX_W, TEX_H);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawHealFlash(Minecraft client, float alpha, float opacity) {
        client.getTextureManager().bindTexture(HEART_TEXTURE);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha * opacity);
        GlStateManager.pushMatrix();
        GlStateManager.translate(8.0F, 8.0F, 0.0F);
        GlStateManager.scale(1.2F, 1.2F, 1.0F);
        GlStateManager.translate(-8.0F, -8.0F, 0.0F);
        drawModalRectWithCustomSizedTexture(0, 0, 0.0F, 32.0F, 16, 16, TEX_W, TEX_H);
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void drawSparkle(RegenHudState state, float opacity) {
        float alpha = state.getSparkleAlpha() * opacity;
        if (alpha <= 0.0F) return;
        float progress = state.getSparkleProgress();
        float ease   = 1.0F - (1.0F - progress) * (1.0F - progress);
        float innerR = 2.2F + ease * 2.2F;
        float outerR = innerR + 0.8F + ease * 0.5F;

        int lineAlpha = (int) (alpha * 0.9F * 255);
        int dotAlpha  = (int) (alpha * 255);

        for (int i = 0; i < 8; i++) {
            float angle = (i / 8.0F) * (float) (Math.PI * 2) + (float) (Math.PI * 0.125);
            GlStateManager.pushMatrix();
            GlStateManager.translate(8.0F, 8.0F, 0.0F);
            GlStateManager.rotate((float) Math.toDegrees(angle), 0.0F, 0.0F, 1.0F);
            GlStateManager.translate(-0.5F, 0.0F, 0.0F);

            Gui.drawRect(0, (int) innerR, 1, (int) Math.ceil(outerR),
                    (lineAlpha << 24) | (0xFF << 16) | (0xF0 << 8) | 0xC8);

            Gui.drawRect(0, (int) Math.ceil(outerR), 1, (int) Math.ceil(outerR) + 1,
                    (dotAlpha << 24) | 0xFFFFFF);
            GlStateManager.popMatrix();
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawTimer(FontRenderer font, String text, int x, int y, int argb) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(TIMER_TEXT_SCALE, TIMER_TEXT_SCALE, 1.0F);
        font.drawStringWithShadow(text, 0.0F, 0.0F, argb);
        GlStateManager.popMatrix();
    }

    private static boolean shouldRender(RegenHudConfig config, RegenHudState state) {
        if (config == null || !config.showRegenHud) {
            return false;
        }

        boolean fullHealth = state.getMaxHealth() > 0.0F && state.getCurrentHealth() >= state.getMaxHealth();
        if (config.hideAtFullHealth && fullHealth && !state.isRegenActive() && state.isReady() && !state.isPoisoned() && !state.isWithered()) {
            return false;
        }

        String condition = config.showCondition == null ? "injured" : config.showCondition.trim().toLowerCase(Locale.ROOT);
        switch (condition) {
            case "always":
                return true;
            case "out_of_combat":
                return state.isCooldownCounting() || state.isRegenActive() || state.isHungerBlocked() || state.isPoisoned() || state.isWithered() || state.getCurrentHealth() < state.getMaxHealth();
            case "injured":
            default:
                return state.getCurrentHealth() < state.getMaxHealth() || state.isRegenActive() || state.isHungerBlocked() || state.isPoisoned() || state.isWithered();
        }
    }

    private static String getTimerText(RegenHudState state) {
        if (state.isRegenActive() || state.isReady()) {
            return "";
        }
        return String.format(Locale.ROOT, "%.1fs", state.getSecondsRemaining());
    }
}
