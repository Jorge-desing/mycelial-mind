package com.alex.mycelialmind.world;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.FluidState;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Pure, deterministic terrain-quality and placement-safety rules for Overworld
 * Mycelial features (biome exclusions, permanent spawn safety, vanilla surface
 * structure avoidance, 2-ring slope evaluation, subterranean fluid checks, and
 * adaptive organic foundation anchoring).
 */
public final class TerrainPlacementPolicy {
    public static final TagKey<Biome> SURFACE_EXCLUDED_BIOMES = TagKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "surface_excluded")
    );

    private static final Set<ResourceKey<Structure>> PROTECTED_VANILLA_SURFACE_STRUCTURES = Set.of(
            BuiltinStructures.PILLAGER_OUTPOST,
            BuiltinStructures.DESERT_PYRAMID,
            BuiltinStructures.JUNGLE_TEMPLE,
            BuiltinStructures.SWAMP_HUT,
            BuiltinStructures.IGLOO,
            BuiltinStructures.WOODLAND_MANSION,
            BuiltinStructures.RUINED_PORTAL_STANDARD,
            BuiltinStructures.RUINED_PORTAL_DESERT,
            BuiltinStructures.RUINED_PORTAL_JUNGLE,
            BuiltinStructures.RUINED_PORTAL_SWAMP,
            BuiltinStructures.RUINED_PORTAL_MOUNTAIN
    );

    public record SlopeEvaluation(int minY, int maxY, int deltaY, boolean valid) {
    }

    public record CavernAnchorResult(int floorY, int headroom, boolean valid) {
    }

    private TerrainPlacementPolicy() {
    }

    public static boolean isWithinSpawnSafeZone(int x, int z, int spawnX, int spawnZ, int minDistance) {
        if (minDistance <= 0) {
            return false;
        }
        long dx = (long) x - spawnX;
        long dz = (long) z - spawnZ;
        return dx * dx + dz * dz < (long) minDistance * minDistance;
    }

    public static SlopeEvaluation evaluateSlopeMetrics(int[] sampleHeights, int maxAllowedDelta) {
        if (sampleHeights == null || sampleHeights.length == 0) {
            return new SlopeEvaluation(0, 0, Integer.MAX_VALUE, false);
        }
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int h : sampleHeights) {
            if (h < minY) {
                minY = h;
            }
            if (h > maxY) {
                maxY = h;
            }
        }
        int deltaY = maxY - minY;
        return new SlopeEvaluation(minY, maxY, deltaY, deltaY <= maxAllowedDelta);
    }

    public static boolean isSubterraneanFluidSafe(int waterBlocks, int lavaBlocks, int totalSamples) {
        if (lavaBlocks > 0 || waterBlocks > 12) {
            return false;
        }
        return totalSamples <= 0 || ((double) waterBlocks / (double) totalSamples) <= 0.12D;
    }

    public static int computeAdaptiveFoundationDepth(int topY, int solidY, int maxDepth) {
        if (maxDepth <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(maxDepth, topY - solidY));
    }

    public static boolean isExcludedSurfaceBiome(LevelReader level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(SURFACE_EXCLUDED_BIOMES)
                || biome.is(BiomeTags.IS_OCEAN)
                || biome.is(BiomeTags.IS_RIVER)
                || biome.is(BiomeTags.IS_BEACH);
    }

    /**
     * Checks whether an already-generated nearby chunk references or starts a protected
     * vanilla surface structure (villages, outposts, temples, etc.) without force-loading
     * neighboring chunks during worldgen.
     */
    public static boolean isNearVanillaSurfaceStructure(WorldGenLevel level, BlockPos origin, int chunkRadius) {
        if (level == null || origin == null) {
            return false;
        }
        Optional<Registry<Structure>> optRegistry = level.registryAccess().registry(Registries.STRUCTURE);
        if (optRegistry.isEmpty()) {
            return false;
        }
        Registry<Structure> structReg = optRegistry.get();
        ChunkPos centerChunk = new ChunkPos(origin);
        int radius = Math.max(0, chunkRadius);

        for (int cx = centerChunk.x - radius; cx <= centerChunk.x + radius; cx++) {
            for (int cz = centerChunk.z - radius; cz <= centerChunk.z + radius; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                ChunkAccess chunk = level.getChunk(cx, cz, ChunkStatus.STRUCTURE_REFERENCES, false);
                if (chunk == null) {
                    continue;
                }
                for (var entry : chunk.getAllReferences().entrySet()) {
                    if (!entry.getValue().isEmpty() && isProtectedVanillaStructure(structReg, entry.getKey())) {
                        return true;
                    }
                }
                for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
                    if (entry.getValue() != null && entry.getValue().isValid()
                            && isProtectedVanillaStructure(structReg, entry.getKey())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isProtectedVanillaStructure(Registry<Structure> registry, Structure structure) {
        if (structure == null) {
            return false;
        }
        Optional<ResourceKey<Structure>> optKey = registry.getResourceKey(structure);
        if (optKey.isEmpty()) {
            return false;
        }
        ResourceKey<Structure> key = optKey.get();
        if (PROTECTED_VANILLA_SURFACE_STRUCTURES.contains(key)) {
            return true;
        }
        return registry.getHolder(key).map(holder -> holder.is(StructureTags.VILLAGE)).orElse(false);
    }

    /**
     * Evaluates two concentric octagonal sample rings around {@code origin} for solid
     * non-fluid footing and bounds {@code maxY - minY <= maxSlopeDelta}.
     */
    public static boolean hasSuitableTwoRingSurfaceTerrain(
            WorldGenLevel level,
            BlockPos origin,
            int innerRadius,
            int outerRadius,
            int maxSlopeDelta,
            int maxSearchDelta
    ) {
        if (level == null || origin == null || level.isOutsideBuildHeight(origin)) {
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
                || !groundState.isFaceSturdy(level, ground, Direction.UP)) {
            return false;
        }

        int[][] innerOffsets = ringOffsets(innerRadius);
        int[][] outerOffsets = ringOffsets(outerRadius);
        int[] heights = new int[1 + innerOffsets.length + outerOffsets.length];
        heights[0] = ground.getY();

        int idx = 1;
        for (int[] offset : innerOffsets) {
            Integer sampleY = probeRingColumn(level, origin, ground, offset[0], offset[1], maxSearchDelta);
            if (sampleY == null) {
                return false;
            }
            heights[idx++] = sampleY;
        }
        for (int[] offset : outerOffsets) {
            Integer sampleY = probeRingColumn(level, origin, ground, offset[0], offset[1], maxSearchDelta);
            if (sampleY == null) {
                return false;
            }
            heights[idx++] = sampleY;
        }

        return evaluateSlopeMetrics(heights, maxSlopeDelta).valid();
    }

    private static int[][] ringOffsets(int r) {
        int safeR = Math.max(1, r);
        int d = Math.max(1, (int) Math.round(safeR * 0.7071D));
        return new int[][]{
                {safeR, 0}, {-safeR, 0}, {0, safeR}, {0, -safeR},
                {d, d}, {d, -d}, {-d, d}, {-d, -d}
        };
    }

    private static Integer probeRingColumn(
            WorldGenLevel level,
            BlockPos origin,
            BlockPos ground,
            int dx,
            int dz,
            int maxSearchDelta
    ) {
        BlockPos airProbe = origin.offset(dx, 0, dz);
        BlockPos gProbe = ground.offset(dx, 0, dz);
        if (level.isOutsideBuildHeight(gProbe)
                || !level.getFluidState(airProbe).isEmpty()
                || !level.getFluidState(gProbe).isEmpty()) {
            return null;
        }
        for (int step = 0; step <= maxSearchDelta; step++) {
            int[] deltas = step == 0 ? new int[]{0} : new int[]{step, -step};
            for (int dy : deltas) {
                BlockPos cand = gProbe.offset(0, dy, 0);
                BlockPos above = cand.above();
                if (!level.isOutsideBuildHeight(cand)
                        && !level.isOutsideBuildHeight(above)
                        && level.getFluidState(cand).isEmpty()
                        && level.getFluidState(above).isEmpty()
                        && level.getBlockState(cand).isFaceSturdy(level, cand, Direction.UP)
                        && isOpenOrReplaceable(level.getBlockState(above))) {
                    return cand.getY();
                }
            }
        }
        return null;
    }

    private static boolean isOpenOrReplaceable(BlockState state) {
        return state.isAir() || state.canBeReplaced();
    }

    /**
     * Rejects sinkhole generation when the subterranean throat/cavern volume intersects
     * lava pockets or substantial aquifers.
     */
    public static boolean hasSafeSubterraneanSinkholeColumn(WorldGenLevel level, BlockPos origin) {
        if (level == null || origin == null) {
            return false;
        }
        int waterBlocks = 0;
        int lavaBlocks = 0;
        int totalSamples = 0;

        for (int dy = -1; dy >= -21; dy -= 2) {
            for (int dx = -4; dx <= 4; dx += 2) {
                for (int dz = -4; dz <= 4; dz += 2) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (level.isOutsideBuildHeight(p)) {
                        continue;
                    }
                    totalSamples++;
                    FluidState fluid = level.getFluidState(p);
                    if (fluid.is(FluidTags.LAVA)) {
                        lavaBlocks++;
                    } else if (fluid.is(FluidTags.WATER)) {
                        waterBlocks++;
                    }
                }
            }
        }

        int[] dxSamples = {-6, -3, 0, 3, 6};
        int[] dzSamples = {-12, -8, -4, 0, 4, 8, 12};
        for (int dy = -22; dy >= -32; dy -= 2) {
            for (int dx : dxSamples) {
                for (int dz : dzSamples) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (level.isOutsideBuildHeight(p)) {
                        continue;
                    }
                    totalSamples++;
                    FluidState fluid = level.getFluidState(p);
                    if (fluid.is(FluidTags.LAVA)) {
                        lavaBlocks++;
                    } else if (fluid.is(FluidTags.WATER)) {
                        waterBlocks++;
                    }
                }
            }
        }

        return isSubterraneanFluidSafe(waterBlocks, lavaBlocks, totalSamples);
    }

    /**
     * Anchors a perimeter foundation block column downward onto sturdy non-liquid terrain
     * up to {@code maxDepth} blocks, blending {@code accentFill} every 3rd step.
     */
    public static void anchorOrganicFoundation(
            WorldGenLevel level,
            BlockPos startPos,
            int maxDepth,
            BlockState primaryFill,
            BlockState accentFill,
            Predicate<BlockPos> canOverwrite
    ) {
        if (level == null || startPos == null || maxDepth <= 0 || primaryFill == null) {
            return;
        }
        for (int d = 0; d < maxDepth; d++) {
            BlockPos probe = startPos.below(d);
            if (level.isOutsideBuildHeight(probe)) {
                return;
            }
            BlockState existing = level.getBlockState(probe);
            boolean sturdyDry = existing.isFaceSturdy(level, probe, Direction.UP)
                    && level.getFluidState(probe).isEmpty()
                    && !existing.liquid();
            if (sturdyDry && d > 0) {
                return;
            }
            if (canOverwrite == null || canOverwrite.test(probe)) {
                BlockState fill = (d % 3 == 1 && accentFill != null) ? accentFill : primaryFill;
                level.setBlock(probe, fill, 2);
            }
            if (sturdyDry) {
                return;
            }
        }
    }

    /**
     * Scans a pre-sampled vertical column for a dry solid floor with at least {@code minHeadroom}
     * contiguous dry open blocks above it, picking the valid floor closest to {@code preferredY}.
     */
    public static CavernAnchorResult findCavernFloorInColumn(
            boolean[] solidByOffset,
            boolean[] fluidByOffset,
            int baseY,
            int minHeadroom,
            int preferredY
    ) {
        if (solidByOffset == null || fluidByOffset == null
                || solidByOffset.length == 0
                || solidByOffset.length != fluidByOffset.length
                || minHeadroom <= 0) {
            return new CavernAnchorResult(baseY, 0, false);
        }
        int n = solidByOffset.length;
        int bestFloorY = baseY;
        int bestHeadroom = 0;
        int bestDist = Integer.MAX_VALUE;
        boolean found = false;

        for (int i = 0; i <= n - 1 - minHeadroom; i++) {
            if (!solidByOffset[i] || fluidByOffset[i]) {
                continue;
            }
            int headroom = 0;
            for (int k = i + 1; k < n; k++) {
                if (!solidByOffset[k] && !fluidByOffset[k]) {
                    headroom++;
                } else {
                    break;
                }
            }
            if (headroom >= minHeadroom) {
                int floorY = baseY + i;
                int dist = Math.abs(floorY - preferredY);
                if (!found || dist < bestDist) {
                    found = true;
                    bestDist = dist;
                    bestFloorY = floorY;
                    bestHeadroom = headroom;
                }
            }
        }
        return new CavernAnchorResult(bestFloorY, bestHeadroom, found);
    }

    /**
     * Computes the median of candidate cavern floor heights clamped to {@code [minY, maxY]}.
     */
    public static int selectBestCavernAnchorY(int[] candidateFloorYs, int fallbackY, int minY, int maxY) {
        if (candidateFloorYs == null || candidateFloorYs.length == 0) {
            return Math.max(minY, Math.min(maxY, fallbackY));
        }
        int[] sorted = Arrays.copyOf(candidateFloorYs, candidateFloorYs.length);
        Arrays.sort(sorted);
        int median = sorted[sorted.length / 2];
        return Math.max(minY, Math.min(maxY, median));
    }

    /**
     * Probes 5 noise columns around {@code (centerX, centerZ)} via {@link ChunkGenerator#getBaseColumn}
     * without forcing chunk loads, returning a median dry cavern floor Y with required headroom.
     */
    public static OptionalInt findCavernPlacementY(
            ChunkGenerator generator,
            LevelHeightAccessor heightAccessor,
            RandomState randomState,
            int centerX,
            int centerZ,
            int minY,
            int maxY,
            int minHeadroom,
            int preferredY,
            int sampleRadius
    ) {
        if (generator == null || heightAccessor == null || randomState == null) {
            return OptionalInt.empty();
        }
        int clampedMinY = Math.max(heightAccessor.getMinBuildHeight() + 1, minY);
        int scanTopY = Math.min(heightAccessor.getMaxBuildHeight() - 1, maxY + minHeadroom);
        if (scanTopY <= clampedMinY + minHeadroom) {
            return OptionalInt.empty();
        }
        int span = scanTopY - clampedMinY + 1;
        int[][] probes = {
                {0, 0},
                {sampleRadius, 0},
                {-sampleRadius, 0},
                {0, sampleRadius},
                {0, -sampleRadius}
        };
        int[] validFloors = new int[probes.length];
        int validCount = 0;
        boolean centerValid = false;

        for (int p = 0; p < probes.length; p++) {
            NoiseColumn column = generator.getBaseColumn(
                    centerX + probes[p][0], centerZ + probes[p][1], heightAccessor, randomState);
            boolean[] solid = new boolean[span];
            boolean[] fluid = new boolean[span];
            for (int i = 0; i < span; i++) {
                BlockState state = column.getBlock(clampedMinY + i);
                boolean hasFluid = !state.getFluidState().isEmpty();
                fluid[i] = hasFluid;
                solid[i] = !hasFluid && !state.isAir() && !state.canBeReplaced();
            }
            CavernAnchorResult res = findCavernFloorInColumn(solid, fluid, clampedMinY, minHeadroom, preferredY);
            if (res.valid()) {
                validFloors[validCount++] = res.floorY();
                if (p == 0) {
                    centerValid = true;
                }
            }
        }

        if ((!centerValid && validCount < 3) || validCount < 2) {
            return OptionalInt.empty();
        }
        int[] trimmed = Arrays.copyOf(validFloors, validCount);
        return OptionalInt.of(selectBestCavernAnchorY(trimmed, preferredY, minY, maxY));
    }
}
