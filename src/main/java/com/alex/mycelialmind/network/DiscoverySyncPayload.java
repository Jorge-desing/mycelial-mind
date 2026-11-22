package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Synchronizes player bestiary, experimental secrets, milestones, and bastion score to the client.
 */
public record DiscoverySyncPayload(
        int scannedSpeciesMask,
        int discoveredTraitsMask,
        int discoveredStrainsMask,
        int discoveredSecretsMask,
        int craftedMilestonesMask,
        int rareVariantsFound,
        int bastionScore,
        int highestActTier
) implements CustomPacketPayload {
    public static final Type<DiscoverySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "discovery_sync"));

    public static final StreamCodec<ByteBuf, DiscoverySyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DiscoverySyncPayload decode(ByteBuf buf) {
            return new DiscoverySyncPayload(
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf)
            );
        }

        @Override
        public void encode(ByteBuf buf, DiscoverySyncPayload payload) {
            ByteBufCodecs.VAR_INT.encode(buf, payload.scannedSpeciesMask());
            ByteBufCodecs.VAR_INT.encode(buf, payload.discoveredTraitsMask());
            ByteBufCodecs.VAR_INT.encode(buf, payload.discoveredStrainsMask());
            ByteBufCodecs.VAR_INT.encode(buf, payload.discoveredSecretsMask());
            ByteBufCodecs.VAR_INT.encode(buf, payload.craftedMilestonesMask());
            ByteBufCodecs.VAR_INT.encode(buf, payload.rareVariantsFound());
            ByteBufCodecs.VAR_INT.encode(buf, payload.bastionScore());
            ByteBufCodecs.VAR_INT.encode(buf, payload.highestActTier());
        }
    };

    public static DiscoverySyncPayload forPlayer(ServerPlayer player) {
        PlayerDiscoveryData data = player.getData(ModAttachments.DISCOVERY);
        int highestActTier = StructureProgressionPolicy.resolveTier(player.serverLevel()).level();
        return new DiscoverySyncPayload(
                data.getScannedSpeciesMask(),
                data.getDiscoveredTraitsMask(),
                data.getDiscoveredStrainsMask(),
                data.getDiscoveredSecretsMask(),
                data.getCraftedMilestonesMask(),
                data.getRareVariantsFound(),
                data.getBastionScore(),
                highestActTier
        );
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, forPlayer(player));
    }

    public static void handleOnClient(DiscoverySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientDiscoveryCache.update(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
