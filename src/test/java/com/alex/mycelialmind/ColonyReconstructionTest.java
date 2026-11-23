package com.alex.mycelialmind;

import com.alex.mycelialmind.colony.ColonyReconstruction;
import com.alex.mycelialmind.colony.ColonyReconstructionRules;
import com.alex.mycelialmind.colony.ColonyWitnessMemory;
import com.alex.mycelialmind.colony.ColonyReconstructionStatus;
import com.alex.mycelialmind.colony.ColonySiteHabit;
import com.alex.mycelialmind.colony.SovereignColonyData;
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

class ColonyReconstructionTest {
    private static final BlockPos NEXUS = new BlockPos(0, 64, 0);
    private static final BlockPos RUIN = new BlockPos(16, 64, 0);
    private static final BlockPos SUCCESSOR = new BlockPos(32, 64, 0);

    @Test
    void threeSurvivorsCarryDistinctMemoriesAndEachTypeRestoresOnce() {
        SovereignColonyData colony = newColony();
        UUID courier = UUID.randomUUID();
        UUID sentinel = UUID.randomUUID();
        UUID veteran = UUID.randomUUID();
        BlockPos existingChest = new BlockPos(20, 64, 2);
        BlockPos patrolOffset = new BlockPos(5, 0, -3);

        assertTrue(colony.recordWitnessMemory(new ColonyWitnessMemory(courier, RUIN,
                ColonyWitnessMemory.Type.COURIER_INPUT, existingChest)));
        assertTrue(colony.recordWitnessMemory(new ColonyWitnessMemory(sentinel, RUIN,
                ColonyWitnessMemory.Type.SENTINEL_PATROL, patrolOffset)));
        assertTrue(colony.recordWitnessMemory(new ColonyWitnessMemory(veteran, RUIN,
                ColonyWitnessMemory.Type.HABIT_STAGGERED_PICKUP, BlockPos.ZERO)));
        assertTrue(colony.beginOutpostReconstruction(RUIN, 1200L));
        assertTrue(colony.setReconstructionSuccessor(RUIN, SUCCESSOR));

        ColonyReconstruction entry = colony.getReconstruction(RUIN);
        assertNotNull(entry);
        assertEquals(3, Integer.bitCount(entry.requiredMask()));

        assertTrue(colony.beginReconstructionTrip(RUIN, courier, ColonyWitnessMemory.Type.COURIER_INPUT));
        colony.completeReconstructionMemory(RUIN, courier, ColonyWitnessMemory.Type.COURIER_INPUT);
        assertTrue(entry.isRestored(ColonyWitnessMemory.Type.COURIER_INPUT));
        assertFalse(colony.beginReconstructionTrip(RUIN, courier, ColonyWitnessMemory.Type.COURIER_INPUT),
                "A memory type is consumed once even if another survivor recorded it.");

        assertTrue(colony.beginReconstructionTrip(RUIN, sentinel, ColonyWitnessMemory.Type.SENTINEL_PATROL));
        colony.completeReconstructionMemory(RUIN, sentinel, ColonyWitnessMemory.Type.SENTINEL_PATROL);
        assertTrue(colony.beginReconstructionTrip(RUIN, veteran,
                ColonyWitnessMemory.Type.HABIT_STAGGERED_PICKUP));
        colony.completeReconstructionMemory(RUIN, veteran,
                ColonyWitnessMemory.Type.HABIT_STAGGERED_PICKUP);
        assertTrue(entry.isClosed());
        assertEquals(0, entry.requiredMask() & ~entry.restoredMask());
    }

    @Test
    void partialRecoverySurvivesWitnessLossAndSaveLoad() {
        SovereignColonyData colony = newColony();
        UUID courier = UUID.randomUUID();
        UUID sentinel = UUID.randomUUID();
        UUID lostWitness = UUID.randomUUID();
        colony.recordWitnessMemory(new ColonyWitnessMemory(courier, RUIN,
                ColonyWitnessMemory.Type.COURIER_OUTPUT, new BlockPos(18, 64, 1)));
        colony.recordWitnessMemory(new ColonyWitnessMemory(sentinel, RUIN,
                ColonyWitnessMemory.Type.SENTINEL_PATROL, new BlockPos(-4, 1, 6)));
        colony.recordWitnessMemory(new ColonyWitnessMemory(lostWitness, RUIN,
                ColonyWitnessMemory.Type.HABIT_REST_BEFORE_RETURN, BlockPos.ZERO));
        colony.beginOutpostReconstruction(RUIN, 2400L);
        colony.setReconstructionSuccessor(RUIN, SUCCESSOR);

        assertTrue(colony.beginReconstructionTrip(RUIN, courier, ColonyWitnessMemory.Type.COURIER_OUTPUT));
        colony.completeReconstructionMemory(RUIN, courier, ColonyWitnessMemory.Type.COURIER_OUTPUT);
        colony.removeWitnessMemories(lostWitness);
        assertEquals(2, colony.getWitnessMemories(RUIN).size());

        assertTrue(colony.beginReconstructionTrip(RUIN, sentinel, ColonyWitnessMemory.Type.SENTINEL_PATROL));
        CompoundTag saved = colony.save();
        SovereignColonyData restored = SovereignColonyData.load(saved);
        ColonyReconstruction restoredEntry = restored.getReconstruction(RUIN);
        assertNotNull(restoredEntry);
        assertTrue(restoredEntry.isRestored(ColonyWitnessMemory.Type.COURIER_OUTPUT));
        assertTrue(restoredEntry.isAssignedTo(sentinel), "The in-flight witness must resume after loading.");
        assertEquals(3, Integer.bitCount(restoredEntry.requiredMask()),
                "The missing witness does not erase the unrecovered memory from the ruin record.");
        assertEquals(2, restored.getWitnessMemories(RUIN).size());
        assertFalse(restoredEntry.isClosed(), "A partial recovery stays open for another surviving memory.");
    }

