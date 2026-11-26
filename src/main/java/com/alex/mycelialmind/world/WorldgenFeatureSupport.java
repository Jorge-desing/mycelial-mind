package com.alex.mycelialmind.world;

import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared worldgen block placement and terrain-probing helpers for mycelial features.
 */
public final class WorldgenFeatureSupport {
    private WorldgenFeatureSupport() {
    }

    public static BlockPos findSurfaceFloor(WorldGenLevel level, int x, int baseY, int z, int maxDeltaY) {
        for (int step = 0; step <= maxDeltaY; step++) {
            int[] offsets = step == 0 ? new int[]{0} : new int[]{step, -step};
            for (int dy : offsets) {
                BlockPos p = new BlockPos(x, baseY + dy, z);
                if (!level.isOutsideBuildHeight(p)
                        && !level.isOutsideBuildHeight(p.above())
                        && isSafeToOverwrite(level, p)
                        && level.getFluidState(p).isEmpty()
                        && level.getBlockState(p).isFaceSturdy(level, p, Direction.UP)
                        && isOpenAirOrReplaceable(level, p.above())) {
                    return p;
                }
            }
        }
        return null;
    }

    public static void anchorBaseDown(WorldGenLevel level, BlockPos startPos, int maxDepth, BlockState fill) {
        TerrainPlacementPolicy.anchorOrganicFoundation(
                level, startPos, maxDepth, fill, null, pos -> isSafeToOverwrite(level, pos));
    }

    public static boolean safeSetBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!isSafeToOverwrite(level, pos)) {
            return false;
        }
        return level.setBlock(pos, state, 2);
    }

    public static boolean isSafeToOverwrite(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        return !state.is(ModBlocks.PURIFICATION_PYLON.get())
                && !state.is(ModBlocks.HIVE_ALTAR.get())
                && !state.is(ModBlocks.MATRIARCH_ALTAR.get())
                && !state.is(ModBlocks.MYCELIAL_RIFT.get())
                && !state.is(ModBlocks.SOVEREIGN_NEXUS.get())
                && !state.is(ModBlocks.RECLAIMED_BLOOM.get());
    }

    public static boolean isOpenAirOrReplaceable(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || level.getBlockEntity(pos) != null) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }
}
