package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.entity.ColonyWardenEntity;
import com.alex.mycelialmind.entity.SporeBehemothEntity;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModStructures;
import com.alex.mycelialmind.world.MycelialLootSystem;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Procedural 53x50x53 Organic-Alien Imperial Fungal Citadel for the Apex Matriarch:
 * - Abyssal Hanging Roots & stalactites (dy = -11..-1)
 * - Subterranean Throne Crypt (dy = -5..-1) with Throne Relic Chest
 * - Double Imperial Fungal Cap (dy = 14..38) with purple glass, amethyst & obsidian-amethyst lattice gills
 * - 6 Giant Coiling Tentacles wrapping the exterior hull
 * - South Processional Bridge, 4 Anatomical Effigies, Amber/Crimson Strain Chapels & Royal Apse
 * - Persistent Spore Behemoth + 2 Colony Wardens + Matriarch Altar throne
 */
public class MatriarchCitadelPiece extends StructurePiece {
    private static final int RADIUS = 26;
    private static final int HEIGHT = 38;
    private final BlockPos center;
    private boolean spawnedGuardians;

    public MatriarchCitadelPiece(BlockPos center) {
        super(ModStructures.MATRIARCH_CITADEL_PIECE.get(), 0, new BoundingBox(
                center.getX() - RADIUS, center.getY() - 32, center.getZ() - RADIUS,
                center.getX() + RADIUS, center.getY() + HEIGHT, center.getZ() + RADIUS
        ));
        this.center = center;
        this.spawnedGuardians = false;
    }

