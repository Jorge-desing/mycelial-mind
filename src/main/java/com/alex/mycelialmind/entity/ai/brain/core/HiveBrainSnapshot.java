package com.alex.mycelialmind.entity.ai.brain.core;

import java.util.List;

/**
 * Immutable telemetry snapshot of a {@link HiveBrain}'s live state for diagnostics,
 * bio-scanner inspection, and automated tests.
 */
public record HiveBrainSnapshot(
        String activeActivity,
        String previousActivity,
        long ticksInActivity,
        long brainTickCount,
        long skippedLodTicks,
        long signalCount,
        String lastSignalType,
        String activeProfile,
        List<String> runningBehaviours,
        int activeCooldownCount,
        int blackboardEntries,
        boolean paused
) {
    public HiveBrainSnapshot {
        activeActivity = normalize(activeActivity, "none");
        previousActivity = normalize(previousActivity, "none");
        lastSignalType = normalize(lastSignalType, "NONE");
        activeProfile = normalize(activeProfile, "default");
        runningBehaviours = runningBehaviours != null ? List.copyOf(runningBehaviours) : List.of();
        ticksInActivity = Math.max(0L, ticksInActivity);
        brainTickCount = Math.max(0L, brainTickCount);
        skippedLodTicks = Math.max(0L, skippedLodTicks);
        signalCount = Math.max(0L, signalCount);
        activeCooldownCount = Math.max(0, activeCooldownCount);
        blackboardEntries = Math.max(0, blackboardEntries);
    }

    private static String normalize(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public String formatCompact() {
        return "act=" + this.activeActivity
                + " | prof=" + this.activeProfile
                + " | sig=" + this.signalCount + " (" + this.lastSignalType + ")"
                + " | cd=" + this.activeCooldownCount
                + " | lodSkip=" + this.skippedLodTicks
                + (this.paused ? " | PAUSED" : "");
    }
}
