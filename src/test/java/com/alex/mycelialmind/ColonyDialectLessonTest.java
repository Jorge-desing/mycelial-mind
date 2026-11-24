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
}
