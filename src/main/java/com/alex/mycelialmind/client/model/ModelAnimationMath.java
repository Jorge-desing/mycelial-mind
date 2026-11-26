package com.alex.mycelialmind.client.model;

import net.minecraft.util.Mth;

/**
 * Shared procedural animation math helpers used by generated entity models
 * (critically-damped springs and FABRIK / two-bone inverse kinematics).
 */
public final class ModelAnimationMath {
    private ModelAnimationMath() {
    }

    public static float damperExact(float x, float goal, float halflife, float dt) {
        float y = (4.0F * 0.6931472F) / Math.max(1.0E-4F, halflife);
        float ydt = y * dt;
        float e = 1.0F / (1.0F + ydt + 0.48F * ydt * ydt + 0.235F * ydt * ydt * ydt);
        return goal + (x - goal) * e;
    }

    public static void stepSpringDamperExact(float[] state, int offset, float targetGoal, float halflife, float dt) {
        float y = (4.0F * 0.6931472F) / Math.max(1.0E-4F, halflife);
        float ydt = y * dt;
        float e = 1.0F / (1.0F + ydt + 0.48F * ydt * ydt + 0.235F * ydt * ydt * ydt);
        float j0 = state[offset] - targetGoal;
        float j1 = state[offset + 1] + j0 * y;
        state[offset] = Mth.clamp(e * (j0 + j1 * dt) + targetGoal, -0.45F, 0.45F);
        state[offset + 1] = e * (state[offset + 1] - j1 * y * dt);
    }

    public static float solveTwoBoneIK(float targetDist, float l1, float l2) {
        float clamped = Mth.clamp(targetDist, Math.abs(l1 - l2) + 0.05F, l1 + l2 - 0.05F);
        float cosAngle = (l1 * l1 + l2 * l2 - clamped * clamped) / (2.0F * l1 * l2);
        return (float) Math.acos(Mth.clamp(cosAngle, -1.0F, 1.0F));
    }

    public static float[] solveFabrikChainAngles(float targetX, float targetY, float[] lengths, int iterations) {
        int n = lengths.length;
        float[] px = new float[n + 1];
        float[] py = new float[n + 1];
        for (int i = 0; i < n; i++) {
            px[i + 1] = px[i] + lengths[i];
            py[i + 1] = 0.0F;
        }
        for (int iter = 0; iter < Math.max(1, iterations); iter++) {
            px[n] = targetX;
            py[n] = targetY;
            for (int i = n - 1; i >= 0; i--) {
                float dx = px[i] - px[i + 1];
                float dy = py[i] - py[i + 1];
                float d = Math.max(1.0E-4F, (float) Math.sqrt(dx * dx + dy * dy));
                px[i] = px[i + 1] + (dx / d) * lengths[i];
                py[i] = py[i + 1] + (dy / d) * lengths[i];
            }
            px[0] = 0.0F;
            py[0] = 0.0F;
            for (int i = 0; i < n; i++) {
                float dx = px[i + 1] - px[i];
                float dy = py[i + 1] - py[i];
                float d = Math.max(1.0E-4F, (float) Math.sqrt(dx * dx + dy * dy));
                px[i + 1] = px[i] + (dx / d) * lengths[i];
                py[i + 1] = py[i] + (dy / d) * lengths[i];
            }
        }
        float[] angles = new float[n];
        for (int i = 0; i < n; i++) {
            angles[i] = (float) Math.atan2(py[i + 1] - py[i], px[i + 1] - px[i]);
        }
        return angles;
    }
}
