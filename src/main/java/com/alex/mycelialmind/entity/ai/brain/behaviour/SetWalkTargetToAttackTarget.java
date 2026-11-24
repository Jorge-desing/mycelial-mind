package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.ToIntBiFunction;

/**
 * Keeps {@code WALK_TARGET} and {@code LOOK_TARGET} locked onto {@code ATTACK_TARGET}
 * whenever the entity is not yet within close-enough distance.
 */
public class SetWalkTargetToAttackTarget<E extends Mob> extends HiveBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = List.of(
            Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
            Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED),
            Pair.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED)
    );

    protected BiFunction<E, LivingEntity, Float> speedMod = (mob, target) -> 1.0F;
    protected ToIntBiFunction<E, LivingEntity> closeEnoughWhen = (mob, target) -> 0;
    protected BiPredicate<E, LivingEntity> targetEyePosition = (mob, target) -> false;

    public SetWalkTargetToAttackTarget<E> speedMod(float speed) {
        this.speedMod = (mob, target) -> speed;
        return this;
    }

    public SetWalkTargetToAttackTarget<E> speedMod(BiFunction<E, LivingEntity, Float> speedMod) {
        if (speedMod != null) {
            this.speedMod = speedMod;
        }
        return this;
    }

    public SetWalkTargetToAttackTarget<E> closeEnoughDist(ToIntBiFunction<E, LivingEntity> closeEnoughWhen) {
        if (closeEnoughWhen != null) {
            this.closeEnoughWhen = closeEnoughWhen;
        }
        return this;
    }

    public SetWalkTargetToAttackTarget<E> targetEyePosition() {
        this.targetEyePosition = (mob, target) -> true;
        return this;
    }

    public SetWalkTargetToAttackTarget<E> targetEyePosition(BiPredicate<E, LivingEntity> predicate) {
        if (predicate != null) {
            this.targetEyePosition = predicate;
        }
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected void start(E mob) {
        Brain<?> brain = mob.getBrain();
        LivingEntity target = HiveBrainUtils.getMemory(brain, MemoryModuleType.ATTACK_TARGET);
        if (target == null) {
            return;
        }
        if (mob.isWithinMeleeAttackRange(target)) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            return;
        }
        PositionTracker tracker = new EntityTracker(target, this.targetEyePosition.test(mob, target));
        brain.setMemory(MemoryModuleType.LOOK_TARGET, tracker);
        brain.setMemory(
                MemoryModuleType.WALK_TARGET,
                new WalkTarget(tracker, this.speedMod.apply(mob, target), this.closeEnoughWhen.applyAsInt(mob, target))
        );
    }
}
