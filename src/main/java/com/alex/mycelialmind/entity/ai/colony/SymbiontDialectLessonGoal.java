package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.colony.ColonyDialectLesson;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Moves the selected apprentice to the demonstration and back to its own site. */
public final class SymbiontDialectLessonGoal extends Goal {
    private final ColonySymbiontEntity worker;

    public SymbiontDialectLessonGoal(ColonySymbiontEntity worker) {
        this.worker = worker;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(worker.level() instanceof ServerLevel level) || worker.isStandby() || worker.isResting()) return false;
        SovereignColonyData colony = worker.getColonyData(level);
        return colony != null && colony.getDialectLesson() != null
                && colony.getDialectLesson().apprentice().equals(worker.getUUID());
    }

    @Override
    public boolean canContinueToUse() { return canUse(); }

    @Override
    public void stop() { worker.getNavigation().stop(); }

    @Override
    public void tick() {
        if (!(worker.level() instanceof ServerLevel level)) return;
        SovereignColonyData colony = worker.getColonyData(level);
        ColonyDialectLesson lesson = colony == null ? null : colony.getDialectLesson();
        if (lesson == null) return;
        BlockPos destination = lesson.stage() == ColonyDialectLesson.Stage.RETURNING ? lesson.target() : lesson.source();
        if (!level.isLoaded(destination)) {
            worker.getNavigation().stop();
            return;
        }
        if (!colony.getMemorySitePositions().contains(lesson.source())
                || !colony.getMemorySitePositions().contains(lesson.target())) {
            colony.cancelDialectLesson();
            SovereignColonySavedData.get(level).setDirty();
            return;
        }
        if (worker.blockPosition().distSqr(destination) > 9.0D) {
            worker.getNavigation().moveTo(destination.getX() + 0.5D, destination.getY(), destination.getZ() + 0.5D, 1.0D);
            return;
        }
        worker.getNavigation().stop();
        if (lesson.stage() == ColonyDialectLesson.Stage.TRAVELLING) {
            lesson.arriveAtSource();
            SovereignColonySavedData.get(level).setDirty();
        } else if (lesson.stage() == ColonyDialectLesson.Stage.OBSERVING) {
            // The work event validates both participants at the instant it happens.
            worker.setTaskState("dialect_observing");
        } else {
            com.alex.mycelialmind.colony.ColonyDialectLesson finished = colony.getDialectLesson();
            if (colony.finishDialectLesson(worker.getUUID())) {
                SovereignColonySavedData.get(level).setDirty();
                if (finished != null) {
                    com.alex.mycelialmind.colony.ColonyDialectCallDirector.playSequence(level, finished.target(), finished.call());
                    com.alex.mycelialmind.colony.ColonyDialectCallDirector.notifyNearbyPlayers(
                            level,
                            finished.target(),
                            net.minecraft.network.chat.Component.translatable(
                                    "message.mycelialmind.colony.dialect_learned",
                                    net.minecraft.network.chat.Component.translatable(finished.meaning().translationKey())));
                }
            }
        }
    }
}
