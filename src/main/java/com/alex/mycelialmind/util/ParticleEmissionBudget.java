package com.alex.mycelialmind.util;

/** Pure math for particle emission budgets (kept client-free for JUnit/Pitest). */
public final class ParticleEmissionBudget {
    /** Hard cap for a single emission call; every tuned burst stays at or below this. */
    public static final int MAX_PER_CALL = 24;
    /** Soft cap for a composed scene (several types fired together). */
    public static final int MAX_SCENE_TOTAL = 48;

    public static int clamp(int requested) {
        return Math.max(0, Math.min(MAX_PER_CALL, requested));
    }

    public static int scaled(int requested, double densityScale) {
        double safe = Double.isFinite(densityScale) ? Math.max(0.25D, Math.min(1.5D, densityScale)) : 1.0D;
        return clamp((int) Math.round(requested * safe));
    }

    public static int composition(int first, int second, int third) {
        return Math.min(MAX_SCENE_TOTAL, Math.max(0, first) + Math.max(0, second) + Math.max(0, third));
    }

    private ParticleEmissionBudget() {}
}
