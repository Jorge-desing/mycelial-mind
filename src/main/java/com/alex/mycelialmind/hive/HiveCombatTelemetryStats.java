package com.alex.mycelialmind.hive;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.apache.commons.statistics.descriptive.DoubleStatistics;
import org.apache.commons.statistics.descriptive.Statistic;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-dimension descriptive combat damage telemetry backed by Apache Commons Statistics.
 * Tracks rolling mean, standard deviation, max damage, and statistical z-score damage spikes.
 */
public final class HiveCombatTelemetryStats {
    private static final int MIN_BASELINE_SAMPLES = 5;
    private static final double MIN_STDDEV_FOR_SPIKE = 0.5D;
    private static final double SPIKE_Z_THRESHOLD = 2.0D;

    private static final Map<ResourceKey<Level>, HiveCombatTelemetryStats> BY_DIMENSION = new ConcurrentHashMap<>();

    private final EnumMap<StimulusType, DoubleStatistics> statsByStimulus = new EnumMap<>(StimulusType.class);
    private final EnumMap<StimulusType, Long> spikeCounts = new EnumMap<>(StimulusType.class);

    public HiveCombatTelemetryStats() {
        reset();
    }

    public static HiveCombatTelemetryStats forLevel(ServerLevel level) {
        if (level == null) {
            return new HiveCombatTelemetryStats();
        }
        return BY_DIMENSION.computeIfAbsent(level.dimension(), ignored -> new HiveCombatTelemetryStats());
    }

    public static void clearLevel(ServerLevel level) {
        if (level != null) {
            BY_DIMENSION.remove(level.dimension());
        }
    }

    /**
     * Records a damage sample and returns {@code true} if the value represents a positive
     * statistical spike ({@code amount > mean + 2 * stdDev}) over an established baseline.
     */
    public synchronized boolean recordDamage(StimulusType type, double amount) {
        if (type == null || !Double.isFinite(amount) || amount <= 0.0D) {
            return false;
        }
        DoubleStatistics stats = statsByStimulus.get(type);
        boolean isSpike = false;
        long count = stats.getCount();
        if (count >= MIN_BASELINE_SAMPLES) {
            double mean = stats.getAsDouble(Statistic.MEAN);
            double stdDev = stats.getAsDouble(Statistic.STANDARD_DEVIATION);
            if (Double.isFinite(stdDev) && stdDev > MIN_STDDEV_FOR_SPIKE
                    && amount > mean + SPIKE_Z_THRESHOLD * stdDev) {
                isSpike = true;
                spikeCounts.put(type, spikeCounts.getOrDefault(type, 0L) + 1L);
            }
        }
        stats.accept(amount);
        return isSpike;
    }

    public synchronized long getSampleCount(StimulusType type) {
        if (type == null) {
            return 0L;
        }
        return statsByStimulus.get(type).getCount();
    }

    public synchronized double getMeanDamage(StimulusType type) {
        if (type == null) {
            return 0.0D;
        }
        DoubleStatistics stats = statsByStimulus.get(type);
        return stats.getCount() == 0 ? 0.0D : stats.getAsDouble(Statistic.MEAN);
    }

    public synchronized double getStdDevDamage(StimulusType type) {
        if (type == null) {
            return 0.0D;
        }
        DoubleStatistics stats = statsByStimulus.get(type);
        if (stats.getCount() < 2) {
            return 0.0D;
        }
        double value = stats.getAsDouble(Statistic.STANDARD_DEVIATION);
        return Double.isFinite(value) ? value : 0.0D;
    }

    public synchronized double getMaxDamage(StimulusType type) {
        if (type == null) {
            return 0.0D;
        }
        DoubleStatistics stats = statsByStimulus.get(type);
        return stats.getCount() == 0 ? 0.0D : stats.getAsDouble(Statistic.MAX);
    }

    public synchronized long getSpikeCount(StimulusType type) {
        if (type == null) {
            return 0L;
        }
        return spikeCounts.getOrDefault(type, 0L);
    }

    public synchronized long getTotalSpikeCount() {
        long total = 0L;
        for (long count : spikeCounts.values()) {
            total += count;
        }
        return total;
    }

    public synchronized void reset() {
        for (StimulusType type : StimulusType.values()) {
            statsByStimulus.put(type, DoubleStatistics.of(
                    Statistic.MEAN,
                    Statistic.STANDARD_DEVIATION,
                    Statistic.MAX
            ));
            spikeCounts.put(type, 0L);
        }
    }
}
