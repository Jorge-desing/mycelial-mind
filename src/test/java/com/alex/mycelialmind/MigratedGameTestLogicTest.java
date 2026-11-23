package com.alex.mycelialmind;

import com.alex.mycelialmind.symbiosis.SymbioticMutagen;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.network.PinExpeditionGoalPayload;
import com.alex.mycelialmind.network.SymbiosisSyncPayload;
import com.alex.mycelialmind.world.SynapticNodeType;
import com.alex.mycelialmind.world.WorldgenAuditEngine;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests migrated from MycelialGameTests so they execute in headless
 * JUnit 5 milliseconds without requiring a full MinecraftServer / ServerLevel tick.
 */
class MigratedGameTestLogicTest {

    @Test
    void symbiosisSyncPayloadRoundTrip() {
        int mask = (1 << SymbioticMutagen.CHITIN_THORNS.ordinal()) | (1 << SymbioticMutagen.ARCANE_SYNAPSE.ordinal());
        SymbiosisSyncPayload original = new SymbiosisSyncPayload(mask, 2);
        var buf = Unpooled.buffer();
        try {
            SymbiosisSyncPayload.STREAM_CODEC.encode(buf, original);
            SymbiosisSyncPayload decoded = SymbiosisSyncPayload.STREAM_CODEC.decode(buf);

            var decodedSet = decoded.decodeMutagens();
            assertTrue(decodedSet.contains(SymbioticMutagen.CHITIN_THORNS));
            assertTrue(decodedSet.contains(SymbioticMutagen.ARCANE_SYNAPSE));
            assertEquals(2, decoded.infectionLevel());
        } finally {
            buf.release();
        }
    }

    @Test
    void hiveSyncPayloadIncludesTerritoryMarkers() {
        BlockPos anchor = new BlockPos(10, 64, 10);
        BlockPos nodePos = new BlockPos(20, 64, 15);
        BlockPos pylonPos = new BlockPos(5, 64, 5);
        HiveSyncPayload payload = new HiveSyncPayload(
                42.0, 1 << AdaptationTrait.PROJECTILE_DEFLECT.ordinal(), 0, 0, true, true, nodePos, 2, 0,
                10, 20, 5, 0, 0,
                3, 1, 24, 28, anchor,
                List.of(new HiveSyncPayload.NodeMarker(nodePos, SynapticNodeType.INCUBATOR)),
                List.of(pylonPos)
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            HiveSyncPayload.STREAM_CODEC.encode(buf, payload);
            HiveSyncPayload decoded = HiveSyncPayload.STREAM_CODEC.decode(buf);
            assertEquals(payload.threatLevel(), decoded.threatLevel(), 0.001);
            assertTrue(decoded.decodeTraits().contains(AdaptationTrait.PROJECTILE_DEFLECT));
            assertTrue(decoded.inProtectedZone());
            assertTrue(decoded.hasNearestNode());
            assertEquals(nodePos, decoded.nearestNodePos());
            assertEquals(3, decoded.totalNodes());
            assertEquals(1, decoded.totalPylons());
            assertEquals(1, decoded.nearbyNodes().size());
            assertEquals(SynapticNodeType.INCUBATOR, decoded.nearbyNodes().get(0).type());
            assertEquals(1, decoded.nearbyPylons().size());
            assertEquals(pylonPos, decoded.nearbyPylons().get(0));
        } finally {
            buf.release();
        }
    }

