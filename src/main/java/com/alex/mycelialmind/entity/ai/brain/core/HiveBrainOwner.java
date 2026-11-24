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
 * Contract for entities powered by the native {@link HiveBrain} engine.
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

    @Nullable
    default HiveBrainSchedule getBrainSchedule() {
        return null;
    }

    default void handleAdditionalBrainSetup(Brain<? extends T> brain) {
    }

    default void handleAdditionalBrainSetup(HiveBrain<T> hiveBrain) {
    }

    default boolean shouldTickBrain(ServerLevel level, T entity) {
        return true;
    }

    default void onBrainTickStart(ServerLevel level, T entity, HiveBrain<T> brain) {
    }

    default void onActivityChanged(
            ServerLevel level, T entity, @Nullable Activity previousActivity, Activity nextActivity) {
    }

    default void onBrainTickEnd(ServerLevel level, T entity, HiveBrain<T> brain) {
    }

    @Nullable
    default HiveBrain<T> getHiveBrain() {
        return getHiveBrainRuntime();
    }

    default void setHiveBrain(@Nullable HiveBrain<T> brain) {
        if (brain == null || brain instanceof HiveBrainRuntime<T>) {
            setHiveBrainRuntime((HiveBrainRuntime<T>) brain);
        } else {
            HiveBrainRuntime<T> wrapped = new HiveBrainRuntime<>(
                    brain.getVanillaBrain(),
                    brain.getSensors(),
                    brain.getActivityPriorities(),
                    brain.getTaskGroups(),
                    getDefaultActivity()
            );
            wrapped.setSchedule(brain.getSchedule());
            setHiveBrainRuntime(wrapped);
        }
    }

    @Nullable
    HiveBrainRuntime<T> getHiveBrainRuntime();

    void setHiveBrainRuntime(@Nullable HiveBrainRuntime<T> runtime);

    @SuppressWarnings("unchecked")
    default void tickBrain(T entity) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        HiveBrain<T> hiveBrain = getHiveBrain();
        if (hiveBrain != null) {
            hiveBrain.tick(serverLevel, entity, (Brain<T>) entity.getBrain());
        } else {
            ((Brain<T>) entity.getBrain()).tick(serverLevel, entity);
        }
    }
}
