package com.alex.mycelialmind;

import com.alex.mycelialmind.hive.HiveCombatTelemetryStats;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.util.MycelialRateLimiter;
import com.alex.mycelialmind.world.MycelialSpatialBitset;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNetworkTopology;
import com.alex.mycelialmind.world.SynapticNodeType;
import io.github.bucket4j.TimeMeter;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaCoreLibrariesIntegrationTest {

    @Test
    void jgraphtTopologyDetectsClustersArticulationPointsAndShortestPath() {
        PurificationZoneTracker tracker = new PurificationZoneTracker();
        BlockPos nodeA = new BlockPos(0, 64, 0);
        BlockPos nodeB = new BlockPos(26, 64, 0);
        BlockPos nodeC = new BlockPos(52, 64, 0);
        BlockPos nodeD = new BlockPos(200, 64, 0);

        tracker.addNode(nodeA, SynapticNodeType.RELAY);
        tracker.addNode(nodeB, SynapticNodeType.RELAY);
        tracker.addNode(nodeC, SynapticNodeType.INCUBATOR);
        tracker.addNode(nodeD, SynapticNodeType.SENTINEL);

        SynapticNetworkTopology.TopologySummary summary = tracker.getNetworkTopologySummary();
        assertEquals(4, summary.nodeCount());
        assertEquals(2, summary.edgeCount());
        assertEquals(2, summary.clusterCount());
        assertTrue(summary.criticalNodes().contains(nodeB));
        assertFalse(summary.criticalNodes().contains(nodeA));
        assertFalse(summary.criticalNodes().contains(nodeC));
        assertFalse(summary.criticalNodes().contains(nodeD));

        List<BlockPos> path = tracker.findShortestSynapticPath(nodeA, nodeC);
        assertEquals(List.of(nodeA, nodeB, nodeC), path);
        assertTrue(tracker.findShortestSynapticPath(nodeA, nodeD).isEmpty());

        // Placing a pylon on nodeB removes nodeB from the active enemy graph and fragments the bridge.
        tracker.addPylon(nodeB);
        SynapticNetworkTopology.TopologySummary fragmented = tracker.getNetworkTopologySummary();
        assertEquals(3, fragmented.nodeCount());
        assertEquals(0, fragmented.edgeCount());
        assertEquals(3, fragmented.clusterCount());
        assertTrue(fragmented.criticalNodes().isEmpty());
    }

    @Test
    void roaringBitmapSpatialIndexTracksNegativeChunksAndContestedOverlap() {
        MycelialSpatialBitset bitset = new MycelialSpatialBitset();
        bitset.addChunk(-5, -12);
        bitset.addChunk(0, 0);
        bitset.addChunk(120, -45);
        assertEquals(3L, bitset.cardinality());
        assertTrue(bitset.containsChunk(-5, -12));
        assertFalse(bitset.containsChunk(-5, -11));
        bitset.removeChunk(-5, -12);
        assertFalse(bitset.containsChunk(-5, -12));
        assertEquals(2L, bitset.cardinality());

        PurificationZoneTracker tracker = new PurificationZoneTracker();
        BlockPos pylon1 = new BlockPos(-30, 64, -30); // chunk (-2, -2)
        BlockPos pylon2 = new BlockPos(-25, 64, -25); // same chunk (-2, -2)
        BlockPos nodeInSharedChunk = new BlockPos(-28, 64, -28); // chunk (-2, -2)
        BlockPos nodeElsewhere = new BlockPos(64, 64, 64); // chunk (4, 4)

        tracker.addPylon(pylon1);
        tracker.addPylon(pylon2);
        tracker.addNode(nodeInSharedChunk, SynapticNodeType.RELAY);
        tracker.addNode(nodeElsewhere, SynapticNodeType.SENTINEL);

        assertTrue(tracker.hasPylonInChunk(-2, -2));
        assertTrue(tracker.hasNodeInChunk(-2, -2));
        assertTrue(tracker.hasNodeInChunk(4, 4));
        assertEquals(1L, tracker.countOccupiedPylonChunks());
        assertEquals(2L, tracker.countOccupiedNodeChunks());
        assertEquals(1L, tracker.countContestedChunks());

        // Removing one of two pylons in the same chunk must keep that chunk occupied.
        tracker.removePylon(pylon1);
        assertTrue(tracker.hasPylonInChunk(-2, -2));
        assertEquals(1L, tracker.countContestedChunks());

        tracker.removePylon(pylon2);
        assertFalse(tracker.hasPylonInChunk(-2, -2));
        assertEquals(0L, tracker.countContestedChunks());

        tracker.clearAll();
        assertEquals(0L, tracker.countOccupiedNodeChunks());
    }

    @Test
    void commonsStatisticsTracksDistributionAndDetectsSpikes() {
        HiveCombatTelemetryStats stats = new HiveCombatTelemetryStats();
        double[] baseline = {10.0D, 11.0D, 9.0D, 10.5D, 9.5D, 10.0D};
        for (double sample : baseline) {
            assertFalse(stats.recordDamage(StimulusType.MELEE, sample));
        }

        assertEquals(6L, stats.getSampleCount(StimulusType.MELEE));
        assertEquals(10.0D, stats.getMeanDamage(StimulusType.MELEE), 1e-6D);
        assertTrue(stats.getStdDevDamage(StimulusType.MELEE) > 0.5D);
        assertEquals(11.0D, stats.getMaxDamage(StimulusType.MELEE), 1e-6D);
        assertEquals(0L, stats.getSpikeCount(StimulusType.MELEE));

        // Large outlier above mean + 2 * stdDev must be flagged as a statistical spike.
        assertTrue(stats.recordDamage(StimulusType.MELEE, 45.0D));
        assertEquals(1L, stats.getSpikeCount(StimulusType.MELEE));
        assertEquals(1L, stats.getTotalSpikeCount());
        assertEquals(45.0D, stats.getMaxDamage(StimulusType.MELEE), 1e-6D);

        stats.reset();
        assertEquals(0L, stats.getSampleCount(StimulusType.MELEE));
        assertEquals(0.0D, stats.getMeanDamage(StimulusType.MELEE), 1e-6D);
        assertEquals(0L, stats.getTotalSpikeCount());
    }

    @Test
    void bucket4jRateLimiterEnforcesBurstAndDeterministicRefill() {
        AtomicLong clockNanos = new AtomicLong(1_000_000_000L);
        TimeMeter fakeMeter = new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                return clockNanos.get();
            }

            @Override
            public boolean isWallClockBased() {
                return false;
            }
        };

        MycelialRateLimiter limiter = new MycelialRateLimiter(3L, 1L, Duration.ofSeconds(1), fakeMeter);
        assertEquals(3L, limiter.getAvailableTokens());
        assertTrue(limiter.tryConsume());
        assertTrue(limiter.tryConsume());
        assertTrue(limiter.tryConsume());
        assertFalse(limiter.tryConsume());
        assertEquals(0L, limiter.getAvailableTokens());

        // Advance simulated time by 1 second -> 1 greedy token refilled.
        clockNanos.addAndGet(1_000_000_000L);
        assertTrue(limiter.tryConsume());
        assertFalse(limiter.tryConsume());

        limiter.reset();
        assertEquals(3L, limiter.getAvailableTokens());
    }
}
