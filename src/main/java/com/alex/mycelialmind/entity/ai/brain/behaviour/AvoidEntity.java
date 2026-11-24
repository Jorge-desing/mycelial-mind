package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Sets a retreat {@code WALK_TARGET} away from the closest visible entity matching {@link #avoidingPredicate}
 * whenever it enters {@link #noCloserThan} blocks.
 */
public class AvoidEntity<E extends PathfinderMob> extends HiveBehaviour<E> {
    protected Predicate<LivingEntity> avoidingPredicate = entity -> false;
    protected float noCloserThan = 7.0F;
    protected float speedModifier = 1.2F;
    @Nullable
    protected LivingEntity threat;

    public AvoidEntity() {
        cooldownFor(entity -> 20);
    }

    public AvoidEntity<E> avoiding(Predicate<LivingEntity> predicate) {
        this.avoidingPredicate = predicate != null ? predicate : entity -> false;
        return this;
    }

    public AvoidEntity<E> noCloserThan(float distance) {
        this.noCloserThan = Math.max(1.0F, distance);
        return this;
    }

    public AvoidEntity<E> speedModifier(float speed) {
        this.speedModifier = Math.max(0.1F, speed);
        return this;
    }

    public float getNoCloserThan() {
        return this.noCloserThan;
    }

    public float getSpeedModifier() {
        return this.speedModifier;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(
                Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT),
                Pair.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.REGISTERED),
                Pair.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.REGISTERED),
                Pair.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.REGISTERED)
        );
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        this.threat = findClosestThreat(entity);
        return this.threat != null;
    }

    @Nullable
    private LivingEntity findClosestThreat(E entity) {
        double maxDistSqr = this.noCloserThan * this.noCloserThan;
        LivingEntity best = null;
        double bestDistSqr = maxDistSqr;

        NearestVisibleLivingEntities visible = HiveBrainUtils.getMemory(
                entity, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (visible != null) {
            LivingEntity found = visible.findClosest(candidate ->
                    candidate != entity
                            && candidate.isAlive()
                            && this.avoidingPredicate.test(candidate)
                            && entity.distanceToSqr(candidate) <= maxDistSqr).orElse(null);
            if (found != null) {
                best = found;
                bestDistSqr = entity.distanceToSqr(found);
            }
        }

        if (best == null) {
            List<LivingEntity> nearby = HiveBrainUtils.getMemory(entity, MemoryModuleType.NEAREST_LIVING_ENTITIES);
            if (nearby != null) {
                for (LivingEntity candidate : nearby) {
                    double dSqr = entity.distanceToSqr(candidate);
                    if (candidate != entity
                            && candidate.isAlive()
                            && dSqr <= bestDistSqr
                            && this.avoidingPredicate.test(candidate)
                            && entity.hasLineOfSight(candidate)) {
                        best = candidate;
                        bestDistSqr = dSqr;
                    }
                }
            }
        }

        List<Player> players = HiveBrainUtils.getMemory(entity, MemoryModuleType.NEAREST_PLAYERS);
        if (players != null) {
            for (Player player : players) {
                double dSqr = entity.distanceToSqr(player);
                if (player.isAlive()
                        && dSqr <= bestDistSqr
                        && this.avoidingPredicate.test(player)
                        && entity.hasLineOfSight(player)) {
                    best = player;
                    bestDistSqr = dSqr;
                }
            }
        }
        return best;
    }

    @Override
    protected void start(E entity) {
        if (this.threat == null) {
            return;
        }
        Vec3 away = DefaultRandomPos.getPosAway(entity, 12, 5, this.threat.position());
        if (away != null && away.distanceToSqr(this.threat.position()) > entity.distanceToSqr(this.threat)) {
            entity.getBrain().setMemory(
                    MemoryModuleType.WALK_TARGET,
                    new WalkTarget(away, this.speedModifier, 0)
            );
        }
        this.threat = null;
    }
}
