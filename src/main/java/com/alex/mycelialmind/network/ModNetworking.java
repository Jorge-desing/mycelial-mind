package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registers custom network payloads for Mycelial Mind.
 */
public final class ModNetworking {
    private ModNetworking() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetworking::onRegisterPayloads);
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(MycelialMind.MODID).versioned("20");
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
        registrar.playToServer(
                PinExpeditionGoalPayload.TYPE,
                PinExpeditionGoalPayload.STREAM_CODEC,
                PinExpeditionGoalPayload::handleOnServer
        );
    }
}
