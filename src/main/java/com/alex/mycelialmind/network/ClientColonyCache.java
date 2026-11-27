package com.alex.mycelialmind.network;

import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.colony.SymbiontRank;
import com.alex.mycelialmind.colony.SymbiontTemperament;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * Client-side synchronized snapshot of the player's Sovereign Hive Colony.
 */
public final class ClientColonyCache {
    public record SiteMemorySnapshot(
            BlockPos sitePos,
            int learnedHabit,
            int trialHabit,
            int incidentCount,
            int successfulTrials,
            int failedTrials,
            int lastIncidentCode,
            int lastOutcomeCode,
            int episodeMask,
            int escortCall,
            int restCall,
            int pickupCall,
            int lessonStage,
            UUID escortActor,
            UUID restActor,
            UUID pickupActor
    ) {
        public SiteMemorySnapshot(BlockPos pos, int learned, int trial, int incidents, int successes,
                                  int failures, int incidentCode, int outcomeCode) {
            this(pos, learned, trial, incidents, successes, failures, incidentCode, outcomeCode, 0, 0, 0, 0, -1, null, null, null);
        }
    }

    public record WorkerSnapshot(
            UUID workerId,
            SymbiontCaste caste,
            SymbiontRank rank,
            SymbiontTemperament temperament,
            int nutrition,
            int fatigue,
            int morale,
            boolean standby,
            int xp
    ) {
    }

    public record ReconstructionWitnessSnapshot(
            UUID witnessId,
            int type,
            BlockPos memoryPos,
            int status
    ) {
    }

    public record ReconstructionSnapshot(
            BlockPos ruinedSite,
            BlockPos successorSite,
            boolean hasSuccessor,
            long createdTick,
            int requiredMask,
            int restoredMask,
            UUID activeWitness,
            int activeType,
            UUID lastFailedWitness,
            int lastFailedType,
            int lastFailure,
            List<ReconstructionWitnessSnapshot> witnesses
    ) {
    }

    private static final SovereignColonySyncPayload EMPTY = new SovereignColonySyncPayload(
            false, false, BlockPos.ZERO, 1, 24, 4, 0, 0, 0, 70,
            0, 0, 0, 0, 0, 0, List.of(), List.of(), false,
            BlockPos.ZERO, BlockPos.ZERO, 0, false, List.of(), false);

    private static SovereignColonySyncPayload state = EMPTY;

    private ClientColonyCache() {
    }

    public static void update(SovereignColonySyncPayload payload) {
        state = new SovereignColonySyncPayload(
                payload.hasColony(),
                payload.unlocked(),
                payload.nexusPos(),
                payload.tier(),
                payload.radius(),
                payload.popCap(),
                payload.biomass(),
                payload.domainXp(),
                payload.traitMask(),
                payload.moralePct(),
                payload.linkedOutposts(),
                payload.inputChests(),
                payload.outputChests(),
                payload.workZones(),
                payload.activeOrders(),
                payload.activeRaidRemaining(),
                List.copyOf(payload.workers()),
                List.copyOf(payload.siteMemories()),
                payload.memoryTransferActive(),
                payload.memoryTransferSource(),
                payload.memoryTransferTarget(),
                payload.memoryTransferHabit(),
                payload.memoryTransferInTransit(),
                List.copyOf(payload.reconstructions()),
                payload.openScreen()
        );
    }

    public static void reset() {
        state = EMPTY;
    }

    public static boolean hasColony() {
        return state.hasColony();
    }

    public static boolean isUnlocked() {
        return state.unlocked();
    }

    @Nullable
    public static BlockPos getNexusPos() {
        return state.hasColony() ? state.nexusPos() : null;
    }

    public static int getTier() {
        return state.tier();
    }

    public static int getRadius() {
        return state.radius();
    }

    public static int getPopCap() {
        return state.popCap();
    }

    public static int getBiomass() {
        return state.biomass();
    }

    public static int getDomainXp() {
        return state.domainXp();
    }

    public static int getTraitMask() {
        return state.traitMask();
    }

    public static int getMoralePct() {
        return state.moralePct();
    }

    public static int getLinkedOutposts() {
        return state.linkedOutposts();
    }

    public static int getInputChests() {
        return state.inputChests();
    }

    public static int getOutputChests() {
        return state.outputChests();
    }

    public static int getWorkZones() {
        return state.workZones();
    }

    public static int getActiveOrders() {
        return state.activeOrders();
    }

    public static int getActiveRaidRemaining() {
        return state.activeRaidRemaining();
    }

    public static List<WorkerSnapshot> getWorkers() {
        return state.workers();
    }

    public static List<SiteMemorySnapshot> getSiteMemories() {
        return state.siteMemories();
    }

    @Nullable
    public static SiteMemorySnapshot getSiteMemory(BlockPos pos) {
        for (SiteMemorySnapshot memory : state.siteMemories()) {
            if (memory.sitePos().equals(pos)) {
                return memory;
            }
        }
        return null;
    }

    public static boolean hasMemoryTransfer() {
        return state.memoryTransferActive();
    }

    public static BlockPos getMemoryTransferSource() {
        return state.memoryTransferSource();
    }

    public static BlockPos getMemoryTransferTarget() {
        return state.memoryTransferTarget();
    }

    public static int getMemoryTransferHabit() {
        return state.memoryTransferHabit();
    }

    public static boolean isMemoryTransferInTransit() {
        return state.memoryTransferInTransit();
    }

    public static List<ReconstructionSnapshot> getReconstructions() {
        return state.reconstructions();
    }
}
