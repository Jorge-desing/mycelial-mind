package com.alex.mycelialmind.world;

import com.alex.mycelialmind.hive.HiveProgressionTier;

/**
 * Pure deterministic engine governing vertical surface-to-subterranean Mycelial Sinkhole
 * thermodynamics, spore updraft physics, sap regeneration, and containment/assault scaling.
 */
public final class SinkholeEcosystemEngine {
    public static final int PURIFIED_UPDRAFT_HEIGHT = 20;
    public static final double PURIFIED_AGGRO_RANGE = 6.0D;

    private SinkholeEcosystemEngine() {
    }

    /**
     * Vertical reach (in blocks) of the central Spore Updraft Vent column.
     * Unpurified vents scale with HiveProgressionTier; purified vents stabilize as safe elevators.
     */
    public static int computeUpdraftHeight(HiveProgressionTier tier, boolean purified) {
        if (purified) {
            return PURIFIED_UPDRAFT_HEIGHT;
        }
        HiveProgressionTier safeTier = tier != null ? tier : HiveProgressionTier.DORMANT;
        return switch (safeTier) {
            case DORMANT -> 14;
            case AWAKENED -> 18;
            case ADAPTIVE -> 22;
            case OVERMIND -> 26;
        };
    }

    /**
     * Computes target vertical velocity for an entity inside the updraft column.
     * Sneaking provides a controlled cushioned descent; normal stance lifts smoothly.
     */
    public static double computeUpdraftVerticalBoost(double distAboveVent, int maxHeight, boolean sneaking) {
        if (!Double.isFinite(distAboveVent) || maxHeight <= 0 || distAboveVent < 0.0D || distAboveVent > maxHeight) {
            return 0.0D;
        }
        if (sneaking) {
            return -0.06D;
        }
        double norm = Math.max(0.0D, Math.min(1.0D, distAboveVent / maxHeight));
        return 0.16D - norm * 0.08D;
    }

    /**
     * Passive threat pressure emitted by an active, unsealed sinkhole root system.
     */
    public static double computePassiveThreatPulse(HiveProgressionTier tier, boolean purified, boolean rootNodeAlive) {
        if (purified || !rootNodeAlive) {
            return 0.0D;
        }
        HiveProgressionTier safeTier = tier != null ? tier : HiveProgressionTier.DORMANT;
        return switch (safeTier) {
            case DORMANT -> 0.05D;
            case AWAKENED -> 0.10D;
            case ADAPTIVE -> 0.18D;
            case OVERMIND -> 0.28D;
        };
    }

    /**
     * Aggression/detection range of the subterranean Root Keeper guardian.
     */
    public static double computeRootKeeperAggroRange(HiveProgressionTier tier, boolean purified) {
        if (purified) {
            return PURIFIED_AGGRO_RANGE;
        }
        HiveProgressionTier safeTier = tier != null ? tier : HiveProgressionTier.DORMANT;
        return switch (safeTier) {
            case DORMANT -> 10.0D;
            case AWAKENED -> 16.0D;
            case ADAPTIVE -> 22.0D;
            case OVERMIND -> 28.0D;
        };
    }

    /**
     * Random-tick probability that depleted Root Sap Moss regenerates usable sap.
     */
    public static float computeSapRegrowthChance(HiveProgressionTier tier) {
        HiveProgressionTier safeTier = tier != null ? tier : HiveProgressionTier.DORMANT;
        return switch (safeTier) {
            case DORMANT -> 0.12F;
            case AWAKENED -> 0.20F;
            case ADAPTIVE -> 0.30F;
            case OVERMIND -> 0.42F;
        };
    }

    /**
     * Returns true when the sinkhole breach has been contained (either the Root Node was
     * destroyed or a Purification Pylon suppresses the cavity).
     */
    public static boolean isBreachSealed(boolean rootNodeAlive, boolean pylonProtected) {
        return !rootNodeAlive || pylonProtected;
    }
}
