package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.entity.ColonyWardenEntity;
import com.alex.mycelialmind.entity.MycelialStalkerEntity;
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
 * Procedural 17x31x17 Outer End Spore Spire anchored onto solid outer End islands
 * ({@code r >= 1000m}, {@code Y >= 50}) with a stepped End-Stone foundation skirt,
 * four purpur-chitin buttresses, dual-tier sanctuary galleries, beacon spire,
 * Incubator + Sentinel nodes, treasure vault, and Stalker / Warden guardians.
 */
public class EndSpirePiece extends StructurePiece {
    private static final int RADIUS = 8;
    private final BlockPos center;
    private boolean spawnedGuardians;

    public EndSpirePiece(BlockPos center) {
        super(ModStructures.END_SPIRE_PIECE.get(), 0, new BoundingBox(
                center.getX() - RADIUS, center.getY() - 8, center.getZ() - RADIUS,
                center.getX() + RADIUS, center.getY() + 22, center.getZ() + RADIUS
        ));
        this.center = center;
        this.spawnedGuardians = false;
    }

    public EndSpirePiece(CompoundTag tag) {
        super(ModStructures.END_SPIRE_PIECE.get(), tag);
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
        BlockState endBricks = Blocks.END_STONE_BRICKS.defaultBlockState();
        BlockState purpur = Blocks.PURPUR_BLOCK.defaultBlockState();
        BlockState purpurPillar = Blocks.PURPUR_PILLAR.defaultBlockState();
        BlockState amethyst = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState obsidian = Blocks.OBSIDIAN.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();

        // 1) Grounding skirt (dy = -8..-1) and base dais floor (dy = 0)
        for (int dx = -6; dx <= 6; dx++) {
            for (int dz = -6; dz <= 6; dz++) {
                double dist = Math.hypot(dx, dz);
                if (dist > 5.6) {
                    continue;
                }
                for (int dy = 1; dy <= 5; dy++) {
                    if (dist <= 4.6) {
                        setIfInside(level, box, center.offset(dx, dy, dz), air);
                    }
                }
                BlockState floorState = dist >= 4.4
                        ? endBricks
                        : ((Math.abs(dx) <= 1 || Math.abs(dz) <= 1) ? purpur : archBrick);
                setIfInside(level, box, center.offset(dx, 0, dz), floorState);

                for (int dy = -1; dy >= -8; dy--) {
                    double maxR = Math.max(2.0, 5.4 + dy * 0.42);
                    if (dist > maxR) {
                        break;
                    }
                    BlockPos below = center.offset(dx, dy, dz);
                    if (!box.isInside(below) || level.isOutsideBuildHeight(below)) {
                        break;
                    }
                    BlockState existing = level.getBlockState(below);
                    if (dy <= -2 && !existing.isAir() && existing.isSolid() && existing.getFluidState().isEmpty()) {
                        break;
                    }
                    level.setBlock(below, (dy % 2 == 0) ? endBricks : purpur, 2);
                }
            }
        }

        // 2) 4 corner buttress towers + perimeter rings at dy=5 & dy=11
        for (int[] c : new int[][]{{-3, -3}, {3, -3}, {-3, 3}, {3, 3}}) {
            for (int dy = 1; dy <= 11; dy++) {
                setIfInside(level, box, center.offset(c[0], dy, c[1]), (dy % 3 == 0) ? purpurPillar : chitinPillar);
            }
        }
        for (int k = -3; k <= 3; k++) {
            setIfInside(level, box, center.offset(k, 5, -3), archBrick);
            setIfInside(level, box, center.offset(k, 5, 3), archBrick);
            setIfInside(level, box, center.offset(-3, 5, k), archBrick);
            setIfInside(level, box, center.offset(3, 5, k), archBrick);

            setIfInside(level, box, center.offset(k, 11, -3), purpur);
            setIfInside(level, box, center.offset(k, 11, 3), purpur);
            setIfInside(level, box, center.offset(-3, 11, k), purpur);
            setIfInside(level, box, center.offset(3, 11, k), purpur);
        }

        // 3) Upper observatory deck (dy=12) anchored onto corner rings & central core
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (Math.abs(dx) == 3 && Math.abs(dz) == 3) {
                    continue;
                }
                setIfInside(level, box, center.offset(dx, 12, dz),
                        (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) ? purpur : archBrick);
            }
        }
        // Central spinal core dy = 1..20 (grounded directly on floor dy=0 & bridging decks)
        for (int dy = 1; dy <= 20; dy++) {
            BlockState coreState = dy <= 6
                    ? obsidian
                    : (dy <= 14 ? chitinPillar : (dy >= 19 ? amberVein : amethyst));
            setIfInside(level, box, center.offset(0, dy, 0), coreState);
            if (dy <= 14 && (dy == 5 || dy == 11)) {
                setIfInside(level, box, center.offset(1, dy, 0), archBrick);
                setIfInside(level, box, center.offset(-1, dy, 0), archBrick);
                setIfInside(level, box, center.offset(0, dy, 1), archBrick);
                setIfInside(level, box, center.offset(0, dy, -1), archBrick);
            }
        }

        // Exterior spore bulb brackets mounted cleanly on cardinal deck corners
        for (int[] bulb : new int[][]{{-2, 12, 0}, {2, 12, 0}}) {
            setIfInside(level, box, center.offset(bulb[0], bulb[1] + 1, bulb[2]), ModBlocks.SPORE_BULB.get().defaultBlockState());
        }

        // 4) Nodes & Reliquary Chest
        BlockPos incubatorPos = center.offset(0, 1, -2);
        setIfInside(level, box, incubatorPos, ModBlocks.INCUBATOR_NODE.get().defaultBlockState());
        registerNodeIfInside(level, box, incubatorPos, SynapticNodeType.INCUBATOR);

        BlockPos sentinelPos = center.offset(0, 13, -1);
        setIfInside(level, box, sentinelPos, ModBlocks.SENTINEL_NODE.get().defaultBlockState());
        registerNodeIfInside(level, box, sentinelPos, SynapticNodeType.SENTINEL);

        BlockPos chestPos = center.offset(2, 1, 0);
        if (box.isInside(chestPos)) {
            level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 2);
            if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
                MycelialLootSystem.populateStructureChest(
                        level, chestPos, chest, MycelialLootSystem.END_SPIRE_VAULT, random, HiveProgressionTier.ADAPTIVE, false
                );
            }
        }

        // 5) One-time Stalker + Colony Warden guardians
        BlockPos guardAnchor = center.offset(-2, 1, 2);
        if (!spawnedGuardians && box.isInside(guardAnchor) && level.getLevel() instanceof ServerLevel serverLevel) {
            spawnedGuardians = true;
            for (int[] s : new int[][]{{-2, 2}, {2, 2}}) {
                MycelialStalkerEntity stalker = ModEntities.MYCELIAL_STALKER.get().create(serverLevel);
                if (stalker != null) {
                    stalker.moveTo(center.getX() + s[0] + 0.5, center.getY() + 1.0, center.getZ() + s[1] + 0.5, 0.0F, 0.0F);
                    stalker.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(center), MobSpawnType.STRUCTURE, null);
                    stalker.setPersistenceRequired();
                    serverLevel.addFreshEntity(stalker);
                }
            }
            ColonyWardenEntity warden = ModEntities.COLONY_WARDEN.get().create(serverLevel);
            if (warden != null) {
                warden.moveTo(center.getX() + 0.5, center.getY() + 1.0, center.getZ() + 3.5, 180.0F, 0.0F);
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
