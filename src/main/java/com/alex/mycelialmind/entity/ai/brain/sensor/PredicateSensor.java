package com.alex.mycelialmind.entity.ai.brain.sensor;

import net.minecraft.world.entity.LivingEntity;

import java.util.function.BiPredicate;

/**
 * {@link HiveSensor} specialization exposing a configurable predicate filter for sensed targets or events.
 */
public abstract class PredicateSensor<P, E extends LivingEntity> extends HiveSensor<E> {
    private BiPredicate<P, E> predicate;

    protected PredicateSensor() {
        this((candidate, entity) -> true);
    }

    protected PredicateSensor(BiPredicate<P, E> predicate) {
        this.predicate = predicate != null ? predicate : (candidate, entity) -> true;
    }

    public PredicateSensor<P, E> setPredicate(BiPredicate<P, E> predicate) {
        this.predicate = predicate != null ? predicate : (candidate, entity) -> true;
        return this;
    }

    protected BiPredicate<P, E> predicate() {
        return this.predicate;
    }
}
