package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * Erases {@code ATTACK_TARGET} (and matching {@link Mob#getTarget()}) when the target dies,
 * becomes unattackable, moves out of range, or exceeds the pathfinding attention span.
 */
public class InvalidateAttackTarget<E extends LivingEntity> extends HiveBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = List.of(
            Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT),
            Pair.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED)
    );

    protected BiPredicate<E, LivingEntity> customPredicate = (entity, target) -> false;
    protected long pathfindingAttentionSpan = 200L;

    public InvalidateAttackTarget<E> invalidateIf(BiPredicate<E, LivingEntity> predicate) {
        if (predicate != null) {
            this.customPredicate = predicate;
        }
        return this;
    }

    public InvalidateAttackTarget<E> ignoreFailedPathfinding() {
        this.pathfindingAttentionSpan = 0L;
        return this;
    }

    public InvalidateAttackTarget<E> stopTryingToPathAfter(long ticks) {
        this.pathfindingAttentionSpan = Math.max(0L, ticks);
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected void start(E entity) {
        LivingEntity target = HiveBrainUtils.getMemory(entity, MemoryModuleType.ATTACK_TARGET);
        if (target == null || isTargetInvalid(entity, target) || isTiredOfPathing(entity) || this.customPredicate.test(entity, target)) {
            entity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            entity.getBrain().eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            if (entity instanceof Mob mob && mob.getTarget() == target) {
                mob.setTarget(null);
            }
        }
    }

    protected boolean isTargetInvalid(E entity, LivingEntity target) {
        if (!target.isAlive() || target.isRemoved() || target.level() != entity.level()) {
            return true;
        }
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return true;
        }
        if (!canAttack(entity, target)) {
            return true;
        }
        double range = entity.getAttributeValue(Attributes.FOLLOW_RANGE);
        return entity.distanceToSqr(target) > range * range;
    }

    protected boolean canAttack(E entity, LivingEntity target) {
        if (entity instanceof Mob mob) {
            return mob.canAttack(target);
        }
        return entity.canAttack(target);
    }

    protected boolean isTiredOfPathing(E entity) {
        if (this.pathfindingAttentionSpan <= 0L) {
            return false;
        }
        Long cantReachSince = HiveBrainUtils.getMemory(entity, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        return cantReachSince != null && entity.level().getGameTime() - cantReachSince > this.pathfindingAttentionSpan;
    }
}
