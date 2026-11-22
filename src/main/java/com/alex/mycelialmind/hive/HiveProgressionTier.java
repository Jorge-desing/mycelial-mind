package com.alex.mycelialmind.hive;

import javax.annotation.Nullable;

/**
 * Escalating progression stages of the Hive Mind based on global threat level.
 * Drives continuous stat scaling on spawned MycelialMobs, wave promotion pressure,
 * and HUD stage telemetry so players feel tangible progression difficulty.
 */
public enum HiveProgressionTier {
    DORMANT(0, "DORMANT", 0.0, 1.00, 1.00, 0.0, 1.00, 0.00, 0.85),
    AWAKENED(1, "AWAKENED", 25.0, 1.15, 1.10, 1.0, 1.03, 0.05, 1.00),
    ADAPTIVE(2, "ADAPTIVE", 50.0, 1.30, 1.22, 2.5, 1.06, 0.12, 1.15),
    OVERMIND(3, "OVERMIND", 75.0, 1.50, 1.35, 4.0, 1.10, 0.20, 1.30);

    public static final double DEEP_MYCELIUM_HEALTH_BONUS = 0.25;
    public static final double DEEP_MYCELIUM_DAMAGE_BONUS = 0.20;
    public static final double DEEP_MYCELIUM_ARMOR_BONUS = 3.0;
    public static final double DEEP_MYCELIUM_SPEED_BONUS = 0.05;

    private final int level;
    private final String displayTag;
    private final double minThreat;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double armorBonus;
    private final double speedMultiplier;
    private final double wavePromotionBonus;
    private final double deepMyceliumStressMultiplier;

    HiveProgressionTier(
            int level,
            String displayTag,
            double minThreat,
            double healthMultiplier,
            double damageMultiplier,
            double armorBonus,
            double speedMultiplier,
            double wavePromotionBonus,
            double deepMyceliumStressMultiplier
    ) {
        this.level = level;
        this.displayTag = displayTag;
        this.minThreat = minThreat;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.armorBonus = armorBonus;
        this.speedMultiplier = speedMultiplier;
        this.wavePromotionBonus = wavePromotionBonus;
        this.deepMyceliumStressMultiplier = deepMyceliumStressMultiplier;
    }

    public int level() {
        return level;
    }

    public String displayTag() {
        return displayTag;
    }

    public double minThreat() {
        return minThreat;
    }

    public double healthMultiplier() {
        return healthMultiplier;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double armorBonus() {
        return armorBonus;
    }

    public double speedMultiplier() {
        return speedMultiplier;
    }

    public double wavePromotionBonus() {
        return wavePromotionBonus;
    }

    public double deepMyceliumStressMultiplier() {
        return deepMyceliumStressMultiplier;
    }

    public int actNumber() {
        return level + 1;
    }

    public boolean meetsRequiredAct(@Nullable HiveProgressionTier required) {
        return required == null || this.level >= required.level;
    }

    public float primordialSpawnChance(boolean inDeepMycelium, boolean fungalTideActive, boolean inRemission) {
        float base = switch (this) {
            case DORMANT -> 0.015F;
            case AWAKENED -> 0.035F;
            case ADAPTIVE -> 0.055F;
            case OVERMIND -> 0.080F;
        };
        if (inDeepMycelium || fungalTideActive) {
            base += 0.030F;
        }
        if (inRemission) {
            base *= 0.50F;
        }
        return Math.max(0.005F, Math.min(0.120F, base));
    }

    public float primordialAllyHealAmount(boolean inRemission) {
        float heal = switch (this) {
            case DORMANT -> 0.75F;
            case AWAKENED -> 1.0F;
            case ADAPTIVE -> 1.5F;
            case OVERMIND -> 2.0F;
        };
        return inRemission ? heal * 0.5F : heal;
    }

    public float floraRegrowthMultiplier(boolean inRemission) {
        float mult = switch (this) {
            case DORMANT -> 1.00F;
            case AWAKENED -> 1.10F;
            case ADAPTIVE -> 1.20F;
            case OVERMIND -> 1.30F;
        };
        return inRemission ? mult * 1.50F : mult;
    }

    public float floraBonusYieldChance() {
        return switch (this) {
            case DORMANT -> 0.00F;
            case AWAKENED -> 0.15F;
            case ADAPTIVE -> 0.25F;
            case OVERMIND -> 0.35F;
        };
    }

    public float pylonTier3PurgeDamage() {
        return switch (this) {
            case DORMANT, AWAKENED -> 1.0F;
            case ADAPTIVE -> 1.5F;
            case OVERMIND -> 2.0F;
        };
    }

    public static HiveProgressionTier requiredActForSecret(int secretBit) {
        return switch (secretBit) {
            case 0, 2 -> AWAKENED;
            case 3 -> ADAPTIVE;
            default -> DORMANT;
        };
    }

    public static HiveProgressionTier resolveEffectiveTier(double currentThreat, int highestTierReached) {
        HiveProgressionTier current = fromThreat(currentThreat);
        HiveProgressionTier[] vals = values();
        int clamped = Math.max(0, Math.min(OVERMIND.level, highestTierReached));
        return vals[Math.max(current.level, clamped)];
    }

    public static boolean isRiftResonantTier(HiveProgressionTier tier) {
        return tier != null && tier.level >= ADAPTIVE.level;
    }

    public static double clampThreat(double threat) {
        if (!Double.isFinite(threat)) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(100.0, threat));
    }

    public static double computeSyncedDeepThreat(double overworldThreat, double currentDeepThreat) {
        double safeOw = clampThreat(overworldThreat);
        double safeDeep = clampThreat(currentDeepThreat);
        return Math.max(safeDeep, safeOw * 0.85);
    }

    public static double computePostMatriarchThreat(double currentThreat) {
        return clampThreat(clampThreat(currentThreat) * 0.65);
    }

    public static HiveProgressionTier fromThreat(double threat) {
        if (Double.isNaN(threat) || threat < AWAKENED.minThreat) {
            return DORMANT;
        }
        if (threat < ADAPTIVE.minThreat) {
            return AWAKENED;
        }
        if (threat < OVERMIND.minThreat) {
            return ADAPTIVE;
        }
        return OVERMIND;
    }
}
