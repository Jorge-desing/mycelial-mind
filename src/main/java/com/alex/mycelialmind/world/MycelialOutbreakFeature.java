package com.alex.mycelialmind.world;

import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.structure.StructureProgressionPolicy;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;


/** Generates a low mycelial fissure around a synaptic node. */
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
        if (enforceWorldgenGates && isOverworld && !isSubterranean) {
            return false;
        }
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

        if (level.isOutsideBuildHeight(center.below())
                || !level.getBlockState(center.below()).isSolid()
                || !level.getFluidState(center).isEmpty()) {
            return false;
        }

        SynapticNodeType nodeType = StructureProgressionPolicy.pickOutbreakNodeType(tier, random.nextFloat());
        int minSpacing = StructureProgressionPolicy.outbreakMinSpacing(tier);
        if (level.getLevel() instanceof ServerLevel serverLevel) {
            PurificationZoneTracker tracker = PurificationZoneTracker.get(serverLevel);
            if (tracker.isProtected(center) || !tracker.tryReserveNodePosition(center, minSpacing, nodeType)) {
                return false;
            }
        }

        BlockState nodeState = switch (nodeType) {
            case SENTINEL -> ModBlocks.SENTINEL_NODE.get().defaultBlockState();
            case INCUBATOR -> ModBlocks.INCUBATOR_NODE.get().defaultBlockState();
            case RELAY, SYMBIOTIC -> ModBlocks.MYCELIAL_NODE.get().defaultBlockState();
        };
        level.setBlock(center, nodeState, 2);
        buildFissure(level, center, random, isSubterranean);
        if (isOverworld && isSubterranean && level.getBlockEntity(center)
                instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity node) {
            node.markOutbreakOrigin();
        }

        BlockPos vaultCenter = center.below(4);
        if (random.nextFloat() < StructureProgressionPolicy.sealedBioVaultChance(tier)
                && canPlaceSealedBioVault(level, vaultCenter)) {
            placeSealedBioVault(level, vaultCenter, random, tier);
        }

        return true;
    }

    private static void buildFissure(WorldGenLevel level, BlockPos center, RandomSource random, boolean underground) {
        Direction axis = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        Direction side = axis.getClockWise();
        int halfLength = 4 + random.nextInt(3);
        int bend = random.nextBoolean() ? 1 : -1;
        for (int sign : new int[]{-1, 1}) {
            int previousY = center.getY();
            for (int distance = 1; distance <= halfLength; distance++) {
                int shift = distance >= 3 ? bend : 0;
                BlockPos column = center.relative(axis, sign * distance).relative(side, shift);
                BlockPos floor = findSlopeFloor(level, column.getX(), previousY, column.getZ(), 1);
                if (floor == null || Math.abs(floor.getY() - previousY) > 1 || !canExcavateFissure(level, floor)) {
                    break;
                }
                previousY = floor.getY();
                carveFissureCell(level, floor, distance == 1 || (distance == 2 && random.nextBoolean()));
                if (distance == 3) {
                    BlockPos connector = floor.relative(side, -bend);
                    if (canExcavateFissure(level, connector)) {
                        carveFissureCell(level, connector, false);
                    }
                }
                if (distance <= 2 || (distance == 3 && sign == 1)) {
                    BlockPos branch = floor.relative(side, sign == 1 ? 1 : -1);
                    if (canExcavateFissure(level, branch)) {
                        carveFissureCell(level, branch, distance == 1);
                    }
                }
                BlockPos lip = floor.relative(side, sign == 1 ? -1 : 1);
                if (isSafeToOverwrite(level, lip) && canReplaceGround(level.getBlockState(lip))
                        && level.getFluidState(lip).isEmpty()) {
                    level.setBlock(lip, ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 2);
                }
                BlockPos rim = floor.relative(side, sign == 1 ? 2 : -2);
                if (isSafeToOverwrite(level, rim) && canReplaceGround(level.getBlockState(rim))
                        && level.getFluidState(rim).isEmpty()) {
                    level.setBlock(rim, ModBlocks.MYCELIAL_STONE.get().defaultBlockState(), 2);
                    if (distance % 2 == 0 && isOpenAirOrReplaceable(level, rim.above())) {
                        level.setBlock(rim.above(), (distance == 2 ? ModBlocks.SPORE_VENT.get()
                                : ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()).defaultBlockState(), 2);
                    }
                }
                if (underground && distance <= 3) {
                    for (int height = 3; height <= 8; height++) {
                        BlockPos ceiling = floor.above(height);
                        if (level.isOutsideBuildHeight(ceiling)) {
                            break;
                        }
                        if (level.getBlockState(ceiling).isSolid()) {
                            if (isOpenAirOrReplaceable(level, ceiling.below()) && random.nextBoolean()) {
                                level.setBlock(ceiling.below(), ModBlocks.MYCELIAL_TENDRILS.get().defaultBlockState(), 2);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    private static boolean canExcavateFissure(WorldGenLevel level, BlockPos floor) {
        BlockPos bottom = floor.below();
        return !level.isOutsideBuildHeight(bottom)
                && isSafeToOverwrite(level, floor)
                && isSafeToOverwrite(level, bottom)
                && canReplaceGround(level.getBlockState(floor))
                && canReplaceGround(level.getBlockState(bottom))
                && level.getBlockState(bottom).isSolid()
                && level.getFluidState(floor).isEmpty()
                && level.getFluidState(bottom).isEmpty()
                && isOpenAirOrReplaceable(level, floor.above());
    }

    private static void carveFissureCell(WorldGenLevel level, BlockPos floor, boolean luminous) {
        level.setBlock(floor.below(), (luminous ? ModBlocks.LUMINESCENT_AMBER_VEIN.get()
                : ModBlocks.MYCELIAL_STONE.get()).defaultBlockState(), 2);
        level.setBlock(floor, Blocks.AIR.defaultBlockState(), 2);
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

    private static boolean isSafeToOverwrite(WorldGenLevel level, BlockPos pos) {
        return WorldgenFeatureSupport.isSafeToOverwrite(level, pos);
    }

    private static boolean isOpenAirOrReplaceable(WorldGenLevel level, BlockPos pos) {
        return WorldgenFeatureSupport.isOpenAirOrReplaceable(level, pos);
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
