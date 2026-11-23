package com.alex.mycelialmind.colony;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Clean-room behavioral temperament traits governing Symbiont work cadence,
 * fatigue buildup, retreat thresholds, morale resilience, and Domain XP yield.
 */
public enum SymbiontTemperament {
    DILIGENT("diligent", 0, 1.18D, 0.25F, 1.15D, 0.95D, 1.00D, 1.05D),
    CAUTIOUS("cautious", 1, 0.96D, 0.45F, 0.88D, 1.15D, 0.90D, 1.00D),
    ZEALOUS("zealous", 2, 1.10D, 0.15F, 1.08D, 0.95D, 1.22D, 1.10D),
    CURIOUS("curious", 3, 1.06D, 0.30F, 0.95D, 1.08D, 1.00D, 1.25D),
    STOIC("stoic", 4, 1.00D, 0.20F, 0.72D, 1.25D, 1.05D, 1.00D);

    private final String id;
    private final int index;
    private final double workSpeedMultiplier;
    private final float retreatHealthRatio;
    private final double fatigueRateMultiplier;
    private final double moraleStabilityMultiplier;
    private final double combatDamageMultiplier;
    private final double domainXpMultiplier;

    SymbiontTemperament(
            String id,
            int index,
            double workSpeedMultiplier,
            float retreatHealthRatio,
            double fatigueRateMultiplier,
            double moraleStabilityMultiplier,
            double combatDamageMultiplier,
            double domainXpMultiplier
    ) {
        this.id = id;
        this.index = index;
        this.workSpeedMultiplier = workSpeedMultiplier;
        this.retreatHealthRatio = retreatHealthRatio;
        this.fatigueRateMultiplier = fatigueRateMultiplier;
        this.moraleStabilityMultiplier = moraleStabilityMultiplier;
        this.combatDamageMultiplier = combatDamageMultiplier;
        this.domainXpMultiplier = domainXpMultiplier;
    }

    public String id() {
        return id;
    }

    public int index() {
        return index;
    }

    public double workSpeedMultiplier() {
        return workSpeedMultiplier;
    }

    public float retreatHealthRatio() {
        return retreatHealthRatio;
    }

    public double fatigueRateMultiplier() {
        return fatigueRateMultiplier;
    }

    public double moraleStabilityMultiplier() {
        return moraleStabilityMultiplier;
    }

    public double combatDamageMultiplier() {
        return combatDamageMultiplier;
    }

    public double domainXpMultiplier() {
        return domainXpMultiplier;
    }

    public String nameKey() {
        return "temperament.mycelialmind." + id + ".name";
    }

    public static SymbiontTemperament fromIndex(int index) {
        SymbiontTemperament[] vals = values();
        if (index < 0 || index >= vals.length) {
            return DILIGENT;
        }
        return vals[index];
    }

    @Nullable
    public static SymbiontTemperament byId(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String norm = rawId.trim().toLowerCase(Locale.ROOT);
        for (SymbiontTemperament value : values()) {
            if (value.id.equals(norm)) {
                return value;
            }
        }
        return null;
    }
}
