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

    public enum DiegeticHeartStyle {
        VANILLA,
        MYCELIAL_INFECTION,
        SPORE_ROT
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
}
