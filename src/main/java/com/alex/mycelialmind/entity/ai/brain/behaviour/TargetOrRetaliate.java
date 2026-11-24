package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Selects {@code ATTACK_TARGET} from a priority memory (default {@code HURT_BY_ENTITY}) or
 * nearby visible living entities matching {@link #canAttackPredicate}.
 */
public class TargetOrRetaliate<E extends Mob> extends HiveBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = List.of(
            Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.HURT_BY_ENTITY, MemoryStatus.REGISTERED),
            Pair.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.REGISTERED),
            Pair.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.REGISTERED)
    );

    protected Predicate<LivingEntity> canAttackPredicate = entity -> entity.isAlive()
            && (!(entity instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    protected BiPredicate<E, Entity> alertAlliesPredicate = (mob, target) -> false;
    protected BiPredicate<E, LivingEntity> allyPredicate = (mob, other) -> mob.getType() == other.getType();
    protected boolean canSwapTarget = true;
    @Nullable
    protected LivingEntity toTarget;
    protected MemoryModuleType<? extends LivingEntity> priorityTargetMemory = MemoryModuleType.HURT_BY_ENTITY;

    public TargetOrRetaliate<E> attackablePredicate(Predicate<LivingEntity> predicate) {
        this.canAttackPredicate = predicate != null ? predicate : entity -> true;
        return this;
    }

    public TargetOrRetaliate<E> useMemory(MemoryModuleType<? extends LivingEntity> memory) {
        if (memory != null) {
            this.priorityTargetMemory = memory;
        }
        return this;
    }

    public TargetOrRetaliate<E> alertAlliesWhen(BiPredicate<E, Entity> predicate) {
        this.alertAlliesPredicate = predicate != null ? predicate : (mob, target) -> false;
        return this;
    }

    public TargetOrRetaliate<E> isAllyIf(BiPredicate<E, LivingEntity> predicate) {
        this.allyPredicate = predicate != null ? predicate : (mob, other) -> mob.getType() == other.getType();
        return this;
    }

    public TargetOrRetaliate<E> noTargetSwapping() {
        this.canSwapTarget = false;
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean doStartCheck(ServerLevel level, E mob, long gameTime) {
        if (!this.canSwapTarget && HiveBrainUtils.hasMemory(mob, MemoryModuleType.ATTACK_TARGET)) {
            return false;
        }
        return super.doStartCheck(level, mob, gameTime);
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E mob) {
        this.toTarget = getTarget(mob, level, HiveBrainUtils.getMemory(mob, this.priorityTargetMemory));
        return this.toTarget != null;
    }

    @Nullable
    protected LivingEntity getTarget(E mob, ServerLevel level, @Nullable LivingEntity priorityTarget) {
        if (isValidCandidate(mob, priorityTarget)) {
            return priorityTarget;
        }
        NearestVisibleLivingEntities visible = HiveBrainUtils.getMemory(mob, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (visible != null) {
            LivingEntity found = visible.findClosest(candidate -> isValidCandidate(mob, candidate)).orElse(null);
            if (found != null) {
                return found;
            }
        }
        List<LivingEntity> nearby = HiveBrainUtils.getMemory(mob, MemoryModuleType.NEAREST_LIVING_ENTITIES);
        if (nearby != null) {
            for (LivingEntity candidate : nearby) {
                if (isValidCandidate(mob, candidate) && mob.hasLineOfSight(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isValidCandidate(E mob, @Nullable LivingEntity candidate) {
        if (candidate == null || candidate == mob || !candidate.isAlive() || candidate.level() != mob.level()) {
            return false;
        }
        if (!mob.canAttack(candidate) || !this.canAttackPredicate.test(candidate)) {
            return false;
        }
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return mob.distanceToSqr(candidate) <= range * range;
    }

    @Override
    protected void start(E mob) {
        if (this.toTarget == null) {
            return;
        }
        Brain<?> brain = mob.getBrain();
        brain.setMemory(MemoryModuleType.ATTACK_TARGET, this.toTarget);
        brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        if (mob.level() instanceof ServerLevel level && this.alertAlliesPredicate.test(mob, this.toTarget)) {
            alertAllies(level, mob);
        }
        this.toTarget = null;
    }

    protected void alertAllies(ServerLevel level, E mob) {
        if (this.toTarget == null) {
            return;
        }
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        for (LivingEntity nearby : level.getEntitiesOfClass(
                LivingEntity.class,
                mob.getBoundingBox().inflate(range, 8.0D, range),
                other -> other != mob && other.isAlive() && this.allyPredicate.test(mob, other))) {
            Brain<?> otherBrain = nearby.getBrain();
            if (!otherBrain.hasMemoryValue(MemoryModuleType.ATTACK_TARGET)
                    && (!(nearby instanceof Mob otherMob) || otherMob.canAttack(this.toTarget))) {
                otherBrain.setMemory(MemoryModuleType.ATTACK_TARGET, this.toTarget);
            }
        }
    }
}
