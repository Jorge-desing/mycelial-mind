package com.alex.mycelialmind.world;

import com.alex.mycelialmind.block.RootCapShelfBlock;
import com.alex.mycelialmind.block.RootSapMossBlock;
import com.alex.mycelialmind.entity.RootKeeperEntity;
import com.alex.mycelialmind.entity.SporeGliderEntity;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import com.alex.mycelialmind.world.structure.VoxelStructureOps;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Generates a 3-stratum vertical Mycelial Sinkhole connecting the Overworld surface
 * to a living subterranean Root Cavern:
 *   1) Surface Crown (crater rim, creeping mycelium, hyphal veins, perimeter spore vents)
 *   2) Twilight Throat (21-block spiral vertical shaft, cushioned Root Cap Shelves, amber veins, tendrils)
 *   3) Root Cavern (vaulted chamber with Root Sap Moss, central Spore Updraft Vent, Root Node,
 *      vault chest, Root Keeper guardian, and Spore Glider pollinators)
 */
public class MycelialSinkholeFeature extends Feature<NoneFeatureConfiguration> {
    public MycelialSinkholeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(level);
        int minSpawnDist = StructureProgressionPolicy.sinkholeMinSpawnDistance(tier);
        if (minSpawnDist > 0 && level.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos spawn = serverLevel.getSharedSpawnPos();
            long dx = (long) origin.getX() - spawn.getX();
            long dz = (long) origin.getZ() - spawn.getZ();
            if (dx * dx + dz * dz < (long) minSpawnDist * minSpawnDist) {
                return false;
            }
        }
        return generateSinkholeAt(level, origin, context.random());
    }

    public static boolean generateSinkholeAt(WorldGenLevel level, BlockPos origin, RandomSource random) {
        if (level.isOutsideBuildHeight(origin)
                || origin.getY() + 6 >= level.getMaxBuildHeight()
                || origin.getY() - 34 < level.getMinBuildHeight() + 4) {
            return false;
        }
        BlockPos ground = origin.below();
        BlockState groundState = level.getBlockState(ground);
        if (groundState.liquid()
                || !level.getFluidState(origin).isEmpty()
                || !level.getFluidState(ground).isEmpty()
                || !groundState.isFaceSturdy(level, ground, Direction.UP)
                || !isSuitableSurfaceTerrain(level, origin, ground)) {
            return false;
        }

        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(level);
        int minSpacing = StructureProgressionPolicy.sinkholeMinSpacing(tier);
        int minNodeClearance = StructureProgressionPolicy.sinkholeNodeClearance(tier);
        if (level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            if (tracker.isProtected(origin)
                    || !tracker.tryReserveSinkhole(origin, minSpacing, minNodeClearance)) {
                return false;
            }
        }

        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
        BlockState stone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState pillar = ModBlocks.CHITIN_PILLAR.get().defaultBlockState();
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState tendrils = ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState();
        BlockState cap = ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState();
        BlockState sporeBulb = ModBlocks.SPORE_BULB.get().defaultBlockState();
        BlockState sporeVent = ModBlocks.SPORE_VENT.get().defaultBlockState();
        BlockState sapMoss = ModBlocks.ROOT_SAP_MOSS.get().defaultBlockState().setValue(RootSapMossBlock.SAP_READY, true);
        BlockState updraftVent = ModBlocks.SPORE_UPDRAFT_VENT.get().defaultBlockState();

        long noiseSeed = origin.asLong() ^ 0x53494E4B484F4C45L;

        // 1) Stratum 1: Surface Crown (irregular organic crater rim + outer creeping mycelium)
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 10.0D) {
                    continue;
                }
                if (dist <= 3.6D) {
                    for (int dy = 0; dy <= 5; dy++) {
                        safeSetBlock(level, origin.offset(dx, dy, dz), air);
                    }
                    continue;
                }
                var cell = MycelialFastNoise.domainWarpedCellular2D(
                        (origin.getX() + dx) * 0.24D,
                        (origin.getZ() + dz) * 0.24D,
                        noiseSeed,
                        0.30D
                );
                if (dist > 7.2D && cell.edge() < 0.38D) {
                    continue;
                }
                BlockPos floorPos = findSurfaceFloor(level, origin.getX() + dx, origin.getY() - 1, origin.getZ() + dz, 3);
                if (floorPos == null) {
                    continue;
                }
                if (dist <= 5.8D) {
                    BlockState rimMat = (dist <= 4.7D && random.nextFloat() < 0.35F) ? archBrick : stone;
                    safeSetBlock(level, floorPos, rimMat);
                    anchorBaseDown(level, floorPos.below(), 3, stone);
                    if (dist <= 5.2D && random.nextFloat() < 0.45F) {
                        BlockPos crownLip = floorPos.above();
                        if (isOpenAirOrReplaceable(level, crownLip)) {
                            safeSetBlock(level, crownLip, random.nextFloat() < 0.25F ? archBrick : mycelium);
                        }
                    }
                } else {
                    safeSetBlock(level, floorPos, random.nextFloat() < 0.72F ? mycelium : stone);
                    BlockPos top = floorPos.above();
                    if (isOpenAirOrReplaceable(level, top)) {
                        if (dist > 6.8D && random.nextFloat() < 0.28F) {
                            safeSetBlock(level, top, ModBlocks.HYPHAL_VEIN.get().defaultBlockState());
                        } else if (dist <= 7.0D && random.nextFloat() < 0.08F) {
                            safeSetBlock(level, top, sporeVent);
                        }
                    }
                }
            }
        }

        // Ensure perimeter spore vents on the cardinal rim
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos probe = origin.relative(dir, 6);
            BlockPos floor = findSurfaceFloor(level, probe.getX(), origin.getY() - 1, probe.getZ(), 3);
            if (floor != null && isOpenAirOrReplaceable(level, floor.above())) {
                safeSetBlock(level, floor, stone);
                safeSetBlock(level, floor.above(), sporeVent);
            }
        }

        // 2) Stratum 2: Twilight Throat (dy = -1 .. -21)
        for (int dy = -1; dy >= -21; dy--) {
            double twistX = Math.sin(dy * 0.24D) * 1.1D;
            double twistZ = Math.cos(dy * 0.24D) * 1.1D;
            boolean pillarBand = (Math.abs(dy) % 4) == 0;
            boolean amberBand = (Math.abs(dy) % 3) == 0;

            for (int dx = -6; dx <= 6; dx++) {
                for (int dz = -6; dz <= 6; dz++) {
                    double ox = dx - twistX;
                    double oz = dz - twistZ;
                    double dist = Math.sqrt(ox * ox + oz * oz);
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (dist <= 3.35D) {
                        safeSetBlock(level, p, air);
                    } else if (dist <= 5.25D) {
                        BlockState wallState = stone;
                        if (pillarBand && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1)) {
                            wallState = pillar;
                        } else if (amberBand && random.nextFloat() < 0.16F) {
                            wallState = amberVein;
                        } else if (random.nextFloat() < 0.14F) {
                            wallState = archBrick;
                        }
                        safeSetBlock(level, p, wallState);
                    }
                }
            }

            // Spiral Root Cap Shelves every 3 blocks of depth
            if ((Math.abs(dy) % 3) == 0 && dy <= -3 && dy >= -18) {
                int stepIdx = Math.abs(dy) / 3;
                double angle = stepIdx * (Math.PI * 0.55D);
                int lx = (int) Math.round(twistX + Math.cos(angle) * 2.9D);
                int lz = (int) Math.round(twistZ + Math.sin(angle) * 2.9D);
                BlockPos ledgePos = origin.offset(lx, dy, lz);
                Direction towardCenter = horizontalFacingTowardCenter(lx, lz);
                BlockPos wallBehind = ledgePos.relative(towardCenter.getOpposite());
                safeSetBlock(level, wallBehind, stone);
                safeSetBlock(level, ledgePos,
                        ModBlocks.ROOT_CAP_SHELF.get().defaultBlockState().setValue(RootCapShelfBlock.FACING, towardCenter));
                BlockPos sideLedge = ledgePos.relative(towardCenter.getClockWise());
                if (sideLedge.getX() != origin.getX() || sideLedge.getZ() != origin.getZ()) {
                    safeSetBlock(level, sideLedge.relative(towardCenter.getOpposite()), stone);
                    safeSetBlock(level, sideLedge,
                            ModBlocks.ROOT_CAP_SHELF.get().defaultBlockState().setValue(RootCapShelfBlock.FACING, towardCenter));
                }
            }
        }

        // 3) Stratum 3: Root Cavern (dy = -22 .. -32, vaulted chamber centered at origin.below(27))
        int cavernCenterY = -27;
        for (int dx = -10; dx <= 10; dx++) {
            for (int dz = -10; dz <= 10; dz++) {
                for (int dy = -22; dy >= -32; dy--) {
                    double nx = dx / 9.0D;
                    double nz = dz / 9.0D;
                    double ny = (dy - cavernCenterY) / 5.0D;
                    double ellip = nx * nx + ny * ny + nz * nz;
                    BlockPos p = origin.offset(dx, dy, dz);

                    if (dy == -32) {
                        if (nx * nx + nz * nz <= 1.05D) {
                            safeSetBlock(level, p, stone);
                        }
                    } else if (dy == -31) {
                        double rHoriz = Math.sqrt(dx * dx + dz * dz);
                        if (rHoriz <= 9.2D) {
                            BlockState floorMat = rHoriz <= 6.2D ? sapMoss
                                    : (random.nextFloat() < 0.65F ? mycelium : stone);
                            safeSetBlock(level, p, floorMat);
                        }
                    } else if (ellip <= 0.78D) {
                        safeSetBlock(level, p, air);
                    } else if (ellip <= 1.16D) {
                        // Preserve central throat opening at top of cavern (dy >= -23, horiz <= 3.35)
                        double horiz = Math.sqrt(dx * dx + dz * dz);
                        if (dy >= -23 && horiz <= 3.35D) {
                            safeSetBlock(level, p, air);
                        } else {
                            BlockState shellMat = random.nextFloat() < 0.14F ? amberVein
                                    : (random.nextFloat() < 0.22F ? archBrick : stone);
                            safeSetBlock(level, p, shellMat);
                        }
                    }
                }
            }
        }

        // Hang mycelial tendrils on cavern ceiling & throat overhangs
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    continue;
                }
                for (int dy = -24; dy >= -26; dy--) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (isOpenAirOrReplaceable(level, p)
                            && level.getBlockState(p.above()).isSolid()
                            && random.nextFloat() < 0.16F) {
                        safeSetBlock(level, p, tendrils);
                    }
                }
            }
        }

        // Cavern flora scatter (outside central vent & pedestals)
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r < 2.2D || r > 7.5D || (Math.abs(dx) >= 3 && Math.abs(dz) <= 1)) {
                    continue;
                }
                BlockPos floor = origin.offset(dx, -31, dz);
                BlockPos floraPos = floor.above();
                if (level.getBlockState(floor).isSolid()
                        && isOpenAirOrReplaceable(level, floraPos)
                        && random.nextFloat() < 0.14F) {
                    safeSetBlock(level, floraPos, random.nextFloat() < 0.65F ? cap : sporeBulb);
                }
            }
        }

        // Central Spore Updraft Vent at origin.below(30) sitting on arch-brick socket
        BlockPos ventPos = origin.below(30);
        safeSetBlock(level, ventPos.below(), archBrick);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            safeSetBlock(level, ventPos.relative(dir).below(), archBrick);
        }
        safeSetBlock(level, ventPos, updraftVent);
        // Ensure vertical shaft above vent is clear of accidental obstructions
        for (int dy = -29; dy <= 2; dy++) {
            safeSetBlock(level, origin.offset(0, dy, 0), air);
        }

        // Root Synaptic Node on elevated eastern plinth + embedded amber conduit to vent
        BlockPos nodePedestal = origin.offset(4, -30, 0);
        BlockPos nodePos = nodePedestal.above();
        safeSetBlock(level, nodePedestal.below(), archBrick);
        safeSetBlock(level, nodePedestal, archBrick);
        SynapticNodeType nodeType = StructureProgressionPolicy.pickOutbreakNodeType(tier, random.nextFloat());
        BlockState nodeState = switch (nodeType) {
            case SENTINEL -> ModBlocks.SENTINEL_NODE.get().defaultBlockState();
            case INCUBATOR -> ModBlocks.INCUBATOR_NODE.get().defaultBlockState();
            case RELAY, SYMBIOTIC -> ModBlocks.MYCELIAL_NODE.get().defaultBlockState();
        };
        safeSetBlock(level, nodePos, nodeState);
        if (level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker.get(serverLevel).addNode(nodePos, nodeType);
        }
        BoundingBox cavernBox = new BoundingBox(
                origin.getX() - 10, origin.getY() - 32, origin.getZ() - 10,
                origin.getX() + 10, origin.getY() - 20, origin.getZ() + 10
        );
        VoxelStructureOps.placeSynapticConduit(level, cavernBox, ventPos.below(), nodePedestal.below(), archBrick, amberVein);

        // Root Vault Chest on western plinth
        BlockPos chestPlinth = origin.offset(-4, -31, 0);
        BlockPos chestPos = chestPlinth.above();
        safeSetBlock(level, chestPlinth, archBrick);
        BlockState chestState = Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.EAST);
        if (safeSetBlock(level, chestPos, chestState)
                && level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            MycelialLootSystem.populateStructureChest(
                    level, chestPos, chest, MycelialLootSystem.SINKHOLE_ROOT_VAULT, random, tier, false);
        }

        // Spawn native fauna (1 Root Keeper guardian + 2 Spore Gliders)
        RootKeeperEntity keeper = ModEntities.ROOT_KEEPER.get().create(level.getLevel());
        if (keeper != null) {
            keeper.moveTo(origin.getX() + 3.5D, origin.getY() - 29.8D, origin.getZ() + 3.5D, random.nextFloat() * 360.0F, 0.0F);
            keeper.finalizeSpawn(level, level.getCurrentDifficultyAt(nodePos), MobSpawnType.STRUCTURE, null);
            keeper.setPersistenceRequired();
            level.addFreshEntity(keeper);
        }

        BlockPos gliderAnchor = origin.below(18);
        for (int i = 0; i < 2; i++) {
            SporeGliderEntity glider = ModEntities.SPORE_GLIDER.get().create(level.getLevel());
            if (glider != null) {
                double gx = origin.getX() + (i == 0 ? 0.5D : 1.5D);
                double gy = origin.getY() - (i == 0 ? 14.0D : 24.0D);
                double gz = origin.getZ() + (i == 0 ? 0.5D : -1.0D);
                glider.moveTo(gx, gy, gz, random.nextFloat() * 360.0F, 0.0F);
                glider.setHomeAnchor(gliderAnchor);
                glider.finalizeSpawn(level, level.getCurrentDifficultyAt(gliderAnchor), MobSpawnType.STRUCTURE, null);
                glider.setPersistenceRequired();
                level.addFreshEntity(glider);
            }
        }

        return true;
    }

    private static Direction horizontalFacingTowardCenter(int lx, int lz) {
        if (Math.abs(lx) >= Math.abs(lz)) {
            return lx >= 0 ? Direction.WEST : Direction.EAST;
        }
        return lz >= 0 ? Direction.NORTH : Direction.SOUTH;
    }

    private static boolean isSuitableSurfaceTerrain(WorldGenLevel level, BlockPos origin, BlockPos ground) {
        int[][] samples = {
                {6, 0}, {-6, 0}, {0, 6}, {0, -6},
                {4, 4}, {4, -4}, {-4, 4}, {-4, -4}
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

    private static BlockPos findSurfaceFloor(WorldGenLevel level, int x, int baseY, int z, int maxDeltaY) {
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
                && !state.is(ModBlocks.MYCELIAL_RIFT.get())
                && !state.is(ModBlocks.SOVEREIGN_NEXUS.get())
                && !state.is(ModBlocks.RECLAIMED_BLOOM.get());
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
