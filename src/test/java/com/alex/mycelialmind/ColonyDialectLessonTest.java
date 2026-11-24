package com.alex.mycelialmind;

import com.alex.mycelialmind.colony.ColonyDialect;
import com.alex.mycelialmind.colony.ColonyDialectLesson;
import com.alex.mycelialmind.colony.ColonySiteHabit;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ColonyDialectLessonTest {
    @Test
    void demonstrationRequiresNewEpisodeAndSurvivesReload() {
        UUID teacher = UUID.randomUUID();
        UUID apprentice = UUID.randomUUID();
        UUID baseline = UUID.randomUUID();
        UUID demonstration = UUID.randomUUID();
        ColonyDialectLesson lesson = new ColonyDialectLesson(new BlockPos(1, 64, 1), new BlockPos(9, 64, 1),
                ColonySiteHabit.ESCORTED_COURIER, 5, apprentice, teacher, baseline, true);
        assertFalse(lesson.witness(demonstration));
        lesson.arriveAtSource();
        assertFalse(lesson.witness(baseline));
        assertTrue(lesson.witness(demonstration));
        assertFalse(lesson.witness(demonstration));
        ColonyDialectLesson loaded = ColonyDialectLesson.load(lesson.save());
        assertNotNull(loaded);
        assertEquals(teacher, loaded.teacher());
        assertEquals(apprentice, loaded.apprentice());
        assertEquals(ColonyDialectLesson.Stage.RETURNING, loaded.stage());
    }

    @Test
    void callsCannotCollideAndPersistPerMeaning() {
        ColonyDialect dialect = new ColonyDialect();
        assertTrue(dialect.teach(ColonySiteHabit.ESCORTED_COURIER, 4));
        assertFalse(dialect.teach(ColonySiteHabit.REST_BEFORE_RETURN, 4));
        assertTrue(dialect.teach(ColonySiteHabit.REST_BEFORE_RETURN, 5));
        ColonyDialect loaded = ColonyDialect.load(dialect.save());
        assertEquals(4, loaded.call(ColonySiteHabit.ESCORTED_COURIER));
        assertEquals(5, loaded.call(ColonySiteHabit.REST_BEFORE_RETURN));
        assertEquals(-1, loaded.call(ColonySiteHabit.STAGGERED_PICKUP));
    }

    @Test
    void callCooldownSurvivesReloadAndExpires() {
        ColonyDialect dialect = new ColonyDialect();
        assertTrue(dialect.tryEmit(100));
        assertFalse(dialect.tryEmit(299));
        ColonyDialect loaded = ColonyDialect.load(dialect.save());
        assertFalse(loaded.tryEmit(299));
        assertTrue(loaded.tryEmit(300));
    }

    @Test
    void pickupCallRespectsCurrentLeaseAndTheOldestWaitingCourier() {
        var memory = new com.alex.mycelialmind.colony.ColonySiteMemory();
        UUID current = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        assertTrue(memory.tryAcquirePickupTurn(current, 10, 20));
        assertFalse(memory.tryAcquirePickupTurn(first, 11, 20));
        assertFalse(memory.tryAcquirePickupTurn(second, 12, 20));
        assertFalse(memory.givePickupTurnToOldest(15, 20));
        memory.releasePickupTurn(current);
        assertTrue(memory.givePickupTurnToOldest(16, 20));
        assertFalse(memory.tryAcquirePickupTurn(second, 17, 20));
        assertTrue(memory.tryAcquirePickupTurn(first, 17, 20));
    }

    @Test
    void alternativeCallPreservesEarlierSpeakersAndLegacySave() {
        ColonyDialect source = new ColonyDialect();
        ColonyDialect oldListener = new ColonyDialect();
        ColonyDialect newListener = new ColonyDialect();
        assertTrue(source.teach(ColonySiteHabit.ESCORTED_COURIER, 1));
        assertTrue(oldListener.teach(ColonySiteHabit.ESCORTED_COURIER, 1));
        assertTrue(source.teach(ColonySiteHabit.ESCORTED_COURIER, 7));
        assertTrue(newListener.teach(ColonySiteHabit.ESCORTED_COURIER, 7));
        ColonyDialect restored = ColonyDialect.load(source.save());
        assertEquals(1, restored.sharedCall(ColonySiteHabit.ESCORTED_COURIER, oldListener));
        assertEquals(7, restored.sharedCall(ColonySiteHabit.ESCORTED_COURIER, newListener));
        assertFalse(restored.teach(ColonySiteHabit.REST_BEFORE_RETURN, 7));
        var old = new net.minecraft.nbt.CompoundTag();
        var list = new net.minecraft.nbt.ListTag();
        var entry = new net.minecraft.nbt.CompoundTag();
        entry.putInt("Habit", ColonySiteHabit.ESCORTED_COURIER.ordinal());
        entry.putInt("Call", 3);
        list.add(entry);
        old.put("Entries", list);
        assertEquals(3, ColonyDialect.load(old).call(ColonySiteHabit.ESCORTED_COURIER));
    }

    @Test
    void clearCallUnblocksConflictingLessonAndDropsEmptyMask() {
        ColonyDialect dialect = new ColonyDialect();
        assertTrue(dialect.teach(ColonySiteHabit.ESCORTED_COURIER, 2));
        assertFalse(dialect.teach(ColonySiteHabit.REST_BEFORE_RETURN, 2));
        assertTrue(dialect.clearCall(2));
        assertFalse(dialect.clearCall(2));
        assertEquals(-1, dialect.call(ColonySiteHabit.ESCORTED_COURIER));
        assertTrue(dialect.teach(ColonySiteHabit.REST_BEFORE_RETURN, 2));
        assertEquals(2, dialect.call(ColonySiteHabit.REST_BEFORE_RETURN));
    }

    @Test
    void selectableAlternativeTeacherAllowedAndConflictClearableOnColony() {
        var nexus = new BlockPos(0, 64, 0);
        var source = new BlockPos(10, 64, 0);
        var target = new BlockPos(20, 64, 0);
        var colony = new com.alex.mycelialmind.colony.SovereignColonyData(
                UUID.randomUUID(), UUID.randomUUID(), "Tester", nexus);
        colony.replaceLinkedOutposts(java.util.List.of(source, target));

        UUID protagonistId = UUID.randomUUID();
        UUID altTeacherId = UUID.randomUUID();
        UUID apprenticeId = UUID.randomUUID();
        for (UUID id : java.util.List.of(protagonistId, altTeacherId, apprenticeId)) {
            colony.upsertWorker(new com.alex.mycelialmind.colony.SovereignColonyData.WorkerRosterEntry(
                    id,
                    com.alex.mycelialmind.colony.SymbiontCaste.COURIER,
                    com.alex.mycelialmind.colony.SymbiontRank.NOVICE,
                    com.alex.mycelialmind.colony.SymbiontTemperament.DILIGENT,
                    100.0D, 0.0D, 80.0D, false, 0));
        }

        colony.getDialect(source).recordEpisode(ColonySiteHabit.ESCORTED_COURIER, protagonistId, 100L);
        assertTrue(colony.getDialect(target).teach(ColonySiteHabit.REST_BEFORE_RETURN, 4));
        assertFalse(colony.startDialectLesson(source, target, ColonySiteHabit.ESCORTED_COURIER, 4, apprenticeId, altTeacherId, true));
        assertTrue(colony.clearDialectCall(target, 4));
        assertEquals(-1, colony.getDialect(target).call(ColonySiteHabit.REST_BEFORE_RETURN));
        assertTrue(colony.startDialectLesson(source, target, ColonySiteHabit.ESCORTED_COURIER, 4, apprenticeId, altTeacherId, true));
        assertEquals(altTeacherId, colony.getDialectLesson().teacher());
        assertFalse(colony.clearDialectCall(target, 4));
    }
}
