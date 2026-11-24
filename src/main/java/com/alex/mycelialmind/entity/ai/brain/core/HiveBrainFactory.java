package com.alex.mycelialmind.entity.ai.brain.core;

import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Clean-room factory replacing SmartBrainProvider without Access Transformers.
 * Collects declared memories from sensors and behaviour trees, builds a vanilla
 * {@link Brain.Provider}, wires all {@link BrainActivityGroup}s, and returns a {@link HiveBrainRuntime}.
 */
public final class HiveBrainFactory {
    private static final Map<EntityType<?>, List<MemoryModuleType<?>>> MEMORY_CACHE = new ConcurrentHashMap<>();

    private HiveBrainFactory() {
    }

    public static <E extends LivingEntity & HiveBrainOwner<E>> Brain.Provider<E> createProvider(E owner) {
        List<MemoryModuleType<?>> memories = MEMORY_CACHE.computeIfAbsent(
                owner.getType(),
                ignored -> List.copyOf(collectDeclaredMemories(owner, compileTaskGroups(owner), owner.getSensors()))
        );
        return Brain.provider(memories, List.of());
    }

    @SuppressWarnings("unchecked")
    public static <E extends LivingEntity & HiveBrainOwner<E>> HiveBrainRuntime<E> initializeBrain(
            E owner, Brain<E> brain) {
        List<BrainActivityGroup<E>> groups = compileTaskGroups(owner);
        for (BrainActivityGroup<E> group : groups) {
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
        Activity defaultActivity = owner.getDefaultActivity();
        if (defaultActivity != null) {
            brain.setDefaultActivity(defaultActivity);
            brain.useDefaultActivity();
        }
        owner.handleAdditionalBrainSetup(brain);

        List<HiveSensor<? super E>> sensors = new ArrayList<>();
        for (HiveSensor<? extends E> sensor : owner.getSensors()) {
            if (sensor != null) {
                sensors.add((HiveSensor<? super E>) sensor);
            }
        }
        return new HiveBrainRuntime<>(sensors, owner.getActivityPriorities());
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
            List<BrainActivityGroup<E>> groups, BrainActivityGroup<E> group) {
        if (group != null) {
            groups.add(group);
        }
    }

    private static <E extends LivingEntity & HiveBrainOwner<E>> Set<MemoryModuleType<?>> collectDeclaredMemories(
            E owner,
            List<BrainActivityGroup<E>> groups,
            List<? extends HiveSensor<? extends E>> sensors) {
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
        for (BrainActivityGroup<E> group : groups) {
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
        return memories;
    }
}
