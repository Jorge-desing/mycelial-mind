package com.alex.mycelialmind.world;

import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** A rare surface opening, only when it can be joined to an existing cave outbreak. */
public final class SurfaceOutbreakFissure {
    private static final int[][] RING = {
            {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {-1, 2}, {-2, 2}, {-2, 1},
            {-2, 0}, {-2, -1}, {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2}, {2, -1}
    };
    private SurfaceOutbreakFissure() {}

    public static boolean generate(WorldGenLevel level, BlockPos surface, RandomSource random, HiveProgressionTier tier) {
        if (TerrainPlacementPolicy.isExcludedSurfaceBiome(level, surface)
                || TerrainPlacementPolicy.isNearVanillaSurfaceStructure(level, surface, 1)
                || !TerrainPlacementPolicy.hasSuitableTwoRingSurfaceTerrain(level, surface.above(), 2, 4, 3, 3)) {
            return false;
        }
        ServerLevel server = level.getLevel() instanceof ServerLevel sl ? sl : null;
        if (server == null || TerrainPlacementPolicy.isWithinSpawnSafeZone(surface.getX(), surface.getZ(),
                server.getSharedSpawnPos().getX(), server.getSharedSpawnPos().getZ(),
                StructureProgressionPolicy.surfaceOutbreakMinSpawnDistance(tier))) {
            return false;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(server);
        // The surface fissure extends up to eight blocks from its anchor.
        for (int dx = -8; dx <= 8; dx++) {
            for (int dz = -8; dz <= 8; dz++) {
                if (tracker.isProtected(surface.offset(dx, 0, dz))) return false;
            }
        }
        List<BlockPos> route = new ArrayList<>();
        int rotation = random.nextInt(RING.length);
        for (int depth = 0; depth <= 40; depth++) {
            int[] offset = RING[(rotation + depth) % RING.length];
            BlockPos floor = surface.offset(offset[0], -depth, offset[1]);
            if (!safeRouteCell(level, tracker, floor)) {
                return false;
            }
            route.add(floor);
            if (depth < 12) continue;
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos cave = floor.relative(direction).above();
                if (intersectsRoute(route, cave) || !WorldgenFeatureSupport.isSafeToOverwrite(level, cave)
                        || level.getBlockState(cave).is(ModBlocks.HIVE_PROTECTED_BLOCKS)
                        || !natural(level.getBlockState(cave)) || !level.getFluidState(cave).isEmpty()
                        || !level.getBlockState(cave.below()).isSolid()
                        || !level.getBlockState(cave).isFaceSturdy(level, cave, Direction.UP)
                        || !level.getBlockState(cave.above()).isAir()
                        || !level.getBlockState(cave.above(2)).isAir()
                        || !TerrainPlacementPolicy.hasSafeSubterraneanOutbreakFooting(level, cave)
                        || tracker.isProtected(cave)) continue;
                if (!MycelialOutbreakFeature.generateOutbreakAt(level, cave, random)) continue;
                MycelialOutbreakFeature.buildFissure(level, surface, random, false);
                for (BlockPos step : route) {
                    BlockState floorState = level.getBlockState(step);
                    if (!(floorState.is(BlockTags.BASE_STONE_OVERWORLD) || floorState.is(BlockTags.DIRT)
                            || floorState.is(ModBlocks.MYCELIAL_STONE.get()))) {
                        level.setBlock(step, Blocks.STONE.defaultBlockState(), 2);
                    }
                    for (int height = 1; height <= 3; height++) {
                        level.setBlock(step.above(height), Blocks.CAVE_AIR.defaultBlockState(), 2);
                    }
                }
                // Overlapping, uneven ellipsoids turn the narrow route into a cave-like fissure.
                // The planned floor is retained so the descent and mycelial growth remain continuous.
                for (int i = 0; i < route.size(); i++) {
                    BlockPos step = route.get(i);
                    for (int dx = -2; dx <= 2; dx++) {
                        for (int dz = -2; dz <= 2; dz++) {
                            for (int h = 1; h <= 4; h++) {
                                double shape = (dx * dx + dz * dz) / 5.5D
                                        + (h - 1.8D) * (h - 1.8D) / 5.0D;
                                double irregularity = ((i * 17 + dx * 13 + dz * 7 + h * 11) & 7) * 0.035D;
                                BlockPos air = step.offset(dx, h, dz);
                                if (shape + irregularity >= 1.0D || air.equals(cave) || air.equals(cave.below())
                                        || isRouteFloor(route, air) || !safeExcavation(level, tracker, air)) continue;
                                if (!level.getBlockState(air).isAir())
                                    level.setBlock(air, Blocks.CAVE_AIR.defaultBlockState(), 2);
                            }
                        }
                    }
                }
                // A visible organic edge marks the opening; living mycelium grows later from the cave node.
                BlockPos rim = surface.relative(Direction.from2DDataValue(rotation / 4), 4);
                if (WorldgenFeatureSupport.isSafeToOverwrite(level, rim)
                        && !level.getBlockState(rim).is(ModBlocks.HIVE_PROTECTED_BLOCKS)
                        && natural(level.getBlockState(rim)) && level.getFluidState(rim).isEmpty()) {
                    level.setBlock(rim, ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState(), 2);
                }
                return true;
            }
        }
        return false;
    }

    private static boolean intersectsRoute(List<BlockPos> route, BlockPos pos) {
        for (BlockPos floor : route) {
            if (pos.getX() == floor.getX() && pos.getZ() == floor.getZ()
                    && pos.getY() >= floor.getY() && pos.getY() <= floor.getY() + 3) return true;
        }
        return false;
    }

    private static boolean isRouteFloor(List<BlockPos> route, BlockPos pos) {
        return route.contains(pos);
    }

    private static boolean safeExcavation(WorldGenLevel level, PurificationZoneTracker tracker, BlockPos pos) {
        if (!WorldgenFeatureSupport.isSafeToOverwrite(level, pos) || tracker.isProtected(pos)
                || level.getBlockState(pos).is(ModBlocks.HIVE_PROTECTED_BLOCKS)
                || !level.getFluidState(pos).isEmpty() || !natural(level.getBlockState(pos))) return false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = pos.relative(direction);
            if (level.isOutsideBuildHeight(neighbor) || !level.getFluidState(neighbor).isEmpty()) return false;
        }
        return true;
    }

    private static boolean safeRouteCell(WorldGenLevel level, PurificationZoneTracker tracker, BlockPos floor) {
        for (int height = 0; height <= 3; height++) {
            BlockPos pos = floor.above(height);
            if (!WorldgenFeatureSupport.isSafeToOverwrite(level, pos) || tracker.isProtected(pos)
                    || level.getBlockState(pos).is(ModBlocks.HIVE_PROTECTED_BLOCKS)
                    || !level.getFluidState(pos).isEmpty() || !natural(level.getBlockState(pos))) return false;
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = pos.relative(direction);
                if (level.isOutsideBuildHeight(neighbor) || !level.getFluidState(neighbor).isEmpty()) return false;
            }
        }
        return true;
    }

    private static boolean natural(BlockState state) {
        return state.isAir() || state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND) || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY) || state.is(Blocks.MUD)
                || state.is(Blocks.SNOW_BLOCK);
    }
}
