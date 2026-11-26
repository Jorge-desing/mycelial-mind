package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Shared bounded placement and synaptic-node registration helpers for structure pieces.
 */
public final class StructurePieceSupport {
    private StructurePieceSupport() {
    }

    public static void setIfInside(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    public static void registerNodeIfInside(
            WorldGenLevel level, BoundingBox box, BlockPos pos, SynapticNodeType type) {
        if (box.isInside(pos) && level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker.get(serverLevel).addNode(pos, type);
        }
    }
}
