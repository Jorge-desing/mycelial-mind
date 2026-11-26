package com.alex.mycelialmind.entity.ai.brain.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Executable behavioural profile for {@link HiveBrain} capable of overriding activity priorities,
 * schedule rules, enabled sensors, LOD cadence multiplier, and even swapping vanilla {@code Brain}
 * task groups cleanly at runtime.
 */
public final class HiveBrainProfile<E extends LivingEntity & HiveBrainOwner<E>> {
    private final String name;
    @Nullable
    private final List<Activity> activityPriorities;
    @Nullable
    private final HiveBrainSchedule schedule;
    private final Set<Class<?>> disabledSensors;
    private final Set<Class<?>> enabledSensors;
    private final int lodIntervalMultiplier;
    @Nullable
    private final List<BrainActivityGroup<E>> taskGroups;
    @Nullable
    private final BiConsumer<ServerLevel, E> onEnter;
    @Nullable
    private final BiConsumer<ServerLevel, E> onExit;

    private HiveBrainProfile(Builder<E> builder) {
        this.name = builder.name;
        this.activityPriorities = builder.activityPriorities != null
                ? List.copyOf(builder.activityPriorities)
                : null;
        this.schedule = builder.schedule;
        this.disabledSensors = Collections.unmodifiableSet(new LinkedHashSet<>(builder.disabledSensors));
        this.enabledSensors = Collections.unmodifiableSet(new LinkedHashSet<>(builder.enabledSensors));
        this.lodIntervalMultiplier = Math.max(0, builder.lodIntervalMultiplier);
        this.taskGroups = (builder.taskGroups != null && !builder.taskGroups.isEmpty())
                ? List.copyOf(builder.taskGroups)
                : null;
        this.onEnter = builder.onEnter;
        this.onExit = builder.onExit;
    }

    public static <E extends LivingEntity & HiveBrainOwner<E>> Builder<E> builder(String name) {
        return new Builder<>(name);
    }

    public String name() {
        return this.name;
    }

    @Nullable
    public List<Activity> activityPriorities() {
        return this.activityPriorities;
    }

    @Nullable
    public HiveBrainSchedule schedule() {
        return this.schedule;
    }

    public Set<Class<?>> disabledSensors() {
        return this.disabledSensors;
    }

    public Set<Class<?>> enabledSensors() {
        return this.enabledSensors;
    }

    public int lodIntervalMultiplier() {
        return this.lodIntervalMultiplier;
    }

    @Nullable
    public List<BrainActivityGroup<E>> taskGroups() {
        return this.taskGroups;
    }

    public boolean hasTaskGroupOverrides() {
        return this.taskGroups != null && !this.taskGroups.isEmpty();
    }

    @Nullable
    public BiConsumer<ServerLevel, E> onEnter() {
        return this.onEnter;
    }

    @Nullable
    public BiConsumer<ServerLevel, E> onExit() {
        return this.onExit;
    }

    public static final class Builder<E extends LivingEntity & HiveBrainOwner<E>> {
        private final String name;
        @Nullable
        private List<Activity> activityPriorities;
        @Nullable
        private HiveBrainSchedule schedule;
        private final Set<Class<?>> disabledSensors = new LinkedHashSet<>();
        private final Set<Class<?>> enabledSensors = new LinkedHashSet<>();
        private int lodIntervalMultiplier = 1;
        @Nullable
        private List<BrainActivityGroup<E>> taskGroups;
        @Nullable
        private BiConsumer<ServerLevel, E> onEnter;
        @Nullable
        private BiConsumer<ServerLevel, E> onExit;

        private Builder(String name) {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("HiveBrainProfile name must not be blank");
            }
            this.name = name;
        }

        public Builder<E> activityPriorities(@Nullable List<Activity> activityPriorities) {
            this.activityPriorities = activityPriorities;
            return this;
        }

        public Builder<E> schedule(@Nullable HiveBrainSchedule schedule) {
            this.schedule = schedule;
            return this;
        }

        public Builder<E> disableSensors(Class<?>... sensorClasses) {
            if (sensorClasses != null) {
                for (Class<?> cls : sensorClasses) {
                    if (cls != null) {
                        this.disabledSensors.add(cls);
                    }
                }
            }
            return this;
        }

        public Builder<E> enableSensors(Class<?>... sensorClasses) {
            if (sensorClasses != null) {
                for (Class<?> cls : sensorClasses) {
                    if (cls != null) {
                        this.enabledSensors.add(cls);
                    }
                }
            }
            return this;
        }

        public Builder<E> lodIntervalMultiplier(int multiplier) {
            this.lodIntervalMultiplier = Math.max(0, multiplier);
            return this;
        }

        @SafeVarargs
        public final Builder<E> taskGroups(BrainActivityGroup<E>... groups) {
            if (groups != null && groups.length > 0) {
                List<BrainActivityGroup<E>> list = new ArrayList<>(groups.length);
                for (BrainActivityGroup<E> group : groups) {
                    if (group != null) {
                        list.add(group);
                    }
                }
                this.taskGroups = list;
            }
            return this;
        }

        public Builder<E> taskGroups(@Nullable List<BrainActivityGroup<E>> groups) {
            this.taskGroups = (groups != null && !groups.isEmpty()) ? List.copyOf(groups) : null;
            return this;
        }

        public Builder<E> onEnter(@Nullable BiConsumer<ServerLevel, E> onEnter) {
            this.onEnter = onEnter;
            return this;
        }

        public Builder<E> onExit(@Nullable BiConsumer<ServerLevel, E> onExit) {
            this.onExit = onExit;
            return this;
        }

        public HiveBrainProfile<E> build() {
            Objects.requireNonNull(this.name, "name");
            return new HiveBrainProfile<>(this);
        }
    }
}
