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
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class RegenHudRenderer implements HudRenderCallback {
    private static final ResourceLocation HEART_TEXTURE = new ResourceLocation(PassiveRegenMod.MODID, "textures/gui/regen_heart.png");
    private static final int TEX_W = 16;
    private static final int TEX_H = 64;
    private static final float TIMER_TEXT_SCALE = 0.75F;

    private static final List<Ember> embers = new ArrayList<>();
    private static long lastEmberSpawnMs = 0L;

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

        boolean hungerBlocked = state.isHungerBlocked();
        boolean campfireActive = state.isNearCampfire() && !hungerBlocked;
        boolean critical = !hungerBlocked && state.isCriticalHealth();

        float progress = state.isRegenActive() ? 1.0F : state.getCooldownProgress();
        int tint = hungerBlocked ? config.getHudBlockedArgb() : config.getHudArgb();
        float thumpScale = config.hudRichAnimations ? state.getHealThumpScale() : 1.0F;
        float flashAlpha = state.getHealFlashAlpha();
        boolean glowSuppressed = state.isGlowSuppressed();
        float t = System.currentTimeMillis() / 1000.0F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x(pos), y(pos), 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);

        if (critical) {
            float shakeX = (float) (Math.sin(t * 25.0) * 0.6 + Math.sin(t * 41.0) * 0.3);
            float shakeY = (float) (Math.sin(t * 29.0) * 0.5 + Math.sin(t * 37.0) * 0.2);
            guiGraphics.pose().translate(shakeX, shakeY, 0.0F);
        }

        if (campfireActive && !glowSuppressed) {
            drawCampfireGlow(guiGraphics, campfireFlicker(t), opacity);
        }

        if (hungerBlocked) {
            // Quick rattle with pause: 3 fast bounces in 250 ms, then 3.25 s still.
            // Modulo in long ms  -- float t (~1.7B s) has no sub-second precision.
            long cyclePosMs = System.currentTimeMillis() % 3500L;
            float phase = 0.0F;
            if (cyclePosMs < 250L) {
                float p = cyclePosMs / 250.0F;
                phase = (float) Math.sin(p * Math.PI * 6.0); // 3 rapid bounces, returns to 0
            }
            float shakeX  = phase * 0.6F;               // ±0.6 px horizontal
            float tiltRad = phase * 0.0873F;             // ±5 degrees in radians
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

        // Glow + flash drawn before hunger popPose so they follow the shake
        if (critical) {
            float critPulse = 0.18F + 0.18F * (0.5F + 0.5F * (float) Math.sin(t * 4.2F));
            drawGlow(guiGraphics, critPulse * 1.2F, 0xFFFF3232, opacity, 1.10F);
            drawGlow(guiGraphics, critPulse * 0.75F, 0xFFDC1414, opacity, 1.28F);
        } else if (hungerBlocked) {
            float hungerPulse = 0.10F + 0.16F * (0.5F + 0.5F * (float) Math.sin(t * 0.9F));
            drawGlow(guiGraphics, hungerPulse * 1.5F, 0xFFDC2828, opacity, 1.15F);
            drawGlow(guiGraphics, hungerPulse * 1.0F, 0xFFB41414, opacity, 1.35F);
        } else if (campfireActive && !glowSuppressed) {
            float campfireA = campfireFlicker(t);
            drawGlow(guiGraphics, campfireA * 0.55F, 0xFFDC6400, opacity, 1.10F);
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
            drawGlow(guiGraphics, pulse, tint, opacity);
        }

        if (flashAlpha > 0.0F) {
            drawHealFlash(guiGraphics, flashAlpha, campfireActive, opacity);
        }

        // End hunger shake context  -- glow and flash were inside it above
        if (hungerBlocked) {
            guiGraphics.pose().popPose();
        }

        if (campfireActive) {
            updateAndDrawEmbers(guiGraphics, glowSuppressed ? 0.0F : campfireFlicker(t), opacity);
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

    private static void drawHealFlash(GuiGraphics guiGraphics, float alpha, boolean campfire, float opacity) {
        float r = 1.0F;
        float g = campfire ? 0.85F : 1.0F;
        float b = campfire ? 0.50F : 1.0F;
        RenderSystem.setShaderColor(r, g, b, alpha * opacity);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(8.0F, 8.0F, 0.0F);
        guiGraphics.pose().scale(1.2F, 1.2F, 1.0F);
        guiGraphics.pose().translate(-8.0F, -8.0F, 0.0F);
        guiGraphics.blit(HEART_TEXTURE, 0, 0, 0, 32, 16, 16, TEX_W, TEX_H);
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
}
