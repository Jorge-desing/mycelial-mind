package com.alex.mycelialmind.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Shared UI drawing helpers across Mycelial Mind client screens.
 */
public final class MycelialScreenDrawing {
    private MycelialScreenDrawing() {
    }

    public static void drawScaledText(
            GuiGraphics graphics, Font font, Component text, int x, int y, int maxWidth, int color, boolean shadow) {
        int textW = font.width(text);
        if (textW > maxWidth && textW > 0) {
            float scale = (float) maxWidth / (float) textW;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, text, 0, 0, color, shadow);
            graphics.pose().popPose();
        } else {
            graphics.drawString(font, text, x, y, color, shadow);
        }
    }

    public static void drawScaledText(
            GuiGraphics graphics,
            Font font,
            Component text,
            int x,
            int y,
            int maxWidth,
            int color,
            boolean shadow,
            float minScale,
            int minWidthGuard,
            float verticalCenterFactor) {
        int w = font.width(text);
        if (w <= maxWidth || maxWidth <= minWidthGuard) {
            graphics.drawString(font, text, x, y, color, shadow);
        } else {
            float scale = Math.max(minScale, (float) maxWidth / (float) w);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y + (1.0F - scale) * verticalCenterFactor, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, text, 0, 0, color, shadow);
            graphics.pose().popPose();
        }
    }
}
