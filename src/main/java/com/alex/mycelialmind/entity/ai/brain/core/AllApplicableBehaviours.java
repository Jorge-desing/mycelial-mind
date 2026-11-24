package com.alex.mycelialmind.entity.ai.brain.core;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * Parallel composite behaviour that starts every eligible child and ticks all active children concurrently.
 */
public final class AllApplicableBehaviours<E extends LivingEntity> extends GroupBehaviour<E> {
    @SafeVarargs
    public AllApplicableBehaviours(Pair<HiveBehaviour<? super E>, Integer>... behaviours) {
        super(behaviours);
    }

    @SafeVarargs
    public AllApplicableBehaviours(HiveBehaviour<? super E>... behaviours) {
        super(behaviours);
    }

    public AllApplicableBehaviours(Collection<Pair<HiveBehaviour<? super E>, Integer>> behaviours) {
        super(behaviours);
    }

    @Override
    protected boolean doStartCheck(ServerLevel level, E entity, long gameTime) {
        if (HiveBrainScheduler.isOnCooldown(gameTime, this.cooldownFinishedAt) || !this.startCondition.test(entity)) {
            return false;
        }
        boolean anyStarted = false;
        for (Pair<HiveBehaviour<? super E>, Integer> entry : this.behaviours) {
            if (entry.getFirst().tryStart(level, entity, gameTime)) {
                anyStarted = true;
            }
        }
        return anyStarted;
    }

    @Override
    @Nullable
    protected HiveBehaviour<? super E> pickBehaviour(
            ServerLevel level, E entity, long gameTime, List<Pair<HiveBehaviour<? super E>, Integer>> entries) {
        return null;
    }

    @Override
    protected boolean shouldKeepRunning(E entity) {
        for (Pair<HiveBehaviour<? super E>, Integer> entry : this.behaviours) {
            if (entry.getFirst().getStatus() == Behavior.Status.RUNNING) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void tick(ServerLevel level, E entity, long gameTime) {
        boolean anyStillRunning = false;
        for (Pair<HiveBehaviour<? super E>, Integer> entry : this.behaviours) {
            HiveBehaviour<? super E> child = entry.getFirst();
            if (child.getStatus() == Behavior.Status.RUNNING) {
                child.tickOrStop(level, entity, gameTime);
                if (child.getStatus() == Behavior.Status.RUNNING) {
                    anyStillRunning = true;
                }
            }
        }
        if (!anyStillRunning) {
            doStop(level, entity, gameTime);
        }
    }

    @Override
    protected void stop(ServerLevel level, E entity, long gameTime) {
        for (Pair<HiveBehaviour<? super E>, Integer> entry : this.behaviours) {
            HiveBehaviour<? super E> child = entry.getFirst();
            if (child.getStatus() == Behavior.Status.RUNNING) {
                child.doStop(level, entity, gameTime);
            }
        }
    }
}
