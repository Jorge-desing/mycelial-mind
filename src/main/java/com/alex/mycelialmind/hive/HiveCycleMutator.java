package com.alex.mycelialmind.hive;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Post-Matriarch tactical mutators selectable (1..3 active) at the Matriarch Altar
 * to configure each Hive Cycle run.
 */
public enum HiveCycleMutator {
    HYPER_ADAPTATION("hyper_adaptation", 0, 35, 0xFFF4A261),
    PERPETUAL_TIDE("perpetual_tide", 1, 40, 0xFFFF4D8D),
    ACUTE_HOMEOSTASIS("acute_homeostasis", 2, 35, 0xFFFF758F),
    SYNAPTIC_SWARM("synaptic_swarm", 3, 40, 0xFFC77DFF),
    PRIMORDIAL_APEX("primordial_apex", 4, 50, 0xFFFFD166),
    HYPHAL_OVERLOAD("hyphal_overload", 5, 30, 0xFF72EFDD);

    public static final int MAX_ACTIVE_MUTATORS = 3;
    public static final int ALL_MASK = (1 << values().length) - 1;

    private final String id;
    private final int bitIndex;
    private final int scoreBonusPct;
    private final int colorRgb;

    HiveCycleMutator(String id, int bitIndex, int scoreBonusPct, int colorRgb) {
        this.id = id;
        this.bitIndex = bitIndex;
        this.scoreBonusPct = scoreBonusPct;
        this.colorRgb = colorRgb;
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

    public int scoreBonusPct() {
        return scoreBonusPct;
    }

    public int colorRgb() {
        return colorRgb;
    }

    public String nameKey() {
        return "mutator.mycelialmind." + id + ".name";
    }

    public String descKey() {
        return "mutator.mycelialmind." + id + ".desc";
    }

    public boolean isActiveIn(int mask) {
        return (mask & bitMask()) != 0;
    }

    public static int countActive(int mask) {
        return Integer.bitCount(mask & ALL_MASK);
    }

    public static List<HiveCycleMutator> decodeMask(int mask) {
        List<HiveCycleMutator> out = new ArrayList<>();
        for (HiveCycleMutator mutator : values()) {
            if (mutator.isActiveIn(mask)) {
                out.add(mutator);
            }
        }
        return out;
    }

    @Nullable
    public static HiveCycleMutator byId(@Nullable String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return null;
        }
        String normalized = rawId.trim().toLowerCase(Locale.ROOT);
        for (HiveCycleMutator value : values()) {
            if (value.id.equals(normalized)) {
                return value;
            }
        }
        return null;
    }

    public static HiveCycleMutator byBit(int bitIndex) {
        for (HiveCycleMutator value : values()) {
            if (value.bitIndex == bitIndex) {
                return value;
            }
        }
        return HYPER_ADAPTATION;
    }
}
