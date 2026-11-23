package com.alex.mycelialmind.colony;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Specialized functional castes for polymorphic Colony Symbionts.
 */
public enum SymbiontCaste {
    HARVESTER("harvester", 0, 0xFF4AD66D, 24.0D, 4.0D, 0.28D),
    EXCAVATOR("excavator", 1, 0xFFF4A261, 30.0D, 6.0D, 0.25D),
    COURIER("courier", 2, 0xFF4CC9F0, 20.0D, 3.0D, 0.34D),
    ARCHITECT("architect", 3, 0xFFC77DFF, 26.0D, 5.0D, 0.27D),
    SENTINEL("sentinel", 4, 0xFFFF4D8D, 38.0D, 9.0D, 0.29D);

    private final String id;
    private final int index;
    private final int accentColor;
    private final double baseHealth;
    private final double baseAttackDamage;
    private final double baseMoveSpeed;

    SymbiontCaste(
            String id,
            int index,
            int accentColor,
            double baseHealth,
            double baseAttackDamage,
            double baseMoveSpeed
    ) {
        this.id = id;
        this.index = index;
        this.accentColor = accentColor;
        this.baseHealth = baseHealth;
        this.baseAttackDamage = baseAttackDamage;
        this.baseMoveSpeed = baseMoveSpeed;
    }

    public String id() {
        return id;
    }

    public int index() {
        return index;
    }

    public int accentColor() {
        return accentColor;
    }

    public double baseHealth() {
        return baseHealth;
    }

    public double baseAttackDamage() {
        return baseAttackDamage;
    }

    public double baseMoveSpeed() {
        return baseMoveSpeed;
    }

    public String nameKey() {
        return "caste.mycelialmind." + id + ".name";
    }

    public String descKey() {
        return "caste.mycelialmind." + id + ".desc";
    }

    public SymbiontCaste next() {
        SymbiontCaste[] vals = values();
        return vals[(this.ordinal() + 1) % vals.length];
    }

    public static SymbiontCaste fromIndex(int index) {
        SymbiontCaste[] vals = values();
        if (index < 0 || index >= vals.length) {
            return HARVESTER;
        }
        return vals[index];
    }

    @Nullable
    public static SymbiontCaste byId(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String norm = rawId.trim().toLowerCase(Locale.ROOT);
        for (SymbiontCaste caste : values()) {
            if (caste.id.equals(norm)) {
                return caste;
            }
        }
        return null;
    }
}
