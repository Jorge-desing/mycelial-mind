package com.alex.mycelialmind.client;

import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.FieldEngramId;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.item.GeneticSampleItem;
import com.alex.mycelialmind.item.SymbioticMutagenItem;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Diegetic {@link IItemDecorator} badges and live Hive Threat indicator rendered in inventory
 * and hotbar slots without altering item textures or screen logic.
 */
public final class MycelialItemDecorators {
    private static final int PIP_BORDER_COLOR = 0xCC100A18;
    private static final int THREAT_TRACK_COLOR = 0xB0100A18;

    private MycelialItemDecorators() {
    }

    public static void register(RegisterItemDecorationsEvent event) {
        event.register(ModItems.GENETIC_SAMPLE.get(),
                codedBadgeDecorator(stack -> colorForTrait(GeneticSampleItem.getTrait(stack))));
        event.register(ModItems.COUNTER_SERUM.get(),
                codedBadgeDecorator(stack -> colorForTrait(GeneticSampleItem.getTrait(stack))));
        event.register(ModItems.SYMBIOTIC_MUTAGEN.get(),
                codedBadgeDecorator(stack -> colorForMutagen(SymbioticMutagenItem.getMutagen(stack))));
        event.register(ModItems.FIELD_ENGRAM.get(),
                codedBadgeDecorator(stack -> colorForEngram(
                        FieldEngramId.byIdOrNull(stack.get(ModDataComponents.ENGRAM_ID.get())))));
        event.register(ModItems.BIO_SCANNER.get(), bioScannerThreatDecorator());
    }

    private static IItemDecorator codedBadgeDecorator(Function<ItemStack, Integer> colorResolver) {
        return (GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) -> {
            int argb = colorResolver.apply(stack);
            if (argb == 0) {
                return false;
            }
            int px = xOffset + 12;
            int py = yOffset + 1;
            guiGraphics.fill(px - 1, py - 1, px + 4, py + 4, PIP_BORDER_COLOR);
            guiGraphics.fill(px, py, px + 3, py + 3, argb);
            return false;
        };
    }

    private static IItemDecorator bioScannerThreatDecorator() {
        return (GuiGraphics guiGraphics, Font font, ItemStack stack, int xOffset, int yOffset) -> {
            if (Minecraft.getInstance().level == null) {
                return false;
            }
            double threat = ClientHiveCache.getThreatLevel();
            int width = threatBarWidth(threat);
            int color = threatBarColor(threat);
            int x = xOffset + 2;
            int y = yOffset + 14;
            guiGraphics.fill(x, y, x + 13, y + 1, THREAT_TRACK_COLOR);
            if (width > 0) {
                guiGraphics.fill(x, y, x + width, y + 1, color);
            }
            return false;
        };
    }

    public static int colorForStimulus(@Nullable StimulusType stimulus) {
        if (stimulus == null) {
            return 0;
        }
        return switch (stimulus) {
            case MELEE -> 0xFFE05252;
            case PROJECTILE -> 0xFF52D17A;
            case FIRE -> 0xFFFF8C32;
            case EXPLOSION -> 0xFFF4D35E;
            case MAGIC -> 0xFFA970FF;
        };
    }

    public static int colorForTrait(@Nullable AdaptationTrait trait) {
        return trait == null ? 0 : colorForStimulus(trait.trigger());
    }

    public static int colorForMutagen(@Nullable SymbioticMutagen mutagen) {
        return mutagen == null ? 0 : colorForTrait(mutagen.sourceTrait());
    }

    public static int colorForEngram(@Nullable FieldEngramId engram) {
        if (engram == null) {
            return 0;
        }
        return switch (engram.actNumber()) {
            case 1 -> 0xFF4AD69D;
            case 2 -> 0xFFFFB703;
            case 3 -> 0xFFA970FF;
            default -> 0xFFFF4D8D;
        };
    }

    public static int threatBarWidth(double threat) {
        double clamped = Math.clamp(threat, 0.0D, 100.0D);
        return Math.clamp((int) Math.round((clamped / 100.0D) * 13.0D), 0, 13);
    }

    public static int threatBarColor(double threat) {
        if (threat < 25.0D) {
            return 0xFF38B000;
        }
        if (threat < 50.0D) {
            return 0xFF2AE4CC;
        }
        if (threat < 75.0D) {
            return 0xFFFFB703;
        }
        return 0xFFFF3366;
    }
}
