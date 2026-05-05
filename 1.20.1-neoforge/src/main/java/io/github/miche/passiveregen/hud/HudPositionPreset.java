package io.github.miche.passiveregen.hud;

import io.github.miche.passiveregen.config.RegenHudConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class HudPositionPreset {
    private static final int HEART_SIZE = 16;
    private static final int TIMER_GAP = 4;
    private static final int DEFAULT_MARGIN = 2;

    private HudPositionPreset() {
    }

    public static int[] calculate(RegenHudConfig config, int screenWidth, int screenHeight, boolean showTimer, int timerWidth, float scale) {
        int elementWidth = Math.max(HEART_SIZE, HEART_SIZE + (showTimer ? TIMER_GAP + timerWidth : 0));
        int elementHeight = HEART_SIZE;
        int scaledWidth = Math.max(1, Math.round(elementWidth * scale));
        int scaledHeight = Math.max(1, Math.round(elementHeight * scale));

        String preset = config.hudPosition == null ? "RIGHT_OF_HEALTH" : config.hudPosition.trim().toUpperCase();
        int x;
        int y;

        switch (preset) {
            case "LEFT_OF_HEALTH": {
                x = screenWidth / 2 - 91 - scaledWidth - 6;
                y = screenHeight - 39;
                break;
            }
            case "ABOVE_HUNGER": {
                x = screenWidth / 2 + 91 - scaledWidth;
                y = screenHeight - 49;
                break;
            }
            case "BELOW_HUNGER": {
                x = screenWidth / 2 + 91 - scaledWidth;
                y = screenHeight - 29;
                break;
            }
            case "TOP_LEFT": {
                x = DEFAULT_MARGIN;
                y = DEFAULT_MARGIN;
                break;
            }
            case "TOP_RIGHT": {
                x = screenWidth - scaledWidth - DEFAULT_MARGIN;
                y = DEFAULT_MARGIN;
                break;
            }
            case "BOTTOM_LEFT": {
                x = DEFAULT_MARGIN;
                y = screenHeight - scaledHeight - DEFAULT_MARGIN;
                break;
            }
            case "BOTTOM_RIGHT": {
                x = screenWidth - scaledWidth - DEFAULT_MARGIN;
                y = screenHeight - scaledHeight - DEFAULT_MARGIN;
                break;
            }
            case "CUSTOM": {
                int[] custom = applyAnchor(screenWidth, screenHeight, scaledWidth, scaledHeight, config.hudCustomAnchor);
                x = custom[0];
                y = custom[1];
                break;
            }
            case "RIGHT_OF_HEALTH":
            default: {
                x = screenWidth / 2 + 91 + 6;
                y = screenHeight - 39;
                break;
            }
        }

        x += config.hudOffsetX;
        y += config.hudOffsetY;
        return new int[] {x, y};
    }

    private static int[] applyAnchor(int screenWidth, int screenHeight, int width, int height, String anchorName) {
        String anchor = anchorName == null ? "TOP_LEFT" : anchorName.trim().toUpperCase();
        int x;
        int y;

        switch (anchor) {
            case "CENTER": {
                x = (screenWidth - width) / 2;
                y = (screenHeight - height) / 2;
                break;
            }
            case "TOP": {
                x = (screenWidth - width) / 2;
                y = DEFAULT_MARGIN;
                break;
            }
            case "BOTTOM": {
                x = (screenWidth - width) / 2;
                y = screenHeight - height - DEFAULT_MARGIN;
                break;
            }
            case "LEFT": {
                x = DEFAULT_MARGIN;
                y = (screenHeight - height) / 2;
                break;
            }
            case "RIGHT": {
                x = screenWidth - width - DEFAULT_MARGIN;
                y = (screenHeight - height) / 2;
                break;
            }
            case "TOP_RIGHT": {
                x = screenWidth - width - DEFAULT_MARGIN;
                y = DEFAULT_MARGIN;
                break;
            }
            case "BOTTOM_LEFT": {
                x = DEFAULT_MARGIN;
                y = screenHeight - height - DEFAULT_MARGIN;
                break;
            }
            case "BOTTOM_RIGHT": {
                x = screenWidth - width - DEFAULT_MARGIN;
                y = screenHeight - height - DEFAULT_MARGIN;
                break;
            }
            case "TOP_LEFT":
            default: {
                x = DEFAULT_MARGIN;
                y = DEFAULT_MARGIN;
                break;
            }
        }

        return new int[] {x, y};
    }
}
