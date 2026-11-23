package com.alex.mycelialmind.network;

import com.alex.mycelialmind.colony.SymbiontCaste;
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
}
