package com.alex.mycelialmind.colony;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Global adaptive Sovereign Hive traits unlocked as the player's colony
 * accumulates Domain XP across harvests, excavations, deliveries, repairs, and raid defenses.
 */
public enum SovereignTrait {
    SYMBIOTIC_HARVEST("symbiotic_harvest", 0, 120, 0xFF4AD66D),
    DEEP_QUARRY_SENSE("deep_quarry_sense", 1, 280, 0xFFF4A261),
    HYPHAL_LOGISTICS("hyphal_logistics", 2, 480, 0xFF4CC9F0),
    LIVING_BULWARK("living_bulwark", 3, 720, 0xFFC77DFF),
    APEX_GUARDIAN_MATRIX("apex_guardian_matrix", 4, 1000, 0xFFFF4D8D);

    public static final int ALL_MASK = (1 << values().length) - 1;

    private final String id;
    private final int bitIndex;
    private final int unlockDomainXp;
    private final int accentColor;

    SovereignTrait(String id, int bitIndex, int unlockDomainXp, int accentColor) {
        this.id = id;
        this.bitIndex = bitIndex;
        this.unlockDomainXp = unlockDomainXp;
        this.accentColor = accentColor;
    }

    public String id() {
        return id;
    }

    public int bitIndex() {
        return bitIndex;
    }

    public int bitMask() {
        return 1 << bitIndex;
    }

    public int unlockDomainXp() {
        return unlockDomainXp;
    }

    public int accentColor() {
        return accentColor;
    }

    public String nameKey() {
        return "sovereign_trait.mycelialmind." + id + ".name";
    }

    public String descKey() {
        return "sovereign_trait.mycelialmind." + id + ".desc";
    }

    public boolean isUnlockedIn(int mask) {
        return (mask & bitMask()) != 0;
    }

    public static int computeUnlockedMask(int domainXp) {
        int xp = Math.max(0, domainXp);
        int mask = 0;
        for (SovereignTrait trait : values()) {
            if (xp >= trait.unlockDomainXp) {
                mask |= trait.bitMask();
            }
        }
        return mask;
    }

    public static List<SovereignTrait> decodeMask(int mask) {
        List<SovereignTrait> unlocked = new ArrayList<>();
        for (SovereignTrait trait : values()) {
            if (trait.isUnlockedIn(mask)) {
                unlocked.add(trait);
            }
        }
        return unlocked;
    }

    @Nullable
    public static SovereignTrait byId(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String norm = rawId.trim().toLowerCase(Locale.ROOT);
        for (SovereignTrait trait : values()) {
            if (trait.id.equals(norm)) {
                return trait;
            }
        }
        return null;
    }
}
