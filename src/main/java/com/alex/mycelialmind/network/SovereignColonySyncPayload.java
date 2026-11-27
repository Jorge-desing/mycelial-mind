package com.alex.mycelialmind.network;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.client.screen.ClientColonyOpener;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.ColonyReconstruction;
import com.alex.mycelialmind.colony.ColonyReconstructionStatus;
import com.alex.mycelialmind.colony.ColonyWitnessMemory;
import com.alex.mycelialmind.hive.ColonyReconstructionDirector;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.colony.ColonySiteHabit;
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
        List<ClientColonyCache.SiteMemorySnapshot> siteMemories,
        boolean memoryTransferActive,
        BlockPos memoryTransferSource,
        BlockPos memoryTransferTarget,
        int memoryTransferHabit,
        boolean memoryTransferInTransit,
        List<ClientColonyCache.ReconstructionSnapshot> reconstructions,
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
            List<WorkerSnapshot> workers = NetworkCodecSupport.decodeClampedList(buf, 32, b -> new WorkerSnapshot(
                    NetworkCodecSupport.readUuid(b),
                    SymbiontCaste.fromIndex(ByteBufCodecs.VAR_INT.decode(b)),
                    SymbiontRank.fromIndex(ByteBufCodecs.VAR_INT.decode(b)),
                    SymbiontTemperament.fromIndex(ByteBufCodecs.VAR_INT.decode(b)),
                    ByteBufCodecs.VAR_INT.decode(b),
                    ByteBufCodecs.VAR_INT.decode(b),
                    ByteBufCodecs.VAR_INT.decode(b),
                    ByteBufCodecs.BOOL.decode(b),
                    ByteBufCodecs.VAR_INT.decode(b)));
            List<ClientColonyCache.SiteMemorySnapshot> memories = NetworkCodecSupport.decodeClampedList(buf, 5,
                    b -> new ClientColonyCache.SiteMemorySnapshot(
                            BlockPos.of(b.readLong()),
                            ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                            ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                            ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                            ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                            ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                            ByteBufCodecs.VAR_INT.decode(b), ByteBufCodecs.VAR_INT.decode(b),
                            NetworkCodecSupport.readOptionalUuid(b),
                            NetworkCodecSupport.readOptionalUuid(b),
                            NetworkCodecSupport.readOptionalUuid(b)));
            boolean transferActive = ByteBufCodecs.BOOL.decode(buf);
            BlockPos transferSource = BlockPos.of(buf.readLong());
            BlockPos transferTarget = BlockPos.of(buf.readLong());
            int transferHabit = ByteBufCodecs.VAR_INT.decode(buf);
            boolean transferInTransit = ByteBufCodecs.BOOL.decode(buf);
            List<ClientColonyCache.ReconstructionSnapshot> reconstructions = NetworkCodecSupport.decodeClampedList(buf, 4,
                    b -> {
                        BlockPos ruined = BlockPos.of(b.readLong());
                        boolean hasSuccessor = ByteBufCodecs.BOOL.decode(b);
                        BlockPos successor = BlockPos.of(b.readLong());
                        long createdTick = b.readLong();
                        int requiredMask = ByteBufCodecs.VAR_INT.decode(b);
                        int restoredMask = ByteBufCodecs.VAR_INT.decode(b);
                        UUID activeWitness = NetworkCodecSupport.readOptionalUuid(b);
                        int activeType = ByteBufCodecs.VAR_INT.decode(b);
                        UUID failedWitness = NetworkCodecSupport.readOptionalUuid(b);
                        int failedType = ByteBufCodecs.VAR_INT.decode(b);
                        int failure = ByteBufCodecs.VAR_INT.decode(b);
                        List<ClientColonyCache.ReconstructionWitnessSnapshot> witnesses =
                                NetworkCodecSupport.decodeClampedList(b, 128, wb ->
                                        new ClientColonyCache.ReconstructionWitnessSnapshot(
                                                NetworkCodecSupport.readUuid(wb),
                                                ByteBufCodecs.VAR_INT.decode(wb),
                                                BlockPos.of(wb.readLong()),
                                                ByteBufCodecs.VAR_INT.decode(wb)));
                        return new ClientColonyCache.ReconstructionSnapshot(ruined, successor, hasSuccessor,
                                createdTick, requiredMask, restoredMask, activeWitness, activeType, failedWitness,
                                failedType, failure, witnesses);
                    });
            boolean openScreen = ByteBufCodecs.BOOL.decode(buf);
            return new SovereignColonySyncPayload(
                    hasColony, unlocked, nexusPos, tier, radius, popCap, biomass, domainXp,
                    traitMask, moralePct, linkedOutposts, inputChests, outputChests,
                    workZones, activeOrders, activeRaidRemaining, workers, memories,
                    transferActive, transferSource, transferTarget, transferHabit, transferInTransit, reconstructions, openScreen
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
            NetworkCodecSupport.encodeBoundedList(buf, payload.workers(), 32, (b, w) -> {
                NetworkCodecSupport.writeUuid(b, w.workerId());
                ByteBufCodecs.VAR_INT.encode(b, w.caste().index());
                ByteBufCodecs.VAR_INT.encode(b, w.rank().index());
                ByteBufCodecs.VAR_INT.encode(b, w.temperament().index());
                ByteBufCodecs.VAR_INT.encode(b, w.nutrition());
                ByteBufCodecs.VAR_INT.encode(b, w.fatigue());
                ByteBufCodecs.VAR_INT.encode(b, w.morale());
                ByteBufCodecs.BOOL.encode(b, w.standby());
                ByteBufCodecs.VAR_INT.encode(b, w.xp());
            });
            NetworkCodecSupport.encodeBoundedList(buf, payload.siteMemories(), 5, (b, memory) -> {
                b.writeLong(memory.sitePos().asLong());
                ByteBufCodecs.VAR_INT.encode(b, memory.learnedHabit());
                ByteBufCodecs.VAR_INT.encode(b, memory.trialHabit());
                ByteBufCodecs.VAR_INT.encode(b, memory.incidentCount());
                ByteBufCodecs.VAR_INT.encode(b, memory.successfulTrials());
                ByteBufCodecs.VAR_INT.encode(b, memory.failedTrials());
                ByteBufCodecs.VAR_INT.encode(b, memory.lastIncidentCode());
                ByteBufCodecs.VAR_INT.encode(b, memory.lastOutcomeCode());
                ByteBufCodecs.VAR_INT.encode(b, memory.episodeMask());
                ByteBufCodecs.VAR_INT.encode(b, memory.escortCall());
                ByteBufCodecs.VAR_INT.encode(b, memory.restCall());
                ByteBufCodecs.VAR_INT.encode(b, memory.pickupCall());
                ByteBufCodecs.VAR_INT.encode(b, memory.lessonStage());
                NetworkCodecSupport.writeOptionalUuid(b, memory.escortActor());
                NetworkCodecSupport.writeOptionalUuid(b, memory.restActor());
                NetworkCodecSupport.writeOptionalUuid(b, memory.pickupActor());
            });
            ByteBufCodecs.BOOL.encode(buf, payload.memoryTransferActive());
            buf.writeLong(payload.memoryTransferSource().asLong());
            buf.writeLong(payload.memoryTransferTarget().asLong());
            ByteBufCodecs.VAR_INT.encode(buf, payload.memoryTransferHabit());
            ByteBufCodecs.BOOL.encode(buf, payload.memoryTransferInTransit());
            NetworkCodecSupport.encodeBoundedList(buf, payload.reconstructions(), 4, (b, reconstruction) -> {
                b.writeLong(reconstruction.ruinedSite().asLong());
                ByteBufCodecs.BOOL.encode(b, reconstruction.hasSuccessor());
                b.writeLong(reconstruction.successorSite().asLong());
                b.writeLong(reconstruction.createdTick());
                ByteBufCodecs.VAR_INT.encode(b, reconstruction.requiredMask());
                ByteBufCodecs.VAR_INT.encode(b, reconstruction.restoredMask());
                NetworkCodecSupport.writeOptionalUuid(b, reconstruction.activeWitness());
                ByteBufCodecs.VAR_INT.encode(b, reconstruction.activeType());
                NetworkCodecSupport.writeOptionalUuid(b, reconstruction.lastFailedWitness());
                ByteBufCodecs.VAR_INT.encode(b, reconstruction.lastFailedType());
                ByteBufCodecs.VAR_INT.encode(b, reconstruction.lastFailure());
                NetworkCodecSupport.encodeBoundedList(b, reconstruction.witnesses(), 128, (wb, witness) -> {
                    NetworkCodecSupport.writeUuid(wb, witness.witnessId());
                    ByteBufCodecs.VAR_INT.encode(wb, witness.type());
                    wb.writeLong(witness.memoryPos().asLong());
                    ByteBufCodecs.VAR_INT.encode(wb, witness.status());
                });
            });
            ByteBufCodecs.BOOL.encode(buf, payload.openScreen());
        }
    };

    public static SovereignColonySyncPayload forNexus(ServerLevel level, @Nullable BlockPos nexusPos, boolean openScreen) {
        boolean unlocked = SovereignNexusBlockEntity.isColonyActive(level, nexusPos);
        SovereignColonySavedData saved = SovereignColonySavedData.get(level);
        SovereignColonyData colony = unlocked && nexusPos != null ? saved.getByNexus(nexusPos) : null;
        if (colony == null) {
            return new SovereignColonySyncPayload(
                    false, unlocked, BlockPos.ZERO, 1, 24, 4, 0, 0, 0, 70,
                    0, 0, 0, 0, 0, 0, List.of(), List.of(), false,
                    BlockPos.ZERO, BlockPos.ZERO, ColonySiteHabit.NONE.ordinal(), false, List.of(), openScreen
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
        List<ClientColonyCache.SiteMemorySnapshot> memories = new ArrayList<>();
        for (SovereignColonyData.SiteMemorySnapshot memory : colony.getSiteMemorySnapshots()) {
            var dialect = colony.getDialect(memory.sitePos());
            int episodeMask = 0;
            for (ColonySiteHabit habit : ColonySiteHabit.values()) {
                if (habit != ColonySiteHabit.NONE && dialect.episode(habit) != null) episodeMask |= 1 << habit.ordinal();
            }
            var lesson = colony.getDialectLesson();
            int lessonStage = lesson != null && lesson.source().equals(memory.sitePos()) ? lesson.stage().ordinal() : -1;
            memories.add(new ClientColonyCache.SiteMemorySnapshot(
                    memory.sitePos(), memory.learnedHabit().ordinal(), memory.trialHabit().ordinal(),
                    memory.incidentCount(), memory.successfulTrials(), memory.failedTrials(),
                    memory.lastIncidentCode(), memory.lastOutcomeCode(), episodeMask,
                    dialect.callMask(ColonySiteHabit.ESCORTED_COURIER), dialect.callMask(ColonySiteHabit.REST_BEFORE_RETURN),
                    dialect.callMask(ColonySiteHabit.STAGGERED_PICKUP), lessonStage,
                    dialect.episode(ColonySiteHabit.ESCORTED_COURIER) == null ? null : dialect.episode(ColonySiteHabit.ESCORTED_COURIER).worker(),
                    dialect.episode(ColonySiteHabit.REST_BEFORE_RETURN) == null ? null : dialect.episode(ColonySiteHabit.REST_BEFORE_RETURN).worker(),
                    dialect.episode(ColonySiteHabit.STAGGERED_PICKUP) == null ? null : dialect.episode(ColonySiteHabit.STAGGERED_PICKUP).worker()));
        }
        List<ClientColonyCache.ReconstructionSnapshot> reconstructions = new ArrayList<>();
        for (ColonyReconstruction entry : colony.getReconstructionEntries()) {
            List<ClientColonyCache.ReconstructionWitnessSnapshot> witnesses = new ArrayList<>();
            for (ColonyWitnessMemory memory : colony.getWitnessMemories(entry.ruinedSite())) {
                if (witnesses.size() >= 64) {
                    break;
                }
                ColonyReconstructionStatus status = ColonyReconstructionDirector.getCandidateStatus(
                        level, colony, entry, memory);
                witnesses.add(new ClientColonyCache.ReconstructionWitnessSnapshot(
                        memory.witnessId(), memory.type().ordinal(), memory.memoryPos(), status.ordinal()));
            }
            reconstructions.add(new ClientColonyCache.ReconstructionSnapshot(
                    entry.ruinedSite(), entry.successorSite() != null ? entry.successorSite() : BlockPos.ZERO,
                    entry.hasSuccessor(), entry.createdTick(), entry.requiredMask(), entry.restoredMask(),
                    entry.activeWitness(), entry.activeType() != null ? entry.activeType().ordinal() : 0,
                    entry.lastFailedWitness(), entry.lastFailedType() != null ? entry.lastFailedType().ordinal() : 0,
                    entry.lastFailure().ordinal(), witnesses));
        }
        SovereignColonyData.MemoryTransferSnapshot transfer = colony.getMemoryTransferSnapshot();
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
                memories,
                transfer != null,
                transfer != null ? transfer.sourcePos() : BlockPos.ZERO,
                transfer != null ? transfer.targetPos() : BlockPos.ZERO,
                transfer != null ? transfer.habit().ordinal() : ColonySiteHabit.NONE.ordinal(),
                transfer != null && transfer.inTransit(),
                reconstructions,
                openScreen
        );
    }

    public static void syncToPlayer(ServerPlayer player, @Nullable BlockPos nexusPos, boolean openScreen) {
        ModNetworking.sendToPlayerIfReady(player, () -> forNexus(player.serverLevel(), nexusPos, openScreen));
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
