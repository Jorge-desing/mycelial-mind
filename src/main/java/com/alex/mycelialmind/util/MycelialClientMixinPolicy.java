package com.alex.mycelialmind.util;

/**
 * Pure, headless rules for client-side Mixin hooks (Synaptic Vision outlines, night-vision clarity,
 * and diegetic HUD heart styling). Kept free of {@code net.minecraft.client.*} classes so JUnit tests
 * can verify every edge case without FML or LWJGL bootstrap.
 */
public final class MycelialClientMixinPolicy {
    public static final double SYNAPTIC_VISION_GLOW_RANGE = 32.0D;
    public static final double DISGUISED_MIMIC_GLOW_RANGE = 6.0D;

    public static final float SYNAPTIC_VISION_NIGHT_SCALE = 0.35F;
    public static final int SYNAPTIC_VISION_FADE_TICKS = 100;

    public static final int OUTLINE_COLOR_BOSS = 0xC77DFF;
    public static final int OUTLINE_COLOR_ELITE = 0xFF4D6D;
    public static final int OUTLINE_COLOR_MUTATED = 0xFFB703;
    public static final int OUTLINE_COLOR_BASE = 0x00F5D4;
    public static final int OUTLINE_COLOR_ALLY = 0x38B000;

    public enum DiegeticHeartStyle {
        VANILLA,
        MYCELIAL_INFECTION,
        SPORE_ROT
    }

    public enum OutlineCategory {
        NONE,
        BOSS,
        ELITE,
        MUTATED,
        BASE,
        ALLY
    }

    private MycelialClientMixinPolicy() {
    }

    public static boolean shouldSynapticVisionGlow(
            boolean original,
            boolean viewerHasSynapticVision,
            boolean targetIsMycelialMob,
            boolean targetIsDisguisedMimic,
            double distanceSq) {
        if (original) {
            return true;
        }
        if (!viewerHasSynapticVision || !targetIsMycelialMob || distanceSq < 0.0D || !Double.isFinite(distanceSq)) {
            return false;
        }
        double maxRange = targetIsDisguisedMimic ? DISGUISED_MIMIC_GLOW_RANGE : SYNAPTIC_VISION_GLOW_RANGE;
        return distanceSq <= (maxRange * maxRange);
    }

    public static boolean shouldEnableSynapticNightVision(boolean vanillaHasNightVision, boolean hasSynapticVision) {
        return vanillaHasNightVision || hasSynapticVision;
    }

    public static float computeSynapticNightVisionScale(
            float vanillaScale,
            boolean hasSynapticVision,
            int synapticDurationTicks,
            float partialTicks) {
        float safeVanilla = Float.isFinite(vanillaScale) ? Math.max(0.0F, vanillaScale) : 0.0F;
        if (!hasSynapticVision || synapticDurationTicks == 0) {
            return safeVanilla;
        }
        float fadeFactor;
        if (synapticDurationTicks < 0 || synapticDurationTicks > SYNAPTIC_VISION_FADE_TICKS) {
            fadeFactor = 1.0F;
        } else {
            float clampedPartial = Float.isFinite(partialTicks) ? Math.max(0.0F, Math.min(1.0F, partialTicks)) : 0.0F;
            fadeFactor = Math.max(0.0F, Math.min(1.0F, ((float) synapticDurationTicks - clampedPartial) / (float) SYNAPTIC_VISION_FADE_TICKS));
        }
        float synapticScale = SYNAPTIC_VISION_NIGHT_SCALE * fadeFactor;
        return Math.max(safeVanilla, synapticScale);
    }

    public static DiegeticHeartStyle resolveDiegeticHeartStyle(
            boolean isNormalHeart,
            boolean hasSporeRot,
            boolean hasMycelialInfection) {
        if (!isNormalHeart) {
            return DiegeticHeartStyle.VANILLA;
        }
        if (hasSporeRot) {
            return DiegeticHeartStyle.SPORE_ROT;
        }
        if (hasMycelialInfection) {
            return DiegeticHeartStyle.MYCELIAL_INFECTION;
        }
        return DiegeticHeartStyle.VANILLA;
    }

    public static int resolveSynapticOutlineColor(
            int originalColor,
            boolean hasExplicitVanillaTeam,
            boolean viewerHasSynapticVision,
            boolean targetIsDisguisedMimic,
            OutlineCategory category) {
        if (hasExplicitVanillaTeam
                || !viewerHasSynapticVision
                || targetIsDisguisedMimic
                || category == null
                || category == OutlineCategory.NONE) {
            return originalColor;
        }
        return switch (category) {
            case BOSS -> OUTLINE_COLOR_BOSS;
            case ELITE -> OUTLINE_COLOR_ELITE;
            case MUTATED -> OUTLINE_COLOR_MUTATED;
            case BASE -> OUTLINE_COLOR_BASE;
            case ALLY -> OUTLINE_COLOR_ALLY;
            case NONE -> originalColor;
        };
    }

    public static boolean shouldEntityShake(
            boolean originalShaking,
            boolean hasSporeRot,
            boolean isHostileNonBossMycelialMob,
            int hiveSuppressionSecondsRemaining) {
        if (originalShaking) {
            return true;
        }
        return hasSporeRot || (isHostileNonBossMycelialMob && hiveSuppressionSecondsRemaining > 0);
    }

    public static boolean shouldSuppressVanillaMusic(
            boolean bossNearby,
            boolean fungalTideActiveOutsidePylon,
            boolean inDeepMycelium,
            boolean homeostasisElevated) {
        return bossNearby || fungalTideActiveOutsidePylon || (inDeepMycelium && homeostasisElevated);
    }

    public static boolean shouldShowSporeRotFoodSprites(
            boolean vanillaHasHunger,
            boolean hasSporeRot,
            boolean inProtectedZone) {
        if (vanillaHasHunger) {
            return true;
        }
        return hasSporeRot && !inProtectedZone;
    }
}
