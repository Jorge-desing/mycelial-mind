package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.client.screen.ClientColonyOpener;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.colony.SymbiontRank;
import com.alex.mycelialmind.colony.SymbiontTemperament;
import com.alex.mycelialmind.network.ClientColonyCache.WorkerSnapshot;
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Synchronizes Sovereign Hive Colony telemetry and worker roster to the client.
 */
public record SovereignColonySyncPayload(
        boolean hasColony,
        boolean unlocked,
        BlockPos nexusPos,
        int tier,
        int radius,
        int popCap,
        int biomass,
        int domainXp,
        int traitMask,
        int moralePct,
        int linkedOutposts,
        int inputChests,
        int outputChests,
        int workZones,
        int activeOrders,
        int activeRaidRemaining,
        List<WorkerSnapshot> workers,
        boolean openScreen
) implements CustomPacketPayload {
    public static final Type<SovereignColonySyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "sovereign_colony_sync"));

    public static final StreamCodec<ByteBuf, SovereignColonySyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SovereignColonySyncPayload decode(ByteBuf buf) {
            boolean hasColony = ByteBufCodecs.BOOL.decode(buf);
            boolean unlocked = ByteBufCodecs.BOOL.decode(buf);
            BlockPos nexusPos = BlockPos.of(buf.readLong());
            int tier = ByteBufCodecs.VAR_INT.decode(buf);
            int radius = ByteBufCodecs.VAR_INT.decode(buf);
            int popCap = ByteBufCodecs.VAR_INT.decode(buf);
            int biomass = ByteBufCodecs.VAR_INT.decode(buf);
            int domainXp = ByteBufCodecs.VAR_INT.decode(buf);
            int traitMask = ByteBufCodecs.VAR_INT.decode(buf);
            int moralePct = ByteBufCodecs.VAR_INT.decode(buf);
            int linkedOutposts = ByteBufCodecs.VAR_INT.decode(buf);
            int inputChests = ByteBufCodecs.VAR_INT.decode(buf);
            int outputChests = ByteBufCodecs.VAR_INT.decode(buf);
            int workZones = ByteBufCodecs.VAR_INT.decode(buf);
            int activeOrders = ByteBufCodecs.VAR_INT.decode(buf);
            int activeRaidRemaining = ByteBufCodecs.VAR_INT.decode(buf);
            int count = Math.max(0, Math.min(32, ByteBufCodecs.VAR_INT.decode(buf)));
            List<WorkerSnapshot> workers = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                UUID id = new UUID(buf.readLong(), buf.readLong());
                SymbiontCaste caste = SymbiontCaste.fromIndex(ByteBufCodecs.VAR_INT.decode(buf));
                SymbiontRank rank = SymbiontRank.fromIndex(ByteBufCodecs.VAR_INT.decode(buf));
                SymbiontTemperament temp = SymbiontTemperament.fromIndex(ByteBufCodecs.VAR_INT.decode(buf));
                int nut = ByteBufCodecs.VAR_INT.decode(buf);
                int fat = ByteBufCodecs.VAR_INT.decode(buf);
                int mor = ByteBufCodecs.VAR_INT.decode(buf);
                boolean standby = ByteBufCodecs.BOOL.decode(buf);
                int xp = ByteBufCodecs.VAR_INT.decode(buf);
                workers.add(new WorkerSnapshot(id, caste, rank, temp, nut, fat, mor, standby, xp));
            }
            boolean openScreen = ByteBufCodecs.BOOL.decode(buf);
            return new SovereignColonySyncPayload(
                    hasColony, unlocked, nexusPos, tier, radius, popCap, biomass, domainXp,
                    traitMask, moralePct, linkedOutposts, inputChests, outputChests,
                    workZones, activeOrders, activeRaidRemaining, workers, openScreen
            );
        }

        @Override
        public void encode(ByteBuf buf, SovereignColonySyncPayload payload) {
            ByteBufCodecs.BOOL.encode(buf, payload.hasColony());
            ByteBufCodecs.BOOL.encode(buf, payload.unlocked());
            buf.writeLong(payload.nexusPos().asLong());
            ByteBufCodecs.VAR_INT.encode(buf, payload.tier());
            ByteBufCodecs.VAR_INT.encode(buf, payload.radius());
            ByteBufCodecs.VAR_INT.encode(buf, payload.popCap());
            ByteBufCodecs.VAR_INT.encode(buf, payload.biomass());
            ByteBufCodecs.VAR_INT.encode(buf, payload.domainXp());
            ByteBufCodecs.VAR_INT.encode(buf, payload.traitMask());
            ByteBufCodecs.VAR_INT.encode(buf, payload.moralePct());
            ByteBufCodecs.VAR_INT.encode(buf, payload.linkedOutposts());
            ByteBufCodecs.VAR_INT.encode(buf, payload.inputChests());
            ByteBufCodecs.VAR_INT.encode(buf, payload.outputChests());
            ByteBufCodecs.VAR_INT.encode(buf, payload.workZones());
            ByteBufCodecs.VAR_INT.encode(buf, payload.activeOrders());
            ByteBufCodecs.VAR_INT.encode(buf, payload.activeRaidRemaining());
            List<WorkerSnapshot> list = payload.workers() != null ? payload.workers() : List.of();
            int count = Math.min(32, list.size());
            ByteBufCodecs.VAR_INT.encode(buf, count);
            for (int i = 0; i < count; i++) {
                WorkerSnapshot w = list.get(i);
                buf.writeLong(w.workerId().getMostSignificantBits());
                buf.writeLong(w.workerId().getLeastSignificantBits());
                ByteBufCodecs.VAR_INT.encode(buf, w.caste().index());
                ByteBufCodecs.VAR_INT.encode(buf, w.rank().index());
                ByteBufCodecs.VAR_INT.encode(buf, w.temperament().index());
                ByteBufCodecs.VAR_INT.encode(buf, w.nutrition());
                ByteBufCodecs.VAR_INT.encode(buf, w.fatigue());
                ByteBufCodecs.VAR_INT.encode(buf, w.morale());
                ByteBufCodecs.BOOL.encode(buf, w.standby());
                ByteBufCodecs.VAR_INT.encode(buf, w.xp());
            }
            ByteBufCodecs.BOOL.encode(buf, payload.openScreen());
        }
    };

    public static SovereignColonySyncPayload forNexus(ServerLevel level, @Nullable BlockPos nexusPos, boolean openScreen) {
        boolean unlocked = SovereignNexusBlockEntity.isSovereignUnlocked(level);
        SovereignColonySavedData saved = SovereignColonySavedData.get(level);
        SovereignColonyData colony = nexusPos != null ? saved.getByNexus(nexusPos) : null;
        if (colony == null) {
            return new SovereignColonySyncPayload(
                    false, unlocked, BlockPos.ZERO, 1, 24, 4, 0, 0, 0, 70,
                    0, 0, 0, 0, 0, 0, List.of(), openScreen
            );
        }
        List<WorkerSnapshot> roster = new ArrayList<>();
        for (var e : colony.getRoster().values()) {
            roster.add(new WorkerSnapshot(
                    e.workerId(),
                    e.caste(),
                    e.rank(),
                    e.temperament(),
                    (int) Math.round(e.nutrition()),
                    (int) Math.round(e.fatigue()),
                    (int) Math.round(e.morale()),
                    e.standby(),
                    e.xp()
            ));
        }
        return new SovereignColonySyncPayload(
                true,
                unlocked,
                colony.getNexusPos(),
                colony.getTier(),
                colony.getEffectiveRadius(),
                colony.getPopulationCap(),
                colony.getBiomassStored(),
                colony.getDomainXp(),
                colony.getUnlockedTraitMask(),
                (int) Math.round(colony.getColonyMorale()),
                colony.getLinkedOutposts().size(),
                colony.getInputChests().size(),
                colony.getOutputChests().size(),
                colony.getTotalWorkZoneCount(),
                colony.getOrderBoard().size(),
                colony.getActiveRaidRemaining(),
                roster,
                openScreen
        );
    }

    public static void syncToPlayer(ServerPlayer player, @Nullable BlockPos nexusPos, boolean openScreen) {
        if (player == null || player.connection == null) {
            return;
        }
        PacketDistributor.sendToPlayer(player, forNexus(player.serverLevel(), nexusPos, openScreen));
    }

    public static void handleOnClient(SovereignColonySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientColonyCache.update(payload);
            if (payload.openScreen()) {
                ClientColonyOpener.openSovereignNexusScreen();
            }
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
