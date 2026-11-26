package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.client.screen.ClientCodexOpener;
import com.alex.mycelialmind.hive.HiveCycleSavedData;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Synchronizes Post-Matriarch Hive Cycle state, mutators, and score records to clients.
 */
public record CycleProtocolSyncPayload(
        boolean unlocked,
        boolean cycleActive,
        int activeCycleNumber,
        int activeMutatorMask,
        int elapsedSeconds,
        int cycleDeaths,
        int completedCycles,
        int bestScore,
        int bestCycleNumber,
        int lastCompletedScore,
        String lastCompletedGrade,
        boolean openScreen
) implements CustomPacketPayload {
    public static final Type<CycleProtocolSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "cycle_protocol_sync"));

    public static final StreamCodec<ByteBuf, CycleProtocolSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CycleProtocolSyncPayload decode(ByteBuf buf) {
            return new CycleProtocolSyncPayload(
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.BOOL.decode(buf)
            );
        }

        @Override
        public void encode(ByteBuf buf, CycleProtocolSyncPayload payload) {
            ByteBufCodecs.BOOL.encode(buf, payload.unlocked());
            ByteBufCodecs.BOOL.encode(buf, payload.cycleActive());
            ByteBufCodecs.VAR_INT.encode(buf, payload.activeCycleNumber());
            ByteBufCodecs.VAR_INT.encode(buf, payload.activeMutatorMask());
            ByteBufCodecs.VAR_INT.encode(buf, payload.elapsedSeconds());
            ByteBufCodecs.VAR_INT.encode(buf, payload.cycleDeaths());
            ByteBufCodecs.VAR_INT.encode(buf, payload.completedCycles());
            ByteBufCodecs.VAR_INT.encode(buf, payload.bestScore());
            ByteBufCodecs.VAR_INT.encode(buf, payload.bestCycleNumber());
            ByteBufCodecs.VAR_INT.encode(buf, payload.lastCompletedScore());
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.lastCompletedGrade() != null ? payload.lastCompletedGrade() : "");
            ByteBufCodecs.BOOL.encode(buf, payload.openScreen());
        }
    };

    public static CycleProtocolSyncPayload forLevel(ServerLevel level, boolean openScreen) {
        ServerLevel overworld = level.getServer().overworld();
        boolean unlocked = HiveMindSavedData.get(overworld).isMatriarchDefeated()
                || HiveMindSavedData.get(level).isMatriarchDefeated();
        HiveCycleSavedData cycles = HiveCycleSavedData.get(overworld);
        long now = overworld.getGameTime();
        int elapsedSec = cycles.isCycleActive()
                ? (int) Math.max(0L, (now - cycles.getCycleStartGameTime()) / 20L)
                : 0;
        return new CycleProtocolSyncPayload(
                unlocked,
                cycles.isCycleActive(),
                cycles.getActiveCycleNumber(),
                cycles.getActiveMutatorMask(),
                elapsedSec,
                cycles.getCycleDeaths(),
                cycles.getCompletedCycles(),
                cycles.getBestScore(),
                cycles.getBestCycleNumber(),
                cycles.getLastCompletedScore(),
                cycles.getLastCompletedGrade(),
                openScreen
        );
    }

    public static void syncToPlayer(ServerPlayer player, boolean openScreen) {
        ModNetworking.sendToPlayerIfReady(player, () -> forLevel(player.serverLevel(), openScreen));
    }

    public static void syncToAll(ServerLevel level) {
        CycleProtocolSyncPayload payload = forLevel(level, false);
        for (ServerPlayer sp : level.getServer().getPlayerList().getPlayers()) {
            if (sp.connection != null) {
                PacketDistributor.sendToPlayer(sp, payload);
            }
        }
    }

    public static void handleOnClient(CycleProtocolSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientCycleCache.update(payload);
            if (payload.openScreen()) {
                ClientCodexOpener.openCycleProtocolScreen();
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
