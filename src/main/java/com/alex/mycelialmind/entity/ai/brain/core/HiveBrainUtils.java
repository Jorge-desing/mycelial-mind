package com.alex.mycelialmind.entity.ai.brain.core;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Null-safe utilities for reading and mutating vanilla {@link Brain} memories and combat targets.
 */
public final class HiveBrainUtils {
    private HiveBrainUtils() {
    }

    public static <T> T memoryOrDefault(LivingEntity entity, MemoryModuleType<T> type, Supplier<T> fallback) {
        return memoryOrDefault(entity.getBrain(), type, fallback);
    }

    public static <T> T memoryOrDefault(Brain<?> brain, MemoryModuleType<T> type, Supplier<T> fallback) {
        Optional<T> value = brain.getMemory(type);
        return value.isPresent() ? value.get() : fallback.get();
    }

    public static <T> T computeMemoryIfAbsent(LivingEntity entity, MemoryModuleType<T> type, Supplier<T> supplier) {
        return computeMemoryIfAbsent(entity.getBrain(), type, supplier);
    }

    public static <T> T computeMemoryIfAbsent(Brain<?> brain, MemoryModuleType<T> type, Supplier<T> supplier) {
        Optional<T> existing = brain.getMemory(type);
        if (existing.isPresent()) {
            return existing.get();
        }
        T computed = supplier.get();
        brain.setMemory(type, computed);
        return computed;
    }

    @Nullable
    public static <T> T getMemory(LivingEntity entity, MemoryModuleType<T> type) {
        return getMemory(entity.getBrain(), type);
    }

    @Nullable
    public static <T> T getMemory(Brain<?> brain, MemoryModuleType<T> type) {
        return brain.getMemory(type).orElse(null);
    }

    public static <T> void withMemory(LivingEntity entity, MemoryModuleType<T> type, Consumer<T> consumer) {
        withMemory(entity.getBrain(), type, consumer);
    }

    public static <T> void withMemory(Brain<?> brain, MemoryModuleType<T> type, Consumer<T> consumer) {
        brain.getMemory(type).ifPresent(consumer);
    }

    public static boolean hasMemory(LivingEntity entity, MemoryModuleType<?> type) {
        return hasMemory(entity.getBrain(), type);
    }

    public static boolean hasMemory(Brain<?> brain, MemoryModuleType<?> type) {
        return brain.hasMemoryValue(type);
    }

    public static long getTimeUntilMemoryExpires(LivingEntity entity, MemoryModuleType<?> type) {
        return getTimeUntilMemoryExpires(entity.getBrain(), type);
    }

    public static long getTimeUntilMemoryExpires(Brain<?> brain, MemoryModuleType<?> type) {
        return brain.getTimeUntilExpiry(type);
    }

    public static <T> void setMemory(LivingEntity entity, MemoryModuleType<T> type, @Nullable T value) {
        setMemory(entity.getBrain(), type, value);
    }

    public static <T> void setMemory(Brain<?> brain, MemoryModuleType<T> type, @Nullable T value) {
        brain.setMemory(type, value);
    }

    public static <T> void setForgettableMemory(
            LivingEntity entity, MemoryModuleType<T> type, T value, int expiryTicks) {
        setForgettableMemory(entity.getBrain(), type, value, expiryTicks);
    }

    public static <T> void setForgettableMemory(
            Brain<?> brain, MemoryModuleType<T> type, T value, int expiryTicks) {
        brain.setMemoryWithExpiry(type, value, Math.max(1, expiryTicks));
    }

    public static void clearMemory(LivingEntity entity, MemoryModuleType<?> type) {
        clearMemory(entity.getBrain(), type);
    }

    public static void clearMemory(Brain<?> brain, MemoryModuleType<?> type) {
        brain.eraseMemory(type);
    }

    public static void clearMemories(LivingEntity entity, MemoryModuleType<?>... types) {
        clearMemories(entity.getBrain(), types);
    }

    public static void clearMemories(Brain<?> brain, MemoryModuleType<?>... types) {
        for (MemoryModuleType<?> type : types) {
            brain.eraseMemory(type);
        }
    }

    @Nullable
    public static LivingEntity getTargetOfEntity(LivingEntity entity) {
        return getTargetOfEntity(entity, null);
    }

    @Nullable
    public static LivingEntity getTargetOfEntity(LivingEntity entity, @Nullable LivingEntity fallback) {
        LivingEntity brainTarget = getMemory(entity, MemoryModuleType.ATTACK_TARGET);
        if (brainTarget != null) {
            return brainTarget;
        }
        return fallback;
    }

    public static void setTargetOfEntity(LivingEntity entity, @Nullable LivingEntity target) {
        if (target == null) {
            clearMemory(entity, MemoryModuleType.ATTACK_TARGET);
        } else {
            setMemory(entity, MemoryModuleType.ATTACK_TARGET, target);
        }
    }

    @Nullable
    public static LivingEntity getLastAttacker(LivingEntity entity) {
        LivingEntity hurtBy = getMemory(entity, MemoryModuleType.HURT_BY_ENTITY);
        return hurtBy != null ? hurtBy : entity.getLastHurtByMob();
    }

    public static boolean canSee(LivingEntity viewer, LivingEntity target) {
        Optional<NearestVisibleLivingEntities> visible = viewer.getBrain()
                .getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (visible.isPresent()) {
            return visible.get().contains(target);
        }
        return viewer.hasLineOfSight(target);
    }
}
