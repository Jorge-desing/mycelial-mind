package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.List;
import java.util.function.Predicate;

/**
 * Maintains spacing around {@code ATTACK_TARGET} while strafing laterally using {@code MoveControl#strafe}.
 */
public class StrafeTarget<E extends PathfinderMob> extends HiveBehaviour<E> {
    protected float strafeDistance = 10.0F;
    protected float speedMod = 0.85F;
    protected Predicate<E> stopStrafingWhen = entity -> false;
    protected boolean strafeClockwise = false;
    protected int strafeTicks = 0;

    public StrafeTarget() {
        runForBetween(40, 80);
        cooldownFor(entity -> 10);
    }

    public StrafeTarget<E> strafeDistance(float distance) {
        this.strafeDistance = Math.max(1.0F, distance);
        return this;
    }

    public StrafeTarget<E> speedMod(float speed) {
        this.speedMod = Math.max(0.1F, speed);
        return this;
    }

    public StrafeTarget<E> stopStrafingWhen(Predicate<E> predicate) {
        this.stopStrafingWhen = predicate != null ? predicate : entity -> false;
        return this;
    }

    public float getStrafeDistance() {
        return this.strafeDistance;
    }

    public float getSpeedMod() {
        return this.speedMod;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(
                Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
                Pair.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED)
        );
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        if (this.stopStrafingWhen.test(entity)) {
            return false;
        }
        LivingEntity target = HiveBrainUtils.getMemory(entity, MemoryModuleType.ATTACK_TARGET);
        return target != null && target.isAlive() && entity.hasLineOfSight(target);
    }

    @Override
    protected boolean shouldKeepRunning(E entity) {
        if (this.stopStrafingWhen.test(entity)) {
            return false;
        }
        LivingEntity target = HiveBrainUtils.getMemory(entity, MemoryModuleType.ATTACK_TARGET);
        return target != null && target.isAlive();
    }

    @Override
    protected void start(E entity) {
        this.strafeClockwise = entity.getRandom().nextBoolean();
        this.strafeTicks = 0;
    }

    @Override
    protected void tick(E entity) {
        LivingEntity target = HiveBrainUtils.getMemory(entity, MemoryModuleType.ATTACK_TARGET);
        if (target == null || !target.isAlive()) {
            return;
        }
        entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(target, true));
        entity.getLookControl().setLookAt(target, 30.0F, 30.0F);

        double distSqr = entity.distanceToSqr(target);
        double idealSqr = this.strafeDistance * this.strafeDistance;
        float forward = 0.0F;
        if (distSqr > idealSqr * 1.15D) {
            forward = this.speedMod;
        } else if (distSqr < idealSqr * 0.65D) {
            forward = -this.speedMod;
        }
        if (++this.strafeTicks >= 20) {
            this.strafeTicks = 0;
            if (entity.getRandom().nextFloat() < 0.35F) {
                this.strafeClockwise = !this.strafeClockwise;
            }
        }
        float lateral = (this.strafeClockwise ? 1.0F : -1.0F) * this.speedMod * 0.75F;
        entity.getMoveControl().strafe(forward, lateral);
    }

    @Override
    protected void stop(E entity) {
        entity.getMoveControl().strafe(0.0F, 0.0F);
    }
}
