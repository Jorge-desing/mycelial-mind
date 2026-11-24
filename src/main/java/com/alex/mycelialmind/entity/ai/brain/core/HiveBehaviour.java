package com.alex.mycelialmind.entity.ai.brain.core;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Clean-room {@link BehaviorControl} implementation providing fluent preconditions,
 * stop guards, dynamic runtime/cooldown suppliers, and memory-requirement collection
 * without requiring Access Transformers.
 */
public abstract class HiveBehaviour<E extends LivingEntity> implements BehaviorControl<E> {
    protected Predicate<E> startCondition = entity -> true;
    protected Predicate<E> stopCondition = entity -> false;
    protected Consumer<E> taskStartCallback = entity -> {};
    protected Consumer<E> taskStopCallback = entity -> {};
    protected Function<E, Integer> runtimeProvider = entity -> 60;
    protected Function<E, Integer> cooldownProvider = entity -> 0;

    protected long endTimestamp = Long.MAX_VALUE;
    protected long cooldownFinishedAt = HiveBrainScheduler.INITIAL_TICK_SENTINEL;
    private Behavior.Status status = Behavior.Status.STOPPED;

    public final HiveBehaviour<E> whenStarting(Consumer<E> callback) {
        this.taskStartCallback = callback;
        return this;
    }

    public final HiveBehaviour<E> whenStopping(Consumer<E> callback) {
        this.taskStopCallback = callback;
        return this;
    }

    public final HiveBehaviour<E> runFor(Function<E, Integer> runtimeProvider) {
        this.runtimeProvider = runtimeProvider;
        return this;
    }

    public final HiveBehaviour<E> runForBetween(int minTicks, int maxTicks) {
        int lo = Math.min(minTicks, maxTicks);
        int hi = Math.max(minTicks, maxTicks);
        this.runtimeProvider = entity -> lo == hi ? lo : entity.getRandom().nextInt(lo, hi + 1);
        return this;
    }

    public final HiveBehaviour<E> cooldownFor(Function<E, Integer> cooldownProvider) {
        this.cooldownProvider = cooldownProvider;
        return this;
    }

    public final HiveBehaviour<E> cooldownForBetween(int minTicks, int maxTicks) {
        int lo = Math.min(minTicks, maxTicks);
        int hi = Math.max(minTicks, maxTicks);
        this.cooldownProvider = entity -> lo == hi ? lo : entity.getRandom().nextInt(lo, hi + 1);
        return this;
    }

    public final HiveBehaviour<E> startCondition(Predicate<E> predicate) {
        this.startCondition = predicate;
        return this;
    }

    public final HiveBehaviour<E> stopIf(Predicate<E> predicate) {
        this.stopCondition = predicate;
        return this;
    }

    public final HiveBehaviour<E> noTimeout() {
        this.runtimeProvider = entity -> Integer.MAX_VALUE;
        return this;
    }

    @Override
    public Behavior.Status getStatus() {
        return this.status;
    }

    @Override
    public final boolean tryStart(ServerLevel level, E entity, long gameTime) {
        if (!doStartCheck(level, entity, gameTime)) {
            return false;
        }
        this.status = Behavior.Status.RUNNING;
        int runtime = this.runtimeProvider.apply(entity);
        this.endTimestamp = HiveBrainScheduler.computeEndTimestamp(gameTime, runtime);
        start(level, entity, gameTime);
        this.taskStartCallback.accept(entity);
        return true;
    }

    protected boolean doStartCheck(ServerLevel level, E entity, long gameTime) {
        return !HiveBrainScheduler.isOnCooldown(gameTime, this.cooldownFinishedAt)
                && hasRequiredMemories(entity)
                && this.startCondition.test(entity)
                && checkExtraStartConditions(level, entity);
    }

    @Override
    public final void tickOrStop(ServerLevel level, E entity, long gameTime) {
        if (this.status != Behavior.Status.RUNNING) {
            return;
        }
        if (!this.stopCondition.test(entity) && !timedOut(gameTime) && canStillUse(level, entity, gameTime)) {
            tick(level, entity, gameTime);
        } else {
            doStop(level, entity, gameTime);
        }
    }

    @Override
    public final void doStop(ServerLevel level, E entity, long gameTime) {
        if (this.status == Behavior.Status.STOPPED) {
            return;
        }
        this.status = Behavior.Status.STOPPED;
        stop(level, entity, gameTime);
        this.taskStopCallback.accept(entity);
        int cooldown = this.cooldownProvider.apply(entity);
        this.cooldownFinishedAt = HiveBrainScheduler.computeCooldownFinishedAt(gameTime, cooldown);
    }

    protected boolean canStillUse(ServerLevel level, E entity, long gameTime) {
        return hasRequiredMemories(entity) && shouldKeepRunning(entity);
    }

    protected boolean timedOut(long gameTime) {
        return HiveBrainScheduler.isTimedOut(gameTime, this.endTimestamp);
    }

    public final boolean hasRequiredMemories(E entity) {
        Brain<?> brain = entity.getBrain();
        for (Pair<MemoryModuleType<?>, MemoryStatus> requirement : getMemoryRequirements()) {
            if (!brain.checkMemory(requirement.getFirst(), requirement.getSecond())) {
                return false;
            }
        }
        return true;
    }

    public Collection<MemoryModuleType<?>> collectMemoryTypes() {
        Set<MemoryModuleType<?>> types = new LinkedHashSet<>();
        for (Pair<MemoryModuleType<?>, MemoryStatus> req : getMemoryRequirements()) {
            if (req.getFirst() != null) {
                types.add(req.getFirst());
            }
        }
        return types;
    }

    protected boolean checkExtraStartConditions(ServerLevel level, E entity) {
        return true;
    }

    protected boolean shouldKeepRunning(E entity) {
        return false;
    }

    protected void start(ServerLevel level, E entity, long gameTime) {
        start(entity);
    }

    protected void start(E entity) {
    }

    protected void tick(ServerLevel level, E entity, long gameTime) {
        tick(entity);
    }

    protected void tick(E entity) {
    }

    protected void stop(ServerLevel level, E entity, long gameTime) {
        stop(entity);
    }

    protected void stop(E entity) {
    }

    protected abstract List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements();

    @Override
    public String debugString() {
        return getClass().getSimpleName();
    }
}
