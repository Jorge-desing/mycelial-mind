package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Selects a nearby random walkable position and sets {@code WALK_TARGET}.
 */
public class SetRandomWalkTarget<E extends PathfinderMob> extends HiveBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = List.of(
            Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT)
    );

    protected BiFunction<E, Vec3, Float> speedModifier = (mob, pos) -> 1.0F;
    protected Predicate<E> avoidWaterPredicate = mob -> true;
    protected int radiusXZ = 10;
    protected int radiusY = 7;
    protected BiPredicate<E, Vec3> positionPredicate = (mob, pos) -> true;

    public SetRandomWalkTarget<E> setRadius(double radius) {
        return setRadius(radius, radius);
    }

    public SetRandomWalkTarget<E> setRadius(double xz, double y) {
        this.radiusXZ = Math.max(1, (int) Math.round(xz));
        this.radiusY = Math.max(1, (int) Math.round(y));
        return this;
    }

    public SetRandomWalkTarget<E> speedModifier(float speed) {
        this.speedModifier = (mob, pos) -> speed;
        return this;
    }

    public SetRandomWalkTarget<E> speedModifier(BiFunction<E, Vec3, Float> speedModifier) {
        if (speedModifier != null) {
            this.speedModifier = speedModifier;
        }
        return this;
    }

    public SetRandomWalkTarget<E> walkTargetPredicate(BiPredicate<E, Vec3> predicate) {
        if (predicate != null) {
            this.positionPredicate = predicate;
        }
        return this;
    }

    public SetRandomWalkTarget<E> dontAvoidWater() {
        this.avoidWaterPredicate = mob -> false;
        return this;
    }

    public SetRandomWalkTarget<E> avoidWaterWhen(Predicate<E> predicate) {
        if (predicate != null) {
            this.avoidWaterPredicate = predicate;
        }
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected void start(E mob) {
        Vec3 targetPos = getTargetPos(mob);
        if (targetPos != null && this.positionPredicate.test(mob, targetPos)) {
            mob.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(targetPos, this.speedModifier.apply(mob, targetPos), 0)
            );
        } else {
            mob.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        }
    }

    @Nullable
    protected Vec3 getTargetPos(E mob) {
        if (this.avoidWaterPredicate.test(mob)) {
            return LandRandomPos.getPos(mob, this.radiusXZ, this.radiusY);
        }
        return DefaultRandomPos.getPos(mob, this.radiusXZ, this.radiusY);
    }
}
