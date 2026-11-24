package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * Sets {@code LOOK_TARGET} to a nearby visible player matching the configured predicate.
 */
public class SetPlayerLookTarget<E extends LivingEntity> extends HiveBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = List.of(
            Pair.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT),
            Pair.of(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.REGISTERED),
            Pair.of(MemoryModuleType.NEAREST_PLAYERS, MemoryStatus.REGISTERED)
    );

    protected BiPredicate<E, Player> lookPredicate = this::defaultPredicate;
    @Nullable
    protected Player target;

    public SetPlayerLookTarget<E> predicate(Predicate<Player> predicate) {
        Predicate<Player> safe = predicate != null ? predicate : p -> true;
        this.lookPredicate = (entity, player) -> defaultPredicate(entity, player) && safe.test(player);
        return this;
    }

    public SetPlayerLookTarget<E> lookPredicate(BiPredicate<E, Player> predicate) {
        this.lookPredicate = predicate != null ? predicate : this::defaultPredicate;
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        this.target = null;
        NearestVisibleLivingEntities visible = HiveBrainUtils.getMemory(
                entity, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (visible != null) {
            LivingEntity found = visible.findClosest(
                    candidate -> candidate instanceof Player player && this.lookPredicate.test(entity, player))
                    .orElse(null);
            if (found instanceof Player player) {
                this.target = player;
                return true;
            }
        }
        List<Player> players = HiveBrainUtils.getMemory(entity, MemoryModuleType.NEAREST_PLAYERS);
        if (players != null) {
            for (Player player : players) {
                if (this.lookPredicate.test(entity, player) && entity.hasLineOfSight(player)) {
                    this.target = player;
                    return true;
                }
            }
        }
        return false;
    }

    protected boolean defaultPredicate(E entity, Player player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        double range = entity.getAttributeValue(Attributes.FOLLOW_RANGE);
        return entity.distanceToSqr(player) <= range * range;
    }

    @Override
    protected void start(E entity) {
        if (this.target != null) {
            entity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(this.target, true));
        }
    }

    @Override
    protected void stop(E entity) {
        this.target = null;
    }
}
