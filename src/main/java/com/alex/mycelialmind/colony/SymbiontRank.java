package com.alex.mycelialmind.colony;

/**
 * Individual progression ranks for Colony Symbionts earned by executing orders
 * and defending the Sovereign Colony.
 */
public enum SymbiontRank {
    NOVICE(0, "novice", 0, 1.00D, 1.00D, 1.00D, 1.00F),
    ADEPT(1, "adept", 100, 1.25D, 1.20D, 1.20D, 1.08F),
    SOVEREIGN(2, "sovereign", 320, 1.55D, 1.45D, 1.45D, 1.18F);

    private final int index;
    private final String id;
    private final int requiredXp;
    private final double healthMultiplier;
    private final double damageMultiplier;
    private final double workEfficiencyMultiplier;
    private final float visualScale;

    SymbiontRank(
            int index,
            String id,
            int requiredXp,
            double healthMultiplier,
            double damageMultiplier,
            double workEfficiencyMultiplier,
            float visualScale
    ) {
        this.index = index;
        this.id = id;
        this.requiredXp = requiredXp;
        this.healthMultiplier = healthMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.workEfficiencyMultiplier = workEfficiencyMultiplier;
        this.visualScale = visualScale;
    }

    public int index() {
        return index;
    }

    public String id() {
        return id;
    }

    public int requiredXp() {
        return requiredXp;
    }

    public double healthMultiplier() {
        return healthMultiplier;
    }

    public double damageMultiplier() {
        return damageMultiplier;
    }

    public double workEfficiencyMultiplier() {
        return workEfficiencyMultiplier;
    }

    public float visualScale() {
        return visualScale;
    }

    public String nameKey() {
        return "symbiont_rank.mycelialmind." + id + ".name";
    }

    public static SymbiontRank fromIndex(int index) {
        SymbiontRank[] vals = values();
        if (index < 0 || index >= vals.length) {
            return NOVICE;
        }
        return vals[index];
    }

    public static SymbiontRank forXp(int xp) {
        int clamped = Math.max(0, xp);
        if (clamped >= SOVEREIGN.requiredXp) {
            return SOVEREIGN;
        }
        if (clamped >= ADEPT.requiredXp) {
            return ADEPT;
        }
        return NOVICE;
    }
}
