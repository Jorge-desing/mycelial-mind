package com.alex.mycelialmind.entity.ai.brain.sensor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;

import java.util.List;

/**
 * Records recent {@link DamageSource} and attacking {@link LivingEntity} into {@code HURT_BY} / {@code HURT_BY_ENTITY},
 * clearing expired or unreachable attackers.
 */
public class HurtBySensor<E extends Mob> extends PredicateSensor<DamageSource, E> {
    private static final List<MemoryModuleType<?>> MEMORIES = List.of(
            MemoryModuleType.HURT_BY,
            MemoryModuleType.HURT_BY_ENTITY
    );

    public HurtBySensor() {
        super((source, mob) -> true);
    }

    @Override
    public List<MemoryModuleType<?>> memoriesUsed() {
        return MEMORIES;
    }

    @Override
    public SensorType<? extends Sensor<?>> type() {
        return SensorType.HURT_BY;
    }

    @Override
    protected void scan(ServerLevel level, E entity) {
        Brain<?> brain = entity.getBrain();
        DamageSource damageSource = entity.getLastDamageSource();
        if (damageSource != null && predicate().test(damageSource, entity)) {
            brain.setMemory(MemoryModuleType.HURT_BY, damageSource);
            if (damageSource.getEntity() instanceof LivingEntity attacker
                    && attacker.isAlive()
                    && attacker.level() == entity.level()) {
                brain.setMemory(MemoryModuleType.HURT_BY_ENTITY, attacker);
            }
        } else {
            brain.eraseMemory(MemoryModuleType.HURT_BY);
        }

        brain.getMemory(MemoryModuleType.HURT_BY_ENTITY).ifPresent(attacker -> {
            double maxDist = Math.max(16.0D, entity.getAttributeValue(Attributes.FOLLOW_RANGE));
            if (!attacker.isAlive()
                    || attacker.level() != entity.level()
                    || entity.distanceToSqr(attacker) > maxDist * maxDist) {
                brain.eraseMemory(MemoryModuleType.HURT_BY_ENTITY);
            }
        });
    }
}
