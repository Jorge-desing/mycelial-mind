package com.alex.mycelialmind.util;

/**
 * Pure, headless anti-cheese balance and decision rules for Mycelial Mind Mixin hooks.
 * Kept free of Minecraft registries so unit tests can exercise every edge case without FML bootstrap.
 */
public final class MycelialMixinPolicy {
    public static final double MIN_VISIBILITY_FLOOR = 0.55D;
    public static final double PIECE_VISIBILITY_REDUCTION = 0.08D;
    public static final double ELITE_EXTRA_REDUCTION = 0.03D;
    public static final int COMBAT_LOCKOUT_TICKS = 100;

    public static final float MOB_MYCELIAL_TRACTION_CAP = 1.08F;
    public static final float PLAYER_FULL_SET_TRACTION_CAP = 1.10F;
    public static final float PLAYER_BOOTS_TRACTION_CAP = 1.06F;

    public static final float HIGH_FALL_ANTI_CHEESE_DISTANCE = 14.0F;
    public static final double BASE_FALL_REDUCTION = 0.30D;
    public static final double CHITIN_BOOTS_FALL_REDUCTION = 0.45D;
    public static final double HIGH_FALL_MIN_RETAINED_RATIO = 0.50D;

    private MycelialMixinPolicy() {
    }

    public static boolean isRecentlyInCombat(int tickCount, int lastHurtMobTimestamp, int lastHurtByMobTimestamp) {
        if (lastHurtMobTimestamp > 0 && tickCount >= lastHurtMobTimestamp
                && (tickCount - lastHurtMobTimestamp) < COMBAT_LOCKOUT_TICKS) {
            return true;
        }
        return lastHurtByMobTimestamp > 0 && tickCount >= lastHurtByMobTimestamp
                && (tickCount - lastHurtByMobTimestamp) < COMBAT_LOCKOUT_TICKS;
    }

    public static double computePheromoneVisibility(
            double originalVisibility,
            boolean observerIsNonBossMycelialMob,
            int chitinPieces,
            int eliteChitinPieces,
            boolean sprinting,
            boolean onFire,
            boolean infectedOrRotted,
            boolean recentlyInCombat) {
        if (originalVisibility <= 0.0D || !observerIsNonBossMycelialMob || chitinPieces <= 0) {
            return originalVisibility;
        }
        if (sprinting || onFire || infectedOrRotted || recentlyInCombat) {
            return originalVisibility;
        }
        int clampedPieces = Math.min(4, chitinPieces);
        int clampedElite = Math.min(clampedPieces, Math.max(0, eliteChitinPieces));
        double totalReduction = Math.min(
                0.45D,
                clampedPieces * PIECE_VISIBILITY_REDUCTION + clampedElite * ELITE_EXTRA_REDUCTION);
        double scaled = originalVisibility * (1.0D - totalReduction);
        double floor = Math.min(originalVisibility, MIN_VISIBILITY_FLOOR);
        return Math.max(floor, scaled);
    }

    public static float computeMycelialSpeedFactor(
            float originalFactor,
            boolean onMycelialSurface,
            boolean isMycelialMob,
            boolean hasFullChitinSet,
            boolean hasEvolvedChitinBoots,
            boolean inLiquidOrAirborne) {
        if (!onMycelialSurface || inLiquidOrAirborne || originalFactor <= 0.0F) {
            return originalFactor;
        }
        float multiplier;
        float cap;
        if (isMycelialMob) {
            multiplier = 1.08F;
            cap = MOB_MYCELIAL_TRACTION_CAP;
        } else if (hasFullChitinSet) {
            multiplier = 1.10F;
            cap = PLAYER_FULL_SET_TRACTION_CAP;
        } else if (hasEvolvedChitinBoots) {
            multiplier = 1.06F;
            cap = PLAYER_BOOTS_TRACTION_CAP;
        } else {
            return originalFactor;
        }
        if (originalFactor >= cap) {
            return originalFactor;
        }
        return Math.min(cap, originalFactor * multiplier);
    }

    public static int computeMycelialFallDamage(
            int originalDamage,
            float fallDistance,
            boolean onCushionedMycelialBlock,
            boolean wearingChitinBoots) {
        if (!onCushionedMycelialBlock || originalDamage <= 0) {
            return originalDamage;
        }
        double reduction = wearingChitinBoots ? CHITIN_BOOTS_FALL_REDUCTION : BASE_FALL_REDUCTION;
        int reduced = (int) Math.floor(originalDamage * (1.0D - reduction));
        if (fallDistance >= HIGH_FALL_ANTI_CHEESE_DISTANCE || originalDamage >= 10) {
            int antiCheeseFloor = Math.max(1, (int) Math.ceil(originalDamage * HIGH_FALL_MIN_RETAINED_RATIO));
            return Math.max(antiCheeseFloor, reduced);
        }
        return Math.max(0, reduced);
    }
}
