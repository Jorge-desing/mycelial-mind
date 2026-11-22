package com.alex.mycelialmind.client.hud;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.network.ClientDiscoveryCache;
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
        if (!hasScanner) {
            return;
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
        int extraEffectLines = (sporeRotEffect != null ? 1 : 0) + (insightEffect != null ? 1 : 0) + (deceptionActive ? 1 : 0);
        Font font = mc.font;

        boolean compact = MycelialClientConfig.isHudCompact();
        int width = compact ? 186 : 204;
        int squadLine = squadStatus != SynapticCommandSystem.SquadTacticalStatus.NONE ? 1 : 0;
        int homeoLines = (inDeepMycelium ? 1 : 0) + (smotheredPylons > 0 ? 1 : 0);
        int height;
        if (compact) {
            int compactExtra = (infectionLevel > 0 ? 1 : 0) + squadLine + homeoLines + extraEffectLines;
            height = 73 + compactExtra * 10;
        } else {
            int extraLines = (suppressedSec > 0 || !synergies.isEmpty()) ? 1 + synergies.size() : 0;
            int symLines = 1 + mutagens.size() + (infectionLevel > 0 ? 1 : 0) + squadLine + homeoLines + extraEffectLines;
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
        graphics.fill(barX, barY, barX + barW, barY + 6, 0xFF2B193D);
        int fillW = (int) Math.round(barW * Math.min(1.0, Math.max(0.0, threat / 100.0)));
        int barColor = threat >= 75.0 ? 0xFFFF3366 : (threat >= 25.0 ? 0xFFE07A5F : 0xFF64DFDF);
        graphics.fill(barX, barY, barX + fillW, barY + 6, barColor);

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
                    squadStatus, squadSec, inDeepMycelium, homeoStress, homeoStage, smotheredPylons, x + 6, lineY);
            drawGoalTrackerLine(graphics, font, x + 6, lineY, width - 12);
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
                squadStatus, squadSec, inDeepMycelium, homeoStress, homeoStage, smotheredPylons, x + 6, lineY);
        drawGoalTrackerLine(graphics, font, x + 6, lineY, width - 12);
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
}
