package com.alex.mycelialmind.world;

import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import com.alex.mycelialmind.world.structure.VoxelStructureOps;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates an Organic-Alien Mega-Fungus Capsule (radius 8, height 16) following
 * 70/20/10 form hierarchy, deterministic stratification, 6-connected coiling tentacles,
 * synaptic floor conduits, clean crypt/gallery circulation, and WorldgenAuditEngine invariants.
 */
public class SurfaceHiveFeature extends Feature<NoneFeatureConfiguration> {
    public SurfaceHiveFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        return generateHiveAt(context.level(), context.origin(), context.random());
    }

    public static final int MIN_HIVE_SPACING = 192;
    public static final int MIN_NODE_CLEARANCE = 64;

    public static boolean generateHiveAt(WorldGenLevel level, BlockPos origin, RandomSource random) {
        BlockPos ground = origin.below();
        BlockState groundState = level.getBlockState(ground);
        if (groundState.liquid() || !groundState.isFaceSturdy(level, ground, Direction.UP)) {
            return false;
        }
        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(level);
        int minHiveSpacing = StructureProgressionPolicy.surfaceHiveMinSpacing(tier);
        int minNodeClearance = StructureProgressionPolicy.surfaceHiveNodeClearance(tier);
        if (level.getLevel() instanceof ServerLevel serverLevel) {
            if (!PurificationZoneTracker.get(serverLevel)
                    .tryReserveSurfaceHive(origin, minHiveSpacing, minNodeClearance)) {
                return false;
            }
        }

        BoundingBox fullBox = new BoundingBox(
                origin.getX() - 10, origin.getY() - 6, origin.getZ() - 10,
                origin.getX() + 10, origin.getY() + 16, origin.getZ() + 10
        );

        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
        BlockState mycelialStone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState chitinPillar = ModBlocks.CHITIN_PILLAR.get().defaultBlockState();
        BlockState lattice = ModBlocks.BIO_CHITIN_LATTICE.get().defaultBlockState();
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState deepslateTiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState deepslateBricks = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState polishedBasalt = Blocks.POLISHED_BASALT.defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState purpleGlass = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState wall = Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
        BlockState stairBase = Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState();

        // 1) Organic creeping mycelium carpet & sub-foundation (radius 10)
        long noiseSeed = origin.asLong() ^ 0x5355524641434548L;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 10.2) {
                    continue;
                }
                var cell = MycelialFastNoise.domainWarpedCellular2D(
                        (origin.getX() + dx) * 0.22D,
                        (origin.getZ() + dz) * 0.22D,
                        noiseSeed,
                        0.32D
                );
                boolean inFootprint = dist <= 6.8 || (dist <= 10.0 && cell.edge() >= 0.40D);
                if (!inFootprint) {
                    continue;
                }
                BlockPos floorPos = ground.offset(dx, 0, dz);
                if (!level.getBlockState(floorPos).liquid() && level.getBlockEntity(floorPos) == null) {
                    if (dist >= 5.2 && dist <= 7.8 && level.getBlockEntity(floorPos.below()) == null) {
                        level.setBlock(floorPos.below(), deepslateBricks, 2);
                    }
                    level.setBlock(floorPos, mycelium, 2);
                }
                if (dist > 7.5 && level.getBlockState(floorPos.above()).isAir() && random.nextFloat() < 0.28F) {
                    BlockState flora = random.nextBoolean()
                            ? ModBlocks.SPORE_BULB.get().defaultBlockState()
                            : ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState();
                    level.setBlock(floorPos.above(), flora, 2);
                }
            }
        }

        // 2) Subterranean Root Crypt (dy = -5..-1, radius 4) with clean 2-wide descent
        for (int dy = -5; dy <= -2; dy++) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    double rSq = dx * dx + dz * dz;
                    if (rSq > 18.0) {
                        continue;
                    }
                    BlockPos cp = origin.offset(dx, dy, dz);
                    if (dy == -5) {
                        level.setBlock(cp, ((Math.abs(dx) + Math.abs(dz)) % 3 == 0) ? mycelium : deepslateBricks, 2);
                    } else if (rSq >= 12.5) {
                        boolean isCorner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                        level.setBlock(cp, isCorner ? chitinPillar : archBrick, 2);
                    } else {
                        level.setBlock(cp, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos corbel = origin.relative(dir, 3).offset(0, -2, 0);
            level.setBlock(corbel, stairBase.setValue(StairBlock.FACING, dir).setValue(StairBlock.HALF, Half.TOP), 2);
        }
        level.setBlock(origin.offset(0, -2, 0), amberVein, 2);
        level.setBlock(origin.offset(-2, -4, 0), ModBlocks.SPORE_VENT.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(2, -4, 1), ModBlocks.SPORE_VENT.get().defaultBlockState(), 2);
        // Clean 2-wide stairwell descent with >=3 blocks headroom at dx = 3..4
        for (int step = 0; step < 4; step++) {
            for (int w = 3; w <= 4; w++) {
                level.setBlock(origin.offset(w, -1 - step, -2 - (step / 2)),
                        stairBase.setValue(StairBlock.FACING, Direction.NORTH), 2);
                for (int h = 1; h <= 3; h++) {
                    level.setBlock(origin.offset(w, -1 - step + h, -2 - (step / 2)), Blocks.AIR.defaultBlockState(), 2);
                }
            }
        }
        BlockPos cryptChestPos = origin.offset(0, -4, -3);
        level.setBlock(cryptChestPos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(cryptChestPos) instanceof ChestBlockEntity cryptChest) {
            MycelialLootSystem.populateStructureChest(
                    level, cryptChestPos, cryptChest, MycelialLootSystem.SURFACE_HIVE_CRYPT, random, tier, false);
        }

        // 3) 70% Deterministic Stratified Stem Chamber (dy = 0..8, radius 6.5) - zero random noise
        for (int dy = 0; dy <= 8; dy++) {
            double stemR = 6.6 - Math.sin((dy / 8.0) * Math.PI) * 0.7;
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    double r = Math.sqrt(dx * dx + dz * dz);
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (dz >= 4 && Math.abs(dx) <= 1 && dy <= 4) {
                        if (dy == 4 && Math.abs(dx) == 1) {
                            level.setBlock(p, stairBase.setValue(StairBlock.FACING, dx < 0 ? Direction.WEST : Direction.EAST).setValue(StairBlock.HALF, Half.TOP), 2);
                        } else if (dy == 4 && dx == 0) {
                            level.setBlock(p, amberVein, 2);
                        } else {
                            level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                        }
                        continue;
                    }
                    if (r >= stemR - 1.15 && r <= stemR + 0.65) {
                        boolean isLancet = (dx == 0 || dz == 0) && dy >= 3 && dy <= 6;
                        boolean isRib = Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1;
                        if (isLancet) {
                            level.setBlock(p, (dy % 2 == 0) ? purpleGlass : lattice, 2);
                        } else if (isRib) {
                            level.setBlock(p, (dy % 2 == 0) ? chitinPillar : archBrick, 2);
                        } else if (dy <= 1) {
                            level.setBlock(p, polishedBasalt, 2);
                        } else {
                            level.setBlock(p, (dy % 2 == 0) ? deepslateTiles : mycelialStone, 2);
                        }
                    } else if (r < stemR - 1.15) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // 4) Overhanging Scalloped Fungal Cap (dy = 9..16) - deterministic 70/20/10 stratification
        for (int dy = 9; dy <= 16; dy++) {
            double t = (dy - 9) / 7.0;
            double capR = 8.6 * Math.cos(t * Math.PI * 0.44);
            for (int dx = -9; dx <= 9; dx++) {
                for (int dz = -9; dz <= 9; dz++) {
                    double angle = Math.atan2(dz, dx);
                    double scallop = 0.65 * Math.sin(angle * 6.0);
                    double r = Math.sqrt(dx * dx + dz * dz);
                    double effectiveR = capR + (dy <= 11 ? scallop : 0.0);
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (r <= effectiveR && r >= Math.max(0.0, effectiveR - 2.4)) {
                        BlockState capBlock;
                        if (dy >= 15) {
                            capBlock = (r <= 1.2) ? amberVein : amethyst;
                        } else if (dy == 9) {
                            capBlock = ((Math.abs(dx) + Math.abs(dz)) % 3 == 0) ? amethyst : archBrick;
                        } else if ((Math.abs(dx) + Math.abs(dz)) % 5 == 0) {
                            capBlock = cryingObsidian;
                        } else {
                            capBlock = ((dy + dx + dz) % 2 == 0) ? deepslateTiles : mycelialStone;
                        }
                        level.setBlock(p, capBlock, 2);
                        if (dy == 9 && r >= 6.8 && level.isEmptyBlock(p.below()) && (Math.abs(dx) + Math.abs(dz)) % 3 == 0) {
                            level.setBlock(p.below(), ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState(), 2);
                        }
                    } else if (r < Math.max(0.0, effectiveR - 1.6) && dy <= 14) {
                        level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    }
                }
            }
        }

        // 5) 3 Giant 6-Connected Coiling Tentacles clasping from outer roots into the cap
        for (int arm = 0; arm < 3; arm++) {
            double baseAngle = (Math.PI * 2.0 / 3.0) * arm + 0.4;
            List<BlockPos> waypoints = new ArrayList<>();
            for (int step = 0; step <= 8; step++) {
                double frac = step / 8.0;
                double ang = baseAngle + frac * 1.35;
                double rad = 8.6 - frac * 2.8;
                int tx = (int) Math.round(Math.cos(ang) * rad);
                int tz = (int) Math.round(Math.sin(ang) * rad);
                int ty = (int) Math.round(frac * 12.0);
                if (tz >= 4 && Math.abs(tx) <= 2 && ty <= 4) {
                    continue;
                }
                waypoints.add(origin.offset(tx, ty, tz));
            }
            if (!waypoints.isEmpty()) {
                // Anchor base firmly at ground level
                BlockPos base = waypoints.get(0);
                level.setBlock(base.below(), archBrick, 2);
                VoxelStructureOps.placeFaceConnectedPath(
                        level, fullBox, waypoints,
                        idx -> (idx % 6 == 0) ? amberVein : ((idx % 2 == 0) ? chitinPillar : archBrick)
                );
            }
        }

        // South Portal framing pillars & stairs
        for (int dy = 0; dy <= 4; dy++) {
            level.setBlock(origin.offset(-2, dy, 6), chitinPillar, 2);
            level.setBlock(origin.offset(2, dy, 6), chitinPillar, 2);
        }
        level.setBlock(origin.offset(0, 5, 6), amberVein, 2);
        level.setBlock(origin.offset(-1, 5, 6), wall, 2);
        level.setBlock(origin.offset(1, 5, 6), wall, 2);
        BlockState southStair = stairBase.setValue(StairBlock.FACING, Direction.NORTH);
        for (int dx = -1; dx <= 1; dx++) {
            level.setBlock(origin.offset(dx, -2, 7), deepslateBricks, 2);
            level.setBlock(origin.offset(dx, -1, 7), mycelium, 2);
            level.setBlock(origin.offset(dx, 0, 7), southStair, 2);
        }
        // Connect the three-wide entrance landing to both portal-frame pillars by full faces.
        level.setBlock(origin.offset(-2, 0, 7), chitinPillar, 2);
        level.setBlock(origin.offset(2, 0, 7), chitinPillar, 2);

        // 6) Upper mezzanine gallery (dy = 4) with wall balustrade & welded ceiling chandeliers
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double rSq = dx * dx + dz * dz;
                if (rSq >= 10.5 && rSq <= 26.0 && !(dz >= 3 && Math.abs(dx) <= 2)) {
                    BlockPos galleryPos = origin.offset(dx, 4, dz);
                    BlockState galleryBlock = ((Math.abs(dx) + Math.abs(dz)) % 3 == 0)
                            ? lattice
                            : Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState();
                    level.setBlock(galleryPos, galleryBlock, 2);
                    if (rSq >= 10.5 && rSq <= 13.5 && (Math.abs(dx) + Math.abs(dz)) % 2 == 0) {
                        level.setBlock(galleryPos.above(), wall, 2);
                    }
                }
            }
        }
        BlockState eastStair = stairBase.setValue(StairBlock.FACING, Direction.SOUTH);
        for (int step = 0; step < 4; step++) {
            for (int fy = 0; fy < step; fy++) {
                level.setBlock(origin.offset(-4, fy, 1 - step), deepslateBricks, 2);
            }
            BlockPos stairPos = origin.offset(-4, step, 1 - step);
            level.setBlock(stairPos, eastStair, 2);
            level.setBlock(stairPos.above(), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(stairPos.above(2), Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(stairPos.above(3), Blocks.AIR.defaultBlockState(), 2);
        }
        for (int cx : new int[]{-3, 3}) {
            // Weld chandelier anchor directly to solid cap ceiling at dy = 15 -> 14 -> 13..12 -> 11
            level.setBlock(origin.offset(cx, 15, 0), archBrick, 2);
            VoxelStructureOps.placeAnchoredChain(level, fullBox, origin.offset(cx, 14, 0), 2, archBrick, amberVein);
        }

        // 7) Synaptic Floor Conduits & Central Spire (INCUBATOR at origin, SENTINEL at origin.above(3))
        // Reach the dy=0 stem wall so the elevated shell is physically tied to its central node.
        VoxelStructureOps.placeSynapticConduit(level, fullBox, origin.offset(0, -1, 0), origin.offset(-6, -1, 0), archBrick, amberVein);
        VoxelStructureOps.placeSynapticConduit(level, fullBox, origin.offset(0, -1, 0), origin.offset(0, -1, -6), archBrick, amberVein);

        BlockPos incubatorPos = origin;
        BlockPos sentinelPos = origin.above(3);
        level.setBlock(incubatorPos, ModBlocks.INCUBATOR_NODE.get().defaultBlockState(), 2);
        level.setBlock(origin.above(1), lattice, 2);
        level.setBlock(origin.above(2), amberVein, 2);
        level.setBlock(sentinelPos, ModBlocks.SENTINEL_NODE.get().defaultBlockState(), 2);

        level.setBlock(origin.offset(-2, 0, 1), ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(2, 0, 1), ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(-3, 0, -2), ModBlocks.SPORE_VENT.get().defaultBlockState(), 2);
        level.setBlock(origin.offset(2, 0, -3), ModBlocks.SPORE_VENT.get().defaultBlockState(), 2);

        // 8) Primary loot chest at origin.east() (preserved for WorldgenAuditEngine) + upper gallery chest
        BlockPos chestPos = origin.east();
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            MycelialLootSystem.populateStructureChest(
                    level, chestPos, chest, MycelialLootSystem.SURFACE_HIVE_CACHE, random, tier, true);
        }

        BlockPos upperChestPos = origin.offset(0, 5, -3);
        level.setBlock(upperChestPos.below(), archBrick, 2);
        // Bridge the chest plinth across the one-block gap to the gallery's inner edge.
        level.setBlock(origin.offset(1, 4, -3), archBrick, 2);
        level.setBlock(upperChestPos, Blocks.CHEST.defaultBlockState(), 2);
        if (level.getBlockEntity(upperChestPos) instanceof ChestBlockEntity upperChest) {
            MycelialLootSystem.populateStructureChest(
                    level, upperChestPos, upperChest, MycelialLootSystem.SURFACE_HIVE_GALLERY, random, tier, false);
        }

        return true;
    }
}
