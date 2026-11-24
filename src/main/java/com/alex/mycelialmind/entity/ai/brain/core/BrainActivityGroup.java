package com.alex.mycelialmind.entity.ai.brain.core;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Declarative container pairing an {@link Activity} with ordered behaviours,
 * activity-level memory preconditions, and memories wiped upon activity exit.
 */
public final class BrainActivityGroup<T extends LivingEntity & HiveBrainOwner<T>> {
    private final Activity activity;
    private int priorityStart = 0;
    private final List<HiveBehaviour<? super T>> behaviours = new ArrayList<>();
    private final Set<Pair<MemoryModuleType<?>, MemoryStatus>> activityStartMemoryConditions = new LinkedHashSet<>();
    private final Set<MemoryModuleType<?>> wipedMemoriesOnFinish = new LinkedHashSet<>();

    public BrainActivityGroup(Activity activity) {
        this.activity = activity;
    }

    public BrainActivityGroup<T> priority(int priorityStart) {
        this.priorityStart = priorityStart;
        return this;
    }

    @SafeVarargs
    public final BrainActivityGroup<T> behaviours(HiveBehaviour<? super T>... behaviours) {
        for (HiveBehaviour<? super T> behaviour : behaviours) {
            if (behaviour != null) {
                this.behaviours.add(behaviour);
            }
        }
        return this;
    }

    public BrainActivityGroup<T> onlyStartWithMemoryStatus(MemoryModuleType<?> memory, MemoryStatus status) {
        this.activityStartMemoryConditions.add(Pair.of(memory, status));
        return this;
    }

    public BrainActivityGroup<T> wipeMemoriesWhenFinished(MemoryModuleType<?>... memories) {
        Collections.addAll(this.wipedMemoriesOnFinish, memories);
        return this;
    }

    public BrainActivityGroup<T> requireAndWipeMemoriesOnUse(MemoryModuleType<?>... memories) {
        for (MemoryModuleType<?> memory : memories) {
            this.activityStartMemoryConditions.add(Pair.of(memory, MemoryStatus.VALUE_PRESENT));
            this.wipedMemoriesOnFinish.add(memory);
        }
        return this;
    }

    public Activity getActivity() {
        return this.activity;
    }

    public int getPriorityStart() {
        return this.priorityStart;
    }

    public List<HiveBehaviour<? super T>> getBehaviours() {
        return Collections.unmodifiableList(this.behaviours);
    }

    public Set<Pair<MemoryModuleType<?>, MemoryStatus>> getActivityStartMemoryConditions() {
        return Collections.unmodifiableSet(this.activityStartMemoryConditions);
    }

    public Set<MemoryModuleType<?>> getWipedMemoriesOnFinish() {
        return Collections.unmodifiableSet(this.wipedMemoriesOnFinish);
    }

    public ImmutableList<Pair<Integer, BehaviorControl<? super T>>> pairBehaviourPriorities() {
        ImmutableList.Builder<Pair<Integer, BehaviorControl<? super T>>> builder = ImmutableList.builder();
        int priority = this.priorityStart;
        for (HiveBehaviour<? super T> behaviour : this.behaviours) {
            builder.add(Pair.of(priority++, behaviour));
        }
        return builder.build();
    }

    public static <T extends LivingEntity & HiveBrainOwner<T>> BrainActivityGroup<T> empty() {
        return new BrainActivityGroup<>(Activity.IDLE);
    }

    @SafeVarargs
    public static <T extends LivingEntity & HiveBrainOwner<T>> BrainActivityGroup<T> coreTasks(
            HiveBehaviour<? super T>... behaviours) {
        return new BrainActivityGroup<T>(Activity.CORE).behaviours(behaviours);
    }

    @SafeVarargs
    public static <T extends LivingEntity & HiveBrainOwner<T>> BrainActivityGroup<T> idleTasks(
            HiveBehaviour<? super T>... behaviours) {
        return new BrainActivityGroup<T>(Activity.IDLE).behaviours(behaviours);
    }

    @SafeVarargs
    public static <T extends LivingEntity & HiveBrainOwner<T>> BrainActivityGroup<T> fightTasks(
            HiveBehaviour<? super T>... behaviours) {
        return new BrainActivityGroup<T>(Activity.FIGHT)
                .behaviours(behaviours)
                .requireAndWipeMemoriesOnUse(MemoryModuleType.ATTACK_TARGET);
    }
}
