package com.alex.mycelialmind.client;

import com.alex.mycelialmind.item.tooltip.MycelialTooltipData.ThreatBarTooltip;
import com.alex.mycelialmind.item.tooltip.MycelialTooltipData.TraitBadgeTooltip;
import com.alex.mycelialmind.network.ClientHiveCache;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;

import java.util.Locale;

/**
 * Client-side visual renderers for custom {@link net.minecraft.world.inventory.tooltip.TooltipComponent}s.
 */
public final class MycelialClientTooltips {
    private MycelialClientTooltips() {
    }

    public static void register(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(ThreatBarTooltip.class, ThreatBarClientComponent::new);
        event.register(TraitBadgeTooltip.class, TraitBadgeClientComponent::new);
    }

    public static ThreatBarTooltip createBioScannerTooltip() {
        return new ThreatBarTooltip(
                (float) ClientHiveCache.getThreatLevel(),
                ClientHiveCache.getSuppressionSecondsRemaining() > 0,
                ClientHiveCache.getActiveTraits().size()
        );
    }

    private record ThreatBarClientComponent(ThreatBarTooltip data) implements ClientTooltipComponent {
        private static final int BAR_WIDTH = 56;

        private Component formattedLabel() {
            String suffix = data.suppressed() ? " [SUP]" : (" (" + data.clampedTraitCount() + ")");
            return Component.literal(String.format(Locale.ROOT, "%.0f%%%s", data.clampedThreat(), suffix));
        }

        @Override
        public int getHeight() {
            return 12;
        }

        @Override
        public int getWidth(Font font) {
            return BAR_WIDTH + 6 + font.width(formattedLabel());
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
            int border = data.suppressed() ? 0xFF4CD6E8 : 0xFF3E5A68;
            int barTop = y + 2;
            guiGraphics.fill(x, barTop, x + BAR_WIDTH, barTop + 7, border);
            guiGraphics.fill(x + 1, barTop + 1, x + BAR_WIDTH - 1, barTop + 6, 0xFF0A141A);

            int fillWidth = Math.round((BAR_WIDTH - 2) * (data.clampedThreat() / 100.0F));
            if (fillWidth > 0) {
                int fillColor = data.suppressed()
                        ? 0xFF4CD6E8
                        : MycelialItemDecorators.threatBarColor(data.clampedThreat());
                guiGraphics.fill(x + 1, barTop + 1, x + 1 + fillWidth, barTop + 6, fillColor);
            }

            int textColor = data.suppressed() ? 0xFF7BE8F5 : 0xFFD9E6EE;
            guiGraphics.drawString(font, formattedLabel(), x + BAR_WIDTH + 4, y + 1, textColor, true);
        }
    }

    private record TraitBadgeClientComponent(TraitBadgeTooltip data) implements ClientTooltipComponent {
        @Override
        public int getHeight() {
            return data.subLabel() != null ? 20 : 11;
        }

        @Override
        public int getWidth(Font font) {
            int mainWidth = 12 + font.width(data.badgeLabel());
            if (data.subLabel() == null) {
                return mainWidth;
            }
            return Math.max(mainWidth, 12 + font.width(data.subLabel()));
        }

        @Override
        public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics) {
            int accent = data.accentRgb() == 0 ? 0xFF6B7B85 : data.accentRgb();
            guiGraphics.fill(x, y + 1, x + 8, y + 9, 0xFF101820);
            guiGraphics.fill(x + 1, y + 2, x + 7, y + 8, accent);
            guiGraphics.drawString(font, data.badgeLabel(), x + 11, y + 1, accent, true);
            if (data.subLabel() != null) {
                guiGraphics.drawString(font, data.subLabel(), x + 11, y + 11, 0xFFAAB8C2, true);
            }
        }
    }
}
