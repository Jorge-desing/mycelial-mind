package com.alex.mycelialmind.entity.ai.brain.core;

import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * First-class clean-room AI brain coordinator wrapping a vanilla {@link Brain},
 * typed {@link HiveSensor}s, {@link HiveBrainSchedule}, activity transition tracking,
 * and zero-allocation per-tick scheduling without Access Transformers or Mixins.
 */
public class HiveBrain<E extends LivingEntity & HiveBrainOwner<E>> {
    @Nullable
    private Brain<E> vanillaBrain;
    private final List<HiveSensor<? super E>> sensors;
    private final List<Activity> activityPriorities;
    private final List<BrainActivityGroup<E>> taskGroups;
    private final List<Activity> priorityResolutionBuffer = new ArrayList<>(8);

    @Nullable
    private HiveBrainSchedule schedule;
    private boolean prioritizeScheduleAfterFight = true;

    @Nullable
    private Activity activeActivity;
    @Nullable
    private Activity previousActivity;
    @Nullable
    private Activity defaultActivity;
    private long lastActivityChangeGameTime = HiveBrainScheduler.INITIAL_TICK_SENTINEL;
    private long brainTickCount = 0L;

    public HiveBrain(List<HiveSensor<? super E>> sensors, List<Activity> activityPriorities) {
        this(null, sensors, activityPriorities, List.of(), null);
    }

    public HiveBrain(
            @Nullable Brain<E> vanillaBrain,
            List<HiveSensor<? super E>> sensors,
            List<Activity> activityPriorities,
            List<BrainActivityGroup<E>> taskGroups,
            @Nullable Activity defaultActivity) {
        this.vanillaBrain = vanillaBrain;
        this.sensors = List.copyOf(sensors);
        this.activityPriorities = List.copyOf(activityPriorities);
        this.taskGroups = List.copyOf(taskGroups);
        this.defaultActivity = defaultActivity;
        this.activeActivity = defaultActivity;
    }

    @Nullable
    public Brain<E> getVanillaBrain() {
        return this.vanillaBrain;
    }

    public List<HiveSensor<? super E>> getSensors() {
        return Collections.unmodifiableList(this.sensors);
    }

    public List<Activity> getActivityPriorities() {
        return Collections.unmodifiableList(this.activityPriorities);
    }

    public List<BrainActivityGroup<E>> getTaskGroups() {
        return Collections.unmodifiableList(this.taskGroups);
    }

    @Nullable
    public HiveBrainSchedule getSchedule() {
        return this.schedule;
    }

    public void setSchedule(@Nullable HiveBrainSchedule schedule) {
        this.schedule = schedule;
    }

    public boolean isPrioritizeScheduleAfterFight() {
        return this.prioritizeScheduleAfterFight;
    }

    public void setPrioritizeScheduleAfterFight(boolean prioritizeScheduleAfterFight) {
        this.prioritizeScheduleAfterFight = prioritizeScheduleAfterFight;
    }

    @Nullable
    public Activity getActiveActivity() {
        return this.activeActivity;
    }

    @Nullable
    public Activity getPreviousActivity() {
        return this.previousActivity;
    }

    public long getLastActivityChangeGameTime() {
        return this.lastActivityChangeGameTime;
    }

    public long getTicksInCurrentActivity(long gameTime) {
        if (this.lastActivityChangeGameTime == HiveBrainScheduler.INITIAL_TICK_SENTINEL) {
            return this.brainTickCount;
        }
        return Math.max(0L, gameTime - this.lastActivityChangeGameTime);
    }

    public long getBrainTickCount() {
        return this.brainTickCount;
    }

    public void initializeActiveActivity(@Nullable Activity activity) {
        this.defaultActivity = activity;
        this.activeActivity = activity;
    }

    public void tick(ServerLevel level, E entity) {
        @SuppressWarnings("unchecked")
        Brain<E> brain = this.vanillaBrain != null ? this.vanillaBrain : (Brain<E>) entity.getBrain();
        tick(level, entity, brain);
    }

