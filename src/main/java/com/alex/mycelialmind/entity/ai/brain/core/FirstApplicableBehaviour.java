package com.alex.mycelialmind.entity.ai.brain.core;

import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Composite selector that evaluates children in registration order and runs the first one that starts.
 */
public final class FirstApplicableBehaviour<E extends LivingEntity> extends GroupBehaviour<E> {
    @SafeVarargs
    public FirstApplicableBehaviour(Pair<HiveBehaviour<? super E>, Integer>... behaviours) {
        super(behaviours);
    }

    @SafeVarargs
    public FirstApplicableBehaviour(HiveBehaviour<? super E>... behaviours) {
        super(behaviours);
    }

    public FirstApplicableBehaviour(List<Pair<HiveBehaviour<? super E>, Integer>> behaviours) {
        super(behaviours);
    }

    @Override
    @Nullable
    protected HiveBehaviour<? super E> pickBehaviour(
            ServerLevel level, E entity, long gameTime, List<Pair<HiveBehaviour<? super E>, Integer>> entries) {
        for (Pair<HiveBehaviour<? super E>, Integer> entry : entries) {
            HiveBehaviour<? super E> candidate = entry.getFirst();
            if (candidate.tryStart(level, entity, gameTime)) {
                return candidate;
            }
        }
        return null;
    }
}
