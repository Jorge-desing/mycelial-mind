package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.level.pathfinder.Path;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Drives mob pathfinding toward {@code WALK_TARGET}, clearing it on arrival
 * and recording {@code CANT_REACH_WALK_TARGET_SINCE} if pathfinding fails.
 */
public class MoveToWalkTarget<E extends PathfinderMob> extends HiveBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = List.of(
            Pair.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED),
            Pair.of(MemoryModuleType.PATH, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT)
    );

    @Nullable
    protected Path path;
    @Nullable
    protected BlockPos lastTargetPos;
    protected float speedModifier;
    private long nextRetryAtTick = 0L;

    public MoveToWalkTarget() {
        runForBetween(150, 250);
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E mob) {
        WalkTarget target = HiveBrainUtils.getMemory(mob, MemoryModuleType.WALK_TARGET);
        if (target == null) {
            return false;
        }
        if (hasReachedTarget(mob, target)) {
            mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
            return false;
        }
        if (level.getGameTime() < this.nextRetryAtTick) {
            return false;
        }
        return attemptNewPath(mob, target, true);
    }

    @Override
    protected boolean shouldKeepRunning(E mob) {
        WalkTarget target = HiveBrainUtils.getMemory(mob, MemoryModuleType.WALK_TARGET);
        if (target == null || this.path == null || hasReachedTarget(mob, target)) {
            return false;
        }
        return !mob.getNavigation().isDone();
    }

    @Override
    protected void start(E mob) {
        startOnNewPath(mob);
    }

    @Override
    protected void tick(E mob) {
        Brain<?> brain = mob.getBrain();
        WalkTarget walkTarget = HiveBrainUtils.getMemory(brain, MemoryModuleType.WALK_TARGET);
        if (walkTarget == null) {
            return;
        }
        if (hasReachedTarget(mob, walkTarget)) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            return;
        }
        BlockPos currentPos = walkTarget.getTarget().currentBlockPosition();
        if (this.lastTargetPos == null || currentPos.distSqr(this.lastTargetPos) > 4.0D) {
            if (attemptNewPath(mob, walkTarget, false)) {
                startOnNewPath(mob);
            }
        } else if (!mob.getNavigation().isInProgress() && !attemptNewPath(mob, walkTarget, false)) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    @Override
    protected void stop(E mob) {
        Brain<?> brain = mob.getBrain();
        WalkTarget walkTarget = HiveBrainUtils.getMemory(brain, MemoryModuleType.WALK_TARGET);
        if (walkTarget != null && hasReachedTarget(mob, walkTarget)) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        }
        if (mob.getNavigation().isStuck()) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        }
        brain.eraseMemory(MemoryModuleType.PATH);
        mob.getNavigation().stop();
        this.path = null;
        this.lastTargetPos = null;
    }

    protected boolean attemptNewPath(E mob, WalkTarget walkTarget, boolean recordRetryDelay) {
        Brain<?> brain = mob.getBrain();
        BlockPos targetPos = walkTarget.getTarget().currentBlockPosition();
        this.path = mob.getNavigation().createPath(targetPos, 0);
        this.speedModifier = walkTarget.getSpeedModifier();
        if (this.path != null && this.path.canReach()) {
            this.lastTargetPos = targetPos;
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            return true;
        }
        if (recordRetryDelay) {
            this.nextRetryAtTick = mob.level().getGameTime() + 40L;
        }
        if (!brain.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
            brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, mob.level().getGameTime());
        }
        if (this.path != null) {
            this.lastTargetPos = targetPos;
            return true;
        }
        return false;
    }

    protected boolean hasReachedTarget(E mob, WalkTarget walkTarget) {
        return walkTarget.getTarget().currentBlockPosition().distManhattan(mob.blockPosition())
                <= walkTarget.getCloseEnoughDist();
    }

    protected void startOnNewPath(E mob) {
        if (this.path != null) {
            mob.getBrain().setMemory(MemoryModuleType.PATH, this.path);
            mob.getNavigation().moveTo(this.path, this.speedModifier);
        }
    }
}
