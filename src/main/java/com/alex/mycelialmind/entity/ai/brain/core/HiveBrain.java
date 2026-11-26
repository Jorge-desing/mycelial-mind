package com.alex.mycelialmind.entity.ai.brain.core;

import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * First-class clean-room AI brain coordinator wrapping a vanilla {@link Brain},
 * typed {@link HiveSensor}s, {@link HiveBrainSchedule}, {@link HiveBrainBlackboard},
 * reactive {@link HiveBrainSignal}s, NBT persistence, and zero-allocation per-tick scheduling
 * without Access Transformers or Mixins.
 */
public class HiveBrain<E extends LivingEntity & HiveBrainOwner<E>> {
    public static final String NBT_BRAIN_STATE = "HiveBrainState";
    public static final int DEFAULT_SIGNAL_WAKE_TICKS = 40;
    private static final int MAX_QUEUED_SIGNALS = 16;
    private static final String TAG_PROFILE = "Profile";
    private static final String TAG_PAUSED = "Paused";

    @Nullable
    private Brain<E> vanillaBrain;
    private final List<HiveSensor<? super E>> sensors;
    private final boolean[] sensorEnabled;
    private final List<Activity> activityPriorities;
    private final List<BrainActivityGroup<E>> taskGroups;
    private final List<Activity> priorityResolutionBuffer = new ArrayList<>(8);
    private final HiveBrainBlackboard blackboard = new HiveBrainBlackboard();
    private final ArrayDeque<HiveBrainSignal> pendingSignals = new ArrayDeque<>(4);

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
    private long skippedLodTickCount = 0L;
    private long lodWakeUntilGameTime = HiveBrainScheduler.INITIAL_TICK_SENTINEL;
    private long signalCount = 0L;
    @Nullable
    private HiveBrainSignal lastSignal;
    private boolean paused = false;
    private String activeProfile = "default";
    private boolean dispatchingUrgentSensors = false;

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
        this.sensorEnabled = new boolean[this.sensors.size()];
        Arrays.fill(this.sensorEnabled, true);
        this.activityPriorities = List.copyOf(activityPriorities);
        this.taskGroups = List.copyOf(taskGroups);
        this.defaultActivity = defaultActivity;
        this.activeActivity = defaultActivity;
    }

    public HiveBrainBlackboard getBlackboard() {
        return this.blackboard;
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
        if (this.paused) {
            return;
        }
        if (!entity.shouldTickBrain(level, entity)) {
            this.skippedLodTickCount++;
            return;
        }

        entity.onBrainTickStart(level, entity, this);
        this.brainTickCount++;
        drainPendingSignals(level, entity);
        if ((this.brainTickCount % 40L) == 0L) {
            this.blackboard.pruneExpired(level.getGameTime());
        }

        for (int i = 0, size = this.sensors.size(); i < size; i++) {
            if (this.sensorEnabled[i]) {
                this.sensors.get(i).tick(level, entity);
            }
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
            if (this.sensorEnabled[i]) {
                this.sensors.get(i).forceScan(level, entity);
            }
        }
    }

    public void dispatchSignal(ServerLevel level, E entity, HiveBrainSignal signal) {
        if (level == null || entity == null || signal == null) {
            return;
        }
        deliverSignal(level, entity, signal);
    }

    public void enqueueSignal(HiveBrainSignal signal) {
        if (signal == null) {
            return;
        }
        while (this.pendingSignals.size() >= MAX_QUEUED_SIGNALS) {
            this.pendingSignals.pollFirst();
        }
        this.pendingSignals.addLast(signal);
        if (signal.emittedGameTime() >= 0L) {
            wakeLodForTicks(signal.emittedGameTime(), DEFAULT_SIGNAL_WAKE_TICKS);
        }
    }

    private void drainPendingSignals(ServerLevel level, E entity) {
        int remaining = Math.min(this.pendingSignals.size(), MAX_QUEUED_SIGNALS);
        while (remaining-- > 0 && !this.pendingSignals.isEmpty()) {
            HiveBrainSignal signal = this.pendingSignals.pollFirst();
            if (signal != null) {
                deliverSignal(level, entity, signal);
            }
        }
    }

    private void deliverSignal(ServerLevel level, E entity, HiveBrainSignal signal) {
        this.signalCount++;
        this.lastSignal = signal;
        wakeLodForTicks(level.getGameTime(), DEFAULT_SIGNAL_WAKE_TICKS);
        if (signal.urgent() && !this.paused && !this.dispatchingUrgentSensors) {
            this.dispatchingUrgentSensors = true;
            try {
                triggerSensorsNow(level, entity);
            } finally {
                this.dispatchingUrgentSensors = false;
            }
        }
        entity.onBrainSignal(level, entity, this, signal);
    }

    public void wakeLodForTicks(long gameTime, int ticks) {
        long candidate = HiveBrainScheduler.computeLodWakeUntil(gameTime, ticks);
        if (candidate != HiveBrainScheduler.INITIAL_TICK_SENTINEL
                && (this.lodWakeUntilGameTime == HiveBrainScheduler.INITIAL_TICK_SENTINEL
                        || candidate > this.lodWakeUntilGameTime)) {
            this.lodWakeUntilGameTime = candidate;
        }
    }

    public long getLodWakeUntilGameTime() {
        return this.lodWakeUntilGameTime;
    }

    public boolean isLodAwake(long gameTime) {
        return this.lodWakeUntilGameTime != HiveBrainScheduler.INITIAL_TICK_SENTINEL
                && gameTime < this.lodWakeUntilGameTime;
    }

    public long getSkippedLodTickCount() {
        return this.skippedLodTickCount;
    }

    public int getPendingSignalCount() {
        return this.pendingSignals.size();
    }

    public long getSignalCount() {
        return this.signalCount;
    }

    @Nullable
    public HiveBrainSignal getLastSignal() {
        return this.lastSignal;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public String getActiveProfile() {
        return this.activeProfile;
    }

    public void setActiveProfile(@Nullable String profile) {
        this.activeProfile = (profile == null || profile.isBlank()) ? "default" : profile;
    }

    public int setSensorEnabled(Class<?> sensorClass, boolean enabled) {
        if (sensorClass == null) {
            return 0;
        }
        int matched = 0;
        for (int i = 0, size = this.sensors.size(); i < size; i++) {
            if (sensorClass.isInstance(this.sensors.get(i))) {
                this.sensorEnabled[i] = enabled;
                matched++;
            }
        }
        return matched;
    }

    public boolean isSensorEnabled(Class<?> sensorClass) {
        if (sensorClass == null) {
            return false;
        }
        for (int i = 0, size = this.sensors.size(); i < size; i++) {
            if (sensorClass.isInstance(this.sensors.get(i))) {
                return this.sensorEnabled[i];
            }
        }
        return false;
    }

    public boolean isSensorEnabled(HiveSensor<?> sensor) {
        if (sensor == null) {
            return false;
        }
        for (int i = 0, size = this.sensors.size(); i < size; i++) {
            if (this.sensors.get(i) == sensor) {
                return this.sensorEnabled[i];
            }
        }
        return false;
    }

    public void setCooldown(String name, long currentGameTime, int ticks) {
        this.blackboard.setCooldown(name, currentGameTime, ticks);
    }

    public boolean isOnCooldown(String name, long currentGameTime) {
        return this.blackboard.isOnCooldown(name, currentGameTime);
    }

    public long getRemainingCooldown(String name, long currentGameTime) {
        return this.blackboard.getRemainingCooldown(name, currentGameTime);
    }

    public void clearCooldown(String name) {
        this.blackboard.clearCooldown(name);
    }

    public void saveToNbt(CompoundTag entityTag, long currentGameTime) {
        if (entityTag == null) {
            return;
        }
        CompoundTag stateTag = new CompoundTag();
        if (!"default".equals(this.activeProfile)) {
            stateTag.putString(TAG_PROFILE, this.activeProfile);
        }
        if (this.paused) {
            stateTag.putBoolean(TAG_PAUSED, true);
        }
        this.blackboard.saveToNbt(stateTag, currentGameTime);
        if (!stateTag.isEmpty()) {
            entityTag.put(NBT_BRAIN_STATE, stateTag);
        }
    }

    public void loadFromNbt(CompoundTag entityTag) {
        if (entityTag == null || !entityTag.contains(NBT_BRAIN_STATE, Tag.TAG_COMPOUND)) {
            this.activeProfile = "default";
            this.paused = false;
            this.blackboard.clear();
            return;
        }
        CompoundTag stateTag = entityTag.getCompound(NBT_BRAIN_STATE);
        setActiveProfile(stateTag.contains(TAG_PROFILE, Tag.TAG_STRING)
                ? stateTag.getString(TAG_PROFILE)
                : "default");
        setPaused(stateTag.getBoolean(TAG_PAUSED));
        this.blackboard.loadFromNbt(stateTag);
    }

    public HiveBrainSnapshot captureSnapshot(long gameTime) {
        String activeAct = this.activeActivity != null ? this.activeActivity.getName() : "none";
        String prevAct = this.previousActivity != null ? this.previousActivity.getName() : "none";
        String lastSig = this.lastSignal != null ? this.lastSignal.type().name() : "NONE";
        return new HiveBrainSnapshot(
                activeAct,
                prevAct,
                getTicksInCurrentActivity(gameTime),
                this.brainTickCount,
                this.skippedLodTickCount,
                this.signalCount,
                lastSig,
                this.activeProfile,
                describeRunningBehaviours(),
                this.blackboard.getActiveCooldownCount(gameTime),
                this.blackboard.activeEntryCount(gameTime),
                this.paused
        );
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
