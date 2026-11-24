package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.DelayedBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

/**
 * Timed melee strike behaviour that swings at start, waits {@code delayTicks} for the hit frame,
 * deals damage via {@link Mob#doHurtTarget}, and sets {@code ATTACK_COOLING_DOWN}.
 */
public class AnimatableMeleeAttack<E extends Mob> extends DelayedBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = List.of(
            Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
            Pair.of(MemoryModuleType.ATTACK_COOLING_DOWN, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED)
    );

    protected Function<E, Integer> attackIntervalSupplier = mob -> 20;
    @Nullable
    protected LivingEntity target;

    public AnimatableMeleeAttack(int delayTicks) {
        super(delayTicks);
    }

    public AnimatableMeleeAttack<E> attackInterval(Function<E, Integer> intervalSupplier) {
        if (intervalSupplier != null) {
            this.attackIntervalSupplier = intervalSupplier;
        }
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E mob) {
        this.target = HiveBrainUtils.getMemory(mob, MemoryModuleType.ATTACK_TARGET);
        return this.target != null
                && this.target.isAlive()
                && mob.isWithinMeleeAttackRange(this.target)
                && mob.getSensing().hasLineOfSight(this.target);
    }

    @Override
    protected void start(E mob) {
        if (this.target != null) {
            mob.swing(InteractionHand.MAIN_HAND);
            mob.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.target, true));
        }
    }

    @Override
    protected void stop(E mob) {
        this.target = null;
    }

    @Override
    protected void doDelayedAction(E mob) {
        if (this.target != null && this.target.isAlive() && mob.isWithinMeleeAttackRange(this.target)) {
            mob.doHurtTarget(this.target);
        }
        int interval = Math.max(1, this.attackIntervalSupplier.apply(mob));
        HiveBrainUtils.setForgettableMemory(mob, MemoryModuleType.ATTACK_COOLING_DOWN, true, interval);
    }
}
