package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Retreats away from {@code HURT_BY_ENTITY} or {@code ATTACK_TARGET} whenever the threat
 * lies within {@link #fleeDistance}, populating {@code WALK_TARGET}.
 */
public class FleeTarget<E extends PathfinderMob> extends HiveBehaviour<E> {
    protected float fleeDistance = 12.0F;
    protected float speedModifier = 1.25F;
    @Nullable
    protected LivingEntity threat;

    public FleeTarget() {
        cooldownFor(entity -> 30);
    }

    public FleeTarget<E> fleeDistance(float distance) {
        this.fleeDistance = Math.max(2.0F, distance);
        return this;
    }

    public FleeTarget<E> speedModifier(float speed) {
        this.speedModifier = Math.max(0.1F, speed);
        return this;
    }

    public float getFleeDistance() {
        return this.fleeDistance;
    }

    public float getSpeedModifier() {
        return this.speedModifier;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(
                Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED),
                Pair.of(MemoryModuleType.HURT_BY_ENTITY, MemoryStatus.REGISTERED),
                Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)
        );
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        LivingEntity candidate = HiveBrainUtils.getMemory(entity, MemoryModuleType.HURT_BY_ENTITY);
        if (candidate == null || !candidate.isAlive()) {
            candidate = HiveBrainUtils.getMemory(entity, MemoryModuleType.ATTACK_TARGET);
        }
        if (candidate == null || !candidate.isAlive() || candidate.level() != entity.level()) {
            this.threat = null;
            return false;
        }
        double maxDistSqr = this.fleeDistance * this.fleeDistance;
        if (entity.distanceToSqr(candidate) > maxDistSqr) {
            this.threat = null;
            return false;
        }
        this.threat = candidate;
        return true;
    }

    @Override
    protected void start(E entity) {
        if (this.threat == null) {
            return;
        }
        Vec3 away = DefaultRandomPos.getPosAway(entity, 14, 6, this.threat.position());
        if (away != null && away.distanceToSqr(this.threat.position()) > entity.distanceToSqr(this.threat)) {
            entity.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(away, this.speedModifier, 0)
            );
        }
        this.threat = null;
    }
}
