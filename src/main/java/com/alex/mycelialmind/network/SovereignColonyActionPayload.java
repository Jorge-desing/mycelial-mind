package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.colony.SymbiontCaste;
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

import java.util.UUID;

/**
 * Client-to-server control actions for the Sovereign Nexus screen.
 */
public record SovereignColonyActionPayload(
        int action,
        BlockPos nexusPos,
        int param,
        UUID targetWorkerId
) implements CustomPacketPayload {
    public static final int ACTION_INCUBATE = 1;
    public static final int ACTION_UPGRADE_TIER = 2;
    public static final int ACTION_RECALL_ALL = 3;
    public static final int ACTION_CYCLE_WORKER_CASTE = 4;
    public static final int ACTION_TOGGLE_WORKER_STANDBY = 5;
    public static final int ACTION_REQUEST_SYNC = 6;

    private static final UUID ZERO_UUID = new UUID(0L, 0L);

    public static final Type<SovereignColonyActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "sovereign_colony_action"));

    public static final StreamCodec<ByteBuf, SovereignColonyActionPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SovereignColonyActionPayload decode(ByteBuf buf) {
            int action = ByteBufCodecs.VAR_INT.decode(buf);
            BlockPos pos = BlockPos.of(buf.readLong());
            int param = ByteBufCodecs.VAR_INT.decode(buf);
            UUID workerId = new UUID(buf.readLong(), buf.readLong());
            return new SovereignColonyActionPayload(action, pos, param, workerId);
        }

        @Override
        public void encode(ByteBuf buf, SovereignColonyActionPayload payload) {
            ByteBufCodecs.VAR_INT.encode(buf, payload.action());
            buf.writeLong(payload.nexusPos().asLong());
            ByteBufCodecs.VAR_INT.encode(buf, payload.param());
            UUID id = payload.targetWorkerId() != null ? payload.targetWorkerId() : ZERO_UUID;
            buf.writeLong(id.getMostSignificantBits());
            buf.writeLong(id.getLeastSignificantBits());
        }
    };

    public static void sendToServer(int action, BlockPos nexusPos, int param, UUID workerId) {
        PacketDistributor.sendToServer(new SovereignColonyActionPayload(
                action,
                nexusPos != null ? nexusPos : BlockPos.ZERO,
                param,
                workerId != null ? workerId : ZERO_UUID
        ));
    }

    public static void handleOnServer(SovereignColonyActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> {
            ServerLevel level = player.serverLevel();
            BlockPos nexusPos = payload.nexusPos();
            if (!level.isLoaded(nexusPos) || player.blockPosition().distSqr(nexusPos) > 128.0D * 128.0D) {
                return;
            }
            if (!(level.getBlockEntity(nexusPos) instanceof SovereignNexusBlockEntity nexus)) {
                return;
            }
            switch (payload.action()) {
                case ACTION_INCUBATE -> nexus.incubateSymbiont(level, player, SymbiontCaste.fromIndex(payload.param()), false);
                case ACTION_UPGRADE_TIER -> nexus.upgradeTier(level, player, false);
                case ACTION_RECALL_ALL -> nexus.recallAllWorkers(level);
                case ACTION_CYCLE_WORKER_CASTE -> {
                    if (level.getEntity(payload.targetWorkerId()) instanceof com.alex.mycelialmind.entity.ColonySymbiontEntity worker) {
                        nexus.setWorkerCaste(level, payload.targetWorkerId(), worker.getCaste().next());
                    }
                }
                case ACTION_TOGGLE_WORKER_STANDBY -> nexus.toggleWorkerStandby(level, payload.targetWorkerId());
                case ACTION_REQUEST_SYNC -> {
                }
                default -> {
                }
            }
            nexus.syncLiveWorkers(level, nexus.getOrCreateColony(level));
            SovereignColonySyncPayload.syncToPlayer(player, nexusPos, false);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
