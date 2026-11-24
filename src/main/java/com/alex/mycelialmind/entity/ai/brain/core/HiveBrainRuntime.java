package com.alex.mycelialmind.entity.ai.brain.core;

import com.alex.mycelialmind.entity.ai.brain.sensor.HiveSensor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.schedule.Activity;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Per-entity runtime coordinator extending {@link HiveBrain} for full backwards compatibility
 * while sharing its unified scheduling, activity tracking, and typed memory APIs.
 */
public final class HiveBrainRuntime<E extends LivingEntity & HiveBrainOwner<E>> extends HiveBrain<E> {
    public HiveBrainRuntime(List<HiveSensor<? super E>> sensors, List<Activity> activityPriorities) {
        super(sensors, activityPriorities);
    }

    public HiveBrainRuntime(
            Brain<E> vanillaBrain,
            List<HiveSensor<? super E>> sensors,
            List<Activity> activityPriorities,
            List<BrainActivityGroup<E>> taskGroups,
            @Nullable Activity defaultActivity) {
        super(vanillaBrain, sensors, activityPriorities, taskGroups, defaultActivity);
    }

    @Override
    public void tick(ServerLevel level, E entity, Brain<E> brain) {
        super.tick(level, entity, brain);
    }
}
