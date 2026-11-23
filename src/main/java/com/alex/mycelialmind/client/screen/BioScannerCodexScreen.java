package com.alex.mycelialmind.client.screen;

import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.network.ClientDiplomacyCache;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.network.ClientExpeditionCache;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.network.ClientSymbiosisCache;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.network.PinExpeditionGoalPayload;
import com.alex.mycelialmind.network.PostMatriarchExpeditionSyncPayload;
import com.alex.mycelialmind.network.EnclaveDirectoryRequestPayload;
import com.alex.mycelialmind.network.EnclaveDirectoryPayload;
import com.alex.mycelialmind.network.EnclaveActionPayload;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.SynapticNodeType;
import com.alex.mycelialmind.world.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Interactive Bio-Scanner Codex GUI with 5 localized tabs:
 * (0) Live Stimulus Telemetry, (1) Evolutions & Symbiosis, (2) Territory 2D Radar,
 * (3) Chemical Diplomacy, (4) Field Guide (4 pages including Ongoing Research).
 */
public class BioScannerCodexScreen extends Screen {
    private static final int PANEL_W = 312;
    private static final int PANEL_H = 220;

    private int activeTab = 0;
    private int guidePage = 0;
    private Button guidePageButton;
    private Button pinAdvantageButton;
    private Button pinReclaimButton;
    private Button directoryButton;
    private Button directoryPageButton;
    private boolean directoryOpen;
    private BlockPos selectedEnclave;
    private EditBox enclaveName;
    private Button enclavePinButton;
    private Button enclaveRenameButton;
    private Button abandonButton;
    private boolean abandonConfirmation;

    public BioScannerCodexScreen() {
        super(Component.translatable("screen.mycelialmind.bio_codex"));
    }

