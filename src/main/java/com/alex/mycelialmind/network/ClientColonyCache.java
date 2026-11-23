package com.alex.mycelialmind.network;

import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.colony.ColonyWitnessMemory;
import com.alex.mycelialmind.colony.SymbiontRank;
import com.alex.mycelialmind.colony.SymbiontTemperament;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
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
            int lastOutcomeCode
    ) {
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

    private static boolean hasColony = false;
    private static boolean unlocked = false;
    @Nullable
    private static BlockPos nexusPos = null;
    private static int tier = 1;
    private static int radius = 24;
    private static int popCap = 4;
    private static int biomass = 0;
    private static int domainXp = 0;
    private static int traitMask = 0;
    private static int moralePct = 70;
    private static int linkedOutposts = 0;
    private static int inputChests = 0;
    private static int outputChests = 0;
    private static int workZones = 0;
    private static int activeOrders = 0;
    private static int activeRaidRemaining = 0;
    private static List<WorkerSnapshot> workers = List.of();
    private static List<SiteMemorySnapshot> siteMemories = List.of();
    private static boolean memoryTransferActive;
    private static BlockPos memoryTransferSource = BlockPos.ZERO;
    private static BlockPos memoryTransferTarget = BlockPos.ZERO;
    private static int memoryTransferHabit;
    private static boolean memoryTransferInTransit;
    private static List<ReconstructionSnapshot> reconstructions = List.of();

    private ClientColonyCache() {
    }

    public static void update(SovereignColonySyncPayload payload) {
        hasColony = payload.hasColony();
        unlocked = payload.unlocked();
        nexusPos = payload.hasColony() ? payload.nexusPos() : null;
        tier = payload.tier();
        radius = payload.radius();
        popCap = payload.popCap();
        biomass = payload.biomass();
        domainXp = payload.domainXp();
        traitMask = payload.traitMask();
        moralePct = payload.moralePct();
        linkedOutposts = payload.linkedOutposts();
        inputChests = payload.inputChests();
        outputChests = payload.outputChests();
        workZones = payload.workZones();
        activeOrders = payload.activeOrders();
        activeRaidRemaining = payload.activeRaidRemaining();
        workers = List.copyOf(payload.workers());
        siteMemories = List.copyOf(payload.siteMemories());
        memoryTransferActive = payload.memoryTransferActive();
        memoryTransferSource = payload.memoryTransferSource();
        memoryTransferTarget = payload.memoryTransferTarget();
        memoryTransferHabit = payload.memoryTransferHabit();
        memoryTransferInTransit = payload.memoryTransferInTransit();
        reconstructions = List.copyOf(payload.reconstructions());
    }

    public static void reset() {
        hasColony = false;
        unlocked = false;
        nexusPos = null;
        tier = 1;
        radius = 24;
        popCap = 4;
        biomass = 0;
        domainXp = 0;
        traitMask = 0;
        moralePct = 70;
        linkedOutposts = 0;
        inputChests = 0;
        outputChests = 0;
        workZones = 0;
        activeOrders = 0;
        activeRaidRemaining = 0;
        workers = List.of();
        siteMemories = List.of();
        memoryTransferActive = false;
        memoryTransferSource = BlockPos.ZERO;
        memoryTransferTarget = BlockPos.ZERO;
        memoryTransferHabit = 0;
        memoryTransferInTransit = false;
        reconstructions = List.of();
    }

    public static boolean hasColony() {
        return hasColony;
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    @Nullable
    public static BlockPos getNexusPos() {
        return nexusPos;
    }

    public static int getTier() {
        return tier;
    }

    public static int getRadius() {
        return radius;
    }

    public static int getPopCap() {
        return popCap;
    }

    public static int getBiomass() {
        return biomass;
    }

    public static int getDomainXp() {
        return domainXp;
    }

    public static int getTraitMask() {
        return traitMask;
    }

    public static int getMoralePct() {
        return moralePct;
    }

    public static int getLinkedOutposts() {
        return linkedOutposts;
    }

    public static int getInputChests() {
        return inputChests;
    }

    public static int getOutputChests() {
        return outputChests;
    }

    public static int getWorkZones() {
        return workZones;
    }

    public static int getActiveOrders() {
        return activeOrders;
    }

    public static int getActiveRaidRemaining() {
        return activeRaidRemaining;
    }

    public static List<WorkerSnapshot> getWorkers() {
        return Collections.unmodifiableList(workers);
    }

    public static List<SiteMemorySnapshot> getSiteMemories() {
        return Collections.unmodifiableList(siteMemories);
    }

    @Nullable
    public static SiteMemorySnapshot getSiteMemory(BlockPos pos) {
        for (SiteMemorySnapshot memory : siteMemories) {
            if (memory.sitePos().equals(pos)) {
                return memory;
            }
        }
        return null;
    }

    public static boolean hasMemoryTransfer() {
        return memoryTransferActive;
    }

    public static BlockPos getMemoryTransferSource() {
        return memoryTransferSource;
    }

    public static BlockPos getMemoryTransferTarget() {
        return memoryTransferTarget;
    }

    public static int getMemoryTransferHabit() {
        return memoryTransferHabit;
    }

    public static boolean isMemoryTransferInTransit() {
        return memoryTransferInTransit;
    }

    public static List<ReconstructionSnapshot> getReconstructions() {
        return Collections.unmodifiableList(reconstructions);
    }
}
