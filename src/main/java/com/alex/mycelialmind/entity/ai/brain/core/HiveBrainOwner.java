package com.alex.mycelialmind.entity.ai.brain.core;

import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contract for entities powered by the native {@code HiveBrain} engine.
 */
public interface HiveBrainOwner<T extends LivingEntity & HiveBrainOwner<T>> {
    List<? extends HiveSensor<? extends T>> getSensors();

    default BrainActivityGroup<? extends T> getCoreTasks() {
        return BrainActivityGroup.empty();
    }

    default BrainActivityGroup<? extends T> getIdleTasks() {
        return BrainActivityGroup.empty();
    }

    default BrainActivityGroup<? extends T> getFightTasks() {
        return BrainActivityGroup.empty();
    }

    default Map<Activity, BrainActivityGroup<? extends T>> getAdditionalTasks() {
        return Map.of();
    }

    default Set<Activity> getAlwaysRunningActivities() {
        return Set.of(Activity.CORE);
    }

    default Activity getDefaultActivity() {
        return Activity.IDLE;
    }

    default List<Activity> getActivityPriorities() {
        return List.of(Activity.FIGHT, Activity.IDLE);
    }

    default void handleAdditionalBrainSetup(Brain<? extends T> brain) {
    }

    @Nullable
    HiveBrainRuntime<T> getHiveBrainRuntime();

    void setHiveBrainRuntime(@Nullable HiveBrainRuntime<T> runtime);

    @SuppressWarnings("unchecked")
    default void tickBrain(T entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        HiveBrainRuntime<T> runtime = getHiveBrainRuntime();
        if (runtime != null) {
            runtime.tick(serverLevel, entity, (Brain<T>) entity.getBrain());
        } else {
            ((Brain<T>) entity.getBrain()).tick(serverLevel, entity);
        }
    }
}
