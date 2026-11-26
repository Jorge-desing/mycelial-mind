package com.alex.mycelialmind.entity.ai.brain.core;

import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.ExpirableValue;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clean-room brain factory operating without Access Transformers or Mixins.
 * Caches declared memories per entity {@link Class} and avoids duplicate compilation
 * between {@link #createProvider} and {@link #initializeBrain}.
 */
public final class HiveBrainFactory {
    private static final Map<Class<?>, List<MemoryModuleType<?>>> MEMORY_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<CompiledBrainSpec<?>> PENDING_SPEC = new ThreadLocal<>();

    private HiveBrainFactory() {
    }

    public static void clearMemoryCache() {
        MEMORY_CACHE.clear();
        PENDING_SPEC.remove();
    }

    public static int getCachedMemoryClassCount() {
        return MEMORY_CACHE.size();
    }

    public static PathNavigation createDefaultFlyingNavigation(Mob mob, Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(mob, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        nav.setCanPassDoors(true);
        return nav;
    }

    public static <E extends LivingEntity & HiveBrainOwner<E>> Brain.Provider<E> createProvider(E owner) {
        List<MemoryModuleType<?>> cached = MEMORY_CACHE.get(owner.getClass());
        if (cached != null) {
            return Brain.provider(cached, List.of());
        }

        List<BrainActivityGroup<E>> groups = compileTaskGroups(owner);
        List<? extends HiveSensor<? extends E>> sensors = owner.getSensors();
        List<MemoryModuleType<?>> memories = List.copyOf(collectDeclaredMemories(owner, groups, sensors));
        MEMORY_CACHE.putIfAbsent(owner.getClass(), memories);
        PENDING_SPEC.set(new CompiledBrainSpec<>(owner, groups, sensors));
        return Brain.provider(memories, List.of());
    }

    @SuppressWarnings("unchecked")
    public static <E extends LivingEntity & HiveBrainOwner<E>> HiveBrainRuntime<E> initializeBrain(
            E owner, Brain<E> brain) {
        List<BrainActivityGroup<E>> groups;
        List<? extends HiveSensor<? extends E>> rawSensors;
        try {
            CompiledBrainSpec<?> pending = PENDING_SPEC.get();
            if (pending != null && pending.matches(owner)) {
                groups = (List<BrainActivityGroup<E>>) (List<?>) pending.groups;
                rawSensors = (List<? extends HiveSensor<? extends E>>) (List<?>) pending.sensors;
            } else {
                groups = compileTaskGroups(owner);
                rawSensors = owner.getSensors();
            }
        } finally {
            PENDING_SPEC.remove();
        }

        applyGroupsToBrain(owner, brain, groups, owner.getDefaultActivity());

        List<HiveSensor<? super E>> sensors = new ArrayList<>(rawSensors != null ? rawSensors.size() : 0);
        if (rawSensors != null) {
            for (int i = 0, size = rawSensors.size(); i < size; i++) {
                HiveSensor<? extends E> sensor = rawSensors.get(i);
                if (sensor != null) {
                    sensors.add((HiveSensor<? super E>) sensor);
                }
            }
        }
        HiveBrainRuntime<E> runtime = new HiveBrainRuntime<>(
                brain,
                sensors,
                owner.getActivityPriorities(),
                groups,
                owner.getDefaultActivity()
        );
        HiveBrainSchedule schedule = owner.getBrainSchedule();
        if (schedule != null) {
            runtime.setSchedule(schedule);
        }
        Map<String, HiveBrainProfile<E>> profiles = owner.getBrainProfiles();
        if (profiles != null && !profiles.isEmpty()) {
            for (HiveBrainProfile<E> profile : profiles.values()) {
                runtime.registerProfile(profile);
            }
        }
        List<HiveBrainModule<E>> modules = owner.getBrainModules();
        if (modules != null && !modules.isEmpty()) {
            for (HiveBrainModule<E> module : modules) {
                runtime.addModule(module);
            }
        }
        owner.handleAdditionalBrainSetup(runtime);
        return runtime;
    }

    public static <E extends LivingEntity & HiveBrainOwner<E>> Brain<E> rebuildBrainForProfile(
            E owner, @Nullable Brain<E> oldBrain, List<BrainActivityGroup<E>> targetGroups) {
        Brain.Provider<E> provider;
        try {
            provider = createProvider(owner);
        } finally {
            PENDING_SPEC.remove();
        }
        Brain<E> newBrain = provider.makeBrain(new Dynamic<>(NbtOps.INSTANCE, new CompoundTag()));
        if (oldBrain != null) {
            copyPresentMemories(oldBrain, newBrain);
        }
        applyGroupsToBrain(owner, newBrain, targetGroups != null ? targetGroups : List.of(), owner.getDefaultActivity());
        return newBrain;
    }

    private static <E extends LivingEntity & HiveBrainOwner<E>> void applyGroupsToBrain(
            E owner,
            Brain<E> brain,
            List<BrainActivityGroup<E>> groups,
            @Nullable Activity defaultActivity) {
        for (int i = 0, size = groups.size(); i < size; i++) {
            BrainActivityGroup<E> group = groups.get(i);
            if (!group.getBehaviours().isEmpty()
                    || !group.getActivityStartMemoryConditions().isEmpty()
                    || !group.getWipedMemoriesOnFinish().isEmpty()) {
                brain.addActivityAndRemoveMemoriesWhenStopped(
                        group.getActivity(),
                        group.pairBehaviourPriorities(),
                        group.getActivityStartMemoryConditions(),
                        group.getWipedMemoriesOnFinish()
                );
            }
        }
        Set<Activity> alwaysRunning = owner.getAlwaysRunningActivities();
        if (!alwaysRunning.isEmpty()) {
            brain.setCoreActivities(alwaysRunning);
        }
        if (defaultActivity != null) {
            brain.setDefaultActivity(defaultActivity);
            brain.useDefaultActivity();
        }
        owner.handleAdditionalBrainSetup(brain);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <E extends LivingEntity> void copyPresentMemories(Brain<E> source, Brain<E> target) {
        for (Map.Entry<MemoryModuleType<?>, Optional<? extends ExpirableValue<?>>> entry : source.getMemories().entrySet()) {
            Optional<? extends ExpirableValue<?>> opt = entry.getValue();
            if (opt == null || opt.isEmpty()) {
                continue;
            }
            ExpirableValue<?> val = opt.get();
            MemoryModuleType type = entry.getKey();
            if (val.canExpire()) {
                target.setMemoryWithExpiry(type, val.getValue(), Math.max(1L, val.getTimeToLive()));
            } else {
                target.setMemory(type, val.getValue());
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends LivingEntity & HiveBrainOwner<E>> List<BrainActivityGroup<E>> compileTaskGroups(E owner) {
        List<BrainActivityGroup<E>> groups = new ArrayList<>(4);
        addIfNonNull(groups, (BrainActivityGroup<E>) owner.getCoreTasks());
        addIfNonNull(groups, (BrainActivityGroup<E>) owner.getIdleTasks());
        addIfNonNull(groups, (BrainActivityGroup<E>) owner.getFightTasks());
        Map<Activity, BrainActivityGroup<? extends E>> additional = owner.getAdditionalTasks();
        if (additional != null && !additional.isEmpty()) {
            for (BrainActivityGroup<? extends E> extra : additional.values()) {
                addIfNonNull(groups, (BrainActivityGroup<E>) extra);
            }
        }
        return groups;
    }

    private static <E extends LivingEntity & HiveBrainOwner<E>> void addIfNonNull(
            List<BrainActivityGroup<E>> groups, @Nullable BrainActivityGroup<E> group) {
        if (group != null) {
            groups.add(group);
        }
    }

    private static <E extends LivingEntity & HiveBrainOwner<E>> Set<MemoryModuleType<?>> collectDeclaredMemories(
            E owner,
            List<BrainActivityGroup<E>> groups,
            @Nullable List<? extends HiveSensor<? extends E>> sensors) {
        Set<MemoryModuleType<?>> memories = new LinkedHashSet<>();
        // Core vanilla navigation/combat/entity memories so standard behaviours & goals never drop state
        memories.add(MemoryModuleType.LOOK_TARGET);
        memories.add(MemoryModuleType.WALK_TARGET);
        memories.add(MemoryModuleType.PATH);
        memories.add(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        memories.add(MemoryModuleType.ATTACK_TARGET);
        memories.add(MemoryModuleType.ATTACK_COOLING_DOWN);
        memories.add(MemoryModuleType.HURT_BY);
        memories.add(MemoryModuleType.HURT_BY_ENTITY);
        memories.add(MemoryModuleType.NEAREST_LIVING_ENTITIES);
        memories.add(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        memories.add(MemoryModuleType.NEAREST_PLAYERS);
        memories.add(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        memories.add(MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER);

        if (sensors != null) {
            for (HiveSensor<? extends E> sensor : sensors) {
                if (sensor != null) {
                    memories.addAll(sensor.memoriesUsed());
                }
            }
        }
        collectMemoriesFromGroups(memories, groups);
        Map<String, HiveBrainProfile<E>> profiles = owner.getBrainProfiles();
        if (profiles != null && !profiles.isEmpty()) {
            for (HiveBrainProfile<E> profile : profiles.values()) {
                if (profile != null && profile.hasTaskGroupOverrides()) {
                    collectMemoriesFromGroups(memories, profile.taskGroups());
                }
            }
        }
        return memories;
    }

    private static <E extends LivingEntity & HiveBrainOwner<E>> void collectMemoriesFromGroups(
            Set<MemoryModuleType<?>> memories, @Nullable List<BrainActivityGroup<E>> groups) {
        if (groups == null) {
            return;
        }
        for (BrainActivityGroup<E> group : groups) {
            if (group == null) {
                continue;
            }
            for (HiveBehaviour<? super E> behaviour : group.getBehaviours()) {
                if (behaviour != null) {
                    memories.addAll(behaviour.collectMemoryTypes());
                }
            }
            for (Pair<MemoryModuleType<?>, MemoryStatus> condition : group.getActivityStartMemoryConditions()) {
                if (condition != null && condition.getFirst() != null) {
                    memories.add(condition.getFirst());
                }
            }
            memories.addAll(group.getWipedMemoriesOnFinish());
        }
    }

    private static final class CompiledBrainSpec<E extends LivingEntity & HiveBrainOwner<E>> {
        private final WeakReference<Object> ownerRef;
        private final List<BrainActivityGroup<E>> groups;
        @Nullable
        private final List<? extends HiveSensor<? extends E>> sensors;

        private CompiledBrainSpec(
                E owner,
                List<BrainActivityGroup<E>> groups,
                @Nullable List<? extends HiveSensor<? extends E>> sensors) {
            this.ownerRef = new WeakReference<>(owner);
            this.groups = groups;
            this.sensors = sensors;
        }

        private boolean matches(Object candidate) {
            return this.ownerRef.get() == candidate;
        }
    }
}
