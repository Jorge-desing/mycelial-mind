package com.alex.mycelialmind.entity.ai.brain.core;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility-based composite selector that evaluates dynamic scores for eligible children,
 * applies hysteresis to stabilize the currently running branch, and optionally preempts
 * the active branch when another eligible branch achieves a strictly higher utility score.
 */
public final class UtilitySelectorBehaviour<E extends LivingEntity> extends GroupBehaviour<E> {
    @FunctionalInterface
    public interface UtilityScorer<E> {
        float score(E entity);
    }

    public record Entry<E extends LivingEntity>(
            HiveBehaviour<? super E> behaviour,
            UtilityScorer<? super E> scorer) {
    }

    private final List<HiveBehaviour<? super E>> branches = new ArrayList<>();
    private final List<UtilityScorer<? super E>> scorers = new ArrayList<>();
    private float[] scoreBuffer = new float[0];
    private float minScoreThreshold = 0.001F;
    private float hysteresisBonus = 0.10F;
    private boolean continuousPreemption = true;
    private int runningIndex = -1;

    public UtilitySelectorBehaviour() {
        super(List.of());
    }

    @SafeVarargs
    public UtilitySelectorBehaviour(Entry<E>... entries) {
        this();
        if (entries != null) {
            for (Entry<E> entry : entries) {
                if (entry != null) {
                    add(entry.behaviour(), entry.scorer());
                }
            }
        }
    }

    public static <E extends LivingEntity> Entry<E> entry(
            HiveBehaviour<? super E> behaviour, UtilityScorer<? super E> scorer) {
        return new Entry<>(behaviour, scorer);
    }

    public UtilitySelectorBehaviour<E> add(
            HiveBehaviour<? super E> behaviour, UtilityScorer<? super E> scorer) {
        if (behaviour != null && scorer != null) {
            this.branches.add(behaviour);
            this.scorers.add(scorer);
            this.scoreBuffer = new float[this.branches.size()];
        }
        return this;
    }

    public UtilitySelectorBehaviour<E> minScoreThreshold(float minScoreThreshold) {
        this.minScoreThreshold = minScoreThreshold;
        return this;
    }

    public UtilitySelectorBehaviour<E> hysteresisBonus(float hysteresisBonus) {
        this.hysteresisBonus = Math.max(0.0F, hysteresisBonus);
        return this;
    }

    public UtilitySelectorBehaviour<E> continuousPreemption(boolean continuousPreemption) {
        this.continuousPreemption = continuousPreemption;
        return this;
    }

    public float getMinScoreThreshold() {
        return this.minScoreThreshold;
    }

    public float getHysteresisBonus() {
        return this.hysteresisBonus;
    }

    public boolean isContinuousPreemption() {
        return this.continuousPreemption;
    }

    @Override
    public List<HiveBehaviour<? super E>> getChildBehaviours() {
        return Collections.unmodifiableList(this.branches);
    }

    @Override
    public Collection<MemoryModuleType<?>> collectMemoryTypes() {
        Set<MemoryModuleType<?>> types = new LinkedHashSet<>(super.collectMemoryTypes());
        for (int i = 0, size = this.branches.size(); i < size; i++) {
            types.addAll(this.branches.get(i).collectMemoryTypes());
        }
        return types;
    }

    @Override
    @Nullable
    protected HiveBehaviour<? super E> pickBehaviour(
            ServerLevel level, E entity, long gameTime, List<Pair<HiveBehaviour<? super E>, Integer>> entries) {
        int count = this.branches.size();
        if (count == 0) {
            return null;
        }
        for (int i = 0; i < count; i++) {
            HiveBehaviour<? super E> branch = this.branches.get(i);
            this.scoreBuffer[i] = branch.doStartCheck(level, entity, gameTime)
                    ? this.scorers.get(i).score(entity)
                    : Float.NEGATIVE_INFINITY;
        }

        while (true) {
            int winner = HiveBrainScheduler.selectHighestUtilityIndex(
                    this.scoreBuffer, -1, this.minScoreThreshold, this.hysteresisBonus);
            if (winner < 0) {
                this.runningIndex = -1;
                return null;
            }
            HiveBehaviour<? super E> chosen = this.branches.get(winner);
            if (chosen.tryStart(level, entity, gameTime)) {
                this.runningIndex = winner;
                return chosen;
            }
            this.scoreBuffer[winner] = Float.NEGATIVE_INFINITY;
        }
    }

    @Override
    protected void tick(ServerLevel level, E entity, long gameTime) {
        if (this.runningBehaviour == null) {
            this.runningIndex = -1;
            doStop(level, entity, gameTime);
            return;
        }

        int count = this.branches.size();
        if (this.continuousPreemption && count > 1) {
            for (int i = 0; i < count; i++) {
                HiveBehaviour<? super E> branch = this.branches.get(i);
                boolean eligible;
                if (i == this.runningIndex) {
                    eligible = !branch.stopCondition.test(entity)
                            && !branch.timedOut(gameTime)
                            && branch.canStillUse(level, entity, gameTime);
                } else {
                    eligible = branch.doStartCheck(level, entity, gameTime);
                }
                this.scoreBuffer[i] = eligible ? this.scorers.get(i).score(entity) : Float.NEGATIVE_INFINITY;
            }
            int best = HiveBrainScheduler.selectHighestUtilityIndex(
                    this.scoreBuffer, this.runningIndex, this.minScoreThreshold, this.hysteresisBonus);
            if (best >= 0 && best != this.runningIndex) {
                HiveBehaviour<? super E> next = this.branches.get(best);
                this.runningBehaviour.doStop(level, entity, gameTime);
                if (next.tryStart(level, entity, gameTime)) {
                    this.runningBehaviour = next;
                    this.runningIndex = best;
                } else {
                    this.runningBehaviour = null;
                    this.runningIndex = -1;
                    doStop(level, entity, gameTime);
                    return;
                }
            }
        }

        this.runningBehaviour.tickOrStop(level, entity, gameTime);
        if (this.runningBehaviour.getStatus() == Behavior.Status.STOPPED) {
            this.runningBehaviour = null;
            this.runningIndex = -1;
            doStop(level, entity, gameTime);
        }
    }

    @Override
    protected void stop(ServerLevel level, E entity, long gameTime) {
        this.runningIndex = -1;
        super.stop(level, entity, gameTime);
    }
}
