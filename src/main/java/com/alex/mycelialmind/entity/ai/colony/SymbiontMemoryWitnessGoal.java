package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.colony.ColonyReconstruction;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.hive.ColonyReconstructionDirector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/** Carries one selected site memory to its chosen successor outpost. */
public final class SymbiontMemoryWitnessGoal extends Goal {
    private final ColonySymbiontEntity worker;
    private int navigationTicks;

    public SymbiontMemoryWitnessGoal(ColonySymbiontEntity worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(worker.level() instanceof ServerLevel level) || worker.isStandby()
                || worker.isResting() || worker.canRetreatToRest()) {
            return false;
        }
        SovereignColonyData colony = worker.getColonyData(level);
        return colony != null && colony.findReconstructionForWitness(worker.getUUID()) != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(worker.level() instanceof ServerLevel level) || worker.isStandby() || worker.isResting()) {
            return false;
        }
        SovereignColonyData colony = worker.getColonyData(level);
        return colony != null && colony.findReconstructionForWitness(worker.getUUID()) != null;
    }

    @Override
    public void start() {
        navigationTicks = 0;
    }

    @Override
    public void stop() {
        worker.getNavigation().stop();
        if (!worker.isStandby() && !worker.isResting()) {
            worker.setTaskState("idle");
        }
    }

    @Override
    public void tick() {
        if (!(worker.level() instanceof ServerLevel level)) {
            return;
        }
        SovereignColonyData colony = worker.getColonyData(level);
        ColonyReconstruction entry = colony == null ? null : colony.findReconstructionForWitness(worker.getUUID());
        if (entry == null || entry.successorSite() == null || entry.activeType() == null) {
            worker.setTaskState("idle");
            return;
        }
        BlockPos successor = entry.successorSite();
        if (!level.isLoaded(successor)) {
            worker.getNavigation().stop();
            worker.setTaskState("reconstruction_waiting_chunk");
            return;
        }
        if (!level.getBlockState(successor).getBlock().defaultBlockState().is(com.alex.mycelialmind.registry.ModBlocks.SYMBIOTIC_NODE.get())) {
            colony.recordReconstructionFailure(entry.ruinedSite(), worker.getUUID(),
                    com.alex.mycelialmind.colony.ColonyReconstructionStatus.SUCCESSOR_UNAVAILABLE);
            worker.setTaskState("idle");
            return;
        }
        worker.setTaskState("reconstruction_witness");
        if (worker.blockPosition().distSqr(successor) <= 6.5D) {
            worker.getNavigation().stop();
            ColonyReconstructionDirector.completeWitnessTrip(level, colony, entry, worker);
            return;
        }
        navigationTicks++;
        if (navigationTicks % 15 == 1 || worker.getNavigation().isDone()) {
            var path = worker.getNavigation().createPath(successor, 0);
            if (path != null && path.canReach()) {
                worker.getNavigation().moveTo(successor.getX() + 0.5D, successor.getY(), successor.getZ() + 0.5D, 1.1D);
                navigationTicks = 0;
            } else if (navigationTicks >= 600) {
                colony.recordReconstructionFailure(entry.ruinedSite(), worker.getUUID(),
                        com.alex.mycelialmind.colony.ColonyReconstructionStatus.NO_PATH);
                worker.getNavigation().stop();
                worker.setTaskState("idle");
                navigationTicks = 0;
            }
        }
    }
}