    public void tick(ServerLevel level, E entity, Brain<E> brain) {
        if (this.vanillaBrain == null) {
            this.vanillaBrain = brain;
        }
        if (!entity.shouldTickBrain(level, entity)) {
            return;
        }

        entity.onBrainTickStart(level, entity, this);
        this.brainTickCount++;

        for (int i = 0, size = this.sensors.size(); i < size; i++) {
            this.sensors.get(i).tick(level, entity);
        }

        Activity scheduledActivity = null;
        if (this.schedule != null) {
            this.schedule.runCallbacks(level, entity);
            scheduledActivity = this.schedule.resolveActivity(level, entity);
        }

        brain.tick(level, entity);

        if (scheduledActivity != null) {
            resolvePriorityBuffer(scheduledActivity);
            brain.setActiveActivityToFirstValid(this.priorityResolutionBuffer);
        } else if (!this.activityPriorities.isEmpty()) {
            brain.setActiveActivityToFirstValid(this.activityPriorities);
        }

        Activity current = resolveCurrentActiveActivity(brain, scheduledActivity);
        if (current != null && current != this.activeActivity) {
            Activity prev = this.activeActivity;
            this.previousActivity = prev;
            this.activeActivity = current;
            this.lastActivityChangeGameTime = level.getGameTime();
            entity.onActivityChanged(level, entity, prev, current);
        }

        entity.onBrainTickEnd(level, entity, this);
    }

    private void resolvePriorityBuffer(Activity scheduledActivity) {
        this.priorityResolutionBuffer.clear();
        if (this.prioritizeScheduleAfterFight) {
            for (int i = 0, size = this.activityPriorities.size(); i < size; i++) {
                Activity candidate = this.activityPriorities.get(i);
                if (candidate == Activity.FIGHT || candidate == Activity.PANIC) {
                    this.priorityResolutionBuffer.add(candidate);
                }
            }
            if (!this.priorityResolutionBuffer.contains(scheduledActivity)) {
                this.priorityResolutionBuffer.add(scheduledActivity);
            }
            for (int i = 0, size = this.activityPriorities.size(); i < size; i++) {
                Activity candidate = this.activityPriorities.get(i);
                if (!this.priorityResolutionBuffer.contains(candidate)) {
                    this.priorityResolutionBuffer.add(candidate);
                }
            }
        } else {
            this.priorityResolutionBuffer.add(scheduledActivity);
            for (int i = 0, size = this.activityPriorities.size(); i < size; i++) {
                Activity candidate = this.activityPriorities.get(i);
                if (candidate != scheduledActivity && !this.priorityResolutionBuffer.contains(candidate)) {
                    this.priorityResolutionBuffer.add(candidate);
                }
            }
        }
    }

    @Nullable
    private Activity resolveCurrentActiveActivity(Brain<E> brain, @Nullable Activity scheduledActivity) {
        if (scheduledActivity != null && !this.prioritizeScheduleAfterFight && brain.isActive(scheduledActivity)) {
            return scheduledActivity;
        }
        for (int i = 0, size = this.activityPriorities.size(); i < size; i++) {
            Activity candidate = this.activityPriorities.get(i);
            if (brain.isActive(candidate)) {
                return candidate;
            }
        }
        if (scheduledActivity != null && brain.isActive(scheduledActivity)) {
            return scheduledActivity;
        }
        if (this.defaultActivity != null && brain.isActive(this.defaultActivity)) {
            return this.defaultActivity;
        }
        for (int i = 0, size = this.taskGroups.size(); i < size; i++) {
            Activity candidate = this.taskGroups.get(i).getActivity();
            if (candidate != Activity.CORE && brain.isActive(candidate)) {
                return candidate;
            }
        }
        return this.activeActivity;
    }

    public void triggerSensorsNow(ServerLevel level, E entity) {
        for (int i = 0, size = this.sensors.size(); i < size; i++) {
            this.sensors.get(i).forceScan(level, entity);
        }
    }

    public boolean forceActivity(Activity activity) {
        if (this.vanillaBrain == null || activity == null) {
            return false;
        }
        this.vanillaBrain.setActiveActivityIfPossible(activity);
        if (this.vanillaBrain.isActive(activity)) {
            if (activity != this.activeActivity) {
                this.previousActivity = this.activeActivity;
                this.activeActivity = activity;
            }
            return true;
        }
        return false;
    }

