package com.alex.mycelialmind.colony;

/**
 * Pure domain mathematics and progression rules for the player's Sovereign Hive Colony.
 */
public final class SovereignColonyEngine {
    public static final int MIN_TIER = 1;
    public static final int MAX_TIER = 3;
    public static final int MAX_BIOMASS = 512;
    public static final int MAX_LINKED_OUTPOSTS = 4;
    public static final int OUTPOST_RADIUS_BONUS = 8;
    public static final int MAX_INPUT_CHESTS = 16;
    public static final int MAX_OUTPUT_CHESTS = 16;
    public static final int MAX_WORK_ZONES = 24;
    public static final int RIVAL_RAID_COOLDOWN_TICKS = 7200; // 6 minutes

    private SovereignColonyEngine() {
    }

    public static int clampTier(int tier) {
        return Math.max(MIN_TIER, Math.min(MAX_TIER, tier));
    }

    public static int getPopulationCap(int tier) {
        return switch (clampTier(tier)) {
            case 1 -> 4;
            case 2 -> 8;
            default -> 12;
        };
    }

    public static int getBaseRadius(int tier) {
        return switch (clampTier(tier)) {
            case 1 -> 24;
            case 2 -> 36;
            default -> 48;
        };
    }

    public static int getEffectiveRadius(int tier, int linkedOutpostCount) {
        int outposts = Math.max(0, Math.min(MAX_LINKED_OUTPOSTS, linkedOutpostCount));
        return getBaseRadius(tier) + (outposts * OUTPOST_RADIUS_BONUS);
    }

    public static int getIncubationBiomassCost(int tier) {
        return clampTier(tier) >= 3 ? 6 : 8;
    }

    public static int getRestBiomassCost() {
        return 1;
    }

    public static int getUpgradeBiomassCost(int currentTier) {
        return switch (clampTier(currentTier)) {
            case 1 -> 24;
            case 2 -> 48;
            default -> 0;
        };
    }

    public static int getUpgradeDomainXpRequirement(int currentTier) {
        return switch (clampTier(currentTier)) {
            case 1 -> 150;
            case 2 -> 450;
            default -> Integer.MAX_VALUE;
        };
    }

    public static boolean canUpgradeTier(int currentTier, int storedBiomass, int domainXp) {
        int clamped = clampTier(currentTier);
        if (clamped >= MAX_TIER) {
            return false;
        }
        return storedBiomass >= getUpgradeBiomassCost(clamped)
                && domainXp >= getUpgradeDomainXpRequirement(clamped);
    }

    /**
     * Computes composite colony morale [0..100] inspired by multi-factor citizen happiness models.
     */
    public static double computeColonyMorale(
            int storedBiomass,
            double avgWorkerNutrition,
            double avgWorkerFatigue,
            int linkedOutposts,
            int recentRaidVictories,
            int recentWorkerLosses
    ) {
        double biomassFactor = Math.min(1.0D, Math.max(0.0D, storedBiomass / 48.0D)) * 20.0D;
        double nutritionFactor = Math.min(100.0D, Math.max(0.0D, avgWorkerNutrition)) * 0.35D;
        double restFactor = Math.max(0.0D, 100.0D - Math.min(100.0D, Math.max(0.0D, avgWorkerFatigue))) * 0.25D;
        double outpostFactor = Math.min(MAX_LINKED_OUTPOSTS, Math.max(0, linkedOutposts)) * 4.0D;
        double victoryBonus = Math.min(3, Math.max(0, recentRaidVictories)) * 5.0D;
        double lossPenalty = Math.min(4, Math.max(0, recentWorkerLosses)) * 8.0D;
        double raw = 10.0D + biomassFactor + nutritionFactor + restFactor + outpostFactor + victoryBonus - lossPenalty;
        if (!Double.isFinite(raw)) {
            return 50.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, raw));
    }

    /**
     * Computes effective work delay in ticks (lower is faster) given caste base,
     * worker rank/temperament, colony morale, and global traits.
     */
    public static int computeWorkIntervalTicks(
            int baseTicks,
            SymbiontRank rank,
            SymbiontTemperament temperament,
            double colonyMorale,
            boolean hasSpeedTrait
    ) {
        double mult = (rank == null ? 1.0D : rank.workEfficiencyMultiplier())
                * (temperament == null ? 1.0D : temperament.workSpeedMultiplier());
        if (colonyMorale >= 75.0D) {
            mult *= 1.12D;
        } else if (colonyMorale < 30.0D) {
            mult *= 0.80D;
        }
        if (hasSpeedTrait) {
            mult *= 1.15D;
        }
        int computed = (int) Math.round(Math.max(4.0D, baseTicks / Math.max(0.4D, mult)));
        return Math.max(4, Math.min(200, computed));
    }

    public static double computeOrderDomainXp(
            ColonyOrder.OrderType type,
            SymbiontTemperament temperament,
            int tier
    ) {
        double base = switch (type) {
            case HARVEST -> 4.0D;
            case EXCAVATE -> 6.0D;
            case COURIER_PICKUP, COURIER_DEPOSIT -> 3.0D;
            case REPAIR_STRUCTURE -> 7.0D;
            case DEFEND_THREAT -> 10.0D;
        };
        double tempMult = temperament == null ? 1.0D : temperament.domainXpMultiplier();
        double tierMult = 1.0D + ((clampTier(tier) - 1) * 0.15D);
        return base * tempMult * tierMult;
    }

    public static int computeRivalRaidSquadSize(int tier, int workerCount, int activeCycleNumber) {
        int base = 2 + clampTier(tier) + Math.max(0, workerCount / 3);
        int cycleBonus = Math.min(4, Math.max(0, activeCycleNumber));
        return Math.max(3, Math.min(12, base + cycleBonus));
    }

    public static int computeRaidDefenseDomainXp(int tier, int defeatedRaiders) {
        return 35 + (clampTier(tier) * 20) + (Math.max(0, defeatedRaiders) * 8);
    }

    public static boolean shouldTriggerFatigueRest(double fatigue, double nutrition, boolean inCombat) {
        if (inCombat) {
            return nutrition <= 8.0D || fatigue >= 96.0D;
        }
        return fatigue >= 80.0D || nutrition <= 20.0D;
    }

    public static boolean isRecoveredFromRest(double fatigue, double nutrition) {
        return fatigue <= 15.0D && nutrition >= 75.0D;
    }
}
