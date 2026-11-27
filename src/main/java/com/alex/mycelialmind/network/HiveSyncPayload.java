package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.SynapticMimicEntity;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.util.EnumBitmask;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Synchronizes Hive Mind threat level, Tier 1 & Tier 2 traits, HiveSynergies,
 * 5 StimulusType scores, protection status, network links, and nearest Node to clients.
 */
public record HiveSyncPayload(
        double threatLevel,
        int traitMask,
        int tier2Mask,
        int synergyMask,
        boolean inProtectedZone,
        boolean hasNearestNode,
        BlockPos nearestNodePos,
        int activeNetworkLinks,
        int suppressionSecondsRemaining,
        double meleeScore,
        double projectileScore,
        double fireScore,
        double explosionScore,
        double magicScore,
        int totalNodes,
        int totalPylons,
        int pylonRadius,
        int linkRadius,
        BlockPos playerAnchorPos,
        List<NodeMarker> nearbyNodes,
        List<BlockPos> nearbyPylons,
        int fungalTideSecondsRemaining,
        double medicHabitScore,
        double mobilityHabitScore,
        double bastionHabitScore,
        double shadowHabitScore,
        int falseStimulusOrdinal,
        int deceptionSecondsRemaining,
        int deceptionCooldownSecondsRemaining,
        double nearestVolatileBuffer,
        int squadTacticalStatus,
        int squadStatusSecondsRemaining,
        double homeostasisStress,
        int homeostasisStage,
        boolean acidRainExposed,
        int smotheredPylonCount,
        double traitActivationThreshold,
        double tier2ActivationThreshold,
        int homeostasisSensitization,
        List<BlockPos> smotheredPylons,
        List<InflamedChunkMarker> inflamedChunks,
        boolean deepMyceliumBreached,
        boolean matriarchDefeated,
        int matriarchRemissionSecondsRemaining
) implements CustomPacketPayload {
    private static final int MAX_SYNC_NODES = 64;
    private static final int MAX_SYNC_PYLONS = 32;
    private static final int MAX_SYNC_INFLAMED_CHUNKS = 64;

    public record NodeMarker(BlockPos pos, SynapticNodeType type) {
    }

    public record InflamedChunkMarker(int chunkX, int chunkZ, int stressPercent, int stageId, int sensitization) {
    }

    public static final Type<HiveSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_sync"));

    public static final StreamCodec<ByteBuf, HiveSyncPayload> STREAM_CODEC =
            StreamCodec.of(HiveSyncPayload::encode, HiveSyncPayload::decode);

    public HiveSyncPayload(
            double threatLevel,
            int traitMask,
            boolean inProtectedZone,
            boolean hasNearestNode,
            BlockPos nearestNodePos
    ) {
        this(threatLevel, traitMask, 0, 0, inProtectedZone, hasNearestNode, nearestNodePos, 0, 0,
                0, 0, 0, 0, 0, 0, 0, 24, 28, BlockPos.ZERO, List.of(), List.of(), 0,
                0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0.0, 0, false, 0, 70.0, 200.0, 0, List.of(), List.of(), false, false, 0);
    }

    public HiveSyncPayload(
            double threatLevel,
            int traitMask,
            int tier2Mask,
            int synergyMask,
            boolean inProtectedZone,
            boolean hasNearestNode,
            BlockPos nearestNodePos,
            int activeNetworkLinks,
            int suppressionSecondsRemaining
    ) {
        this(threatLevel, traitMask, tier2Mask, synergyMask, inProtectedZone, hasNearestNode,
                nearestNodePos, activeNetworkLinks, suppressionSecondsRemaining,
                0, 0, 0, 0, 0, 0, 0, 24, 28, BlockPos.ZERO, List.of(), List.of(), 0,
                0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0.0, 0, false, 0, 70.0, 200.0, 0, List.of(), List.of(), false, false, 0);
    }

    public HiveSyncPayload(
            double threatLevel,
            int traitMask,
            int tier2Mask,
            int synergyMask,
            boolean inProtectedZone,
            boolean hasNearestNode,
            BlockPos nearestNodePos,
            int activeNetworkLinks,
            int suppressionSecondsRemaining,
            double meleeScore,
            double projectileScore,
            double fireScore,
            double explosionScore,
            double magicScore,
            int totalNodes,
            int totalPylons,
            int pylonRadius,
            int linkRadius,
            BlockPos playerAnchorPos,
            List<NodeMarker> nearbyNodes,
            List<BlockPos> nearbyPylons,
            int fungalTideSecondsRemaining,
            double medicHabitScore,
            double mobilityHabitScore,
            double bastionHabitScore,
            double shadowHabitScore,
            int falseStimulusOrdinal,
            int deceptionSecondsRemaining,
            int deceptionCooldownSecondsRemaining,
            double nearestVolatileBuffer
    ) {
        this(threatLevel, traitMask, tier2Mask, synergyMask, inProtectedZone, hasNearestNode,
                nearestNodePos, activeNetworkLinks, suppressionSecondsRemaining,
                meleeScore, projectileScore, fireScore, explosionScore, magicScore,
                totalNodes, totalPylons, pylonRadius, linkRadius, playerAnchorPos, nearbyNodes, nearbyPylons,
                fungalTideSecondsRemaining, medicHabitScore, mobilityHabitScore, bastionHabitScore, shadowHabitScore,
                falseStimulusOrdinal, deceptionSecondsRemaining, deceptionCooldownSecondsRemaining, nearestVolatileBuffer,
                0, 0, 0.0, 0, false, 0, 70.0, 200.0, 0, List.of(), List.of(), false, false, 0);
    }

    public HiveSyncPayload(
            double threatLevel,
            int traitMask,
            int tier2Mask,
            int synergyMask,
            boolean inProtectedZone,
            boolean hasNearestNode,
            BlockPos nearestNodePos,
            int activeNetworkLinks,
            int suppressionSecondsRemaining,
            double meleeScore,
            double projectileScore,
            double fireScore,
            double explosionScore,
            double magicScore,
            int totalNodes,
            int totalPylons,
            int pylonRadius,
            int linkRadius,
            BlockPos playerAnchorPos,
            List<NodeMarker> nearbyNodes,
            List<BlockPos> nearbyPylons,
            int fungalTideSecondsRemaining,
            double medicHabitScore,
            double mobilityHabitScore,
            double bastionHabitScore,
            double shadowHabitScore,
            int falseStimulusOrdinal,
            int deceptionSecondsRemaining,
            int deceptionCooldownSecondsRemaining,
            double nearestVolatileBuffer,
            int squadTacticalStatus,
            int squadStatusSecondsRemaining
    ) {
        this(threatLevel, traitMask, tier2Mask, synergyMask, inProtectedZone, hasNearestNode,
                nearestNodePos, activeNetworkLinks, suppressionSecondsRemaining,
                meleeScore, projectileScore, fireScore, explosionScore, magicScore,
                totalNodes, totalPylons, pylonRadius, linkRadius, playerAnchorPos, nearbyNodes, nearbyPylons,
                fungalTideSecondsRemaining, medicHabitScore, mobilityHabitScore, bastionHabitScore, shadowHabitScore,
                falseStimulusOrdinal, deceptionSecondsRemaining, deceptionCooldownSecondsRemaining, nearestVolatileBuffer,
                squadTacticalStatus, squadStatusSecondsRemaining, 0.0, 0, false, 0, 70.0, 200.0, 0, List.of(), List.of(), false, false, 0);
    }

    public HiveSyncPayload(
            double threatLevel,
            int traitMask,
            int tier2Mask,
            int synergyMask,
            boolean inProtectedZone,
            boolean hasNearestNode,
            BlockPos nearestNodePos,
            int activeNetworkLinks,
            int suppressionSecondsRemaining,
            double meleeScore,
            double projectileScore,
            double fireScore,
            double explosionScore,
            double magicScore
    ) {
        this(threatLevel, traitMask, tier2Mask, synergyMask, inProtectedZone, hasNearestNode,
                nearestNodePos, activeNetworkLinks, suppressionSecondsRemaining,
                meleeScore, projectileScore, fireScore, explosionScore, magicScore,
                0, 0, 24, 28, BlockPos.ZERO, List.of(), List.of(), 0,
                0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0.0, 0, false, 0, 70.0, 200.0, 0, List.of(), List.of(), false, false, 0);
    }

    public HiveSyncPayload(
            double threatLevel,
            int traitMask,
            int tier2Mask,
            int synergyMask,
            boolean inProtectedZone,
            boolean hasNearestNode,
            BlockPos nearestNodePos,
            int activeNetworkLinks,
            int suppressionSecondsRemaining,
            double meleeScore,
            double projectileScore,
            double fireScore,
            double explosionScore,
            double magicScore,
            int totalNodes,
            int totalPylons,
            int pylonRadius,
            int linkRadius,
            BlockPos playerAnchorPos,
            List<NodeMarker> nearbyNodes,
            List<BlockPos> nearbyPylons
    ) {
        this(threatLevel, traitMask, tier2Mask, synergyMask, inProtectedZone, hasNearestNode,
                nearestNodePos, activeNetworkLinks, suppressionSecondsRemaining,
                meleeScore, projectileScore, fireScore, explosionScore, magicScore,
                totalNodes, totalPylons, pylonRadius, linkRadius, playerAnchorPos, nearbyNodes, nearbyPylons, 0,
                0, 0, 0, 0, -1, 0, 0, 0, 0, 0, 0.0, 0, false, 0, 70.0, 200.0, 0, List.of(), List.of(), false, false, 0);
    }

    public HiveSyncPayload(
            double threatLevel,
            int traitMask,
            int tier2Mask,
            int synergyMask,
            boolean inProtectedZone,
            boolean hasNearestNode,
            BlockPos nearestNodePos,
            int activeNetworkLinks,
            int suppressionSecondsRemaining,
            double meleeScore,
            double projectileScore,
            double fireScore,
            double explosionScore,
            double magicScore,
            int totalNodes,
            int totalPylons,
            int pylonRadius,
            int linkRadius,
            BlockPos playerAnchorPos,
            List<NodeMarker> nearbyNodes,
            List<BlockPos> nearbyPylons,
            int fungalTideSecondsRemaining,
            double medicHabitScore,
            double mobilityHabitScore,
            double bastionHabitScore,
            double shadowHabitScore,
            int falseStimulusOrdinal,
            int deceptionSecondsRemaining,
            int deceptionCooldownSecondsRemaining,
            double nearestVolatileBuffer,
            int squadTacticalStatus,
            int squadStatusSecondsRemaining,
            double homeostasisStress,
            int homeostasisStage,
            boolean acidRainExposed,
            int smotheredPylonCount
    ) {
        this(threatLevel, traitMask, tier2Mask, synergyMask, inProtectedZone, hasNearestNode,
                nearestNodePos, activeNetworkLinks, suppressionSecondsRemaining,
                meleeScore, projectileScore, fireScore, explosionScore, magicScore,
                totalNodes, totalPylons, pylonRadius, linkRadius, playerAnchorPos, nearbyNodes, nearbyPylons,
                fungalTideSecondsRemaining, medicHabitScore, mobilityHabitScore, bastionHabitScore, shadowHabitScore,
                falseStimulusOrdinal, deceptionSecondsRemaining, deceptionCooldownSecondsRemaining, nearestVolatileBuffer,
                squadTacticalStatus, squadStatusSecondsRemaining,
                homeostasisStress, homeostasisStage, acidRainExposed, smotheredPylonCount, 70.0, 200.0, 0, List.of(), List.of(), false, false, 0);
    }

    public HiveSyncPayload(
            double threatLevel,
            int traitMask,
            int tier2Mask,
            int synergyMask,
            boolean inProtectedZone,
            boolean hasNearestNode,
            BlockPos nearestNodePos,
            int activeNetworkLinks,
            int suppressionSecondsRemaining,
            double meleeScore,
            double projectileScore,
            double fireScore,
            double explosionScore,
            double magicScore,
            int totalNodes,
            int totalPylons,
            int pylonRadius,
            int linkRadius,
            BlockPos playerAnchorPos,
            List<NodeMarker> nearbyNodes,
            List<BlockPos> nearbyPylons,
            int fungalTideSecondsRemaining,
            double medicHabitScore,
            double mobilityHabitScore,
            double bastionHabitScore,
            double shadowHabitScore,
            int falseStimulusOrdinal,
            int deceptionSecondsRemaining,
            int deceptionCooldownSecondsRemaining,
            double nearestVolatileBuffer,
            int squadTacticalStatus,
            int squadStatusSecondsRemaining,
            double homeostasisStress,
            int homeostasisStage,
            boolean acidRainExposed,
            int smotheredPylonCount,
            double traitActivationThreshold,
            double tier2ActivationThreshold
    ) {
        this(threatLevel, traitMask, tier2Mask, synergyMask, inProtectedZone, hasNearestNode,
                nearestNodePos, activeNetworkLinks, suppressionSecondsRemaining,
                meleeScore, projectileScore, fireScore, explosionScore, magicScore,
                totalNodes, totalPylons, pylonRadius, linkRadius, playerAnchorPos, nearbyNodes, nearbyPylons,
                fungalTideSecondsRemaining, medicHabitScore, mobilityHabitScore, bastionHabitScore, shadowHabitScore,
                falseStimulusOrdinal, deceptionSecondsRemaining, deceptionCooldownSecondsRemaining, nearestVolatileBuffer,
                squadTacticalStatus, squadStatusSecondsRemaining,
                homeostasisStress, homeostasisStage, acidRainExposed, smotheredPylonCount,
                traitActivationThreshold, tier2ActivationThreshold, 0, List.of(), List.of(), false, false, 0);
    }

    public HiveSyncPayload(
            double threatLevel,
            int traitMask,
            int tier2Mask,
            int synergyMask,
            boolean inProtectedZone,
            boolean hasNearestNode,
            BlockPos nearestNodePos,
            int activeNetworkLinks,
            int suppressionSecondsRemaining,
            double meleeScore,
            double projectileScore,
            double fireScore,
            double explosionScore,
            double magicScore,
            int totalNodes,
            int totalPylons,
            int pylonRadius,
            int linkRadius,
            BlockPos playerAnchorPos,
            List<NodeMarker> nearbyNodes,
            List<BlockPos> nearbyPylons,
            int fungalTideSecondsRemaining,
            double medicHabitScore,
            double mobilityHabitScore,
            double bastionHabitScore,
            double shadowHabitScore,
            int falseStimulusOrdinal,
            int deceptionSecondsRemaining,
            int deceptionCooldownSecondsRemaining,
            double nearestVolatileBuffer,
            int squadTacticalStatus,
            int squadStatusSecondsRemaining,
            double homeostasisStress,
            int homeostasisStage,
            boolean acidRainExposed,
            int smotheredPylonCount,
            double traitActivationThreshold,
            double tier2ActivationThreshold,
            int homeostasisSensitization,
            List<BlockPos> smotheredPylons,
            List<InflamedChunkMarker> inflamedChunks
    ) {
        this(threatLevel, traitMask, tier2Mask, synergyMask, inProtectedZone, hasNearestNode,
                nearestNodePos, activeNetworkLinks, suppressionSecondsRemaining,
                meleeScore, projectileScore, fireScore, explosionScore, magicScore,
                totalNodes, totalPylons, pylonRadius, linkRadius, playerAnchorPos, nearbyNodes, nearbyPylons,
                fungalTideSecondsRemaining, medicHabitScore, mobilityHabitScore, bastionHabitScore, shadowHabitScore,
                falseStimulusOrdinal, deceptionSecondsRemaining, deceptionCooldownSecondsRemaining, nearestVolatileBuffer,
                squadTacticalStatus, squadStatusSecondsRemaining,
                homeostasisStress, homeostasisStage, acidRainExposed, smotheredPylonCount,
                traitActivationThreshold, tier2ActivationThreshold, homeostasisSensitization, smotheredPylons, inflamedChunks,
                false, false, 0);
    }

    private static void encode(ByteBuf buf, HiveSyncPayload payload) {
        ByteBufCodecs.DOUBLE.encode(buf, payload.threatLevel());
        ByteBufCodecs.VAR_INT.encode(buf, payload.traitMask());
        ByteBufCodecs.VAR_INT.encode(buf, payload.tier2Mask());
        ByteBufCodecs.VAR_INT.encode(buf, payload.synergyMask());
        ByteBufCodecs.BOOL.encode(buf, payload.inProtectedZone());
        ByteBufCodecs.BOOL.encode(buf, payload.hasNearestNode());
        BlockPos.STREAM_CODEC.encode(buf, payload.nearestNodePos());
        ByteBufCodecs.VAR_INT.encode(buf, payload.activeNetworkLinks());
        ByteBufCodecs.VAR_INT.encode(buf, payload.suppressionSecondsRemaining());
        ByteBufCodecs.DOUBLE.encode(buf, payload.meleeScore());
        ByteBufCodecs.DOUBLE.encode(buf, payload.projectileScore());
        ByteBufCodecs.DOUBLE.encode(buf, payload.fireScore());
        ByteBufCodecs.DOUBLE.encode(buf, payload.explosionScore());
        ByteBufCodecs.DOUBLE.encode(buf, payload.magicScore());
        ByteBufCodecs.VAR_INT.encode(buf, payload.totalNodes());
        ByteBufCodecs.VAR_INT.encode(buf, payload.totalPylons());
        ByteBufCodecs.VAR_INT.encode(buf, payload.pylonRadius());
        ByteBufCodecs.VAR_INT.encode(buf, payload.linkRadius());
        BlockPos.STREAM_CODEC.encode(buf, payload.playerAnchorPos());

        NetworkCodecSupport.encodeBoundedList(buf, payload.nearbyNodes(), MAX_SYNC_NODES, (b, marker) -> {
            BlockPos.STREAM_CODEC.encode(b, marker.pos());
            ByteBufCodecs.VAR_INT.encode(b, marker.type().ordinal());
        });
        NetworkCodecSupport.encodeBoundedList(buf, payload.nearbyPylons(), MAX_SYNC_PYLONS,
                (b, pos) -> BlockPos.STREAM_CODEC.encode(b, pos));
        ByteBufCodecs.VAR_INT.encode(buf, payload.fungalTideSecondsRemaining());
        ByteBufCodecs.DOUBLE.encode(buf, payload.medicHabitScore());
        ByteBufCodecs.DOUBLE.encode(buf, payload.mobilityHabitScore());
        ByteBufCodecs.DOUBLE.encode(buf, payload.bastionHabitScore());
        ByteBufCodecs.DOUBLE.encode(buf, payload.shadowHabitScore());
        ByteBufCodecs.VAR_INT.encode(buf, payload.falseStimulusOrdinal());
        ByteBufCodecs.VAR_INT.encode(buf, payload.deceptionSecondsRemaining());
        ByteBufCodecs.VAR_INT.encode(buf, payload.deceptionCooldownSecondsRemaining());
        ByteBufCodecs.DOUBLE.encode(buf, payload.nearestVolatileBuffer());
        ByteBufCodecs.VAR_INT.encode(buf, payload.squadTacticalStatus());
        ByteBufCodecs.VAR_INT.encode(buf, payload.squadStatusSecondsRemaining());
        ByteBufCodecs.DOUBLE.encode(buf, payload.homeostasisStress());
        ByteBufCodecs.VAR_INT.encode(buf, payload.homeostasisStage());
        ByteBufCodecs.BOOL.encode(buf, payload.acidRainExposed());
        ByteBufCodecs.VAR_INT.encode(buf, payload.smotheredPylonCount());
        ByteBufCodecs.DOUBLE.encode(buf, payload.traitActivationThreshold());
        ByteBufCodecs.DOUBLE.encode(buf, payload.tier2ActivationThreshold());
        ByteBufCodecs.VAR_INT.encode(buf, payload.homeostasisSensitization());
        NetworkCodecSupport.encodeBoundedList(buf, payload.smotheredPylons(), MAX_SYNC_PYLONS,
                (b, pos) -> BlockPos.STREAM_CODEC.encode(b, pos));
        NetworkCodecSupport.encodeBoundedList(buf, payload.inflamedChunks(), MAX_SYNC_INFLAMED_CHUNKS, (b, ic) -> {
            ByteBufCodecs.VAR_INT.encode(b, ic.chunkX());
            ByteBufCodecs.VAR_INT.encode(b, ic.chunkZ());
            ByteBufCodecs.VAR_INT.encode(b, ic.stressPercent());
            ByteBufCodecs.VAR_INT.encode(b, ic.stageId());
            ByteBufCodecs.VAR_INT.encode(b, ic.sensitization());
        });
        ByteBufCodecs.BOOL.encode(buf, payload.deepMyceliumBreached());
        ByteBufCodecs.BOOL.encode(buf, payload.matriarchDefeated());
        ByteBufCodecs.VAR_INT.encode(buf, payload.matriarchRemissionSecondsRemaining());
    }

    private static HiveSyncPayload decode(ByteBuf buf) {
        double threat = ByteBufCodecs.DOUBLE.decode(buf);
        int t1 = ByteBufCodecs.VAR_INT.decode(buf);
        int t2 = ByteBufCodecs.VAR_INT.decode(buf);
        int syn = ByteBufCodecs.VAR_INT.decode(buf);
        boolean protectedZone = ByteBufCodecs.BOOL.decode(buf);
        boolean hasNearest = ByteBufCodecs.BOOL.decode(buf);
        BlockPos nearest = BlockPos.STREAM_CODEC.decode(buf);
        int links = ByteBufCodecs.VAR_INT.decode(buf);
        int supSeconds = ByteBufCodecs.VAR_INT.decode(buf);
        double melee = ByteBufCodecs.DOUBLE.decode(buf);
        double proj = ByteBufCodecs.DOUBLE.decode(buf);
        double fire = ByteBufCodecs.DOUBLE.decode(buf);
        double exp = ByteBufCodecs.DOUBLE.decode(buf);
        double magic = ByteBufCodecs.DOUBLE.decode(buf);
        int totalNodes = ByteBufCodecs.VAR_INT.decode(buf);
        int totalPylons = ByteBufCodecs.VAR_INT.decode(buf);
        int pylonRadius = ByteBufCodecs.VAR_INT.decode(buf);
        int linkRadius = ByteBufCodecs.VAR_INT.decode(buf);
        BlockPos anchor = BlockPos.STREAM_CODEC.decode(buf);

        SynapticNodeType[] types = SynapticNodeType.values();
        List<NodeMarker> nodes = NetworkCodecSupport.decodeBoundedList(buf, MAX_SYNC_NODES, "nearby nodes", b -> {
            BlockPos pos = BlockPos.STREAM_CODEC.decode(b);
            int ord = ByteBufCodecs.VAR_INT.decode(b);
            SynapticNodeType type = (ord >= 0 && ord < types.length) ? types[ord] : SynapticNodeType.RELAY;
            return new NodeMarker(pos, type);
        });
        List<BlockPos> pylons = NetworkCodecSupport.decodeBoundedList(buf, MAX_SYNC_PYLONS, "nearby pylons",
                BlockPos.STREAM_CODEC::decode);
        int tideSec = ByteBufCodecs.VAR_INT.decode(buf);
        double hMedic = ByteBufCodecs.DOUBLE.decode(buf);
        double hMob = ByteBufCodecs.DOUBLE.decode(buf);
        double hBastion = ByteBufCodecs.DOUBLE.decode(buf);
        double hShadow = ByteBufCodecs.DOUBLE.decode(buf);
        int falseOrd = ByteBufCodecs.VAR_INT.decode(buf);
        int decSec = ByteBufCodecs.VAR_INT.decode(buf);
        int decCd = ByteBufCodecs.VAR_INT.decode(buf);
        double nearestVol = ByteBufCodecs.DOUBLE.decode(buf);
        int squadStatus = ByteBufCodecs.VAR_INT.decode(buf);
        int squadSec = ByteBufCodecs.VAR_INT.decode(buf);
        double homeoStress = ByteBufCodecs.DOUBLE.decode(buf);
        int homeoStage = ByteBufCodecs.VAR_INT.decode(buf);
        boolean acidExposed = ByteBufCodecs.BOOL.decode(buf);
        int smotheredPylons = ByteBufCodecs.VAR_INT.decode(buf);
        double traitThreshold = ByteBufCodecs.DOUBLE.decode(buf);
        double tier2Threshold = ByteBufCodecs.DOUBLE.decode(buf);
        int homeoSens = ByteBufCodecs.VAR_INT.decode(buf);
        List<BlockPos> smotheredList = NetworkCodecSupport.decodeBoundedList(buf, MAX_SYNC_PYLONS, "smothered pylons",
                BlockPos.STREAM_CODEC::decode);
        List<InflamedChunkMarker> inflamedList = NetworkCodecSupport.decodeBoundedList(
                buf, MAX_SYNC_INFLAMED_CHUNKS, "inflamed chunks", b -> new InflamedChunkMarker(
                        ByteBufCodecs.VAR_INT.decode(b),
                        ByteBufCodecs.VAR_INT.decode(b),
                        ByteBufCodecs.VAR_INT.decode(b),
                        ByteBufCodecs.VAR_INT.decode(b),
                        ByteBufCodecs.VAR_INT.decode(b)));
        boolean breached = ByteBufCodecs.BOOL.decode(buf);
        boolean defeated = ByteBufCodecs.BOOL.decode(buf);
        int remissionSec = ByteBufCodecs.VAR_INT.decode(buf);
        return new HiveSyncPayload(threat, t1, t2, syn, protectedZone, hasNearest, nearest, links, supSeconds,
                melee, proj, fire, exp, magic, totalNodes, totalPylons, pylonRadius, linkRadius, anchor, nodes, pylons, tideSec,
                hMedic, hMob, hBastion, hShadow, falseOrd, decSec, decCd, nearestVol, squadStatus, squadSec,
                homeoStress, homeoStage, acidExposed, smotheredPylons, traitThreshold, tier2Threshold,
                homeoSens, smotheredList, inflamedList, breached, defeated, remissionSec);
    }

    public static HiveSyncPayload fromSavedData(HiveMindSavedData data) {
        return new HiveSyncPayload(
                data.getThreatLevel(),
                encodeTraits(data.getActiveTraits()),
                encodeTraits(data.getTier2Traits()),
                encodeSynergies(data.getActiveSynergies()),
                false,
                false,
                BlockPos.ZERO,
                0,
                0,
                data.getStimulusScore(StimulusType.MELEE),
                data.getStimulusScore(StimulusType.PROJECTILE),
                data.getStimulusScore(StimulusType.FIRE),
                data.getStimulusScore(StimulusType.EXPLOSION),
                data.getStimulusScore(StimulusType.MAGIC)
        );
    }

    public static HiveSyncPayload forPlayer(ServerLevel level, ServerPlayer player) {
        HiveMindSavedData data = HiveMindSavedData.get(level);
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        BlockPos anchor = player.blockPosition();
        boolean protectedZone = tracker.isProtected(anchor);
        BlockPos nearest = tracker.findNearestNode(anchor);
        int links = tracker.countActiveNetworkLinks();
        int suppressedSeconds = data.getSuppressionRemainingSeconds(level.getGameTime());

        List<NodeMarker> nearbyNodes = new ArrayList<>();
        for (Map.Entry<BlockPos, SynapticNodeType> entry : tracker.getNodesWithin(anchor, 128, 48)) {
            nearbyNodes.add(new NodeMarker(entry.getKey(), entry.getValue()));
        }
        if (!player.hasEffect(ModEffects.SYNAPTIC_VISION)) {
            for (SynapticMimicEntity mimic : level.getEntitiesOfClass(
                    SynapticMimicEntity.class,
                    player.getBoundingBox().inflate(24.0D),
                    m -> m.isAlive() && m.isInformationWarfareActive() && !m.isUnmasked()
            )) {
                if (nearbyNodes.size() + 2 <= MAX_SYNC_NODES) {
                    nearbyNodes.add(new NodeMarker(mimic.blockPosition().offset(5, 0, -4), SynapticNodeType.INCUBATOR));
                    nearbyNodes.add(new NodeMarker(mimic.blockPosition().offset(-4, 0, 5), SynapticNodeType.SENTINEL));
                }
            }
        }
        List<BlockPos> nearbyPylons = tracker.getPylonsWithin(anchor, 128, 24);
        long now = level.getGameTime();
        double nearestVolatile = 0.0;
        if (nearest != null && level.getBlockEntity(nearest) instanceof SynapticNodeBlockEntity nearestBe) {
            nearestVolatile = nearestBe.getVolatileTotal();
        }
        StimulusType falseType = data.getFalseStimulus();
        int[] squadSummary = SynapticCommandSystem.summarizeSquadAround(level, anchor, 24.0D);
        double homeoStress = 0.0;
        int homeoStage = 0;
        boolean acidExposed = false;
        int smotheredPylons = tracker.countSmotheredPylons();
        int homeoSens = 0;
        List<BlockPos> smotheredList = new ArrayList<>();
        for (BlockPos pylonPos : nearbyPylons) {
            if (tracker.isPylonSmothered(pylonPos)) {
                smotheredList.add(pylonPos);
            }
        }
        List<InflamedChunkMarker> inflamedList = new ArrayList<>();
        if (DimensionalHomeostasisDirector.isHomeostasisDimension(level)) {
            DimensionalHomeostasisSavedData homeoData = DimensionalHomeostasisSavedData.get(level);
            ChunkPos playerChunk = new ChunkPos(anchor);
            homeoStress = homeoData.getEffectiveNeighborhoodStress(playerChunk);
            homeoSens = homeoData.getEffectiveNeighborhoodSensitization(playerChunk);
            DimensionalHomeostasisEngine.HomeostasisStage stage = DimensionalHomeostasisEngine.stageForStress(homeoStress);
            homeoStage = stage.id();
            acidExposed = stage == DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE
                    && DimensionalHomeostasisDirector.isExposedToCeilingAcidRain(level, anchor);
            for (var snap : homeoData.getInflamedChunksWithin(playerChunk, 8, 64)) {
                inflamedList.add(new InflamedChunkMarker(
                        snap.chunkX(), snap.chunkZ(),
                        (int) Math.round(snap.stress()),
                        snap.stage().id(),
                        snap.sensitization()
                ));
            }
        } else {
            ChunkPos playerChunk = new ChunkPos(anchor);
            for (var snap : data.getDenseHyphalChunksWithin(playerChunk, 8, 64)) {
                inflamedList.add(new InflamedChunkMarker(
                        snap.chunkX(), snap.chunkZ(),
                        (int) Math.round(snap.density()),
                        DimensionalHomeostasisEngine.stageForStress(snap.density()).id(),
                        Math.min(3, snap.traversalCount() / 2)
                ));
            }
        }

        return new HiveSyncPayload(
                data.getThreatLevel(),
                encodeTraits(data.getActiveTraits()),
                encodeTraits(data.getTier2Traits()),
                encodeSynergies(data.getActiveSynergies()),
                protectedZone,
                nearest != null,
                nearest != null ? nearest : BlockPos.ZERO,
                links,
                suppressedSeconds,
                data.getStimulusScore(StimulusType.MELEE),
                data.getStimulusScore(StimulusType.PROJECTILE),
                data.getStimulusScore(StimulusType.FIRE),
                data.getStimulusScore(StimulusType.EXPLOSION),
                data.getStimulusScore(StimulusType.MAGIC),
                tracker.countNodes(),
                tracker.countPylons(),
                MycelialConfig.PYLON_PURIFICATION_RADIUS.get(),
                MycelialConfig.SYNAPTIC_NODE_LINK_RADIUS.get(),
                anchor,
                nearbyNodes,
                nearbyPylons,
                data.getFungalTideRemainingSeconds(now),
                data.getHabitScore(PlayerHabit.COMBAT_MEDIC),
                data.getHabitScore(PlayerHabit.HIGH_MOBILITY),
                data.getHabitScore(PlayerHabit.BASTION_BUILDER),
                data.getHabitScore(PlayerHabit.SHADOW_INFILTRATOR),
                falseType != null ? falseType.ordinal() : -1,
                data.getDeceptionRemainingSeconds(now),
                data.getDeceptionCooldownRemainingSeconds(now),
                nearestVolatile,
                squadSummary[0],
                squadSummary[1],
                homeoStress,
                homeoStage,
                acidExposed,
                smotheredPylons,
                MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get(),
                MycelialConfig.getEffectiveTier2ActivationThreshold(),
                homeoSens,
                smotheredList,
                inflamedList,
                data.isDeepMyceliumBreached(),
                data.isMatriarchDefeated(),
                data.getMatriarchRemissionRemainingSeconds(now)
        );
    }

    public static int encodeTraits(Set<AdaptationTrait> traits) {
        return EnumBitmask.encode(traits);
    }

    public static int encodeSynergies(Set<HiveSynergy> synergies) {
        return EnumBitmask.encode(synergies);
    }

    public Set<AdaptationTrait> decodeTraits() {
        return AdaptationTrait.decodeMask(traitMask);
    }

    public Set<AdaptationTrait> decodeTier2Traits() {
        return AdaptationTrait.decodeMask(tier2Mask);
    }

    public Set<HiveSynergy> decodeSynergies() {
        return EnumBitmask.decode(HiveSynergy.class, synergyMask);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleOnClient(HiveSyncPayload payload, IPayloadContext context) {
        double[] scores = {
                payload.meleeScore(),
                payload.projectileScore(),
                payload.fireScore(),
                payload.explosionScore(),
                payload.magicScore()
        };
        double[] habits = {
                payload.medicHabitScore(),
                payload.mobilityHabitScore(),
                payload.bastionHabitScore(),
                payload.shadowHabitScore()
        };
        context.enqueueWork(() -> ClientHiveCache.update(
                payload.threatLevel(),
                payload.decodeTraits(),
                payload.decodeTier2Traits(),
                payload.decodeSynergies(),
                payload.inProtectedZone(),
                payload.hasNearestNode() ? payload.nearestNodePos() : null,
                payload.activeNetworkLinks(),
                payload.suppressionSecondsRemaining(),
                scores,
                payload.totalNodes(),
                payload.totalPylons(),
                payload.pylonRadius(),
                payload.linkRadius(),
                payload.playerAnchorPos(),
                payload.nearbyNodes(),
                payload.nearbyPylons(),
                payload.fungalTideSecondsRemaining(),
                habits,
                payload.falseStimulusOrdinal(),
                payload.deceptionSecondsRemaining(),
                payload.deceptionCooldownSecondsRemaining(),
                payload.nearestVolatileBuffer(),
                payload.squadTacticalStatus(),
                payload.squadStatusSecondsRemaining(),
                payload.homeostasisStress(),
                payload.homeostasisStage(),
                payload.acidRainExposed(),
                payload.smotheredPylonCount(),
                payload.traitActivationThreshold(),
                payload.tier2ActivationThreshold(),
                payload.homeostasisSensitization(),
                payload.smotheredPylons(),
                payload.inflamedChunks(),
                payload.deepMyceliumBreached(),
                payload.matriarchDefeated(),
                payload.matriarchRemissionSecondsRemaining()
        ));
    }
}
