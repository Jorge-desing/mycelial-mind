package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.menu.SynapticBenchMenu;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.util.MycelialText;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;

/**
 * Clean procedural biotechnology GUI for the Synaptic Research Bench.
 */
public class SynapticBenchScreen extends AbstractContainerScreen<SynapticBenchMenu> {
    private static final int BADGE_REL_X = 154;
    private static final int BADGE_REL_Y = 6;
    private static final int BADGE_W = 14;
    private static final int BADGE_H = 12;

    public SynapticBenchScreen(SynapticBenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Main panel
        graphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF140D21);
        graphics.renderOutline(x, y, imageWidth, imageHeight, 0xFF4CC9F0);

        // Formula help badge [?]
        boolean hoverBadge = isHoveringBadge(mouseX, mouseY);
        graphics.fill(x + BADGE_REL_X, y + BADGE_REL_Y, x + BADGE_REL_X + BADGE_W, y + BADGE_REL_Y + BADGE_H,
                hoverBadge ? 0xFF342254 : 0xFF211636);
        graphics.renderOutline(x + BADGE_REL_X, y + BADGE_REL_Y, BADGE_W, BADGE_H,
                hoverBadge ? 0xFF72EFDD : 0xFF8C42B8);

        // Input slots
        drawSlotBox(graphics, x + 33, y + 33, 0xFF8C42B8);
        drawSlotBox(graphics, x + 55, y + 33, 0xFF8C42B8);
        drawSlotBox(graphics, x + 77, y + 33, 0xFF8C42B8);

        // Synthesis arrow
        graphics.drawString(this.font, "=>", x + 103, y + 38, 0xFF72EFDD, true);

        // Output slot
        drawSlotBox(graphics, x + 129, y + 33, 0xFF72EFDD);

        // Player inventory grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                drawSlotBox(graphics, x + 7 + col * 18, y + 83 + row * 18, 0xFF3E2F5B);
            }
        }
        for (int col = 0; col < 9; col++) {
            drawSlotBox(graphics, x + 7 + col * 18, y + 141, 0xFF3E2F5B);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int titleW = this.font.width(this.title);
        if (titleW > 142) {
            float scale = 142.0F / titleW;
            graphics.pose().pushPose();
            graphics.pose().translate(8, 6, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(this.font, this.title, 0, 0, 0xFFE6C2FF, false);
            graphics.pose().popPose();
        } else {
            graphics.drawString(this.font, this.title, 8, 6, 0xFFE6C2FF, false);
        }
        graphics.drawCenteredString(this.font, "?", BADGE_REL_X + BADGE_W / 2, BADGE_REL_Y + 2, 0xFF72EFDD);
        drawScaledLabel(graphics, Component.translatable("gui.mycelialmind.bench.tier", this.menu.getLabTier()),
                8, 22, 18, 0xFFFFD166);
        drawScaledLabel(graphics, Component.translatable("gui.mycelialmind.bench.slots_header"), 28, 22, 86, 0xFF9DE0AD);
        drawScaledLabel(graphics, Component.translatable("gui.mycelialmind.bench.output_header"), 129, 22, 40, 0xFF72EFDD);

        ItemStack out = this.menu.getSlot(SynapticBenchBlockEntity.SLOT_OUTPUT).getItem();
        HiveProgressionTier blockedAct = out.isEmpty()
                ? this.menu.getBlockedAct(ClientDiscoveryCache.getEffectiveActTier())
                : null;
        Component status;
        int statusColor;
        if (!out.isEmpty()) {
            status = Component.translatable("gui.mycelialmind.bench.ready", out.getHoverName());
            statusColor = 0xFF38B000;
        } else if (blockedAct != null) {
            status = Component.translatable("gui.mycelialmind.bench.requires_act",
                    blockedAct.actNumber(), MycelialText.progressionTier(blockedAct));
            statusColor = 0xFFFFB703;
        } else {
            status = Component.translatable("gui.mycelialmind.bench.awaiting");
            statusColor = 0xFF8D99AE;
        }
        int statusW = this.font.width(status);
        if (statusW > 160) {
            float scale = 160.0F / statusW;
            graphics.pose().pushPose();
            graphics.pose().translate(8, 57, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(this.font, status, 0, 0, statusColor, false);
            graphics.pose().popPose();
        } else {
            graphics.drawString(this.font, status, 8, 57, statusColor, false);
        }

        graphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0xFF8D99AE, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (isHoveringBadge(mouseX, mouseY)) {
            List<Component> lines = List.of(
                    Component.translatable("tooltip.mycelialmind.bench.formulas.title").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
                    Component.translatable("tooltip.mycelialmind.bench.formulas.r1").withStyle(ChatFormatting.LIGHT_PURPLE),
                    Component.translatable("tooltip.mycelialmind.bench.formulas.r2").withStyle(ChatFormatting.YELLOW),
                    Component.translatable("tooltip.mycelialmind.bench.formulas.r3").withStyle(ChatFormatting.GREEN),
                    Component.translatable("tooltip.mycelialmind.bench.formulas.r4").withStyle(ChatFormatting.GOLD)
            );
            graphics.renderTooltip(this.font, lines, Optional.empty(), mouseX, mouseY);
        } else {
            this.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    @Override
    protected void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderTooltip(graphics, mouseX, mouseY);
        if (this.hoveredSlot != null && !this.hoveredSlot.hasItem()) {
            String key = switch (this.hoveredSlot.index) {
                case SynapticBenchBlockEntity.SLOT_SAMPLE -> "tooltip.mycelialmind.bench.slot0";
                case SynapticBenchBlockEntity.SLOT_BIOMASS -> "tooltip.mycelialmind.bench.slot1";
                case SynapticBenchBlockEntity.SLOT_CATALYST -> "tooltip.mycelialmind.bench.slot2";
                default -> null;
            };
            if (key != null) {
                graphics.renderTooltip(this.font, Component.translatable(key).withStyle(ChatFormatting.GRAY), mouseX, mouseY);
            }
        }
    }

    private void drawScaledLabel(GuiGraphics graphics, Component label, int x, int y, int maxWidth, int color) {
        int labelW = this.font.width(label);
        if (labelW > maxWidth && labelW > 0) {
            float scale = (float) maxWidth / (float) labelW;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(this.font, label, 0, 0, color, false);
            graphics.pose().popPose();
        } else {
            graphics.drawString(this.font, label, x, y, color, false);
        }
    }

    private boolean isHoveringBadge(int mouseX, int mouseY) {
        int bx = this.leftPos + BADGE_REL_X;
        int by = this.topPos + BADGE_REL_Y;
        return mouseX >= bx && mouseX < bx + BADGE_W && mouseY >= by && mouseY < by + BADGE_H;
    }

    private static void drawSlotBox(GuiGraphics graphics, int x, int y, int borderColor) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF211636);
        graphics.renderOutline(x, y, 18, 18, borderColor);
    }
}
