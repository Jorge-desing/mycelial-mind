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
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(level);
        int minSpawnDist = StructureProgressionPolicy.surfaceHiveMinSpawnDistance(tier);
        if (minSpawnDist > 0 && level.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos spawn = serverLevel.getSharedSpawnPos();
            long dx = (long) origin.getX() - spawn.getX();
            long dz = (long) origin.getZ() - spawn.getZ();
            if (dx * dx + dz * dz < (long) minSpawnDist * minSpawnDist) {
                return false;
            }
        }
        return generateHiveAt(level, origin, context.random());
    }

    public static final int MIN_HIVE_SPACING = 192;
    public static final int MIN_NODE_CLEARANCE = 64;

    public static boolean generateHiveAt(WorldGenLevel level, BlockPos origin, RandomSource random) {
        if (level.isOutsideBuildHeight(origin) || origin.getY() + 16 >= level.getMaxBuildHeight()) {
            return false;
        }
        BlockPos ground = origin.below();
        if (level.isOutsideBuildHeight(ground)) {
            return false;
        }
        BlockState groundState = level.getBlockState(ground);
        if (groundState.liquid()
                || !level.getFluidState(origin).isEmpty()
                || !level.getFluidState(ground).isEmpty()
                || !groundState.isFaceSturdy(level, ground, Direction.UP)
                || !isSuitableSurfaceTerrain(level, origin, ground)) {
            return false;
        }
        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(level);
        int minHiveSpacing = StructureProgressionPolicy.surfaceHiveMinSpacing(tier);
        int minNodeClearance = StructureProgressionPolicy.surfaceHiveNodeClearance(tier);
        if (level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            if (tracker.isProtected(origin)
                    || !tracker.tryReserveSurfaceHive(origin, minHiveSpacing, minNodeClearance)) {
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

        // 1) Organic creeping mycelium carpet & sub-foundation (radius 10, slope-adaptive outer rim)
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
                BlockPos floorPos = dist <= 6.8
                        ? ground.offset(dx, 0, dz)
                        : findCarpetFloor(level, origin.getX() + dx, ground.getY(), origin.getZ() + dz, 2);
                if (floorPos == null) {
                    continue;
                }
                if (isSafeToOverwrite(level, floorPos) && level.getFluidState(floorPos).isEmpty()) {
                    if (dist >= 5.2 && dist <= 7.8) {
                        safeSetBlock(level, floorPos.below(), deepslateBricks);
                    }
                    safeSetBlock(level, floorPos, mycelium);
                }
                if (dist > 7.5 && isOpenAirOrReplaceable(level, floorPos.above()) && random.nextFloat() < 0.28F) {
                    BlockState flora = random.nextBoolean()
                            ? ModBlocks.SPORE_BULB.get().defaultBlockState()
                            : ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState();
                    safeSetBlock(level, floorPos.above(), flora);
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
                        safeSetBlock(level, cp, ((Math.abs(dx) + Math.abs(dz)) % 3 == 0) ? mycelium : deepslateBricks);
                    } else if (rSq >= 12.5) {
                        boolean isCorner = Math.abs(dx) == 3 && Math.abs(dz) == 3;
                        safeSetBlock(level, cp, isCorner ? chitinPillar : archBrick);
                    } else {
                        safeSetBlock(level, cp, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos corbel = origin.relative(dir, 3).offset(0, -2, 0);
            safeSetBlock(level, corbel, stairBase.setValue(StairBlock.FACING, dir).setValue(StairBlock.HALF, Half.TOP));
        }
        safeSetBlock(level, origin.offset(0, -2, 0), amberVein);
        safeSetBlock(level, origin.offset(-2, -4, 0), ModBlocks.SPORE_VENT.get().defaultBlockState());
        safeSetBlock(level, origin.offset(2, -4, 1), ModBlocks.SPORE_VENT.get().defaultBlockState());
        // Clean 2-wide stairwell descent (w=2..3, z=0..-3) with >=3 blocks headroom and sealed crypt walls
        for (int w = 2; w <= 3; w++) {
            safeSetBlock(level, origin.offset(w, -5, -3), deepslateBricks);
            for (int sy = -4; sy <= -2; sy++) {
                safeSetBlock(level, origin.offset(w, sy, -4), archBrick);
            }
        }
        for (int sy = -3; sy <= -2; sy++) {
            safeSetBlock(level, origin.offset(4, sy, -2), archBrick);
            safeSetBlock(level, origin.offset(4, sy, -3), archBrick);
        }
        safeSetBlock(level, origin.offset(4, -4, -3), archBrick);
        for (int step = 0; step < 4; step++) {
            int zOff = -step;
            int yOff = -1 - step;
            for (int w = 2; w <= 3; w++) {
                BlockPos stairPos = origin.offset(w, yOff, zOff);
                if (step < 3) {
                    safeSetBlock(level, stairPos.below(), deepslateBricks);
                }
                safeSetBlock(level, stairPos, stairBase.setValue(StairBlock.FACING, Direction.NORTH));
                for (int h = 1; h <= 3; h++) {
                    safeSetBlock(level, stairPos.above(h), Blocks.AIR.defaultBlockState());
                }
            }
        }
        BlockPos cryptChestPos = origin.offset(0, -4, -3);
        if (safeSetBlock(level, cryptChestPos, Blocks.CHEST.defaultBlockState())
                && level.getBlockEntity(cryptChestPos) instanceof ChestBlockEntity cryptChest) {
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
                            safeSetBlock(level, p, stairBase.setValue(StairBlock.FACING, dx < 0 ? Direction.WEST : Direction.EAST).setValue(StairBlock.HALF, Half.TOP));
                        } else if (dy == 4 && dx == 0) {
                            safeSetBlock(level, p, amberVein);
                        } else {
                            safeSetBlock(level, p, Blocks.AIR.defaultBlockState());
                        }
                        continue;
                    }
                    if (r >= stemR - 1.15 && r <= stemR + 0.65) {
                        boolean isLancet = (dx == 0 || dz == 0) && dy >= 3 && dy <= 6;
                        boolean isRib = Math.abs(Math.abs(dx) - Math.abs(dz)) <= 1;
                        if (isLancet) {
                            safeSetBlock(level, p, (dy % 2 == 0) ? purpleGlass : lattice);
                        } else if (isRib) {
                            safeSetBlock(level, p, (dy % 2 == 0) ? chitinPillar : archBrick);
                        } else if (dy <= 1) {
                            safeSetBlock(level, p, polishedBasalt);
                        } else {
                            safeSetBlock(level, p, (dy % 2 == 0) ? deepslateTiles : mycelialStone);
                        }
                    } else if (r < stemR - 1.15) {
                        safeSetBlock(level, p, Blocks.AIR.defaultBlockState());
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
                        safeSetBlock(level, p, capBlock);
                        if (dy == 9 && r >= 6.8 && isOpenAirOrReplaceable(level, p.below()) && (Math.abs(dx) + Math.abs(dz)) % 3 == 0) {
                            safeSetBlock(level, p.below(), ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState());
                        }
                    } else if (r < Math.max(0.0, effectiveR - 1.6) && dy <= 14) {
                        safeSetBlock(level, p, Blocks.AIR.defaultBlockState());
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
                // Anchor base firmly into sloped or flat ground up to 4 blocks down
                BlockPos base = waypoints.get(0);
                anchorBaseDown(level, base.below(), 4, archBrick);
                VoxelStructureOps.placeFaceConnectedPath(
                        level, fullBox, waypoints,
                        idx -> (idx % 6 == 0) ? amberVein : ((idx % 2 == 0) ? chitinPillar : archBrick)
                );
            }
        }

        // South Portal framing pillars & stairs
        for (int dy = 0; dy <= 4; dy++) {
            safeSetBlock(level, origin.offset(-2, dy, 6), chitinPillar);
            safeSetBlock(level, origin.offset(2, dy, 6), chitinPillar);
        }
        safeSetBlock(level, origin.offset(0, 5, 6), amberVein);
        safeSetBlock(level, origin.offset(-1, 5, 6), wall);
        safeSetBlock(level, origin.offset(1, 5, 6), wall);
        BlockState southStair = stairBase.setValue(StairBlock.FACING, Direction.NORTH);
        for (int dx = -1; dx <= 1; dx++) {
            safeSetBlock(level, origin.offset(dx, -2, 7), deepslateBricks);
            safeSetBlock(level, origin.offset(dx, -1, 7), mycelium);
            safeSetBlock(level, origin.offset(dx, 0, 7), southStair);
        }
        // Connect the three-wide entrance landing to both portal-frame pillars by full faces.
        safeSetBlock(level, origin.offset(-2, 0, 7), chitinPillar);
        safeSetBlock(level, origin.offset(2, 0, 7), chitinPillar);

        // 6) Upper mezzanine gallery (dy = 4) with wall balustrade & welded ceiling chandeliers
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double rSq = dx * dx + dz * dz;
                if (rSq >= 10.5 && rSq <= 26.0 && !(dz >= 3 && Math.abs(dx) <= 2)) {
                    BlockPos galleryPos = origin.offset(dx, 4, dz);
                    BlockState galleryBlock = ((Math.abs(dx) + Math.abs(dz)) % 3 == 0)
                            ? lattice
                            : Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState();
                    safeSetBlock(level, galleryPos, galleryBlock);
                    if (rSq >= 10.5 && rSq <= 13.5 && (Math.abs(dx) + Math.abs(dz)) % 2 == 0) {
                        safeSetBlock(level, galleryPos.above(), wall);
                    }
                }
            }
        }
        BlockState eastStair = stairBase.setValue(StairBlock.FACING, Direction.SOUTH);
        for (int step = 0; step < 4; step++) {
            for (int fy = 0; fy < step; fy++) {
                safeSetBlock(level, origin.offset(-4, fy, 1 - step), deepslateBricks);
            }
            BlockPos stairPos = origin.offset(-4, step, 1 - step);
            safeSetBlock(level, stairPos, eastStair);
            safeSetBlock(level, stairPos.above(), Blocks.AIR.defaultBlockState());
            safeSetBlock(level, stairPos.above(2), Blocks.AIR.defaultBlockState());
            safeSetBlock(level, stairPos.above(3), Blocks.AIR.defaultBlockState());
        }
        for (int cx : new int[]{-3, 3}) {
            // Weld chandelier anchor directly to solid cap ceiling at dy = 15 -> 14 -> 13..12 -> 11
            safeSetBlock(level, origin.offset(cx, 15, 0), archBrick);
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

        safeSetBlock(level, origin.offset(-2, 0, 1), ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
        safeSetBlock(level, origin.offset(2, 0, 1), ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
        safeSetBlock(level, origin.offset(-3, 0, -2), ModBlocks.SPORE_VENT.get().defaultBlockState());
        safeSetBlock(level, origin.offset(-2, 0, -3), ModBlocks.SPORE_VENT.get().defaultBlockState());

        // 8) Primary loot chest at origin.east() (preserved for WorldgenAuditEngine) + upper gallery chest
        BlockPos chestPos = origin.east();
        if (safeSetBlock(level, chestPos, Blocks.CHEST.defaultBlockState())
                && level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            MycelialLootSystem.populateStructureChest(
                    level, chestPos, chest, MycelialLootSystem.SURFACE_HIVE_CACHE, random, tier, true);
        }

        BlockPos upperChestPos = origin.offset(0, 5, -3);
        safeSetBlock(level, upperChestPos.below(), archBrick);
        // Bridge the chest plinth across the one-block gap to the gallery's inner edge.
        safeSetBlock(level, origin.offset(1, 4, -3), archBrick);
        if (safeSetBlock(level, upperChestPos, Blocks.CHEST.defaultBlockState())
                && level.getBlockEntity(upperChestPos) instanceof ChestBlockEntity upperChest) {
            MycelialLootSystem.populateStructureChest(
                    level, upperChestPos, upperChest, MycelialLootSystem.SURFACE_HIVE_GALLERY, random, tier, false);
        }

        return true;
    }

    private static boolean isSuitableSurfaceTerrain(WorldGenLevel level, BlockPos origin, BlockPos ground) {
        int[][] samples = {
                {4, 0}, {-4, 0}, {0, 4}, {0, -4},
                {3, 3}, {3, -3}, {-3, 3}, {-3, -3}
        };
        for (int[] s : samples) {
            BlockPos airProbe = origin.offset(s[0], 0, s[1]);
            BlockPos gProbe = ground.offset(s[0], 0, s[1]);
            if (level.isOutsideBuildHeight(gProbe)
                    || !level.getFluidState(airProbe).isEmpty()
                    || !level.getFluidState(gProbe).isEmpty()) {
                return false;
            }
            boolean hasSolidSlope = false;
            for (int dy = -3; dy <= 3; dy++) {
                BlockPos cand = gProbe.offset(0, dy, 0);
                if (!level.isOutsideBuildHeight(cand)
                        && level.getFluidState(cand).isEmpty()
                        && level.getBlockState(cand).isFaceSturdy(level, cand, Direction.UP)) {
                    hasSolidSlope = true;
                    break;
                }
            }
            if (!hasSolidSlope) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos findCarpetFloor(WorldGenLevel level, int x, int baseY, int z, int maxDeltaY) {
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

    private static void anchorBaseDown(WorldGenLevel level, BlockPos startPos, int maxDepth, BlockState fill) {
        for (int d = 0; d < maxDepth; d++) {
            BlockPos probe = startPos.below(d);
            if (level.isOutsideBuildHeight(probe)) {
                return;
            }
            BlockState existing = level.getBlockState(probe);
            if (existing.isSolid() && !existing.liquid() && d > 0) {
                return;
            }
            safeSetBlock(level, probe, fill);
            if (existing.isSolid() && !existing.liquid()) {
                return;
            }
        }
    }

    private static boolean safeSetBlock(WorldGenLevel level, BlockPos pos, BlockState state) {
        if (!isSafeToOverwrite(level, pos)) {
            return false;
        }
        return level.setBlock(pos, state, 2);
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
}
