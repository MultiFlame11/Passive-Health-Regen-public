package io.github.miche.passiveregen.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.miche.passiveregen.PassiveRegenMod;
import io.github.miche.passiveregen.client.PassiveRegenClientMod;
import io.github.miche.passiveregen.client.RegenHudState;
import io.github.miche.passiveregen.config.RegenHudConfig;
import java.util.Locale;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public final class RegenHudRenderer implements HudRenderCallback {
    private static final ResourceLocation HEART_TEXTURE = ResourceLocation.fromNamespaceAndPath(PassiveRegenMod.MODID, "textures/gui/regen_heart.png");
    private static final int TEX_W = 16;
    private static final int TEX_H = 48;

    @Override
    public void onHudRender(GuiGraphics guiGraphics, DeltaTracker tickCounter) {
        float tickDelta = tickCounter.getGameTimeDeltaTicks();
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }

        RegenHudConfig config = PassiveRegenClientMod.CONFIG;
        RegenHudState state = RegenHudState.get();
        if (!shouldRender(config, state)) {
            return;
        }

        float scale = (float) config.hudScale;
        String timerText = getTimerText(state);
        int timerWidth = config.showTimer ? client.font.width(timerText) : 0;
        int[] pos = HudPositionPreset.calculate(config, guiGraphics.guiWidth(), guiGraphics.guiHeight(), config.showTimer, timerWidth, scale);
        int x = pos[0];
        int y = pos[1];

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(scale, scale, 1.0F);

        float progress = state.isRegenActive() ? 1.0F : state.getCooldownProgress();
        int tint = config.getHudArgb();

        drawHeart(guiGraphics, progress, tint);

        if (state.isRegenActive()) {
            float time = client.level == null ? tickDelta : client.level.getGameTime() + tickDelta;
            float pulse = 0.78F + 0.22F * (float) Math.sin(time * 0.35F);
            drawGlow(guiGraphics, pulse);
        }

        if (config.showTimer && !timerText.isEmpty()) {
            drawTimer(guiGraphics, client.font, timerText, 20, 4);
        }

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

        String condition = config.showCondition == null ? "injured" : config.showCondition.trim().toLowerCase();
        return switch (condition) {
            case "always" -> true;
            case "out_of_combat" -> state.isCooldownCounting() || state.isRegenActive() || state.getCurrentHealth() < state.getMaxHealth();
            default -> state.getCurrentHealth() < state.getMaxHealth() || state.isRegenActive();
        };
    }

    private static void drawHeart(GuiGraphics guiGraphics, float progress, int tint) {
        // Fill drawn first so outline renders on top of it.
        // The outline row has a transparent interior, so the fill shows through
        // while the dark border pixels sit on top  -- giving a natural black border over the fill.
        int fillHeight = Mth.ceil(16.0F * Mth.clamp(progress, 0.0F, 1.0F));
        if (fillHeight > 0) {
            float a = ((tint >>> 24) & 0xFF) / 255.0F;
            float r = ((tint >>> 16) & 0xFF) / 255.0F;
            float g = ((tint >>> 8) & 0xFF) / 255.0F;
            float b = (tint & 0xFF) / 255.0F;
            RenderSystem.setShaderColor(r, g, b, a);
            int drawY = 16 - fillHeight;
            guiGraphics.blit(HEART_TEXTURE, 0, drawY, 0, 32 - fillHeight, 16, fillHeight, TEX_W, TEX_H);
        }
        // Outline drawn last  -- border appears over the fill.
        // Reset color first to guard against stale vanilla shader state (white flash bug fix).
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.blit(HEART_TEXTURE, 0, 0, 0, 0, 16, 16, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void drawGlow(GuiGraphics guiGraphics, float alpha) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
        guiGraphics.blit(HEART_TEXTURE, 0, 0, 0, 32, 16, 16, TEX_W, TEX_H);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static final float TIMER_TEXT_SCALE = 0.75F;

    private static void drawTimer(GuiGraphics guiGraphics, Font font, String text, int x, int y) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(x, y, 0.0F);
        guiGraphics.pose().scale(TIMER_TEXT_SCALE, TIMER_TEXT_SCALE, 1.0F);
        guiGraphics.drawString(font, text, 0, 0, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    private static String getTimerText(RegenHudState state) {
        if (state.isRegenActive() || state.isReady()) {
            return "";
        }
        return String.format(Locale.ROOT, "%.1fs", state.getSecondsRemaining());
    }
}
