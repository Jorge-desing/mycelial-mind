package com.alex.mycelialmind;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainScheduler;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HiveBrainEngineUnitTest {

    @Test
    void sensorSchedulingAlwaysRunsInitiallyAndRespectsScanRate() {
        assertTrue(HiveBrainScheduler.shouldRunSensor(0L, HiveBrainScheduler.INITIAL_TICK_SENTINEL));
        assertTrue(HiveBrainScheduler.shouldRunSensor(100L, HiveBrainScheduler.INITIAL_TICK_SENTINEL));

        long next = HiveBrainScheduler.computeNextTickTime(100L, 20);
        assertEquals(120L, next);
        assertFalse(HiveBrainScheduler.shouldRunSensor(100L, next));
        assertFalse(HiveBrainScheduler.shouldRunSensor(119L, next));
        assertTrue(HiveBrainScheduler.shouldRunSensor(120L, next));
        assertTrue(HiveBrainScheduler.shouldRunSensor(150L, next));

        // Clamps zero/negative scan rates to at least 1 tick and protects against Long.MAX_VALUE overflow
        assertEquals(11L, HiveBrainScheduler.computeNextTickTime(10L, 0));
        assertEquals(11L, HiveBrainScheduler.computeNextTickTime(10L, -5));
        assertEquals(Long.MAX_VALUE, HiveBrainScheduler.computeNextTickTime(Long.MAX_VALUE - 5L, 20));
    }

    @Test
    void cooldownAndTimeoutBoundsHandleNoTimeoutAndOverflow() {
        assertFalse(HiveBrainScheduler.isOnCooldown(0L, HiveBrainScheduler.INITIAL_TICK_SENTINEL));

        long cooldownEnd = HiveBrainScheduler.computeCooldownFinishedAt(50L, 40);
        assertEquals(90L, cooldownEnd);
        assertTrue(HiveBrainScheduler.isOnCooldown(50L, cooldownEnd));
        assertTrue(HiveBrainScheduler.isOnCooldown(89L, cooldownEnd));
        assertFalse(HiveBrainScheduler.isOnCooldown(90L, cooldownEnd));

        assertEquals(HiveBrainScheduler.INITIAL_TICK_SENTINEL,
                HiveBrainScheduler.computeCooldownFinishedAt(50L, 0));
        assertEquals(HiveBrainScheduler.INITIAL_TICK_SENTINEL,
                HiveBrainScheduler.computeCooldownFinishedAt(50L, -10));
        assertEquals(Long.MAX_VALUE,
                HiveBrainScheduler.computeCooldownFinishedAt(Long.MAX_VALUE - 2L, 10));

        // Timeout handling including Integer.MAX_VALUE (noTimeout) and zero runtime
        long noTimeoutEnd = HiveBrainScheduler.computeEndTimestamp(100L, Integer.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, noTimeoutEnd);
        assertFalse(HiveBrainScheduler.isTimedOut(Long.MAX_VALUE, noTimeoutEnd));

        long zeroEnd = HiveBrainScheduler.computeEndTimestamp(100L, 0);
        assertEquals(100L, zeroEnd);
        assertFalse(HiveBrainScheduler.isTimedOut(100L, zeroEnd));
        assertTrue(HiveBrainScheduler.isTimedOut(101L, zeroEnd));

        long clampedNegEnd = HiveBrainScheduler.computeEndTimestamp(100L, -5);
        assertEquals(100L, clampedNegEnd);
        assertEquals(Long.MAX_VALUE, HiveBrainScheduler.computeEndTimestamp(Long.MAX_VALUE - 3L, 10));
    }

    @Test
    void weightedSelectionAndSequentialProgressionAreDeterministic() {
        assertEquals(-1, HiveBrainScheduler.selectWeightedIndex(null, 0));
        assertEquals(-1, HiveBrainScheduler.selectWeightedIndex(new int[0], 0));
        assertEquals(-1, HiveBrainScheduler.selectWeightedIndex(new int[]{0, -3, 0}, 5));

        int[] weights = {1, 0, 3, -2, 2}; // total positive weight = 6 (indices 0:[0], 2:[1,2,3], 4:[4,5])
        assertEquals(0, HiveBrainScheduler.selectWeightedIndex(weights, 0));
        assertEquals(2, HiveBrainScheduler.selectWeightedIndex(weights, 1));
        assertEquals(2, HiveBrainScheduler.selectWeightedIndex(weights, 3));
        assertEquals(4, HiveBrainScheduler.selectWeightedIndex(weights, 4));
        assertEquals(4, HiveBrainScheduler.selectWeightedIndex(weights, 5));
        assertEquals(0, HiveBrainScheduler.selectWeightedIndex(weights, 6));
        assertTrue(HiveBrainScheduler.selectWeightedIndex(weights, Integer.MIN_VALUE) >= 0);

        assertEquals(-1, HiveBrainScheduler.nextSequentialIndex(0, 0));
        assertEquals(1, HiveBrainScheduler.nextSequentialIndex(0, 3));
        assertEquals(2, HiveBrainScheduler.nextSequentialIndex(1, 3));
        assertEquals(-1, HiveBrainScheduler.nextSequentialIndex(2, 3));
    }
}
