package com.alex.mycelialmind.block;

/**
 * Pure probability policy for Creeping Mycelium's random-tick spread attempts.
 */
public final class MyceliumSpreadPolicy {
    public static final double DEFAULT_CHANCE = 0.36;

    private MyceliumSpreadPolicy() {
    }

    public static boolean shouldAttempt(double roll, double chance) {
        return roll < chance;
    }
}
