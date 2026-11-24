package com.alex.mycelialmind.entity.ai.brain.core;

/**
 * Pure deterministic scheduling, cooldown, timeout, and selection math for the native HiveBrain engine.
 * Intentionally decoupled from Minecraft classes so it can be unit-tested headlessly and mutation-tested.
 */
public final class HiveBrainScheduler {
    public static final long INITIAL_TICK_SENTINEL = Long.MIN_VALUE;

    private HiveBrainScheduler() {
    }

    /**
     * Returns true when a sensor should execute at {@code gameTime}.
     * Uses {@link #INITIAL_TICK_SENTINEL} so a fresh sensor always executes on its first tick.
     */
    public static boolean shouldRunSensor(long gameTime, long nextTickTime) {
        return nextTickTime == INITIAL_TICK_SENTINEL || gameTime >= nextTickTime;
    }

    /**
     * Computes the next gameTime at which a sensor may fire again.
     */
    public static long computeNextTickTime(long gameTime, int scanRate) {
        int clampedRate = Math.max(1, scanRate);
        if (gameTime > Long.MAX_VALUE - clampedRate) {
            return Long.MAX_VALUE;
        }
        return gameTime + clampedRate;
    }

    /**
     * Returns true if a behaviour is still in its post-stop cooldown window.
     */
    public static boolean isOnCooldown(long gameTime, long cooldownFinishedAt) {
        return cooldownFinishedAt != INITIAL_TICK_SENTINEL && gameTime < cooldownFinishedAt;
    }

    /**
     * Computes the expiration timestamp for a behaviour started at {@code gameTime}.
     * {@link Integer#MAX_VALUE} indicates no timeout ({@link Long#MAX_VALUE}).
     */
    public static long computeEndTimestamp(long gameTime, int runtimeTicks) {
        if (runtimeTicks == Integer.MAX_VALUE) {
            return Long.MAX_VALUE;
        }
        int clampedTicks = Math.max(0, runtimeTicks);
        if (gameTime > Long.MAX_VALUE - clampedTicks) {
            return Long.MAX_VALUE;
        }
        return gameTime + clampedTicks;
    }

    /**
     * Returns true if {@code gameTime} has strictly passed {@code endTimestamp}.
     */
    public static boolean isTimedOut(long gameTime, long endTimestamp) {
        return endTimestamp != Long.MAX_VALUE && gameTime > endTimestamp;
    }

    /**
     * Computes when a post-stop cooldown ends, or {@link #INITIAL_TICK_SENTINEL} if no cooldown is active.
     */
    public static long computeCooldownFinishedAt(long gameTime, int cooldownTicks) {
        if (cooldownTicks <= 0) {
            return INITIAL_TICK_SENTINEL;
        }
        if (gameTime > Long.MAX_VALUE - cooldownTicks) {
            return Long.MAX_VALUE;
        }
        return gameTime + cooldownTicks;
    }

    /**
     * Selects an index proportionally to positive weights given a deterministic integer roll.
     * Returns -1 if {@code weights} is empty or no weight is positive.
     */
    public static int selectWeightedIndex(int[] weights, int roll) {
        if (weights == null || weights.length == 0) {
            return -1;
        }
        long total = 0L;
        for (int weight : weights) {
            if (weight > 0) {
                total += weight;
            }
        }
        if (total <= 0L) {
            return -1;
        }
        long target = Long.remainderUnsigned(Integer.toUnsignedLong(roll), total);
        long cumulative = 0L;
        for (int i = 0; i < weights.length; i++) {
            int weight = weights[i];
            if (weight <= 0) {
                continue;
            }
            cumulative += weight;
            if (target < cumulative) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Advances a sequential behaviour index, returning -1 once all elements have completed.
     */
    public static int nextSequentialIndex(int currentIndex, int size) {
        if (size <= 0) {
            return -1;
        }
        int next = currentIndex + 1;
        return next >= size ? -1 : next;
    }

    /**
     * Computes a deterministic phased next tick time for a sensor based on {@code entityId}
     * after its initial immediate execution, preventing synchronized swarm scan spikes.
     */
    public static long computePhasedFirstTickTime(long gameTime, int scanRate, int entityId) {
        int clampedRate = Math.max(1, scanRate);
        if (clampedRate <= 1) {
            return computeNextTickTime(gameTime, 1);
        }
        int offset = 1 + Math.floorMod(entityId, clampedRate);
        if (gameTime > Long.MAX_VALUE - offset) {
            return Long.MAX_VALUE;
        }
        return gameTime + offset;
    }

    /**
     * Normalizes a world dayTime value into the canonical {@code [0, 24000)} Minecraft day tick range.
     */
    public static int normalizeDayTick(long dayTime) {
        return (int) Math.floorMod(dayTime, 24000L);
    }

    /**
     * Returns true if {@code tick} falls inside {@code [startInclusive, endInclusive]},
     * supporting midnight wrap-around when {@code startInclusive > endInclusive}.
     */
    public static boolean isTickWithinWindow(int tick, int startInclusive, int endInclusive) {
        if (startInclusive <= endInclusive) {
            return tick >= startInclusive && tick <= endInclusive;
        }
        return tick >= startInclusive || tick <= endInclusive;
    }

    /**
     * Selects the index with the highest valid utility score above {@code minThreshold}.
     * Applies {@code hysteresisBonus} to {@code currentRunningIndex} only if its raw score
     * already satisfies {@code >= minThreshold}, ensuring stable switching without sticky deadlocks.
     */
    public static int selectHighestUtilityIndex(
            float[] scores, int currentRunningIndex, float minThreshold, float hysteresisBonus) {
        if (scores == null || scores.length == 0) {
            return -1;
        }
        float effectiveHysteresis = Math.max(0.0F, hysteresisBonus);
        int bestIndex = -1;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < scores.length; i++) {
            float raw = scores[i];
            if (Float.isNaN(raw) || raw < minThreshold) {
                continue;
            }
            float effective = (i == currentRunningIndex) ? (raw + effectiveHysteresis) : raw;
            if (bestIndex == -1 || effective > bestScore) {
                bestIndex = i;
                bestScore = effective;
            }
        }
        return bestIndex;
    }

    /**
     * Adaptive AI LOD gate for {@link HiveBrainOwner#shouldTickBrain}:
     * ticks at full 1-tick cadence during initial setup, combat, alerts, or within player proximity,
     * and throttles distant completely idle mobs to a staggered 1-in-4 tick cadence.
     */
    public static boolean shouldTickBrainWithLod(
            long brainTickCount, int entityId, long gameTime, boolean highPriority, boolean nearPlayer) {
        if (brainTickCount <= 0L || highPriority || nearPlayer) {
            return true;
        }
        return Math.floorMod(gameTime + entityId, 4L) == 0L;
    }
}
