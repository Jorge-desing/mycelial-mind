package com.alex.mycelialmind.world;

import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Generates a low mycelial fissure around a synaptic node. */
public class MycelialOutbreakFeature extends Feature<NoneFeatureConfiguration> {
    public MycelialOutbreakFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    public static final int MIN_OUTBREAK_SPACING = 56;

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return placeInternal(context.level(), context.origin(), context.random(), true);
    }

    public static boolean generateOutbreakAt(WorldGenLevel level, BlockPos origin, RandomSource random) {
        return placeInternal(level, origin, random, false);
    }

    private static boolean placeInternal(
            WorldGenLevel level,
            BlockPos origin,
            RandomSource random,
            boolean enforceWorldgenGates
    ) {
        BlockPos center = findAnchor(level, origin);
        if (center == null) {
            return false;
        }
        boolean isOverworld = level.getLevel() instanceof ServerLevel sl && sl.dimension().equals(Level.OVERWORLD);
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, center.getX(), center.getZ());
        boolean isSubterranean = center.getY() < surfaceY - 6;

        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(level);
        if (isSubterranean && enforceWorldgenGates && !TerrainPlacementPolicy.hasSafeSubterraneanOutbreakFooting(level, center)) {
            return false;
        }
        if (isOverworld && !isSubterranean) {
            if (enforceWorldgenGates) {
                if (TerrainPlacementPolicy.isExcludedSurfaceBiome(level, center)
                        || TerrainPlacementPolicy.isNearVanillaSurfaceStructure(level, center, 1)) {
                    return false;
                }
                int minSpawnDist = StructureProgressionPolicy.surfaceOutbreakMinSpawnDistance(tier);
                if (level.getLevel() instanceof ServerLevel serverLevel) {
                    BlockPos spawn = serverLevel.getSharedSpawnPos();
                    if (TerrainPlacementPolicy.isWithinSpawnSafeZone(
                            center.getX(), center.getZ(), spawn.getX(), spawn.getZ(), minSpawnDist)) {
                        return false;
                    }
                }
            }
            if (!TerrainPlacementPolicy.hasSuitableTwoRingSurfaceTerrain(level, center, 2, 4, 3, 3)) {
                return false;
            }
        }

        if (level.isOutsideBuildHeight(center.below())
                || !level.getBlockState(center.below()).isSolid()
                || !level.getFluidState(center).isEmpty()) {
            return false;
        }

        SynapticNodeType nodeType = StructureProgressionPolicy.pickOutbreakNodeType(tier, random.nextFloat());
        int minSpacing = StructureProgressionPolicy.outbreakMinSpacing(tier);
        if (level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            if (tracker.isProtected(center) || !tracker.tryReserveNodePosition(center, minSpacing, nodeType)) {
                return false;
            }
        }

        BlockState nodeState = switch (nodeType) {
            case SENTINEL -> ModBlocks.SENTINEL_NODE.get().defaultBlockState();
            case INCUBATOR -> ModBlocks.INCUBATOR_NODE.get().defaultBlockState();
            case RELAY, SYMBIOTIC -> ModBlocks.MYCELIAL_NODE.get().defaultBlockState();
        };
        int headroom = measureHeadroom(level, center, 9);
        level.setBlock(center, nodeState, 2);
        buildFissure(level, center, random, isSubterranean);

        BlockPos vaultCenter = center.below(4);
        if (random.nextFloat() < StructureProgressionPolicy.sealedBioVaultChance(tier)
                && canPlaceSealedBioVault(level, vaultCenter)) {
            placeSealedBioVault(level, vaultCenter, random, tier);
        }

        // Overworld subterranean <-> surface hyphal taproot & linked relay connection
        if (isOverworld) {
            if (isSubterranean) {
                int depthBelowSurface = surfaceY - center.getY();
                if (depthBelowSurface >= 8 && depthBelowSurface <= 72
                        && random.nextFloat() < StructureProgressionPolicy.surfaceBreachChance(tier)) {
                    connectSubterraneanToSurface(level, center, surfaceY, Math.max(2, headroom), random);
                }
            } else {
                connectSurfaceToSubterranean(level, center, random);
            }
        }
        return true;
    }

    private static void buildFissure(WorldGenLevel level, BlockPos center, RandomSource random, boolean underground) {
        Direction axis = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Direction side = axis.getClockWise();
        int halfLength = 4 + random.nextInt(3);
        int bend = random.nextBoolean() ? 1 : -1;
        for (int sign : new int[]{-1, 1}) {
            int previousY = center.getY();
            for (int distance = 1; distance <= halfLength; distance++) {
                int shift = distance >= 3 ? bend : 0;
                BlockPos column = center.relative(axis, sign * distance).relative(side, shift);
                BlockPos floor = findSlopeFloor(level, column.getX(), previousY, column.getZ(), 1);
                if (floor == null || Math.abs(floor.getY() - previousY) > 1 || !canExcavateFissure(level, floor)) {
                    break;
                }
                previousY = floor.getY();
                carveFissureCell(level, floor, distance == 1 || (distance == 2 && random.nextBoolean()));
                if (distance == 3) {
                    BlockPos connector = floor.relative(side, -bend);
                    if (canExcavateFissure(level, connector)) {
                        carveFissureCell(level, connector, false);
                    }
                }
                if (distance <= 2 || (distance == 3 && sign == 1)) {
                    BlockPos branch = floor.relative(side, sign == 1 ? 1 : -1);
                    if (canExcavateFissure(level, branch)) {
                        carveFissureCell(level, branch, distance == 1);
                    }
                }
                BlockPos lip = floor.relative(side, sign == 1 ? -1 : 1);
                if (isSafeToOverwrite(level, lip) && canReplaceGround(level.getBlockState(lip))
                        && level.getFluidState(lip).isEmpty()) {
                    level.setBlock(lip, ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 2);
                }
                BlockPos rim = floor.relative(side, sign == 1 ? 2 : -2);
                if (isSafeToOverwrite(level, rim) && canReplaceGround(level.getBlockState(rim))
                        && level.getFluidState(rim).isEmpty()) {
                    level.setBlock(rim, ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 2);
                    if (distance % 2 == 0 && isOpenAirOrReplaceable(level, rim.above())) {
                        level.setBlock(rim.above(), (distance == 2 ? ModBlocks.SPORE_VENT.get()
                                : ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()).defaultBlockState(), 2);
                    }
                }
                if (underground && distance <= 3) {
                    for (int height = 3; height <= 8; height++) {
                        BlockPos ceiling = floor.above(height);
                        if (level.isOutsideBuildHeight(ceiling)) {
                            break;
                        }
                        if (level.getBlockState(ceiling).isSolid()) {
                            if (isOpenAirOrReplaceable(level, ceiling.below()) && random.nextBoolean()) {
                                level.setBlock(ceiling.below(), ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState(), 2);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean canExcavateFissure(WorldGenLevel level, BlockPos floor) {
        BlockPos bottom = floor.below();
        return !level.isOutsideBuildHeight(bottom)
                && isSafeToOverwrite(level, floor)
                && isSafeToOverwrite(level, bottom)
                && canReplaceGround(level.getBlockState(floor))
                && canReplaceGround(level.getBlockState(bottom))
                && level.getBlockState(bottom).isSolid()
                && level.getFluidState(floor).isEmpty()
                && level.getFluidState(bottom).isEmpty()
                && isOpenAirOrReplaceable(level, floor.above());
    }

    private static void carveFissureCell(WorldGenLevel level, BlockPos floor, boolean luminous) {
        level.setBlock(floor.below(), (luminous ? ModBlocks.LUMINESCENT_AMBER_VEIN.get()
                : ModBlocks.MYCELIAL_STONE.get()).defaultBlockState(), 2);
        level.setBlock(floor, Blocks.AIR.defaultBlockState(), 2);
    }

    public static void placeSealedBioVault(
            WorldGenLevel level,
            BlockPos vaultCenter,
            RandomSource random,
            HiveProgressionTier tier
    ) {
        if (level.isOutsideBuildHeight(vaultCenter) || !isSafeToOverwrite(level, vaultCenter)) {
            return;
        }
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = vaultCenter.offset(dx, dy, dz);
                    if (level.isOutsideBuildHeight(p) || !isSafeToOverwrite(level, p)) {
                        continue;
                    }
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    boolean isCorner = Math.abs(dx) == 1 && Math.abs(dy) == 1 && Math.abs(dz) == 1;
                    level.setBlock(p, isCorner ? amberVein : archBrick, 2);
                }
            }
        }
        if (!level.isOutsideBuildHeight(vaultCenter.above(2)) && isSafeToOverwrite(level, vaultCenter.above(2))) {
            level.setBlock(vaultCenter.above(2), amberVein, 2);
        }
        level.setBlock(vaultCenter, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(vaultCenter) instanceof ChestBlockEntity chest) {
            MycelialLootSystem.populateStructureChest(
                    level,
                    vaultCenter,
                    chest,
                    MycelialLootSystem.selectSealedBioVaultTable(tier, random),
                    random,
                    tier,
                    false
            );
        }
    }

    private static boolean canPlaceSealedBioVault(WorldGenLevel level, BlockPos vaultCenter) {
        if (level.isOutsideBuildHeight(vaultCenter.below())
                || level.isOutsideBuildHeight(vaultCenter.above(2))
                || !isSafeToOverwrite(level, vaultCenter.above(2))
                || !level.getFluidState(vaultCenter.above(2)).isEmpty()) {
            return false;
        }
        int solidShell = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = vaultCenter.offset(dx, dy, dz);
                    if (level.isOutsideBuildHeight(p)
                            || !isSafeToOverwrite(level, p)
                            || !level.getFluidState(p).isEmpty()) {
                        return false;
                    }
                    boolean solid = level.getBlockState(p).isSolid();
                    if (dy == -1 && !solid) {
                        return false;
                    }
                    if (!(dx == 0 && dy == 0 && dz == 0) && solid) {
                        solidShell++;
                    }
                }
            }
        }
        return solidShell >= 20;
    }

    private static BlockPos findAnchor(WorldGenLevel level, BlockPos origin) {
        for (int step = 0; step <= 16; step++) {
            int[] offsets = step == 0 ? new int[]{0} : new int[]{step, -step};
            for (int dy : offsets) {
                BlockPos candidate = origin.offset(0, dy, 0);
                if (!level.isOutsideBuildHeight(candidate)
                        && !level.isOutsideBuildHeight(candidate.above())
                        && isSafeToOverwrite(level, candidate)
                        && canReplaceGround(level.getBlockState(candidate))
                        && isOpenAirOrReplaceable(level, candidate.above())
                        && level.getFluidState(candidate.above()).isEmpty()) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private static BlockPos findSlopeFloor(WorldGenLevel level, int x, int baseY, int z, int maxDeltaY) {
        for (int step = 0; step <= maxDeltaY; step++) {
            int[] offsets = step == 0 ? new int[]{0} : new int[]{step, -step};
            for (int dy : offsets) {
                BlockPos p = new BlockPos(x, baseY + dy, z);
                if (!level.isOutsideBuildHeight(p)
                        && !level.isOutsideBuildHeight(p.above())
                        && isSafeToOverwrite(level, p)
                        && canReplaceGround(level.getBlockState(p))
                        && isOpenAirOrReplaceable(level, p.above())) {
                    return p;
                }
            }
        }
        return null;
    }

    private static boolean isSafeToOverwrite(WorldGenLevel level, BlockPos pos) {
        return WorldgenFeatureSupport.isSafeToOverwrite(level, pos);
    }

    private static boolean isOpenAirOrReplaceable(WorldGenLevel level, BlockPos pos) {
        return WorldgenFeatureSupport.isOpenAirOrReplaceable(level, pos);
    }

    private static int measureHeadroom(WorldGenLevel level, BlockPos center, int maxCheck) {
        int clear = 0;
        for (int dy = 1; dy <= maxCheck; dy++) {
            if (!isOpenAirOrReplaceable(level, center.above(dy))) {
                break;
            }
            clear = dy;
        }
        return clear;
    }

    private static void connectSubterraneanToSurface(
            WorldGenLevel level,
            BlockPos caveCenter,
            int surfaceY,
            int headroom,
            RandomSource random
    ) {
        BlockPos surfaceAnchor = null;
        int[] offsets = {0, -1, 1, -2, 2, -3};
        for (int dy : offsets) {
            BlockPos cand = new BlockPos(caveCenter.getX(), surfaceY + dy, caveCenter.getZ());
            if (!level.isOutsideBuildHeight(cand)
                    && !level.isOutsideBuildHeight(cand.above())
                    && isSafeToOverwrite(level, cand)
                    && canReplaceGround(level.getBlockState(cand))
                    && isOpenAirOrReplaceable(level, cand.above())
                    && level.getFluidState(cand.above()).isEmpty()) {
                surfaceAnchor = cand;
                break;
            }
        }
        if (surfaceAnchor == null) {
            return;
        }
        int deltaY = surfaceAnchor.getY() - caveCenter.getY();
        if (deltaY < 8 || deltaY > 76) {
            return;
        }
        if (level.getLevel() instanceof ServerLevel serverLevel
                && PurificationZoneTracker.get(serverLevel).isProtected(surfaceAnchor)) {
            return;
        }

        BlockPos tapStart = caveCenter.above(Math.min(headroom + 1, 9));
        buildVerticalTaproot(level, tapStart, surfaceAnchor.below(), random);
        spreadMiniSporePatch(level, surfaceAnchor, 3, random);

        if (level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            if (!tracker.hasAnyNodeWithin(surfaceAnchor, 16) && isSafeToOverwrite(level, surfaceAnchor)) {
                level.setBlock(surfaceAnchor, ModBlocks.MYCELIAL_NODE.get().defaultBlockState(), 2);
                tracker.addNode(surfaceAnchor, SynapticNodeType.RELAY);
            }
        }
    }

    private static void connectSurfaceToSubterranean(
            WorldGenLevel level,
            BlockPos surfaceCenter,
            RandomSource random
    ) {
        int minY = Math.max(level.getMinBuildHeight() + 8, surfaceCenter.getY() - 72);
        BlockPos caveFloor = null;
        for (int y = surfaceCenter.getY() - 8; y >= minY; y--) {
            BlockPos cand = new BlockPos(surfaceCenter.getX(), y, surfaceCenter.getZ());
            if (!level.isOutsideBuildHeight(cand)
                    && !level.isOutsideBuildHeight(cand.above(2))
                    && isSafeToOverwrite(level, cand)
                    && canReplaceGround(level.getBlockState(cand))
                    && isOpenAirOrReplaceable(level, cand.above())
                    && isOpenAirOrReplaceable(level, cand.above(2))
                    && level.getFluidState(cand.above()).isEmpty()) {
                caveFloor = cand;
                break;
            }
        }
        if (caveFloor == null) {
            return;
        }
        if (level.getLevel() instanceof ServerLevel serverLevel
                && PurificationZoneTracker.get(serverLevel).isProtected(caveFloor)) {
            return;
        }

        int ceilingY = caveFloor.getY() + 3;
        while (ceilingY < surfaceCenter.getY() - 2 && isOpenAirOrReplaceable(level, new BlockPos(caveFloor.getX(), ceilingY, caveFloor.getZ()))) {
            ceilingY++;
        }
        BlockPos ceilingPos = new BlockPos(caveFloor.getX(), ceilingY, caveFloor.getZ());
        buildVerticalTaproot(level, ceilingPos, surfaceCenter.below(2), random);
        if (isOpenAirOrReplaceable(level, ceilingPos.below())) {
            level.setBlock(ceilingPos.below(), ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState(), 2);
        }
        spreadMiniSporePatch(level, caveFloor, 3, random);

        if (level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            if (!tracker.hasAnyNodeWithin(caveFloor, 16) && isSafeToOverwrite(level, caveFloor)) {
                level.setBlock(caveFloor, ModBlocks.MYCELIAL_NODE.get().defaultBlockState(), 2);
                tracker.addNode(caveFloor, SynapticNodeType.RELAY);
            }
        }
    }

    private static void buildVerticalTaproot(
            WorldGenLevel level,
            BlockPos bottom,
            BlockPos top,
            RandomSource random
    ) {
        int startY = Math.min(bottom.getY(), top.getY());
        int endY = Math.max(bottom.getY(), top.getY());
        int baseX = bottom.getX();
        int baseZ = bottom.getZ();
        int curX = baseX;
        int curZ = baseZ;
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState stone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();

        for (int y = startY; y <= endY; y++) {
            int step = y - startY;
            if (step > 1 && y < endY - 1 && step % 5 == 0) {
                int nextX = curX + (random.nextInt(3) - 1);
                int nextZ = curZ + (random.nextInt(3) - 1);
                if (Math.abs(nextX - baseX) <= 1) {
                    curX = nextX;
                }
                if (Math.abs(nextZ - baseZ) <= 1) {
                    curZ = nextZ;
                }
            } else if (y >= endY - 1) {
                curX = baseX;
                curZ = baseZ;
            }
            BlockPos p = new BlockPos(curX, y, curZ);
            if (isSafeToOverwrite(level, p) && level.getFluidState(p).isEmpty()) {
                BlockState state = (step % 4 == 0) ? amberVein : ((step % 2 == 0) ? archBrick : stone);
                level.setBlock(p, state, 2);
            }
        }
    }

    private static void spreadMiniSporePatch(
            WorldGenLevel level,
            BlockPos anchor,
            int radius,
            RandomSource random
    ) {
        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                if (dx * dx + dz * dz > radius * radius + 1 || random.nextFloat() > 0.78F) {
                    continue;
                }
                BlockPos floor = findSlopeFloor(level, anchor.getX() + dx, anchor.getY(), anchor.getZ() + dz, 1);
                if (floor != null && isSafeToOverwrite(level, floor) && canReplaceGround(level.getBlockState(floor))) {
                    level.setBlock(floor, mycelium, 2);
                    if (isOpenAirOrReplaceable(level, floor.above())) {
                        float roll = random.nextFloat();
                        if (roll < 0.16F) {
                            level.setBlock(floor.above(), ModBlocks.SPORE_VENT.get().defaultBlockState(), 2);
                        } else if (roll < 0.36F) {
                            level.setBlock(floor.above(), ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState(), 2);
                        } else if (roll < 0.50F) {
                            level.setBlock(floor.above(), ModBlocks.SPORE_BULB.get().defaultBlockState(), 2);
                        }
                    }
                }
            }
        }
    }

    private static boolean canReplaceGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.SAND)
                || state.is(BlockTags.TERRACOTTA)
                || state.is(Blocks.GRAVEL)
                || state.is(Blocks.MUD)
                || state.is(Blocks.CLAY)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(ModBlocks.MYCELIAL_STONE.get())
                || state.is(ModBlocks.CREEPING_MYCELIUM.get());
    }
}
