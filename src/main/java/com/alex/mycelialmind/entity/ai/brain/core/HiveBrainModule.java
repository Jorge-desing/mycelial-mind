package com.alex.mycelialmind.entity.ai.brain.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Pluggable per-entity cognitive module executed inside {@link HiveBrain#tick} so auxiliary
 * subsystems (such as {@link com.alex.mycelialmind.entity.ai.brain.btree.HiveBehaviorTree})
 * obey unified pause, LOD gating, signals, and profile transitions.
 */
public interface HiveBrainModule<E extends LivingEntity & HiveBrainOwner<E>> {
    String id();

    void tick(ServerLevel level, E entity, HiveBrain<E> brain, long gameTime);

    default void onSignal(ServerLevel level, E entity, HiveBrain<E> brain, HiveBrainSignal signal) {
    }

    default void onProfileChanged(
            ServerLevel level,
            E entity,
            HiveBrain<E> brain,
            String previousProfile,
            String nextProfile) {
    }

    default boolean isEnabled() {
        return true;
    }
}
