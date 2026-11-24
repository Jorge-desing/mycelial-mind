package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.ColonySiteHabit;
import com.alex.mycelialmind.colony.ColonyReconstructionStatus;
import com.alex.mycelialmind.colony.ColonyWitnessMemory;
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
import java.util.UUID;

/**
 * Sovereign Hive Nexus command screen displaying colony tier/radius/biomass/morale,
 * adaptive Sovereign Traits, worker roster controls, and incubation/upgrade buttons.
 */
public class SovereignNexusScreen extends Screen {
    private static final int PANEL_W = 368;
    private static final int PANEL_H = 360;
    private static final int VISIBLE_WORKERS = 5;

    private SymbiontCaste selectedCaste = SymbiontCaste.HARVESTER;
    private int rosterOffset = 0;
    private int memorySiteIndex;
    private int memorySourceIndex;
    private int memoryTargetIndex = 1;
    private int reconstructionIndex;
    private int reconstructionSuccessorIndex;
    private int reconstructionWitnessIndex;
    private boolean memoryPage;
    private boolean dialectPage;
    private int dialectSource;
    private int dialectTarget = 1;
    private int dialectMeaning = 1;
    private int dialectCall;
    private int dialectWorker;
    private int dialectTeacher;
    private boolean dialectTeacherCustom;

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

    private float panelScale() {
        return Math.min(1.0F, Math.min((float) width / (PANEL_W + 8), (float) height / (PANEL_H + 8)));
    }

