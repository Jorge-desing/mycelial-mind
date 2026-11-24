package com.alex.mycelialmind.hive;

/**
 * Pure, common-safe mathematical helpers for UX/Juice animations, camera micro-shake,
 * perimeter status vignettes, smoothed HUD bars, and diegetic station render states.
 */
public final class UxJuiceEngine {
    public static final float MAX_SHAKE_DEGREES = 2.5F;
    public static final float MAX_VIGNETTE_ALPHA = 0.40F;

    public record VignetteState(int argb, float alpha) {
    }

    public record StationVisualState(float speedMultiplier, float bobAmplitude, int tintArgb) {
    }

    public record TurretVisualState(float aimLerpSpeed, int tintArgb) {
    }

    private UxJuiceEngine() {
    }

    public static float computeShakeDecay(float current, float decayPerTick) {
        if (!Float.isFinite(current) || current <= 0.0F) {
            return 0.0F;
        }
        float decay = Float.isFinite(decayPerTick) ? Math.max(0.0F, decayPerTick) : 0.0F;
        return Math.max(0.0F, current - decay);
    }

    public static float[] computeShakeAngles(float intensity, float timeTicks, float scale) {
        if (!Float.isFinite(intensity) || !Float.isFinite(scale) || intensity <= 0.0F || scale <= 0.0F) {
            return new float[]{0.0F, 0.0F, 0.0F};
        }
        float clampedIntensity = Math.min(1.0F, intensity);
        float clampedScale = Math.min(1.0F, scale);
        float amplitude = Math.min(MAX_SHAKE_DEGREES, clampedIntensity * clampedScale * MAX_SHAKE_DEGREES);
        if (amplitude <= 0.001F) {
            return new float[]{0.0F, 0.0F, 0.0F};
        }
        float safeTime = Float.isFinite(timeTicks) ? timeTicks : 0.0F;
        float yaw = clampAngle((float) Math.sin(safeTime * 2.7F) * amplitude * 0.65F);
        float pitch = clampAngle((float) Math.cos(safeTime * 3.3F) * amplitude * 0.55F);
        float roll = clampAngle((float) Math.sin(safeTime * 4.1F + 0.8F) * amplitude);
        return new float[]{yaw, pitch, roll};
    }

    private static float clampAngle(float angle) {
        return Math.max(-MAX_SHAKE_DEGREES, Math.min(MAX_SHAKE_DEGREES, angle));
    }

    public static VignetteState computeVignetteState(
            boolean infected,
            boolean sporeRot,
            boolean suppressed,
            boolean highThreat,
            float pulsePhase,
            float scale
    ) {
        if (!Float.isFinite(scale) || scale <= 0.0F) {
            return new VignetteState(0, 0.0F);
        }
        float clampedScale = Math.min(1.0F, scale);
        float safePhase = Float.isFinite(pulsePhase) ? pulsePhase : 0.0F;
        float pulse = 0.5F + 0.5F * (float) Math.sin(safePhase);

        int baseRgb;
        float rawAlpha;
        if (sporeRot) {
            baseRgb = 0x9D4EDD;
            rawAlpha = 0.24F + pulse * 0.14F;
        } else if (infected) {
            baseRgb = 0x6EEB83;
            rawAlpha = 0.18F + pulse * 0.12F;
        } else if (suppressed) {
            baseRgb = 0x00F5D4;
            rawAlpha = 0.16F + pulse * 0.10F;
        } else if (highThreat) {
            baseRgb = 0xE63946;
            rawAlpha = 0.10F + pulse * 0.08F;
        } else {
            return new VignetteState(0, 0.0F);
        }
        float alpha = Math.max(0.0F, Math.min(MAX_VIGNETTE_ALPHA, rawAlpha * clampedScale));
        int alphaByte = Math.round(alpha * 255.0F) & 0xFF;
        return new VignetteState((alphaByte << 24) | (baseRgb & 0x00FFFFFF), alpha);
    }

    public static float lerpBarValue(float displayed, float target, float factor) {
        if (!Float.isFinite(target)) {
            return 0.0F;
        }
        if (!Float.isFinite(displayed)) {
            return target;
        }
        float t = Float.isFinite(factor) ? Math.max(0.0F, Math.min(1.0F, factor)) : 1.0F;
        if (Math.abs(target - displayed) < 0.005F) {
            return target;
        }
        return displayed + (target - displayed) * t;
    }

    public static float computeBarPulseAlpha(float delta) {
        if (!Float.isFinite(delta) || Math.abs(delta) < 0.01F) {
            return 0.0F;
        }
        return Math.min(1.0F, Math.abs(delta) * 0.25F);
    }

    public static StationVisualState computePylonVisualState(int charge, int maxCharge) {
        if (charge <= 0 || maxCharge <= 0) {
            return new StationVisualState(0.0F, 0.0F, 0xA06E7885);
        }
        float ratio = (float) charge / (float) maxCharge;
        if (ratio <= 0.20F) {
            return new StationVisualState(0.70F, 0.014F, 0xEBFFB703);
        }
        return new StationVisualState(1.60F, 0.025F, 0xEB82F5FF);
    }

    public static TurretVisualState computeTurretVisualState(int ammo, int maxAmmo, boolean jammed, int tier) {
        if (jammed || ammo <= 0 || maxAmmo <= 0) {
            return new TurretVisualState(0.06F, 0xFFE63946);
        }
        if (ammo <= 6) {
            return new TurretVisualState(0.14F, 0xFFFFB703);
        }
        int healthyTint = tier >= 3 ? 0xF5FFE182 : 0xF58CF5FF;
        return new TurretVisualState(tier >= 3 ? 0.24F : 0.18F, healthyTint);
    }

    public static StationVisualState computeBenchVisualState(int effectiveLabTier, boolean hasOutput, boolean blockedByAct) {
        if (blockedByAct) {
            return new StationVisualState(0.60F, 0.008F, 0xEBFFB703);
        }
        if (hasOutput) {
            return new StationVisualState(2.80F, 0.024F, 0xEB72EFDD);
        }
        float speed = 1.40F + Math.max(0, effectiveLabTier - 1) * 0.50F;
        int tint = effectiveLabTier >= 3 ? 0xEBFFD166 : (effectiveLabTier == 2 ? 0xEB9DE0AD : 0xEB78FFDC);
        return new StationVisualState(speed, 0.015F, tint);
    }
}
