package io.github.miche.passiveregen.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import io.github.miche.passiveregen.PassiveRegenMod;
import io.github.miche.passiveregen.client.PassiveRegenClientMod;
import io.github.miche.passiveregen.client.RegenHudState;
import io.github.miche.passiveregen.config.RegenHudConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public final class RegenHudRenderer implements HudRenderCallback {
    private static final ResourceLocation HEART_TEXTURE = new ResourceLocation(PassiveRegenMod.MODID, "textures/gui/regen_heart.png");
    private static final int TEX_W = 16;
    private static final int TEX_H = 64;
    private static final float TIMER_TEXT_SCALE = 0.75F;

    private static final List<Ember> embers = new ArrayList<>();
    private static long lastEmberSpawnMs = 0L;

    private static final List<Snow> snow = new ArrayList<>();
    private static long lastSnowSpawnMs = 0L;

    private static final List<GoldMote> goldMotes = new ArrayList<>();
    private static long lastGoldMoteSpawnMs = 0L;

    private static final List<PoisonBubble> poisonBubbles = new ArrayList<>();
    private static long lastPoisonBubbleSpawnMs = 0L;

    private static final List<WitherDebris> witherDebris = new ArrayList<>();
    private static long lastWitherDebrisSpawnMs = 0L;

    private static final class PoisonBubble {
        float x;
        float y;
        float vy;
        float wobPhase;
        int size;
        long born;
        int lifeMs;

        PoisonBubble(float x, float y, float vy, float wobPhase, int size, long born, int lifeMs) {
            this.x = x;
            this.y = y;
            this.vy = vy;
            this.wobPhase = wobPhase;
            this.size = size;
            this.born = born;
            this.lifeMs = lifeMs;
        }
    }

    private static final class PoisonDrip {
        float x;
        float y;
        float vy;
        int streak;
        long born;
        int lifeMs;

        PoisonDrip(float x, float y, float vy, int streak, long born, int lifeMs) {
            this.x = x;
            this.y = y;
            this.vy = vy;
            this.streak = streak;
            this.born = born;
            this.lifeMs = lifeMs;
        }
    }

    private static final List<PoisonDrip> poisonDrips = new ArrayList<>();
    private static long lastPoisonDripSpawnMs = 0L;

    private static final class WitherDebris {
        float x;
        float y;
        float vx;
        float vy;
        long born;
        int lifeMs;

        WitherDebris(float x, float y, float vx, float vy, long born, int lifeMs) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.born = born;
            this.lifeMs = lifeMs;
        }
    }

    private static final class GoldMote {
        float x;
        float y;
        float vx;
        float vy;
        float wobPhase;
        long born;
        int lifeMs;

        GoldMote(float x, float y, float vx, float vy, float wobPhase, long born, int lifeMs) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.wobPhase = wobPhase;
            this.born = born;
            this.lifeMs = lifeMs;
        }
    }

    private static final class Snow {
        float x;
        float y;
        float vx;
        float vy;
        long born;
        int lifeMs;

        Snow(float x, float y, float vx, float vy, long born, int lifeMs) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.born = born;
            this.lifeMs = lifeMs;
        }
    }

    private static final class Ember {
        float x;
        float y;
        float vx;
        float vy;
        float wobPhase;
        float size;
        long born;
        int lifeMs;

        Ember(float x, float y, float vx, float vy, float wobPhase, float size, long born, int lifeMs) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.wobPhase = wobPhase;
            this.size = size;
            this.born = born;
            this.lifeMs = lifeMs;
        }
    }

    @Override
    public void onHudRender(GuiGraphics guiGraphics, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }

        RegenHudConfig config = PassiveRegenClientMod.CONFIG;
        RegenHudState state = RegenHudState.get();
        boolean logicallyVisible = shouldRender(config, state);
        float fadeAlpha = config.hudFadeEnabled
            ? state.updateAndGetFadeAlpha(logicallyVisible, config.hudFadeInMs, config.hudFadeOutMs)
            : (logicallyVisible ? 1.0F : 0.0F);
        if (fadeAlpha <= 0.0F) {
            return;
        }

        float scale = (float) config.hudScale;
        float opacity = (float) config.hudOpacity * fadeAlpha;
        float fillOpacity = (float) config.hudOpacity * (logicallyVisible ? Math.max(fadeAlpha, 0.35F) : fadeAlpha);
        String timerText = getTimerText(state);
        int timerWidth = config.showTimer ? client.font.width(timerText) : 0;
        int[] pos = HudPositionPreset.calculate(config, guiGraphics.guiWidth(), guiGraphics.guiHeight(), config.showTimer, timerWidth, scale);

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

        float progress;
        if (withered || poisoned) {
            progress = 1.0F;
        } else {
            progress = state.isRegenActive() ? 1.0F : state.getCooldownProgress();
        }

        int tint;
        if (withered) {
            tint = 0xFF1A121A;
        } else if (poisoned) {
            tint = 0xFF4E9A2A;
        } else if (freezing && config.hudFreezingTintEnabled) {
            tint = config.getHudFreezingArgb();
        } else if (hungerBlocked) {
            tint = config.getHudBlockedArgb();
        } else if (campfireActive) {
            tint = 0xFFFF8C2A;
        } else {
            tint = config.getHudArgb();
        }
        float thumpScale = config.hudRichAnimations ? state.getHealThumpScale() : 1.0F;
        float flashAlpha = state.getHealFlashAlpha();
        boolean glowSuppressed = state.isGlowSuppressed();
        float t = (System.currentTimeMillis() % 3_600_000L) / 1000.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x(pos), y(pos), 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);

        if (withered) {

            float shakeX = (float) (Math.sin(t * 9.0) * 0.35 + Math.sin(t * 14.3) * 0.2);
            float shakeY = (float) (Math.sin(t * 11.0) * 0.3);
            guiGraphics.pose().translate(shakeX, shakeY, 0.0F);
        } else if (poisoned) {

            float swayX = (float) (Math.sin(t * 1.8) * 0.5);
            float swayY = (float) (Math.sin(t * 1.3 + 0.7) * 0.35);
            guiGraphics.pose().translate(swayX, swayY, 0.0F);
        } else if (freezingDamaging && config.hudFreezingShakeEnabled) {
            float speed = (float) (config.hudFreezingShakeSpeedHz * Math.PI * 2.0);
            float amp = (float) config.hudFreezingShakeIntensity;
            float shakeX = (float) (Math.sin(t * speed) * amp + Math.sin(t * speed * 1.37F) * amp * 0.5F);
            float shakeY = (float) (Math.cos(t * speed * 0.91F) * amp * 0.6F);
            guiGraphics.pose().translate(shakeX, shakeY, 0.0F);
        } else if (critical) {
            float shakeX = (float) (Math.sin(t * 25.0) * 0.6 + Math.sin(t * 41.0) * 0.3);
            float shakeY = (float) (Math.sin(t * 29.0) * 0.5 + Math.sin(t * 37.0) * 0.2);
            guiGraphics.pose().translate(shakeX, shakeY, 0.0F);
        }

        if (withered) {
            float decayPulse = 0.18F + 0.10F * (0.5F + 0.5F * (float) Math.sin(t * 1.6F));
            drawGlow(guiGraphics, decayPulse, 0xFF501050, opacity, 1.40F);
            drawGlow(guiGraphics, decayPulse * 0.5F, 0xFF200820, opacity, 1.15F);
        } else if (poisoned) {

            float sickPulse = 0.14F + 0.08F * (0.5F + 0.5F * (float) Math.sin(t * 1.1F));
            drawGlow(guiGraphics, sickPulse, 0xFF5FD43B, opacity, 1.30F);
        } else if (freezing) {

            float frostPulse = 0.16F + 0.10F * (0.5F + 0.5F * (float) Math.sin(t * 1.3F));
            if (freezingDamaging) frostPulse *= 1.35F;
            drawGlow(guiGraphics, frostPulse, 0xFF6FB8E8, opacity, 1.35F);
            drawGlow(guiGraphics, frostPulse * 0.55F, 0xFF2A7AB4, opacity, 1.15F);
        }

        if (campfireActive) {
            float underA = glowSuppressed ? 0.5F : 1.0F;
            drawCampfireGlow(guiGraphics, campfireFlicker(t) * underA, opacity);
        }

        if (hungerBlocked) {
            long cyclePosMs = System.currentTimeMillis() % 3500L;
            float phase = 0.0F;
            if (cyclePosMs < 250L) {
                float p = cyclePosMs / 250.0F;
                phase = (float) Math.sin(p * Math.PI * 6.0);
            }
            float shakeX  = phase * 0.6F;
            float tiltRad = phase * 0.0873F;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(shakeX, 0.0F, 0.0F);
            guiGraphics.pose().translate(8.0F, 8.0F, 0.0F);
            guiGraphics.pose().mulPose(Axis.ZP.rotation(tiltRad));
            guiGraphics.pose().translate(-8.0F, -8.0F, 0.0F);
        }

        if (thumpScale != 1.0F) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(8.0F, 8.0F, 0.0F);
            guiGraphics.pose().scale(thumpScale, thumpScale, 1.0F);
            guiGraphics.pose().translate(-8.0F, -8.0F, 0.0F);
        }
        drawHeart(guiGraphics, progress, tint, opacity, fillOpacity);
        if (thumpScale != 1.0F) {
            guiGraphics.pose().popPose();
        }

        if (freezing && config.hudFreezingFrostEnabled) {
            drawFrostIcing(guiGraphics, opacity, t);
        }

        if (withered) {
            drawWitherCracks(guiGraphics, opacity, t);
        }

        if (saturationBonus && flashAlpha <= 0.0F && thumpScale <= 1.001F) {
            drawSaturationSheen(guiGraphics, opacity, t, campfireActive);
        }

        if (critical) {
            float critPulse = 0.18F + 0.18F * (0.5F + 0.5F * (float) Math.sin(t * 4.2F));
            drawGlow(guiGraphics, critPulse * 1.2F, 0xFFFF3232, opacity, 1.10F);
            drawGlow(guiGraphics, critPulse * 0.75F, 0xFFDC1414, opacity, 1.28F);
        } else if (hungerBlocked) {
            float hungerPulse = 0.10F + 0.16F * (0.5F + 0.5F * (float) Math.sin(t * 0.9F));
            drawGlow(guiGraphics, hungerPulse * 1.5F, 0xFFDC2828, opacity, 1.15F);
            drawGlow(guiGraphics, hungerPulse * 1.0F, 0xFFB41414, opacity, 1.35F);
        } else if (freezing) {
            drawFreezingHighlight(guiGraphics, config.getHudFreezingArgb(), opacity, t);
        } else if (withered || poisoned) {

        } else if (flashAlpha <= 0.0F && !glowSuppressed && state.isRegenActive() && !saturationBonus && !state.isSaturationBonus()) {
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
            drawGlow(guiGraphics, pulse, tint, opacity);
        }

        if (flashAlpha > 0.0F) {
            drawHealFlash(guiGraphics, flashAlpha, campfireActive, critical, freezing, saturationBonus, opacity);
        }

        if (campfireActive) {
            float campfireA = campfireFlicker(t);
            float glowMult = glowSuppressed ? 0.5F : 1.0F;
            drawGlow(guiGraphics, campfireA * 0.55F * glowMult, 0xFFDC6400, opacity, 1.10F);
            drawCampfireRays(guiGraphics, campfireA, opacity, t);
        }

        if (hungerBlocked) {
            guiGraphics.pose().popPose();
        }

        if (campfireActive) {
            updateAndDrawEmbers(guiGraphics, glowSuppressed ? 0.0F : campfireFlicker(t), opacity);
        }

        if (freezing && config.hudFreezingSnowEnabled && config.hudFreezingSnowCount > 0) {
            updateAndDrawSnow(guiGraphics, config.hudFreezingSnowCount, opacity);
        } else if (!freezing && !snow.isEmpty()) {
            snow.clear();
        }

        if (saturationBonus && config.hudSaturationSparkleEnabled) {
            updateAndDrawGoldMotes(guiGraphics, opacity);
        } else if (!goldMotes.isEmpty()) {
            goldMotes.clear();
        }

        if (poisoned) {
            updateAndDrawPoisonBubbles(guiGraphics, opacity);
        } else {
            if (!poisonBubbles.isEmpty()) poisonBubbles.clear();
            if (!poisonDrips.isEmpty()) poisonDrips.clear();
        }

        if (withered) {
            updateAndDrawWitherDebris(guiGraphics, opacity);
        } else if (!witherDebris.isEmpty()) {
            witherDebris.clear();
        }

        drawSparkle(guiGraphics, state, opacity);

        if (config.showTimer && !timerText.isEmpty()) {
            int timerArgb = ((int) (opacity * 255) << 24) | 0xFFFFFF;
            drawTimer(guiGraphics, client.font, timerText, 20, 4, timerArgb);
        }

        guiGraphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float campfireFlicker(float t) {
        float raw = (float) Math.sin(t * 2.5F) * 0.50F
            + (float) Math.sin(t * 3.7F) * 0.30F
            + (float) Math.sin(t * 5.1F) * 0.20F;
        return 0.06F + 0.20F * ((raw + 1.0F) * 0.5F);
    }

    private static void drawCampfireGlow(GuiGraphics guiGraphics, float flickerA, float opacity) {
        drawGlow(guiGraphics, flickerA * 0.35F, 0xFFA02800, opacity, 1.55F);
        drawGlow(guiGraphics, flickerA * 0.55F, 0xFFDC6400, opacity, 1.35F);
        drawGlow(guiGraphics, flickerA * 0.80F, 0xFFFFAF00, opacity, 1.20F);
        drawGlow(guiGraphics, flickerA * 0.70F, 0xFFFFF078, opacity, 1.07F);
    }

    private static void drawFreezingHighlight(GuiGraphics guiGraphics, int tint, float opacity, float t) {
        float pulse = 0.40F + 0.25F * (0.5F + 0.5F * (float) Math.sin(t * 1.4F));
        float r = ((tint >>> 16) & 0xFF) / 255.0F;
        float g = ((tint >>> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, pulse * opacity);
        guiGraphics.blit(HEART_TEXTURE, 0, 0, 0, 0, 16, 16, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawGlow(GuiGraphics guiGraphics, float alpha, int tint, float opacity) {
        drawGlow(guiGraphics, alpha, tint, opacity, 1.25F);
    }

    private static void drawGlow(GuiGraphics guiGraphics, float alpha, int tint, float opacity, float scale) {
        float a = (((tint >>> 24) & 0xFF) / 255.0F) * opacity;
        float r = ((tint >>> 16) & 0xFF) / 255.0F;
        float g = ((tint >>> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        RenderSystem.setShaderColor(r, g, b, alpha * a);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(8.0F, 8.0F, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);
        guiGraphics.pose().translate(-8.0F, -8.0F, 0.0F);
        guiGraphics.blit(HEART_TEXTURE, 0, 0, 0, 16, 16, 16, TEX_W, TEX_H);
        guiGraphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawHeart(GuiGraphics guiGraphics, float progress, int tint, float opacity, float fillOpacity) {
        int fillHeight = Mth.ceil(16.0F * Mth.clamp(progress, 0.0F, 1.0F));
        if (fillHeight > 0) {
            float a = (((tint >>> 24) & 0xFF) / 255.0F) * fillOpacity;
            float r = ((tint >>> 16) & 0xFF) / 255.0F;
            float g = ((tint >>> 8) & 0xFF) / 255.0F;
            float b = (tint & 0xFF) / 255.0F;
            RenderSystem.setShaderColor(r, g, b, a);
            int drawY = 16 - fillHeight;
            guiGraphics.blit(HEART_TEXTURE, 0, drawY, 0, 32 - fillHeight, 16, fillHeight, TEX_W, TEX_H);
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, opacity * 0.78F);
        guiGraphics.blit(HEART_TEXTURE, 0, 0, 0, 0, 16, 16, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawHealFlash(GuiGraphics guiGraphics, float alpha, boolean campfire, boolean critical, boolean freezing, boolean saturationBonus, float opacity) {
        if (freezing) {
            drawFreezeHealBurst(guiGraphics, alpha, opacity);
            return;
        }
        float r;
        float g;
        float b;
        float flashScale;
        if (critical) {
            r = 1.0F;
            g = 0.188F;
            b = 0.188F;
            flashScale = 1.32F;
        } else if (campfire) {
            r = 1.0F;
            g = 0.85F;
            b = 0.50F;

            flashScale = 1.0F;
        } else if (saturationBonus) {
            r = 1.0F;
            g = 0.82F;
            b = 0.10F;

            flashScale = 1.05F;
        } else {
            r = 1.0F;
            g = 1.0F;
            b = 1.0F;
            flashScale = 1.2F;
        }
        RenderSystem.setShaderColor(r, g, b, alpha * opacity);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(8.0F, 8.0F, 0.0F);
        guiGraphics.pose().scale(flashScale, flashScale, 1.0F);
        guiGraphics.pose().translate(-8.0F, -8.0F, 0.0F);
        guiGraphics.blit(HEART_TEXTURE, 0, 0, 0, 32, 16, 16, TEX_W, TEX_H);
        guiGraphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawFreezeHealBurst(GuiGraphics guiGraphics, float alpha, float opacity) {
        float progress = (float) Math.sqrt(1.0F - alpha);
        float innerR = 3.5F + progress * 2.0F;
        float outerR = innerR + 2.5F + progress * 5.0F;
        float crossPos = innerR + (outerR - innerR) * 0.45F;
        float crossLen = 1.0F + progress * 1.5F;

        float coreAlpha = alpha * 0.95F * opacity;
        float edgeAlpha = alpha * 0.5F * opacity;
        int core = ((int) (coreAlpha * 255) << 24) | 0xD4F0FF;
        int edge = ((int) (edgeAlpha * 255) << 24) | 0x90C8F0;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(8.0F, 5.0F, 0.0F);
        for (int i = 0; i < 6; i++) {
            float angle = (i / 6.0F) * Mth.TWO_PI + Mth.PI / 6.0F;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().mulPose(Axis.ZP.rotation(angle));
            int ir = Math.round(innerR);
            int or = Math.round(outerR);
            int cp = Math.round(crossPos);
            int cl = Math.max(1, Math.round(crossLen));
            guiGraphics.fill(ir, 0, or, 1, core);
            guiGraphics.fill(cp - cl, -1, cp + cl, 0, edge);
            guiGraphics.pose().popPose();
        }
        guiGraphics.pose().popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void updateAndDrawEmbers(GuiGraphics guiGraphics, float flickerA, float opacity) {
        long now = System.currentTimeMillis();

        float baseInterval = 80.0F + (float) (Math.random() * 90.0F);
        if (flickerA > 0.18F) {
            baseInterval *= 0.7F;
        }
        if (now - lastEmberSpawnMs > (long) baseInterval) {
            lastEmberSpawnMs = now;
            float ex = 8.0F + (float) (Math.random() - 0.5D) * 9.0F;
            float ey = 9.5F + (float) Math.random() * 1.5F;
            float vx = (float) (Math.random() - 0.5D) * 2.0F;
            float vy = -(4.8F + (float) Math.random() * 3.7F);
            float wob = (float) (Math.random() * Math.PI * 2.0D);
            float sz = 1.1F + (float) Math.random() * 1.3F;
            int lms = 1300 + (int) (Math.random() * 700.0D);
            embers.add(new Ember(ex, ey, vx, vy, wob, sz, now, lms));
        }

        for (int i = embers.size() - 1; i >= 0; i--) {
            Ember em = embers.get(i);
            long elapsed = now - em.born;
            if (elapsed >= em.lifeMs) {
                embers.remove(i);
                continue;
            }

            float p = elapsed / (float) em.lifeMs;
            float dt = elapsed / 1000.0F;
            float x = em.x + em.vx * dt + (float) Math.sin(elapsed * 0.006F + em.wobPhase) * 1.4F;
            float y = em.y + em.vy * dt;

            float alpha = (p < 0.15F) ? (p / 0.15F) : ((1.0F - p) / 0.85F);
            alpha *= 0.9F * opacity;

            int g = (int) (170 + (1.0F - p) * 55);
            int b = (int) (30 + (1.0F - p) * 40);
            int color = ((int) (alpha * 255) << 24) | (0xFF << 16) | (g << 8) | b;

            int px = Math.round(x);
            int py = Math.round(y);
            int sz = Math.max(1, Math.round(em.size));
            guiGraphics.fill(px, py, px + sz, py + sz, color);
        }
    }

    private static void drawCampfireRays(GuiGraphics guiGraphics, float flickerA, float opacity, float t) {
        int rayCount = 10;
        float baseAngle = t * 0.35F;
        float coreAlpha = Math.min(1.0F, flickerA * 1.8F) * opacity;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(8.0F, 5.0F, 0.0F);
        for (int i = 0; i < rayCount; i++) {
            float angle = baseAngle + (i / (float) rayCount) * Mth.TWO_PI;
            float phase = t * 2.1F + i * 0.73F;
            float lenWobble = (float) Math.sin(phase) * 1.2F + (float) Math.sin(phase * 1.6F) * 0.6F;
            float rayLen = 3.5F + flickerA * 5.5F + lenWobble;
            int startR = 9;
            int endR = startR + Math.max(1, Math.round(rayLen));

            float rayAlpha = coreAlpha * (0.55F + 0.45F * (0.5F + 0.5F * (float) Math.sin(phase * 1.3F)));
            int core = ((int) (rayAlpha * 255) << 24) | 0xFFD070;
            int edge = ((int) (rayAlpha * 0.45F * 255) << 24) | 0xFF8030;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().mulPose(Axis.ZP.rotation(angle));
            guiGraphics.fill(startR, -1, endR, 0, edge);
            guiGraphics.fill(startR, 0, endR, 1, core);
            guiGraphics.fill(startR, 1, endR, 2, edge);
            guiGraphics.pose().popPose();
        }
        guiGraphics.pose().popPose();
    }

    private static void drawFrostIcing(GuiGraphics guiGraphics, float opacity, float t) {
        float baseAlpha = 0.85F * opacity;
        int frostHi = ((int) (baseAlpha * 255) << 24) | 0xEAF6FF;
        int frostLo = ((int) (baseAlpha * 0.65F * 255) << 24) | 0xB8DDF5;

        int[] row1X = {3, 4, 5, 6, 9, 10, 11, 12};
        for (int x : row1X) guiGraphics.fill(x, 1, x + 1, 2, frostHi);
        int[] row2X = {2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13};
        for (int x : row2X) guiGraphics.fill(x, 2, x + 1, 3, frostLo);
        guiGraphics.fill(4, 0, 5, 1, frostLo);
        guiGraphics.fill(11, 0, 12, 1, frostLo);

        int[] dripX = {3, 6, 8, 10, 13};
        float[] dripPhase = {0.0F, 1.3F, 2.7F, 3.9F, 5.4F};
        for (int i = 0; i < dripX.length; i++) {
            float osc = (float) Math.sin(t * 0.6F + dripPhase[i]);
            float depth = 1.5F + osc * 1.5F + (float) Math.sin(t * 1.3F + dripPhase[i]) * 0.5F;
            int dripPx = Math.max(0, Math.round(depth));
            for (int dy = 0; dy < dripPx; dy++) {
                int y = 3 + dy;
                if (y >= 14) break;
                int color = dy == 0 ? frostLo : (((int) (baseAlpha * (0.5F - dy * 0.1F) * 255) << 24) | 0xA0CFEB);
                guiGraphics.fill(dripX[i], y, dripX[i] + 1, y + 1, color);
            }
        }
    }

    private static void updateAndDrawSnow(GuiGraphics guiGraphics, int targetCount, float opacity) {
        long now = System.currentTimeMillis();

        long spawnInterval = Math.max(40L, 260L / Math.max(1, targetCount));
        if (snow.size() < targetCount && now - lastSnowSpawnMs > spawnInterval) {
            lastSnowSpawnMs = now;
            float sx = -1.0F + (float) Math.random() * 18.0F;
            float sy = -2.0F + (float) Math.random() * 3.0F;
            float svx = (float) (Math.random() - 0.5D) * 1.2F;
            float svy = 3.5F + (float) Math.random() * 2.0F;
            int lms = 1400 + (int) (Math.random() * 900.0D);
            snow.add(new Snow(sx, sy, svx, svy, now, lms));
        }

        for (int i = snow.size() - 1; i >= 0; i--) {
            Snow s = snow.get(i);
            long elapsed = now - s.born;
            if (elapsed >= s.lifeMs || s.y > 20.0F) {
                snow.remove(i);
                continue;
            }

            float p = elapsed / (float) s.lifeMs;
            float dt = elapsed / 1000.0F;
            float x = s.x + s.vx * dt + (float) Math.sin(elapsed * 0.004F) * 0.8F;
            float y = s.y + s.vy * dt;

            float alpha = (p < 0.15F) ? (p / 0.15F) : ((1.0F - p) / 0.85F);
            alpha *= 0.95F * opacity;

            int color = ((int) (alpha * 255) << 24) | 0xFFFFFF;
            int px = Math.round(x);
            int py = Math.round(y);
            guiGraphics.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static void drawSparkle(GuiGraphics guiGraphics, RegenHudState state, float opacity) {
        float alpha = state.getSparkleAlpha() * opacity;
        if (alpha <= 0.0F) {
            return;
        }

        float progress = state.getSparkleProgress();
        float ease = 1.0F - (1.0F - progress) * (1.0F - progress);
        float innerR = 2.2F + ease * 2.2F;
        float outerR = innerR + 0.8F + ease * 0.5F;
        int lineColor = ((int) (alpha * 0.9F * 255) << 24) | 0xFFF0C8;
        int dotColor = ((int) (alpha * 255) << 24) | 0xFFFFFF;

        for (int i = 0; i < 8; i++) {
            float angle = (i / 8.0F) * Mth.TWO_PI + Mth.PI * 0.125F;
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(8.0F, 8.0F, 0.0F);
            guiGraphics.pose().mulPose(Axis.ZP.rotation(angle));
            guiGraphics.pose().translate(-0.5F, 0.0F, 0.0F);
            guiGraphics.fill(0, (int) innerR, 1, (int) Math.ceil(outerR), lineColor);
            guiGraphics.fill(0, (int) Math.ceil(outerR), 1, (int) Math.ceil(outerR) + 1, dotColor);
            guiGraphics.pose().popPose();
        }
    }

    private static void updateAndDrawGoldMotes(GuiGraphics guiGraphics, float opacity) {
        long now = System.currentTimeMillis();

        long spawnInterval = 650L + (long) (Math.random() * 550.0D);
        if (goldMotes.size() < 3 && now - lastGoldMoteSpawnMs > spawnInterval) {
            lastGoldMoteSpawnMs = now;
            float mx = 3.0F + (float) Math.random() * 10.0F;
            float my = 10.0F + (float) Math.random() * 3.0F;
            float vx = (float) (Math.random() - 0.5D) * 1.2F;
            float vy = -(1.4F + (float) Math.random() * 1.1F);
            float wob = (float) (Math.random() * Math.PI * 2.0D);
            int lms = 1600 + (int) (Math.random() * 900.0D);
            goldMotes.add(new GoldMote(mx, my, vx, vy, wob, now, lms));
        }

        for (int i = goldMotes.size() - 1; i >= 0; i--) {
            GoldMote m = goldMotes.get(i);
            long elapsed = now - m.born;
            if (elapsed >= m.lifeMs) {
                goldMotes.remove(i);
                continue;
            }

            float p = elapsed / (float) m.lifeMs;
            float dt = elapsed / 1000.0F;
            float x = m.x + m.vx * dt + (float) Math.sin(elapsed * 0.004F + m.wobPhase) * 1.1F;
            float y = m.y + m.vy * dt;

            float envelope = (p < 0.2F) ? (p / 0.2F) : ((1.0F - p) / 0.8F);
            float twinkle = 0.55F + 0.45F * (float) Math.sin(elapsed * 0.011F + m.wobPhase * 2.0F);
            float alpha = envelope * twinkle * 0.85F * opacity;
            if (alpha <= 0.0F) continue;

            int core = ((int) (alpha * 255) << 24) | 0xFFF0A0;
            int edge = ((int) (alpha * 0.35F * 255) << 24) | 0xFFC040;

            int px = Math.round(x);
            int py = Math.round(y);

            guiGraphics.fill(px, py, px + 1, py + 1, core);
            guiGraphics.fill(px - 1, py, px, py + 1, edge);
            guiGraphics.fill(px + 1, py, px + 2, py + 1, edge);
            guiGraphics.fill(px, py - 1, px + 1, py, edge);
            guiGraphics.fill(px, py + 1, px + 1, py + 2, edge);
        }
    }

    private static void drawSaturationSheen(GuiGraphics guiGraphics, float opacity, float t, boolean skipStaticGlow) {

        if (!skipStaticGlow) {
            float glowAlpha = 0.07F + 0.04F * (0.5F + 0.5F * (float) Math.sin(t * 1.1F));
            drawGlow(guiGraphics, glowAlpha, 0xFFFFCC22, opacity, 1.15F);
        }

        float period = 5.0F;
        float phase = (t % period) / period;
        float glintWindow = 0.20F;
        if (phase >= glintWindow) return;
        float p = phase / glintWindow;
        float ease = p < 0.15F ? p / 0.15F : p > 0.85F ? (1.0F - p) / 0.15F : 1.0F;
        if (ease <= 0.001F) return;

        float glintCenterX = p * 18.0F - 1.0F;
        int shineColor = ((int) (ease * 0.28F * opacity * 255) << 24) | 0xFFEE66;
        int edgeColor  = ((int) (ease * 0.11F * opacity * 255) << 24) | 0xFFDD44;

        for (int py = 2; py <= 13; py++) {
            int gx = Math.round(glintCenterX - (py - 2) * 0.6F);
            if (gx >= 1 && gx <= 14) {
                guiGraphics.fill(gx, py, gx + 1, py + 1, shineColor);
            }
            int gx2 = gx + 1;
            if (gx2 >= 1 && gx2 <= 14) {
                guiGraphics.fill(gx2, py, gx2 + 1, py + 1, edgeColor);
            }
        }
    }

    private static void drawTimer(GuiGraphics guiGraphics, Font font, String text, int x, int y, int argb) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(TIMER_TEXT_SCALE, TIMER_TEXT_SCALE, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, argb, true);
        guiGraphics.pose().popPose();
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
        return switch (condition) {
            case "always" -> true;
            case "out_of_combat" -> state.isCooldownCounting() || state.isRegenActive() || state.isHungerBlocked() || state.getCurrentHealth() < state.getMaxHealth();
            default -> state.getCurrentHealth() < state.getMaxHealth() || state.isRegenActive() || state.isHungerBlocked();
        };
    }

    private static String getTimerText(RegenHudState state) {
        if (state.isRegenActive() || state.isReady()) {
            return "";
        }
        return String.format(Locale.ROOT, "%.1fs", state.getSecondsRemaining());
    }

    private static int x(int[] pos) {
        return pos[0];
    }

    private static int y(int[] pos) {
        return pos[1];
    }

    private static void drawWitherCracks(GuiGraphics guiGraphics, float opacity, float t) {

        float baseAlpha = 0.82F * opacity;
        int crackDark = ((int) (baseAlpha * 255) << 24) | 0x0A0008;
        int crackEdge = ((int) (baseAlpha * 0.55F * 255) << 24) | 0x3A1230;

        int jA = Math.round((float) Math.sin(t * 0.7F) * 0.6F);
        int jB = Math.round((float) Math.sin(t * 0.9F + 1.3F) * 0.6F);

        int[][] spineA = {
            {4 + jA, 3}, {5 + jA, 4}, {5 + jA, 5}, {6 + jA, 6},
            {7 + jA, 6}, {8 + jA, 7}, {9 + jA, 8}, {9 + jA, 9},
            {10 + jA, 10}
        };
        for (int[] p : spineA) {
            guiGraphics.fill(p[0], p[1], p[0] + 1, p[1] + 1, crackDark);
            guiGraphics.fill(p[0] + 1, p[1], p[0] + 2, p[1] + 1, crackEdge);
        }

        int[][] spineB = {
            {11 + jB, 3}, {10 + jB, 4}, {11 + jB, 5}, {10 + jB, 6},
            {9 + jB, 7}, {8 + jB, 8}, {7 + jB, 9}, {7 + jB, 10},
            {6 + jB, 11}
        };
        for (int[] p : spineB) {
            guiGraphics.fill(p[0], p[1], p[0] + 1, p[1] + 1, crackDark);
            guiGraphics.fill(p[0] - 1, p[1], p[0], p[1] + 1, crackEdge);
        }

        int chipAlpha = ((int) (baseAlpha * 0.7F * 255) << 24) | 0x000000;
        guiGraphics.fill(3, 5, 4, 6, chipAlpha);
        guiGraphics.fill(12, 5, 13, 6, chipAlpha);
        guiGraphics.fill(5, 11, 6, 12, chipAlpha);
        guiGraphics.fill(11, 10, 12, 11, chipAlpha);
    }

    private static void updateAndDrawPoisonBubbles(GuiGraphics guiGraphics, float opacity) {
        long now = System.currentTimeMillis();
        float t = (now % 3_600_000L) / 1000.0F;

        drawPoisonSplotches(guiGraphics, opacity, t);

        long spawnInterval = 110L + (long) (Math.random() * 180.0D);
        if (poisonBubbles.size() < 8 && now - lastPoisonBubbleSpawnMs > spawnInterval) {
            lastPoisonBubbleSpawnMs = now;
            float bx = 2.5F + (float) Math.random() * 11.0F;
            float by = 10.5F + (float) Math.random() * 3.5F;
            float vy = -(2.2F + (float) Math.random() * 2.4F);
            float wob = (float) (Math.random() * Math.PI * 2.0D);

            int size = Math.random() < 0.30D ? 2 : 1;
            int lms = 1000 + (int) (Math.random() * 800.0D);
            poisonBubbles.add(new PoisonBubble(bx, by, vy, wob, size, now, lms));
        }

        for (int i = poisonBubbles.size() - 1; i >= 0; i--) {
            PoisonBubble bub = poisonBubbles.get(i);
            long elapsed = now - bub.born;
            if (elapsed >= bub.lifeMs) {
                poisonBubbles.remove(i);
                continue;
            }

            float p = elapsed / (float) bub.lifeMs;
            float dt = elapsed / 1000.0F;
            float x = bub.x + (float) Math.sin(elapsed * 0.0055F + bub.wobPhase) * 1.6F;
            float y = bub.y + bub.vy * dt;

            float envelope = (p < 0.12F) ? (p / 0.12F) : ((p > 0.70F) ? ((1.0F - p) / 0.30F) : 1.0F);
            float alpha = envelope * 0.9F * opacity;
            if (alpha <= 0.0F) continue;

            int core = ((int) (alpha * 255) << 24) | 0xB4F050;
            int edge = ((int) (alpha * 0.55F * 255) << 24) | 0x4A8A1A;
            int highlight = ((int) (alpha * 0.75F * 255) << 24) | 0xDEFF9A;

            int px = Math.round(x);
            int py = Math.round(y);

            if (bub.size >= 2) {

                guiGraphics.fill(px, py, px + 2, py + 2, core);
                guiGraphics.fill(px - 1, py, px, py + 2, edge);
                guiGraphics.fill(px + 2, py, px + 3, py + 2, edge);
                guiGraphics.fill(px, py - 1, px + 2, py, edge);
                guiGraphics.fill(px, py + 2, px + 2, py + 3, edge);
                guiGraphics.fill(px, py, px + 1, py + 1, highlight);
            } else {

                guiGraphics.fill(px, py, px + 1, py + 1, core);
                guiGraphics.fill(px - 1, py, px, py + 1, edge);
                guiGraphics.fill(px + 1, py, px + 2, py + 1, edge);
                guiGraphics.fill(px, py + 1, px + 1, py + 2, edge);
                guiGraphics.fill(px, py - 1, px + 1, py, highlight);
            }
        }

        long dripInterval = 260L + (long) (Math.random() * 280.0D);
        if (poisonDrips.size() < 4 && now - lastPoisonDripSpawnMs > dripInterval) {
            lastPoisonDripSpawnMs = now;

            float dx = 5.0F + (float) Math.random() * 6.0F;
            float dy = 12.0F + (float) Math.random() * 1.5F;
            float vy = 2.6F + (float) Math.random() * 1.4F;
            int streak = 2 + (int) (Math.random() * 3.0D);
            int lms = 900 + (int) (Math.random() * 600.0D);
            poisonDrips.add(new PoisonDrip(dx, dy, vy, streak, now, lms));
        }

        for (int i = poisonDrips.size() - 1; i >= 0; i--) {
            PoisonDrip d = poisonDrips.get(i);
            long elapsed = now - d.born;
            if (elapsed >= d.lifeMs || d.y > 22.0F) {
                poisonDrips.remove(i);
                continue;
            }

            float p = elapsed / (float) d.lifeMs;
            float dt = elapsed / 1000.0F;

            float y = d.y + d.vy * dt + 2.5F * dt * dt;

            float envelope = (p < 0.10F) ? (p / 0.10F) : ((1.0F - p) / 0.90F);
            float alpha = envelope * 0.85F * opacity;
            if (alpha <= 0.0F) continue;

            int head = ((int) (alpha * 255) << 24) | 0x8CD836;
            int tail = ((int) (alpha * 0.55F * 255) << 24) | 0x4A8A1A;
            int glint = ((int) (alpha * 0.9F * 255) << 24) | 0xDEFF9A;

            int px = Math.round(d.x);
            int py = Math.round(y);

            guiGraphics.fill(px, py, px + 1, py + 1, head);
            guiGraphics.fill(px, py - 1, px + 1, py, glint);
            for (int s = 1; s < d.streak; s++) {
                guiGraphics.fill(px, py - 1 - s, px + 1, py - s, tail);
            }
        }
    }

    private static void drawPoisonSplotches(GuiGraphics guiGraphics, float opacity, float t) {

        float baseAlpha = 0.55F * opacity;

        float a0 = baseAlpha * (0.45F + 0.55F * (0.5F + 0.5F * (float) Math.sin(t * 2.3F)));
        float a1 = baseAlpha * (0.45F + 0.55F * (0.5F + 0.5F * (float) Math.sin(t * 1.7F + 1.9F)));
        float a2 = baseAlpha * (0.45F + 0.55F * (0.5F + 0.5F * (float) Math.sin(t * 2.9F + 3.4F)));
        float a3 = baseAlpha * (0.45F + 0.55F * (0.5F + 0.5F * (float) Math.sin(t * 2.1F + 0.6F)));

        int c0 = ((int) (a0 * 255) << 24) | 0x79C234;
        int c1 = ((int) (a1 * 255) << 24) | 0x79C234;
        int c2 = ((int) (a2 * 255) << 24) | 0x79C234;
        int c3 = ((int) (a3 * 255) << 24) | 0x79C234;

        int speck = ((int) (baseAlpha * 0.7F * 255) << 24) | 0x2E5A12;

        guiGraphics.fill(3, 4, 5, 5, c0);
        guiGraphics.fill(4, 5, 5, 6, c0);
        guiGraphics.fill(3, 5, 4, 6, speck);

        guiGraphics.fill(10, 3, 12, 4, c1);
        guiGraphics.fill(11, 4, 13, 5, c1);
        guiGraphics.fill(12, 5, 13, 6, speck);

        guiGraphics.fill(4, 8, 6, 9, c2);
        guiGraphics.fill(5, 9, 6, 10, c2);
        guiGraphics.fill(3, 9, 4, 10, speck);

        guiGraphics.fill(9, 9, 11, 10, c3);
        guiGraphics.fill(10, 10, 11, 11, c3);
        guiGraphics.fill(8, 10, 9, 11, speck);

        guiGraphics.fill(7, 6, 8, 7, speck);
        guiGraphics.fill(6, 11, 7, 12, speck);
    }

    private static void updateAndDrawWitherDebris(GuiGraphics guiGraphics, float opacity) {
        long now = System.currentTimeMillis();

        long spawnInterval = 220L + (long) (Math.random() * 200.0D);
        if (witherDebris.size() < 6 && now - lastWitherDebrisSpawnMs > spawnInterval) {
            lastWitherDebrisSpawnMs = now;

            float dx = 2.0F + (float) Math.random() * 12.0F;
            float dy = 6.0F + (float) Math.random() * 8.0F;
            float vx = (float) (Math.random() - 0.5D) * 1.6F;
            float vy = 3.0F + (float) Math.random() * 2.4F;
            int lms = 900 + (int) (Math.random() * 500.0D);
            witherDebris.add(new WitherDebris(dx, dy, vx, vy, now, lms));
        }

        for (int i = witherDebris.size() - 1; i >= 0; i--) {
            WitherDebris d = witherDebris.get(i);
            long elapsed = now - d.born;
            if (elapsed >= d.lifeMs || d.y > 22.0F) {
                witherDebris.remove(i);
                continue;
            }

            float p = elapsed / (float) d.lifeMs;
            float dt = elapsed / 1000.0F;

            float x = d.x + d.vx * dt;
            float y = d.y + d.vy * dt + 3.0F * dt * dt;

            float alpha = (p < 0.10F) ? (p / 0.10F) : ((1.0F - p) / 0.90F);
            alpha *= 0.92F * opacity;
            if (alpha <= 0.0F) continue;

            int core = ((int) (alpha * 255) << 24) | 0x110810;
            int edge = ((int) (alpha * 0.45F * 255) << 24) | 0x3A1234;

            int px = Math.round(x);
            int py = Math.round(y);
            guiGraphics.fill(px, py, px + 1, py + 1, core);

            guiGraphics.fill(px, py - 1, px + 1, py, edge);
        }
    }
}
