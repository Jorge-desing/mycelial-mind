package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import javax.annotation.Nullable;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Registers custom network payloads for Mycelial Mind.
 */
public final class ModNetworking {
    private ModNetworking() {
    }

    public static void sendToPlayerIfReady(
            @Nullable ServerPlayer player, Supplier<? extends CustomPacketPayload> payloadSupplier) {
        if (player == null || player.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, payloadSupplier.get());
    }

    public static void sendToAllReady(ServerLevel level, CustomPacketPayload payload) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.connection != null) {
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    public static void sendToPlayersInLevelReady(
            ServerLevel level, Function<ServerPlayer, ? extends CustomPacketPayload> payloadFactory) {
        for (ServerPlayer player : level.players()) {
            if (player.connection != null) {
                PacketDistributor.sendToPlayer(player, payloadFactory.apply(player));
            }
        }
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetworking::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MycelialMind.MODID).versioned("27");
        registrar.playToClient(EnclaveDirectoryPayload.TYPE, EnclaveDirectoryPayload.STREAM_CODEC, EnclaveDirectoryPayload::handleOnClient);
        registrar.playToServer(EnclaveDirectoryRequestPayload.TYPE, EnclaveDirectoryRequestPayload.STREAM_CODEC, EnclaveDirectoryRequestPayload::handleOnServer);
        registrar.playToServer(EnclaveActionPayload.TYPE, EnclaveActionPayload.STREAM_CODEC, EnclaveActionPayload::handleOnServer);
        registrar.playToClient(
                HiveSyncPayload.TYPE,
                HiveSyncPayload.STREAM_CODEC,
                HiveSyncPayload::handleOnClient
        );
        registrar.playToClient(
                SymbiosisSyncPayload.TYPE,
                SymbiosisSyncPayload.STREAM_CODEC,
                SymbiosisSyncPayload::handleOnClient
        );
        registrar.playToClient(
                DiplomacySyncPayload.TYPE,
                DiplomacySyncPayload.STREAM_CODEC,
                DiplomacySyncPayload::handleOnClient
        );
        registrar.playToClient(
                DiscoverySyncPayload.TYPE,
                DiscoverySyncPayload.STREAM_CODEC,
                DiscoverySyncPayload::handleOnClient
        );
        registrar.playToClient(
                PostMatriarchExpeditionSyncPayload.TYPE,
                PostMatriarchExpeditionSyncPayload.STREAM_CODEC,
                PostMatriarchExpeditionSyncPayload::handleOnClient
        );
        registrar.playToClient(
                CycleProtocolSyncPayload.TYPE,
                CycleProtocolSyncPayload.STREAM_CODEC,
                CycleProtocolSyncPayload::handleOnClient
        );
        registrar.playToServer(
                CycleProtocolActionPayload.TYPE,
                CycleProtocolActionPayload.STREAM_CODEC,
                CycleProtocolActionPayload::handleOnServer
        );
        registrar.playToServer(
                PinExpeditionGoalPayload.TYPE,
                PinExpeditionGoalPayload.STREAM_CODEC,
                PinExpeditionGoalPayload::handleOnServer
        );
        registrar.playToClient(
                SovereignColonySyncPayload.TYPE,
                SovereignColonySyncPayload.STREAM_CODEC,
                SovereignColonySyncPayload::handleOnClient
        );
        registrar.playToServer(
                SovereignColonyActionPayload.TYPE,
                SovereignColonyActionPayload.STREAM_CODEC,
                SovereignColonyActionPayload::handleOnServer
        );
    }
}
