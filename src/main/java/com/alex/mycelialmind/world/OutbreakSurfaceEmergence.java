package com.alex.mycelialmind.world;

import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Allows a colony that has reached the surface to establish a related structure. */
public final class OutbreakSurfaceEmergence {
    private OutbreakSurfaceEmergence() {}

    public static boolean tryEmerge(ServerLevel level, BlockPos source, BlockPos grown, RandomSource random) {
        if (!level.dimension().equals(Level.OVERWORLD) || !level.hasChunkAt(grown)
                || !level.getBlockState(grown).is(ModBlocks.CREEPING_MYCELIUM.get())
                || grown.getY() < level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                        grown.getX(), grown.getZ()) - 1
                || grown.getY() - source.getY() < 8 || random.nextInt(100) != 0) {
            return false;
        }
        BlockPos origin = grown.above();
        boolean sinkhole = origin.getY() - source.getY() >= 40 && random.nextInt(3) == 0;
        if (!isNaturalVolume(level, origin, sinkhole)) {
            return false;
        }
        List<BlockPos> grownSoil = new ArrayList<>();
        for (int dx = -11; dx <= 11; dx++) {
            for (int dz = -11; dz <= 11; dz++) {
                BlockPos floor = grown.offset(dx, 0, dz);
                if (level.getBlockState(floor).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                    grownSoil.add(floor.immutable());
                }
            }
        }
        boolean built = sinkhole
                ? MycelialSinkholeFeature.generateSinkholeFromOutbreak(level, origin, source, random)
                : SurfaceHiveFeature.generateHiveFromOutbreak(level, origin, source, random);
        if (built) {
            for (BlockPos floor : grownSoil) {
                if (level.getBlockState(floor).is(Blocks.STONE) && level.getBlockEntity(floor) == null) {
                    level.setBlockAndUpdate(floor, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());
                }
            }
        }
        return built;
    }

    private static boolean isNaturalVolume(ServerLevel level, BlockPos origin, boolean sinkhole) {
        int radiusX = sinkhole ? 12 : 11;
        int radiusZ = sinkhole ? 24 : 11;
        int bottom = sinkhole ? -38 : -9;
        int top = sinkhole ? 6 : 16;
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        for (int dx = -radiusX; dx <= radiusX; dx++) {
            for (int dz = -radiusZ; dz <= radiusZ; dz++) {
                BlockPos column = origin.offset(dx, 0, dz);
                if (!level.hasChunkAt(column)) {
                    return false;
                }
                for (int dy = bottom; dy <= top; dy++) {
                    BlockPos pos = column.offset(0, dy, 0);
                    if (level.isOutsideBuildHeight(pos) || tracker.isProtected(pos)
                            || level.getBlockEntity(pos) != null || !level.getFluidState(pos).isEmpty()) {
                        return false;
                    }
                    BlockState state = level.getBlockState(pos);
                    // Do not sever the colony below the footprint or erase grown slopes.
                    if (dy != -1 && state.is(ModBlocks.CREEPING_MYCELIUM.get())) {
                        return false;
                    }
                    if (!(state.isAir() || state.canBeReplaced() || state.is(BlockTags.BASE_STONE_OVERWORLD)
                            || state.is(BlockTags.DIRT) || state.is(BlockTags.SAND)
                            || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.MUD)
                            || state.is(ModBlocks.MYCELIAL_STONE.get())
                            || state.is(ModBlocks.CREEPING_MYCELIUM.get()))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