    public void resetToDefaultActivity() {
        if (this.vanillaBrain != null) {
            this.vanillaBrain.useDefaultActivity();
            if (this.defaultActivity != null && this.vanillaBrain.isActive(this.defaultActivity)
                    && this.defaultActivity != this.activeActivity) {
                this.previousActivity = this.activeActivity;
                this.activeActivity = this.defaultActivity;
            }
        }
    }

    public int getRunningBehaviourCount() {
        int running = 0;
        for (int i = 0, gSize = this.taskGroups.size(); i < gSize; i++) {
            List<HiveBehaviour<? super E>> behaviours = this.taskGroups.get(i).getBehaviours();
            for (int j = 0, bSize = behaviours.size(); j < bSize; j++) {
                if (behaviours.get(j).getStatus() == Behavior.Status.RUNNING) {
                    running++;
                }
            }
        }
        return running;
    }

    public List<String> describeRunningBehaviours() {
        List<String> active = new ArrayList<>();
        for (int i = 0, gSize = this.taskGroups.size(); i < gSize; i++) {
            BrainActivityGroup<E> group = this.taskGroups.get(i);
            List<HiveBehaviour<? super E>> behaviours = group.getBehaviours();
            for (int j = 0, bSize = behaviours.size(); j < bSize; j++) {
                HiveBehaviour<? super E> behaviour = behaviours.get(j);
                if (behaviour.getStatus() == Behavior.Status.RUNNING) {
                    active.add(group.getActivity().getName() + ":" + behaviour.debugString());
                }
            }
        }
        return active;
    }

    @Nullable
    public <M> M getMemory(MemoryModuleType<M> type) {
        return this.vanillaBrain != null ? HiveBrainUtils.getMemory(this.vanillaBrain, type) : null;
    }

    public <M> Optional<M> findMemory(MemoryModuleType<M> type) {
        return this.vanillaBrain != null ? this.vanillaBrain.getMemory(type) : Optional.empty();
    }

    public <M> M memoryOrDefault(MemoryModuleType<M> type, Supplier<M> fallback) {
        return this.vanillaBrain != null
                ? HiveBrainUtils.memoryOrDefault(this.vanillaBrain, type, fallback)
                : fallback.get();
    }

    public <M> M computeMemoryIfAbsent(MemoryModuleType<M> type, Supplier<M> supplier) {
        if (this.vanillaBrain == null) {
            return supplier.get();
        }
        return HiveBrainUtils.computeMemoryIfAbsent(this.vanillaBrain, type, supplier);
    }

    public <M> void setMemory(MemoryModuleType<M> type, @Nullable M value) {
        if (this.vanillaBrain != null) {
            HiveBrainUtils.setMemory(this.vanillaBrain, type, value);
        }
    }

    public <M> void setForgettableMemory(MemoryModuleType<M> type, M value, int expiryTicks) {
        if (this.vanillaBrain != null) {
            HiveBrainUtils.setForgettableMemory(this.vanillaBrain, type, value, expiryTicks);
        }
    }

    public void clearMemory(MemoryModuleType<?> type) {
        if (this.vanillaBrain != null) {
            HiveBrainUtils.clearMemory(this.vanillaBrain, type);
        }
    }

    public void clearMemories(MemoryModuleType<?>... types) {
        if (this.vanillaBrain != null) {
            HiveBrainUtils.clearMemories(this.vanillaBrain, types);
        }
    }

    public boolean hasMemory(MemoryModuleType<?> type) {
        return this.vanillaBrain != null && HiveBrainUtils.hasMemory(this.vanillaBrain, type);
    }

    public long getTimeUntilMemoryExpires(MemoryModuleType<?> type) {
        return this.vanillaBrain != null ? HiveBrainUtils.getTimeUntilMemoryExpires(this.vanillaBrain, type) : 0L;
    }

    @Nullable
    public LivingEntity getAttackTarget() {
        return getMemory(MemoryModuleType.ATTACK_TARGET);
    }

    public void setAttackTarget(@Nullable LivingEntity target) {
        if (target == null) {
            clearMemory(MemoryModuleType.ATTACK_TARGET);
        } else {
            setMemory(MemoryModuleType.ATTACK_TARGET, target);
        }
    }
}
