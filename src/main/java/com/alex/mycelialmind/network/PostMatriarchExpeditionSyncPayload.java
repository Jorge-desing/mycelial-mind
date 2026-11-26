package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.PostMatriarchExpeditionData;
import com.alex.mycelialmind.world.ModDimensions;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.world.level.ChunkPos;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/** Syncs the shared expedition and recovered enclave markers from the deep dimension. */
public record PostMatriarchExpeditionSyncPayload(
        boolean unlocked,
        boolean active,
        long activePos,
        int outcomeId,
        int palette,
        int progress,
        int linkedFlora,
        int siteStress,
        int siteFlags,
        BlockPos defensePylon,
        boolean pulseInProgress,
        int attackersRemaining,
        List<Marker> enclaves
) implements CustomPacketPayload {
    public static final int TIDE = 1;
    public static final int ACID = 2;
    public static final int PYLON_SMOTHERED = 4;
    public static final int PYLON_MISSING = 8;
    public static final int SITE_UNLOADED = 16;
    public static final Type<PostMatriarchExpeditionSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "post_matriarch_expedition_sync"));

    private static final int MAX_ENCLAVE_MARKERS = 128;
    private static final StreamCodec<ByteBuf, List<Marker>> MARKERS_CODEC = new StreamCodec<>() {
        @Override
        public List<Marker> decode(ByteBuf buffer) {
            int size = ByteBufCodecs.VAR_INT.decode(buffer);
            if (size < 0 || size > MAX_ENCLAVE_MARKERS) {
                throw new io.netty.handler.codec.DecoderException("Invalid recovered enclave marker count: " + size);
            }
            List<Marker> markers = new ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                markers.add(Marker.STREAM_CODEC.decode(buffer));
            }
            return markers;
        }

        @Override
        public void encode(ByteBuf buffer, List<Marker> markers) {
            if (markers.size() > MAX_ENCLAVE_MARKERS) {
                throw new io.netty.handler.codec.EncoderException("Too many recovered enclave markers: " + markers.size());
            }
            ByteBufCodecs.VAR_INT.encode(buffer, markers.size());
            for (Marker marker : markers) {
                Marker.STREAM_CODEC.encode(buffer, marker);
            }
        }
    };

    public static final StreamCodec<ByteBuf, PostMatriarchExpeditionSyncPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public PostMatriarchExpeditionSyncPayload decode(ByteBuf buffer) {
                    return new PostMatriarchExpeditionSyncPayload(
                            ByteBufCodecs.BOOL.decode(buffer),
                            ByteBufCodecs.BOOL.decode(buffer),
                            ByteBufCodecs.VAR_LONG.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            ByteBufCodecs.BOOL.decode(buffer) ? BlockPos.STREAM_CODEC.decode(buffer) : null,
                            ByteBufCodecs.BOOL.decode(buffer),
                            ByteBufCodecs.VAR_INT.decode(buffer),
                            MARKERS_CODEC.decode(buffer)
                    );
                }

                @Override
                public void encode(ByteBuf buffer, PostMatriarchExpeditionSyncPayload payload) {
                    ByteBufCodecs.BOOL.encode(buffer, payload.unlocked());
                    ByteBufCodecs.BOOL.encode(buffer, payload.active());
                    ByteBufCodecs.VAR_LONG.encode(buffer, payload.activePos());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.outcomeId());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.palette());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.progress());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.linkedFlora());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.siteStress());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.siteFlags());
                    ByteBufCodecs.BOOL.encode(buffer, payload.defensePylon() != null);
                    if (payload.defensePylon() != null) BlockPos.STREAM_CODEC.encode(buffer, payload.defensePylon());
                    ByteBufCodecs.BOOL.encode(buffer, payload.pulseInProgress());
                    ByteBufCodecs.VAR_INT.encode(buffer, payload.attackersRemaining());
                    MARKERS_CODEC.encode(buffer, payload.enclaves());
                }
            };

    public record Marker(BlockPos pos, int outcomeId, int palette, int floraCount, boolean protectedZone) {
        public static final StreamCodec<ByteBuf, Marker> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, Marker::pos,
                ByteBufCodecs.VAR_INT, Marker::outcomeId,
                ByteBufCodecs.VAR_INT, Marker::palette,
                ByteBufCodecs.VAR_INT, Marker::floraCount,
                ByteBufCodecs.BOOL, Marker::protectedZone,
                Marker::new
        );
    }

    public static PostMatriarchExpeditionSyncPayload forPlayer(ServerPlayer player) {
        ServerLevel deepLevel = player.serverLevel().getServer().getLevel(ModDimensions.DEEP_MYCELIUM_LEVEL);
        if (deepLevel == null) {
            return empty(false);
        }
        HiveMindSavedData hive = HiveMindSavedData.get(deepLevel);
        PostMatriarchExpeditionData data = PostMatriarchExpeditionData.get(deepLevel);
        BlockPos activePos = data.getActivePos();
        int siteStress = 0;
        int siteFlags = 0;
        if (activePos != null) {
            double stress = DimensionalHomeostasisSavedData.get(deepLevel).getEffectiveNeighborhoodStress(new ChunkPos(activePos));
            siteStress = (int) Math.round(stress);
            if (hive.isFungalTideActive(deepLevel.getGameTime())) siteFlags |= TIDE;
            if (!deepLevel.isLoaded(activePos)) {
                siteFlags |= SITE_UNLOADED;
            } else if (DimensionalHomeostasisEngine.stageForStress(stress) == DimensionalHomeostasisEngine.HomeostasisStage.IMMUNE_RESPONSE
                    && DimensionalHomeostasisDirector.isExposedToCeilingAcidRain(deepLevel, activePos)) {
                siteFlags |= ACID;
            }
            BlockPos pylon = data.getDefensePylon();
            if (pylon != null && deepLevel.isLoaded(pylon)) {
                if (!deepLevel.getBlockState(pylon).is(ModBlocks.PURIFICATION_PYLON.get())) siteFlags |= PYLON_MISSING;
                else if (PurificationZoneTracker.get(deepLevel).isPylonSmothered(pylon)) siteFlags |= PYLON_SMOTHERED;
            }
        }
        BlockPos origin = player.blockPosition();
        List<Marker> markers = data.getEnclaves().stream()
                .sorted(java.util.Comparator.comparingDouble(enclave -> enclave.pos().distSqr(origin)))
                .limit(MAX_ENCLAVE_MARKERS)
                .map(enclave -> new Marker(enclave.pos(), enclave.outcome().id(), enclave.palette(),
                        enclave.floraPositions().size(), PurificationZoneTracker.get(deepLevel).isProtected(enclave.pos())))
                .toList();
        return new PostMatriarchExpeditionSyncPayload(
                hive.isMatriarchDefeated(),
                data.hasActiveExpedition(),
                activePos == null ? 0L : activePos.asLong(),
                data.getActiveOutcome().id(),
                data.getActivePalette(),
                data.getPulsesCompleted(),
                data.getLinkedFlora().size(),
                siteStress,
                siteFlags,
                data.getDefensePylon(),
                data.isPulseInProgress(),
                data.getPulseAttackers().size(),
                markers
        );
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player != null && player.connection != null) {
            syncSummaryToPlayer(player);
            EnclaveDirectoryPayload.refreshPage(player);
        }
    }

    public static void syncSummaryToPlayer(ServerPlayer player) {
        ModNetworking.sendToPlayerIfReady(player, () -> forPlayer(player));
    }

    public static void syncToAll(ServerLevel deepLevel) {
        for (ServerPlayer player : deepLevel.getServer().getPlayerList().getPlayers()) {
            syncToPlayer(player);
        }
    }

    private static PostMatriarchExpeditionSyncPayload empty(boolean unlocked) {
        return new PostMatriarchExpeditionSyncPayload(unlocked, false, 0L, 0, 0, 0, 0, 0, 0, null, false, 0, List.of());
    }

    public static void handleOnClient(PostMatriarchExpeditionSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientExpeditionCache.update(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