    @Test
    void unlinkingIsNotRuinButLaterConfirmedRemovalCanStillReconstructIt() {
        SovereignColonyData colony = newColony();
        colony.replaceLinkedOutposts(List.of(SUCCESSOR));
        assertTrue(colony.isKnownOutpost(RUIN));
        assertFalse(colony.getLinkedOutposts().contains(RUIN));
        assertNull(colony.getReconstruction(RUIN), "A link loss alone must not open a reconstruction.");

        assertTrue(colony.beginOutpostReconstruction(RUIN, 3600L),
                "The explicit block-removal callback can still open the known site's recovery later.");
        assertNotNull(colony.getReconstruction(RUIN));
    }

    @Test
    void legacySavedDataLoadsWithoutWitnessOrReconstructionTags() {
        SovereignColonyData colony = newColony();
        CompoundTag legacy = colony.save();
        legacy.remove("KnownOutposts");
        legacy.remove("SiteWitnessMemories");
        legacy.remove("SiteReconstructions");
        SovereignColonyData restored = SovereignColonyData.load(legacy);
        assertTrue(restored.getReconstructionEntries().isEmpty());
        assertTrue(restored.getWitnessMemories(RUIN).isEmpty());
        assertTrue(restored.isKnownOutpost(RUIN), "Older worlds seed the history from their active links.");
    }

    @Test
    void coordinateRulesRejectWrongSuccessorsAndUnsafePatrolOffsets() {
        assertTrue(ColonyReconstructionRules.isValidSuccessor(RUIN, SUCCESSOR, List.of(SUCCESSOR)));
        assertFalse(ColonyReconstructionRules.isValidSuccessor(RUIN, RUIN, List.of(RUIN, SUCCESSOR)));
        assertFalse(ColonyReconstructionRules.isValidSuccessor(RUIN, NEXUS, List.of(SUCCESSOR)));

        BlockPos offset = new BlockPos(-9, 2, 11);
        assertTrue(ColonyReconstructionRules.isSafePatrolOffset(offset));
        assertEquals(SUCCESSOR.offset(-9, 2, 11),
                ColonyReconstructionRules.resolvePatrolPoint(SUCCESSOR, offset));
        assertFalse(ColonyReconstructionRules.isSafePatrolOffset(new BlockPos(49, 0, 0)));
        assertNull(ColonyReconstructionRules.resolvePatrolPoint(SUCCESSOR, new BlockPos(0, 0, 60)));
    }

    @Test
    void inaccessibleCandidatesExplainTheirBlockingReason() {
        assertEquals(ColonyReconstructionStatus.TARGET_UNLOADED,
                ColonyReconstructionRules.routeCandidateStatus(SUCCESSOR, new BlockPos(35, 64, 0), 80,
                        false, false, true));
        assertEquals(ColonyReconstructionStatus.ROUTE_INVALID,
                ColonyReconstructionRules.routeCandidateStatus(SUCCESSOR, new BlockPos(35, 64, 0), 80,
                        true, false, true));
        assertEquals(ColonyReconstructionStatus.CAPACITY_REACHED,
                ColonyReconstructionRules.routeCandidateStatus(SUCCESSOR, new BlockPos(35, 64, 0), 80,
                        true, true, false));
        assertEquals(ColonyReconstructionStatus.TARGET_UNLOADED,
                ColonyReconstructionRules.patrolCandidateStatus(true, false, false, true));
        assertEquals(ColonyReconstructionStatus.PATROL_INACCESSIBLE,
                ColonyReconstructionRules.patrolCandidateStatus(true, true, false, true));
        assertEquals(ColonyReconstructionStatus.HABIT_OCCUPIED,
                ColonyReconstructionRules.habitSlotStatus(ColonySiteHabit.ESCORTED_COURIER,
                        ColonySiteHabit.NONE, ColonySiteHabit.REST_BEFORE_RETURN));
        assertEquals(ColonyReconstructionStatus.HABIT_ALREADY_PRESENT,
                ColonyReconstructionRules.habitSlotStatus(ColonySiteHabit.NONE,
                        ColonySiteHabit.STAGGERED_PICKUP, ColonySiteHabit.STAGGERED_PICKUP));
    }

    @Test
    void fifthRuinArchivesAnAlreadyClosedRecovery() {
        SovereignColonyData colony = newColony();
        for (int i = 0; i < 5; i++) {
            BlockPos ruin = new BlockPos(16 + i * 4, 64, 0);
            colony.replaceLinkedOutposts(List.of(ruin));
            assertTrue(colony.beginOutpostReconstruction(ruin, i * 100L));
        }
        assertEquals(4, colony.getReconstructionEntries().size());
        assertEquals(1, colony.getArchivedReconstructionCount());
    }

    private static SovereignColonyData newColony() {
        SovereignColonyData colony = new SovereignColonyData(UUID.randomUUID(), UUID.randomUUID(), "test", NEXUS);
        colony.replaceLinkedOutposts(List.of(RUIN, SUCCESSOR));
        return colony;
    }
}
