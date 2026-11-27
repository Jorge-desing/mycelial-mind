package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.HiveCycleDirector;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client-to-server action payload for starting, aborting, or refreshing a Post-Matriarch Hive Cycle.
 */
public record CycleProtocolActionPayload(
        int action,
        int mutatorMask
) implements CustomPacketPayload {
    public static final int ACTION_START = 1;
    public static final int ACTION_ABORT = 2;
    public static final int ACTION_REQUEST_SYNC = 3;

    public static final Type<CycleProtocolActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "cycle_protocol_action"));

    public static final StreamCodec<ByteBuf, CycleProtocolActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            CycleProtocolActionPayload::action,
            ByteBufCodecs.VAR_INT,
            CycleProtocolActionPayload::mutatorMask,
            CycleProtocolActionPayload::new
    );

    public static void sendToServer(int action, int mutatorMask) {
        PacketDistributor.sendToServer(new CycleProtocolActionPayload(action, mutatorMask));
    }

    public static void handleOnServer(CycleProtocolActionPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        context.enqueueWork(() -> {
            switch (payload.action()) {
                case ACTION_START -> HiveCycleDirector.startCycle(player, player.serverLevel(), payload.mutatorMask());
                case ACTION_ABORT -> HiveCycleDirector.abortCycle(player, player.serverLevel());
                case ACTION_REQUEST_SYNC -> CycleProtocolSyncPayload.syncToPlayer(player, false);
                default -> {
                }
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
