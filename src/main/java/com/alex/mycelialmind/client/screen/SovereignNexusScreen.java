package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.ColonySiteHabit;
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
    private int memorySiteIndex;
    private int memorySourceIndex;
    private int memoryTargetIndex = 1;
    private boolean memoryPage;

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

        addPageTabs(left, top);
        if (memoryPage) {
            rebuildMemoryWidgets(left, top, nexusPos);
            return;
        }

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

    private void addPageTabs(int left, int top) {
        Button overview = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.tab_overview"), button -> {
                    memoryPage = false;
                    playClick(1.0F);
                    rebuildColonyWidgets();
                }).bounds(left + 4, top + 3, 78, 16).build());
        overview.active = memoryPage;
        Button memories = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.tab_memory"), button -> {
                    memoryPage = true;
                    playClick(1.18F);
                    rebuildColonyWidgets();
                }).bounds(left + PANEL_W - 82, top + 3, 78, 16).build());
        memories.active = !memoryPage;
    }

    private void rebuildMemoryWidgets(int left, int top, BlockPos nexusPos) {
        List<ClientColonyCache.SiteMemorySnapshot> sites = ClientColonyCache.getSiteMemories();
        if (sites.isEmpty()) {
            this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                    .bounds(left + PANEL_W - 84, top + PANEL_H - 25, 74, 18).build());
            return;
        }
        memorySiteIndex = Math.floorMod(memorySiteIndex, sites.size());
        memorySourceIndex = Math.floorMod(memorySourceIndex, sites.size());
        memoryTargetIndex = Math.floorMod(memoryTargetIndex, sites.size());
        ClientColonyCache.SiteMemorySnapshot selected = sites.get(memorySiteIndex);
        BlockPos selectedPos = selected.sitePos();

        this.addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            memorySiteIndex = Math.floorMod(memorySiteIndex - 1, sites.size());
            rebuildColonyWidgets();
        }).bounds(left + 10, top + 22, 22, 18).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            memorySiteIndex = (memorySiteIndex + 1) % sites.size();
            rebuildColonyWidgets();
        }).bounds(left + PANEL_W - 32, top + 22, 22, 18).build());

        ColonySiteHabit learned = ColonySiteHabit.fromIndex(selected.learnedHabit());
        ColonySiteHabit trial = ColonySiteHabit.fromIndex(selected.trialHabit());
        boolean hasHistory = learned != ColonySiteHabit.NONE || trial != ColonySiteHabit.NONE
                || selected.incidentCount() > 0;
        Button adopt = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.memory_adopt"), button -> {
                    if (nexusPos != null) {
                        SovereignColonyActionPayload.sendMemoryAction(
                                SovereignColonyActionPayload.ACTION_ADOPT_SITE_MEMORY, nexusPos, selectedPos, null);
                        playClick(1.25F);
                    }
                }).bounds(left + 10, top + 108, 170, 18).build());
        adopt.active = nexusPos != null && trial != ColonySiteHabit.NONE && selected.successfulTrials() >= 2;

        Button forget = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.memory_forget"), button -> {
                    if (nexusPos != null) {
                        SovereignColonyActionPayload.sendMemoryAction(
                                SovereignColonyActionPayload.ACTION_FORGET_SITE_MEMORY, nexusPos, selectedPos, null);
                        playClick(0.82F);
                    }
                }).bounds(left + 188, top + 108, 170, 18).build());
        forget.active = nexusPos != null && hasHistory;

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.memory_source",
                        getSiteName(sites.get(memorySourceIndex).sitePos(), nexusPos)), button -> {
                    memorySourceIndex = (memorySourceIndex + 1) % sites.size();
                    rebuildColonyWidgets();
                }).bounds(left + 10, top + 135, 168, 20).build());
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.memory_target",
                        getSiteName(sites.get(memoryTargetIndex).sitePos(), nexusPos)), button -> {
                    memoryTargetIndex = (memoryTargetIndex + 1) % sites.size();
                    rebuildColonyWidgets();
                }).bounds(left + 188, top + 135, 170, 20).build());

        ClientColonyCache.SiteMemorySnapshot sourceMemory = sites.get(memorySourceIndex);
        ClientColonyCache.SiteMemorySnapshot targetMemory = sites.get(memoryTargetIndex);
        boolean canTeach = nexusPos != null && !ClientColonyCache.hasMemoryTransfer()
                && memorySourceIndex != memoryTargetIndex
                && sourceMemory.learnedHabit() != ColonySiteHabit.NONE.ordinal()
                && targetMemory.trialHabit() == ColonySiteHabit.NONE.ordinal();
        Button teach = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.memory_teach"), button -> {
                    if (nexusPos != null) {
                        SovereignColonyActionPayload.sendMemoryAction(
                                SovereignColonyActionPayload.ACTION_TEACH_SITE_MEMORY, nexusPos,
                                sites.get(memorySourceIndex).sitePos(), sites.get(memoryTargetIndex).sitePos());
                        playClick(1.38F);
                    }
                }).bounds(left + 10, top + 163, 170, 18).build());
        teach.active = canTeach;

        Button cancel = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.memory_cancel_transfer"), button -> {
                    if (nexusPos != null) {
                        SovereignColonyActionPayload.sendMemoryAction(
                                SovereignColonyActionPayload.ACTION_CANCEL_MEMORY_TRANSFER, nexusPos, null, null);
                        playClick(0.9F);
                    }
                }).bounds(left + 188, top + 163, 170, 18).build());
        cancel.active = nexusPos != null && ClientColonyCache.hasMemoryTransfer();

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + PANEL_W - 84, top + PANEL_H - 25, 74, 18).build());
    }

    private Component getSiteName(BlockPos sitePos, BlockPos nexusPos) {
        if (nexusPos != null && sitePos.equals(nexusPos)) {
            return Component.translatable("gui.mycelialmind.colony.memory_nexus");
        }
        return Component.translatable("gui.mycelialmind.colony.memory_outpost",
                sitePos.getX(), sitePos.getY(), sitePos.getZ());
    }

    private int lastWorkerCount = -1;
    private int lastBiomass = -1;
    private int lastTier = -1;
    private int lastTraitMask = -1;
    private int lastMemoryHash;
    private boolean lastTransferActive;

    @Override
    public void tick() {
        super.tick();
        if (lastWorkerCount != ClientColonyCache.getWorkers().size()
                || lastBiomass != ClientColonyCache.getBiomass()
                || lastTier != ClientColonyCache.getTier()
                || lastTraitMask != ClientColonyCache.getTraitMask()
                || lastMemoryHash != ClientColonyCache.getSiteMemories().hashCode()
                || lastTransferActive != ClientColonyCache.hasMemoryTransfer()) {
            lastWorkerCount = ClientColonyCache.getWorkers().size();
            lastBiomass = ClientColonyCache.getBiomass();
            lastTier = ClientColonyCache.getTier();
            lastTraitMask = ClientColonyCache.getTraitMask();
            lastMemoryHash = ClientColonyCache.getSiteMemories().hashCode();
            lastTransferActive = ClientColonyCache.hasMemoryTransfer();
            rebuildColonyWidgets();
        }
    }

    @Override
    protected void rebuildWidgets() {
        lastWorkerCount = ClientColonyCache.getWorkers().size();
        lastBiomass = ClientColonyCache.getBiomass();
        lastTier = ClientColonyCache.getTier();
        lastTraitMask = ClientColonyCache.getTraitMask();
        lastMemoryHash = ClientColonyCache.getSiteMemories().hashCode();
        lastTransferActive = ClientColonyCache.hasMemoryTransfer();
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

        if (memoryPage) {
            renderMemoryPage(graphics, left, top);
            super.render(graphics, mouseX, mouseY, partialTick);
            return;
        }

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

    private void renderMemoryPage(GuiGraphics graphics, int left, int top) {
        List<ClientColonyCache.SiteMemorySnapshot> sites = ClientColonyCache.getSiteMemories();
        if (sites.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.mycelialmind.colony.memory_no_sites"),
                    left + 10, top + 34, 0xFFD8F3DC, false);
            return;
        }
        memorySiteIndex = Math.floorMod(memorySiteIndex, sites.size());
        memorySourceIndex = Math.floorMod(memorySourceIndex, sites.size());
        memoryTargetIndex = Math.floorMod(memoryTargetIndex, sites.size());
        BlockPos nexusPos = ClientColonyCache.getNexusPos();
        ClientColonyCache.SiteMemorySnapshot memory = sites.get(memorySiteIndex);
        ColonySiteHabit learned = ColonySiteHabit.fromIndex(memory.learnedHabit());
        ColonySiteHabit trial = ColonySiteHabit.fromIndex(memory.trialHabit());

        graphics.drawCenteredString(this.font,
                Component.translatable("gui.mycelialmind.colony.memory_site", memorySiteIndex + 1, sites.size(),
                        getSiteName(memory.sitePos(), nexusPos)),
                left + PANEL_W / 2, top + 34, 0xFF9BF6B0);
        graphics.drawString(this.font,
                Component.translatable("gui.mycelialmind.colony.memory_learned",
                        Component.translatable(learned.translationKey())),
                left + 10, top + 56, 0xFFE9ECEF, false);
        graphics.drawString(this.font,
                Component.translatable("gui.mycelialmind.colony.memory_trial",
                        Component.translatable(trial.translationKey()), memory.successfulTrials(), 2),
                left + 10, top + 72, trial == ColonySiteHabit.NONE ? 0xFFADB5BD : 0xFFFFD166, false);
        graphics.drawString(this.font,
                Component.translatable("gui.mycelialmind.colony.memory_incident",
                        memory.incidentCount(), Component.translatable(incidentTranslationKey(memory.lastIncidentCode()))),
                left + 10, top + 88, 0xFF72EFDD, false);
        graphics.drawString(this.font,
                Component.translatable("gui.mycelialmind.colony.memory_outcome",
                        Component.translatable(outcomeTranslationKey(memory.lastOutcomeCode())), memory.failedTrials()),
                left + 10, top + 104, 0xFFD8F3DC, false);

        if (ClientColonyCache.hasMemoryTransfer()) {
            Component transfer = Component.translatable("gui.mycelialmind.colony.memory_transfer_status",
                    Component.translatable(ColonySiteHabit.fromIndex(ClientColonyCache.getMemoryTransferHabit()).translationKey()),
                    getSiteName(ClientColonyCache.getMemoryTransferSource(), nexusPos),
                    getSiteName(ClientColonyCache.getMemoryTransferTarget(), nexusPos),
                    Component.translatable(ClientColonyCache.isMemoryTransferInTransit()
                            ? "gui.mycelialmind.colony.memory_carried"
                            : "gui.mycelialmind.colony.memory_not_carried"));
            String trimmed = this.font.plainSubstrByWidth(transfer.getString(), PANEL_W - 20);
            graphics.drawString(this.font, trimmed, left + 10, top + 193, 0xFF72EFDD, false);
        } else {
            graphics.drawString(this.font, Component.translatable("gui.mycelialmind.colony.memory_idle"),
                    left + 10, top + 193, 0xFFADB5BD, false);
        }
    }

    private static String incidentTranslationKey(int incidentCode) {
        return switch (incidentCode) {
            case 1 -> "gui.mycelialmind.colony.memory_incident_courier";
            case 2 -> "gui.mycelialmind.colony.memory_incident_fatigue";
            case 3 -> "gui.mycelialmind.colony.memory_incident_contention";
            case 4 -> "gui.mycelialmind.colony.memory_incident_taught";
            default -> "gui.mycelialmind.colony.memory_incident_none";
        };
    }

    private static String outcomeTranslationKey(int outcomeCode) {
        return outcomeCode == 0
                ? "gui.mycelialmind.colony.memory_outcome_none"
                : "gui.mycelialmind.colony.memory_outcome_recorded";
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
