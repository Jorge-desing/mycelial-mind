package com.alex.mycelialmind.hive;

/**
 * Pure, deterministic scaling and scoring engine for Post-Matriarch Hive Cycles.
 */
public final class HiveCycleEngine {
    public static final int BASE_CYCLE_SCORE = 1000;
    public static final int MAX_SPEED_BONUS = 500;
    public static final long SPEED_BONUS_WINDOW_TICKS = 24_000L;
    public static final int DEATH_PENALTY = 120;
    public static final int MIN_CYCLE_SCORE = 250;

    private HiveCycleEngine() {
    }

    public static boolean isValidMutatorSelection(int mask) {
        if ((mask & ~HiveCycleMutator.ALL_MASK) != 0) {
            return false;
        }
        int count = HiveCycleMutator.countActive(mask);
        return count >= 1 && count <= HiveCycleMutator.MAX_ACTIVE_MUTATORS;
    }

    public static double computeBossHealthMultiplier(boolean active, int cycleNumber, int mutatorMask) {
        if (!active) {
            return 1.0D;
        }
        int extraCycles = Math.max(0, cycleNumber - 1);
        double cycleBonus = Math.min(1.50D, extraCycles * 0.12D);
        double apexBonus = HiveCycleMutator.PRIMORDIAL_APEX.isActiveIn(mutatorMask) ? 0.25D : 0.0D;
        return 1.0D + cycleBonus + apexBonus;
    }

    public static double computeBossDamageMultiplier(boolean active, int cycleNumber, int mutatorMask) {
        if (!active) {
            return 1.0D;
        }
        int extraCycles = Math.max(0, cycleNumber - 1);
        double cycleBonus = Math.min(0.80D, extraCycles * 0.08D);
        double apexBonus = HiveCycleMutator.PRIMORDIAL_APEX.isActiveIn(mutatorMask) ? 0.15D : 0.0D;
        return 1.0D + cycleBonus + apexBonus;
    }

    public static double computeBossCooldownScale(boolean active, int cycleNumber, int mutatorMask) {
        if (!active) {
            return 1.0D;
        }
        int extraCycles = Math.max(0, cycleNumber - 1);
        double base = HiveCycleMutator.PRIMORDIAL_APEX.isActiveIn(mutatorMask) ? 0.85D : 1.0D;
        double floor = HiveCycleMutator.PRIMORDIAL_APEX.isActiveIn(mutatorMask) ? 0.70D : 0.80D;
        return Math.max(floor, base - extraCycles * 0.02D);
    }

    public static int computeExpeditionExtraAttackers(boolean active, int cycleNumber, int mutatorMask) {
        if (!active) {
            return 0;
        }
        int extra = (HiveCycleMutator.SYNAPTIC_SWARM.isActiveIn(mutatorMask) ? 1 : 0)
                + (cycleNumber >= 3 ? 1 : 0);
        return Math.min(2, extra);
    }

    public static double computeExpeditionEliteBonus(boolean active, int cycleNumber, int mutatorMask) {
        if (!active) {
            return 0.0D;
        }
        int extraCycles = Math.max(0, cycleNumber - 1);
        double raw = (HiveCycleMutator.SYNAPTIC_SWARM.isActiveIn(mutatorMask) ? 0.20D : 0.0D) + extraCycles * 0.05D;
        return Math.min(0.45D, raw);
    }

    public static double computeLearningMultiplier(boolean active, int cycleNumber, int mutatorMask) {
        if (!active) {
            return 1.0D;
        }
        int extraCycles = Math.max(0, cycleNumber - 1);
        double mutMult = HiveCycleMutator.HYPER_ADAPTATION.isActiveIn(mutatorMask) ? 1.45D : 1.0D;
        return mutMult * (1.0D + Math.min(0.30D, extraCycles * 0.05D));
    }

    public static double computeThresholdScale(boolean active, int mutatorMask) {
        return (active && HiveCycleMutator.HYPER_ADAPTATION.isActiveIn(mutatorMask)) ? 0.85D : 1.0D;
    }

    public static double computeHomeostasisStressMultiplier(boolean active, int cycleNumber, int mutatorMask) {
        if (!active) {
            return 1.0D;
        }
        int extraCycles = Math.max(0, cycleNumber - 1);
        double base = HiveCycleMutator.ACUTE_HOMEOSTASIS.isActiveIn(mutatorMask) ? 1.40D : 1.0D;
        return base + Math.min(0.30D, extraCycles * 0.05D);
    }

    public static double computeHyphalPressureMultiplier(boolean active, int cycleNumber, int mutatorMask) {
        if (!active) {
            return 1.0D;
        }
        int extraCycles = Math.max(0, cycleNumber - 1);
        double base = HiveCycleMutator.HYPHAL_OVERLOAD.isActiveIn(mutatorMask) ? 1.35D : 1.0D;
        return base + Math.min(0.25D, extraCycles * 0.04D);
    }

    public static double computeTideDurationMultiplier(boolean active, int mutatorMask) {
        return (active && HiveCycleMutator.PERPETUAL_TIDE.isActiveIn(mutatorMask)) ? 1.50D : 1.0D;
    }

    public static double computeScoreMultiplier(int cycleNumber, int mutatorMask) {
        int extraCycles = Math.max(0, cycleNumber - 1);
        double mult = 1.0D + extraCycles * 0.25D;
        for (HiveCycleMutator mutator : HiveCycleMutator.values()) {
            if (mutator.isActiveIn(mutatorMask)) {
                mult += mutator.scoreBonusPct() / 100.0D;
            }
        }
        return mult;
    }

    public static int computeCycleScore(int cycleNumber, int mutatorMask, long elapsedTicks, int deathsDuringCycle) {
        double mult = computeScoreMultiplier(cycleNumber, mutatorMask);
        int base = (int) Math.round(BASE_CYCLE_SCORE * mult);
        long safeElapsed = Math.max(0L, elapsedTicks);
        int speedBonus = safeElapsed < SPEED_BONUS_WINDOW_TICKS
                ? (int) Math.round(MAX_SPEED_BONUS * (1.0D - (double) safeElapsed / (double) SPEED_BONUS_WINDOW_TICKS))
                : 0;
        int penalty = Math.max(0, deathsDuringCycle) * DEATH_PENALTY;
        return Math.max(MIN_CYCLE_SCORE, base + speedBonus - penalty);
    }

    public static String resolveRankGrade(int score) {
        if (score >= 2600) {
            return "S+";
        }
        if (score >= 2000) {
            return "S";
        }
        if (score >= 1500) {
            return "A";
        }
        if (score >= 1000) {
            return "B";
        }
        return "C";
    }
}
