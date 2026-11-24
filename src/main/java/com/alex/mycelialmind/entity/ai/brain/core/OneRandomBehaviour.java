package com.alex.mycelialmind.entity.ai.brain.core;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Composite selector that picks an eligible child behaviour using weighted random sampling.
 */
public final class OneRandomBehaviour<E extends LivingEntity> extends GroupBehaviour<E> {
    @SafeVarargs
    public OneRandomBehaviour(Pair<HiveBehaviour<? super E>, Integer>... behaviours) {
        super(behaviours);
    }

    @SafeVarargs
    public OneRandomBehaviour(HiveBehaviour<? super E>... behaviours) {
        super(behaviours);
    }

    public OneRandomBehaviour(Collection<Pair<HiveBehaviour<? super E>, Integer>> behaviours) {
        super(behaviours);
    }

    @Override
    @Nullable
    protected HiveBehaviour<? super E> pickBehaviour(
            ServerLevel level, E entity, long gameTime, List<Pair<HiveBehaviour<? super E>, Integer>> entries) {
        if (entries.isEmpty()) {
            return null;
        }
        List<Pair<HiveBehaviour<? super E>, Integer>> pool = new ArrayList<>(entries);
        while (!pool.isEmpty()) {
            int[] weights = new int[pool.size()];
            for (int i = 0; i < pool.size(); i++) {
                weights[i] = pool.get(i).getSecond();
            }
            int pickedIdx = HiveBrainScheduler.selectWeightedIndex(weights, entity.getRandom().nextInt());
            if (pickedIdx < 0) {
                return null;
            }
            HiveBehaviour<? super E> candidate = pool.remove(pickedIdx).getFirst();
            if (candidate.tryStart(level, entity, gameTime)) {
                return candidate;
            }
        }
        return null;
    }
}
