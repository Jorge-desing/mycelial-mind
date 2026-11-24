package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Triggers a custom reaction callback once {@code CANT_REACH_WALK_TARGET_SINCE} exceeds a
 * configured tick window, clearing stalled navigation state (and optionally {@code ATTACK_TARGET}).
 */
public class ReactToUnreachableTarget<E extends PathfinderMob> extends HiveBehaviour<E> {
    protected long unreachableThresholdTicks = 40L;
    protected BiConsumer<ServerLevel, E> reaction = (level, entity) -> {};
    protected boolean clearAttackTarget = false;

    public ReactToUnreachableTarget() {
        cooldownFor(entity -> 60);
    }

    public ReactToUnreachableTarget<E> afterTicks(long ticks) {
        this.unreachableThresholdTicks = Math.max(1L, ticks);
        return this;
    }

    public ReactToUnreachableTarget<E> reaction(BiConsumer<ServerLevel, E> reaction) {
        this.reaction = reaction != null ? reaction : (level, entity) -> {};
        return this;
    }

    public ReactToUnreachableTarget<E> clearAttackTarget(boolean clear) {
        this.clearAttackTarget = clear;
        return this;
    }

    public long getUnreachableThresholdTicks() {
        return this.unreachableThresholdTicks;
    }

    public boolean isClearAttackTarget() {
        return this.clearAttackTarget;
    }

    public static boolean hasExceededUnreachableWindow(long gameTime, long sinceTick, long thresholdTicks) {
        return sinceTick >= 0L && thresholdTicks >= 0L && (gameTime - sinceTick) >= thresholdTicks;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(
                Pair.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.VALUE_PRESENT),
                Pair.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED),
                Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED)
        );
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        Long sinceTick = HiveBrainUtils.getMemory(entity, MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        return sinceTick != null && hasExceededUnreachableWindow(level.getGameTime(), sinceTick, this.unreachableThresholdTicks);
    }

    @Override
    protected void start(ServerLevel level, E entity, long gameTime) {
        this.reaction.accept(level, entity);
        Brain<?> brain = entity.getBrain();
        brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        if (this.clearAttackTarget) {
            brain.eraseMemory(MemoryModuleType.ATTACK_TARGET);
            entity.setTarget(null);
        }
    }
}
