package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.SovereignTrait;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.network.ClientColonyCache;
import com.alex.mycelialmind.network.ClientColonyCache.WorkerSnapshot;
import com.alex.mycelialmind.network.SovereignColonyActionPayload;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Sovereign Hive Nexus command screen displaying colony tier/radius/biomass/morale,
 * adaptive Sovereign Traits, worker roster controls, and incubation/upgrade buttons.
 */
public class SovereignNexusScreen extends Screen {
    private static final int PANEL_W = 368;
    private static final int PANEL_H = 248;
    private static final int VISIBLE_WORKERS = 5;

    private SymbiontCaste selectedCaste = SymbiontCaste.HARVESTER;
    private int rosterOffset = 0;

    private Button casteSelectButton;
    private Button incubateButton;
    private Button upgradeButton;

    public SovereignNexusScreen() {
        super(Component.translatable("screen.mycelialmind.sovereign_nexus"));
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    private void rebuildColonyWidgets() {
        clearWidgets();
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;
        BlockPos nexusPos = ClientColonyCache.getNexusPos();

        // Top action bar (y = top + 76)
        int btnY = top + 76;
        this.casteSelectButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.caste_select",
                        Component.translatable(selectedCaste.nameKey())),
                btn -> {
                    selectedCaste = selectedCaste.next();
                    playClick(1.15F);
                    rebuildColonyWidgets();
                }
        ).bounds(left + 10, btnY, 118, 18).build());

        int incCost = SovereignColonyEngine.getIncubationBiomassCost(ClientColonyCache.getTier());
        this.incubateButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.incubate", incCost),
                btn -> {
                    if (nexusPos != null) {
                        SovereignColonyActionPayload.sendToServer(
                                SovereignColonyActionPayload.ACTION_INCUBATE, nexusPos, selectedCaste.index(), null);
                        playClick(1.30F);
                    }
                }
        ).bounds(left + 132, btnY, 92, 18).build());
        this.incubateButton.active = ClientColonyCache.hasColony()
                && ClientColonyCache.getWorkers().size() < ClientColonyCache.getPopCap()
                && ClientColonyCache.getBiomass() >= incCost;

        int tier = ClientColonyCache.getTier();
        Component upgradeLabel = tier >= SovereignColonyEngine.MAX_TIER
                ? Component.translatable("gui.mycelialmind.colony.tier_max")
                : Component.translatable("gui.mycelialmind.colony.upgrade",
                SovereignColonyEngine.getUpgradeBiomassCost(tier),
                SovereignColonyEngine.getUpgradeDomainXpRequirement(tier));
        this.upgradeButton = this.addRenderableWidget(Button.builder(
                upgradeLabel,
                btn -> {
                    if (nexusPos != null) {
                        SovereignColonyActionPayload.sendToServer(
                                SovereignColonyActionPayload.ACTION_UPGRADE_TIER, nexusPos, 0, null);
                        playClick(1.25F);
                    }
                }
        ).bounds(left + 228, btnY, 130, 18).build());
        this.upgradeButton.active = ClientColonyCache.hasColony()
                && SovereignColonyEngine.canUpgradeTier(tier, ClientColonyCache.getBiomass(), ClientColonyCache.getDomainXp());

        // Worker roster rows (y = top + 102 .. top + 202)
        List<WorkerSnapshot> workers = ClientColonyCache.getWorkers();
        int maxOffset = Math.max(0, workers.size() - VISIBLE_WORKERS);
        if (rosterOffset > maxOffset) {
            rosterOffset = maxOffset;
        }
        int rowStartY = top + 102;
        for (int i = 0; i < VISIBLE_WORKERS && (rosterOffset + i) < workers.size(); i++) {
            WorkerSnapshot w = workers.get(rosterOffset + i);
            int rowY = rowStartY + i * 21;
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.mycelialmind.colony.btn_caste"),
                    btn -> {
                        if (nexusPos != null) {
                            SovereignColonyActionPayload.sendToServer(
                                    SovereignColonyActionPayload.ACTION_CYCLE_WORKER_CASTE, nexusPos, 0, w.workerId());
                            playClick(1.10F);
                        }
                    }
            ).bounds(left + 234, rowY, 58, 17).build());

            this.addRenderableWidget(Button.builder(
                    Component.translatable(w.standby()
                            ? "gui.mycelialmind.colony.btn_resume"
                            : "gui.mycelialmind.colony.btn_standby"),
                    btn -> {
                        if (nexusPos != null) {
                            SovereignColonyActionPayload.sendToServer(
                                    SovereignColonyActionPayload.ACTION_TOGGLE_WORKER_STANDBY, nexusPos, 0, w.workerId());
                            playClick(0.95F);
                        }
                    }
            ).bounds(left + 296, rowY, 62, 17).build());
        }

        // Bottom controls (y = top + PANEL_H - 25)
        int botY = top + PANEL_H - 25;
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.recall"),
                btn -> {
                    if (nexusPos != null) {
                        SovereignColonyActionPayload.sendToServer(
                                SovereignColonyActionPayload.ACTION_RECALL_ALL, nexusPos, 0, null);
                        playClick(1.0F);
                    }
                }
        ).bounds(left + 10, botY, 94, 18).build());

        if (workers.size() > VISIBLE_WORKERS) {
            Button prevBtn = this.addRenderableWidget(Button.builder(
                    Component.literal("<"),
                    btn -> {
                        if (rosterOffset > 0) {
                            rosterOffset--;
                            rebuildColonyWidgets();
                        }
                    }
            ).bounds(left + 112, botY, 22, 18).build());
            prevBtn.active = rosterOffset > 0;

            Button nextBtn = this.addRenderableWidget(Button.builder(
                    Component.literal(">"),
                    btn -> {
                        if (rosterOffset < maxOffset) {
                            rosterOffset++;
                            rebuildColonyWidgets();
                        }
                    }
            ).bounds(left + 138, botY, 22, 18).build());
            nextBtn.active = rosterOffset < maxOffset;
        }

        this.addRenderableWidget(Button.builder(
                CommonComponents.GUI_DONE,
                btn -> onClose()
        ).bounds(left + PANEL_W - 84, botY, 74, 18).build());
    }

    private int lastWorkerCount = -1;
    private int lastBiomass = -1;
    private int lastTier = -1;
    private int lastTraitMask = -1;

    @Override
    public void tick() {
        super.tick();
        if (lastWorkerCount != ClientColonyCache.getWorkers().size()
                || lastBiomass != ClientColonyCache.getBiomass()
                || lastTier != ClientColonyCache.getTier()
                || lastTraitMask != ClientColonyCache.getTraitMask()) {
            lastWorkerCount = ClientColonyCache.getWorkers().size();
            lastBiomass = ClientColonyCache.getBiomass();
            lastTier = ClientColonyCache.getTier();
            lastTraitMask = ClientColonyCache.getTraitMask();
            rebuildColonyWidgets();
        }
    }

    @Override
    protected void rebuildWidgets() {
        lastWorkerCount = ClientColonyCache.getWorkers().size();
        lastBiomass = ClientColonyCache.getBiomass();
        lastTier = ClientColonyCache.getTier();
        lastTraitMask = ClientColonyCache.getTraitMask();
        rebuildColonyWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE0F1B18);
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, 0xFF4AD66D);
        graphics.fill(left + 1, top + 1, left + PANEL_W - 1, top + 22, 0xFF142E25);

        graphics.drawCenteredString(this.font, this.title, left + PANEL_W / 2, top + 7, 0xFF9BF6B0);

        List<WorkerSnapshot> workers = ClientColonyCache.getWorkers();
        Component summary = Component.translatable("gui.mycelialmind.colony.summary",
                ClientColonyCache.getTier(),
                ClientColonyCache.getRadius(),
                workers.size(),
                ClientColonyCache.getPopCap(),
                ClientColonyCache.getBiomass(),
                ClientColonyCache.getMoralePct(),
                ClientColonyCache.getDomainXp());
        graphics.drawString(this.font, summary, left + 10, top + 28, 0xFFD8F3DC, false);

        Component logistics = Component.translatable("gui.mycelialmind.colony.logistics",
                ClientColonyCache.getLinkedOutposts(),
                ClientColonyCache.getInputChests(),
                ClientColonyCache.getOutputChests(),
                ClientColonyCache.getWorkZones(),
                ClientColonyCache.getActiveOrders());
        graphics.drawString(this.font, logistics, left + 10, top + 40, 0xFF72EFDD, false);

        // Sovereign Traits strip (y = top + 54)
        SovereignTrait[] traits = SovereignTrait.values();
        int tx = left + 10;
        int ty = top + 54;
        int badgeW = 68;
        int mask = ClientColonyCache.getTraitMask();
        for (int i = 0; i < traits.length; i++) {
            SovereignTrait t = traits[i];
            boolean unlocked = t.isUnlockedIn(mask);
            int bx = tx + i * (badgeW + 2);
            graphics.fill(bx, ty, bx + badgeW, ty + 16, unlocked ? 0xCC183A2D : 0xAA17141F);
            graphics.renderOutline(bx, ty, badgeW, 16, unlocked ? t.accentColor() : 0xFF4A4E69);
            Component tLabel = Component.translatable(t.nameKey());
            String trimmed = this.font.plainSubstrByWidth(tLabel.getString(), badgeW - 6);
            graphics.drawString(this.font, trimmed, bx + 3, ty + 4, unlocked ? 0xFFFFFFFF : 0xFF8D99AE, false);
        }

        // Worker roster rows
        int rowStartY = top + 102;
        if (workers.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("gui.mycelialmind.colony.empty_roster"),
                    left + PANEL_W / 2, rowStartY + 34, 0xFF9DE0AD);
        } else {
            for (int i = 0; i < VISIBLE_WORKERS && (rosterOffset + i) < workers.size(); i++) {
                WorkerSnapshot w = workers.get(rosterOffset + i);
                int rowY = rowStartY + i * 21;
                graphics.fill(left + 10, rowY, left + 230, rowY + 17, 0xAA13241E);
                graphics.renderOutline(left + 10, rowY, 220, 17, w.caste().accentColor());
                Component rowText = Component.translatable(
                        "gui.mycelialmind.colony.worker_row",
                        Component.translatable(w.caste().nameKey()),
                        Component.translatable(w.rank().nameKey()),
                        Component.translatable(w.temperament().nameKey()),
                        w.nutrition(),
                        w.fatigue()
                );
                String trimmed = this.font.plainSubstrByWidth(rowText.getString(), 212);
                graphics.drawString(this.font, trimmed, left + 14, rowY + 4, w.standby() ? 0xFFADB5BD : 0xFFE9ECEF, false);
            }
        }

        if (ClientColonyCache.getActiveRaidRemaining() > 0) {
            graphics.drawString(this.font,
                    Component.translatable("gui.mycelialmind.colony.raid_active", ClientColonyCache.getActiveRaidRemaining()),
                    left + 170, top + PANEL_H - 20, 0xFFFF758F, true);
        }

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static void playClick(float pitch) {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(ModSounds.BIO_SCANNER_PING.get(), pitch));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
