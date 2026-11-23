package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record EnclaveDirectoryRequestPayload(int page) implements CustomPacketPayload {
    public static final Type<EnclaveDirectoryRequestPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "enclave_directory_request"));
    public static final StreamCodec<ByteBuf, EnclaveDirectoryRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnclaveDirectoryRequestPayload::page, EnclaveDirectoryRequestPayload::new);
    public static void request(int page) { PacketDistributor.sendToServer(new EnclaveDirectoryRequestPayload(page)); }
    public static void handleOnServer(EnclaveDirectoryRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) EnclaveDirectoryPayload.sendPage(player, payload.page()); });
    }
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
