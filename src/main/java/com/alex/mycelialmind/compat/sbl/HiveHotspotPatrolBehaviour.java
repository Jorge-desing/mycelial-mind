package com.alex.mycelialmind.compat.sbl;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.world.MycelialFastNoise;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.phys.Vec3;
import net.tslat.smartbrainlib.api.core.behaviour.ExtendedBehaviour;

import java.util.List;

/**
 * SBL Brain behaviour: directs idle MycelialMobs toward recent spatial combat hotspots.
 */
public class HiveHotspotPatrolBehaviour extends ExtendedBehaviour<MycelialMob> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORIES = List.of(
            Pair.of(ModMemoryTypes.SHARED_TARGET_POS.get(), MemoryStatus.VALUE_PRESENT),
            Pair.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT)
    );

    public HiveHotspotPatrolBehaviour() {
        cooldownFor(entity -> 60);
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORIES;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, MycelialMob entity) {
        BlockPos targetPos = MycelialSblBridge.getSharedTargetPos(entity);
        return targetPos != null && entity.blockPosition().distSqr(targetPos) > 16.0D;
    }

    @Override
    protected void start(ServerLevel level, MycelialMob entity, long gameTime) {
        BlockPos targetPos = MycelialSblBridge.getSharedTargetPos(entity);
        if (targetPos != null) {
            Vec3 flow = MycelialFastNoise.organicFlow2D(
                    targetPos.getX(),
                    targetPos.getZ(),
                    level.getSeed() + entity.getId(),
                    gameTime * 0.05D
            );
            BlockPos organicTarget = targetPos.offset((int) Math.round(flow.x * 2.0D), 0, (int) Math.round(flow.z * 2.0D));
            entity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(organicTarget, 1.05F, 2));
        }
    }
}
