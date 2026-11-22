package com.alex.mycelialmind.compat.sbl;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveOrder;
import com.alex.mycelialmind.hive.HiveSwarmCoordinator;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;

import java.util.List;
import java.util.UUID;

/** Publishes a coordinated flank destination while the current hive order is valid. */
public final class StalkerFlankBehaviour extends ExtendedBehaviour<MycelialMob> {
    private UUID lastTarget;
    public StalkerFlankBehaviour() {
        noTimeout();
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(Pair.of(ModMemoryTypes.STALKER_FLANK_POS.get(), MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean shouldKeepRunning(MycelialMob mob) {
        return true;
    }

    @Override
    protected void tick(ServerLevel level, MycelialMob mob, long gameTime) {
        LivingEntity target = mob.getTarget();
        if (mob.isDesynced() || target == null || !target.isAlive() || !mob.canAttack(target)
                || MycelialSblBridge.getHiveOrder(mob) != HiveOrder.FLANK_PERIMETER
                || HiveBlackboard.getOrder(level) != HiveOrder.FLANK_PERIMETER) {
            mob.getBrain().eraseMemory(ModMemoryTypes.STALKER_FLANK_POS.get());
            lastTarget = null;
            return;
        }
        if (!target.getUUID().equals(lastTarget) || (mob.tickCount + mob.getId()) % 10 == 0) {
            BlockPos destination = BlockPos.containing(HiveSwarmCoordinator.computeSwarmApproachPos(mob, target));
            mob.getBrain().setMemory(ModMemoryTypes.STALKER_FLANK_POS.get(), destination);
            lastTarget = target.getUUID();
        }
    }

    @Override
    protected void stop(ServerLevel level, MycelialMob mob, long gameTime) {
        mob.getBrain().eraseMemory(ModMemoryTypes.STALKER_FLANK_POS.get());
        lastTarget = null;
    }
}
