package com.alex.mycelialmind.hive;

import javax.annotation.Nullable;
import java.util.Locale;

/**
 * Pure catalog of the 10 discoverable Field Engrams that unlock diegetic lore
 * and tactical guidance inside the Mycelial Codex.
 */
public enum FieldEngramId {
    OUTBREAK_SIGNAL("outbreak_signal", 0, 1, "secretos_logros", "engram_outbreak_signal"),
    SYNAPTIC_BLOOM("synaptic_bloom", 1, 1, "secretos_logros", "engram_synaptic_bloom"),
    CHITIN_RESONANCE("chitin_resonance", 2, 2, "secretos_logros", "engram_chitin_resonance"),
    TIDE_PRESSURE("tide_pressure", 3, 3, "secretos_logros", "engram_tide_pressure"),
    MIMIC_DECEPTION("mimic_deception", 4, 3, "secretos_logros", "engram_mimic_deception"),
    HYPHAL_MEMORY("hyphal_memory", 5, 2, "secretos_logros", "engram_hyphal_memory"),
    HOMEOSTASIS_TRAUMA("homeostasis_trauma", 6, 3, "secretos_logros", "engram_homeostasis_trauma"),
    PRIMORDIAL_LINEAGE("primordial_lineage", 7, 4, "secretos_logros", "engram_primordial_lineage"),
    MATRIARCH_REQUIEM("matriarch_requiem", 8, 4, "secretos_logros", "engram_matriarch_requiem"),
    OVERMIND_CYCLE("overmind_cycle", 9, 4, "secretos_logros", "engram_overmind_cycle");

    public static final int TOTAL_COUNT = values().length;
    public static final int ALL_MASK = (1 << TOTAL_COUNT) - 1;

    private final String id;
    private final int bitIndex;
    private final int actNumber;
    private final String codexChapterId;
    private final String codexEntryId;

    FieldEngramId(String id, int bitIndex, int actNumber, String codexChapterId, String codexEntryId) {
        this.id = id;
        this.bitIndex = bitIndex;
        this.actNumber = actNumber;
        this.codexChapterId = codexChapterId;
        this.codexEntryId = codexEntryId;
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

    public int actNumber() {
        return actNumber;
    }

    public String codexChapterId() {
        return codexChapterId;
    }

    public String codexEntryId() {
        return codexEntryId;
    }

    public String titleKey() {
        return "engram.mycelialmind." + id + ".title";
    }

    public String hintKey() {
        return "engram.mycelialmind." + id + ".hint";
    }

    public boolean isUnlocked(int mask) {
        return (mask & bitMask()) != 0;
    }

    public static int countUnlocked(int mask) {
        return Integer.bitCount(mask & ALL_MASK);
    }

    public static FieldEngramId byId(@Nullable String rawId) {
        if (rawId != null && !rawId.isBlank()) {
            String normalized = rawId.trim().toLowerCase(Locale.ROOT);
            for (FieldEngramId value : values()) {
                if (value.id.equals(normalized)) {
                    return value;
                }
            }
        }
        return OUTBREAK_SIGNAL;
    }

    @Nullable
    public static FieldEngramId byIdOrNull(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        for (FieldEngramId value : values()) {
            if (value.id.equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    public static FieldEngramId byBit(int bitIndex) {
        for (FieldEngramId value : values()) {
            if (value.bitIndex == bitIndex) {
                return value;
            }
        }
        return OUTBREAK_SIGNAL;
    }
}