    @Test
    void hiveSyncPayloadV6PreservesFungalTideSeconds() {
        HiveSyncPayload payload = new HiveSyncPayload(
                82.0, 1, 0, 0, false, false, BlockPos.ZERO, 2, 0,
                10, 20, 30, 0, 0,
                4, 1, 24, 28, BlockPos.ZERO, List.of(), List.of(), 275,
                0, 0, 0, 0, -1, 0, 0, 0
        );
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            HiveSyncPayload.STREAM_CODEC.encode(buf, payload);
            HiveSyncPayload decoded = HiveSyncPayload.STREAM_CODEC.decode(buf);
            assertEquals(275, decoded.fungalTideSecondsRemaining());
        } finally {
            buf.release();
        }
    }

    @Test
    void worldgenAuditSpatialSpacingAndClusterMetrics() {
        List<BlockPos> outbreaks = List.of(
                new BlockPos(0, 64, 0),
                new BlockPos(30, 64, 0),
                new BlockPos(100, 64, 0)
        );
        var nnStats = WorldgenAuditEngine.computeNearestNeighbor(outbreaks, outbreaks, true, true);
        assertEquals(3, nnStats.count());
        assertEquals(30.0, nnStats.min(), 0.1);
        assertEquals(30.0, nnStats.p50(), 0.1);
        assertEquals(70.0, nnStats.max(), 0.1);

        List<BlockPos> nodes = List.of(
                new BlockPos(0, 64, 0),
                new BlockPos(20, 64, 0),
                new BlockPos(80, 64, 0)
        );
        var netStats = WorldgenAuditEngine.computeNetworkStats(nodes, 28, null);
        assertEquals(1, netStats.activeLinks());
        assertEquals(2, netStats.connectedComponents());
        assertEquals(2, netStats.largestClusterSize());
        assertEquals(1, netStats.isolatedNodes());
    }

    @Test
    void hiveSyncPayloadV9PreservesSquadTacticalStatus() {
        HiveSyncPayload original = new HiveSyncPayload(
                55.0, 1, 0, 0, false, false, BlockPos.ZERO, 2, 0,
                10, 20, 0, 0, 0, 1, 0, 24, 28, BlockPos.ZERO, List.of(), List.of(), 0,
                0, 0, 0, 0, -1, 0, 0, 0.0,
                SynapticCommandSystem.SquadTacticalStatus.FRENZY.id(),
                8
        );
        ByteBuf buf = Unpooled.buffer();
        try {
            HiveSyncPayload.STREAM_CODEC.encode(buf, original);
            HiveSyncPayload decoded = HiveSyncPayload.STREAM_CODEC.decode(buf);
            assertEquals(SynapticCommandSystem.SquadTacticalStatus.FRENZY.id(), decoded.squadTacticalStatus());
            assertEquals(8, decoded.squadStatusSecondsRemaining());
        } finally {
            buf.release();
        }
    }

    @Test
    void discoverySyncAndPinExpeditionGoalPayloadsRoundTrip() {
        DiscoverySyncPayload original = new DiscoverySyncPayload(
                0x15, 0x02, 0x01, 0x04, 0x07, 2, 65, 2,
                ProgressionGoalEngine.CreatureObservation.CONFIRMED_PROJECTILE_ADAPTATION.id(),
                "minecraft:overworld",
                ProgressionGoalEngine.ExpeditionGoal.USE_TO_ADVANTAGE.id(),
                ProgressionGoalEngine.RESEARCH_BIT_SPORE_FOUND
                        | ProgressionGoalEngine.RESEARCH_BIT_DEFLECTION_WITNESSED
                        | ProgressionGoalEngine.RESEARCH_BIT_ADAPTATION_IDENTIFIED,
                14500,
                2500,
                0x19
        );
        ByteBuf buf = Unpooled.buffer();
        try {
            DiscoverySyncPayload.STREAM_CODEC.encode(buf, original);
            DiscoverySyncPayload decoded = DiscoverySyncPayload.STREAM_CODEC.decode(buf);
            assertEquals(original, decoded);
            assertEquals("minecraft:overworld", decoded.lastObservationDimension());
            assertEquals(2500, decoded.lastThreatDropPermille());
            assertEquals(0x19, decoded.discoveredEngramsMask());
        } finally {
            buf.release();
        }

        PinExpeditionGoalPayload pinOrig = new PinExpeditionGoalPayload(
                ProgressionGoalEngine.ExpeditionGoal.RECLAIM_CAVE.id());
        ByteBuf pinBuf = Unpooled.buffer();
        try {
            PinExpeditionGoalPayload.STREAM_CODEC.encode(pinBuf, pinOrig);
            PinExpeditionGoalPayload pinDecoded = PinExpeditionGoalPayload.STREAM_CODEC.decode(pinBuf);
            assertEquals(pinOrig, pinDecoded);
        } finally {
            pinBuf.release();
        }
    }
}
