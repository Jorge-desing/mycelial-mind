package com.alex.mycelialmind.entity.ai.brain.sensor;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainScheduler;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Clean-room sensor base supporting per-instance scan intervals and post-scan callbacks
 * while delegating tick gating to {@link HiveBrainScheduler}.
 */
public abstract class HiveSensor<E extends LivingEntity> extends Sensor<E> {
    protected Function<E, Integer> scanRate = entity -> 20;
    protected Consumer<E> scanCallback = entity -> {};
    protected long nextTickTime = HiveBrainScheduler.INITIAL_TICK_SENTINEL;

    protected HiveSensor() {
        super(1);
    }

    public HiveSensor<E> setScanRate(Function<E, Integer> scanRate) {
        this.scanRate = scanRate != null ? scanRate : entity -> 20;
        return this;
    }

    public HiveSensor<E> afterScanning(Consumer<E> callback) {
        this.scanCallback = callback != null ? callback : entity -> {};
        return this;
    }

    @Override
    public void doTick(ServerLevel level, E entity) {
        long gameTime = level.getGameTime();
        if (!HiveBrainScheduler.shouldRunSensor(gameTime, this.nextTickTime)) {
            return;
        }
        int rate = Math.max(1, this.scanRate.apply(entity));
        this.nextTickTime = HiveBrainScheduler.computeNextTickTime(gameTime, rate);
        scan(level, entity);
        this.scanCallback.accept(entity);
    }

    protected abstract void scan(ServerLevel level, E entity);

    public abstract List<MemoryModuleType<?>> memoriesUsed();

    public SensorType<? extends Sensor<?>> type() {
        return SensorType.DUMMY;
    }

    @Override
    public final Set<MemoryModuleType<?>> requires() {
        return new LinkedHashSet<>(memoriesUsed());
    }
}
