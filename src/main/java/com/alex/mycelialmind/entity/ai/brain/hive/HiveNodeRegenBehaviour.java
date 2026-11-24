package com.alex.mycelialmind.entity.ai.brain.hive;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModParticles;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.List;

/**
 * Brain behaviour: regenerates wounded {@link MycelialMob}s near a Synaptic Node when idle.
 */
public class HiveNodeRegenBehaviour extends HiveBehaviour<MycelialMob> {
    private static final List<Pair<MemoryModuleType<?>, MemoryStatus>> MEMORIES = List.of(
            Pair.of(ModMemoryTypes.NEAREST_NODE_POS.get(), MemoryStatus.VALUE_PRESENT)
    );

    public HiveNodeRegenBehaviour() {
        cooldownFor(entity -> 40);
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return MEMORIES;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, MycelialMob entity) {
        if (entity.getHealth() >= entity.getMaxHealth()) {
            return false;
        }
        BlockPos nodePos = HiveBrainBridge.getNearestNodePos(entity);
        return nodePos != null && entity.blockPosition().distSqr(nodePos) <= 100.0D;
    }

    @Override
    protected void start(ServerLevel level, MycelialMob entity, long gameTime) {
        entity.heal(1.5F);
        level.sendParticles(ModParticles.BIO_LUME.get(),
                entity.getX(), entity.getY() + 0.8, entity.getZ(),
                6, 0.3, 0.3, 0.3, 0.02);
    }
}