    public MatriarchCitadelPiece(CompoundTag tag) {
        super(ModStructures.MATRIARCH_CITADEL_PIECE.get(), tag);
        this.center = new BlockPos(tag.getInt("CenterX"), tag.getInt("CenterY"), tag.getInt("CenterZ"));
        this.spawnedGuardians = tag.getBoolean("SpawnedGuardians");
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("CenterX", center.getX());
        tag.putInt("CenterY", center.getY());
        tag.putInt("CenterZ", center.getZ());
        tag.putBoolean("SpawnedGuardians", spawnedGuardians);
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator generator,
            RandomSource random,
            BoundingBox box,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState chitinPillar = ModBlocks.CHITIN_PILLAR.get().defaultBlockState();
        BlockState lattice = ModBlocks.BIO_CHITIN_LATTICE.get().defaultBlockState();
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState mycelium = ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState();
        BlockState stone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();

        var accent = ArchitecturalStructureAdapter.resolveAccentForLevel(level);
        var palette = ArchitecturalStructureAdapter.resolvePalette(accent);
        boolean customAccent = accent.hasCustomAccent();

        BlockState deepslate = customAccent ? palette.primaryWall() : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState deepslateTiles = customAccent ? palette.primaryWall() : Blocks.DEEPSLATE_TILES.defaultBlockState();
        BlockState basalt = customAccent ? palette.structuralPillar() : Blocks.POLISHED_BASALT.defaultBlockState();
        BlockState bone = Blocks.BONE_BLOCK.defaultBlockState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState purpleGlass = Blocks.PURPLE_STAINED_GLASS.defaultBlockState();
        BlockState slab = customAccent ? palette.slab() : Blocks.DEEPSLATE_BRICK_SLAB.defaultBlockState();
        BlockState wall = customAccent ? palette.wallOrFence() : Blocks.DEEPSLATE_BRICK_WALL.defaultBlockState();
        BlockState stairBase = customAccent ? palette.stairBase() : Blocks.DEEPSLATE_BRICK_STAIRS.defaultBlockState();
        BlockState chandelierLight = customAccent ? palette.glowBlock() : Blocks.SHROOMLIGHT.defaultBlockState();

        // 1) Arena floor, cavern clearance & stepped conical inverted corbels
        for (int dx = -23; dx <= 23; dx++) {
            for (int dz = -23; dz <= 23; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > 23.5) {
                    continue;
                }
                if (Math.abs(dx) <= 2 || Math.abs(dz) <= 2 || Math.abs(Math.abs(dx) - Math.abs(dz)) <= 2) {
                    for (int dy = -1; dy >= -4; dy--) {
                        double innerR = 18.5 + (dy + 1) * 0.5;
                        double outerR = 22.5 + (dy + 1) * 0.75;
                        if (dist >= innerR && dist <= outerR) {
                            setIfInside(level, box, center.offset(dx, dy, dz), (dy <= -3) ? archBrick : basalt);
                        }
                    }
                }
                BlockState floorState;
                if (dist >= 20.5) {
                    floorState = archBrick;
                } else if (((Math.abs(dx) + Math.abs(dz)) % 5 == 0) && dist <= 18.0) {
                    floorState = mycelium;
                } else if (dist <= 8.0 && (Math.abs(dx) <= 1 || Math.abs(dz) <= 1)) {
                    floorState = basalt;
                } else {
                    floorState = deepslate;
                }
                setIfInside(level, box, center.offset(dx, 0, dz), floorState);

                for (int dy = 1; dy <= 34; dy++) {
                    if (dist < 21.5) {
                        setIfInside(level, box, center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }

        // 8 Ground-Reaching Mycelial Roots (6-connected via VoxelStructureOps) descending until solid terrain
        for (int i = 0; i < 8; i++) {
            double ang = (Math.PI / 4.0) * i + 0.15;
            List<BlockPos> rootWaypoints = new ArrayList<>();
            int startRx = (int) Math.round(Math.cos(ang) * 18.0);
            int startRz = (int) Math.round(Math.sin(ang) * 18.0);
            rootWaypoints.add(center.offset(startRx, 0, startRz));
            for (int dy = -1; dy >= -30; dy--) {
                double flare = Math.min(4.5, (-dy) * 0.22);
                double rad = 16.0 + flare;
                int rx = (int) Math.round(Math.cos(ang) * rad + Math.sin(dy * 0.32 + i) * 0.8);
                int rz = (int) Math.round(Math.sin(ang) * rad + Math.cos(dy * 0.32 + i) * 0.8);
                BlockPos rp = center.offset(rx, dy, rz);
                if (rp.getY() <= level.getMinBuildHeight() + 1) {
                    break;
                }
                rootWaypoints.add(rp);
                if (dy <= -2 && box.isInside(rp)) {
                    BlockState existing = level.getBlockState(rp);
                    if (!existing.isAir() && existing.isSolid() && !existing.is(ModBlocks.MYCELIAL_ARCH_BRICK.get())) {
                        for (Direction d : Direction.Plane.HORIZONTAL) {
                            BlockPos spread = rp.relative(d);
                            if (box.isInside(spread) && level.getBlockState(spread).isSolid()) {
                                level.setBlock(spread, mycelium, 2);
                            }
                        }
                        break;
                    }
                }
            }
            VoxelStructureOps.placeFaceConnectedPath(level, box, rootWaypoints, step ->
                    (step % 5 == 0) ? amberVein : ((step % 3 == 0) ? cryingObsidian : ((step % 2 == 0) ? chitinPillar : archBrick)));
        }

        // 2) Crypt Beneath the Throne (dy = -5..-1, radius 8)
        for (int dy = -5; dy <= -1; dy++) {
            for (int dx = -8; dx <= 8; dx++) {
                for (int dz = -8; dz <= 8; dz++) {
                    double r = Math.sqrt(dx * dx + dz * dz);
                    if (r > 8.2) {
                        continue;
                    }
                    BlockPos p = center.offset(dx, dy, dz);
                    if (dy == -5) {
                        setIfInside(level, box, p, ((Math.abs(dx) + Math.abs(dz)) % 3 == 0) ? cryingObsidian : archBrick);
                    } else if (dy == -1) {
                        if (!(Math.abs(dx) == 6 && dz >= -1 && dz <= 2)) {
                            setIfInside(level, box, p, archBrick);
                        }
                    } else if (r >= 6.4) {
                        setIfInside(level, box, p, (Math.abs(dx) == 5 && Math.abs(dz) == 5) ? chitinPillar : archBrick);
                    } else {
                        setIfInside(level, box, p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        for (int sx : new int[]{-4, 4}) {
            for (int sz : new int[]{-4, 4}) {
                for (int dy = -4; dy <= -2; dy++) {
                    setIfInside(level, box, center.offset(sx, dy, sz), chitinPillar);
                }
            }
        }
        for (int side : new int[]{-6, 6}) {
            for (int step = 0; step < 4; step++) {
                setIfInside(level, box, center.offset(side, -step, step - 1),
                        stairBase.setValue(StairBlock.FACING, Direction.SOUTH));
                for (int clearY = 1; clearY <= 3; clearY++) {
                    setIfInside(level, box, center.offset(side, -step + clearY, step - 1), Blocks.AIR.defaultBlockState());
                }
            }
        }
        HiveProgressionTier tier = StructureProgressionPolicy.resolveTier(level);
        BlockPos cryptChestPos = center.offset(0, -4, 0);
        if (box.isInside(cryptChestPos)) {
            level.setBlock(cryptChestPos, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(cryptChestPos) instanceof ChestBlockEntity cryptChest) {
                MycelialLootSystem.populateStructureChest(
                        level, cryptChestPos, cryptChest, MycelialLootSystem.CITADEL_ROYAL_CRYPT, random, tier, false);
            }
        }

        // 3) Lower Organic Stem Wall (dy = 1..13) with lancet purple-glass & amethyst windows
        for (int dy = 1; dy <= 13; dy++) {
            double stemR = 21.5 - Math.sin((dy / 13.0) * Math.PI) * 1.4;
            for (int dx = -23; dx <= 23; dx++) {
                for (int dz = -23; dz <= 23; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (Math.abs(dist - stemR) > 1.15) {
                        continue;
                    }
                    if (dz > 16 && Math.abs(dx) <= 3 && dy <= 7) {
                        continue;
                    }
                    double angle = Math.atan2(dz, dx);
                    double ribFactor = Math.abs(Math.sin(angle * 4.0));
                    BlockPos p = center.offset(dx, dy, dz);
                    if (ribFactor < 0.22) {
                        setIfInside(level, box, p, (dy % 3 == 0) ? chitinPillar : archBrick);
                    } else if (dy >= 5 && dy <= 10 && ribFactor > 0.72) {
                        setIfInside(level, box, p, (dy % 2 == 0) ? purpleGlass : lattice);
                    } else {
                        setIfInside(level, box, p, (dy <= 2) ? basalt : ((dy % 2 == 0) ? deepslateTiles : stone));
                    }
                }
            }
        }

        // 4) Double Imperial Fungal Cap (70/20/10 deterministic stratification: stone/archBrick ribs/focal accents)
        for (int dy = 14; dy <= 26; dy++) {
            double t = (dy - 14) / 12.0;
            double capR = 24.8 * Math.cos(t * Math.PI * 0.42);
            for (int dx = -25; dx <= 25; dx++) {
                for (int dz = -25; dz <= 25; dz++) {
                    double angle = Math.atan2(dz, dx);
                    double scallop = (dy <= 17) ? 1.1 * Math.sin(angle * 8.0) : 0.0;
                    double r = Math.sqrt(dx * dx + dz * dz);
                    double effR = capR + scallop;
                    if (r <= effR && r >= Math.max(0.0, effR - 3.2)) {
                        BlockPos p = center.offset(dx, dy, dz);
                        BlockState capBlock;
                        if (dy == 14 && r >= 20.0) {
                            capBlock = (Math.abs(Math.sin(angle * 4.0)) < 0.25) ? amethyst : purpleGlass;
                        } else if (Math.abs(Math.sin(angle * 4.0)) < 0.18) {
                            capBlock = (dy % 4 == 0) ? amberVein : archBrick;
                        } else if (dy == 18 || dy == 23) {
                            capBlock = deepslateTiles;
                        } else {
                            capBlock = stone;
                        }
                        setIfInside(level, box, p, capBlock);
                    }
                }
            }
        }
        // Upper Crown Cap (dy = 27..37, 70/20/10 deterministic stratification, welded to Main Cap at dy = 26)
        for (int dy = 27; dy <= 37; dy++) {
            double t = (dy - 27) / 10.0;
            double crownR = 11.5 * Math.cos(t * Math.PI * 0.46);
            double minR = (dy == 27) ? 4.0 : Math.max(0.0, crownR - 2.8);
            for (int dx = -12; dx <= 12; dx++) {
                for (int dz = -12; dz <= 12; dz++) {
                    double r = Math.sqrt(dx * dx + dz * dz);
                    if (r <= crownR && r >= minR) {
                        double angle = Math.atan2(dz, dx);
                        BlockPos p = center.offset(dx, dy, dz);
                        BlockState b;
                        if (dy >= 35) {
                            b = (r <= 1.5) ? amberVein : amethyst;
                        } else if (dy == 27) {
                            b = purpleGlass;
                        } else if (Math.abs(Math.sin(angle * 4.0)) < 0.18) {
                            b = archBrick;
                        } else {
                            b = deepslateTiles;
                        }
                        setIfInside(level, box, p, b);
                    }
                }
            }
        }

        // 5) 6 Giant Coiling Tentacles wrapping the exterior hull (6-connected via VoxelStructureOps)
        for (int arm = 0; arm < 6; arm++) {
            double baseAngle = (Math.PI / 3.0) * arm + 0.25;
            List<BlockPos> waypoints = new ArrayList<>();
            for (int step = 0; step <= 32; step++) {
                double frac = step / 32.0;
                double ang = baseAngle + frac * 1.45;
                double rad = 22.0 - Math.sin(frac * Math.PI) * 2.5 - frac * 1.5;
                int tx = (int) Math.round(Math.cos(ang) * rad);
                int tz = (int) Math.round(Math.sin(ang) * rad);
                int ty = (int) Math.round(frac * 27.0);
                if (tz > 15 && Math.abs(tx) <= 4 && ty <= 8) {
                    continue;
                }
                if (waypoints.isEmpty() && ty > 0) {
                    double anchorRad = 20.5;
                    waypoints.add(center.offset(
                            (int) Math.round(Math.cos(ang) * anchorRad),
                            ty,
                            (int) Math.round(Math.sin(ang) * anchorRad)));
                }
                waypoints.add(center.offset(tx, ty, tz));
            }
            VoxelStructureOps.placeFaceConnectedPath(level, box, waypoints, step ->
                    (step % 6 == 0) ? amberVein : ((step % 3 == 0) ? amethyst : chitinPillar));
        }

        // 6) South Processional Bridge & Imperial Portico (dz = 16..24) + stepped buttress plinths
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (dir != Direction.SOUTH) {
                BlockPos plinth = center.relative(dir, 23).above();
                setIfInside(level, box, plinth, stairBase.setValue(StairBlock.FACING, dir.getOpposite()));
            }
        }
        for (int dz = 16; dz <= 24; dz++) {
            for (int dx = -3; dx <= 3; dx++) {
                setIfInside(level, box, center.offset(dx, 0, dz), (Math.abs(dx) <= 1) ? basalt : deepslate);
            }
            setIfInside(level, box, center.offset(-4, 1, dz), wall);
            setIfInside(level, box, center.offset(4, 1, dz), wall);
        }
        for (int ox : new int[]{-4, 4}) {
            for (int dy = 1; dy <= 6; dy++) {
                setIfInside(level, box, center.offset(ox, dy, 22), chitinPillar);
            }
            setIfInside(level, box, center.offset(ox, 7, 22), amberVein);
        }
        for (int dx = -3; dx <= 3; dx++) {
            setIfInside(level, box, center.offset(dx, 7, 19), archBrick);
        }
        setIfInside(level, box, center.offset(-2, 6, 19), stairBase.setValue(StairBlock.FACING, Direction.WEST).setValue(StairBlock.HALF, Half.TOP));
        setIfInside(level, box, center.offset(2, 6, 19), stairBase.setValue(StairBlock.FACING, Direction.EAST).setValue(StairBlock.HALF, Half.TOP));

        // 7) 4 Anatomical Guardian Effigies (center.offset(+-9, 1..8, +-9))
        for (int ex : new int[]{-9, 9}) {
            for (int ez : new int[]{-9, 9}) {
                setIfInside(level, box, center.offset(ex, 1, ez), basalt);
                setIfInside(level, box, center.offset(ex, 2, ez), chitinPillar);
                setIfInside(level, box, center.offset(ex, 3, ez), bone);
                setIfInside(level, box, center.offset(ex, 4, ez), bone);
                setIfInside(level, box, center.offset(ex, 5, ez), chitinPillar);
                setIfInside(level, box, center.offset(ex, 6, ez), archBrick);
                setIfInside(level, box, center.offset(ex, 7, ez), amberVein);
                setIfInside(level, box, center.offset(ex, 8, ez), wall);
                int armDir = ex < 0 ? 1 : -1;
                setIfInside(level, box, center.offset(ex + armDir, 5, ez), wall);
                setIfInside(level, box, center.offset(ex + armDir, 4, ez), stairBase.setValue(StairBlock.FACING, ex < 0 ? Direction.WEST : Direction.EAST).setValue(StairBlock.HALF, Half.TOP));
            }
        }

        // 8) Elevated triforium catwalk ring at dy = 10 (radius 14.5..17.5) with balustrades
        for (int dx = -18; dx <= 18; dx++) {
            for (int dz = -18; dz <= 18; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist >= 14.5 && dist <= 17.5) {
                    BlockState catwalk = ((Math.abs(dx) + Math.abs(dz)) % 3 == 0) ? lattice : slab;
                    setIfInside(level, box, center.offset(dx, 10, dz), catwalk);
                    if (dist <= 15.3 && (Math.abs(dx) + Math.abs(dz)) % 2 == 0) {
                        setIfInside(level, box, center.offset(dx, 11, dz), wall);
                    }
                }
            }
        }
        // Widened 2-block West/East triforium staircases with >= 3 blocks headroom
        for (int step = 0; step < 10; step++) {
            int sy = step + 1;
            int sz = -5 + step;
            for (int wx : new int[]{-15, -14, 14, 15}) {
                for (int fillY = 1; fillY < sy; fillY++) {
                    setIfInside(level, box, center.offset(wx, fillY, sz), deepslate);
                }
                setIfInside(level, box, center.offset(wx, sy, sz), stairBase.setValue(StairBlock.FACING, Direction.SOUTH));
                for (int clearY = 1; clearY <= 4; clearY++) {
                    setIfInside(level, box, center.offset(wx, sy + clearY, sz), Blocks.AIR.defaultBlockState());
                }
            }
        }

        // 9) 5 Monumental Hanging Chandeliers welded to the dome via VoxelStructureOps
        VoxelStructureOps.placeAnchoredChain(level, box, center.offset(0, 36, 0), 8, archBrick, chandelierLight);
        for (int[] c : new int[][]{{-8, 0}, {8, 0}, {0, -8}, {0, 8}}) {
            VoxelStructureOps.placeAnchoredChain(level, box, center.offset(c[0], 25, c[1]), 5, archBrick, chandelierLight);
        }

        // 10) Central Throne Dais + Matriarch Altar
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d <= 5.2) {
                    setIfInside(level, box, center.offset(dx, 1, dz), archBrick);
                }
                if (d <= 3.2) {
                    setIfInside(level, box, center.offset(dx, 2, dz), bone);
                }
                if (d <= 1.5) {
                    setIfInside(level, box, center.offset(dx, 3, dz), (dx == 0 && dz == 0) ? amberVein : archBrick);
                }
            }
        }
        setIfInside(level, box, center.above(4), ModBlocks.MATRIARCH_ALTAR.get().defaultBlockState());

        // 11) Synaptic Nodes on catwalk towers (4 Sentinel + 2 Incubator)
        BlockPos[] sentinels = {
                center.offset(0, 11, -16),
                center.offset(0, 11, 16),
                center.offset(-16, 11, 0),
                center.offset(16, 11, 0)
        };
        BlockPos[] incubators = {
                center.offset(-11, 11, -11),
                center.offset(11, 11, -11)
        };
        for (BlockPos sp : sentinels) {
            setIfInside(level, box, sp.below(), archBrick);
            setIfInside(level, box, sp, ModBlocks.SENTINEL_NODE.get().defaultBlockState());
            registerNodeIfInside(level, box, sp, SynapticNodeType.SENTINEL);
        }
        for (BlockPos ip : incubators) {
            setIfInside(level, box, ip.below(), archBrick);
            setIfInside(level, box, ip, ModBlocks.INCUBATOR_NODE.get().defaultBlockState());
            registerNodeIfInside(level, box, ip, SynapticNodeType.INCUBATOR);
        }

        // 12) Amber Chapel (West), Crimson Chapel (East), and Royal Apse (North) + Synaptic Floor Conduits
        placeStrainChapel(level, box, center.offset(-19, 1, 0), true, random, tier);
        placeStrainChapel(level, box, center.offset(19, 1, 0), false, random, tier);
        placeRoyalApse(level, box, center.offset(0, 1, -19), random, tier);
        VoxelStructureOps.placeSynapticConduit(level, box, center, center.offset(-19, 0, 0), basalt, amberVein);
        VoxelStructureOps.placeSynapticConduit(level, box, center, center.offset(19, 0, 0), basalt, amberVein);
        VoxelStructureOps.placeSynapticConduit(level, box, center, center.offset(0, 0, -19), basalt, amberVein);
        VoxelStructureOps.placeSynapticConduit(level, box, center, center.offset(-14, 0, -5), basalt, amberVein);
        VoxelStructureOps.placeSynapticConduit(level, box, center, center.offset(14, 0, -5), basalt, amberVein);

        if (customAccent) {
            for (int[] spireOffset : new int[][]{{-11, -6}, {11, -6}, {-11, 6}, {11, 6}}) {
                ArchitecturalStructureAdapter.placeCitadelReflectionSpire(
                        level, box, center.offset(spireOffset[0], 1, spireOffset[1]), accent);
            }
        }

        // 13) One-time persistent guardian spawn (Spore Behemoth + 2 Colony Wardens)
        BlockPos spawnAnchor = center.above(2);
        if (!spawnedGuardians && box.isInside(spawnAnchor) && level.getLevel() instanceof ServerLevel serverLevel) {
            spawnedGuardians = true;
            SporeBehemothEntity behemoth = ModEntities.SPORE_BEHEMOTH.get().create(serverLevel);
            if (behemoth != null) {
                behemoth.moveTo(center.getX() + 0.5, center.getY() + 1.5, center.getZ() + 9.5, 180.0F, 0.0F);
                behemoth.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(center), MobSpawnType.STRUCTURE, null);
                behemoth.setPersistenceRequired();
                serverLevel.addFreshEntity(behemoth);
            }
            EvolutionRank guardianRank = StructureProgressionPolicy.citadelGuardianRank(tier);
            for (int side : new int[]{-5, 5}) {
                ColonyWardenEntity warden = ModEntities.COLONY_WARDEN.get().create(serverLevel);
                if (warden != null) {
                    warden.moveTo(center.getX() + side + 0.5, center.getY() + 1.5, center.getZ() + 7.5, 180.0F, 0.0F);
                    warden.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(center), MobSpawnType.STRUCTURE, null);
                    if (guardianRank != EvolutionRank.BASE) {
                        warden.promoteRank(guardianRank);
                    }
                    warden.setPersistenceRequired();
                    serverLevel.addFreshEntity(warden);
                }
            }
        }
    }

    private void placeStrainChapel(WorldGenLevel level, BoundingBox box, BlockPos vaultPos, boolean amber, RandomSource random, HiveProgressionTier tier) {
        BlockState pillar = amber
                ? ModBlocks.CHITIN_PILLAR.get().defaultBlockState()
                : Blocks.CRIMSON_HYPHAE.defaultBlockState();
        BlockState glow = amber
                ? Blocks.OCHRE_FROGLIGHT.defaultBlockState()
                : Blocks.SHROOMLIGHT.defaultBlockState();
        BlockState accent = amber
                ? ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState()
                : Blocks.CRYING_OBSIDIAN.defaultBlockState();

        setIfInside(level, box, vaultPos.below(), ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState());
        setIfInside(level, box, vaultPos.north(), pillar);
        setIfInside(level, box, vaultPos.south(), pillar);
        setIfInside(level, box, vaultPos.north().above(), accent);
        setIfInside(level, box, vaultPos.south().above(), accent);
        setIfInside(level, box, vaultPos.above(3), glow);
        if (box.isInside(vaultPos)) {
            level.setBlock(vaultPos, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(vaultPos) instanceof ChestBlockEntity chest) {
                MycelialLootSystem.populateStructureChest(
                        level,
                        vaultPos,
                        chest,
                        amber ? MycelialLootSystem.CITADEL_AMBER_CHAPEL : MycelialLootSystem.CITADEL_CRIMSON_CHAPEL,
                        random,
                        tier,
                        false
                );
            }
        }
    }

    private void placeRoyalApse(WorldGenLevel level, BoundingBox box, BlockPos vaultPos, RandomSource random, HiveProgressionTier tier) {
        setIfInside(level, box, vaultPos.below(), ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState());
        setIfInside(level, box, vaultPos.west(), ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
        setIfInside(level, box, vaultPos.east(), ModBlocks.SPORE_VENT.get().defaultBlockState());
        if (box.isInside(vaultPos)) {
            level.setBlock(vaultPos, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(vaultPos) instanceof ChestBlockEntity chest) {
                MycelialLootSystem.populateStructureChest(
                        level, vaultPos, chest, MycelialLootSystem.CITADEL_ROYAL_CRYPT, random, tier, false);
            }
        }
    }

    private static void setIfInside(WorldGenLevel level, BoundingBox box, BlockPos pos, BlockState state) {
        if (box.isInside(pos)) {
            level.setBlock(pos, state, 2);
        }
    }

    private static void registerNodeIfInside(WorldGenLevel level, BoundingBox box, BlockPos pos, SynapticNodeType type) {
        if (box.isInside(pos) && level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker.get(serverLevel).addNode(pos, type);
        }
    }
}
