package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.hive.HiveStrain;
import com.alex.mycelialmind.registry.ModAttachments;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Synchronizes player strain reputation, truce duration, and betrayal cooldown to the client.
 */
public record DiplomacySyncPayload(
        int amberRep,
        int crimsonRep,
        int truceTicks,
        int betrayalTicks,
        int selectedStrainId
) implements CustomPacketPayload {
    public static final Type<DiplomacySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "diplomacy_sync"));

    public static final StreamCodec<ByteBuf, DiplomacySyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            DiplomacySyncPayload::amberRep,
            ByteBufCodecs.VAR_INT,
            DiplomacySyncPayload::crimsonRep,
            ByteBufCodecs.VAR_INT,
            DiplomacySyncPayload::truceTicks,
            ByteBufCodecs.VAR_INT,
            DiplomacySyncPayload::betrayalTicks,
            ByteBufCodecs.VAR_INT,
            DiplomacySyncPayload::selectedStrainId,
            DiplomacySyncPayload::new
    );

    public static DiplomacySyncPayload forPlayer(ServerPlayer player) {
        PlayerDiplomacyData data = player.getData(ModAttachments.DIPLOMACY);
        return new DiplomacySyncPayload(
                data.getReputation(HiveStrain.AMBER),
                data.getReputation(HiveStrain.CRIMSON),
                data.getTruceTicks(),
                data.getBetrayalCooldownTicks(),
                data.getSelectedStrain().id()
        );
    }

    public static void syncToPlayer(ServerPlayer player) {
        ModNetworking.sendToPlayerIfReady(player, () -> forPlayer(player));
    }

    public static void handleOnClient(DiplomacySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientDiplomacyCache.update(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
