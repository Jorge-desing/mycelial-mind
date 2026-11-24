package com.alex.mycelialmind.entity.ai.brain.core;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Sequential composite behaviour executing children strictly in order from start to finish,
 * with an optional predicate to reset the sequence early after any step finishes.
 */
public final class SequentialBehaviour<E extends LivingEntity> extends GroupBehaviour<E> {
    private Predicate<HiveBehaviour<? super E>> earlyResetPredicate = behaviour -> false;
    private int runningIndex = 0;

    @SafeVarargs
    public SequentialBehaviour(Pair<HiveBehaviour<? super E>, Integer>... behaviours) {
        super(behaviours);
    }

    @SafeVarargs
    public SequentialBehaviour(HiveBehaviour<? super E>... behaviours) {
        super(behaviours);
    }

    public SequentialBehaviour(List<Pair<HiveBehaviour<? super E>, Integer>> behaviours) {
        super(behaviours);
    }

    public SequentialBehaviour<E> resetIf(Predicate<HiveBehaviour<? super E>> predicate) {
        this.earlyResetPredicate = predicate;
        return this;
    }

    @Override
    @Nullable
    protected HiveBehaviour<? super E> pickBehaviour(
            ServerLevel level, E entity, long gameTime, List<Pair<HiveBehaviour<? super E>, Integer>> entries) {
        while (this.runningIndex >= 0 && this.runningIndex < entries.size()) {
            HiveBehaviour<? super E> candidate = entries.get(this.runningIndex).getFirst();
            if (candidate.tryStart(level, entity, gameTime)) {
                return candidate;
            }
            this.runningIndex = HiveBrainScheduler.nextSequentialIndex(this.runningIndex, entries.size());
        }
        this.runningIndex = 0;
        return null;
    }

    @Override
    protected void tick(ServerLevel level, E entity, long gameTime) {
        if (this.runningBehaviour == null) {
            this.runningIndex = 0;
            doStop(level, entity, gameTime);
            return;
        }
        this.runningBehaviour.tickOrStop(level, entity, gameTime);
        if (this.runningBehaviour.getStatus() == Behavior.Status.STOPPED) {
            if (this.earlyResetPredicate.test(this.runningBehaviour)) {
                this.runningBehaviour = null;
                this.runningIndex = 0;
                doStop(level, entity, gameTime);
                return;
            }
            this.runningIndex = HiveBrainScheduler.nextSequentialIndex(this.runningIndex, this.behaviours.size());
            if (this.runningIndex < 0) {
                this.runningBehaviour = null;
                this.runningIndex = 0;
                doStop(level, entity, gameTime);
                return;
            }
            HiveBehaviour<? super E> next = pickBehaviour(level, entity, gameTime, this.behaviours);
            this.runningBehaviour = next;
            if (next == null) {
                doStop(level, entity, gameTime);
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, E entity, long gameTime) {
        super.stop(level, entity, gameTime);
        this.runningIndex = 0;
    }
}
