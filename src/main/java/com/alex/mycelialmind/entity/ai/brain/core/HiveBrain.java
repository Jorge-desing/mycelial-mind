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
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * First-class clean-room AI brain coordinator wrapping a vanilla {@link Brain},
 * typed {@link HiveSensor}s, {@link HiveBrainSchedule}, {@link HiveBrainBlackboard},
 * executable {@link HiveBrainProfile}s, {@link HiveBrainModule}s, {@link HiveResourceArbiter},
 * reactive {@link HiveBrainSignal}s, {@link HiveBrainTrace} ring-buffer, NBT persistence,
 * and zero-allocation per-tick scheduling without Access Transformers or Mixins.
 */
public class HiveBrain<E extends LivingEntity & HiveBrainOwner<E>> {
    @FunctionalInterface
    public interface SignalListener<E extends LivingEntity & HiveBrainOwner<E>> {
        void handle(ServerLevel level, E entity, HiveBrain<E> brain, HiveBrainSignal signal);
    }

    public static final String NBT_BRAIN_STATE = "HiveBrainState";
    public static final int DEFAULT_SIGNAL_WAKE_TICKS = 40;
    private static final int MAX_QUEUED_SIGNALS = 16;
    private static final String TAG_PROFILE = "Profile";
    private static final String TAG_PAUSED = "Paused";

    @Nullable
    private Brain<E> vanillaBrain;
    private final List<HiveSensor<? super E>> sensors;
    private final boolean[] sensorEnabled;
    private final List<Activity> defaultActivityPriorities;
    private List<Activity> activeActivityPriorities;
    private final List<BrainActivityGroup<E>> defaultTaskGroups;
    private List<BrainActivityGroup<E>> activeTaskGroups;
    private final List<Activity> priorityResolutionBuffer = new ArrayList<>(8);
    private final HiveBrainBlackboard blackboard = new HiveBrainBlackboard();
    private final HiveResourceArbiter resourceArbiter = new HiveResourceArbiter();
    private final HiveBrainTrace trace = new HiveBrainTrace();
    private final ArrayDeque<HiveBrainSignal> pendingSignals = new ArrayDeque<>(4);
    private final Map<String, HiveBrainProfile<E>> profiles = new LinkedHashMap<>();
    private final List<HiveBrainModule<E>> modules = new ArrayList<>();
    private final EnumMap<HiveBrainSignal.SignalType, List<SignalListener<E>>> signalListeners =
            new EnumMap<>(HiveBrainSignal.SignalType.class);

