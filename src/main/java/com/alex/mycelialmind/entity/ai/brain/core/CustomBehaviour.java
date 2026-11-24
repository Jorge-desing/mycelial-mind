package com.alex.mycelialmind.entity.ai.brain.core;

import com.mojang.datafixers.util.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.List;
import java.util.function.Consumer;

/**
 * Lightweight {@link HiveBehaviour} wrapping a one-shot callback on start.
 */
public final class CustomBehaviour<E extends LivingEntity> extends HiveBehaviour<E> {
    private Consumer<E> callback;

    public CustomBehaviour(Consumer<E> callback) {
        this.callback = callback != null ? callback : entity -> {};
    }

    public CustomBehaviour<E> callback(Consumer<E> callback) {
        this.callback = callback != null ? callback : entity -> {};
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of();
    }

    @Override
    protected void start(E entity) {
        this.callback.accept(entity);
    }
}
