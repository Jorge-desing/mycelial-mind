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
        if (level.getLevel() instanceof ServerLevel serverLevel
                && !PurificationZoneTracker.get(serverLevel).tryReserveNodePosition(center, minSpacing, nodeType)) {
            return false;
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
        if (canReplaceGround(level.getBlockState(center.below()))) {
            level.setBlock(center.below(), archBrick, 2);
        }

        BlockState nodeState = switch (nodeType) {
            case SENTINEL -> ModBlocks.SENTINEL_NODE.get().defaultBlockState();
            case INCUBATOR -> ModBlocks.INCUBATOR_NODE.get().defaultBlockState();
            case RELAY, SYMBIOTIC -> ModBlocks.MYCELIAL_NODE.get().defaultBlockState();
        };
        level.setBlock(center, nodeState, 2);

        // Stepped cardinal plinth & diagonal wall posts (all grounded at center.y)
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos stepPos = center.relative(dir);
            if (level.isEmptyBlock(stepPos) && level.getBlockState(stepPos.below()).isSolid()) {
                level.setBlock(stepPos, stairBase.setValue(StairBlock.FACING, dir.getOpposite()), 2);
            }
        }
        for (int sx : new int[]{-2, 2}) {
            for (int sz : new int[]{-2, 2}) {
                BlockPos corner = center.offset(sx, 0, sz);
                if (canReplaceGround(level.getBlockState(corner))
                        || (level.isEmptyBlock(corner) && level.getBlockState(corner.below()).isSolid())) {
                    level.setBlock(corner, archBrick, 2);
                }
                if (!level.isEmptyBlock(corner) && level.isEmptyBlock(corner.above())) {
                    level.setBlock(corner.above(), wall, 2);
                }
            }
        }

        // 70/20/10 6-connected curving fungal stalk, counter-rib & stratified cap (height 9)
        boolean hasHeadroom = true;
        for (int dy = 1; dy <= 9; dy++) {
            if (!level.isEmptyBlock(center.above(dy))) {
                hasHeadroom = false;
                break;
            }
        }
        if (hasHeadroom) {
            BoundingBox canopyBox = new BoundingBox(
                    center.getX() - 4, center.getY() - 1, center.getZ() - 4,
                    center.getX() + 4, center.getY() + 9, center.getZ() + 4
            );
            Direction leanDir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
            Direction opp = leanDir.getOpposite();

            BlockPos baseStalk = center.relative(leanDir, 2);
            if (canReplaceGround(level.getBlockState(baseStalk))
                    || (level.isEmptyBlock(baseStalk) && level.getBlockState(baseStalk.below()).isSolid())) {
                level.setBlock(baseStalk, archBrick, 2);
            }
            VoxelStructureOps.placeFaceConnectedPath(
                    level, canopyBox,
                    List.of(
                            baseStalk,
                            baseStalk.above(1),
                            baseStalk.above(2),
                            center.relative(leanDir, 1).above(4),
                            center.above(6)
                    ),
                    step -> (step % 2 == 0) ? chitinPillar : archBrick
            );

            BlockPos oppBase = center.relative(opp, 2);
            if (canReplaceGround(level.getBlockState(oppBase))
                    || (level.isEmptyBlock(oppBase) && level.getBlockState(oppBase.below()).isSolid())) {
                level.setBlock(oppBase, archBrick, 2);
            }
            VoxelStructureOps.placeFaceConnectedPath(
                    level, canopyBox,
                    List.of(
                            oppBase,
                            oppBase.above(1),
                            center.relative(opp, 1).above(3),
                            center.above(6)
                    ),
                    step -> (step == 0) ? chitinPillar : archBrick
            );

            // Deterministic stratified Fungal Cap (dy = 6..8, radius 3) - zero random block noise
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double r = Math.sqrt(dx * dx + dz * dz);
                    if (r <= 3.1) {
                        BlockPos rim = center.offset(dx, 6, dz);
                        if (r >= 2.2) {
                            level.setBlock(rim, ((Math.abs(dx) + Math.abs(dz)) % 2 == 0) ? purpleGlass : deepslateTiles, 2);
                            if (level.isEmptyBlock(rim.below()) && (Math.abs(dx) + Math.abs(dz)) % 3 == 0) {
                                level.setBlock(rim.below(), ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState(), 2);
                            }
                        } else {
                            level.setBlock(rim, deepslateTiles, 2);
                            level.setBlock(rim.above(), (r <= 1.2) ? amethyst : stone, 2);
                        }
                    }
                    if (r <= 1.8) {
                        level.setBlock(center.offset(dx, 7, dz), (r <= 0.8) ? amethyst : deepslateTiles, 2);
                    }
                }
            }
            level.setBlock(center.above(8), amberVein, 2);
            VoxelStructureOps.placeAnchoredChain(level, canopyBox, center.above(5), 1, archBrick, amberVein);
        }

        // WFC core mosaic + Domain-warped radial root veins (radius 6)
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
                BlockPos floor = center.offset(dx, 0, dz);
                if (canReplaceGround(level.getBlockState(floor))) {
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

                    if (level.isEmptyBlock(floor.above())) {
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
                        if (level.getBlockState(ceiling).isSolid() && level.isEmptyBlock(ceiling.below())) {
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
                && !level.isOutsideBuildHeight(vaultCenter.below())
                && canReplaceGround(level.getBlockState(vaultCenter))
                && canReplaceGround(level.getBlockState(vaultCenter.below()))) {
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
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = vaultCenter.offset(dx, dy, dz);
                    if (level.isOutsideBuildHeight(p)) {
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
        if (!level.isOutsideBuildHeight(vaultCenter.above(2))) {
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

    private static BlockPos findAnchor(WorldGenLevel level, BlockPos origin) {
        for (int dy = 8; dy >= -8; dy--) {
            BlockPos candidate = origin.offset(0, dy, 0);
            if (canReplaceGround(level.getBlockState(candidate)) && level.isEmptyBlock(candidate.above())) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean canReplaceGround(BlockState state) {
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }
}
