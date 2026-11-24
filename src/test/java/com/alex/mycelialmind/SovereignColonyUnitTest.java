package com.alex.mycelialmind;

import com.alex.mycelialmind.colony.ColonyOrder;
import com.alex.mycelialmind.colony.ColonyOrderBoard;
import com.alex.mycelialmind.colony.ColonySiteHabit;
import com.alex.mycelialmind.colony.ColonySiteMemory;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.SovereignTrait;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.colony.SymbiontRank;
import com.alex.mycelialmind.colony.SymbiontTemperament;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SovereignColonyUnitTest {

    @Test
    void castesTemperamentsRanksAndTraitsValidateCleanly() {
        assertEquals(5, SymbiontCaste.values().length);
        assertEquals(SymbiontCaste.EXCAVATOR, SymbiontCaste.HARVESTER.next());
        assertEquals(SymbiontCaste.HARVESTER, SymbiontCaste.SENTINEL.next());
        assertEquals(SymbiontCaste.COURIER, SymbiontCaste.byId(" courier "));
        assertNull(SymbiontCaste.byId("invalid_caste"));
        assertEquals("caste.mycelialmind.sentinel.name", SymbiontCaste.SENTINEL.nameKey());

        assertEquals(5, SymbiontTemperament.values().length);
        assertEquals(SymbiontTemperament.DILIGENT, SymbiontTemperament.fromIndex(99));
        assertEquals(SymbiontTemperament.STOIC, SymbiontTemperament.byId("STOIC"));
        assertEquals(SymbiontTemperament.CURIOUS, SymbiontTemperament.byId("curious"));

        assertEquals(SymbiontRank.NOVICE, SymbiontRank.forXp(0));
        assertEquals(SymbiontRank.NOVICE, SymbiontRank.forXp(99));
        assertEquals(SymbiontRank.ADEPT, SymbiontRank.forXp(100));
        assertEquals(SymbiontRank.SOVEREIGN, SymbiontRank.forXp(320));
        assertTrue(SymbiontRank.SOVEREIGN.visualScale() > SymbiontRank.NOVICE.visualScale());

        assertEquals(5, SovereignTrait.values().length);
        assertEquals((1 << 5) - 1, SovereignTrait.ALL_MASK);
        assertEquals(0, SovereignTrait.computeUnlockedMask(50));
        int unlockedAt500 = SovereignTrait.computeUnlockedMask(500);
        assertTrue(SovereignTrait.SYMBIOTIC_HARVEST.isUnlockedIn(unlockedAt500));
        assertTrue(SovereignTrait.DEEP_QUARRY_SENSE.isUnlockedIn(unlockedAt500));
        assertTrue(SovereignTrait.HYPHAL_LOGISTICS.isUnlockedIn(unlockedAt500));
        assertFalse(SovereignTrait.LIVING_BULWARK.isUnlockedIn(unlockedAt500));
        List<SovereignTrait> decoded = SovereignTrait.decodeMask(unlockedAt500);
        assertEquals(3, decoded.size());
        assertEquals(SovereignTrait.APEX_GUARDIAN_MATRIX, SovereignTrait.byId("apex_guardian_matrix"));
    }

    @Test
    void colonyEngineEnforcesTierCapsRadiusUpgradeAndMorale() {
        assertEquals(4, SovereignColonyEngine.getPopulationCap(1));
        assertEquals(8, SovereignColonyEngine.getPopulationCap(2));
        assertEquals(12, SovereignColonyEngine.getPopulationCap(3));
        assertEquals(12, SovereignColonyEngine.getPopulationCap(99));

        assertEquals(24, SovereignColonyEngine.getEffectiveRadius(1, 0));
        assertEquals(40, SovereignColonyEngine.getEffectiveRadius(1, 2));
        assertEquals(80, SovereignColonyEngine.getEffectiveRadius(3, 4));
        // Capped at MAX_LINKED_OUTPOSTS (4)
        assertEquals(80, SovereignColonyEngine.getEffectiveRadius(3, 10));

        assertFalse(SovereignColonyEngine.canUpgradeTier(1, 20, 200));
        assertFalse(SovereignColonyEngine.canUpgradeTier(1, 30, 100));
        assertTrue(SovereignColonyEngine.canUpgradeTier(1, 24, 150));
        assertTrue(SovereignColonyEngine.canUpgradeTier(2, 48, 450));
        assertFalse(SovereignColonyEngine.canUpgradeTier(3, 500, 5000));

        double highMorale = SovereignColonyEngine.computeColonyMorale(48, 100.0D, 0.0D, 4, 2, 0);
        double lowMorale = SovereignColonyEngine.computeColonyMorale(0, 10.0D, 95.0D, 0, 0, 4);
        assertTrue(highMorale > 90.0D && highMorale <= 100.0D);
        assertTrue(lowMorale >= 0.0D && lowMorale < 30.0D);

        assertTrue(SovereignColonyEngine.shouldTriggerFatigueRest(80.0D, 90.0D, false));
        assertFalse(SovereignColonyEngine.shouldTriggerFatigueRest(80.0D, 90.0D, true));
        assertTrue(SovereignColonyEngine.isRecoveredFromRest(10.0D, 80.0D));
        assertFalse(SovereignColonyEngine.isRecoveredFromRest(40.0D, 80.0D));

        int baseDelay = SovereignColonyEngine.computeWorkIntervalTicks(
                40, SymbiontRank.NOVICE, SymbiontTemperament.STOIC, 50.0D, false);
        int fastDelay = SovereignColonyEngine.computeWorkIntervalTicks(
                40, SymbiontRank.SOVEREIGN, SymbiontTemperament.DILIGENT, 85.0D, true);
        assertTrue(fastDelay < baseDelay);
        assertTrue(SovereignColonyEngine.computeRivalRaidSquadSize(3, 9, 2) >= 6);
    }

    @Test
    void orderBoardHandlesDedupExclusiveReservationTtlAndRelease() {
        ColonyOrderBoard board = new ColonyOrderBoard();
        BlockPos cropA = new BlockPos(10, 64, 10);
        BlockPos cropB = new BlockPos(14, 64, 10);
        UUID w1 = UUID.randomUUID();
        UUID w2 = UUID.randomUUID();

        ColonyOrder first = board.postOrRefreshOrder(ColonyOrder.OrderType.HARVEST, cropA, 20, 100L);
        ColonyOrder refreshed = board.postOrRefreshOrder(ColonyOrder.OrderType.HARVEST, cropA, 60, 105L);
        assertEquals(1, board.size());
        assertEquals(first.getOrderId(), refreshed.getOrderId());
        assertEquals(60, refreshed.getPriority());

        board.postOrRefreshOrder(ColonyOrder.OrderType.HARVEST, cropB, 30, 106L);
        board.postOrRefreshOrder(ColonyOrder.OrderType.EXCAVATE, new BlockPos(20, 60, 20), 90, 106L);

        // First claim gets highest priority HARVEST order (cropA, 60)
        ColonyOrder claimed1 = board.claimBestOrder(w1, SymbiontCaste.HARVESTER, cropA, 110L, 100L);
        assertNotNull(claimed1);
        assertEquals(cropA, claimed1.getTargetPos());

        // Second harvester gets cropB without colliding on cropA
        ColonyOrder claimed2 = board.claimBestOrder(w2, SymbiontCaste.HARVESTER, cropA, 115L, 100L);
        assertNotNull(claimed2);
        assertEquals(cropB, claimed2.getTargetPos());

        // Release w1 reservations frees cropA cleanly
        board.releaseWorkerReservations(w1);
        assertFalse(first.isReserved(120L));

        // Claim with 25t lease expires after TTL (130 + 25 = 155)
        ColonyOrder shortClaim = board.claimBestOrder(w1, SymbiontCaste.HARVESTER, cropA, 130L, 25L);
        assertNotNull(shortClaim);
        assertTrue(shortClaim.isReservedBy(w1, 150L));
        board.purgeExpired(160L);
        assertFalse(shortClaim.isReserved(160L));

        // Reclaim and complete removes order
        ColonyOrder reclaimed = board.claimBestOrder(w2, SymbiontCaste.HARVESTER, shortClaim.getTargetPos(), 165L, 100L);
        assertNotNull(reclaimed);
        assertTrue(board.completeOrder(reclaimed.getOrderId(), w2));
        assertEquals(2, board.size());
    }

    @Test
    void colonyDataRoundTripsViaNbt() {
        UUID colonyId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        BlockPos nexusPos = new BlockPos(120, 70, -45);
        SovereignColonyData data = new SovereignColonyData(colonyId, ownerId, "Alex", nexusPos);
        data.setTier(2);
        data.addBiomass(40);
        data.addDomainXp(300);
        data.replaceLinkedOutposts(List.of(new BlockPos(150, 70, -45), new BlockPos(100, 70, -20)));
        data.toggleInputChest(new BlockPos(122, 70, -45));
        data.toggleOutputChest(new BlockPos(118, 70, -45));
        data.toggleWorkZone(SovereignColonyData.WorkZoneType.HARVEST, new BlockPos(125, 69, -40));

        UUID workerId = UUID.randomUUID();
        data.upsertWorker(new SovereignColonyData.WorkerRosterEntry(
                workerId, SymbiontCaste.ARCHITECT, SymbiontRank.ADEPT, SymbiontTemperament.CURIOUS,
                88.0D, 12.0D, 91.5D, false, 145));

        CompoundTag saved = data.save();
        SovereignColonyData loaded = SovereignColonyData.load(saved);

        assertEquals(colonyId, loaded.getColonyId());
        assertEquals(ownerId, loaded.getOwnerId());
        assertEquals("Alex", loaded.getOwnerName());
        assertEquals(nexusPos, loaded.getNexusPos());
        assertEquals(2, loaded.getTier());
        assertEquals(56, loaded.getBiomassStored()); // initial 16 + 40
        assertEquals(300, loaded.getDomainXp());
        assertTrue(loaded.hasTrait(SovereignTrait.SYMBIOTIC_HARVEST));
        assertTrue(loaded.hasTrait(SovereignTrait.DEEP_QUARRY_SENSE));
        assertEquals(2, loaded.getLinkedOutposts().size());
        assertEquals(52, loaded.getEffectiveRadius()); // tier 2 base 36 + 2*8 = 52
        assertEquals(1, loaded.getInputChests().size());
        assertEquals(1, loaded.getOutputChests().size());
        assertEquals(1, loaded.getWorkZones(SovereignColonyData.WorkZoneType.HARVEST).size());
        assertEquals(1, loaded.getRoster().size());
        SovereignColonyData.WorkerRosterEntry restoredWorker = loaded.getRoster().get(workerId);
        assertNotNull(restoredWorker);
        assertEquals(SymbiontCaste.ARCHITECT, restoredWorker.caste());
        assertEquals(SymbiontRank.ADEPT, restoredWorker.rank());
        assertEquals(SymbiontTemperament.CURIOUS, restoredWorker.temperament());
        assertEquals(145, restoredWorker.xp());
    }

    @Test
    void localMemoryRequiresRepeatedIncidentsAndTwoLocalResults() {
        ColonySiteMemory memory = new ColonySiteMemory();
        assertFalse(memory.recordIncident(ColonySiteHabit.ESCORTED_COURIER, 1, 100L));
        assertTrue(memory.recordIncident(ColonySiteHabit.ESCORTED_COURIER, 1, 120L));
        assertEquals(ColonySiteHabit.ESCORTED_COURIER, memory.trialHabit());
        assertTrue(memory.usesHabit(ColonySiteHabit.ESCORTED_COURIER));

        assertFalse(memory.recordTrialSuccess(ColonySiteHabit.ESCORTED_COURIER, 1));
        assertTrue(memory.recordTrialSuccess(ColonySiteHabit.ESCORTED_COURIER, 1));
        assertEquals(ColonySiteHabit.ESCORTED_COURIER, memory.learnedHabit());

        assertFalse(memory.recordIncident(ColonySiteHabit.REST_BEFORE_RETURN, 2, 200L));
        assertTrue(memory.recordIncident(ColonySiteHabit.REST_BEFORE_RETURN, 2, 220L));
        assertFalse(memory.recordTrialSuccess(ColonySiteHabit.REST_BEFORE_RETURN, 2));
        assertFalse(memory.recordTrialSuccess(ColonySiteHabit.REST_BEFORE_RETURN, 2));
        assertEquals(2, memory.successfulTrials());
        assertTrue(memory.adoptTrialHabit());
        assertEquals(ColonySiteHabit.REST_BEFORE_RETURN, memory.learnedHabit());
    }

    @Test
    void localMemoryPersistsAndTravelsBetweenLinkedSites() {
        BlockPos nexus = new BlockPos(10, 64, 10);
        BlockPos outpost = new BlockPos(30, 64, 10);
        SovereignColonyData data = new SovereignColonyData(UUID.randomUUID(), UUID.randomUUID(), "Owner", nexus);
        data.replaceLinkedOutposts(List.of(outpost));
        ColonySiteMemory source = data.getSiteMemory(nexus);
        assertNotNull(source);
        assertFalse(source.recordIncident(ColonySiteHabit.STAGGERED_PICKUP, 3, 100L));
        assertTrue(source.recordIncident(ColonySiteHabit.STAGGERED_PICKUP, 3, 120L));
        assertFalse(source.recordTrialSuccess(ColonySiteHabit.STAGGERED_PICKUP, 3));
        assertTrue(source.recordTrialSuccess(ColonySiteHabit.STAGGERED_PICKUP, 3));
        assertTrue(data.startMemoryTransfer(nexus, outpost, 130L));

        SovereignColonyData restored = SovereignColonyData.load(data.save());
        SovereignColonyData.MemoryTransferSnapshot transfer = restored.getMemoryTransferSnapshot();
        assertNotNull(transfer);
        assertEquals(nexus, transfer.sourcePos());
        assertEquals(outpost, transfer.targetPos());
        assertEquals(ColonySiteHabit.STAGGERED_PICKUP, transfer.habit());

        UUID courier = UUID.randomUUID();
        assertTrue(restored.claimMemoryTransfer(courier, 140L));
        restored.markMemoryTransferCollected(courier, 150L);
        assertTrue(restored.deliverMemoryTransfer(courier, 160L));
        assertNull(restored.getMemoryTransferSnapshot());

        ColonySiteMemory destination = restored.getSiteMemory(outpost);
        assertNotNull(destination);
        assertEquals(ColonySiteHabit.STAGGERED_PICKUP, destination.trialHabit());
        assertFalse(destination.recordTrialSuccess(ColonySiteHabit.STAGGERED_PICKUP, 3));
        assertTrue(destination.recordTrialSuccess(ColonySiteHabit.STAGGERED_PICKUP, 3));
        assertEquals(ColonySiteHabit.STAGGERED_PICKUP, destination.learnedHabit());
        assertEquals(ColonySiteHabit.NONE, restored.getSiteMemory(nexus).trialHabit());
    }

    @Test
    void pickupTurnLeaseIsExclusiveAndRecoverable() {
        ColonySiteMemory memory = new ColonySiteMemory();
        UUID firstCourier = UUID.randomUUID();
        UUID secondCourier = UUID.randomUUID();
        assertTrue(memory.tryAcquirePickupTurn(firstCourier, 100L, 200L));
        assertFalse(memory.tryAcquirePickupTurn(secondCourier, 110L, 200L));
        memory.releasePickupTurn(firstCourier);
        assertTrue(memory.tryAcquirePickupTurn(secondCourier, 111L, 200L));
        assertTrue(memory.tryAcquirePickupTurn(firstCourier, 312L, 200L));
    }
}
