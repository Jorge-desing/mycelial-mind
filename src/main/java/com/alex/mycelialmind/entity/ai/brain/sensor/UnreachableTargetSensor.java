package com.alex.mycelialmind.entity.ai.brain.sensor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import java.util.List;

/**
 * Monitors failed pathfinding attempts against the active {@code WALK_TARGET} and updates
 * {@code CANT_REACH_WALK_TARGET_SINCE} so behaviours can react or invalidate unreachable targets.
 */
public class UnreachableTargetSensor<E extends Mob> extends HiveSensor<E> {
    private static final List<MemoryModuleType<?>> MEMORIES = List.of(
            MemoryModuleType.WALK_TARGET,
            MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE
    );

    public UnreachableTargetSensor() {
        setScanRate(entity -> 10);
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return MEMORIES;
    }

    @Override
    protected void scan(ServerLevel level, E entity) {
        Brain<?> brain = entity.getBrain();
        WalkTarget walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET).orElse(null);
        if (walkTarget == null) {
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            return;
        }
        boolean blocked = entity.getNavigation().isDone()
                && !walkTarget.getTarget().currentBlockPosition().closerToCenterThan(
                        entity.position(), Math.max(1.0D, walkTarget.getCloseEnoughDist()));
        if (blocked) {
            if (!brain.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
                brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, level.getGameTime());
            }
        } else {
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        }
    }
}
