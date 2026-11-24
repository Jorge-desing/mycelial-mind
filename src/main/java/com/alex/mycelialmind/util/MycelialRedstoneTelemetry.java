package com.alex.mycelialmind.util;

import com.alex.mycelialmind.colony.SovereignColonyEngine;

/**
 * Pure comparator analog output telemetry (0..15) for Mycelial Mind functional blocks.
 * Common-safe: zero Minecraft client/level dependencies for deterministic JUnit & Pitest coverage.
 */
public final class MycelialRedstoneTelemetry {
    private MycelialRedstoneTelemetry() {
    }

    public static int benchSignal(boolean hasOutput, int filledInputs) {
        if (hasOutput) {
            return 15;
        }
        int clamped = Math.max(0, Math.min(3, filledInputs));
        return clamped * 3;
    }

    public static int turretSignal(int tier, boolean jammed) {
        if (jammed) {
            return 0;
        }
        if (tier >= 3) {
            return 15;
        }
        if (tier == 2) {
            return 10;
        }
        return 5;
    }

    public static int pylonSignal(boolean smothered) {
        return smothered ? 0 : 15;
    }

    public static int pylonSignal(boolean smothered, int tier) {
        if (smothered) {
            return 0;
        }
        if (tier >= 3) {
            return 15;
        }
        if (tier == 2) {
            return 10;
        }
        return 5;
    }

    public static int regenerativeFloraSignal(boolean mature) {
        return mature ? 15 : 5;
    }

    public static int symbioticNodeSignal(int stability) {
        int clamped = Math.max(0, Math.min(100, stability));
        return (int) Math.round(clamped * 15.0D / 100.0D);
    }

    public static int hostileNodeSignal(double volatileTotal, int biomassCharge) {
        double safeVolatile = Double.isFinite(volatileTotal) ? Math.max(0.0D, volatileTotal) : 0.0D;
        int safeBiomass = Math.max(0, biomassCharge);
        double norm = Math.max(0.0D, Math.min(1.0D, Math.max(safeVolatile / 100.0D, safeBiomass / 100.0D)));
        return (int) Math.round(norm * 15.0D);
    }

    public static int antibodyCystSignal(boolean active, double chunkStress, int adjacentCysts) {
        if (!active) {
            return 0;
        }
        double safeStress = Double.isFinite(chunkStress) ? Math.max(0.0D, Math.min(100.0D, chunkStress)) : 0.0D;
        int safeAdj = Math.max(0, Math.min(6, adjacentCysts));
        int stressContribution = (int) Math.round((safeStress / 100.0D) * 9.0D);
        return Math.max(3, Math.min(15, 3 + stressContribution + (safeAdj / 2)));
    }

    public static int hyphalVeinSignal(int strength, boolean charged) {
        int clamped = Math.max(0, Math.min(3, strength));
        return Math.min(15, clamped * 4 + (charged ? 3 : 0));
    }

    public static int sovereignNexusSignal(int tier, int activeWorkers, int popCap, int biomassStored) {
        if (tier <= 0 && activeWorkers <= 0 && biomassStored <= 0) {
            return 0;
        }
        double safeTier = Math.max(0.0D, Math.min(3.0D, tier)) / 3.0D;
        double safeWorkers = popCap > 0 ? Math.max(0.0D, Math.min(1.0D, (double) activeWorkers / popCap)) : 0.0D;
        double safeBiomass = Math.max(0.0D, Math.min(1.0D, biomassStored / (double) SovereignColonyEngine.MAX_BIOMASS));
        int score = (int) Math.round(1.0D + 14.0D * (0.35D * safeTier + 0.35D * safeWorkers + 0.30D * safeBiomass));
        return Math.max(1, Math.min(15, score));
    }
}
