package com.alex.mycelialmind.entity.ai.brain.core;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.Consumer;

/**
 * Timed {@link HiveBehaviour} that waits {@code delayTime} ticks after starting before
 * invoking {@link #doDelayedAction(LivingEntity)} and its optional activation callback once.
 */
public abstract class DelayedBehaviour<E extends LivingEntity> extends HiveBehaviour<E> {
    protected final int delayTime;
    protected long delayFinishedAt = Long.MAX_VALUE;
    protected Consumer<E> delayedCallback = entity -> {};
    private boolean actionExecuted = false;

    protected DelayedBehaviour(int delayTicks) {
        this.delayTime = Math.max(0, delayTicks);
        runFor(entity -> this.delayTime);
    }

    public final DelayedBehaviour<E> whenActivating(Consumer<E> callback) {
        this.delayedCallback = callback;
        return this;
    }

    @Override
    protected final void start(ServerLevel level, E entity, long gameTime) {
        start(entity);
        this.delayFinishedAt = gameTime + this.delayTime;
        this.actionExecuted = false;
    }

    @Override
    protected boolean shouldKeepRunning(E entity) {
        return !this.actionExecuted;
    }

    @Override
    protected final void tick(ServerLevel level, E entity, long gameTime) {
        if (!this.actionExecuted && gameTime >= this.delayFinishedAt) {
            this.actionExecuted = true;
            doDelayedAction(entity);
            this.delayedCallback.accept(entity);
        }
    }

    @Override
    protected final void stop(ServerLevel level, E entity, long gameTime) {
        this.delayFinishedAt = Long.MAX_VALUE;
        this.actionExecuted = false;
        stop(entity);
    }

    protected void doDelayedAction(E entity) {
    }
}
