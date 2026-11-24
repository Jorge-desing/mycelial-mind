package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.List;

/**
 * Rotates the mob's {@link net.minecraft.world.entity.ai.control.LookControl} toward {@code LOOK_TARGET}.
 */
public class LookAtTarget<E extends Mob> extends HiveBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = List.of(
            Pair.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT)
    );

    public LookAtTarget() {
        runForBetween(45, 90);
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E mob) {
        return testAndInvalidateLookTarget(mob);
    }

    @Override
    protected boolean shouldKeepRunning(E mob) {
        return testAndInvalidateLookTarget(mob);
    }

    @Override
    protected void tick(E mob) {
        mob.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).ifPresent(tracker ->
                mob.getLookControl().setLookAt(tracker.currentPosition()));
    }

    protected boolean testAndInvalidateLookTarget(E mob) {
        PositionTracker tracker = mob.getBrain().getMemory(MemoryModuleType.LOOK_TARGET).orElse(null);
        if (tracker == null) {
            return false;
        }
        if (tracker instanceof EntityTracker entityTracker && !entityTracker.getEntity().isAlive()) {
            mob.getBrain().eraseMemory(MemoryModuleType.LOOK_TARGET);
            return false;
        }
        return true;
    }
}
