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
        if (TerrainPlacementPolicy.isExcludedSurfaceBiome(level, origin)
                || TerrainPlacementPolicy.isNearVanillaSurfaceStructure(level, origin, 1)) {
            return false;
        }
        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(level);
        int minSpawnDist = StructureProgressionPolicy.sinkholeMinSpawnDistance(tier);
        if (minSpawnDist > 0 && level.getLevel() instanceof ServerLevel serverLevel) {
            BlockPos spawn = serverLevel.getSharedSpawnPos();
            if (TerrainPlacementPolicy.isWithinSpawnSafeZone(
                    origin.getX(), origin.getZ(), spawn.getX(), spawn.getZ(), minSpawnDist)) {
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
                || !TerrainPlacementPolicy.hasSuitableTwoRingSurfaceTerrain(level, origin, 4, 8, 4, 4)
                || !TerrainPlacementPolicy.hasSafeSubterraneanSinkholeColumn(level, origin)) {
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
        BlockState water = Blocks.WATER.defaultBlockState();
        int spawnedRimStrands = 0;

        long noiseSeed = origin.asLong() ^ 0x53494E4B484F4C45L;
        var biomePalette = TerrainPlacementPolicy.resolveSurfaceBiomePalette(level, origin);

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
                    TerrainPlacementPolicy.anchorOrganicFoundation(
                            level, floorPos.below(), 8, stone, archBrick, pos -> isSafeToOverwrite(level, pos));
                    if (dist <= 5.2D && random.nextFloat() < 0.45F) {
                        BlockPos crownLip = floorPos.above();
                        if (isOpenAirOrReplaceable(level, crownLip)) {
                            safeSetBlock(level, crownLip, random.nextFloat() < 0.25F ? archBrick : mycelium);
                        }
                    }
                } else {
                    boolean outerBiomeBlend = dist > 7.2D;
                    boolean slopedRim = Math.abs(floorPos.getY() - (origin.getY() - 1)) >= 1;
                    safeSetBlock(level, floorPos, random.nextFloat() < 0.72F ? mycelium : (slopedRim ? biomePalette.trimStone() : stone));
                    TerrainPlacementPolicy.anchorOrganicFoundation(
                            level,
                            floorPos.below(),
                            dist <= 8.0D ? 6 : 4,
                            (outerBiomeBlend || slopedRim) ? biomePalette.outerSubFoundation() : stone,
                            outerBiomeBlend ? biomePalette.rimAccent() : null,
                            pos -> isSafeToOverwrite(level, pos)
                    );
                    BlockPos top = floorPos.above();
                    if (isOpenAirOrReplaceable(level, top)) {
                        if (dist > 6.8D && spawnedRimStrands < 6 && random.nextFloat() < 0.14F) {
                            BlockPos inward = top.offset(Integer.compare(origin.getX(), top.getX()) * 2, 0,
                                    Integer.compare(origin.getZ(), top.getZ()) * 2);
                            var strand = new com.alex.mycelialmind.entity.HyphalVeinEntity(
                                    level.getLevel(), top, inward, 5, true, 0.72F);
                            strand.setStrengthFloor(2);
                            level.addFreshEntity(strand);
                            spawnedRimStrands++;
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

        // 3a) Lateral Gallery North: Humid Sap Chamber (dz = -7 .. -17) with shallow spring & abundant Root Sap Moss
        for (int dz = -7; dz >= -17; dz--) {
            boolean doorway = dz >= -8;
            int halfW = doorway ? 2 : 5;
            for (int dx = -halfW; dx <= halfW; dx++) {
                for (int dy = -25; dy >= -32; dy--) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    boolean outerShell = dy == -32
                            || dy == -25
                            || (!doorway && (Math.abs(dx) == 5 || dz == -17))
                            || (!doorway && dy == -26 && Math.abs(dx) >= 3)
                            || (doorway && (Math.abs(dx) == 2 && dy == -26));
                    if (outerShell) {
                        BlockState shellMat = (!doorway && dy >= -29 && dy <= -26 && random.nextFloat() < 0.16F)
                                ? amberVein
                                : (random.nextFloat() < 0.18F ? archBrick : stone);
                        safeSetBlock(level, p, shellMat);
                        if (dy == -32) {
                            anchorBaseDown(level, p.below(), 3, stone);
                        }
                    } else if (dy == -31) {
                        boolean inSpring = !doorway && Math.abs(dx) <= 2 && dz <= -12 && dz >= -14;
                        if (inSpring) {
                            safeSetBlock(level, p, water);
                        } else {
                            safeSetBlock(level, p, sapMoss);
                        }
                    } else {
                        safeSetBlock(level, p, air);
                    }
                }
            }
        }
        // Scatter luminous flora & tendrils around North Sap Chamber edges
        for (int dz = -9; dz >= -16; dz--) {
            for (int dx = -4; dx <= 4; dx++) {
                if (Math.abs(dx) <= 2 && dz <= -12 && dz >= -14) {
                    continue;
                }
                BlockPos floor = origin.offset(dx, -31, dz);
                if (level.getBlockState(floor).isSolid()
                        && isOpenAirOrReplaceable(level, floor.above())
                        && (Math.abs(dx) == 4 || dz == -16)
                        && random.nextFloat() < 0.35F) {
                    safeSetBlock(level, floor.above(), random.nextFloat() < 0.65F ? cap : sporeBulb);
                }
                BlockPos ceilHang = origin.offset(dx, -26, dz);
                if (isOpenAirOrReplaceable(level, ceilHang)
                        && level.getBlockState(ceilHang.above()).isSolid()
                        && random.nextFloat() < 0.20F) {
                    safeSetBlock(level, ceilHang, tendrils);
                }
            }
        }

        // 3b) Lateral Gallery South: Winding Root Tunnel (dz = 7 .. 21) opening toward adjacent caves
        int targetFloorDy = findSouthCaveTargetFloorDy(level, origin);
        int floorDelta = targetFloorDy - (-31);
        for (int dz = 7; dz <= 21; dz++) {
            int cx = dz <= 9 ? 0 : (int) Math.round(Math.sin((dz - 9) * 0.45D) * 1.5D);
            int floorDy = dz <= 10
                    ? -31
                    : (-31 + Integer.signum(floorDelta) * Math.min(Math.abs(floorDelta), (dz - 9) / 2));
            boolean pillarRib = dz == 11 || dz == 15 || dz == 19;
            boolean southMouth = dz == 21;
            for (int ox = -2; ox <= 2; ox++) {
                int rx = cx + ox;
                for (int dy = floorDy + 5; dy >= floorDy - 1; dy--) {
                    BlockPos p = origin.offset(rx, dy, dz);
                    boolean insidePassage = (Math.abs(ox) <= 1 && dy >= floorDy + 1 && dy <= floorDy + 3)
                            || (ox == 0 && dy == floorDy + 4);
                    if (insidePassage) {
                        safeSetBlock(level, p, air);
                    } else if (!southMouth) {
                        if (dy == floorDy - 1) {
                            safeSetBlock(level, p, stone);
                            anchorBaseDown(level, p.below(), 3, stone);
                        } else if (dy == floorDy) {
                            BlockState fState = (Math.abs(ox) == 1 && random.nextFloat() < 0.25F) ? mycelium : stone;
                            safeSetBlock(level, p, fState);
                        } else if (pillarRib && Math.abs(ox) == 2 && dy >= floorDy + 1 && dy <= floorDy + 4) {
                            safeSetBlock(level, p, pillar);
                        } else if (pillarRib && dy == floorDy + 5 && Math.abs(ox) <= 1) {
                            safeSetBlock(level, p, archBrick);
                        } else if (Math.abs(ox) == 2 && dy == floorDy + 2 && random.nextFloat() < 0.30F) {
                            safeSetBlock(level, p, amberVein);
                        } else {
                            safeSetBlock(level, p, random.nextFloat() < 0.15F ? archBrick : stone);
                        }
                    } else if (dy == floorDy && Math.abs(ox) <= 1) {
                        safeSetBlock(level, p, stone);
                        anchorBaseDown(level, p.below(), 3, stone);
                    }
                }
            }
            if (!southMouth && (dz == 12 || dz == 18)) {
                BlockPos veinPos = origin.offset(cx, floorDy + 1, dz);
                if (level.getBlockState(veinPos.below()).isSolid()) {
                    var strand = new com.alex.mycelialmind.entity.HyphalVeinEntity(
                            level.getLevel(), veinPos, veinPos.south(2), 5, true, 0.72F);
                    strand.setStrengthFloor(2);
                    level.addFreshEntity(strand);
                }
            }
            if (dz == 15) {
                BlockPos ventTunnelPos = origin.offset(cx + 1, floorDy + 1, dz);
                if (level.getBlockState(ventTunnelPos.below()).isSolid()) {
                    safeSetBlock(level, ventTunnelPos, sporeVent);
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

        // Secret cracked-stone Root Vault chest alcove in NE North Chamber (4, -31, -16) behind cracked bricks
        safeSetBlock(level, origin.offset(3, -31, -16), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState());
        safeSetBlock(level, origin.offset(3, -30, -16), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState());
        safeSetBlock(level, origin.offset(4, -31, -15), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState());
        safeSetBlock(level, origin.offset(4, -30, -15), Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState());
        safeSetBlock(level, origin.offset(4, -32, -16), stone);
        BlockPos sapVaultPos = origin.offset(4, -31, -16);
        if (safeSetBlock(level, sapVaultPos, Blocks.CHEST.defaultBlockState())
                && level.getBlockEntity(sapVaultPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity sapChest) {
            MycelialLootSystem.populateStructureChest(
                    level, sapVaultPos, sapChest, MycelialLootSystem.SINKHOLE_ROOT_VAULT,
                    random, com.alex.mycelialmind.world.structure.StructureProgressionPolicy.resolveTier(level), false);
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

    private static int findSouthCaveTargetFloorDy(WorldGenLevel level, BlockPos origin) {
        int bestDy = -31;
        int bestDist = Integer.MAX_VALUE;
        for (int dz = 16; dz <= 24; dz += 2) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dy = -27; dy >= -33; dy--) {
                    BlockPos floor = origin.offset(dx, dy, dz);
                    if (level.isOutsideBuildHeight(floor) || level.isOutsideBuildHeight(floor.above(3))) {
                        continue;
                    }
                    if (level.getFluidState(floor).isEmpty()
                            && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)
                            && isOpenAirOrReplaceable(level, floor.above(1))
                            && isOpenAirOrReplaceable(level, floor.above(2))
                            && isOpenAirOrReplaceable(level, floor.above(3))) {
                        int dist = Math.abs(dy - (-31));
                        if (dist < bestDist) {
                            bestDist = dist;
                            bestDy = dy;
                        }
                    }
                }
            }
        }
        return bestDy;
    }

    private static Direction horizontalFacingTowardCenter(int lx, int lz) {
        if (Math.abs(lx) >= Math.abs(lz)) {
            return lx >= 0 ? Direction.WEST : Direction.EAST;
        }
        return lz >= 0 ? Direction.NORTH : Direction.SOUTH;
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
        TerrainPlacementPolicy.anchorOrganicFoundation(
                level, startPos, maxDepth, fill, null, pos -> isSafeToOverwrite(level, pos));
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
