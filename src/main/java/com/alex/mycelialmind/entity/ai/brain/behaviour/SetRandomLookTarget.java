package com.alex.mycelialmind.entity.ai.brain.behaviour;

import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.FloatProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.Function;

/**
 * Picks a random horizontal gaze direction and stores it in {@code LOOK_TARGET}.
 */
public class SetRandomLookTarget<E extends Mob> extends HiveBehaviour<E> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORY_REQUIREMENTS = List.of(
            Pair.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT)
    );

    protected FloatProvider runChance = ConstantFloat.of(0.02F);
    protected Function<E, Integer> lookTime = mob -> mob.getRandom().nextInt(20, 41);

    public SetRandomLookTarget<E> lookChance(FloatProvider chance) {
        if (chance != null) {
            this.runChance = chance;
        }
        return this;
    }

    public SetRandomLookTarget<E> lookTime(Function<E, Integer> lookTime) {
        if (lookTime != null) {
            this.lookTime = lookTime;
        }
        return this;
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORY_REQUIREMENTS;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, E mob) {
        return mob.getRandom().nextFloat() < this.runChance.sample(mob.getRandom());
    }

    @Override
    protected void start(E mob) {
        double angle = mob.getRandom().nextDouble() * Math.PI * 2.0D;
        Vec3 gaze = new Vec3(
                mob.getX() + Math.cos(angle) * 6.0D,
                mob.getEyeY() + (mob.getRandom().nextDouble() - 0.5D) * 1.5D,
                mob.getZ() + Math.sin(angle) * 6.0D
        );
        int ticks = Math.max(1, this.lookTime.apply(mob));
        mob.getBrain().setMemoryWithExpiry(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(gaze), ticks);
    }
}
