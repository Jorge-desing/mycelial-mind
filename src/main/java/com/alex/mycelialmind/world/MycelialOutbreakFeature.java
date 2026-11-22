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
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        RandomSource random = context.random();

        BlockPos center = findAnchor(level, origin);
        if (center == null) {
            return false;
        }
        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(level);
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
                } else if (isOpenAirOrReplaceable(level, corner) && anchorColumnDown(level, corner.below(), 2, archBrick)) {
                    level.setBlock(corner, archBrick, 2);
                }
                if (level.getBlockState(corner).is(archBrick.getBlock()) && isOpenAirOrReplaceable(level, corner.above())) {
                    level.setBlock(corner.above(), wall, 2);
                }
            }
        }

        // 70/20/10 6-connected curving fungal stalk, counter-rib & stratified cap (height 9)
        boolean hasHeadroom = true;
        for (int dy = 1; dy <= 9; dy++) {
            if (!isOpenAirOrReplaceable(level, center.above(dy))) {
                hasHeadroom = false;
                break;
            }
        }
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
        for (int d = 1; d <= 2; d++) {
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
        } else if (isOpenAirOrReplaceable(level, basePos) && anchorColumnDown(level, basePos.below(), 2, archBrick)) {
            level.setBlock(basePos, archBrick, 2);
        }
    }

    private static boolean isSafeToOverwrite(WorldGenLevel level, BlockPos pos) {
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
                && !state.is(ModBlocks.MYCELIAL_RIFT.get());
    }

    private static boolean isOpenAirOrReplaceable(WorldGenLevel level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos) || level.getBlockEntity(pos) != null) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty()) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
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
