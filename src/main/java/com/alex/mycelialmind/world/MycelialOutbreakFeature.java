package com.alex.mycelialmind.world;

import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import com.alex.mycelialmind.world.structure.VoxelStructureOps;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * Generates an Organic-Alien 3D Mycelial Outbreak Fungus-Claw (height 9) following
 * 70/20/10 form hierarchy and strict 6-neighbor physical connectivity (zero floating blocks).
 */
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

        SynapticNodeType nodeType = StructureProgressionPolicy.pickOutbreakNodeType(tier, random.nextFloat());
        int minSpacing = StructureProgressionPolicy.outbreakMinSpacing(tier);
        if (level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            if (tracker.isProtected(center) || !tracker.tryReserveNodePosition(center, minSpacing, nodeType)) {
                return false;
            }
        }

        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState chitinPillar = ModBlocks.CHITIN_PILLAR.get().defaultBlockState();
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState stone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
        BlockState deepslateTiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState purpleGlass = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState stairBase = Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState();
        BlockState wall = Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();

        // Pedestal beneath central node
        if (isSafeToOverwrite(level, center.below()) && canReplaceGround(level.getBlockState(center.below()))) {
            level.setBlock(center.below(), archBrick, 2);
        }

        BlockState nodeState = switch (nodeType) {
            case SENTINEL -> ModBlocks.SENTINEL_NODE.get().defaultBlockState();
            case INCUBATOR -> ModBlocks.INCUBATOR_NODE.get().defaultBlockState();
            case RELAY, SYMBIOTIC -> ModBlocks.MYCELIAL_NODE.get().defaultBlockState();
        };
        level.setBlock(center, nodeState, 2);

        // Stepped cardinal plinth stairs: visible on flat ground (stepPos.above()) and recessed on slopes (stepPos)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos stepPos = center.relative(dir);
            BlockState stair = stairBase.setValue(StairBlock.FACING, dir.getOpposite());
            if (isOpenAirOrReplaceable(level, stepPos)
                    && !level.isOutsideBuildHeight(stepPos.below())
                    && level.getBlockState(stepPos.below()).isSolid()) {
                level.setBlock(stepPos, stair, 2);
            } else if (!level.isOutsideBuildHeight(stepPos)
                    && level.getBlockState(stepPos).isFaceSturdy(level, stepPos, Direction.UP)
                    && isOpenAirOrReplaceable(level, stepPos.above())) {
                level.setBlock(stepPos.above(), stair, 2);
            }
        }
        for (int sx : new int[]{-2, 2}) {
            for (int sz : new int[]{-2, 2}) {
                BlockPos corner = center.offset(sx, 0, sz);
                if (!isSafeToOverwrite(level, corner)) {
                    continue;
                }
                if (canReplaceGround(level.getBlockState(corner))) {
                    level.setBlock(corner, archBrick, 2);
                } else if (isOpenAirOrReplaceable(level, corner) && anchorColumnDown(level, corner.below(), 6, archBrick)) {
                    level.setBlock(corner, archBrick, 2);
                }
                if (level.getBlockState(corner).is(archBrick.getBlock()) && isOpenAirOrReplaceable(level, corner.above())) {
                    level.setBlock(corner.above(), wall, 2);
                }
            }
        }

        // 70/20/10 6-connected curving fungal stalk, counter-rib & stratified cap (height 9)
        // or compact ceiling-welded cave crown in low/medium subterranean caverns (headroom 4..8)
        int headroom = measureHeadroom(level, center, 9);
        boolean hasHeadroom = headroom >= 9;
        Direction leanDir = hasHeadroom ? pickSupportedLeanDirection(level, center, random) : null;
        if (leanDir != null) {
            BoundingBox canopyBox = new BoundingBox(
                    center.getX() - 4, center.getY() - 2, center.getZ() - 4,
                    center.getX() + 4, center.getY() + 9, center.getZ() + 4
            );
            Direction opp = leanDir.getOpposite();

            BlockPos baseStalk = center.relative(leanDir, 2);
            anchorBaseSupport(level, baseStalk, archBrick);
            List<BlockPos> mainStalkWaypoints = List.of(
                    baseStalk,
                    baseStalk.above(1),
                    baseStalk.above(2),
                    center.relative(leanDir, 1).above(4),
                    center.above(6)
            );
            VoxelStructureOps.placeFaceConnectedPath(
                    level, canopyBox,
                    mainStalkWaypoints,
                    step -> (step % 2 == 0) ? chitinPillar : archBrick
            );

            BlockPos oppBase = center.relative(opp, 2);
            anchorBaseSupport(level, oppBase, archBrick);
            List<BlockPos> counterRibWaypoints = List.of(
                    oppBase,
                    oppBase.above(2),
                    center.relative(opp, 1).above(3),
                    center.above(6)
            );
            VoxelStructureOps.placeFaceConnectedPath(
                    level, canopyBox,
                    counterRibWaypoints,
                    step -> (step == 0) ? chitinPillar : archBrick
            );

            // Deterministic stratified Fungal Cap (dy = 6..8, radius 3) without overwriting solid walls/BlockEntities
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double r = Math.sqrt(dx * dx + dz * dz);
                    if (r <= 3.1) {
                        BlockPos rim = center.offset(dx, 6, dz);
                        if (r >= 2.2) {
                            if (isOpenAirOrReplaceable(level, rim)) {
                                level.setBlock(rim, ((Math.abs(dx) + Math.abs(dz)) % 2 == 0) ? purpleGlass : deepslateTiles, 2);
                            }
                            if (level.getBlockState(rim).isSolid()
                                    && isOpenAirOrReplaceable(level, rim.below())
                                    && (Math.abs(dx) + Math.abs(dz)) % 3 == 0) {
                                level.setBlock(rim.below(), ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState(), 2);
                            }
                        } else {
                            if (isOpenAirOrReplaceable(level, rim) || isOwnCanopyBlock(level.getBlockState(rim))) {
                                level.setBlock(rim, deepslateTiles, 2);
                            }
                            BlockPos upperRim = rim.above();
                            if (isOpenAirOrReplaceable(level, upperRim) || isOwnCanopyBlock(level.getBlockState(upperRim))) {
                                level.setBlock(upperRim, (r <= 1.2) ? amethyst : stone, 2);
                            }
                        }
                    }
                    if (r <= 1.8) {
                        BlockPos midCap = center.offset(dx, 7, dz);
                        if (isOpenAirOrReplaceable(level, midCap) || isOwnCanopyBlock(level.getBlockState(midCap))) {
                            level.setBlock(midCap, (r <= 0.8) ? amethyst : deepslateTiles, 2);
                        }
                    }
                }
            }
            if (isOpenAirOrReplaceable(level, center.above(8)) || isOwnCanopyBlock(level.getBlockState(center.above(8)))) {
                level.setBlock(center.above(8), amberVein, 2);
            }
            VoxelStructureOps.placeAnchoredChain(level, canopyBox, center.above(5), 1, archBrick, amberVein);
        } else if (isSubterranean && headroom >= 4) {
            buildCompactCaveCrown(level, center, headroom, random, archBrick, chitinPillar, amberVein, amethyst, deepslateTiles);
        }

        // WFC core mosaic + Domain-warped radial root veins (radius 6, slope-adaptive ±2 Y)
        long noiseSeed = center.asLong() ^ 0x4D5943454C49414CL;
        WfcCavernLayout.Tile[][] wfc = WfcCavernLayout.generate(5, 5, noiseSeed);
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                double distSq = dx * dx + dz * dz;
                var cell = MycelialFastNoise.domainWarpedCellular2D(
                        (center.getX() + dx) * 0.24D,
                        (center.getZ() + dz) * 0.24D,
                        noiseSeed,
                        0.40D
                );
                boolean inCore = distSq <= 7.5D;
                boolean alongVein = distSq <= 36.0D && cell.edge() >= 0.40D;
                if ((!inCore && !alongVein) || random.nextFloat() > 0.86F) {
                    continue;
                }
                boolean isCardinalAdjacent = Math.abs(dx) + Math.abs(dz) == 1;
                BlockPos floor = isCardinalAdjacent
                        ? center.offset(dx, 0, dz)
                        : findSlopeFloor(level, center.getX() + dx, center.getY(), center.getZ() + dz, 2);
                if (floor != null && isSafeToOverwrite(level, floor) && canReplaceGround(level.getBlockState(floor))) {
                    BlockState groundBlock = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
                    if (Math.abs(dx) <= 2 && Math.abs(dz) <= 2) {
                        WfcCavernLayout.Tile tile = wfc[dx + 2][dz + 2];
                        if (tile == WfcCavernLayout.Tile.STONE) {
                            groundBlock = archBrick;
                        } else if (tile == WfcCavernLayout.Tile.MYCELIUM_EDGE) {
                            groundBlock = deepslateTiles;
                        }
                    }
                    level.setBlock(floor, groundBlock, 2);

                    if (isOpenAirOrReplaceable(level, floor.above())) {
                        float floraRoll = random.nextFloat();
                        if (floraRoll < 0.15F) {
                            level.setBlock(floor.above(), ModBlocks.SPORE_VENT.get().defaultBlockState(), 2);
                        } else if (floraRoll < 0.36F) {
                            level.setBlock(floor.above(), ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState(), 2);
                        } else if (floraRoll < 0.50F) {
                            level.setBlock(floor.above(), ModBlocks.SPORE_BULB.get().defaultBlockState(), 2);
                        }
                    }

                    for (int cy = 3; cy <= 8; cy++) {
                        BlockPos ceiling = floor.above(cy);
                        if (level.isOutsideBuildHeight(ceiling)) {
                            break;
                        }
                        if (level.getBlockState(ceiling).isSolid() && isOpenAirOrReplaceable(level, ceiling.below())) {
                            if (random.nextFloat() < 0.28F) {
                                level.setBlock(ceiling.below(), ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState(), 2);
                            }
                            break;
                        }
                    }
                }
            }
        }

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

    private static Direction pickSupportedLeanDirection(WorldGenLevel level, BlockPos center, RandomSource random) {
        Direction start = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        for (int i = 0; i < 4; i++) {
            Direction opp = start.getOpposite();
            BlockPos base = center.relative(start, 2);
            BlockPos oppBase = center.relative(opp, 2);
            if (isBaseAnchorable(level, base)
                    && isBaseAnchorable(level, oppBase)
                    && isOpenAirOrReplaceable(level, base.above(1))
                    && isOpenAirOrReplaceable(level, base.above(2))
                    && isOpenAirOrReplaceable(level, center.relative(start, 1).above(2))
                    && isOpenAirOrReplaceable(level, center.relative(start, 1).above(3))
                    && isOpenAirOrReplaceable(level, center.relative(start, 1).above(4))
                    && isOpenAirOrReplaceable(level, oppBase.above(1))
                    && isOpenAirOrReplaceable(level, oppBase.above(2))
                    && isOpenAirOrReplaceable(level, center.relative(opp, 1).above(2))
                    && isOpenAirOrReplaceable(level, center.relative(opp, 1).above(3))) {
                return start;
            }
            start = start.getClockWise();
        }
        return null;
    }

    private static boolean isBaseAnchorable(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || !isSafeToOverwrite(level, pos)) {
            return false;
        }
        if (canReplaceGround(level.getBlockState(pos))) {
            return true;
        }
        if (!isOpenAirOrReplaceable(level, pos)) {
            return false;
        }
        for (int d = 1; d <= 6; d++) {
            BlockPos below = pos.below(d);
            if (level.isOutsideBuildHeight(below)) {
                return false;
            }
            if (level.getBlockState(below).isSolid()) {
                return true;
            }
        }
        return false;
    }

    private static boolean anchorColumnDown(WorldGenLevel level, BlockPos startPos, int maxDepth, BlockState fill) {
        for (int d = 0; d < maxDepth; d++) {
            BlockPos probe = startPos.below(d);
            if (level.isOutsideBuildHeight(probe)) {
                return false;
            }
            if (level.getBlockState(probe).isSolid()) {
                for (int f = 0; f < d; f++) {
                    BlockPos fillPos = startPos.below(f);
                    if (isSafeToOverwrite(level, fillPos)) {
                        level.setBlock(fillPos, fill, 2);
                    }
                }
                return true;
            }
            if (!isOpenAirOrReplaceable(level, probe)) {
                return false;
            }
        }
        BlockPos bottom = startPos.below(maxDepth);
        if (!level.isOutsideBuildHeight(bottom) && level.getBlockState(bottom).isSolid()) {
            for (int f = 0; f < maxDepth; f++) {
                BlockPos fillPos = startPos.below(f);
                if (isSafeToOverwrite(level, fillPos)) {
                    level.setBlock(fillPos, fill, 2);
                }
            }
            return true;
        }
        return false;
    }

    private static void anchorBaseSupport(WorldGenLevel level, BlockPos basePos, BlockState archBrick) {
        if (!isSafeToOverwrite(level, basePos)) {
            return;
        }
        if (canReplaceGround(level.getBlockState(basePos))) {
            level.setBlock(basePos, archBrick, 2);
        } else if (isOpenAirOrReplaceable(level, basePos) && anchorColumnDown(level, basePos.below(), 6, archBrick)) {
            level.setBlock(basePos, archBrick, 2);
        }
    }

    private static boolean isSafeToOverwrite(WorldGenLevel level, BlockPos pos) {
        return WorldgenFeatureSupport.isSafeToOverwrite(level, pos);
    }

    private static boolean isOpenAirOrReplaceable(WorldGenLevel level, BlockPos pos) {
        return WorldgenFeatureSupport.isOpenAirOrReplaceable(level, pos);
    }

    private static boolean isOwnCanopyBlock(BlockState state) {
        return state.is(ModBlocks.MYCELIAL_ARCH_BRICK.get())
                || state.is(ModBlocks.CHITIN_PILLAR.get())
                || state.is(ModBlocks.LUMINESCENT_AMBER_VEIN.get())
                || state.is(ModBlocks.MYCELIAL_STONE.get())
                || state.is(Blocks.DEEPSLATE_TILES)
                || state.is(Blocks.PURPLE_STAINED_GLASS)
                || state.is(Blocks.AMETHYST_BLOCK);
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

    private static void buildCompactCaveCrown(
            WorldGenLevel level,
            BlockPos center,
            int headroom,
            RandomSource random,
            BlockState archBrick,
            BlockState chitinPillar,
            BlockState amberVein,
            BlockState amethyst,
            BlockState deepslateTiles
    ) {
        int crownY = headroom;
        BoundingBox box = new BoundingBox(
                center.getX() - 3, center.getY() - 2, center.getZ() - 3,
                center.getX() + 3, center.getY() + crownY + 1, center.getZ() + 3
        );
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos base = center.relative(dir, 2);
        if (isBaseAnchorable(level, base)) {
            anchorBaseSupport(level, base, archBrick);
            VoxelStructureOps.placeFaceConnectedPath(
                    level, box,
                    List.of(
                            base,
                            base.above(1),
                            center.relative(dir, 1).above(Math.max(2, crownY - 1)),
                            center.above(crownY)
                    ),
                    step -> (step % 2 == 0) ? chitinPillar : archBrick
            );
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r > 2.15D) {
                    continue;
                }
                BlockPos capPos = center.offset(dx, crownY, dz);
                if (isOpenAirOrReplaceable(level, capPos) || isOwnCanopyBlock(level.getBlockState(capPos))) {
                    BlockState st = (dx == 0 && dz == 0) ? amberVein : (r <= 1.1D ? amethyst : deepslateTiles);
                    level.setBlock(capPos, st, 2);
                }
                if (r >= 1.0D && level.getBlockState(capPos).isSolid()
                        && isOpenAirOrReplaceable(level, capPos.below())
                        && (Math.abs(dx) + Math.abs(dz)) % 2 == 0) {
                    level.setBlock(capPos.below(), ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState(), 2);
                }
            }
        }
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
