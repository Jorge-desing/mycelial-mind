package com.alex.mycelialmind.entity.ai.brain.core;

import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Per-entity runtime coordinator driving {@link HiveSensor} ticks, vanilla {@link Brain} updates,
 * and activity priority transitions cleanly via public NeoForge/Minecraft APIs.
 */
public final class HiveBrainRuntime<E extends LivingEntity & HiveBrainOwner<E>> {
    private final List<HiveSensor<? super E>> sensors;
    private final List<Activity> activityPriorities;

    public HiveBrainRuntime(List<HiveSensor<? super E>> sensors, List<Activity> activityPriorities) {
        this.sensors = List.copyOf(sensors);
        this.activityPriorities = List.copyOf(activityPriorities);
    }

    public List<HiveSensor<? super E>> getSensors() {
        return Collections.unmodifiableList(this.sensors);
    }

    public List<Activity> getActivityPriorities() {
        return Collections.unmodifiableList(this.activityPriorities);
    }

    public void tick(ServerLevel level, E entity, Brain<E> brain) {
        for (HiveSensor<? super E> sensor : this.sensors) {
            sensor.tick(level, entity);
        }
        brain.tick(level, entity);
        if (!this.activityPriorities.isEmpty()) {
            brain.setActiveActivityToFirstValid(new ArrayList<>(this.activityPriorities));
        }
    }
}
