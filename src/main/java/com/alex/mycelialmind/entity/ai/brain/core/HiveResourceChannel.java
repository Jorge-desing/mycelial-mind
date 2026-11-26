package com.alex.mycelialmind.entity.ai.brain.core;

/**
 * Discrete motor and cognitive execution channels arbitrated by {@link HiveResourceArbiter}
 * so concurrent {@link HiveBehaviour} tasks never fight over incompatible subsystems.
 */
public enum HiveResourceChannel {
    MOVE,
    LOOK,
    COMBAT,
    ABILITY,
    TERRAFORM;

    public int bit() {
        return 1 << ordinal();
    }

    public static int maskOf(HiveResourceChannel... channels) {
        if (channels == null || channels.length == 0) {
            return 0;
        }
        int mask = 0;
        for (HiveResourceChannel channel : channels) {
            if (channel != null) {
                mask |= channel.bit();
            }
        }
        return mask;
    }

    public static String formatMask(int mask) {
        if (mask == 0) {
            return "NONE";
        }
        StringBuilder sb = new StringBuilder();
        HiveResourceChannel[] values = values();
        for (int i = 0; i < values.length; i++) {
            if ((mask & values[i].bit()) != 0) {
                if (!sb.isEmpty()) {
                    sb.append('|');
                }
                sb.append(values[i].name());
            }
        }
        return sb.isEmpty() ? "NONE" : sb.toString();
    }
}
