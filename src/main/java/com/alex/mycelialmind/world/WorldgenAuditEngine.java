package com.alex.mycelialmind.world;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Optional advanced terrain generation auditor for Mycelial Mind.
 * Measures outbreak/hive density, nearest-neighbor spacing, synaptic network
 * connectivity, SurfaceHive structural integrity, and hive_sanctum Jigsaw grid
 * spacing, emitting both compact AI-friendly telemetry and structured JSON.
 */
public final class WorldgenAuditEngine {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")
            .withZone(ZoneOffset.UTC);

    public static final ResourceKey<StructureSet> HIVE_SANCTUM_SET = ResourceKey.create(
            Registries.STRUCTURE_SET,
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_sanctum")
    );

    private WorldgenAuditEngine() {
    }

    public record DistanceStats(int count, double min, double mean, double p50, double p90, double max) {
        public static DistanceStats empty() {
            return new DistanceStats(0, 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("count", count);
            obj.addProperty("min", round1(min));
            obj.addProperty("mean", round1(mean));
            obj.addProperty("p50", round1(p50));
            obj.addProperty("p90", round1(p90));
            obj.addProperty("max", round1(max));
            return obj;
        }
    }

    public record NetworkStats(
            int totalNodes,
            int activeLinks,
            int connectedComponents,
            int largestClusterSize,
            int isolatedNodes,
            double isolationRatio
    ) {
        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("total_nodes", totalNodes);
            obj.addProperty("active_links", activeLinks);
            obj.addProperty("connected_components", connectedComponents);
            obj.addProperty("largest_cluster_size", largestClusterSize);
            obj.addProperty("isolated_nodes", isolatedNodes);
            obj.addProperty("isolation_ratio", round3(isolationRatio));
            return obj;
        }
    }