    @Nullable
    private HiveBrainSchedule defaultSchedule;
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
    private int lodIntervalMultiplier = 1;
    private boolean hasActiveTaskGroupOverride = false;
    private boolean pendingProfileSync = false;
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
        this.defaultActivityPriorities = List.copyOf(activityPriorities);
        this.activeActivityPriorities = this.defaultActivityPriorities;
        this.defaultTaskGroups = List.copyOf(taskGroups);
        this.activeTaskGroups = this.defaultTaskGroups;
        this.defaultActivity = defaultActivity;
        this.activeActivity = defaultActivity;
    }

    public HiveBrainBlackboard getBlackboard() {
        return this.blackboard;
    }

    public HiveResourceArbiter getResourceArbiter() {
        return this.resourceArbiter;
    }

    public HiveBrainTrace getTrace() {
        return this.trace;
    }

    public int getLodIntervalMultiplier() {
        return this.lodIntervalMultiplier;
    }

    @Nullable
    public Brain<E> getVanillaBrain() {
        return this.vanillaBrain;
    }

    public List<HiveSensor<? super E>> getSensors() {
        return Collections.unmodifiableList(this.sensors);
    }

    public List<Activity> getActivityPriorities() {
        return Collections.unmodifiableList(this.activeActivityPriorities);
    }

    public List<BrainActivityGroup<E>> getTaskGroups() {
        return Collections.unmodifiableList(this.activeTaskGroups);
    }

    @Nullable
    public HiveBrainSchedule getSchedule() {
        return this.schedule;
    }

    public void setSchedule(@Nullable HiveBrainSchedule schedule) {
        this.defaultSchedule = schedule;
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
        if (this.pendingProfileSync) {
            this.pendingProfileSync = false;
            if ("default".equals(this.activeProfile) || this.profiles.containsKey(this.activeProfile)) {
                String target = this.activeProfile;
                this.activeProfile = "__pending__";
                applyProfile(level, entity, target);
            } else {
                this.activeProfile = "default";
            }
        }
        if (this.paused) {
            return;
        }
        if (!entity.shouldTickBrain(level, entity)) {
            this.skippedLodTickCount++;
            return;
        }

        long gameTime = level.getGameTime();
        entity.onBrainTickStart(level, entity, this);
        this.brainTickCount++;
        drainPendingSignals(level, entity);
        if ((this.brainTickCount % 40L) == 0L) {
            this.blackboard.pruneExpired(gameTime);
        }

        for (int i = 0, size = this.sensors.size(); i < size; i++) {
            if (this.sensorEnabled[i]) {
                this.sensors.get(i).tick(level, entity);
            }
        }

        for (int i = 0, size = this.modules.size(); i < size; i++) {
            HiveBrainModule<E> module = this.modules.get(i);
            if (module.isEnabled()) {
                module.tick(level, entity, this, gameTime);
            }
        }

        Brain<E> activeBrain = this.vanillaBrain != null ? this.vanillaBrain : brain;
        Activity scheduledActivity = null;
        if (this.schedule != null) {
            this.schedule.runCallbacks(level, entity);
            scheduledActivity = this.schedule.resolveActivity(level, entity);
        }

        activeBrain.tick(level, entity);

        if (scheduledActivity != null) {
            resolvePriorityBuffer(scheduledActivity);
            activeBrain.setActiveActivityToFirstValid(this.priorityResolutionBuffer);
        } else if (!this.activeActivityPriorities.isEmpty()) {
            activeBrain.setActiveActivityToFirstValid(this.activeActivityPriorities);
        }

        Activity current = resolveCurrentActiveActivity(activeBrain, scheduledActivity);
        if (current != null && current != this.activeActivity) {
            Activity prev = this.activeActivity;
            this.previousActivity = prev;
            this.activeActivity = current;
            this.lastActivityChangeGameTime = gameTime;
            String prevName = prev != null ? prev.getName() : "none";
            this.trace.record(gameTime, HiveBrainTrace.EventKind.ACTIVITY, prevName + " -> " + current.getName());
            entity.onActivityChanged(level, entity, prev, current);
        }

        entity.onBrainTickEnd(level, entity, this);
    }

    private void resolvePriorityBuffer(Activity scheduledActivity) {
        this.priorityResolutionBuffer.clear();
        if (this.prioritizeScheduleAfterFight) {
            for (int i = 0, size = this.activeActivityPriorities.size(); i < size; i++) {
                Activity candidate = this.activeActivityPriorities.get(i);
                if (candidate == Activity.FIGHT || candidate == Activity.PANIC) {
                    this.priorityResolutionBuffer.add(candidate);
                }
            }
            if (!this.priorityResolutionBuffer.contains(scheduledActivity)) {
                this.priorityResolutionBuffer.add(scheduledActivity);
            }
            for (int i = 0, size = this.activeActivityPriorities.size(); i < size; i++) {
                Activity candidate = this.activeActivityPriorities.get(i);
                if (!this.priorityResolutionBuffer.contains(candidate)) {
                    this.priorityResolutionBuffer.add(candidate);
                }
            }
        } else {
            this.priorityResolutionBuffer.add(scheduledActivity);
            for (int i = 0, size = this.activeActivityPriorities.size(); i < size; i++) {
                Activity candidate = this.activeActivityPriorities.get(i);
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
        for (int i = 0, size = this.activeActivityPriorities.size(); i < size; i++) {
            Activity candidate = this.activeActivityPriorities.get(i);
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
        for (int i = 0, size = this.activeTaskGroups.size(); i < size; i++) {
            Activity candidate = this.activeTaskGroups.get(i).getActivity();
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

    public void registerProfile(HiveBrainProfile<E> profile) {
        if (profile != null) {
            this.profiles.put(profile.name(), profile);
        }
    }

    public Set<String> getRegisteredProfileNames() {
        return Collections.unmodifiableSet(this.profiles.keySet());
    }

    @Nullable
    public HiveBrainProfile<E> getProfile(String name) {
        return name != null ? this.profiles.get(name) : null;
    }

    public boolean applyProfile(ServerLevel level, E entity, @Nullable String profileName) {
        String targetName = (profileName == null || profileName.isBlank()) ? "default" : profileName;
        if (!"default".equals(targetName) && !this.profiles.containsKey(targetName)) {
            return false;
        }
        if (targetName.equals(this.activeProfile) && !this.pendingProfileSync) {
            return false;
        }

        String previous = "__pending__".equals(this.activeProfile) ? "default" : this.activeProfile;
        HiveBrainProfile<E> prevProfile = this.profiles.get(previous);
        HiveBrainProfile<E> nextProfile = "default".equals(targetName) ? null : this.profiles.get(targetName);

        this.activeProfile = targetName;
        this.pendingProfileSync = false;

        if (prevProfile != null && prevProfile.onExit() != null && level != null && entity != null) {
            prevProfile.onExit().accept(level, entity);
        }

        boolean nextHasTaskOverrides = nextProfile != null && nextProfile.hasTaskGroupOverrides();
        if ((nextHasTaskOverrides || this.hasActiveTaskGroupOverride)
                && this.vanillaBrain != null && level != null && entity != null) {
            List<BrainActivityGroup<E>> targetGroups = nextHasTaskOverrides
                    ? nextProfile.taskGroups()
                    : this.defaultTaskGroups;
            this.vanillaBrain.stopAll(level, entity);
            this.resourceArbiter.releaseAll();
            Brain<E> rebuilt = HiveBrainFactory.rebuildBrainForProfile(entity, this.vanillaBrain, targetGroups);
            if (entity.replaceVanillaBrain(rebuilt)) {
                this.vanillaBrain = rebuilt;
                this.activeTaskGroups = targetGroups;
                this.hasActiveTaskGroupOverride = nextHasTaskOverrides;
            }
        }

        this.activeActivityPriorities = (nextProfile != null && nextProfile.activityPriorities() != null)
                ? nextProfile.activityPriorities()
                : this.defaultActivityPriorities;
        this.schedule = (nextProfile != null && nextProfile.schedule() != null)
                ? nextProfile.schedule()
                : this.defaultSchedule;

        Arrays.fill(this.sensorEnabled, true);
        if (nextProfile != null) {
            for (Class<?> disabled : nextProfile.disabledSensors()) {
                setSensorEnabled(disabled, false);
            }
            for (Class<?> enabled : nextProfile.enabledSensors()) {
                setSensorEnabled(enabled, true);
            }
        }

        this.lodIntervalMultiplier = nextProfile != null ? nextProfile.lodIntervalMultiplier() : 1;
        long gameTime = level != null ? level.getGameTime() : 0L;
        if (level != null) {
            wakeLodForTicks(gameTime, 20);
        }
        this.trace.record(gameTime, HiveBrainTrace.EventKind.PROFILE, previous + " -> " + targetName);

        if (nextProfile != null && nextProfile.onEnter() != null && level != null && entity != null) {
            nextProfile.onEnter().accept(level, entity);
        }
        if (level != null && entity != null) {
            for (int i = 0, size = this.modules.size(); i < size; i++) {
                this.modules.get(i).onProfileChanged(level, entity, this, previous, targetName);
            }
            entity.onProfileChanged(level, entity, previous, targetName);
        }
        return true;
    }

    public void addModule(HiveBrainModule<E> module) {
        if (module == null) {
            return;
        }
        removeModule(module.id());
        this.modules.add(module);
    }

    public boolean removeModule(@Nullable String moduleId) {
        if (moduleId == null || moduleId.isBlank()) {
            return false;
        }
        for (int i = 0; i < this.modules.size(); i++) {
            if (moduleId.equals(this.modules.get(i).id())) {
                this.modules.remove(i);
                return true;
            }
        }
        return false;
    }

    public List<HiveBrainModule<E>> getModules() {
        return Collections.unmodifiableList(this.modules);
    }

    @Nullable
    public HiveBrainModule<E> getModule(@Nullable String moduleId) {
        if (moduleId == null) {
            return null;
        }
        for (int i = 0, size = this.modules.size(); i < size; i++) {
            HiveBrainModule<E> module = this.modules.get(i);
            if (moduleId.equals(module.id())) {
                return module;
            }
        }
        return null;
    }

    public void onSignal(HiveBrainSignal.SignalType type, SignalListener<E> listener) {
        if (type != null && listener != null) {
            this.signalListeners.computeIfAbsent(type, unused -> new ArrayList<>(2)).add(listener);
        }
    }

    public void clearSignalListeners(HiveBrainSignal.SignalType type) {
        if (type != null) {
            this.signalListeners.remove(type);
        }
    }

    private void deliverSignal(ServerLevel level, E entity, HiveBrainSignal signal) {
        long gameTime = level.getGameTime();
        this.signalCount++;
        this.lastSignal = signal;
        this.trace.record(
                gameTime,
                HiveBrainTrace.EventKind.SIGNAL,
                signal.type().name() + (signal.tag().isEmpty() ? "" : ":" + signal.tag()));
        wakeLodForTicks(gameTime, DEFAULT_SIGNAL_WAKE_TICKS);
        interruptMatchingBehaviours(level, entity, signal, gameTime);
        if (signal.urgent() && !this.paused && !this.dispatchingUrgentSensors) {
            this.dispatchingUrgentSensors = true;
            try {
                triggerSensorsNow(level, entity);
            } finally {
                this.dispatchingUrgentSensors = false;
            }
        }
        List<SignalListener<E>> listeners = this.signalListeners.get(signal.type());
        if (listeners != null && !listeners.isEmpty()) {
            for (int i = 0, size = listeners.size(); i < size; i++) {
                listeners.get(i).handle(level, entity, this, signal);
            }
        }
        for (int i = 0, size = this.modules.size(); i < size; i++) {
            HiveBrainModule<E> module = this.modules.get(i);
            if (module.isEnabled()) {
                module.onSignal(level, entity, this, signal);
            }
        }
        entity.onBrainSignal(level, entity, this, signal);
    }

    private void interruptMatchingBehaviours(
            ServerLevel level, E entity, HiveBrainSignal signal, long gameTime) {
        for (int i = 0, gSize = this.activeTaskGroups.size(); i < gSize; i++) {
            List<HiveBehaviour<? super E>> behaviours = this.activeTaskGroups.get(i).getBehaviours();
            for (int j = 0, bSize = behaviours.size(); j < bSize; j++) {
                interruptBehaviourTree(behaviours.get(j), level, entity, signal, gameTime);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void interruptBehaviourTree(
            HiveBehaviour<?> behaviour,
            ServerLevel level,
            E entity,
            HiveBrainSignal signal,
            long gameTime) {
        if (behaviour.getStatus() == Behavior.Status.RUNNING && behaviour.isInterruptedBy(signal.type())) {
            HiveBehaviour<E> typed = (HiveBehaviour<E>) behaviour;
            typed.doStop(level, entity, gameTime);
            typed.notifySignalInterrupt(entity);
            this.trace.record(
                    gameTime,
                    HiveBrainTrace.EventKind.INTERRUPT,
                    signal.type().name() + " -> " + behaviour.debugString());
            return;
        }
        if (behaviour instanceof GroupBehaviour<?> group) {
            List<? extends HiveBehaviour<?>> children = group.getChildBehaviours();
            for (int i = 0, size = children.size(); i < size; i++) {
                interruptBehaviourTree(children.get(i), level, entity, signal, gameTime);
            }
        }
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
        setPaused(paused, this.brainTickCount);
    }

    public void setPaused(boolean paused, long gameTime) {
        if (this.paused != paused) {
            this.paused = paused;
            this.trace.record(gameTime, HiveBrainTrace.EventKind.PAUSE, paused ? "PAUSED" : "RESUMED");
        }
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
            this.pendingProfileSync = false;
            this.paused = false;
            this.blackboard.clear();
            return;
        }
        CompoundTag stateTag = entityTag.getCompound(NBT_BRAIN_STATE);
        String savedProfile = stateTag.contains(TAG_PROFILE, Tag.TAG_STRING)
                ? stateTag.getString(TAG_PROFILE)
                : "default";
        setActiveProfile(savedProfile);
        this.pendingProfileSync = !"default".equals(this.activeProfile);
        this.paused = stateTag.getBoolean(TAG_PAUSED);
        this.blackboard.loadFromNbt(stateTag);
    }

    public HiveBrainSnapshot captureSnapshot(long gameTime) {
        String activeAct = this.activeActivity != null ? this.activeActivity.getName() : "none";
        String prevAct = this.previousActivity != null ? this.previousActivity.getName() : "none";
        String lastSig = this.lastSignal != null ? this.lastSignal.type().name() : "NONE";
        HiveBrainTrace.Entry latestTrace = this.trace.latest();
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
                this.paused,
                this.modules.size(),
                this.resourceArbiter.formatOccupied(),
                latestTrace != null ? latestTrace.format() : "-"
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
        for (int i = 0, gSize = this.activeTaskGroups.size(); i < gSize; i++) {
            List<HiveBehaviour<? super E>> behaviours = this.activeTaskGroups.get(i).getBehaviours();
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
        for (int i = 0, gSize = this.activeTaskGroups.size(); i < gSize; i++) {
            BrainActivityGroup<E> group = this.activeTaskGroups.get(i);
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
