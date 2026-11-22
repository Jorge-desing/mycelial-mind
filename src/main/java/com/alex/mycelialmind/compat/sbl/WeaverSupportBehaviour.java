package com.alex.mycelialmind.compat.sbl;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;

import java.util.Comparator;
import java.util.List;

/** Tracks the nearest allied unit for the weaver's support positioning. */
public final class WeaverSupportBehaviour extends ExtendedBehaviour<MycelialMob> {
    public WeaverSupportBehaviour() {
        noTimeout();
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(Pair.of(ModMemoryTypes.WEAVER_SUPPORT_POS.get(), MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean shouldKeepRunning(MycelialMob mob) {
        return true;
    }

    @Override
    protected void tick(ServerLevel level, MycelialMob mob, long time) {
        if (mob.isDesynced()) {
            mob.getBrain().eraseMemory(ModMemoryTypes.WEAVER_SUPPORT_POS.get());
            return;
        }
        if ((mob.tickCount + mob.getId()) % 20 != 0) {
            return;
        }
        level.getEntitiesOfClass(MycelialMob.class, mob.getBoundingBox().inflate(24),
                        ally -> ally != mob && ally.isAlive() && !mob.isRivalStrainTarget(ally))
                .stream().min(Comparator.comparingDouble(mob::distanceToSqr))
                .ifPresentOrElse(ally -> mob.getBrain().setMemory(ModMemoryTypes.WEAVER_SUPPORT_POS.get(), ally.blockPosition()),
                        () -> mob.getBrain().eraseMemory(ModMemoryTypes.WEAVER_SUPPORT_POS.get()));
    }

    @Override
    protected void stop(ServerLevel level, MycelialMob mob, long time) {
        mob.getBrain().eraseMemory(ModMemoryTypes.WEAVER_SUPPORT_POS.get());
    }
}
