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
        boolean paused,
        int moduleCount,
        String occupiedChannels,
        String lastTraceEntry
) {
    public HiveBrainSnapshot(
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
            boolean paused) {
        this(
                activeActivity,
                previousActivity,
                ticksInActivity,
                brainTickCount,
                skippedLodTicks,
                signalCount,
                lastSignalType,
                activeProfile,
                runningBehaviours,
                activeCooldownCount,
                blackboardEntries,
                paused,
                0,
                "NONE",
                "-"
        );
    }

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
        moduleCount = Math.max(0, moduleCount);
        occupiedChannels = normalize(occupiedChannels, "NONE");
        lastTraceEntry = normalize(lastTraceEntry, "-");
    }

    private static String normalize(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    public String formatCompact() {
        StringBuilder sb = new StringBuilder();
        sb.append("act=").append(this.activeActivity)
                .append(" | prof=").append(this.activeProfile)
                .append(" | sig=").append(this.signalCount).append(" (").append(this.lastSignalType).append(')')
                .append(" | cd=").append(this.activeCooldownCount)
                .append(" | mod=").append(this.moduleCount)
                .append(" | lodSkip=").append(this.skippedLodTicks);
        if (!"NONE".equals(this.occupiedChannels)) {
            sb.append(" | ch=").append(this.occupiedChannels);
        }
        if (this.paused) {
            sb.append(" | PAUSED");
        }
        return sb.toString();
    }
}