    public record HiveIntegrityReport(
            BlockPos origin,
            int mycelialStoneCount,
            int creepingMyceliumCount,
            boolean hasIncubator,
            boolean hasSentinel,
            boolean hasChestWithExpectedLoot,
            boolean trackerRegistered,
            boolean valid
    ) {
        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("x", origin.getX());
            obj.addProperty("y", origin.getY());
            obj.addProperty("z", origin.getZ());
            obj.addProperty("mycelial_stone_count", mycelialStoneCount);
            obj.addProperty("creeping_mycelium_count", creepingMyceliumCount);
            obj.addProperty("has_incubator", hasIncubator);
            obj.addProperty("has_sentinel", hasSentinel);
            obj.addProperty("has_chest_with_expected_loot", hasChestWithExpectedLoot);
            obj.addProperty("tracker_registered", trackerRegistered);
            obj.addProperty("valid", valid);
            return obj;
        }
    }

    public record SanctumGridStats(
            boolean structureSetRegistered,
            int gridChunks,
            int spacingChunks,
            int separationChunks,
            String spreadType,
            int candidateCount,
            DistanceStats chunkNearestNeighbor,
            DistanceStats blockNearestNeighbor,
            boolean separationInvariantOk
    ) {
        public JsonObject toJson() {
            JsonObject obj = new JsonObject();
            obj.addProperty("structure_set_registered", structureSetRegistered);
            obj.addProperty("grid_chunks", gridChunks);
            obj.addProperty("spacing_chunks", spacingChunks);
            obj.addProperty("separation_chunks", separationChunks);
            obj.addProperty("spread_type", spreadType);
            obj.addProperty("candidate_count", candidateCount);
            obj.add("chunk_nearest_neighbor", chunkNearestNeighbor.toJson());
            obj.add("block_nearest_neighbor", blockNearestNeighbor.toJson());
            obj.addProperty("separation_invariant_ok", separationInvariantOk);
            return obj;
        }
    }

    public record WorldgenAuditReport(
            String dimension,
            long seed,
            BlockPos origin,
            int radiusChunks,
            int chunksScanned,
            long durationMs,
            int totalNodes,
            int relayNodes,
            int incubatorNodes,
            int sentinelNodes,
            int caveOutbreaks,
            int surfaceOutbreaks,
            int surfaceHives,
            int creepingMyceliumBlocks,
            int mycelialStoneBlocks,
            int floraBlocks,
            DistanceStats outbreakNearestXz,
            DistanceStats outbreakNearest3d,
            DistanceStats hiveNearestXz,
            DistanceStats outbreakToHiveNearestXz,
            DistanceStats nodeNearest3d,
            NetworkStats networkStats,
            List<HiveIntegrityReport> hiveIntegrityReports,
            SanctumGridStats sanctumGridStats,
            List<String> failures,
            boolean passed
    ) {
        public String toCompactAgentLine() {
            int validHives = (int) hiveIntegrityReports.stream().filter(HiveIntegrityReport::valid).count();
            return String.format(
                    Locale.ROOT,
                    "[Mycelial WorldgenAudit] %s | chunks=%d ms=%d | outbreaks=%d(cave=%d,surf=%d) hives=%d(valid=%d/%d) nodes=%d(R=%d,I=%d,S=%d) | nn_xz(outbreak=%.1f/%.1f/%.1f,hive=%.1f,o2h=%.1f) | net(links=%d,clusters=%d,max=%d,iso=%.2f) | sanctum(cand=%d,min_chk=%.1f,sep=%d)",
                    passed ? "PASS" : "FAIL",
                    chunksScanned,
                    durationMs,
                    caveOutbreaks + surfaceOutbreaks,
                    caveOutbreaks,
                    surfaceOutbreaks,
                    surfaceHives,
                    validHives,
                    hiveIntegrityReports.size(),
                    totalNodes,
                    relayNodes,
                    incubatorNodes,
                    sentinelNodes,
                    outbreakNearestXz.min(),
                    outbreakNearestXz.mean(),
                    outbreakNearestXz.p90(),
                    hiveNearestXz.mean(),
                    outbreakToHiveNearestXz.mean(),
                    networkStats.activeLinks(),
                    networkStats.connectedComponents(),
                    networkStats.largestClusterSize(),
                    networkStats.isolationRatio(),
                    sanctumGridStats.candidateCount(),
                    sanctumGridStats.chunkNearestNeighbor().min(),
                    sanctumGridStats.separationChunks()
            );
        }

        public JsonObject toJson() {
            JsonObject root = new JsonObject();

            JsonObject meta = new JsonObject();
            meta.addProperty("timestamp_utc", Instant.now().toString());
            meta.addProperty("dimension", dimension);
            meta.addProperty("seed", seed);
            meta.addProperty("origin_x", origin.getX());
            meta.addProperty("origin_y", origin.getY());
            meta.addProperty("origin_z", origin.getZ());
            meta.addProperty("radius_chunks", radiusChunks);
            meta.addProperty("chunks_scanned", chunksScanned);
            meta.addProperty("duration_ms", durationMs);
            root.add("meta", meta);

            JsonObject counts = new JsonObject();
            counts.addProperty("outbreaks_total", caveOutbreaks + surfaceOutbreaks);
            counts.addProperty("outbreaks_cave", caveOutbreaks);
            counts.addProperty("outbreaks_surface", surfaceOutbreaks);
            counts.addProperty("surface_hives", surfaceHives);
            counts.addProperty("nodes_total", totalNodes);
            counts.addProperty("nodes_relay", relayNodes);
            counts.addProperty("nodes_incubator", incubatorNodes);
            counts.addProperty("nodes_sentinel", sentinelNodes);
            counts.addProperty("creeping_mycelium_blocks", creepingMyceliumBlocks);
            counts.addProperty("mycelial_stone_blocks", mycelialStoneBlocks);
            counts.addProperty("flora_blocks", floraBlocks);
            root.add("counts", counts);

            JsonObject spacing = new JsonObject();
            spacing.add("outbreak_to_outbreak_xz", outbreakNearestXz.toJson());
            spacing.add("outbreak_to_outbreak_3d", outbreakNearest3d.toJson());
            spacing.add("hive_to_hive_xz", hiveNearestXz.toJson());
            spacing.add("outbreak_to_hive_xz", outbreakToHiveNearestXz.toJson());
            spacing.add("node_to_node_3d", nodeNearest3d.toJson());
            root.add("spacing", spacing);

            root.add("synaptic_network", networkStats.toJson());

            JsonObject integrity = new JsonObject();
            integrity.addProperty("inspected_hives", hiveIntegrityReports.size());
            integrity.addProperty("valid_hives", hiveIntegrityReports.stream().filter(HiveIntegrityReport::valid).count());
            JsonArray hiveArr = new JsonArray();
            for (HiveIntegrityReport report : hiveIntegrityReports) {
                hiveArr.add(report.toJson());
            }
            integrity.add("hives", hiveArr);
            root.add("surface_hive_integrity", integrity);

            root.add("sanctum_jigsaw_grid", sanctumGridStats.toJson());

            JsonObject invariants = new JsonObject();
            invariants.addProperty("passed", passed);
            JsonArray failArr = new JsonArray();
            for (String f : failures) {
                failArr.add(f);
            }
            invariants.add("failures", failArr);
            root.add("invariants", invariants);
            root.addProperty("passed", passed);
            return root;
        }
    }

    public static WorldgenAuditReport runAudit(ServerLevel level, BlockPos origin, int radiusChunks, int sanctumGridChunks) {
        long startNs = System.nanoTime();
        ChunkPos centerChunk = new ChunkPos(origin);

        Block relayBlock = ModBlocks.MYCELIAL_NODE.get();
        Block incubatorBlock = ModBlocks.INCUBATOR_NODE.get();
        Block sentinelBlock = ModBlocks.SENTINEL_NODE.get();
        Block myceliumBlock = ModBlocks.CREEPING_MYCELIUM.get();
        Block stoneBlock = ModBlocks.MYCELIAL_STONE.get();
        Block ventBlock = ModBlocks.SPORE_VENT.get();
        Block capBlock = ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get();
        Block bulbBlock = ModBlocks.SPORE_BULB.get();
        Block tendrilsBlock = ModBlocks.MYCELIAL_TENDRILS.get();

        Predicate<BlockState> sectionInteresting = state ->
                state.is(relayBlock)
                        || state.is(incubatorBlock)
                        || state.is(sentinelBlock)
                        || state.is(myceliumBlock)
                        || state.is(stoneBlock)
                        || state.is(ventBlock)
                        || state.is(capBlock)
                        || state.is(bulbBlock)
                        || state.is(tendrilsBlock);

        Map<BlockPos, SynapticNodeType> detectedNodes = new HashMap<>();
        int myceliumCount = 0;
        int stoneCount = 0;
        int floraCount = 0;
        int chunksScanned = 0;

        ServerChunkCache chunkSource = level.getChunkSource();
        int minCx = centerChunk.x - radiusChunks;
        int maxCx = centerChunk.x + radiusChunks;
        int minCz = centerChunk.z - radiusChunks;
        int maxCz = centerChunk.z + radiusChunks;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                chunkSource.getChunkFuture(cx, cz, ChunkStatus.FULL, true);
            }
        }

        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                LevelChunk chunk = level.getChunk(cx, cz);
                chunksScanned++;
                LevelChunkSection[] sections = chunk.getSections();
                int minSectionY = chunk.getMinSection();

                for (int sIdx = 0; sIdx < sections.length; sIdx++) {
                    LevelChunkSection section = sections[sIdx];
                    if (section == null || section.hasOnlyAir() || !section.maybeHas(sectionInteresting)) {
                        continue;
                    }
                    int baseY = (minSectionY + sIdx) << 4;
                    int baseX = chunk.getPos().getMinBlockX();
                    int baseZ = chunk.getPos().getMinBlockZ();
                    for (int ly = 0; ly < 16; ly++) {
                        int wy = baseY + ly;
                        for (int lz = 0; lz < 16; lz++) {
                            int wz = baseZ + lz;
                            for (int lx = 0; lx < 16; lx++) {
                                BlockState state = section.getBlockState(lx, ly, lz);
                                Block block = state.getBlock();
                                if (block == relayBlock) {
                                    detectedNodes.put(new BlockPos(baseX + lx, wy, wz), SynapticNodeType.RELAY);
                                } else if (block == incubatorBlock) {
                                    detectedNodes.put(new BlockPos(baseX + lx, wy, wz), SynapticNodeType.INCUBATOR);
                                } else if (block == sentinelBlock) {
                                    detectedNodes.put(new BlockPos(baseX + lx, wy, wz), SynapticNodeType.SENTINEL);
                                } else if (block == myceliumBlock) {
                                    myceliumCount++;
                                } else if (block == stoneBlock) {
                                    stoneCount++;
                                } else if (block == ventBlock || block == capBlock || block == bulbBlock || block == tendrilsBlock) {
                                    floraCount++;
                                }
                            }
                        }
                    }
                }
            }
        }

        // Classify SurfaceHives vs Outbreaks (SurfaceHive places INCUBATOR at origin and SENTINEL at origin+3)
        List<BlockPos> surfaceHives = new ArrayList<>();
        Set<BlockPos> hiveOwnedNodes = new HashSet<>();
        List<HiveIntegrityReport> hiveIntegrityReports = new ArrayList<>();
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);

        for (Map.Entry<BlockPos, SynapticNodeType> entry : detectedNodes.entrySet()) {
            if (entry.getValue() != SynapticNodeType.INCUBATOR) {
                continue;
            }
            BlockPos pos = entry.getKey();
            BlockPos sentinelPos = pos.above(3);
            if (detectedNodes.get(sentinelPos) == SynapticNodeType.SENTINEL) {
                surfaceHives.add(pos);
                hiveOwnedNodes.add(pos);
                hiveOwnedNodes.add(sentinelPos);
                hiveIntegrityReports.add(inspectSurfaceHive(level, tracker, pos));
            }
        }

        List<BlockPos> caveOutbreaks = new ArrayList<>();
        List<BlockPos> surfaceOutbreaks = new ArrayList<>();
        int relayNodes = 0;
        int incubatorNodes = 0;
        int sentinelNodes = 0;
        for (Map.Entry<BlockPos, SynapticNodeType> entry : detectedNodes.entrySet()) {
            switch (entry.getValue()) {
                case RELAY -> relayNodes++;
                case INCUBATOR -> incubatorNodes++;
                case SENTINEL -> sentinelNodes++;
            }
            BlockPos pos = entry.getKey();
            if (hiveOwnedNodes.contains(pos)) {
                continue;
            }
            int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX(), pos.getZ());
            if (pos.getY() >= Math.min(level.getSeaLevel() - 4, surfaceY - 6)) {
                surfaceOutbreaks.add(pos);
            } else {
                caveOutbreaks.add(pos);
            }
        }

        List<BlockPos> allOutbreaks = new ArrayList<>(caveOutbreaks.size() + surfaceOutbreaks.size());
        allOutbreaks.addAll(caveOutbreaks);
        allOutbreaks.addAll(surfaceOutbreaks);
        List<BlockPos> allNodes = new ArrayList<>(detectedNodes.keySet());

        DistanceStats outbreakNearestXz = computeNearestNeighbor(allOutbreaks, allOutbreaks, true, true);
        DistanceStats outbreakNearest3d = computeNearestNeighbor(allOutbreaks, allOutbreaks, true, false);
        DistanceStats hiveNearestXz = computeNearestNeighbor(surfaceHives, surfaceHives, true, true);
        DistanceStats outbreakToHiveNearestXz = computeNearestNeighbor(allOutbreaks, surfaceHives, false, true);
        DistanceStats nodeNearest3d = computeNearestNeighbor(allNodes, allNodes, true, false);

        NetworkStats networkStats = computeNetworkStats(allNodes, MycelialConfig.SYNAPTIC_NODE_LINK_RADIUS.get(), tracker);
        SanctumGridStats sanctumGridStats = auditSanctumJigsawGrid(level, centerChunk, sanctumGridChunks);

        List<String> failures = new ArrayList<>();
        if (chunksScanned > 0
                && detectedNodes.isEmpty()
                && myceliumCount == 0
                && stoneCount == 0
                && floraCount == 0) {
            failures.add("No Mycelial Mind worldgen content found in scanned chunks; density and integrity checks are inconclusive");
        }
        if (detectedNodes.size() != relayNodes + incubatorNodes + sentinelNodes) {
            failures.add("Node type sum mismatch");
        }
        for (HiveIntegrityReport report : hiveIntegrityReports) {
            if (!report.valid()) {
                failures.add("Invalid SurfaceHive at " + report.origin().toShortString());
            }
        }
        if (!sanctumGridStats.structureSetRegistered()) {
            failures.add("StructureSet mycelialmind:hive_sanctum is not registered");
        }
        if (!sanctumGridStats.separationInvariantOk()) {
            failures.add(String.format(
                    Locale.ROOT,
                    "hive_sanctum min chunk distance %.1f < separation %d",
                    sanctumGridStats.chunkNearestNeighbor().min(),
                    sanctumGridStats.separationChunks()
            ));
        }

        long durationMs = (System.nanoTime() - startNs) / 1_000_000L;
        return new WorldgenAuditReport(
                level.dimension().location().toString(),
                level.getSeed(),
                origin,
                radiusChunks,
                chunksScanned,
                durationMs,
                detectedNodes.size(),
                relayNodes,
                incubatorNodes,
                sentinelNodes,
                caveOutbreaks.size(),
                surfaceOutbreaks.size(),
                surfaceHives.size(),
                myceliumCount,
                stoneCount,
                floraCount,
                outbreakNearestXz,
                outbreakNearest3d,
                hiveNearestXz,
                outbreakToHiveNearestXz,
                nodeNearest3d,
                networkStats,
                hiveIntegrityReports,
                sanctumGridStats,
                failures,
                failures.isEmpty()
        );
    }

    public static HiveIntegrityReport inspectSurfaceHive(ServerLevel level, PurificationZoneTracker tracker, BlockPos origin) {
        int stoneCount = 0;
        for (int dy = 0; dy <= 10; dy++) {
            for (int dx = -7; dx <= 7; dx++) {
                for (int dz = -7; dz <= 7; dz++) {
                    BlockState state = level.getBlockState(origin.offset(dx, dy, dz));
                    if (state.is(ModBlocks.MYCELIAL_STONE.get())
                            || state.is(ModBlocks.MYCELIAL_ARCH_BRICK.get())
                            || state.is(ModBlocks.CHITIN_PILLAR.get())) {
                        stoneCount++;
                    }
                }
            }
        }

        int myceliumCount = 0;
        BlockPos ground = origin.below();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (level.getBlockState(ground.offset(dx, 0, dz)).is(ModBlocks.CREEPING_MYCELIUM.get())) {
                    myceliumCount++;
                }
            }
        }

        boolean hasIncubator = level.getBlockState(origin).is(ModBlocks.INCUBATOR_NODE.get());
        boolean hasSentinel = level.getBlockState(origin.above(3)).is(ModBlocks.SENTINEL_NODE.get());

        boolean hasChestLoot = false;
        BlockPos chestPos = origin.east();
        if (level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest) {
            boolean hasBiomass = false;
            boolean hasChitin = false;
            boolean hasSpore = false;
            boolean hasCore = false;
            for (int i = 0; i < chest.getContainerSize(); i++) {
                ItemStack stack = chest.getItem(i);
                if (stack.is(ModItems.MYCELIAL_BIOMASS.get())) {
                    hasBiomass = true;
                } else if (stack.is(ModItems.CHITIN_PLATE.get())) {
                    hasChitin = true;
                } else if (stack.is(ModItems.LATENT_SPORE.get())) {
                    hasSpore = true;
                } else if (stack.is(ModItems.SYNAPTIC_CORE.get())) {
                    hasCore = true;
                }
            }
            hasChestLoot = hasBiomass && hasChitin && hasSpore && hasCore;
        }

        boolean trackerRegistered = tracker.getNodeType(origin) == SynapticNodeType.INCUBATOR
                && tracker.getNodeType(origin.above(3)) == SynapticNodeType.SENTINEL;

        boolean valid = stoneCount >= 12
                && myceliumCount >= 12
                && hasIncubator
                && hasSentinel
                && hasChestLoot
                && trackerRegistered;

        return new HiveIntegrityReport(
                origin,
                stoneCount,
                myceliumCount,
                hasIncubator,
                hasSentinel,
                hasChestLoot,
                trackerRegistered,
                valid
        );
    }

    public static SanctumGridStats auditSanctumJigsawGrid(ServerLevel level, ChunkPos centerChunk, int gridChunks) {
        Optional<Registry<StructureSet>> optReg = level.registryAccess().registry(Registries.STRUCTURE_SET);
        if (optReg.isEmpty()) {
            return new SanctumGridStats(false, gridChunks, 0, 0, "none", 0,
                    DistanceStats.empty(), DistanceStats.empty(), false);
        }
        StructureSet set = optReg.get().get(HIVE_SANCTUM_SET);
        if (set == null || !(set.placement() instanceof RandomSpreadStructurePlacement spread)) {
            return new SanctumGridStats(false, gridChunks, 0, 0, "none", 0,
                    DistanceStats.empty(), DistanceStats.empty(), false);
        }

        int spacing = spread.spacing();
        int separation = spread.separation();
        String spreadType = spread.spreadType().getSerializedName();
        long seed = level.getSeed();

        int half = Math.max(1, gridChunks / 2);
        int minCx = centerChunk.x - half;
        int maxCx = centerChunk.x + half;
        int minCz = centerChunk.z - half;
        int maxCz = centerChunk.z + half;

        int minRegX = Math.floorDiv(minCx, spacing);
        int maxRegX = Math.floorDiv(maxCx, spacing);
        int minRegZ = Math.floorDiv(minCz, spacing);
        int maxRegZ = Math.floorDiv(maxCz, spacing);

        List<BlockPos> chunkCoords = new ArrayList<>();
        List<BlockPos> blockCoords = new ArrayList<>();

        for (int rx = minRegX; rx <= maxRegX; rx++) {
            for (int rz = minRegZ; rz <= maxRegZ; rz++) {
                ChunkPos candidate = spread.getPotentialStructureChunk(seed, rx * spacing, rz * spacing);
                if (candidate.x >= minCx && candidate.x <= maxCx && candidate.z >= minCz && candidate.z <= maxCz) {
                    chunkCoords.add(new BlockPos(candidate.x, 0, candidate.z));
                    blockCoords.add(new BlockPos(candidate.getMiddleBlockX(), 0, candidate.getMiddleBlockZ()));
                }
            }
        }

        DistanceStats chunkNn = computeNearestNeighbor(chunkCoords, chunkCoords, true, true);
        DistanceStats blockNn = computeNearestNeighbor(blockCoords, blockCoords, true, true);
        boolean invariantOk = chunkCoords.size() < 2 || chunkNn.min() + 1e-6 >= separation;

        return new SanctumGridStats(
                true,
                gridChunks,
                spacing,
                separation,
                spreadType,
                chunkCoords.size(),
                chunkNn,
                blockNn,
                invariantOk
        );
    }

    public static DistanceStats computeNearestNeighbor(
            List<BlockPos> sources,
            List<BlockPos> targets,
            boolean excludeSelf,
            boolean horizontalOnly
    ) {
        if (sources.isEmpty() || targets.isEmpty() || (excludeSelf && sources == targets && sources.size() < 2)) {
            return DistanceStats.empty();
        }
        List<Double> distances = new ArrayList<>(sources.size());
        for (int i = 0; i < sources.size(); i++) {
            BlockPos a = sources.get(i);
            double bestSq = Double.MAX_VALUE;
            for (int j = 0; j < targets.size(); j++) {
                if (excludeSelf && i == j && a.equals(targets.get(j))) {
                    continue;
                }
                BlockPos b = targets.get(j);
                double dx = a.getX() - b.getX();
                double dz = a.getZ() - b.getZ();
                double dy = horizontalOnly ? 0.0 : (a.getY() - b.getY());
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq < bestSq) {
                    bestSq = distSq;
                }
            }
            if (bestSq < Double.MAX_VALUE) {
                distances.add(Math.sqrt(bestSq));
            }
        }
        if (distances.isEmpty()) {
            return DistanceStats.empty();
        }
        Collections.sort(distances);
        double sum = 0.0;
        for (double d : distances) {
            sum += d;
        }
        double min = distances.get(0);
        double max = distances.get(distances.size() - 1);
        double mean = sum / distances.size();
        double p50 = percentile(distances, 0.50);
        double p90 = percentile(distances, 0.90);
        return new DistanceStats(distances.size(), min, mean, p50, p90, max);
    }

    public static NetworkStats computeNetworkStats(
            List<BlockPos> nodes,
            int linkRadius,
            PurificationZoneTracker tracker
    ) {
        int n = nodes.size();
        if (n == 0) {
            return new NetworkStats(0, 0, 0, 0, 0, 0.0);
        }
        double maxSq = (double) linkRadius * linkRadius;
        List<List<Integer>> adj = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        int links = 0;
        for (int i = 0; i < n; i++) {
            BlockPos a = nodes.get(i);
            boolean aProtected = tracker != null && tracker.isProtected(a);
            if (aProtected) {
                continue;
            }
            for (int j = i + 1; j < n; j++) {
                BlockPos b = nodes.get(j);
                boolean bProtected = tracker != null && tracker.isProtected(b);
                if (bProtected) {
                    continue;
                }
                if (a.distSqr(b) <= maxSq && (tracker == null || !tracker.isSegmentBlockedByPylon(a, b))) {
                    adj.get(i).add(j);
                    adj.get(j).add(i);
                    links++;
                }
            }
        }

        boolean[] visited = new boolean[n];
        int components = 0;
        int largest = 0;
        int isolated = 0;
        Queue<Integer> queue = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (visited[i]) {
                continue;
            }
            components++;
            int size = 0;
            visited[i] = true;
            queue.add(i);
            while (!queue.isEmpty()) {
                int cur = queue.poll();
                size++;
                for (int nxt : adj.get(cur)) {
                    if (!visited[nxt]) {
                        visited[nxt] = true;
                        queue.add(nxt);
                    }
                }
            }
            if (size == 1) {
                isolated++;
            }
            if (size > largest) {
                largest = size;
            }
        }

        double isolationRatio = (double) isolated / (double) n;
        return new NetworkStats(n, links, components, largest, isolated, isolationRatio);
    }

    public static Optional<Path> writeDiagnosticFiles(WorldgenAuditReport report) {
        try {
            Path cwd = Path.of("").toAbsolutePath().normalize();
            Path repoRoot = resolveRepoRoot(cwd);
            Path diagDir = repoRoot.resolve("artifacts").resolve("diagnostics");
            Files.createDirectories(diagDir);

            String json = GSON.toJson(report.toJson());
            Path latest = diagDir.resolve("worldgen-audit-latest.json");
            Path stamped = diagDir.resolve("worldgen-audit-" + FILE_TS.format(Instant.now()) + ".json");
            Files.writeString(latest, json, StandardCharsets.UTF_8);
            Files.writeString(stamped, json, StandardCharsets.UTF_8);
            return Optional.of(latest);
        } catch (IOException ex) {
            MycelialMind.LOGGER.warn("Failed to write worldgen audit JSON report", ex);
            return Optional.empty();
        }
    }

    private static Path resolveRepoRoot(Path cwd) {
        Path cur = cwd;
        for (int i = 0; i < 6 && cur != null; i++) {
            if (Files.isRegularFile(cur.resolve("build.gradle")) && Files.isDirectory(cur.resolve("src"))) {
                return cur;
            }
            cur = cur.getParent();
        }
        return cwd;
    }

    private static double percentile(List<Double> sorted, double q) {
        if (sorted.size() == 1) {
            return sorted.get(0);
        }
        double idx = q * (sorted.size() - 1);
        int lo = (int) Math.floor(idx);
        int hi = (int) Math.ceil(idx);
        if (lo == hi) {
            return sorted.get(lo);
        }
        double frac = idx - lo;
        return sorted.get(lo) * (1.0 - frac) + sorted.get(hi) * frac;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
