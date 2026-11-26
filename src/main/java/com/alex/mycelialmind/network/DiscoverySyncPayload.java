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
        int highestActTier,
        int lastObservationCode,
        String lastObservationDimension,
        int pinnedExpeditionGoal,
        int researchStageMask,
        int recordedThreatPermille,
        int lastThreatDropPermille,
        int discoveredEngramsMask
) implements CustomPacketPayload {
    public static final Type<DiscoverySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "discovery_sync"));

    public DiscoverySyncPayload(
            int scannedSpeciesMask,
            int discoveredTraitsMask,
            int discoveredStrainsMask,
            int discoveredSecretsMask,
            int craftedMilestonesMask,
            int rareVariantsFound,
            int bastionScore,
            int highestActTier
    ) {
        this(scannedSpeciesMask, discoveredTraitsMask, discoveredStrainsMask, discoveredSecretsMask,
                craftedMilestonesMask, rareVariantsFound, bastionScore, highestActTier,
                0, "", 0, 0, 0, 0, 0);
    }

    public DiscoverySyncPayload(
            int scannedSpeciesMask,
            int discoveredTraitsMask,
            int discoveredStrainsMask,
            int discoveredSecretsMask,
            int craftedMilestonesMask,
            int rareVariantsFound,
            int bastionScore,
            int highestActTier,
            int lastObservationCode,
            String lastObservationDimension,
            int pinnedExpeditionGoal,
            int researchStageMask,
            int recordedThreatPermille,
            int lastThreatDropPermille
    ) {
        this(scannedSpeciesMask, discoveredTraitsMask, discoveredStrainsMask, discoveredSecretsMask,
                craftedMilestonesMask, rareVariantsFound, bastionScore, highestActTier,
                lastObservationCode, lastObservationDimension, pinnedExpeditionGoal,
                researchStageMask, recordedThreatPermille, lastThreatDropPermille, 0);
    }

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
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
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
            ByteBufCodecs.VAR_INT.encode(buf, payload.lastObservationCode());
            ByteBufCodecs.STRING_UTF8.encode(buf, payload.lastObservationDimension() != null ? payload.lastObservationDimension() : "");
            ByteBufCodecs.VAR_INT.encode(buf, payload.pinnedExpeditionGoal());
            ByteBufCodecs.VAR_INT.encode(buf, payload.researchStageMask());
            ByteBufCodecs.VAR_INT.encode(buf, payload.recordedThreatPermille());
            ByteBufCodecs.VAR_INT.encode(buf, payload.lastThreatDropPermille());
            ByteBufCodecs.VAR_INT.encode(buf, payload.discoveredEngramsMask());
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
                highestActTier,
                data.getLastObservationCode(),
                data.getLastObservationDimension(),
                data.getPinnedExpeditionGoalId(),
                data.getResearchStageMask(),
                data.getRecordedThreatPermille(),
                data.getLastThreatDropPermille(),
                data.getDiscoveredEngramsMask()
        );
    }

    public static void syncToPlayer(ServerPlayer player) {
        ModNetworking.sendToPlayerIfReady(player, () -> forPlayer(player));
    }

    public static void handleOnClient(DiscoverySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientDiscoveryCache.update(payload));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
