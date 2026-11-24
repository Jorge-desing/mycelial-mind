package com.alex.mycelialmind.entity.ai.brain.core;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Base composite {@link HiveBehaviour} managing an ordered/weighted list of child behaviours
 * and propagating their memory requirements automatically for brain registration.
 */
public abstract class GroupBehaviour<E extends LivingEntity> extends HiveBehaviour<E> {
    protected final List<Pair<HiveBehaviour<? super E>, Integer>> behaviours;
    @Nullable
    protected HiveBehaviour<? super E> runningBehaviour;

    @SafeVarargs
    protected GroupBehaviour(Pair<HiveBehaviour<? super E>, Integer>... behaviours) {
        this(List.of(behaviours));
    }

    @SafeVarargs
    protected GroupBehaviour(HiveBehaviour<? super E>... behaviours) {
        this(toDefaultWeightedPairs(behaviours));
    }

    protected GroupBehaviour(Collection<Pair<HiveBehaviour<? super E>, Integer>> behaviours) {
        List<Pair<HiveBehaviour<? super E>, Integer>> copy = new ArrayList<>(behaviours.size());
        for (Pair<HiveBehaviour<? super E>, Integer> entry : behaviours) {
            if (entry != null && entry.getFirst() != null) {
                copy.add(Pair.of(entry.getFirst(), Math.max(1, entry.getSecond() == null ? 1 : entry.getSecond())));
            }
        }
        this.behaviours = Collections.unmodifiableList(copy);
        noTimeout();
    }

    private static <E extends LivingEntity> List<Pair<HiveBehaviour<? super E>, Integer>> toDefaultWeightedPairs(
            HiveBehaviour<? super E>[] behaviours) {
        List<Pair<HiveBehaviour<? super E>, Integer>> pairs = new ArrayList<>(behaviours.length);
        for (HiveBehaviour<? super E> behaviour : behaviours) {
            if (behaviour != null) {
                pairs.add(Pair.of(behaviour, 1));
            }
        }
        return pairs;
    }

    public List<HiveBehaviour<? super E>> getChildBehaviours() {
        List<HiveBehaviour<? super E>> children = new ArrayList<>(this.behaviours.size());
        for (Pair<HiveBehaviour<? super E>, Integer> pair : this.behaviours) {
            children.add(pair.getFirst());
        }
        return children;
    }

    @Override
    public Collection<MemoryModuleType<?>> collectMemoryTypes() {
        Set<MemoryModuleType<?>> types = new LinkedHashSet<>(super.collectMemoryTypes());
        for (Pair<HiveBehaviour<? super E>, Integer> pair : this.behaviours) {
            types.addAll(pair.getFirst().collectMemoryTypes());
        }
        return types;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of();
    }

    @Nullable
    protected abstract HiveBehaviour<? super E> pickBehaviour(
            ServerLevel level, E entity, long gameTime, List<Pair<HiveBehaviour<? super E>, Integer>> entries);

    @Override
    protected boolean doStartCheck(ServerLevel level, E entity, long gameTime) {
        if (!super.doStartCheck(level, entity, gameTime)) {
            return false;
        }
        this.runningBehaviour = pickBehaviour(level, entity, gameTime, this.behaviours);
        return this.runningBehaviour != null;
    }

    @Override
    protected boolean shouldKeepRunning(E entity) {
        return this.runningBehaviour != null && this.runningBehaviour.getStatus() == Behavior.Status.RUNNING;
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    @Override
    protected void tick(ServerLevel level, E entity, long gameTime) {
        if (this.runningBehaviour == null) {
            doStop(level, entity, gameTime);
            return;
        }
        this.runningBehaviour.tickOrStop(level, entity, gameTime);
        if (this.runningBehaviour.getStatus() == Behavior.Status.STOPPED) {
            this.runningBehaviour = null;
            doStop(level, entity, gameTime);
        }
    }

    @Override
    protected void stop(ServerLevel level, E entity, long gameTime) {
        if (this.runningBehaviour != null) {
            this.runningBehaviour.doStop(level, entity, gameTime);
            this.runningBehaviour = null;
        }
    }

    @Override
    public String debugString() {
        return getClass().getSimpleName() + (this.runningBehaviour != null
                ? "[" + this.runningBehaviour.debugString() + "]"
                : "[]");
    }
}
