package com.alex.mycelialmind.entity.ai.brain.sensor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiPredicate;

/**
 * Scans nearby {@link LivingEntity} instances within a configurable box radius,
 * sorting by distance and populating {@code NEAREST_LIVING_ENTITIES} and {@code NEAREST_VISIBLE_LIVING_ENTITIES}.
 */
public class NearbyLivingEntitySensor<E extends LivingEntity> extends PredicateSensor<LivingEntity, E> {
    private static final List<MemoryModuleType<?>> MEMORIES = List.of(
            MemoryModuleType.NEAREST_LIVING_ENTITIES,
            MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES
    );

    protected double radiusXZ = 16.0D;
    protected double radiusY = 16.0D;

    public NearbyLivingEntitySensor() {
        super((target, entity) -> true);
    }

    public NearbyLivingEntitySensor<E> setRadius(double radius) {
        return setRadius(radius, radius);
    }

    public NearbyLivingEntitySensor<E> setRadius(double radiusXZ, double radiusY) {
        this.radiusXZ = Math.max(1.0D, radiusXZ);
        this.radiusY = Math.max(1.0D, radiusY);
        return this;
    }

    @Override
    public NearbyLivingEntitySensor<E> setPredicate(BiPredicate<LivingEntity, E> predicate) {
        super.setPredicate(predicate);
        return this;
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return MEMORIES;
    }

    @Override
    public SensorType<? extends Sensor<?>> type() {
        return SensorType.NEAREST_LIVING_ENTITIES;
    }

    @Override
    protected void scan(ServerLevel level, E entity) {
        List<LivingEntity> entities = level.getEntitiesOfClass(
                LivingEntity.class,
                entity.getBoundingBox().inflate(this.radiusXZ, this.radiusY, this.radiusXZ),
                candidate -> candidate != entity && candidate.isAlive() && predicate().test(candidate, entity)
        );
        entities.sort(Comparator.comparingDouble(entity::distanceToSqr));

        Brain<?> brain = entity.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, entities);
        brain.setMemory(
                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                new NearestVisibleLivingEntities(entity, entities)
        );
    }
}
