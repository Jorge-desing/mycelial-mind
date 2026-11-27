package com.alex.mycelialmind;

import com.alex.mycelialmind.compat.journeymap.MycelialMapOverlayGeometry;
import com.alex.mycelialmind.compat.kubejs.MycelialKubeJSBindings;
import com.alex.mycelialmind.compat.top.MycelialTopCompat;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.network.ClientHiveCache;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenSourceApisIntegrationTest {

    @Test
    void caffeineProtectionAndNearestNodeCachesRecordHitsAndInvalidateOnMutation() {
        PurificationZoneTracker tracker = new PurificationZoneTracker();
        BlockPos pylon = new BlockPos(0, 64, 0);
        BlockPos probeInside = new BlockPos(10, 64, 10);
        BlockPos probeOutside = new BlockPos(100, 64, 100);

        assertFalse(tracker.isProtected(probeInside));
        assertFalse(tracker.isProtected(probeInside));
        CacheStats initialStats = tracker.getProtectionCacheStats();
        assertEquals(1L, initialStats.missCount());
        assertEquals(1L, initialStats.hitCount());

        tracker.addPylon(pylon);
        assertTrue(tracker.isProtected(probeInside));
        assertTrue(tracker.isProtected(probeInside));
        assertFalse(tracker.isProtected(probeOutside));

        CacheStats afterPylonStats = tracker.getProtectionCacheStats();
        assertEquals(3L, afterPylonStats.missCount());
        assertEquals(2L, afterPylonStats.hitCount());

        tracker.setPylonSmothered(pylon, true);
        assertFalse(tracker.isProtected(probeInside));

        tracker.setPylonSmothered(pylon, false);
        assertTrue(tracker.isProtected(probeInside));

        tracker.setPylonDepleted(pylon, true);
        assertFalse(tracker.isProtected(probeInside));

        tracker.setPylonDepleted(pylon, false);
        tracker.setPylonTier(pylon, 3);
        BlockPos tier3Probe = new BlockPos(34, 64, 0);
        assertTrue(tracker.isProtected(tier3Probe));

        tracker.removePylon(pylon);
        assertFalse(tracker.isProtected(tier3Probe));

        BlockPos nodeA = new BlockPos(5, 64, 5);
        BlockPos nodeB = new BlockPos(20, 64, 20);
        tracker.addNode(nodeB, SynapticNodeType.SENTINEL);
        assertEquals(nodeB, tracker.findNearestNode(BlockPos.ZERO));
        assertEquals(nodeB, tracker.findNearestNode(BlockPos.ZERO));
        assertEquals(1L, tracker.getNearestNodeCacheStats().hitCount());

        tracker.addNode(nodeA, SynapticNodeType.RELAY);
        assertEquals(nodeA, tracker.findNearestNode(BlockPos.ZERO));
        assertEquals(nodeB, tracker.findNearestNode(BlockPos.ZERO, SynapticNodeType.SENTINEL));

        tracker.clearAll();
        assertNull(tracker.findNearestNode(BlockPos.ZERO));
    }

    @Test
    void journeyMapOverlayGeometryComputesPolygonsAndSemanticColors() {
        BlockPos center = new BlockPos(16, 64, -32);
        List<BlockPos> circle = MycelialMapOverlayGeometry.buildCirclePolygon(
                center, 24, MycelialMapOverlayGeometry.DEFAULT_PYLON_SEGMENTS);
        assertEquals(16, circle.size());
        assertEquals(new BlockPos(40, 64, -32), circle.getFirst());

        List<BlockPos> diamond = MycelialMapOverlayGeometry.buildNodeDiamondPolygon(center, 3);
        assertEquals(4, diamond.size());
        assertEquals(new BlockPos(16, 64, -35), diamond.getFirst());

        List<BlockPos> chunkRect = MycelialMapOverlayGeometry.buildChunkRectPolygon(2, -1, 70);
        assertEquals(4, chunkRect.size());
        assertEquals(new BlockPos(32, 70, -16), chunkRect.getFirst());
        assertEquals(new BlockPos(47, 70, -1), chunkRect.get(2));

        assertEquals(0x26D0CE, MycelialMapOverlayGeometry.resolvePylonColor(false));
        assertEquals(0xE67E22, MycelialMapOverlayGeometry.resolvePylonColor(true));
        assertEquals(0x2ECC71, MycelialMapOverlayGeometry.resolveNodeColor(SynapticNodeType.SYMBIOTIC));
        assertEquals(0xE74C3C, MycelialMapOverlayGeometry.resolveNodeColor(SynapticNodeType.INCUBATOR));
        assertEquals(0xC0392B, MycelialMapOverlayGeometry.resolveInflamedChunkColor(2));
        assertEquals(0xD35400, MycelialMapOverlayGeometry.resolveInflamedChunkColor(1));
        assertTrue(MycelialClientConfig.journeyMapOverlaysEnabled());
    }

    @Test
    void clientHiveCacheNotifiesAndUnsubscribesListenersCleanly() {
        AtomicInteger calls = new AtomicInteger(0);
        Runnable listener = calls::incrementAndGet;
        ClientHiveCache.addUpdateListener(listener);
        try {
            ClientHiveCache.update(12.5, Set.of(AdaptationTrait.FIRE_RESISTANT), Set.of(), Set.of(),
                    true, new BlockPos(4, 64, 4), 2, 0, new double[StimulusType.values().length]);
            assertEquals(1, calls.get());

            ClientHiveCache.clear();
            assertEquals(2, calls.get());
        } finally {
            ClientHiveCache.removeUpdateListener(listener);
        }
        ClientHiveCache.clear();
        assertEquals(2, calls.get());
    }

    @Test
    void kubeJsBindingsValidateNullsAndEnumsSafely() {
        assertEquals(StimulusType.FIRE, MycelialKubeJSBindings.parseStimulusType(" fire "));
        assertEquals(StimulusType.MAGIC, MycelialKubeJSBindings.parseStimulusType("MAGIC"));
        assertNull(MycelialKubeJSBindings.parseStimulusType("unknown_stimulus"));
        assertNull(MycelialKubeJSBindings.parseStimulusType(null));

        assertEquals(AdaptationTrait.MELEE_THORNS, MycelialKubeJSBindings.parseAdaptationTrait("melee_thorns"));
        assertNull(MycelialKubeJSBindings.parseAdaptationTrait(""));

        assertEquals(0.0, MycelialKubeJSBindings.getThreat(null));
        assertEquals(0.0, MycelialKubeJSBindings.getStimulus(null, "FIRE"));
        assertFalse(MycelialKubeJSBindings.addStimulus(null, "FIRE", 10.0));
        assertTrue(MycelialKubeJSBindings.getActiveTraits(null).isEmpty());
        assertTrue(MycelialKubeJSBindings.getTier2Traits(null).isEmpty());
        assertTrue(MycelialKubeJSBindings.getActiveSynergies(null).isEmpty());
        assertFalse(MycelialKubeJSBindings.hasTrait(null, "FIRE_RESISTANT"));
        assertFalse(MycelialKubeJSBindings.isProtected(null, BlockPos.ZERO));
        assertEquals(0, MycelialKubeJSBindings.getActivePylonCount(null));
        assertEquals(0, MycelialKubeJSBindings.getActiveNodeCount(null));
        assertEquals(0.0, MycelialKubeJSBindings.getHomeostasisStress(null, BlockPos.ZERO));
        MycelialKubeJSBindings.setThreat(null, 50.0);
        MycelialKubeJSBindings.addThreat(null, 10.0);
        MycelialKubeJSBindings.suppressHive(null, 30);
        MycelialKubeJSBindings.resetHive(null);

        assertNotNull(MycelialTopCompat.TOP_MODID);
    }
}
