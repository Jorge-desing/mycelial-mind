package com.alex.mycelialmind.item.tooltip;

import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.FieldEngramId;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

import javax.annotation.Nullable;

/**
 * Common-safe TooltipComponent data records and color lookup helpers for diegetic inventory tooltips.
 */
public final class MycelialTooltipData {
    private MycelialTooltipData() {
    }

    public sealed interface MycelialTooltipComponent extends TooltipComponent
            permits ThreatBarTooltip, TraitBadgeTooltip {
    }

    public record ThreatBarTooltip(float threatPercent, boolean suppressed, int activeTraitCount)
            implements MycelialTooltipComponent {
        public float clampedThreat() {
            if (!Float.isFinite(threatPercent)) {
                return 0.0F;
            }
            return Math.max(0.0F, Math.min(100.0F, threatPercent));
        }

        public int clampedTraitCount() {
            return Math.max(0, activeTraitCount);
        }
    }

    public record TraitBadgeTooltip(int accentRgb, Component badgeLabel, @Nullable Component subLabel)
            implements MycelialTooltipComponent {
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
