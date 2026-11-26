package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.entity.ColonyWardenEntity;
import com.alex.mycelialmind.entity.PyroShroomEntity;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModStructures;
import com.alex.mycelialmind.world.MycelialLootSystem;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/**
 * Procedural 19x25x19 Nether Mycelial Breach anchored above the lava sea ({@code Y >= 33})
 * with floor foundation piers, four-way chitin arches, optional ceiling anchor tendrils,
 * sentinel sanctuary node, reliquary chest, and Pyro-Shroom / Colony Warden guardians.
 */
public class NetherBreachPiece extends StructurePiece {
    private static final int RADIUS = 9;
    private final BlockPos center;
    private boolean spawnedGuardians;

    public NetherBreachPiece(BlockPos center) {
        super(ModStructures.NETHER_BREACH_PIECE.get(), 0, new BoundingBox(
                center.getX() - RADIUS, center.getY() - 10, center.getZ() - RADIUS,
                center.getX() + RADIUS, center.getY() + 14, center.getZ() + RADIUS
        ));
        this.center = center;
        this.spawnedGuardians = false;
    }

    public NetherBreachPiece(CompoundTag tag) {
        super(ModStructures.NETHER_BREACH_PIECE.get(), tag);
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
        BlockState amberVein = ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState();
        BlockState basalt = Blocks.POLISHED_BASALT.defaultBlockState();
        BlockState netherBricks = Blocks.NETHER_BRICKS.defaultBlockState();
        BlockState crimsonNylium = Blocks.CRIMSON_NYLIUM.defaultBlockState();
        BlockState cryingObsidian = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState shroomlight = Blocks.SHROOMLIGHT.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1) Floor plinth & interior cavern headroom clearance
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                double dist = Math.hypot(dx, dz);
                if (dist > 6.6) {
                    continue;
                }
                for (int dy = 1; dy <= 9; dy++) {
                    if (dist <= 5.6) {
                        setIfInside(level, box, center.offset(dx, dy, dz), air);
                    }
                }
                BlockState floorState;
                if (dist >= 5.2) {
                    floorState = archBrick;
                } else if (Math.abs(dx) <= 1 || Math.abs(dz) <= 1) {
                    floorState = basalt;
                } else if (((Math.abs(dx) + Math.abs(dz)) & 1) == 0) {
                    floorState = netherBricks;
                } else {
                    floorState = crimsonNylium;
                }
                setIfInside(level, box, center.offset(dx, 0, dz), floorState);
                if (dist <= 5.2) {
                    setIfInside(level, box, center.offset(dx, -1, dz), netherBricks);
                }
            }
        }

        // 2) Continuous floor piers descending onto dry Netherrack/Basalt
        int[][] pierOffsets = {
                {-5, 0}, {5, 0}, {0, -5}, {0, 5},
                {-4, -4}, {4, -4}, {-4, 4}, {4, 4},
                {0, 0}
        };
        for (int[] off : pierOffsets) {
            for (int dy = -1; dy >= -10; dy--) {
                BlockPos probe = center.offset(off[0], dy, off[1]);
                if (!box.isInside(probe) || level.isOutsideBuildHeight(probe)) {
                    break;
                }
                BlockState existing = level.getBlockState(probe);
                if (dy <= -2 && !existing.isAir() && existing.isSolid() && existing.getFluidState().isEmpty()) {
                    break;
                }
                level.setBlock(probe, (dy % 2 == 0) ? archBrick : basalt, 2);
            }
        }

        // 3) 4 cardinal columns + arched canopy cross & central core crown
        for (int[] col : new int[][]{{-5, 0}, {5, 0}, {0, -5}, {0, 5}}) {
            for (int dy = 1; dy <= 8; dy++) {
                setIfInside(level, box, center.offset(col[0], dy, col[1]), (dy == 1 || dy == 8) ? basalt : chitinPillar);
            }
        }
        for (int d = -5; d <= 5; d++) {
            int archY = Math.abs(d) >= 4 ? 8 : 9;
            setIfInside(level, box, center.offset(d, archY, 0), (d == 0) ? cryingObsidian : archBrick);
            setIfInside(level, box, center.offset(0, archY, d), (d == 0) ? cryingObsidian : archBrick);
            if (Math.abs(d) == 2 || Math.abs(d) == 3) {
                setIfInside(level, box, center.offset(d, 8, 0), chitinPillar);
                setIfInside(level, box, center.offset(0, 8, d), chitinPillar);
            }
        }
        setIfInside(level, box, center.offset(0, 8, 0), shroomlight);
        setIfInside(level, box, center.offset(1, 9, 1), amberVein);
        setIfInside(level, box, center.offset(-1, 9, 1), amberVein);
        setIfInside(level, box, center.offset(1, 9, -1), amberVein);
        setIfInside(level, box, center.offset(-1, 9, -1), amberVein);

        // 4) Optional ceiling anchor tendrils up to dy=14 if cavern ceiling lies within dy=15..24
        for (int[] anchor : new int[][]{{-2, 0}, {2, 0}, {0, -2}, {0, 2}}) {
            boolean hasCeiling = false;
            for (int dy = 15; dy <= 24; dy++) {
                BlockPos ceilProbe = center.offset(anchor[0], dy, anchor[1]);
                if (!level.isOutsideBuildHeight(ceilProbe)) {
                    BlockState s = level.getBlockState(ceilProbe);
                    if (!s.isAir() && s.isSolid() && s.getFluidState().isEmpty()) {
                        hasCeiling = true;
                        break;
                    }
                }
            }
            if (hasCeiling) {
                for (int dy = 10; dy <= 14; dy++) {
                    setIfInside(level, box, center.offset(anchor[0], dy, anchor[1]),
                            (dy == 12) ? amberVein : chitinPillar);
                }
            }
        }

        // 5) Altar, Sentinel Node, Vent & Reliquary Chest
        setIfInside(level, box, center.above(1), ModBlocks.HIVE_ALTAR.get().defaultBlockState());
        BlockPos sentinelPos = center.offset(0, 1, -3);
        setIfInside(level, box, sentinelPos, ModBlocks.SENTINEL_NODE.get().defaultBlockState());
        registerNodeIfInside(level, box, sentinelPos, SynapticNodeType.SENTINEL);
        setIfInside(level, box, center.offset(0, 1, 3), ModBlocks.SPORE_VENT.get().defaultBlockState());

        BlockPos chestPos = center.offset(-3, 1, 0);
        if (box.isInside(chestPos)) {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                MycelialLootSystem.populateStructureChest(
                        level, chestPos, chest, MycelialLootSystem.CITADEL_CRIMSON_CHAPEL, random, HiveProgressionTier.ADAPTIVE, false
                );
            }
        }

        // 6) One-time Pyro-Shroom & Colony Warden guardians
        BlockPos guardAnchor = center.offset(2, 1, 2);
        if (!spawnedGuardians && box.isInside(guardAnchor) && level.getLevel() instanceof ServerLevel serverLevel) {
            spawnedGuardians = true;
            for (int[] g : new int[][]{{2, 2}, {-2, -2}}) {
                PyroShroomEntity pyro = ModEntities.PYRO_SHROOM.get().create(serverLevel);
                if (pyro != null) {
                    pyro.moveTo(center.getX() + g[0] + 0.5, center.getY() + 1.0, center.getZ() + g[1] + 0.5, 0.0F, 0.0F);
                    pyro.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(center), MobSpawnType.STRUCTURE, null);
                    pyro.setPersistenceRequired();
                    serverLevel.addFreshEntity(pyro);
                }
            }
            ColonyWardenEntity warden = ModEntities.COLONY_WARDEN.get().create(serverLevel);
            if (warden != null) {
                warden.moveTo(center.getX() + 3.5, center.getY() + 1.0, center.getZ() + 0.5, 90.0F, 0.0F);
                warden.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(center), MobSpawnType.STRUCTURE, null);
                warden.promoteRank(EvolutionRank.MUTATED);
                warden.setPersistenceRequired();
                serverLevel.addFreshEntity(warden);
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
