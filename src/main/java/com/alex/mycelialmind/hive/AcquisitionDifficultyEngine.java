package com.alex.mycelialmind.hive;

/**
 * Pure progression gating, station/flora maintenance, and field risk-reward
 * rules ensuring high-value mod benefits require earned effort and upkeep.
 */
public final class AcquisitionDifficultyEngine {
    public static final int PYLON_MAX_CHARGE = 100;
    public static final int PYLON_INITIAL_CHARGE = 100;
    public static final int PYLON_COMBINE_RECHARGE_THRESHOLD = 95;

    public static final int TURRET_MAX_AMMO = 64;
    public static final int TURRET_INITIAL_AMMO = 32;

    public static final int BLOOM_PULSES_PER_MATURATION = 6;

    public static final float EXTRACTOR_MAX_HEALTH_RATIO = 0.50F;
    public static final double EXTRACTOR_ALERT_RADIUS = 14.0D;
    public static final double EXTRACTOR_THREAT_SPIKE = 2.0D;

    public static final int RELIC_BACKLASH_HUNGER_TICKS = 160;
    public static final int RELIC_BACKLASH_WEAKNESS_TICKS = 80;
    public static final int RELIC_BACKLASH_FOOD_COST = 4;

    private AcquisitionDifficultyEngine() {
    }

    public static HiveProgressionTier requiredActForTier(int nextTier) {
        return nextTier >= 3 ? HiveProgressionTier.ADAPTIVE : HiveProgressionTier.AWAKENED;
    }

    public static boolean canUpgradeToTier(
            int nextTier,
            HiveProgressionTier effectiveAct,
            boolean hiveCoreDefeated,
            boolean bypassCreative
    ) {
        if (bypassCreative) {
            return true;
        }
        HiveProgressionTier safeAct = effectiveAct != null ? effectiveAct : HiveProgressionTier.DORMANT;
        if (nextTier <= 1) {
            return true;
        }
        if (nextTier == 2) {
            return safeAct.meetsRequiredAct(HiveProgressionTier.AWAKENED) || hiveCoreDefeated;
        }
        return safeAct.meetsRequiredAct(HiveProgressionTier.ADAPTIVE) || hiveCoreDefeated;
    }

    public static boolean canCrystallizeAmber(
            HiveProgressionTier effectiveAct,
            boolean hiveCoreDefeated,
            boolean bypassCreative
    ) {
        if (bypassCreative) {
            return true;
        }
        HiveProgressionTier safeAct = effectiveAct != null ? effectiveAct : HiveProgressionTier.DORMANT;
        return safeAct.meetsRequiredAct(HiveProgressionTier.AWAKENED) || hiveCoreDefeated;
    }

    public static boolean canOpenRift(
            HiveProgressionTier effectiveAct,
            boolean hiveCoreDefeated,
            boolean bypassCreative
    ) {
        if (bypassCreative) {
            return true;
        }
        HiveProgressionTier safeAct = effectiveAct != null ? effectiveAct : HiveProgressionTier.DORMANT;
        return safeAct.meetsRequiredAct(HiveProgressionTier.ADAPTIVE) || hiveCoreDefeated;
    }

    public static boolean canSynthesizeBloomDuplication(
            HiveProgressionTier effectiveAct,
            int effectiveLabTier,
            boolean hiveCoreDefeated
    ) {
        HiveProgressionTier safeAct = effectiveAct != null ? effectiveAct : HiveProgressionTier.DORMANT;
        return safeAct.meetsRequiredAct(HiveProgressionTier.ADAPTIVE)
                || hiveCoreDefeated
                || effectiveLabTier >= 2;
    }

    public static boolean canSynthesizeMutagenOrSerum(
            HiveProgressionTier effectiveAct,
            int effectiveLabTier
    ) {
        HiveProgressionTier safeAct = effectiveAct != null ? effectiveAct : HiveProgressionTier.DORMANT;
        return safeAct.meetsRequiredAct(HiveProgressionTier.AWAKENED) || effectiveLabTier >= 2;
    }

    public static boolean canSynthesizeChimeraOvum(
            HiveProgressionTier effectiveAct,
            int effectiveLabTier,
            boolean hiveCoreDefeated
    ) {
        HiveProgressionTier safeAct = effectiveAct != null ? effectiveAct : HiveProgressionTier.DORMANT;
        return safeAct.meetsRequiredAct(HiveProgressionTier.ADAPTIVE)
                || hiveCoreDefeated
                || effectiveLabTier >= 3;
    }

    public static int computePylonDrain(
            int tier,
            boolean purgedAny,
            boolean inDeepMycelium,
            boolean hasAmberConduit
    ) {
        int rawDrain = 0;
        if (purgedAny) {
            rawDrain = 2;
        } else if (inDeepMycelium || tier >= 2) {
            rawDrain = 1;
        }
        if (rawDrain <= 0) {
            return 0;
        }
        if (hasAmberConduit) {
            return purgedAny ? Math.max(1, rawDrain / 2) : 0;
        }
        return rawDrain;
    }

    public static int pylonRechargeAmount(String itemPath) {
        if (itemPath == null) {
            return 0;
        }
        return switch (itemPath) {
            case "amethyst_shard" -> 25;
            case "latent_spore" -> 15;
            case "mycelial_biomass" -> 10;
            case "synaptic_core" -> 100;
            default -> 0;
        };
    }

    public static int turretAmmoGain(String itemPath) {
        if (itemPath == null) {
            return 0;
        }
        return switch (itemPath) {
            case "latent_spore" -> 16;
            case "amethyst_shard" -> 24;
            case "spore_antidote" -> 32;
            case "concentrated_antidote" -> 64;
            default -> 0;
        };
    }

    public static boolean shouldBloomConsumeMaturation(
            int pulsesUsed,
            boolean hasAdjacentAmber,
            boolean nearActiveNexus
    ) {
        if (hasAdjacentAmber || nearActiveNexus) {
            return false;
        }
        return pulsesUsed >= BLOOM_PULSES_PER_MATURATION;
    }
}
