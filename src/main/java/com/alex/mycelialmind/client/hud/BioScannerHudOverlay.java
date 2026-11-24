package com.alex.mycelialmind.client.hud;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.entity.MycelialBossMob;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossAttackId;
import com.alex.mycelialmind.hive.BossCombatArchitectureEngine.BossCombatState;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
import com.alex.mycelialmind.network.ClientExpeditionCache;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.network.ClientSymbiosisCache;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.util.MycelialText;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage;
import com.alex.mycelialmind.world.ModDimensions;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.util.Set;

/**
 * Displays Hive Mind threat level and active AdaptationTraits on the HUD
 * whenever the player carries or holds the Mycelial Bio-Scanner / Bio-Extractor.
 */
@EventBusSubscriber(modid = MycelialMind.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class BioScannerHudOverlay implements LayeredDraw.Layer {
    private static final ResourceLocation LAYER_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "bio_scanner_hud");
    private static float displayedThreat = -1.0F;

    public static void resetAnimationState() {
        displayedThreat = -1.0F;
    }

    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.HOTBAR, LAYER_ID, new BioScannerHudOverlay());
    }

    private static boolean isScannerOrExtractor(ItemStack stack) {
        return stack.is(ModItems.BIO_SCANNER.get()) || stack.is(ModItems.BIO_EXTRACTOR.get());
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null || mc.options.hideGui) {
            return;
        }
        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (MycelialClientConfig.statusVignetteEnabled()) {
            boolean infected = player.hasEffect(ModEffects.MYCELIAL_INFECTION);
            boolean sporeRot = player.hasEffect(ModEffects.SPORE_ROT);
            boolean suppressed = ClientHiveCache.getSuppressionSecondsRemaining() > 0;
            boolean highThreat = ClientHiveCache.getThreatLevel() >= 75.0D;
            float phase = (player.tickCount + deltaTracker.getGameTimeDeltaPartialTick(true)) * 0.14F;
            var vig = com.alex.mycelialmind.hive.UxJuiceEngine.computeVignetteState(
                    infected, sporeRot, suppressed, highThreat, phase, 1.0F);
            if (vig.alpha() > 0.005F) {
                int edge = 10;
                int innerA = Math.max(4, Math.round(vig.alpha() * 0.45F * 255.0F)) & 0xFF;
                int innerCol = (innerA << 24) | (vig.argb() & 0x00FFFFFF);
                graphics.fill(0, 0, sw, edge, vig.argb());
                graphics.fill(0, sh - edge, sw, sh, vig.argb());
                graphics.fill(0, edge, edge, sh - edge, vig.argb());
                graphics.fill(sw - edge, edge, sw, sh - edge, vig.argb());
                graphics.fill(edge, edge, sw - edge, edge + 6, innerCol);
                graphics.fill(edge, sh - edge - 6, sw - edge, sh - edge, innerCol);
            }
        }

        MobEffectInstance thermalVision = player.getEffect(ModEffects.SYNAPTIC_VISION);
        if (thermalVision != null) {
            int pulseSec = (thermalVision.getDuration() + 19) / 20;
            if (MycelialClientConfig.showSynapticVisionBorder()) {
                int borderCol = 0x5500F5D4;
                graphics.fill(0, 0, sw, 2, borderCol);
                graphics.fill(0, sh - 2, sw, sh, borderCol);
                graphics.fill(0, 2, 2, sh - 2, borderCol);
                graphics.fill(sw - 2, 2, sw, sh - 2, borderCol);
            }
            Component visText = Component.translatable("hud.mycelialmind.synaptic_vision", pulseSec);
            int vw = mc.font.width(visText) + 10;
            int vx = sw - vw - 8;
            graphics.fill(vx, 8, vx + vw, 20, 0xBB05262E);
            graphics.renderOutline(vx, 8, vw, 12, 0xFF00F5D4);
            graphics.drawString(mc.font, visText, vx + 5, 10, 0xFF72EFDD, true);
        }

        int tideSec = ClientHiveCache.getFungalTideSecondsRemaining();
        if (tideSec > 0) {
            int mins = tideSec / 60;
            int secs = tideSec % 60;
            Component banner = Component.translatable("hud.mycelialmind.fungal_tide", mins, secs);
            int bw = mc.font.width(banner) + 14;
            int bx = (sw - bw) / 2;
            graphics.fill(bx, 6, bx + bw, 20, 0xCC240836);
            graphics.renderOutline(bx, 6, bw, 14, 0xFFFF4D8D);
            graphics.drawCenteredString(mc.font, banner, sw / 2, 9, 0xFFFF8CB4);
        }

        boolean inDeepMycelium = player.level().dimension().equals(ModDimensions.DEEP_MYCELIUM_LEVEL);
        HomeostasisStage homeoStage = ClientHiveCache.getHomeostasisStage();
        double homeoStress = ClientHiveCache.getHomeostasisStress();
        if (inDeepMycelium && homeoStage != HomeostasisStage.STABLE) {
            int bannerY = tideSec > 0 ? 23 : 6;
            Component exposureText = Component.translatable(ClientHiveCache.isAcidRainExposed()
                    ? "hud.mycelialmind.acid_rain"
                    : "hud.mycelialmind.sheltered");
            Component banner = homeoStage == HomeostasisStage.IMMUNE_RESPONSE
                    ? Component.translatable("hud.mycelialmind.immune_response",
                    String.format("%.0f", homeoStress), exposureText)
                    : Component.translatable("hud.mycelialmind.dimension_irritated",
                    String.format("%.0f", homeoStress));
            int bw = mc.font.width(banner) + 14;
            int bx = (sw - bw) / 2;
            int bg = homeoStage == HomeostasisStage.IMMUNE_RESPONSE ? 0xCC330614 : 0xCC2E1D05;
            int fg = homeoStage == HomeostasisStage.IMMUNE_RESPONSE ? 0xFFFF758F : 0xFFFFD166;
            graphics.fill(bx, bannerY, bx + bw, bannerY + 14, bg);
            graphics.renderOutline(bx, bannerY, bw, 14, homeoStage.argbColor());
            graphics.drawCenteredString(mc.font, banner, sw / 2, bannerY + 3, fg);
        }

        int remissionSec = ClientHiveCache.getMatriarchRemissionSecondsRemaining();
        if (remissionSec > 0) {
            int mins = remissionSec / 60;
            int secs = remissionSec % 60;
            int bannerY = 6 + (tideSec > 0 ? 17 : 0) + (inDeepMycelium && homeoStage != HomeostasisStage.STABLE ? 17 : 0);
            Component remBanner = Component.translatable("hud.mycelialmind.matriarch_remission", mins, secs);
            int bw = mc.font.width(remBanner) + 14;
            int bx = (sw - bw) / 2;
            graphics.fill(bx, bannerY, bx + bw, bannerY + 14, 0xCC06281E);
            graphics.renderOutline(bx, bannerY, bw, 14, 0xFF4AD66D);
            graphics.drawCenteredString(mc.font, remBanner, sw / 2, bannerY + 3, 0xFF9BF6B0);
        }

        boolean hasBossTelemetry = renderNearbyBossTelemetryBar(graphics, mc, player, sw,
                tideSec > 0, inDeepMycelium && homeoStage != HomeostasisStage.STABLE, remissionSec > 0);

        boolean hasScanner = isScannerOrExtractor(player.getMainHandItem())
                || isScannerOrExtractor(player.getOffhandItem());
        if (!hasScanner && !MycelialClientConfig.isHudRequireHeld()) {
            for (ItemStack invStack : player.getInventory().items) {
                if (isScannerOrExtractor(invStack)) {
                    hasScanner = true;
                    break;
                }
            }
        }
        if (hasScanner) {
            boolean inActiveCombat = hasBossTelemetry
                    || tideSec > 0
                    || player.hurtTime > 0
                    || (player.getLastHurtByMobTimestamp() > 0 && (player.tickCount - player.getLastHurtByMobTimestamp()) < 100)
                    || (player.getLastHurtMobTimestamp() > 0 && (player.tickCount - player.getLastHurtMobTimestamp()) < 100);
            renderScannerPanel(graphics, mc, player, sw, sh, thermalVision, inDeepMycelium, homeoStage, homeoStress, inActiveCombat);
        }
        DiegeticToastManager.render(graphics, mc.font, sw, sh);
    }

    private static void renderScannerPanel(
            GuiGraphics graphics,
            Minecraft mc,
            Player player,
            int sw,
            int sh,
            MobEffectInstance thermalVision,
            boolean inDeepMycelium,
            HomeostasisStage homeoStage,
            double homeoStress,
            boolean inActiveCombat
    ) {

        BlockPos destination = ClientExpeditionCache.getDirectory().pinned();
        if (destination != null) {
            Component destinationText = inDeepMycelium
                    ? Component.translatable("hud.mycelialmind.enclave_destination", destination.getX(), destination.getY(),
                            destination.getZ(), (int) Math.sqrt(destination.distSqr(player.blockPosition())))
                    : Component.translatable("hud.mycelialmind.enclave_other_dimension", destination.getX(), destination.getY(), destination.getZ());
            int destinationWidth = mc.font.width(destinationText) + 10;
            int destinationX = Math.max(4, sw - destinationWidth - 8);
            int destinationY = thermalVision != null ? 24 : 8;
            graphics.fill(destinationX, destinationY, destinationX + destinationWidth, destinationY + 12, 0xBB05262E);
            graphics.drawString(mc.font, destinationText, destinationX + 5, destinationY + 2, 0xFF72EFDD, true);
        }

        double threat = ClientHiveCache.getThreatLevel();
        Set<AdaptationTrait> traits = ClientHiveCache.getActiveTraits();
        Set<AdaptationTrait> tier2 = ClientHiveCache.getTier2Traits();
        Set<HiveSynergy> synergies = ClientHiveCache.getActiveSynergies();
        boolean protectedZone = ClientHiveCache.isInProtectedZone();
        BlockPos nearestNode = ClientHiveCache.getNearestNodePos();
        int links = ClientHiveCache.getActiveNetworkLinks();
        int suppressedSec = ClientHiveCache.getSuppressionSecondsRemaining();
        Set<SymbioticMutagen> mutagens = ClientSymbiosisCache.getEquippedMutagens();
        int infectionLevel = ClientSymbiosisCache.getInfectionLevel();
        SynapticCommandSystem.SquadTacticalStatus squadStatus = ClientHiveCache.getSquadTacticalStatus();
        int squadSec = ClientHiveCache.getSquadStatusSecondsRemaining();
        int smotheredPylons = ClientHiveCache.getSmotheredPylonCount();
        MobEffectInstance sporeRotEffect = player.getEffect(ModEffects.SPORE_ROT);
        MobEffectInstance insightEffect = player.getEffect(ModEffects.SYNAPTIC_INSIGHT);
        boolean deceptionActive = ClientHiveCache.getFalseStimulus() != null && ClientHiveCache.getDeceptionSecondsRemaining() > 0;
        boolean mimicPresent = mc.level != null && !mc.level.getEntitiesOfClass(
                SynapticMimicEntity.class,
                player.getBoundingBox().inflate(24.0D),
                m -> m.isAlive() && m.isInformationWarfareActive()
        ).isEmpty();
        boolean mimicExposed = thermalVision != null;
        int extraEffectLines = (sporeRotEffect != null ? 1 : 0)
                + (insightEffect != null ? 1 : 0)
                + (deceptionActive ? 1 : 0)
                + (mimicPresent ? 1 : 0);
        Font font = mc.font;

        boolean manualCompact = MycelialClientConfig.isHudCompact();
        boolean combatFocus = MycelialClientConfig.isHudContextualCombat() && inActiveCombat;
        boolean compact = MycelialClientConfig.shouldUseCombatCompactHud(
                MycelialClientConfig.isHudContextualCombat(), manualCompact, inActiveCombat);
        boolean cycleActive = com.alex.mycelialmind.network.ClientCycleCache.isCycleActive();
        boolean colonyActive = com.alex.mycelialmind.network.ClientColonyCache.hasColony();
        int unreadCodex = combatFocus ? 0 : com.alex.mycelialmind.client.screen.ClientUnreadCodexTracker.getTotalUnreadCount();
        boolean showGoalLine = !combatFocus;

        int width = compact ? 186 : 204;
        int squadLine = squadStatus != SynapticCommandSystem.SquadTacticalStatus.NONE ? 1 : 0;
        int homeoLines = (inDeepMycelium ? 1 : 0) + (smotheredPylons > 0 ? 1 : 0);
        int extraHudLines = (cycleActive ? 1 : 0) + (colonyActive ? 1 : 0) + (unreadCodex > 0 ? 1 : 0) - (showGoalLine ? 0 : 1);
        int height;
        if (compact) {
            int compactExtra = (infectionLevel > 0 ? 1 : 0) + squadLine + homeoLines + extraEffectLines + extraHudLines;
            height = 73 + compactExtra * 10;
        } else {
            int extraLines = (suppressedSec > 0 || !synergies.isEmpty()) ? 1 + synergies.size() : 0;
            int symLines = 1 + mutagens.size() + (infectionLevel > 0 ? 1 : 0) + squadLine + homeoLines + extraEffectLines + extraHudLines;
            height = 70 + (traits.isEmpty() ? 10 : traits.size() * 10) + extraLines * 10 + symLines * 10;
        }

        int[] pos = MycelialClientConfig.computeHudPosition(
                MycelialClientConfig.hudAnchor(), sw, sh, width, height,
                MycelialClientConfig.hudOffsetX(), MycelialClientConfig.hudOffsetY()
        );
        int x = pos[0];
        int y = pos[1];

        graphics.fill(x, y, x + width, y + height, 0xAA160C24);
        graphics.renderOutline(x, y, width, height, protectedZone ? 0xFF4CC9F0 : 0xFF8C42B8);

        HiveProgressionTier stage = ClientDiscoveryCache.getEffectiveActTier();
        graphics.drawString(font,
                Component.translatable("hud.mycelialmind.threat", String.format("%.1f", threat), MycelialText.progressionTier(stage)),
                x + 6, y + 6, 0xFFE6C2FF, true);

        int barX = x + 6;
        int barY = y + 17;
        int barW = width - 12;
        float shownThreat = (float) threat;
        if (MycelialClientConfig.smoothHudBarsEnabled()) {
            if (displayedThreat < 0.0F) {
                displayedThreat = (float) threat;
            } else {
                displayedThreat = com.alex.mycelialmind.hive.UxJuiceEngine.lerpBarValue(
                        displayedThreat, (float) threat, 0.18F);
            }
            shownThreat = displayedThreat;
        } else {
            displayedThreat = (float) threat;
        }
        graphics.fill(barX, barY, barX + barW, barY + 6, 0xFF2B193D);
        int fillW = (int) Math.round(barW * Math.min(1.0, Math.max(0.0, shownThreat / 100.0)));
        int barColor = threat >= 75.0 ? 0xFFFF3366 : (threat >= 25.0 ? 0xFFE07A5F : 0xFF64DFDF);
        graphics.fill(barX, barY, barX + fillW, barY + 6, barColor);
        float pulseAlpha = com.alex.mycelialmind.hive.UxJuiceEngine.computeBarPulseAlpha((float) threat - shownThreat);
        if (pulseAlpha > 0.05F && fillW > 1) {
            int pByte = Math.min(220, Math.round(pulseAlpha * 200.0F)) & 0xFF;
            graphics.fill(barX + Math.max(0, fillW - 2), barY, barX + fillW, barY + 6, (pByte << 24) | 0x00FFFFFF);
        }

        int lineY = y + 27;
        Component zoneText = Component.translatable(protectedZone
                ? "hud.mycelialmind.zone_protected"
                : "hud.mycelialmind.zone_exposed");
        int zoneColor = protectedZone ? 0xFF72EFDD : 0xFFF4A261;
        graphics.drawString(font, zoneText, x + 6, lineY, zoneColor, true);
        lineY += 11;

        if (compact) {
            if (suppressedSec > 0) {
                graphics.drawString(font, Component.translatable("hud.mycelialmind.suppressed", suppressedSec),
                        x + 6, lineY, 0xFF4CC9F0, true);
            } else {
                graphics.drawString(font, Component.translatable("hud.mycelialmind.traits_compact",
                        traits.size(), tier2.size(), synergies.size()),
                        x + 6, lineY, traits.isEmpty() ? 0xFF9DE0AD : 0xFFFFB703, true);
            }
            lineY += 10;
            drawNearestNodeLine(graphics, font, player, nearestNode, links, x + 6, lineY + 1);
            lineY += 11;
            lineY = drawStatusTailLines(graphics, font, infectionLevel, sporeRotEffect, insightEffect, deceptionActive,
                    mimicPresent, mimicExposed, squadStatus, squadSec, inDeepMycelium, homeoStress, homeoStage, smotheredPylons, x + 6, lineY);
            if (cycleActive) {
                graphics.drawString(font, Component.translatable("hud.mycelialmind.cycle_active",
                                com.alex.mycelialmind.network.ClientCycleCache.getActiveCycleNumber(),
                                com.alex.mycelialmind.hive.HiveCycleMutator.countActive(
                                        com.alex.mycelialmind.network.ClientCycleCache.getActiveMutatorMask())),
                        x + 6, lineY, 0xFFFF4D8D, true);
                lineY += 10;
            }
            if (colonyActive) {
                graphics.drawString(font, Component.translatable("hud.mycelialmind.colony_compact",
                                com.alex.mycelialmind.network.ClientColonyCache.getTier(),
                                com.alex.mycelialmind.network.ClientColonyCache.getWorkers().size(),
                                com.alex.mycelialmind.network.ClientColonyCache.getPopCap(),
                                com.alex.mycelialmind.network.ClientColonyCache.getBiomass(),
                                com.alex.mycelialmind.network.ClientColonyCache.getMoralePct()),
                        x + 6, lineY, 0xFF4AD66D, true);
                lineY += 10;
            }
            if (unreadCodex > 0) {
                graphics.drawString(font, Component.translatable("hud.mycelialmind.codex_unread", unreadCodex),
                        x + 6, lineY, 0xFFB5E48C, true);
                lineY += 10;
            }
            if (showGoalLine) {
                drawGoalTrackerLine(graphics, font, x + 6, lineY, width - 12);
            }
            return;
        }

        if (traits.isEmpty()) {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.traits_none"), x + 6, lineY, 0xFF9DE0AD, true);
            lineY += 10;
        } else {
            for (AdaptationTrait trait : traits) {
                boolean isT2 = tier2.contains(trait);
                Component label = isT2
                        ? Component.literal("• ").append(MycelialText.trait(trait)).append(" [N2]")
                        : Component.literal("• ").append(MycelialText.trait(trait));
                int color = isT2 ? 0xFFFF4D8D : 0xFFFFB703;
                graphics.drawString(font, label, x + 6, lineY, color, true);
                lineY += 10;
            }
        }

        if (suppressedSec > 0) {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.suppressed", suppressedSec),
                    x + 6, lineY, 0xFF4CC9F0, true);
            lineY += 10;
        } else if (!synergies.isEmpty()) {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.synergies"), x + 6, lineY, 0xFFFF6B6B, true);
            lineY += 10;
            for (HiveSynergy syn : synergies) {
                graphics.drawString(font, Component.literal("  * ").append(MycelialText.synergy(syn)),
                        x + 6, lineY, 0xFFFF9F1C, true);
                lineY += 10;
            }
        }

        drawNearestNodeLine(graphics, font, player, nearestNode, links, x + 6, lineY + 1);
        lineY += 11;

        graphics.drawString(font, Component.translatable("hud.mycelialmind.symbiosis", mutagens.size()),
                x + 6, lineY, mutagens.isEmpty() ? 0xFF8D99AE : 0xFFA3F7B5, true);
        lineY += 10;
        for (SymbioticMutagen m : mutagens) {
            graphics.drawString(font, Component.literal("  + ").append(MycelialText.mutagen(m)),
                    x + 6, lineY, 0xFF72EFDD, true);
            lineY += 10;
        }
        lineY = drawStatusTailLines(graphics, font, infectionLevel, sporeRotEffect, insightEffect, deceptionActive,
                mimicPresent, mimicExposed, squadStatus, squadSec, inDeepMycelium, homeoStress, homeoStage, smotheredPylons, x + 6, lineY);
        if (cycleActive) {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.cycle_active",
                            com.alex.mycelialmind.network.ClientCycleCache.getActiveCycleNumber(),
                            com.alex.mycelialmind.hive.HiveCycleMutator.countActive(
                                    com.alex.mycelialmind.network.ClientCycleCache.getActiveMutatorMask())),
                    x + 6, lineY, 0xFFFF4D8D, true);
            lineY += 10;
        }
        if (colonyActive) {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.colony_compact",
                            com.alex.mycelialmind.network.ClientColonyCache.getTier(),
                            com.alex.mycelialmind.network.ClientColonyCache.getWorkers().size(),
                            com.alex.mycelialmind.network.ClientColonyCache.getPopCap(),
                            com.alex.mycelialmind.network.ClientColonyCache.getBiomass(),
                            com.alex.mycelialmind.network.ClientColonyCache.getMoralePct()),
                    x + 6, lineY, 0xFF4AD66D, true);
            lineY += 10;
        }
        if (unreadCodex > 0) {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.codex_unread", unreadCodex),
                    x + 6, lineY, 0xFFB5E48C, true);
            lineY += 10;
        }
        if (showGoalLine) {
            drawGoalTrackerLine(graphics, font, x + 6, lineY, width - 12);
        }
    }

    private static void drawNearestNodeLine(
            GuiGraphics graphics, Font font, Player player, BlockPos nearestNode, int links, int x, int y
    ) {
        if (nearestNode != null) {
            int dx = nearestNode.getX() - player.blockPosition().getX();
            int dz = nearestNode.getZ() - player.blockPosition().getZ();
            int dist = (int) Math.round(Math.sqrt(nearestNode.distSqr(player.blockPosition())));
            Component dir = MycelialText.cardinalDirection(dx, dz);
            graphics.drawString(font, Component.translatable("hud.mycelialmind.node_near", dist, dir, links),
                    x, y, 0xFF72EFDD, true);
        } else {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.node_none", links),
                    x, y, 0xFF8D99AE, true);
        }
    }

    private static int drawStatusTailLines(
            GuiGraphics graphics,
            Font font,
            int infectionLevel,
            MobEffectInstance sporeRotEffect,
            MobEffectInstance insightEffect,
            boolean deceptionActive,
            boolean mimicPresent,
            boolean mimicExposed,
            SynapticCommandSystem.SquadTacticalStatus squadStatus,
            int squadSec,
            boolean inDeepMycelium,
            double homeoStress,
            HomeostasisStage homeoStage,
            int smotheredPylons,
            int x,
            int lineY
    ) {
        if (infectionLevel > 0) {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.infected", infectionLevel),
                    x, lineY, 0xFFFF4D8D, true);
            lineY += 10;
        }
        if (sporeRotEffect != null) {
            int rotSec = Math.max(1, (int) Math.ceil(sporeRotEffect.getDuration() / 20.0));
            graphics.drawString(font, Component.translatable("hud.mycelialmind.spore_rot", rotSec),
                    x, lineY, 0xFFFF595E, true);
            lineY += 10;
        }
        if (insightEffect != null) {
            int insSec = Math.max(1, (int) Math.ceil(insightEffect.getDuration() / 20.0));
            graphics.drawString(font, Component.translatable("hud.mycelialmind.synaptic_insight", insSec),
                    x, lineY, 0xFF72EFDD, true);
            lineY += 10;
        }
        if (deceptionActive) {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.deception_active",
                            MycelialText.stimulus(ClientHiveCache.getFalseStimulus()),
                            ClientHiveCache.getDeceptionSecondsRemaining()),
                    x, lineY, 0xFF38B000, true);
            lineY += 10;
        }
        if (mimicPresent) {
            graphics.drawString(font, Component.translatable(
                            mimicExposed ? "hud.mycelialmind.mimic_exposed" : "hud.mycelialmind.mimic_interference"),
                    x, lineY, mimicExposed ? 0xFF72EFDD : 0xFFFF66CC, true);
            lineY += 10;
        }
        if (squadStatus != SynapticCommandSystem.SquadTacticalStatus.NONE) {
            Component squadText = switch (squadStatus) {
                case SYNCED -> Component.translatable("hud.mycelialmind.squad_synced");
                case DESYNCED -> Component.translatable("hud.mycelialmind.squad_desynced", squadSec);
                case FRENZY -> Component.translatable("hud.mycelialmind.squad_frenzy", squadSec);
                case NONE -> Component.empty();
            };
            int squadColor = switch (squadStatus) {
                case SYNCED -> 0xFFC77DFF;
                case DESYNCED -> 0xFF4CC9F0;
                case FRENZY -> 0xFFFF3366;
                case NONE -> 0xFFFFFFFF;
            };
            graphics.drawString(font, squadText, x, lineY, squadColor, true);
            lineY += 10;
        }
        if (inDeepMycelium) {
            graphics.drawString(font,
                    Component.translatable("hud.mycelialmind.homeostasis",
                            String.format("%.0f", homeoStress),
                            MycelialText.homeostasisStage(homeoStage),
                            ClientHiveCache.getHomeostasisSensitization()),
                    x, lineY, homeoStage.argbColor(), true);
            lineY += 10;
        }
        if (smotheredPylons > 0) {
            graphics.drawString(font, Component.translatable("hud.mycelialmind.smothered_pylons", smotheredPylons),
                    x, lineY, 0xFFFF595E, true);
            lineY += 10;
        }
        return lineY;
    }

    private static void drawGoalTrackerLine(GuiGraphics graphics, Font font, int x, int y, int maxWidth) {
        Minecraft mc = Minecraft.getInstance();
        ProgressionGoalEngine.ExpeditionGoal pinnedGoal = ClientDiscoveryCache.getPinnedExpeditionGoal();
        Component goalLine;
        int color;
        if (pinnedGoal != ProgressionGoalEngine.ExpeditionGoal.NONE) {
            ProgressionGoalEngine.ResearchNextStep step = ClientDiscoveryCache.resolveClientResearchNextStep(mc.player);
            Component stepText = step == ProgressionGoalEngine.ResearchNextStep.COMPLETED_NODE_DESTRUCTION
                    ? Component.translatable(step.hudTranslationKey(), String.format("%.1f", ClientDiscoveryCache.getLastThreatDrop()))
                    : Component.translatable(step.hudTranslationKey());
            goalLine = Component.translatable(
                    "hud.mycelialmind.pinned_goal_tracker",
                    Component.translatable(pinnedGoal.translationKey()),
                    stepText
            );
            color = step.isWarning() ? 0xFFFF758F : 0xFF72EFDD;
        } else {
            HiveProgressionTier actTier = ClientDiscoveryCache.getEffectiveActTier();
            goalLine = Component.translatable(
                    "hud.mycelialmind.goal_tracker",
                    actTier.actNumber(),
                    Component.translatable(ClientDiscoveryCache.getActiveShortTermGoal().translationKey()),
                    ClientDiscoveryCache.getLongTermMasteryPercent()
            );
            color = 0xFFFFD166;
        }
        int textW = font.width(goalLine);
        if (textW > maxWidth && textW > 0) {
            float scale = (float) maxWidth / (float) textW;
            graphics.pose().pushPose();
            graphics.pose().translate(x, y, 0);
            graphics.pose().scale(scale, scale, 1.0F);
            graphics.drawString(font, goalLine, 0, 0, color, true);
            graphics.pose().popPose();
        } else {
            graphics.drawString(font, goalLine, x, y, color, true);
        }
    }

    private static boolean renderNearbyBossTelemetryBar(
            GuiGraphics graphics,
            Minecraft mc,
            Player player,
            int sw,
            boolean hasTideBanner,
            boolean hasHomeoBanner,
            boolean hasRemissionBanner
    ) {
        if (mc.level == null) {
            return false;
        }
        var bosses = mc.level.getEntitiesOfClass(
                MycelialBossMob.class,
                player.getBoundingBox().inflate(48.0D),
                MycelialBossMob::isAlive
        );
        if (bosses.isEmpty()) {
            return false;
        }
        MycelialBossMob nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (MycelialBossMob candidate : bosses) {
            double d = candidate.distanceToSqr(player);
            if (d < bestDistSq) {
                bestDistSq = d;
                nearest = candidate;
            }
        }
        if (nearest == null) {
            return false;
        }

        int topBanners = (hasTideBanner ? 1 : 0) + (hasHomeoBanner ? 1 : 0) + (hasRemissionBanner ? 1 : 0);
        Font font = mc.font;
        boolean broken = nearest.isCarapaceBroken();
        BossCombatState state = nearest.getCombatState();
        boolean telegraphLocked = state == BossCombatState.WINDUP && nearest.isTelegraphLocked();
        boolean comboActive = nearest.isComboActive();
        int remainingAnchors = nearest instanceof HiveCoreBossEntity hiveBoss ? hiveBoss.getSyncedRemainingAnchors() : 0;

        Component header = Component.translatable(
                "hud.mycelialmind.boss_header",
                nearest.getDisplayName(),
                nearest.getActiveBossPhase(),
                nearest.getMaxBossPhases()
        );
        int carapacePct = Math.round(nearest.getCarapaceIntegrityRatio() * 100.0F);
        Component carapaceLabel = broken
                ? Component.translatable("hud.mycelialmind.boss_carapace_broken")
                : Component.translatable("hud.mycelialmind.boss_carapace_intact", carapacePct);
        int headerW = font.width(header);
        int labelW = font.width(carapaceLabel);

        int panelW = Math.min(sw - 16, Math.max(228, headerW + labelW + 18));
        int panelH = 38;
        int panelX = (sw - panelW) / 2;
        int panelY = 40 + topBanners * 17;

        boolean flashToggle = (player.tickCount / 2) % 2 == 0;
        int borderCol;
        if (telegraphLocked) {
            borderCol = flashToggle ? 0xFFFF2A55 : 0xFFFFB703;
        } else if (broken) {
            borderCol = 0xFFFFD166;
        } else if (state == BossCombatState.WINDUP) {
            borderCol = 0xFFFF595E;
        } else {
            borderCol = 0xFF9D4EDD;
        }
        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xCC140A20);
        graphics.renderOutline(panelX, panelY, panelW, panelH, borderCol);

        graphics.drawString(font, header, panelX + 5, panelY + 4, 0xFFF3D5FF, true);
        int carapaceTextColor = broken ? 0xFFFFD166 : 0xFFF4A261;
        graphics.drawString(font, carapaceLabel, panelX + panelW - labelW - 5, panelY + 4, carapaceTextColor, true);

        // Carapace Integrity Bar
        int barX = panelX + 5;
        int barW = panelW - 10;
        int carapaceBarY = panelY + 15;
        graphics.fill(barX, carapaceBarY, barX + barW, carapaceBarY + 4, 0xFF2B193D);
        if (broken) {
            float brokenRatio = Math.min(1.0F, nearest.getCarapaceBrokenTicksRemaining() / 160.0F);
            int fillW = Math.round(barW * brokenRatio);
            graphics.fill(barX, carapaceBarY, barX + fillW, carapaceBarY + 4, 0xFFFFD166);
        } else {
            int fillW = Math.round(barW * nearest.getCarapaceIntegrityRatio());
            graphics.fill(barX, carapaceBarY, barX + fillW, carapaceBarY + 4, 0xFFE07A5F);
        }

        // Telegraph / Combat State Line + Progress Bar
        BossAttackId activeAttack = nearest.getActiveAttack();
        Component attackName = Component.translatable(activeAttack.translationKey());
        Component stateText;
        int stateColor;
        int stateBarColor;
        float barFill;
        switch (state) {
            case WINDUP -> {
                stateText = telegraphLocked
                        ? Component.translatable("hud.mycelialmind.boss_telegraph_locked", attackName)
                        : Component.translatable("hud.mycelialmind.boss_telegraph_windup", attackName);
                stateColor = telegraphLocked ? 0xFFFF4D6D : 0xFFFF9F1C;
                stateBarColor = telegraphLocked ? 0xFFFF2A55 : 0xFFFF9F1C;
                barFill = nearest.getStateProgress();
            }
            case ACTIVE -> {
                stateText = Component.translatable("hud.mycelialmind.boss_telegraph_active", attackName);
                stateColor = 0xFFFF4D6D;
                stateBarColor = 0xFFFF2A55;
                barFill = 1.0F;
            }
            case STAGGERED -> {
                stateText = Component.translatable("hud.mycelialmind.boss_telegraph_staggered");
                stateColor = 0xFF72EFDD;
                stateBarColor = 0xFF00F5D4;
                barFill = 1.0F - nearest.getStateProgress();
            }
            case RECOVERY -> {
                stateText = Component.translatable("hud.mycelialmind.boss_telegraph_recovery");
                stateColor = 0xFFC77DFF;
                stateBarColor = 0xFF7B2CBF;
                barFill = 1.0F - nearest.getStateProgress();
            }
            default -> {
                stateText = Component.translatable("hud.mycelialmind.boss_telegraph_idle");
                stateColor = 0xFFB8A9C9;
                stateBarColor = 0xFF4A3566;
                barFill = 0.0F;
            }
        }
        graphics.drawString(font, stateText, barX, panelY + 22, stateColor, true);

        // Right-aligned live COMBO / Sentinel Anchors badge
        if (comboActive) {
            Component comboBadge = Component.translatable("hud.mycelialmind.boss_combo_badge");
            int badgeW = font.width(comboBadge);
            graphics.drawString(font, comboBadge, panelX + panelW - badgeW - 5, panelY + 22, 0xFFFFD166, true);
        } else if (remainingAnchors > 0) {
            Component anchorsBadge = Component.translatable("hud.mycelialmind.boss_anchors_badge", remainingAnchors, 3);
            int badgeW = font.width(anchorsBadge);
            graphics.drawString(font, anchorsBadge, panelX + panelW - badgeW - 5, panelY + 22, 0xFF4CC9F0, true);
        }
        int stateBarY = panelY + 32;
        graphics.fill(barX, stateBarY, barX + barW, stateBarY + 3, 0xFF221330);
        int stateFillW = Math.round(barW * Math.max(0.0F, Math.min(1.0F, barFill)));
        if (stateFillW > 0) {
            graphics.fill(barX, stateBarY, barX + stateFillW, stateBarY + 3, stateBarColor);
        }
        return true;
    }
}