    private int logicalWidth() { return Math.round(width / panelScale()); }
    private int logicalHeight() { return Math.round(height / panelScale()); }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX / panelScale(), mouseY / panelScale(), button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX / panelScale(), mouseY / panelScale(), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        float scale = panelScale();
        return super.mouseDragged(mouseX / scale, mouseY / scale, button, dragX / scale, dragY / scale);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        return super.mouseScrolled(mouseX / panelScale(), mouseY / panelScale(), horizontal, vertical);
    }

    private void rebuildColonyWidgets() {
        clearWidgets();
        int left = (logicalWidth() - PANEL_W) / 2;
        int top = (logicalHeight() - PANEL_H) / 2;
        BlockPos nexusPos = ClientColonyCache.getNexusPos();

        addPageTabs(left, top);
        if (dialectPage) {
            rebuildDialectWidgets(left, top, nexusPos);
            return;
        }
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
        boolean creative = Minecraft.getInstance().player != null && Minecraft.getInstance().player.isCreative();
        this.incubateButton.active = ClientColonyCache.hasColony()
                && (creative || (ClientColonyCache.getWorkers().size() < ClientColonyCache.getPopCap()
                && ClientColonyCache.getBiomass() >= incCost));

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
        this.upgradeButton.active = ClientColonyCache.hasColony() && tier < SovereignColonyEngine.MAX_TIER
                && (creative || SovereignColonyEngine.canUpgradeTier(tier, ClientColonyCache.getBiomass(), ClientColonyCache.getDomainXp()));

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
                    dialectPage = false;
                    playClick(1.0F);
                    rebuildColonyWidgets();
                }).bounds(left + 4, top + 3, 78, 16).build());
        overview.active = memoryPage || dialectPage;
        Button memories = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.tab_memory"), button -> {
                    memoryPage = true;
                    dialectPage = false;
                    playClick(1.18F);
                    rebuildColonyWidgets();
                }).bounds(left + PANEL_W - 82, top + 3, 78, 16).build());
        memories.active = !memoryPage || dialectPage;
        Button dialect = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.dialect.tab"), button -> {
                    dialectPage = true;
                    memoryPage = false;
                    rebuildColonyWidgets();
                }).bounds(left + 145, top + 3, 78, 16).build());
        dialect.active = !dialectPage;
    }

    private static ColonySiteHabit dialectConflictHabit(ClientColonyCache.SiteMemorySnapshot site,
                                                      ColonySiteHabit exclude, int call) {
        if (site == null || call < 0 || call >= 9) return null;
        int bit = 1 << call;
        if (exclude != ColonySiteHabit.ESCORTED_COURIER && (site.escortCall() & bit) != 0) {
            return ColonySiteHabit.ESCORTED_COURIER;
        }
        if (exclude != ColonySiteHabit.REST_BEFORE_RETURN && (site.restCall() & bit) != 0) {
            return ColonySiteHabit.REST_BEFORE_RETURN;
        }
        if (exclude != ColonySiteHabit.STAGGERED_PICKUP && (site.pickupCall() & bit) != 0) {
            return ColonySiteHabit.STAGGERED_PICKUP;
        }
        return null;
    }

    private static String formatDialectMask(int mask) {
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < 9; c++) {
            if ((mask & (1 << c)) != 0) {
                if (!sb.isEmpty()) sb.append(',');
                sb.append((c / 3) + 1).append('·').append((c % 3) + 1);
            }
        }
        return sb.toString();
    }

    private static Component formatSiteDialectCalls(ClientColonyCache.SiteMemorySnapshot site) {
        if (site == null) return Component.literal("—");
        StringBuilder sb = new StringBuilder();
        if (site.escortCall() != 0) {
            sb.append(Component.translatable("gui.mycelialmind.colony.dialect.short_escort").getString())
                    .append(' ').append(formatDialectMask(site.escortCall()));
        }
        if (site.restCall() != 0) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append(Component.translatable("gui.mycelialmind.colony.dialect.short_rest").getString())
                    .append(' ').append(formatDialectMask(site.restCall()));
        }
        if (site.pickupCall() != 0) {
            if (!sb.isEmpty()) sb.append(" · ");
            sb.append(Component.translatable("gui.mycelialmind.colony.dialect.short_pickup").getString())
                    .append(' ').append(formatDialectMask(site.pickupCall()));
        }
        return Component.literal(sb.isEmpty() ? "—" : sb.toString());
    }

    private void rebuildDialectWidgets(int left, int top, BlockPos nexus) {
        var sites = ClientColonyCache.getSiteMemories().stream()
                .filter(site -> !site.sitePos().equals(nexus)).toList();
        var workers = ClientColonyCache.getWorkers();
        if (sites.size() >= 2) {
            dialectSource = Math.floorMod(dialectSource, sites.size());
            dialectTarget = Math.floorMod(dialectTarget, sites.size());
            var source = sites.get(dialectSource);
            var target = sites.get(dialectTarget);
            ColonySiteHabit habit = ColonySiteHabit.fromIndex(dialectMeaning);
            UUID episodeActor = switch (habit) {
                case ESCORTED_COURIER -> source.escortActor();
                case REST_BEFORE_RETURN -> source.restActor();
                case STAGGERED_PICKUP -> source.pickupActor();
                default -> null;
            };
            addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.colony.dialect.source", formatPos(source.sitePos())), b -> {
                dialectSource = (dialectSource + 1) % sites.size();
                dialectTeacherCustom = false;
                rebuildColonyWidgets();
            }).bounds(left + 10, top + 45, 348, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.colony.dialect.target", formatPos(target.sitePos())), b -> {
                dialectTarget = (dialectTarget + 1) % sites.size(); rebuildColonyWidgets();
            }).bounds(left + 10, top + 68, 348, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.colony.dialect.meaning", Component.translatable(habit.translationKey())), b -> {
                dialectMeaning = dialectMeaning % 3 + 1;
                dialectTeacherCustom = false;
                rebuildColonyWidgets();
            }).bounds(left + 10, top + 91, 348, 20).build());
            addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.colony.dialect.call", dialectCall / 3 + 1, dialectCall % 3 + 1), b -> {
                dialectCall = (dialectCall + 1) % 9; rebuildColonyWidgets();
            }).bounds(left + 10, top + 114, 348, 20).build());
            boolean hasAnyLesson = sites.stream().anyMatch(s -> s.lessonStage() >= 0);
            int call = dialectCall;
            if (!workers.isEmpty()) {
                dialectWorker = Math.floorMod(dialectWorker, workers.size());
                var worker = workers.get(dialectWorker);
                if (!dialectTeacherCustom && episodeActor != null) {
                    for (int i = 0; i < workers.size(); i++) {
                        if (workers.get(i).workerId().equals(episodeActor)) {
                            dialectTeacher = i;
                            break;
                        }
                    }
                }
                addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.colony.dialect.apprentice", worker.workerId().toString().substring(0, 8), Component.translatable(worker.caste().nameKey())), b -> {
                    dialectWorker = (dialectWorker + 1) % workers.size(); rebuildColonyWidgets();
                }).bounds(left + 10, top + 137, 348, 20).build());
                dialectTeacher = Math.floorMod(dialectTeacher, workers.size());
                var teacher = workers.get(dialectTeacher);
                addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.colony.dialect.teacher",
                        teacher.workerId().toString().substring(0, 8), Component.translatable(teacher.caste().nameKey())), b -> {
                    dialectTeacher = (dialectTeacher + 1) % workers.size();
                    dialectTeacherCustom = true;
                    rebuildColonyWidgets();
                }).bounds(left + 10, top + 160, 348, 20).build());
                int knownCall = switch (habit) {
                    case ESCORTED_COURIER -> source.escortCall();
                    case REST_BEFORE_RETURN -> source.restCall();
                    case STAGGERED_PICKUP -> source.pickupCall();
                    default -> -1;
                };
                boolean coinage = knownCall == 0;
                boolean hasEpisode = (source.episodeMask() & (1 << habit.ordinal())) != 0;
                ColonySiteHabit sourceConflict = dialectConflictHabit(source, habit, call);
                ColonySiteHabit targetConflict = dialectConflictHabit(target, habit, call);
                BlockPos conflictSite = sourceConflict != null ? source.sitePos()
                        : targetConflict != null ? target.sitePos() : null;
                Button start = addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.colony.dialect.start"), b -> {
                    SovereignColonyActionPayload.sendDialectLesson(nexus, source.sitePos(), target.sitePos(), worker.workerId(), teacher.workerId(), habit.ordinal(), call, coinage);
                    playClick(1.2F);
                }).bounds(left + 10, top + 183, 170, 20).build());
                start.active = nexus != null && !hasAnyLesson
                        && !source.sitePos().equals(target.sitePos())
                        && workers.size() >= 2
                        && !teacher.workerId().equals(worker.workerId())
                        && hasEpisode
                        && sourceConflict == null
                        && targetConflict == null;

                if (conflictSite != null) {
                    ColonySiteHabit conflictingHabit = sourceConflict != null ? sourceConflict : targetConflict;
                    BlockPos resolvedSite = conflictSite.immutable();
                    Button clearConflict = addRenderableWidget(Button.builder(
                            Component.translatable("gui.mycelialmind.colony.dialect.clear_conflict",
                                    Component.translatable(conflictingHabit.translationKey()),
                                    (call / 3) + 1, (call % 3) + 1),
                            b -> {
                                SovereignColonyActionPayload.sendClearDialectCall(nexus, resolvedSite, call);
                                playClick(1.1F);
                            }).bounds(left + 10, top + 278, 348, 20).build());
                    clearConflict.active = nexus != null && !hasAnyLesson;
                }
            }
            addRenderableWidget(Button.builder(Component.translatable("gui.mycelialmind.colony.dialect.cancel"), b -> {
                SovereignColonyActionPayload.sendToServer(SovereignColonyActionPayload.ACTION_CANCEL_DIALECT_LESSON, nexus, 0, null);
                playClick(0.9F);
            }).bounds(left + 188, top + 183, 170, 20).build()).active = nexus != null && hasAnyLesson;
        }
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(left + PANEL_W - 84, top + PANEL_H - 25, 74, 18).build());
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

        rebuildReconstructionWidgets(left, top, nexusPos);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(left + PANEL_W - 84, top + PANEL_H - 25, 74, 18).build());
    }

    private void rebuildReconstructionWidgets(int left, int top, BlockPos nexusPos) {
        List<ClientColonyCache.ReconstructionSnapshot> entries = ClientColonyCache.getReconstructions();
        if (entries.isEmpty()) {
            return;
        }
        reconstructionIndex = Math.floorMod(reconstructionIndex, entries.size());
        ClientColonyCache.ReconstructionSnapshot entry = entries.get(reconstructionIndex);
        List<BlockPos> successors = getReconstructionSuccessors(entry);
        if (successors.isEmpty()) {
            reconstructionSuccessorIndex = 0;
        } else {
            reconstructionSuccessorIndex = Math.floorMod(reconstructionSuccessorIndex, successors.size());
            if (entry.hasSuccessor()) {
                int current = successors.indexOf(entry.successorSite());
                if (current >= 0) {
                    reconstructionSuccessorIndex = current;
                }
            }
        }
        List<ClientColonyCache.ReconstructionWitnessSnapshot> witnesses = entry.witnesses();
        if (!witnesses.isEmpty()) {
            reconstructionWitnessIndex = Math.floorMod(reconstructionWitnessIndex, witnesses.size());
        } else {
            reconstructionWitnessIndex = 0;
        }

        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.reconstruction.ruin",
                        reconstructionIndex + 1, entries.size(), formatPos(entry.ruinedSite())), button -> {
                    reconstructionIndex = (reconstructionIndex + 1) % entries.size();
                    reconstructionWitnessIndex = 0;
                    playClick(1.05F);
                    rebuildColonyWidgets();
                }).bounds(left + 10, top + 230, 348, 18).build());

        Component successorLabel = entry.hasSuccessor()
                ? Component.translatable("gui.mycelialmind.colony.reconstruction.successor",
                formatPos(entry.successorSite()))
                : Component.translatable("gui.mycelialmind.colony.reconstruction.no_successor");
        Button successor = this.addRenderableWidget(Button.builder(successorLabel, button -> {
            if (nexusPos != null && !successors.isEmpty()) {
                int current = entry.hasSuccessor() ? successors.indexOf(entry.successorSite()) : -1;
                if (current >= 0) {
                    reconstructionSuccessorIndex = (current + 1) % successors.size();
                }
                BlockPos next = successors.get(reconstructionSuccessorIndex);
                SovereignColonyActionPayload.sendReconstructionAction(
                        SovereignColonyActionPayload.ACTION_SET_RECONSTRUCTION_SUCCESSOR,
                        nexusPos, entry.ruinedSite(), next, null, null);
                playClick(1.18F);
            }
        }).bounds(left + 10, top + 250, 348, 18).build());
        successor.active = nexusPos != null && !successors.isEmpty() && !isReconstructionClosed(entry);

        Button witness = this.addRenderableWidget(Button.builder(
                getReconstructionWitnessLabel(entry, witnesses), button -> {
                    if (!witnesses.isEmpty()) {
                        reconstructionWitnessIndex = (reconstructionWitnessIndex + 1) % witnesses.size();
                        playClick(1.12F);
                        rebuildColonyWidgets();
                    }
                }).bounds(left + 10, top + 270, 348, 18).build());
        witness.active = witnesses.size() > 1;

        ClientColonyCache.ReconstructionWitnessSnapshot selectedWitness = witnesses.isEmpty()
                ? null : witnesses.get(reconstructionWitnessIndex);
        Button call = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.reconstruction.call"), button -> {
                    if (nexusPos != null && selectedWitness != null) {
                        ColonyWitnessMemory.Type type = ColonyWitnessMemory.Type.fromIndex(selectedWitness.type());
                        SovereignColonyActionPayload.sendReconstructionAction(
                                SovereignColonyActionPayload.ACTION_CALL_RECONSTRUCTION_WITNESS,
                                nexusPos, entry.ruinedSite(), entry.successorSite(), selectedWitness.witnessId(), type);
                        playClick(1.3F);
                    }
                }).bounds(left + 10, top + 290, 170, 18).build());
        int ready = ColonyReconstructionStatus.READY.ordinal();
        int noPath = ColonyReconstructionStatus.NO_PATH.ordinal();
        int habitPresent = ColonyReconstructionStatus.HABIT_ALREADY_PRESENT.ordinal();
        call.active = nexusPos != null && entry.hasSuccessor() && selectedWitness != null
                && (selectedWitness.status() == ready || selectedWitness.status() == noPath
                || selectedWitness.status() == habitPresent);

        Button abandon = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.mycelialmind.colony.reconstruction.abandon"), button -> {
                    if (nexusPos != null) {
                        SovereignColonyActionPayload.sendReconstructionAction(
                                SovereignColonyActionPayload.ACTION_ABANDON_RECONSTRUCTION,
                                nexusPos, entry.ruinedSite(), null, null, null);
                        playClick(0.8F);
                    }
                }).bounds(left + 188, top + 290, 170, 18).build());
        abandon.active = nexusPos != null;
    }

    private List<BlockPos> getReconstructionSuccessors(ClientColonyCache.ReconstructionSnapshot entry) {
        BlockPos nexus = ClientColonyCache.getNexusPos();
        return ClientColonyCache.getSiteMemories().stream()
                .map(ClientColonyCache.SiteMemorySnapshot::sitePos)
                .filter(pos -> !pos.equals(nexus) && !pos.equals(entry.ruinedSite()))
                .toList();
    }

    private Component getReconstructionWitnessLabel(
            ClientColonyCache.ReconstructionSnapshot entry,
            List<ClientColonyCache.ReconstructionWitnessSnapshot> witnesses
    ) {
        if (witnesses.isEmpty()) {
            return Component.translatable("gui.mycelialmind.colony.reconstruction.no_witnesses");
        }
        reconstructionWitnessIndex = Math.floorMod(reconstructionWitnessIndex, witnesses.size());
        ClientColonyCache.ReconstructionWitnessSnapshot witness = witnesses.get(reconstructionWitnessIndex);
        ColonyWitnessMemory.Type type = ColonyWitnessMemory.Type.fromIndex(witness.type());
        ColonyReconstructionStatus status = statusFromIndex(witness.status());
        return Component.translatable("gui.mycelialmind.colony.reconstruction.witness",
                reconstructionWitnessIndex + 1, witnesses.size(),
                witness.witnessId().toString().substring(0, 8),
                Component.translatable(type != null ? type.translationKey()
                        : "gui.mycelialmind.colony.reconstruction.memory.unknown"),
                Component.translatable(status.translationKey()));
    }

    private static ColonyReconstructionStatus statusFromIndex(int index) {
        ColonyReconstructionStatus[] values = ColonyReconstructionStatus.values();
        return index >= 0 && index < values.length ? values[index] : ColonyReconstructionStatus.WITNESS_UNAVAILABLE;
    }

    private static boolean isReconstructionClosed(ClientColonyCache.ReconstructionSnapshot entry) {
        return (entry.restoredMask() & entry.requiredMask()) == entry.requiredMask();
    }

    private static String formatPos(BlockPos pos) {
        return pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
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
    private int lastReconstructionHash;

    @Override
    public void tick() {
        super.tick();
        if (lastWorkerCount != ClientColonyCache.getWorkers().size()
                || lastBiomass != ClientColonyCache.getBiomass()
                || lastTier != ClientColonyCache.getTier()
                || lastTraitMask != ClientColonyCache.getTraitMask()
                || lastMemoryHash != ClientColonyCache.getSiteMemories().hashCode()
                || lastTransferActive != ClientColonyCache.hasMemoryTransfer()
                || lastReconstructionHash != ClientColonyCache.getReconstructions().hashCode()) {
            lastWorkerCount = ClientColonyCache.getWorkers().size();
            lastBiomass = ClientColonyCache.getBiomass();
            lastTier = ClientColonyCache.getTier();
            lastTraitMask = ClientColonyCache.getTraitMask();
            lastMemoryHash = ClientColonyCache.getSiteMemories().hashCode();
            lastTransferActive = ClientColonyCache.hasMemoryTransfer();
            lastReconstructionHash = ClientColonyCache.getReconstructions().hashCode();
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
        lastReconstructionHash = ClientColonyCache.getReconstructions().hashCode();
        rebuildColonyWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        float scale = panelScale();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 1.0F);
        mouseX = (int) (mouseX / scale);
        mouseY = (int) (mouseY / scale);
        int left = (logicalWidth() - PANEL_W) / 2;
        int top = (logicalHeight() - PANEL_H) / 2;

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xEE0F1B18);
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, 0xFF4AD66D);
        graphics.fill(left + 1, top + 1, left + PANEL_W - 1, top + 22, 0xFF142E25);

        graphics.drawCenteredString(this.font, this.title, left + PANEL_W / 2, top + 7, 0xFF9BF6B0);

        if (dialectPage) {
            graphics.drawString(this.font, Component.translatable("gui.mycelialmind.colony.dialect.title"), left + 10, top + 25, 0xFFD8B4FE, true);
            var sites = ClientColonyCache.getSiteMemories().stream()
                    .filter(site -> !site.sitePos().equals(ClientColonyCache.getNexusPos())).toList();
            if (sites.size() < 2) {
                graphics.drawString(this.font, Component.translatable("gui.mycelialmind.colony.dialect.need_sites"),
                        left + 10, top + 48, 0xFFFDE68A, false);
            } else {
                var source = sites.get(Math.floorMod(dialectSource, sites.size()));
                var target = sites.get(Math.floorMod(dialectTarget, sites.size()));
                var workersList = ClientColonyCache.getWorkers();
                ColonySiteHabit habit = ColonySiteHabit.fromIndex(dialectMeaning);
                int stage = sites.stream().mapToInt(ClientColonyCache.SiteMemorySnapshot::lessonStage)
                        .filter(s -> s >= 0).findFirst().orElse(-1);
                UUID actor = switch (habit) {
                    case ESCORTED_COURIER -> source.escortActor();
                    case REST_BEFORE_RETURN -> source.restActor();
                    case STAGGERED_PICKUP -> source.pickupActor();
                    default -> null;
                };
                String key = stage < 0 ? "idle" : switch (stage) {
                    case 0 -> "travelling";
                    case 1 -> "observing";
                    default -> "returning";
                };
                graphics.drawString(this.font, Component.translatable("gui.mycelialmind.colony.dialect." + key),
                        left + 10, top + 210, 0xFFD8F3DC, false);
                graphics.drawString(this.font, Component.translatable("gui.mycelialmind.colony.dialect.episode_actor",
                                actor == null ? "—" : actor.toString().substring(0, 8)),
                        left + 10, top + 223, 0xFFD8F3DC, false);
                graphics.drawString(this.font, Component.translatable("gui.mycelialmind.colony.dialect.source_calls",
                                formatSiteDialectCalls(source)),
                        left + 10, top + 236, 0xFFA5F3FC, false);
                graphics.drawString(this.font, Component.translatable("gui.mycelialmind.colony.dialect.target_calls",
                                formatSiteDialectCalls(target)),
                        left + 10, top + 249, 0xFFA5F3FC, false);

                ColonySiteHabit sourceConflict = dialectConflictHabit(source, habit, dialectCall);
                ColonySiteHabit targetConflict = dialectConflictHabit(target, habit, dialectCall);
                Component statusMsg;
                int statusColor = 0xFFFDE68A;
                if (stage >= 0) {
                    statusMsg = Component.translatable("gui.mycelialmind.colony.dialect.status_lesson_active");
                } else if (source.sitePos().equals(target.sitePos())) {
                    statusMsg = Component.translatable("gui.mycelialmind.colony.dialect.status_same_site");
                } else if (workersList.size() < 2) {
                    statusMsg = Component.translatable("gui.mycelialmind.colony.dialect.status_need_workers");
                } else {
                    var apprenticeSnap = workersList.get(Math.floorMod(dialectWorker, workersList.size()));
                    var teacherSnap = workersList.get(Math.floorMod(dialectTeacher, workersList.size()));
                    if (apprenticeSnap.workerId().equals(teacherSnap.workerId())) {
                        statusMsg = Component.translatable("gui.mycelialmind.colony.dialect.status_same_worker");
                    } else if ((source.episodeMask() & (1 << habit.ordinal())) == 0) {
                        statusMsg = Component.translatable("gui.mycelialmind.colony.dialect.status_no_episode",
                                Component.translatable(habit.translationKey()));
                    } else if (sourceConflict != null) {
                        statusColor = 0xFFFCA5A5;
                        statusMsg = Component.translatable("gui.mycelialmind.colony.dialect.status_conflict",
                                formatPos(source.sitePos()), Component.translatable(sourceConflict.translationKey()),
                                (dialectCall / 3) + 1, (dialectCall % 3) + 1);
                    } else if (targetConflict != null) {
                        statusColor = 0xFFFCA5A5;
                        statusMsg = Component.translatable("gui.mycelialmind.colony.dialect.status_conflict",
                                formatPos(target.sitePos()), Component.translatable(targetConflict.translationKey()),
                                (dialectCall / 3) + 1, (dialectCall % 3) + 1);
                    } else {
                        int knownMask = switch (habit) {
                            case ESCORTED_COURIER -> source.escortCall();
                            case REST_BEFORE_RETURN -> source.restCall();
                            case STAGGERED_PICKUP -> source.pickupCall();
                            default -> 0;
                        };
                        statusColor = 0xFF9BF6B0;
                        statusMsg = Component.translatable(knownMask == 0
                                        ? "gui.mycelialmind.colony.dialect.status_ready_coinage"
                                        : "gui.mycelialmind.colony.dialect.status_ready_relay",
                                Component.translatable(habit.translationKey()),
                                (dialectCall / 3) + 1, (dialectCall % 3) + 1);
                    }
                }
                graphics.drawString(this.font, statusMsg, left + 10, top + 263, statusColor, false);
            }
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
            return;
        } else if (memoryPage) {
            renderMemoryPage(graphics, left, top);
            super.render(graphics, mouseX, mouseY, partialTick);
            graphics.pose().popPose();
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
        graphics.pose().popPose();
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

        List<ClientColonyCache.ReconstructionSnapshot> reconstructions = ClientColonyCache.getReconstructions();
        if (!reconstructions.isEmpty()) {
            reconstructionIndex = Math.floorMod(reconstructionIndex, reconstructions.size());
            ClientColonyCache.ReconstructionSnapshot entry = reconstructions.get(reconstructionIndex);
            graphics.drawString(this.font,
                    Component.translatable("gui.mycelialmind.colony.reconstruction.title",
                            reconstructionIndex + 1, reconstructions.size()),
                    left + 10, top + 215, 0xFFD8B4FE, true);
            List<ClientColonyCache.ReconstructionWitnessSnapshot> witnesses = entry.witnesses();
            if (!witnesses.isEmpty()) {
                reconstructionWitnessIndex = Math.floorMod(reconstructionWitnessIndex, witnesses.size());
                ClientColonyCache.ReconstructionWitnessSnapshot selected = witnesses.get(reconstructionWitnessIndex);
                ColonyWitnessMemory.Type type = ColonyWitnessMemory.Type.fromIndex(selected.type());
                String detail = type == null ? "" : switch (type) {
                    case COURIER_INPUT, COURIER_OUTPUT -> formatPos(selected.memoryPos());
                    case SENTINEL_PATROL -> "Δ " + formatPos(selected.memoryPos());
                    default -> Component.translatable(type.habit().translationKey()).getString();
                };
                graphics.drawString(this.font, this.font.plainSubstrByWidth(detail, PANEL_W - 20),
                        left + 10, top + 312, 0xFFE9ECEF, false);
                graphics.drawString(this.font,
                        Component.translatable("gui.mycelialmind.colony.reconstruction.reason",
                                Component.translatable(statusFromIndex(selected.status()).translationKey())),
                        left + 10, top + 326, 0xFFFFD166, false);
            } else {
                graphics.drawString(this.font,
                        Component.translatable("gui.mycelialmind.colony.reconstruction.no_witnesses"),
                        left + 10, top + 312, 0xFFFFD166, false);
            }
        } else {
            graphics.drawString(this.font, Component.translatable("gui.mycelialmind.colony.reconstruction.none"),
                    left + 10, top + 215, 0xFFADB5BD, false);
        }

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