    @Override
    protected void init() {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.tab.telemetry"), btn -> setActiveTab(0))
                .bounds(left + 8, top + 22, 56, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.tab.evolutions"), btn -> setActiveTab(1))
                .bounds(left + 66, top + 22, 58, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.tab.territory"), btn -> setActiveTab(2))
                .bounds(left + 126, top + 22, 56, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.tab.diplomacy"), btn -> setActiveTab(3))
                .bounds(left + 184, top + 22, 60, 18).build());
        this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.tab.guide"), btn -> setActiveTab(4))
                .bounds(left + 246, top + 22, 58, 18).build());

        directoryButton = this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.expedition.directory"), btn -> {
            directoryOpen = !directoryOpen;
            if (directoryOpen) EnclaveDirectoryRequestPayload.request(0);
            updateResearchButtonsVisibility();
        }).bounds(left + 76, top + PANEL_H - 22, 100, 16).build());
        directoryPageButton = this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.expedition.next_page"), btn -> {
            var page = ClientExpeditionCache.getDirectory();
            int pages = Math.max(1, (page.total() + EnclaveDirectoryPayload.PAGE_SIZE - 1) / EnclaveDirectoryPayload.PAGE_SIZE);
            selectedEnclave = null;
            EnclaveDirectoryRequestPayload.request((page.page() + 1) % pages);
        }).bounds(left + 182, top + PANEL_H - 22, 120, 16).build());
        abandonButton = this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.expedition.abandon"), btn -> {
            BlockPos target = ClientExpeditionCache.getActivePos();
            if (target == null) return;
            if (!abandonConfirmation) {
                abandonConfirmation = true;
                btn.setMessage(Component.translatable("codex.mycelialmind.expedition.abandon_confirm"));
            } else {
                EnclaveActionPayload.send(EnclaveActionPayload.ABANDON, target, "", 0);
                abandonConfirmation = false;
                btn.setMessage(Component.translatable("codex.mycelialmind.expedition.abandon"));
            }
        }).bounds(left + 182, top + PANEL_H - 22, 120, 16).build());
        enclaveName = this.addRenderableWidget(new EditBox(font, left + 12, top + 171, 140, 16,
                Component.translatable("codex.mycelialmind.expedition.name")));
        enclaveName.setMaxLength(32);
        enclavePinButton = this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.expedition.pin"), btn -> {
            if (selectedEnclave != null) EnclaveActionPayload.send(EnclaveActionPayload.PIN, selectedEnclave, "", ClientExpeditionCache.getDirectory().page());
        }).bounds(left + 157, top + 171, 64, 16).build());
        enclaveRenameButton = this.addRenderableWidget(Button.builder(Component.translatable("codex.mycelialmind.expedition.rename"), btn -> {
            if (selectedEnclave != null) EnclaveActionPayload.send(EnclaveActionPayload.RENAME, selectedEnclave, enclaveName.getValue(), ClientExpeditionCache.getDirectory().page());
        }).bounds(left + 225, top + 171, 75, 16).build());

        this.pinAdvantageButton = this.addRenderableWidget(Button.builder(
                pinButtonLabel(ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE),
                btn -> toggleExpeditionGoal(ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE))
                .bounds(left + 12, top + 176, 138, 16).build());
        this.pinReclaimButton = this.addRenderableWidget(Button.builder(
                pinButtonLabel(ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE),
                btn -> toggleExpeditionGoal(ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE))
                .bounds(left + 158, top + 176, 140, 16).build());

        this.guidePageButton = this.addRenderableWidget(Button.builder(
                guidePageLabel(),
                btn -> {
                    this.guidePage = (this.guidePage + 1) % 5;
                    btn.setMessage(guidePageLabel());
                    updateResearchButtonsVisibility();
                })
                .bounds(left + PANEL_W - 76, top + PANEL_H - 22, 66, 16).build());
        updateResearchButtonsVisibility();

        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && (mc.player.isCreative() || hasCodexBookInInventory(mc.player))) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.mycelialmind.scanner.open_codex"),
                    btn -> ClientCodexOpener.openBookCodex())
                    .bounds(left + 8, top + PANEL_H - 22, 58, 16).build());
        }
    }

    private void toggleExpeditionGoal(ProgressionGoalEngine.ExpeditionGoal target) {
        ProgressionGoalEngine.ExpeditionGoal next = (ClientDiscoveryCache.getPinnedExpeditionGoal() == target)
                ? ProgressionGoalEngine.ExpeditionGoal.NONE
                : target;
        ClientDiscoveryCache.setClientPinnedExpeditionGoal(next.id());
        PinExpeditionGoalPayload.sendToServer(next);
        updateResearchButtonsVisibility();
    }

    private Component pinButtonLabel(ProgressionGoalEngine.ExpeditionGoal goal) {
        boolean selected = ClientDiscoveryCache.getPinnedExpeditionGoal() == goal;
        String prefixKey = selected ? "codex.mycelialmind.research.btn.pinned" : "codex.mycelialmind.research.btn.pin";
        return Component.translatable(prefixKey, Component.translatable(goal.translationKey()));
    }

    private void updateResearchButtonsVisibility() {
        boolean isGuide = this.activeTab == 4;
        if (directoryButton != null) {
            directoryButton.visible = this.activeTab == 2;
            directoryButton.setMessage(Component.translatable(directoryOpen
                    ? "codex.mycelialmind.expedition.radar_button" : "codex.mycelialmind.expedition.directory"));
        }
        if (directoryPageButton != null) directoryPageButton.visible = this.activeTab == 2 && directoryOpen;
        if (abandonButton != null) {
            abandonButton.visible = this.activeTab == 2 && !directoryOpen && ClientExpeditionCache.hasActiveExpedition();
        }
        boolean showDirectory = this.activeTab == 2 && directoryOpen;
        if (enclaveName != null) enclaveName.visible = showDirectory;
        if (enclavePinButton != null) enclavePinButton.visible = showDirectory;
        if (enclaveRenameButton != null) enclaveRenameButton.visible = showDirectory;
        if (this.guidePageButton != null) {
            this.guidePageButton.visible = isGuide;
        }
        boolean isResearchPage = isGuide && this.guidePage == 0;
        boolean canChoose = ClientDiscoveryCache.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_ADAPTATION_IDENTIFIED);
        if (this.pinAdvantageButton != null) {
            this.pinAdvantageButton.visible = isResearchPage;
            this.pinAdvantageButton.active = canChoose;
            this.pinAdvantageButton.setMessage(pinButtonLabel(ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE));
        }
        if (this.pinReclaimButton != null) {
            this.pinReclaimButton.visible = isResearchPage;
            this.pinReclaimButton.active = canChoose;
            this.pinReclaimButton.setMessage(pinButtonLabel(ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE));
        }
    }

    private static boolean hasCodexBookInInventory(net.minecraft.world.entity.player.Player player) {
        var codex = com.alex.mycelialmind.registry.ModItems.MYCELIAL_CODEX.get();
        if (player.getMainHandItem().is(codex) || player.getOffhandItem().is(codex)) {
            return true;
        }
        for (var stack : player.getInventory().items) {
            if (stack.is(codex)) {
                return true;
            }
        }
        return false;
    }

    private void setActiveTab(int tab) {
        this.activeTab = tab;
        updateResearchButtonsVisibility();
    }

    private Component guidePageLabel() {
        return Component.translatable("codex.mycelialmind.guide.page", this.guidePage + 1);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = (this.width - PANEL_W) / 2;
        int top = (this.height - PANEL_H) / 2;

        graphics.fill(left, top, left + PANEL_W, top + PANEL_H, 0xE6140C22);
        graphics.renderOutline(left, top, PANEL_W, PANEL_H, 0xFF4CC9F0);
        graphics.drawCenteredString(this.font, this.title, left + PANEL_W / 2, top + 8, 0xFFE6C2FF);

        super.render(graphics, mouseX, mouseY, partialTick);

        int y = top + 48;
        if (activeTab == 0) {
            renderTelemetryTab(graphics, left + 12, y);
        } else if (activeTab == 1) {
            renderEvolutionsTab(graphics, left + 12, y);
        } else if (activeTab == 2) {
            if (directoryOpen) renderEnclaveDirectory(graphics, left + 12, y);
            else renderTerritoryTab(graphics, left + 12, y, mouseX, mouseY);
        } else if (activeTab == 3) {
            renderDiplomacyTab(graphics, left + 12, y);
        } else {
            renderFieldGuideTab(graphics, left + 12, y);
        }
    }

    private void renderEnclaveDirectory(GuiGraphics graphics, int x, int y) {
        EnclaveDirectoryPayload page = ClientExpeditionCache.getDirectory();
        int pages = Math.max(1, (page.total() + EnclaveDirectoryPayload.PAGE_SIZE - 1) / EnclaveDirectoryPayload.PAGE_SIZE);
        graphics.drawString(font, Component.translatable("codex.mycelialmind.expedition.directory_title",
                page.page() + 1, pages, page.total()), x, y, 0xFF72EFDD, true);
        int row = 0;
        for (var entry : page.entries()) {
            Component name = entry.name().isBlank() ? outcomeComponent(entry.outcome()) : Component.literal(entry.name());
            Component line = Component.translatable("codex.mycelialmind.expedition.directory_row", name,
                    entry.pos().getX(), entry.pos().getY(), entry.pos().getZ());
            int rowY = y + 16 + row * 17;
            if (entry.pos().equals(selectedEnclave)) graphics.fill(x - 2, rowY - 2, x + 287, rowY + 12, 0xAA39524E);
            drawScaledText(graphics, line, x, rowY, 285, 0xFFE6C2FF);
            row++;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeTab == 2 && directoryOpen && button == 0) {
            int left = (width - PANEL_W) / 2;
            int top = (height - PANEL_H) / 2;
            int row = (int) Math.floor((mouseY - (top + 48 + 16)) / 17.0);
            var entries = ClientExpeditionCache.getDirectory().entries();
            if (mouseX >= left + 10 && mouseX < left + 300 && row >= 0 && row < entries.size()
                    && mouseY < top + 48 + 16 + row * 17 + 13) {
                var entry = entries.get(row);
                selectedEnclave = entry.pos();
                enclaveName.setValue(entry.name());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void renderTelemetryTab(GuiGraphics graphics, int x, int y) {
        double threat = ClientHiveCache.getThreatLevel();
        double n1 = ClientHiveCache.getTraitActivationThreshold();
        double n2 = ClientHiveCache.getTier2ActivationThreshold();

        graphics.drawString(this.font,
                Component.translatable("codex.mycelialmind.telemetry.global_threat",
                        String.format("%.1f", threat), ClientHiveCache.getActiveNetworkLinks()),
                x, y, 0xFF72EFDD, true);
        y += 14;

        String n2Str = String.format("%.0f", n2);
        for (StimulusType type : StimulusType.values()) {
            double score = ClientHiveCache.getStimulusScore(type);
            graphics.drawString(this.font, MycelialText.stimulus(type), x, y, 0xFFE6C2FF, true);
            graphics.drawString(this.font, String.format("%.1f / %s", score, n2Str),
                    x + 92, y, 0xFFADB5BD, true);
            int barX = x + 152;
            int barW = 132;
            graphics.fill(barX, y + 1, barX + barW, y + 8, 0xFF2B193D);
            int fill = (int) Math.round(barW * Math.min(1.0, score / n2));
            int col = score >= n2 ? 0xFFFF4D8D : (score >= n1 ? 0xFFFFB703 : 0xFF64DFDF);
            graphics.fill(barX, y + 1, barX + fill, y + 8, col);
            int n1TickX = barX + (int) Math.round(barW * Math.min(1.0, n1 / n2));
            graphics.fill(n1TickX, y, n1TickX + 1, y + 9, 0xCCFFFFFF);
            y += 14;
        }
        graphics.drawString(this.font,
                Component.translatable("codex.mycelialmind.telemetry.thresholds", String.format("%.0f", n1), n2Str),
                x, y + 2, 0xFF9DE0AD, true);
        y += 13;

        double med = ClientHiveCache.getHabitScore(PlayerHabit.COMBAT_MEDIC);
        double mob = ClientHiveCache.getHabitScore(PlayerHabit.HIGH_MOBILITY);
        double bas = ClientHiveCache.getHabitScore(PlayerHabit.BASTION_BUILDER);
        double shd = ClientHiveCache.getHabitScore(PlayerHabit.SHADOW_INFILTRATOR);
        double habitThreshold = HiveMindSavedData.HABIT_ACTIVATION_THRESHOLD;
        boolean anyHabitActive = med >= habitThreshold || mob >= habitThreshold || bas >= habitThreshold || shd >= habitThreshold;
        graphics.drawString(this.font,
                Component.translatable("codex.mycelialmind.telemetry.habits_line",
                        String.format("%.0f", med), String.format("%.0f", mob),
                        String.format("%.0f", bas), String.format("%.0f", shd)),
                x, y, anyHabitActive ? 0xFFFFD166 : 0xFFE6C2FF, true);
        y += 12;

        StimulusType falseType = ClientHiveCache.getFalseStimulus();
        int decSec = ClientHiveCache.getDeceptionSecondsRemaining();
        int decCd = ClientHiveCache.getDeceptionCooldownSecondsRemaining();
        String bufStr = String.format("%.1f", ClientHiveCache.getNearestVolatileBuffer());
        Component decText = (falseType != null && decSec > 0)
                ? Component.translatable("codex.mycelialmind.telemetry.deception_active", MycelialText.stimulus(falseType), decSec, bufStr)
                : (decCd > 0
                ? Component.translatable("codex.mycelialmind.telemetry.deception_cd", decCd, bufStr)
                : Component.translatable("codex.mycelialmind.telemetry.deception_ready", bufStr));
        graphics.drawString(this.font, decText, x, y, decSec > 0 ? 0xFF38B000 : 0xFF72EFDD, true);
        y += 12;

        int tideSec = ClientHiveCache.getFungalTideSecondsRemaining();
        Component tideText = tideSec > 0
                ? Component.translatable("codex.mycelialmind.telemetry.tide_active", tideSec / 60, tideSec % 60)
                : Component.translatable("codex.mycelialmind.telemetry.tide_dormant");
        graphics.drawString(this.font, tideText, x, y, tideSec > 0 ? 0xFFFF4D8D : 0xFF8D99AE, true);
        y += 12;

        HiveProgressionTier stage = ClientDiscoveryCache.getEffectiveActTier();
        int remissionSec = ClientHiveCache.getMatriarchRemissionSecondsRemaining();
        Component nextGoal;
        if (remissionSec > 0) {
            nextGoal = Component.translatable("codex.mycelialmind.telemetry.goal.remission",
                    remissionSec / 60, remissionSec % 60);
        } else if (ClientHiveCache.isMatriarchDefeated()) {
            nextGoal = Component.translatable("codex.mycelialmind.telemetry.goal.matriarch_defeated");
        } else {
            nextGoal = switch (stage) {
                case DORMANT -> Component.translatable("codex.mycelialmind.telemetry.goal.dormant");
                case AWAKENED -> Component.translatable("codex.mycelialmind.telemetry.goal.awakened");
                case ADAPTIVE -> Component.translatable(ClientHiveCache.isDeepMyceliumBreached()
                        ? "codex.mycelialmind.telemetry.goal.adaptive_breached"
                        : "codex.mycelialmind.telemetry.goal.adaptive");
                case OVERMIND -> Component.translatable("codex.mycelialmind.telemetry.goal.overmind");
            };
        }
        graphics.drawString(this.font,
                Component.translatable("codex.mycelialmind.telemetry.stage", MycelialText.progressionTier(stage), nextGoal),
                x, y, 0xFFFFB703, true);
        y += 12;

        var homeoStage = ClientHiveCache.getHomeostasisStage();
        graphics.drawString(this.font,
                Component.translatable("codex.mycelialmind.telemetry.homeostasis",
                        String.format("%.0f", ClientHiveCache.getHomeostasisStress()),
                        MycelialText.homeostasisStage(homeoStage),
                        ClientHiveCache.getHomeostasisSensitization(),
                        ClientHiveCache.getSmotheredPylonCount()),
                x, y, homeoStage.argbColor(), true);
    }

    private void renderEvolutionsTab(GuiGraphics graphics, int x, int y) {
        int startY = y;
        Set<AdaptationTrait> t1 = ClientHiveCache.getActiveTraits();
        Set<AdaptationTrait> t2 = ClientHiveCache.getTier2Traits();
        Set<HiveSynergy> syn = ClientHiveCache.getActiveSynergies();

        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.evolutions.active_traits"), x, y, 0xFFFFB703, true);
        y += 12;
        if (t1.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("codex.mycelialmind.evolutions.none"), x, y, 0xFF8D99AE, true);
            y += 11;
        } else {
            for (AdaptationTrait trait : t1) {
                Component txt = t2.contains(trait)
                        ? Component.literal("  • ").append(MycelialText.trait(trait)).append(" [N2: ").append(MycelialText.traitTier2(trait)).append("]")
                        : Component.literal("  • ").append(MycelialText.trait(trait));
                graphics.drawString(this.font, txt, x, y, t2.contains(trait) ? 0xFFFF4D8D : 0xFFFFE599, true);
                y += 11;
            }
        }

        y += 4;
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.evolutions.active_synergies"), x, y, 0xFFFF6B6B, true);
        y += 12;
        if (syn.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("codex.mycelialmind.evolutions.none"), x, y, 0xFF8D99AE, true);
        } else {
            for (HiveSynergy s : syn) {
                graphics.drawString(this.font, Component.literal("  * ").append(MycelialText.synergy(s)), x, y, 0xFFFF9F1C, true);
                y += 11;
            }
        }

        int rx = x + 150;
        int ry = startY + 76;
        Set<SymbioticMutagen> mutagens = ClientSymbiosisCache.getEquippedMutagens();
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.evolutions.symbiosis", mutagens.size()), rx, ry, 0xFF72EFDD, true);
        ry += 11;
        if (mutagens.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("codex.mycelialmind.evolutions.none"), rx, ry, 0xFF8D99AE, true);
            ry += 10;
        } else {
            for (SymbioticMutagen m : mutagens) {
                graphics.drawString(this.font, Component.literal("  + ").append(MycelialText.mutagen(m)), rx, ry, 0xFFA3F7B5, true);
                ry += 10;
            }
        }

        ry += 4;
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.evolutions.counter_habits"), rx, ry, 0xFFFFD166, true);
        ry += 11;
        boolean anyHabit = false;
        for (PlayerHabit habit : PlayerHabit.values()) {
            if (ClientHiveCache.getHabitScore(habit) >= HiveMindSavedData.HABIT_ACTIVATION_THRESHOLD) {
                graphics.drawString(this.font, Component.literal("  • ").append(MycelialText.habit(habit)), rx, ry, 0xFFFFE599, true);
                ry += 10;
                anyHabit = true;
            }
        }
        if (!anyHabit) {
            graphics.drawString(this.font, Component.translatable("codex.mycelialmind.evolutions.none"), rx, ry, 0xFF8D99AE, true);
        }
    }

    private void renderTerritoryTab(GuiGraphics graphics, int x, int y, int mouseX, int mouseY) {
        int radarSize = 140;
        int cx = x + radarSize / 2;
        int cy = y + radarSize / 2;
        double scale = 66.0 / 128.0;

        graphics.fill(x, y, x + radarSize, y + radarSize, 0xFF0C0714);
        graphics.renderOutline(x, y, radarSize, radarSize, 0xFF3C2A4D);
        graphics.enableScissor(x + 1, y + 1, x + radarSize - 1, y + radarSize - 1);

        BlockPos anchor = ClientHiveCache.getPlayerAnchorPos();
        Component hoveredTooltip = null;
        for (var ic : ClientHiveCache.getInflamedChunks()) {
            int minX = cx + (int) Math.round(((ic.chunkX() << 4) - anchor.getX()) * scale);
            int minY = cy + (int) Math.round(((ic.chunkZ() << 4) - anchor.getZ()) * scale);
            int maxX = cx + (int) Math.round((((ic.chunkX() + 1) << 4) - anchor.getX()) * scale);
            int maxY = cy + (int) Math.round((((ic.chunkZ() + 1) << 4) - anchor.getZ()) * scale);
            if (maxX < x || minX > x + radarSize || maxY < y || minY > y + radarSize) {
                continue;
            }
            var st = com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.byId(ic.stageId());
            int heatCol = st == com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE
                    ? 0x55FF3366
                    : (st == com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage.IRRITATED
                    ? 0x44FFB703
                    : 0x3372EFDD);
            graphics.fill(minX, minY, maxX, maxY, heatCol);
            if (ic.sensitization() > 0) {
                graphics.renderOutline(minX, minY, Math.max(2, maxX - minX), Math.max(2, maxY - minY), 0x88FF758F);
            }
            if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
                hoveredTooltip = Component.translatable("codex.mycelialmind.territory.hover_chunk",
                        ic.chunkX(), ic.chunkZ(), ic.stressPercent(), MycelialText.homeostasisStage(st), ic.sensitization());
            }
        }

        drawCircleOutline(graphics, cx, cy, (int) Math.round(64 * scale), 0x445A4078);
        drawCircleOutline(graphics, cx, cy, (int) Math.round(128 * scale), 0x445A4078);

        List<HiveSyncPayload.NodeMarker> nodes = ClientHiveCache.getNearbyNodes();
        List<BlockPos> pylons = ClientHiveCache.getNearbyPylons();
        int pylonRad = ClientHiveCache.getPylonRadius();
        int linkRad = ClientHiveCache.getLinkRadius();
        double linkSq = (double) linkRad * linkRad;

        for (int i = 0; i < nodes.size(); i++) {
            BlockPos a = nodes.get(i).pos();
            for (int j = i + 1; j < nodes.size(); j++) {
                BlockPos b = nodes.get(j).pos();
                if (a.distSqr(b) <= linkSq && !isBlockedByNearbyPylon(a, b, pylons, pylonRad)) {
                    int ax = cx + (int) Math.round((a.getX() - anchor.getX()) * scale);
                    int ay = cy + (int) Math.round((a.getZ() - anchor.getZ()) * scale);
                    int bx = cx + (int) Math.round((b.getX() - anchor.getX()) * scale);
                    int by = cy + (int) Math.round((b.getZ() - anchor.getZ()) * scale);
                    if (inBox(ax, ay, x, y, radarSize) && inBox(bx, by, x, y, radarSize)) {
                        drawLine(graphics, ax, ay, bx, by, 0x999D4EDD);
                    }
                }
            }
        }

        int rPx = Math.max(4, (int) Math.round(pylonRad * scale));
        for (BlockPos p : pylons) {
            int px = cx + (int) Math.round((p.getX() - anchor.getX()) * scale);
            int py = cy + (int) Math.round((p.getZ() - anchor.getZ()) * scale);
            if (!inBox(px, py, x, y, radarSize)) {
                continue;
            }
            boolean smothered = ClientHiveCache.isPylonSmothered(p);
            if (smothered) {
                graphics.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFF3366);
            } else {
                drawCircleOutline(graphics, px, py, rPx, 0x664CC9F0);
                graphics.fill(px - 2, py - 2, px + 3, py + 3, 0xFF4CC9F0);
            }
            if (Math.abs(mouseX - px) <= 4 && Math.abs(mouseY - py) <= 4) {
                hoveredTooltip = Component.translatable(
                        smothered ? "codex.mycelialmind.territory.hover_pylon_smothered" : "codex.mycelialmind.territory.hover_pylon",
                        p.getX(), p.getY(), p.getZ(), (int) Math.round(Math.sqrt(p.distSqr(anchor))));
            }
        }

        for (HiveSyncPayload.NodeMarker node : nodes) {
            BlockPos p = node.pos();
            int nx = cx + (int) Math.round((p.getX() - anchor.getX()) * scale);
            int ny = cy + (int) Math.round((p.getZ() - anchor.getZ()) * scale);
            if (!inBox(nx, ny, x, y, radarSize)) {
                continue;
            }
            int col = switch (node.type()) {
                case INCUBATOR -> 0xFFFF4D8D;
                case SENTINEL -> 0xFFFFB703;
                case SYMBIOTIC -> 0xFF38B000;
                default -> 0xFFB5179E;
            };
            graphics.fill(nx - 2, ny - 2, nx + 3, ny + 3, col);
            if (Math.abs(mouseX - nx) <= 4 && Math.abs(mouseY - ny) <= 4) {
                hoveredTooltip = Component.translatable("codex.mycelialmind.territory.hover_node",
                        MycelialText.nodeType(node.type()), p.getX(), p.getY(), p.getZ(),
                        (int) Math.round(Math.sqrt(p.distSqr(anchor))));
            }
        }

        Minecraft mc = Minecraft.getInstance();
        boolean viewingDeepMycelium = mc.level != null && mc.level.dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);
        if (viewingDeepMycelium) {
            for (PostMatriarchExpeditionSyncPayload.Marker marker : ClientExpeditionCache.getEnclaves()) {
                int mx = cx + (int) Math.round((marker.pos().getX() - anchor.getX()) * scale);
                int my = cy + (int) Math.round((marker.pos().getZ() - anchor.getZ()) * scale);
                if (!inBox(mx, my, x, y, radarSize)) {
                    continue;
                }
                if (marker.outcomeId() == 1 && marker.protectedZone()) {
                    graphics.fill(mx - 3, my - 3, mx + 4, my + 4, 0xFF4CC9F0);
                    graphics.renderOutline(mx - 4, my - 4, 9, 9, 0xFFFFFFFF);
                } else if (marker.outcomeId() == 1) {
                    graphics.fill(mx - 4, my - 1, mx + 5, my + 2, 0xFFFF6B6B);
                    graphics.fill(mx - 1, my - 4, mx + 2, my + 5, 0xFFFF6B6B);
                } else {
                    int color = ReclaimedBloomBlock.colorForPalette(marker.palette());
                    graphics.fill(mx - 1, my - 4, mx + 2, my + 5, color);
                    graphics.fill(mx - 4, my - 1, mx + 5, my + 2, color);
                    graphics.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFFFFFFFF);
                }
                if (Math.abs(mouseX - mx) <= 5 && Math.abs(mouseY - my) <= 5) {
                    hoveredTooltip = marker.outcomeId() == 1
                            ? Component.translatable(marker.protectedZone()
                                    ? "codex.mycelialmind.expedition.marker.route_protected"
                                    : "codex.mycelialmind.expedition.marker.route_unprotected")
                            : Component.translatable("codex.mycelialmind.expedition.marker.garden",
                                    paletteComponent(marker.palette()));
                }
            }
            BlockPos activePos = ClientExpeditionCache.getActivePos();
            if (activePos != null) {
                int ax = cx + (int) Math.round((activePos.getX() - anchor.getX()) * scale);
                int ay = cy + (int) Math.round((activePos.getZ() - anchor.getZ()) * scale);
                if (inBox(ax, ay, x, y, radarSize)) {
                    graphics.renderOutline(ax - 5, ay - 5, 11, 11, 0xFFFFD166);
                    if (Math.abs(mouseX - ax) <= 6 && Math.abs(mouseY - ay) <= 6) {
                        hoveredTooltip = Component.translatable("codex.mycelialmind.expedition.active",
                                outcomeComponent(ClientExpeditionCache.getOutcomeId()),
                                activePos.getX(), activePos.getY(), activePos.getZ());
                    }
                }
            }
        }

        graphics.fill(cx - 2, cy, cx + 3, cy + 1, 0xFFFFFFFF);
        graphics.fill(cx, cy - 2, cx + 1, cy + 3, 0xFFFFFFFF);
        graphics.disableScissor();

        int rx = x + radarSize + 10;
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.territory.radar"), rx, y, 0xFF72EFDD, true);
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.territory.total_nodes", ClientHiveCache.getTotalNodes()),
                rx, y + 14, 0xFFE6C2FF, true);
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.territory.total_pylons", ClientHiveCache.getTotalPylons()),
                rx, y + 26, 0xFF4CC9F0, true);
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.territory.active_links", ClientHiveCache.getActiveNetworkLinks()),
                rx, y + 38, 0xFF9DE0AD, true);
        Component zoneLabel = Component.translatable(ClientHiveCache.isInProtectedZone()
                ? "codex.mycelialmind.territory.protected"
                : "codex.mycelialmind.territory.exposed");
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.territory.zone", zoneLabel),
                rx, y + 50, ClientHiveCache.isInProtectedZone() ? 0xFF4CC9F0 : 0xFFFF4D8D, true);

        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.territory.legend"), rx, y + 61, 0xFFFFE599, true);
        String[] legendKeys = {"relay", "incubator", "sentinel", "pylon", "symbiotic", "inflamed", "smothered"};
        int[] legendColors = {0xFFB5179E, 0xFFFF4D8D, 0xFFFFB703, 0xFF4CC9F0, 0xFF38B000, 0xAAFF3366, 0xFFFF3366};
        for (int i = 0; i < legendKeys.length; i++) {
            int lx = rx + (i / 4) * 75;
            int ly = y + 72 + (i % 4) * 9;
            graphics.fill(lx, ly + 2, lx + 5, ly + 7, legendColors[i]);
            drawScaledText(graphics, Component.translatable("codex.mycelialmind.territory.legend." + legendKeys[i]),
                    lx + 8, ly, 65, 0xFFE6C2FF);
        }

        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.expedition.header"),
                rx, y + 110, 0xFFFFD166, true);
        if (!ClientExpeditionCache.isUnlocked()) {
            drawScaledText(graphics, Component.translatable("codex.mycelialmind.expedition.locked"),
                    rx, y + 120, 150, 0xFFADB5BD);
        } else if (!ClientExpeditionCache.hasActiveExpedition()) {
            drawScaledText(graphics, Component.translatable("codex.mycelialmind.expedition.idle"),
                    rx, y + 120, 150, 0xFFADB5BD);
        } else {
            BlockPos expeditionPos = ClientExpeditionCache.getActivePos();
            drawScaledText(graphics, Component.translatable("codex.mycelialmind.expedition.active",
                    outcomeComponent(ClientExpeditionCache.getOutcomeId()),
                    expeditionPos.getX(), expeditionPos.getY(), expeditionPos.getZ()),
                    rx, y + 120, 150, 0xFFE6C2FF);
            Component progress = ClientExpeditionCache.getOutcomeId() == 1
                    ? Component.translatable("codex.mycelialmind.expedition.route_progress",
                            ClientExpeditionCache.getProgress(), 3)
                    : Component.translatable("codex.mycelialmind.expedition.garden_progress",
                            ClientExpeditionCache.getLinkedFlora(), 3,
                            paletteComponent(ClientExpeditionCache.getPalette()));
            drawScaledText(graphics, progress, rx, y + 130, 150, 0xFF9DE0AD);
        }
        if (ClientExpeditionCache.hasActiveExpedition()) {
            int flags = ClientExpeditionCache.getSiteFlags();
            var conditions = Component.empty();
            if ((flags & PostMatriarchExpeditionSyncPayload.TIDE) != 0) conditions.append(Component.translatable("codex.mycelialmind.expedition.condition.tide"));
            if ((flags & PostMatriarchExpeditionSyncPayload.ACID) != 0) conditions.append(Component.translatable("codex.mycelialmind.expedition.condition.acid"));
            if ((flags & PostMatriarchExpeditionSyncPayload.PYLON_SMOTHERED) != 0) conditions.append(Component.translatable("codex.mycelialmind.expedition.condition.smothered"));
            if ((flags & PostMatriarchExpeditionSyncPayload.PYLON_MISSING) != 0) conditions.append(Component.translatable("codex.mycelialmind.expedition.condition.missing"));
            if ((flags & PostMatriarchExpeditionSyncPayload.SITE_UNLOADED) != 0) conditions.append(Component.translatable("codex.mycelialmind.expedition.condition.unloaded"));
            drawScaledText(graphics, Component.translatable("codex.mycelialmind.expedition.site_conditions",
                    ClientExpeditionCache.getSiteStress(), conditions), rx, y + 140, 150, 0xFFFFD166);
        } else {
            drawScaledText(graphics, Component.translatable("codex.mycelialmind.expedition.completed_count",
                    ClientExpeditionCache.getEnclaves().size()), rx, y + 140, 150, 0xFF72EFDD);
        }

        if (hoveredTooltip != null) {
            graphics.renderTooltip(this.font, hoveredTooltip, mouseX, mouseY);
        }
    }

    private static Component outcomeComponent(int outcomeId) {
        return Component.translatable(outcomeId == 2
                ? "codex.mycelialmind.expedition.outcome.garden"
                : "codex.mycelialmind.expedition.outcome.route");
    }

    private static Component paletteComponent(int palette) {
        String suffix = switch (palette) {
            case 1 -> "melee";
            case 2 -> "projectile";
            case 3 -> "fire";
            case 4 -> "explosion";
            case 5 -> "magic";
            default -> "base";
        };
        return Component.translatable("codex.mycelialmind.expedition.palette." + suffix);
    }

    private static boolean inBox(int px, int py, int x, int y, int size) {
        return px >= x + 2 && px <= x + size - 3 && py >= y + 2 && py <= y + size - 3;
    }

    private static void drawCircleOutline(GuiGraphics g, int cx, int cy, int r, int color) {
        int steps = Math.max(24, r * 2);
        for (int i = 0; i < steps; i++) {
            double ang = (2.0 * Math.PI * i) / steps;
            int px = cx + (int) Math.round(Math.cos(ang) * r);
            int py = cy + (int) Math.round(Math.sin(ang) * r);
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            g.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    private static boolean isBlockedByNearbyPylon(BlockPos a, BlockPos b, List<BlockPos> pylons, int pylonRad) {
        double rSq = (double) pylonRad * pylonRad;
        double vx = b.getX() - a.getX();
        double vy = b.getY() - a.getY();
        double vz = b.getZ() - a.getZ();
        double lenSq = vx * vx + vy * vy + vz * vz;
        for (BlockPos p : pylons) {
            if (ClientHiveCache.isPylonSmothered(p)) {
                continue;
            }
            if (p.distSqr(a) <= rSq || p.distSqr(b) <= rSq) {
                return true;
            }
            if (lenSq > 0.0001) {
                double t = ((p.getX() - a.getX()) * vx + (p.getY() - a.getY()) * vy + (p.getZ() - a.getZ()) * vz) / lenSq;
                t = Math.max(0.0, Math.min(1.0, t));
                double cx = a.getX() + t * vx - p.getX();
                double cy = a.getY() + t * vy - p.getY();
                double cz = a.getZ() + t * vz - p.getZ();
                if (cx * cx + cy * cy + cz * cz <= rSq) {
                    return true;
                }
            }
        }
        return false;
    }

    private void renderDiplomacyTab(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.diplomacy.header"), x, y, 0xFFFFD166, true);
        y += 14;
        HiveStrain selected = ClientDiplomacyCache.getSelectedStrain();
        graphics.drawString(this.font,
                Component.translatable("codex.mycelialmind.diplomacy.focus_strain", MycelialText.strain(selected)),
                x, y, selected.argbColor(), true);
        y += 16;

        for (HiveStrain strain : new HiveStrain[]{HiveStrain.AMBER, HiveStrain.CRIMSON}) {
            int rep = strain == HiveStrain.AMBER ? ClientDiplomacyCache.getAmberRep() : ClientDiplomacyCache.getCrimsonRep();
            PlayerDiplomacyData.Standing standing = ClientDiplomacyCache.getStanding(strain);
            Component rowLabel = Component.literal("")
                    .append(MycelialText.strain(strain))
                    .append(String.format(" %+d [", rep))
                    .append(MycelialText.standing(standing))
                    .append("]");
            graphics.drawString(this.font, rowLabel, x, y, standing.color(), true);
            int barX = x + 152;
            int barW = 130;
            graphics.fill(barX, y + 1, barX + barW, y + 9, 0xFF2B193D);
            double norm = (rep + 100.0) / 200.0;
            int fill = (int) Math.round(barW * Math.max(0.0, Math.min(1.0, norm)));
            graphics.fill(barX, y + 1, barX + fill, y + 9, strain.argbColor());
            y += 18;
        }

        y += 6;
        int truceSec = ClientDiplomacyCache.getTruceSeconds();
        int betrayalSec = ClientDiplomacyCache.getBetrayalSeconds();
        graphics.drawString(this.font,
                truceSec > 0
                        ? Component.translatable("codex.mycelialmind.diplomacy.truce_active", truceSec)
                        : Component.translatable("codex.mycelialmind.diplomacy.truce_inactive"),
                x, y, truceSec > 0 ? 0xFF4CC9F0 : 0xFF8D99AE, true);
        y += 13;
        graphics.drawString(this.font,
                betrayalSec > 0
                        ? Component.translatable("codex.mycelialmind.diplomacy.betrayal_active", betrayalSec)
                        : Component.translatable("codex.mycelialmind.diplomacy.betrayal_none"),
                x, y, betrayalSec > 0 ? 0xFFFF595E : 0xFF9DE0AD, true);
        y += 16;
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.diplomacy.rule1"), x, y, 0xFFE6C2FF, true);
        y += 12;
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.diplomacy.rule2"), x, y, 0xFFFFB703, true);
        y += 12;
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.diplomacy.rule3"), x, y, 0xFF72EFDD, true);
    }

    private void renderFieldGuideTab(GuiGraphics graphics, int x, int y) {
        if (guidePage == 4) {
            graphics.drawString(font, Component.translatable("codex.mycelialmind.expedition.guide.title"), x, y, 0xFFFFD166, true);
            for (int i = 1; i <= 8; i++) {
                drawScaledText(graphics, Component.translatable("codex.mycelialmind.expedition.guide." + i),
                        x, y + 15 + (i - 1) * 15, 285, 0xFFE6C2FF);
            }
            return;
        }
        if (guidePage == 0) {
            renderOngoingResearchPage(graphics, x, y);
            return;
        }
        if (guidePage == 3) {
            renderProgressionGoalsPage(graphics, x, y);
            return;
        }
        boolean isPage1 = (guidePage == 1);
        Component header = Component.translatable(isPage1
                ? "codex.mycelialmind.guide.header1"
                : "codex.mycelialmind.guide.header2");
        graphics.drawString(this.font, header, x, y, isPage1 ? 0xFFFFD166 : 0xFF72EFDD, true);
        y += 13;

        int count = isPage1 ? 10 : 12;
        String prefix = isPage1 ? "codex.mycelialmind.guide.p1." : "codex.mycelialmind.guide.p2.";
        int lineStep = count > 11 ? 10 : 12;
        for (int i = 1; i <= count; i++) {
            graphics.drawString(this.font, Component.translatable(prefix + i), x, y, 0xFFE6C2FF, true);
            y += lineStep;
        }
    }

    private void renderOngoingResearchPage(GuiGraphics graphics, int x, int y) {
        updateResearchButtonsVisibility();
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.guide.header0"), x, y, 0xFFFFD166, true);
        y += 12;

        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;

        ProgressionGoalEngine.CreatureObservation obs = ClientDiscoveryCache.getLastObservation();
        Component obsText = obs == ProgressionGoalEngine.CreatureObservation.OTHER_DOMINANT_RESPONSE
                ? Component.translatable("codex.mycelialmind.research.obs.other_dominant")
                : Component.translatable(obs.translationKey());
        Component dimText = ClientDiscoveryCache.formatDimensionComponent(ClientDiscoveryCache.getLastObservationDimension());
        drawScaledText(graphics,
                Component.translatable("codex.mycelialmind.research.last_discovery", obsText, dimText),
                x, y, 286, 0xFFE6C2FF);
        y += 11;

        ProgressionGoalEngine.ExpeditionGoal pinnedGoal = ClientDiscoveryCache.getPinnedExpeditionGoal();
        drawScaledText(graphics,
                Component.translatable("codex.mycelialmind.research.pinned_goal",
                        Component.translatable(pinnedGoal.translationKey())),
                x, y, 286, pinnedGoal == ProgressionGoalEngine.ExpeditionGoal.NONE ? 0xFFADB5BD : 0xFFFFD166);
        y += 11;

        if (pinnedGoal != ProgressionGoalEngine.ExpeditionGoal.NONE) {
            String meritKey = pinnedGoal == ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE
                    ? "codex.mycelialmind.research.merit.use_to_advantage"
                    : "codex.mycelialmind.research.merit.reclaim_cave";
            drawScaledText(graphics, Component.translatable(meritKey), x, y, 286, 0xFF72EFDD);
            y += 11;
        }

        ProgressionGoalEngine.ResearchNextStep step = ClientDiscoveryCache.resolveClientResearchNextStep(player);
        Component stepComp = step == ProgressionGoalEngine.ResearchNextStep.COMPLETED_NODE_DESTRUCTION
                ? Component.translatable(step.translationKey(), String.format("%.1f", ClientDiscoveryCache.getLastThreatDrop()))
                : Component.translatable(step.translationKey());
        int stepColor = step.isWarning()
                ? 0xFFFF758F
                : ((step == ProgressionGoalEngine.ResearchNextStep.COMPLETED_BIOTECH_SYNTHESIS
                || step == ProgressionGoalEngine.ResearchNextStep.COMPLETED_NODE_DESTRUCTION)
                ? 0xFF72EFDD : 0xFF9DE0AD);
        List<FormattedCharSequence> stepLines = this.font.split(
                Component.translatable("codex.mycelialmind.research.next_step_prefix", stepComp), 286);
        for (int i = 0; i < stepLines.size() && i < 2; i++) {
            graphics.drawString(this.font, stepLines.get(i), x, y, stepColor, true);
            y += 10;
        }
        y += 2;

        boolean hasSample = ClientDiscoveryCache.hasProjectileDeflectSample(player)
                || ClientDiscoveryCache.hasResearchBit(ProgressionGoalEngine.RESEARCH_BIT_SAMPLE_EXTRACTED);
        if (pinnedGoal == ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE || hasSample) {
            graphics.drawString(this.font,
                    Component.translatable("codex.mycelialmind.research.sample_syntheses_header"),
                    x, y, 0xFFFFE599, true);
            y += 11;

            boolean hasBiomass = ClientDiscoveryCache.hasItemInInventory(player, ModItems.MYCELIAL_BIOMASS.get());
            boolean hasCore = ClientDiscoveryCache.hasItemInInventory(player, ModItems.SYNAPTIC_CORE.get());
            boolean hasSpore = ClientDiscoveryCache.hasItemInInventory(player, ModItems.LATENT_SPORE.get());
            boolean hasCounterCat = ClientDiscoveryCache.hasItemInInventory(player, Items.AMETHYST_SHARD)
                    || ClientDiscoveryCache.hasItemInInventory(player, ModItems.PURIFIER_GRENADE.get());
            boolean hasEcho = ClientDiscoveryCache.hasItemInInventory(player, ModItems.NEURAL_ECHO_CORE.get());

            drawRecipeAvailabilityRow(graphics, x, y,
                    "codex.mycelialmind.research.recipe_name.mutagen",
                    hasSample, hasBiomass, "codex.mycelialmind.research.ing.biomass",
                    hasCore, "codex.mycelialmind.research.ing.synaptic_core");
            y += 10;
            drawRecipeAvailabilityRow(graphics, x, y,
                    "codex.mycelialmind.research.recipe_name.counter",
                    hasSample, hasSpore, "codex.mycelialmind.research.ing.latent_spore",
                    hasCounterCat, "codex.mycelialmind.research.ing.amethyst_or_grenade");
            y += 10;
            drawRecipeAvailabilityRow(graphics, x, y,
                    "codex.mycelialmind.research.recipe_name.chimera",
                    hasSample, hasBiomass, "codex.mycelialmind.research.ing.biomass",
                    hasEcho, "codex.mycelialmind.research.ing.echo_core");
        } else if (pinnedGoal == ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE) {
            drawScaledText(graphics,
                    Component.translatable("codex.mycelialmind.research.cave_telemetry",
                            String.format("%.1f", ClientDiscoveryCache.getRecordedThreat()),
                            String.format("%.1f", ClientHiveCache.getThreatLevel()),
                            String.format("%.1f", ClientDiscoveryCache.getLastThreatDrop())),
                    x, y, 286, 0xFFE6C2FF);
            y += 12;
            drawScaledText(graphics,
                    Component.translatable("codex.mycelialmind.research.cave_tip"),
                    x, y, 286, 0xFFADB5BD);
        } else {
            drawScaledText(graphics,
                    Component.translatable("codex.mycelialmind.research.option_advantage_summary"),
                    x, y, 286, 0xFFA3F7B5);
            y += 11;
            drawScaledText(graphics,
                    Component.translatable("codex.mycelialmind.research.option_reclaim_summary"),
                    x, y, 286, 0xFF72EFDD);
        }
    }

    private void drawRecipeAvailabilityRow(
            GuiGraphics graphics,
            int x,
            int y,
            String recipeNameKey,
            boolean hasSample,
            boolean hasReagent1,
            String reagent1Key,
            boolean hasReagent2,
            String reagent2Key
    ) {
        List<Component> missing = new ArrayList<>();
        if (!hasSample) {
            missing.add(Component.translatable("codex.mycelialmind.research.ing.sample"));
        }
        if (!hasReagent1) {
            missing.add(Component.translatable(reagent1Key));
        }
        if (!hasReagent2) {
            missing.add(Component.translatable(reagent2Key));
        }
        Component status;
        int color;
        if (missing.isEmpty()) {
            status = Component.translatable("codex.mycelialmind.research.recipe_ready",
                    Component.translatable(recipeNameKey));
            color = 0xFF9DE0AD;
        } else {
            var joined = Component.empty();
            for (int i = 0; i < missing.size(); i++) {
                if (i > 0) {
                    joined.append(", ");
                }
                joined.append(missing.get(i));
            }
            status = Component.translatable("codex.mycelialmind.research.recipe_missing",
                    Component.translatable(recipeNameKey), joined);
            color = 0xFFFFB703;
        }
        drawScaledText(graphics, status, x, y, 286, color);
    }

    private void renderProgressionGoalsPage(GuiGraphics graphics, int x, int y) {
        graphics.drawString(this.font, Component.translatable("codex.mycelialmind.guide.header3"), x, y, 0xFFFFD166, true);
        y += 12;

        int craftedMask = ClientDiscoveryCache.getCraftedMilestonesMask();
        int traitsMask = ClientDiscoveryCache.getDiscoveredTraitsMask();
        int secretsMask = ClientDiscoveryCache.getDiscoveredSecretsMask();
        var shortGoal = ProgressionGoalEngine.resolveShortTermGoal(craftedMask, traitsMask, secretsMask);
        int shortPct = ProgressionGoalEngine.computeShortTermProgressPercent(shortGoal, craftedMask, traitsMask, secretsMask);

        HiveProgressionTier tier = ClientDiscoveryCache.getEffectiveActTier();
        boolean hiveCoreDefeated = ProgressionGoalEngine.hasMilestone(
                craftedMask, ProgressionGoalEngine.MILESTONE_HIVE_CORE_DEFEATED);
        var midGoal = ProgressionGoalEngine.resolveMidTermGoal(
                tier, ClientHiveCache.getTotalPylons(), hiveCoreDefeated,
                ClientHiveCache.isDeepMyceliumBreached(), ClientHiveCache.isMatriarchDefeated());
        int midPct = ProgressionGoalEngine.computeMidTermProgressPercent(
                midGoal, tier, ClientHiveCache.getTotalPylons(), hiveCoreDefeated,
                ClientHiveCache.isDeepMyceliumBreached(), ClientHiveCache.isMatriarchDefeated());

        int longPct = ClientDiscoveryCache.getLongTermMasteryPercent();

        drawGoalScaleRow(graphics, x, y,
                Component.translatable("codex.mycelialmind.goals.short", Component.translatable(shortGoal.translationKey()), shortPct),
                shortPct, 0xFF72EFDD);
        y += 11;
        drawGoalScaleRow(graphics, x, y,
                Component.translatable("codex.mycelialmind.goals.mid", Component.translatable(midGoal.translationKey()), midPct),
                midPct, 0xFFFFB703);
        y += 11;
        drawGoalScaleRow(graphics, x, y,
                Component.translatable("codex.mycelialmind.goals.long", longPct),
                longPct, 0xFFFF4D8D);
        y += 12;

        drawScaledText(graphics, Component.translatable("codex.mycelialmind.goals.bestiary_bastion",
                ClientDiscoveryCache.countScannedSpecies(), ProgressionGoalEngine.TOTAL_SPECIES,
                ClientDiscoveryCache.countDiscoveredTraits(), ProgressionGoalEngine.TOTAL_TRAITS,
                ClientDiscoveryCache.getRareVariantsFound(),
                ClientDiscoveryCache.getBastionScore()), x, y, 286, 0xFFA3F7B5);
        y += 11;

        Component cycleSummary = com.alex.mycelialmind.network.ClientCycleCache.isCycleActive()
                ? Component.translatable("codex.mycelialmind.goals.engrams_cycle_active",
                        ClientDiscoveryCache.countDiscoveredEngrams(), com.alex.mycelialmind.hive.FieldEngramId.TOTAL_COUNT,
                        com.alex.mycelialmind.network.ClientCycleCache.getActiveCycleNumber(),
                        com.alex.mycelialmind.network.ClientCycleCache.getBestScore())
                : Component.translatable("codex.mycelialmind.goals.engrams_cycle_idle",
                        ClientDiscoveryCache.countDiscoveredEngrams(), com.alex.mycelialmind.hive.FieldEngramId.TOTAL_COUNT,
                        com.alex.mycelialmind.network.ClientCycleCache.getCompletedCycles(),
                        com.alex.mycelialmind.network.ClientCycleCache.getBestScore());
        drawScaledText(graphics, cycleSummary, x, y, 286, 0xFF72EFDD);
        y += 11;

        graphics.drawString(this.font,
                Component.translatable("codex.mycelialmind.goals.secrets_header",
                        ClientDiscoveryCache.countDiscoveredSecrets(), ProgressionGoalEngine.TOTAL_SECRETS),
                x, y, 0xFFE6C2FF, true);
        y += 11;

        for (int i = 0; i < ProgressionGoalEngine.TOTAL_SECRETS; i++) {
            boolean unlocked = ClientDiscoveryCache.hasSecret(i);
            Component entryText = unlocked
                    ? Component.translatable("codex.mycelialmind.secret.unlocked",
                            Component.translatable("codex.mycelialmind.secret.name." + (i + 1)))
                    : Component.translatable("codex.mycelialmind.secret.locked",
                            Component.translatable("codex.mycelialmind.secret.hint." + (i + 1)));
            int maxW = (i == ProgressionGoalEngine.TOTAL_SECRETS - 1) ? 210 : 286;
            drawScaledText(graphics, entryText, x, y, maxW, unlocked ? 0xFF9DE0AD : 0xFFADB5BD);
            y += 10;
        }
    }

    private void drawGoalScaleRow(GuiGraphics graphics, int x, int y, Component label, int pct, int barColor) {
        drawScaledText(graphics, label, x, y, 198, 0xFFE6C2FF);
        int barX = x + 204;
        int barW = 80;
        graphics.fill(barX, y + 1, barX + barW, y + 7, 0xFF2B193D);
        int fill = (int) Math.round(barW * Math.max(0.0, Math.min(1.0, pct / 100.0)));
        graphics.fill(barX, y + 1, barX + fill, y + 7, barColor);
    }

    private void drawScaledText(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        int textW = this.font.width(text);
        if (textW > maxWidth && textW > 0) {
            float scale = (float) maxWidth / (float) textW;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(this.font, text, 0, 0, color, true);
            graphics.pose().popPose();
        } else {
            graphics.drawString(this.font, text, x, y, color, true);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
