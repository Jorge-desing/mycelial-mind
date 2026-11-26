package com.alex.mycelialmind.entity.ai.brain.core;

import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import net.minecraft.nbt.CompoundTag;
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

    default void onBrainSignal(ServerLevel level, T entity, HiveBrain<T> brain, HiveBrainSignal signal) {
    }

    default Map<String, HiveBrainProfile<T>> getBrainProfiles() {
        return Map.of();
    }

    default List<HiveBrainModule<T>> getBrainModules() {
        return List.of();
    }

    default void onProfileChanged(ServerLevel level, T entity, String previousProfile, String nextProfile) {
    }

    default boolean applyBrainProfile(ServerLevel level, T entity, @Nullable String profileName) {
        HiveBrain<T> brain = getHiveBrain();
        return brain != null && brain.applyProfile(level, entity, profileName);
    }

    default boolean replaceVanillaBrain(Brain<T> newBrain) {
        return false;
    }

    @Nullable
    HiveBrain<T> getHiveBrain();

    void setHiveBrain(@Nullable HiveBrain<T> brain);

    @Nullable
    default HiveBrainRuntime<T> getHiveBrainRuntime() {
        return getHiveBrain() instanceof HiveBrainRuntime<T> runtime ? runtime : null;
    }

    default void setHiveBrainRuntime(@Nullable HiveBrainRuntime<T> runtime) {
        setHiveBrain(runtime);
    }

    default void dispatchBrainSignal(ServerLevel level, T entity, HiveBrainSignal signal) {
        HiveBrain<T> brain = getHiveBrain();
        if (brain != null) {
            brain.dispatchSignal(level, entity, signal);
        }
    }

    default void enqueueBrainSignal(HiveBrainSignal signal) {
        HiveBrain<T> brain = getHiveBrain();
        if (brain != null) {
            brain.enqueueSignal(signal);
        }
    }

    default void saveBrainState(CompoundTag tag) {
        long gameTime = (this instanceof LivingEntity living && living.level() != null)
                ? living.level().getGameTime()
                : -1L;
        saveBrainState(tag, gameTime);
    }

    default void saveBrainState(CompoundTag tag, long currentGameTime) {
        HiveBrain<T> brain = getHiveBrain();
        if (brain != null) {
            brain.saveToNbt(tag, currentGameTime);
        }
    }

    default void loadBrainState(CompoundTag tag) {
        HiveBrain<T> brain = getHiveBrain();
        if (brain != null) {
            brain.loadFromNbt(tag);
        }
    }

    @Nullable
    default HiveBrainSnapshot captureBrainSnapshot(long gameTime) {
        HiveBrain<T> brain = getHiveBrain();
        return brain != null ? brain.captureSnapshot(gameTime) : null;
    }

    static <E extends LivingEntity & HiveBrainOwner<E>> boolean evaluateStandardLod(
            ServerLevel level, E entity, boolean highPriority) {
        HiveBrain<E> brain = entity.getHiveBrain();
        long tickCountVal = brain != null ? brain.getBrainTickCount() : 0L;
        long wakeUntil = brain != null ? brain.getLodWakeUntilGameTime() : HiveBrainScheduler.INITIAL_TICK_SENTINEL;
        int lodMultiplier = brain != null ? brain.getLodIntervalMultiplier() : 1;
        boolean activeBrainState = brain != null
                && (brain.getPendingSignalCount() > 0 || brain.getActiveActivity() != entity.getDefaultActivity());
        boolean effectiveHighPriority = highPriority
                || entity.tickCount <= 2
                || entity.hurtTime > 0
                || activeBrainState;
        boolean nearPlayer = level.getNearestPlayer(entity, 32.0D) != null;
        return HiveBrainScheduler.shouldTickBrainWithLod(
                tickCountVal, entity.getId(), level.getGameTime(), wakeUntil, lodMultiplier, effectiveHighPriority, nearPlayer);
    }

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
