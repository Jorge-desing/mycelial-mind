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
            int memoryCount = Math.max(0, Math.min(5, ByteBufCodecs.VAR_INT.decode(buf)));
            List<ClientColonyCache.SiteMemorySnapshot> memories = new ArrayList<>(memoryCount);
            for (int i = 0; i < memoryCount; i++) {
                BlockPos sitePos = BlockPos.of(buf.readLong());
                int learnedHabit = ByteBufCodecs.VAR_INT.decode(buf);
                int trialHabit = ByteBufCodecs.VAR_INT.decode(buf);
                int incidents = ByteBufCodecs.VAR_INT.decode(buf);
                int successes = ByteBufCodecs.VAR_INT.decode(buf);
                int failures = ByteBufCodecs.VAR_INT.decode(buf);
                int incidentCode = ByteBufCodecs.VAR_INT.decode(buf);
                int outcomeCode = ByteBufCodecs.VAR_INT.decode(buf);
                memories.add(new ClientColonyCache.SiteMemorySnapshot(sitePos, learnedHabit, trialHabit,
                        incidents, successes, failures, incidentCode, outcomeCode,
                        ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                        ByteBufCodecs.VAR_INT.decode(buf), readOptionalUuid(buf), readOptionalUuid(buf), readOptionalUuid(buf)));
            }
            boolean transferActive = ByteBufCodecs.BOOL.decode(buf);
            BlockPos transferSource = BlockPos.of(buf.readLong());
            BlockPos transferTarget = BlockPos.of(buf.readLong());
            int transferHabit = ByteBufCodecs.VAR_INT.decode(buf);
            boolean transferInTransit = ByteBufCodecs.BOOL.decode(buf);
            int reconstructionCount = Math.max(0, Math.min(4, ByteBufCodecs.VAR_INT.decode(buf)));
            List<ClientColonyCache.ReconstructionSnapshot> reconstructions = new ArrayList<>(reconstructionCount);
            for (int i = 0; i < reconstructionCount; i++) {
                BlockPos ruined = BlockPos.of(buf.readLong());
                boolean hasSuccessor = ByteBufCodecs.BOOL.decode(buf);
                BlockPos successor = BlockPos.of(buf.readLong());
                long createdTick = buf.readLong();
                int requiredMask = ByteBufCodecs.VAR_INT.decode(buf);
                int restoredMask = ByteBufCodecs.VAR_INT.decode(buf);
                UUID activeWitness = readOptionalUuid(buf);
                int activeType = ByteBufCodecs.VAR_INT.decode(buf);
                UUID failedWitness = readOptionalUuid(buf);
                int failedType = ByteBufCodecs.VAR_INT.decode(buf);
                int failure = ByteBufCodecs.VAR_INT.decode(buf);
                int witnessCount = Math.max(0, Math.min(128, ByteBufCodecs.VAR_INT.decode(buf)));
                List<ClientColonyCache.ReconstructionWitnessSnapshot> witnesses = new ArrayList<>(witnessCount);
                for (int j = 0; j < witnessCount; j++) {
                    UUID witnessId = new UUID(buf.readLong(), buf.readLong());
                    int type = ByteBufCodecs.VAR_INT.decode(buf);
                    BlockPos memoryPos = BlockPos.of(buf.readLong());
                    int status = ByteBufCodecs.VAR_INT.decode(buf);
                    witnesses.add(new ClientColonyCache.ReconstructionWitnessSnapshot(witnessId, type, memoryPos, status));
                }
                reconstructions.add(new ClientColonyCache.ReconstructionSnapshot(ruined, successor, hasSuccessor,
                        createdTick, requiredMask, restoredMask, activeWitness, activeType, failedWitness,
                        failedType, failure, witnesses));
            }
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
            List<ClientColonyCache.SiteMemorySnapshot> memories = payload.siteMemories() != null
                    ? payload.siteMemories() : List.of();
            int memoryCount = Math.min(5, memories.size());
            ByteBufCodecs.VAR_INT.encode(buf, memoryCount);
            for (int i = 0; i < memoryCount; i++) {
                ClientColonyCache.SiteMemorySnapshot memory = memories.get(i);
                buf.writeLong(memory.sitePos().asLong());
                ByteBufCodecs.VAR_INT.encode(buf, memory.learnedHabit());
                ByteBufCodecs.VAR_INT.encode(buf, memory.trialHabit());
                ByteBufCodecs.VAR_INT.encode(buf, memory.incidentCount());
                ByteBufCodecs.VAR_INT.encode(buf, memory.successfulTrials());
                ByteBufCodecs.VAR_INT.encode(buf, memory.failedTrials());
                ByteBufCodecs.VAR_INT.encode(buf, memory.lastIncidentCode());
                ByteBufCodecs.VAR_INT.encode(buf, memory.lastOutcomeCode());
                ByteBufCodecs.VAR_INT.encode(buf, memory.episodeMask());
                ByteBufCodecs.VAR_INT.encode(buf, memory.escortCall());
                ByteBufCodecs.VAR_INT.encode(buf, memory.restCall());
                ByteBufCodecs.VAR_INT.encode(buf, memory.pickupCall());
                ByteBufCodecs.VAR_INT.encode(buf, memory.lessonStage());
                writeOptionalUuid(buf, memory.escortActor());
                writeOptionalUuid(buf, memory.restActor());
                writeOptionalUuid(buf, memory.pickupActor());
            }
            ByteBufCodecs.BOOL.encode(buf, payload.memoryTransferActive());
            buf.writeLong(payload.memoryTransferSource().asLong());
            buf.writeLong(payload.memoryTransferTarget().asLong());
            ByteBufCodecs.VAR_INT.encode(buf, payload.memoryTransferHabit());
            ByteBufCodecs.BOOL.encode(buf, payload.memoryTransferInTransit());
            List<ClientColonyCache.ReconstructionSnapshot> reconstructions = payload.reconstructions() != null
                    ? payload.reconstructions() : List.of();
            int reconstructionCount = Math.min(4, reconstructions.size());
            ByteBufCodecs.VAR_INT.encode(buf, reconstructionCount);
            for (int i = 0; i < reconstructionCount; i++) {
                ClientColonyCache.ReconstructionSnapshot reconstruction = reconstructions.get(i);
                buf.writeLong(reconstruction.ruinedSite().asLong());
                ByteBufCodecs.BOOL.encode(buf, reconstruction.hasSuccessor());
                buf.writeLong(reconstruction.successorSite().asLong());
                buf.writeLong(reconstruction.createdTick());
                ByteBufCodecs.VAR_INT.encode(buf, reconstruction.requiredMask());
                ByteBufCodecs.VAR_INT.encode(buf, reconstruction.restoredMask());
                writeOptionalUuid(buf, reconstruction.activeWitness());
                ByteBufCodecs.VAR_INT.encode(buf, reconstruction.activeType());
                writeOptionalUuid(buf, reconstruction.lastFailedWitness());
                ByteBufCodecs.VAR_INT.encode(buf, reconstruction.lastFailedType());
                ByteBufCodecs.VAR_INT.encode(buf, reconstruction.lastFailure());
                List<ClientColonyCache.ReconstructionWitnessSnapshot> witnesses = reconstruction.witnesses() != null
                        ? reconstruction.witnesses() : List.of();
                int witnessCount = Math.min(128, witnesses.size());
                ByteBufCodecs.VAR_INT.encode(buf, witnessCount);
                for (int j = 0; j < witnessCount; j++) {
                    ClientColonyCache.ReconstructionWitnessSnapshot witness = witnesses.get(j);
                    buf.writeLong(witness.witnessId().getMostSignificantBits());
                    buf.writeLong(witness.witnessId().getLeastSignificantBits());
                    ByteBufCodecs.VAR_INT.encode(buf, witness.type());
                    buf.writeLong(witness.memoryPos().asLong());
                    ByteBufCodecs.VAR_INT.encode(buf, witness.status());
                }
            }
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

    private static UUID readOptionalUuid(ByteBuf buf) {
        return ByteBufCodecs.BOOL.decode(buf) ? new UUID(buf.readLong(), buf.readLong()) : null;
    }

    private static void writeOptionalUuid(ByteBuf buf, @Nullable UUID value) {
        ByteBufCodecs.BOOL.encode(buf, value != null);
        if (value != null) {
            buf.writeLong(value.getMostSignificantBits());
            buf.writeLong(value.getLeastSignificantBits());
        }
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
