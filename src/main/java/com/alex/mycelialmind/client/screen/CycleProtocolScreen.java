package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.hive.HiveCycleEngine;
import com.alex.mycelialmind.hive.HiveCycleMutator;
import com.alex.mycelialmind.network.ClientCycleCache;
import com.alex.mycelialmind.network.CycleProtocolActionPayload;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Diegetic Post-Matriarch Cycle Protocol selection screen opened at the Matriarch Altar.
 * Lets the player configure 1..3 Mutators, launch or abort a Hive Cycle, and review records.
 */
public class CycleProtocolScreen extends Screen {
    private static final int PANEL_W = 336;
    private static final int PANEL_H = 228;
    private static final int CARD_W = 102;
    private static final int CARD_H = 44;

    private int selectedMask;
    private Button actionButton;

    public CycleProtocolScreen() {
        super(Component.translatable("screen.mycelialmind.cycle_protocol"));
        int initial = ClientCycleCache.getActiveMutatorMask();
        this.selectedMask = initial != 0 ? initial : HiveCycleMutator.HYPER_ADAPTATION.bitMask();
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        this.actionButton = this.addRenderableWidget(Button.builder(
                actionLabel(),
                btn -> onActionPressed()
        ).bounds(left + 12, top + PANEL_H - 26, 176, 18).build());

        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                btn -> onClose()
        ).bounds(left + PANEL_W - 88, top + PANEL_H - 26, 76, 18).build());

        refreshActionButton();
    }

    private Component actionLabel() {
        if (ClientCycleCache.isCycleActive()) {
            return Component.translatable("gui.mycelialmind.cycle.abort", ClientCycleCache.getActiveCycleNumber());
        }
        return Component.translatable("gui.mycelialmind.cycle.start", ClientCycleCache.getActiveCycleNumber());
    }

    private void refreshActionButton() {
        if (this.actionButton == null) {
            return;
        }
        this.actionButton.setMessage(actionLabel());
        if (ClientCycleCache.isCycleActive()) {
            this.actionButton.active = true;
        } else {
            this.actionButton.active = ClientCycleCache.isUnlocked()
                    && HiveCycleEngine.isValidMutatorSelection(this.selectedMask);
        }
    }

    private void onActionPressed() {
        if (ClientCycleCache.isCycleActive()) {
            CycleProtocolActionPayload.sendToServer(CycleProtocolActionPayload.ACTION_ABORT, 0);
            playUiPing(0.85F);
            onClose();
            return;
        }
        if (HiveCycleEngine.isValidMutatorSelection(this.selectedMask)) {
            CycleProtocolActionPayload.sendToServer(CycleProtocolActionPayload.ACTION_START, this.selectedMask);
            playUiPing(1.30F);
            onClose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !ClientCycleCache.isCycleActive()) {
            int left = (this.width - PANEL_W) / 2;
            int top = (this.height - PANEL_H) / 2;
            int gridX = left + 10;
            int gridY = top + 66;
            HiveCycleMutator[] mutators = HiveCycleMutator.values();
            for (int i = 0; i < mutators.length; i++) {
                int col = i % 3;
                int row = i / 3;
                int cx = gridX + col * (CARD_W + 5);
                int cy = gridY + row * (CARD_H + 6);
                if (mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H) {
                    toggleMutator(mutators[i]);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void toggleMutator(HiveCycleMutator mutator) {
        int bit = mutator.bitMask();
        if ((this.selectedMask & bit) != 0) {
            this.selectedMask &= ~bit;
            playUiPing(0.95F);
        } else if (HiveCycleMutator.countActive(this.selectedMask) < HiveCycleMutator.MAX_ACTIVE_MUTATORS) {
            this.selectedMask |= bit;
            playUiPing(1.20F);
        } else {
            playUiPing(0.65F);
        }
        refreshActionButton();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        refreshActionButton();

        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEC130A20);
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, ClientCycleCache.isCycleActive() ? 0xFFFF4D8D : 0xFF9D4EDD);

        graphics.drawCenteredString(this.font, this.title, left + PANEL_W / 2, top + 8, 0xFFFFD166);

        // Top summary bar: Status + Record
        int infoX = left + 10;
        int infoY = top + 24;
        int infoW = PANEL_W - 20;
        graphics.fill(infoX, infoY, infoX + infoW, infoY + 34, 0xAA1D1130);
        graphics.renderOutline(infoX, infoY, infoW, 34, 0xFF5A3E85);

        Component statusLine;
        int statusColor;
        if (ClientCycleCache.isCycleActive()) {
            int mins = ClientCycleCache.getElapsedSeconds() / 60;
            int secs = ClientCycleCache.getElapsedSeconds() % 60;
            statusLine = Component.translatable("gui.mycelialmind.cycle.status_active",
                    ClientCycleCache.getActiveCycleNumber(), mins, secs, ClientCycleCache.getCycleDeaths());
            statusColor = 0xFFFF758F;
        } else {
            statusLine = Component.translatable("gui.mycelialmind.cycle.status_idle",
                    ClientCycleCache.getActiveCycleNumber(),
                    ClientCycleCache.getCompletedCycles());
            statusColor = 0xFF72EFDD;
        }
        graphics.drawString(this.font, statusLine, infoX + 6, infoY + 5, statusColor, true);

        Component recordLine = ClientCycleCache.getBestScore() > 0
                ? Component.translatable("gui.mycelialmind.cycle.record_some",
                        ClientCycleCache.getBestScore(),
                        ClientCycleCache.getBestCycleNumber(),
                        ClientCycleCache.getLastCompletedGrade().isEmpty() ? "-" : ClientCycleCache.getLastCompletedGrade())
                : Component.translatable("gui.mycelialmind.cycle.record_none");
        graphics.drawString(this.font, recordLine, infoX + 6, infoY + 19, 0xFFFFE599, true);

        // 2x3 Mutator card grid
        int gridX = left + 10;
        int gridY = top + 66;
        int displayMask = ClientCycleCache.isCycleActive() ? ClientCycleCache.getActiveMutatorMask() : this.selectedMask;
        HiveCycleMutator[] mutators = HiveCycleMutator.values();
        for (int i = 0; i < mutators.length; i++) {
            HiveCycleMutator mut = mutators[i];
            int col = i % 3;
            int row = i / 3;
            int cx = gridX + col * (CARD_W + 5);
            int cy = gridY + row * (CARD_H + 6);
            boolean active = mut.isActiveIn(displayMask);
            boolean hovered = mouseX >= cx && mouseX < cx + CARD_W && mouseY >= cy && mouseY < cy + CARD_H;

            int bg = active ? 0xCC2A1542 : (hovered ? 0xCC1E1430 : 0xCC150E22);
            int border = active ? mut.colorRgb() : (hovered ? 0xFF8D74B8 : 0xFF3E2D59);
            graphics.fill(cx, cy, cx + CARD_W, cy + CARD_H, bg);
            graphics.renderOutline(cx, cy, CARD_W, CARD_H, border);
            if (active) {
                graphics.fill(cx, cy, cx + 3, cy + CARD_H, mut.colorRgb());
            }

            Component title = Component.translatable(mut.nameKey());
            drawScaled(graphics, title, cx + 6, cy + 5, CARD_W - 36, active ? 0xFFFFFFFF : 0xFFE6C2FF);
            Component bonus = Component.literal("+" + mut.scoreBonusPct() + "%");
            int bw = this.font.width(bonus);
            graphics.drawString(this.font, bonus, cx + CARD_W - bw - 4, cy + 5, mut.colorRgb(), true);

            List<FormattedCharSequence> descLines = this.font.split(Component.translatable(mut.descKey()), CARD_W - 10);
            for (int l = 0; l < descLines.size() && l < 3; l++) {
                graphics.drawString(this.font, descLines.get(l), cx + 5, cy + 16 + l * 9, 0xFFB8A9C9, false);
            }
        }

        // Bottom multiplier summary
        int activeCount = HiveCycleMutator.countActive(displayMask);
        double mult = HiveCycleEngine.computeScoreMultiplier(ClientCycleCache.getActiveCycleNumber(), displayMask);
        Component footer = Component.translatable("gui.mycelialmind.cycle.selection_summary",
                activeCount, HiveCycleMutator.MAX_ACTIVE_MUTATORS, String.format("%.2fx", mult));
        graphics.drawString(this.font, footer, left + 12, top + PANEL_H - 42,
                HiveCycleEngine.isValidMutatorSelection(displayMask) ? 0xFFB5E48C : 0xFFFF758F, true);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawScaled(GuiGraphics graphics, Component text, int x, int y, int maxW, int color) {
        int tw = this.font.width(text);
        if (tw <= maxW || maxW <= 4) {
            graphics.drawString(this.font, text, x, y, color, true);
        } else {
            float scale = Math.max(0.65F, (float) maxW / (float) tw);
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(this.font, text, 0, 0, color, true);
            graphics.pose().popPose();
        }
    }

    private static void playUiPing(float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BIO_SCANNER_PING.get(), pitch, 0.75F));
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
