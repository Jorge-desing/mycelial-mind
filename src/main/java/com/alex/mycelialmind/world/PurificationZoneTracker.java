package com.alex.mycelialmind.world;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

/**
 * Tracks active Purification Pylons and typed Synaptic Nodes per dimension,
 * computing synaptic network links and Pylon link-severing geometry.
 */
public class PurificationZoneTracker extends SavedData {
    private static final String DATA_NAME = "mycelialmind_territory_tracker";
    private static final String TAG_PYLONS = "Pylons";
    private static final String TAG_SMOTHERED_PYLONS = "SmotheredPylons";
    private static final String TAG_DEPLETED_PYLONS = "DepletedPylons";
    private static final String TAG_PYLON_TIERS = "PylonTiers";
    private static final String TAG_NODES = "Nodes";
    private static final String TAG_TYPED_NODES = "TypedNodes";
    private static final String TAG_SINKHOLES = "Sinkholes";

    private static final SavedData.Factory<PurificationZoneTracker> FACTORY = new SavedData.Factory<>(
            PurificationZoneTracker::new,
            PurificationZoneTracker::load,
            null
    );

    private final Set<BlockPos> pylonPositions = new HashSet<>();
    private final Set<BlockPos> smotheredPylons = new HashSet<>();
    private final Set<BlockPos> depletedPylons = new HashSet<>();
    private final Map<BlockPos, Integer> pylonTiers = new HashMap<>();
    private final Map<BlockPos, Integer> pylonBonusRadii = new HashMap<>();
    private final Set<BlockPos> nodePositions = new HashSet<>();
    private final Map<BlockPos, SynapticNodeType> typedNodes = new HashMap<>();
    private final Set<BlockPos> sinkholePositions = new HashSet<>();

    private final Cache<Long, Boolean> protectionCache = Caffeine.newBuilder()
            .maximumSize(4096)
            .recordStats()
            .build();
    private final Cache<Long, Optional<BlockPos>> nearestNodeCache = Caffeine.newBuilder()
            .maximumSize(1024)
            .recordStats()
            .build();
    private final MycelialSpatialBitset pylonChunkIndex = new MycelialSpatialBitset();
    private final MycelialSpatialBitset nodeChunkIndex = new MycelialSpatialBitset();
    @Nullable
    private SynapticNetworkTopology.TopologySummary cachedTopologySummary;
    private int cachedBasePylonRadius = -1;

    private synchronized void invalidateSpatialCaches() {
        protectionCache.invalidateAll();
        nearestNodeCache.invalidateAll();
        cachedTopologySummary = null;
    }

    private static int getNodeLinkRadiusSafe() {
        try {
            return MycelialConfig.SPEC.isLoaded()
                    ? MycelialConfig.SYNAPTIC_NODE_LINK_RADIUS.get()
                    : MycelialConfig.SYNAPTIC_NODE_LINK_RADIUS.getDefault();
        } catch (RuntimeException ignored) {
            return 28;
        }
    }

    private static int getBasePylonRadiusSafe() {
        try {
            return MycelialConfig.SPEC.isLoaded()
                    ? MycelialConfig.PYLON_PURIFICATION_RADIUS.get()
                    : MycelialConfig.PYLON_PURIFICATION_RADIUS.getDefault();
        } catch (RuntimeException ignored) {
            return 24;
        }
    }

    private synchronized void ensureRadiusCacheConsistency() {
        int currentBase = getBasePylonRadiusSafe();
        if (cachedBasePylonRadius != currentBase) {
            cachedBasePylonRadius = currentBase;
            protectionCache.invalidateAll();
        }
    }

    public CacheStats getProtectionCacheStats() {
        return protectionCache.stats();
    }

    public CacheStats getNearestNodeCacheStats() {
        return nearestNodeCache.stats();
    }

    public static PurificationZoneTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public synchronized void addPylon(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        if (pylonPositions.add(immutable)) {
            pylonChunkIndex.addBlockChunk(immutable);
            invalidateSpatialCaches();
            setDirty();
        }
    }

    public synchronized void removePylon(BlockPos pos) {
        boolean removed = pylonPositions.remove(pos);
        boolean unsmothered = smotheredPylons.remove(pos);
        boolean undepleted = depletedPylons.remove(pos);
        boolean removedTier = pylonTiers.remove(pos) != null;
        boolean removedBonus = pylonBonusRadii.remove(pos) != null;
        if (removed || unsmothered || undepleted || removedTier || removedBonus) {
            if (removed) {
                pylonChunkIndex.rebuildFromBlocks(pylonPositions);
            }
            invalidateSpatialCaches();
            setDirty();
        }
    }

    public synchronized void clearAll() {
        pylonPositions.clear();
        smotheredPylons.clear();
        depletedPylons.clear();
        pylonTiers.clear();
        pylonBonusRadii.clear();
        nodePositions.clear();
        typedNodes.clear();
        sinkholePositions.clear();
        pylonChunkIndex.clear();
        nodeChunkIndex.clear();
        invalidateSpatialCaches();
        setDirty();
    }

    public synchronized void restoreFromSnapshot(CompoundTag snapshot, HolderLookup.Provider registries) {
        PurificationZoneTracker restored = load(snapshot, registries);
        pylonPositions.clear();
        pylonPositions.addAll(restored.pylonPositions);
        smotheredPylons.clear();
        smotheredPylons.addAll(restored.smotheredPylons);
        depletedPylons.clear();
        depletedPylons.addAll(restored.depletedPylons);
        pylonTiers.clear();
        pylonTiers.putAll(restored.pylonTiers);
        pylonBonusRadii.clear();
        pylonBonusRadii.putAll(restored.pylonBonusRadii);
        nodePositions.clear();
        nodePositions.addAll(restored.nodePositions);
        typedNodes.clear();
        typedNodes.putAll(restored.typedNodes);
        sinkholePositions.clear();
        sinkholePositions.addAll(restored.sinkholePositions);
        pylonChunkIndex.rebuildFromBlocks(pylonPositions);
        nodeChunkIndex.rebuildFromBlocks(nodePositions);
        invalidateSpatialCaches();
        setDirty();
    }

    public synchronized void setPylonTier(BlockPos pos, int tier) {
        BlockPos immutable = pos.immutable();
        int clamped = Math.max(1, Math.min(3, tier));
        if (!pylonPositions.contains(immutable) || clamped <= 1) {
            if (pylonTiers.remove(immutable) != null) {
                invalidateSpatialCaches();
                setDirty();
            }
            return;
        }
        Integer prev = pylonTiers.put(immutable, clamped);
        if (prev == null || prev != clamped) {
            invalidateSpatialCaches();
            setDirty();
        }
    }

    public synchronized int getPylonTier(BlockPos pos) {
        return pylonTiers.getOrDefault(pos, 1);
    }

    public synchronized void setPylonBonusRadius(BlockPos pos, int bonus) {
        BlockPos immutable = pos.immutable();
        int clamped = Math.max(0, Math.min(12, bonus));
        if (!pylonPositions.contains(immutable) || clamped == 0) {
            if (pylonBonusRadii.remove(immutable) != null) {
                invalidateSpatialCaches();
                setDirty();
            }
            return;
        }
        Integer prev = pylonBonusRadii.put(immutable, clamped);
        if (prev == null || prev != clamped) {
            invalidateSpatialCaches();
            setDirty();
        }
    }

    public synchronized int getPylonRadius(BlockPos pos) {
        int base = getBasePylonRadiusSafe();
        int tier = pylonTiers.getOrDefault(pos, 1);
        int tierRadius = tier >= 3 ? base + 12 : (tier == 2 ? base + 6 : base);
        return tierRadius + pylonBonusRadii.getOrDefault(pos, 0);
    }

    public synchronized void setPylonSmothered(BlockPos pos, boolean smothered) {
        BlockPos immutable = pos.immutable();
        if (!pylonPositions.contains(immutable)) {
            if (smotheredPylons.remove(immutable)) {
                invalidateSpatialCaches();
                setDirty();
            }
            return;
        }
        boolean changed = smothered ? smotheredPylons.add(immutable) : smotheredPylons.remove(immutable);
        if (changed) {
            invalidateSpatialCaches();
            setDirty();
        }
    }

    public synchronized boolean isPylonSmothered(BlockPos pos) {
        return smotheredPylons.contains(pos);
    }

    public synchronized void setPylonDepleted(BlockPos pos, boolean depleted) {
        BlockPos immutable = pos.immutable();
        if (!pylonPositions.contains(immutable)) {
            if (depletedPylons.remove(immutable)) {
                invalidateSpatialCaches();
                setDirty();
            }
            return;
        }
        boolean changed = depleted ? depletedPylons.add(immutable) : depletedPylons.remove(immutable);
        if (changed) {
            invalidateSpatialCaches();
            setDirty();
        }
    }

    public synchronized boolean isPylonDepleted(BlockPos pos) {
        return depletedPylons.contains(pos);
    }

    public synchronized int countSmotheredPylons() {
        return smotheredPylons.size();
    }

    public synchronized int countActivePylonsWithin(BlockPos center, int radius) {
        double maxSq = (double) radius * radius;
        int count = 0;
        for (BlockPos pylon : pylonPositions) {
            if (!smotheredPylons.contains(pylon) && !depletedPylons.contains(pylon) && pylon.distSqr(center) <= maxSq) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getMaxActivePylonTierWithin(BlockPos center, int baseRadius) {
        int maxTier = 0;
        for (BlockPos pylon : pylonPositions) {
            if (smotheredPylons.contains(pylon) || depletedPylons.contains(pylon)) {
                continue;
            }
            int effRadius = Math.max(baseRadius, getPylonRadius(pylon));
            if (pylon.distSqr(center) <= (double) effRadius * effRadius) {
                maxTier = Math.max(maxTier, getPylonTier(pylon));
            }
        }
        return maxTier;
    }

    public synchronized List<BlockPos> getPylonsInChunk(int chunkX, int chunkZ) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos pos : pylonPositions) {
            if ((pos.getX() >> 4) == chunkX && (pos.getZ() >> 4) == chunkZ) {
                out.add(pos);
            }
        }
        return out;
    }

    public synchronized void addNode(BlockPos pos) {
        addNode(pos, SynapticNodeType.RELAY);
    }

    public synchronized void addNode(BlockPos pos, SynapticNodeType type) {
        BlockPos immutable = pos.immutable();
        boolean added = nodePositions.add(immutable);
        SynapticNodeType prev = typedNodes.put(immutable, type);
        if (added || prev != type) {
            nodeChunkIndex.addBlockChunk(immutable);
            invalidateSpatialCaches();
            setDirty();
        }
    }

    public synchronized void removeNode(BlockPos pos) {
        boolean removed = nodePositions.remove(pos);
        boolean removedTyped = typedNodes.remove(pos) != null;
        if (removed || removedTyped) {
            if (removed) {
                nodeChunkIndex.rebuildFromBlocks(nodePositions);
            }
            invalidateSpatialCaches();
            setDirty();
        }
    }

    public synchronized boolean hasPylonInChunk(int chunkX, int chunkZ) {
        return pylonChunkIndex.containsChunk(chunkX, chunkZ);
    }

    public synchronized boolean hasNodeInChunk(int chunkX, int chunkZ) {
        return nodeChunkIndex.containsChunk(chunkX, chunkZ);
    }

    public synchronized long countOccupiedPylonChunks() {
        return pylonChunkIndex.cardinality();
    }

    public synchronized long countOccupiedNodeChunks() {
        return nodeChunkIndex.cardinality();
    }

    public synchronized long countContestedChunks() {
        return pylonChunkIndex.intersectionCardinality(nodeChunkIndex);
    }

    private SimpleWeightedGraph<BlockPos, DefaultWeightedEdge> buildActiveEnemyNetworkGraph() {
        int maxRadius = getNodeLinkRadiusSafe();
        List<BlockPos> activeNodes = new ArrayList<>();
        for (BlockPos pos : nodePositions) {
            if (getNodeType(pos) != SynapticNodeType.SYMBIOTIC && !isProtected(pos)) {
                activeNodes.add(pos);
            }
        }
        return SynapticNetworkTopology.buildGraph(
                activeNodes,
                (a, b) -> isWithinLinkRange(a, b, maxRadius) && !isSegmentBlockedByPylon(a, b)
        );
    }

    public synchronized SynapticNetworkTopology.TopologySummary getNetworkTopologySummary() {
        ensureRadiusCacheConsistency();
        if (cachedTopologySummary == null) {
            cachedTopologySummary = SynapticNetworkTopology.analyze(buildActiveEnemyNetworkGraph());
        }
        return cachedTopologySummary;
    }

    public synchronized int getNetworkClusterCount() {
        return getNetworkTopologySummary().clusterCount();
    }

    public synchronized Set<BlockPos> getCriticalSynapticNodes() {
        return getNetworkTopologySummary().criticalNodes();
    }

    public synchronized List<BlockPos> findShortestSynapticPath(BlockPos from, BlockPos to) {
        if (from == null || to == null) {
            return List.of();
        }
        return SynapticNetworkTopology.findShortestPath(buildActiveEnemyNetworkGraph(), from.immutable(), to.immutable());
    }

    public synchronized SynapticNodeType getNodeType(BlockPos pos) {
        return typedNodes.getOrDefault(pos, SynapticNodeType.RELAY);
    }

    public synchronized boolean isProtected(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        ensureRadiusCacheConsistency();
        return Boolean.TRUE.equals(protectionCache.get(pos.asLong(), k -> computeIsProtected(pos)));
    }

    private boolean computeIsProtected(BlockPos pos) {
        for (BlockPos pylon : pylonPositions) {
            if (!smotheredPylons.contains(pylon) && !depletedPylons.contains(pylon)) {
                int radius = getPylonRadius(pylon);
                if (pylon.distSqr(pos) <= (double) radius * radius) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isSegmentBlockedByPylon(BlockPos a, BlockPos b) {
        double ax = a.getX() + 0.5;
        double ay = a.getY() + 0.5;
        double az = a.getZ() + 0.5;
        double bx = b.getX() + 0.5;
        double by = b.getY() + 0.5;
        double bz = b.getZ() + 0.5;
        double vx = bx - ax;
        double vy = by - ay;
        double vz = bz - az;
        double lenSq = vx * vx + vy * vy + vz * vz;

        for (BlockPos pylon : pylonPositions) {
            if (smotheredPylons.contains(pylon) || depletedPylons.contains(pylon)) {
                continue;
            }
            int radius = getPylonRadius(pylon);
            double radiusSq = (double) radius * radius;
            double px = pylon.getX() + 0.5;
            double py = pylon.getY() + 0.5;
            double pz = pylon.getZ() + 0.5;
            double t = lenSq <= 1e-6 ? 0.0 : ((px - ax) * vx + (py - ay) * vy + (pz - az) * vz) / lenSq;
            t = Math.max(0.0, Math.min(1.0, t));
            double cx = ax + t * vx;
            double cy = ay + t * vy;
            double cz = az + t * vz;
            double dx = px - cx;
            double dy = py - cy;
            double dz = pz - cz;
            if ((dx * dx + dy * dy + dz * dz) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    public static final int VERTICAL_TAPROOT_MAX_HORIZ = 12;
    public static final int VERTICAL_TAPROOT_MIN_DY = 8;
    public static final int VERTICAL_TAPROOT_MAX_DY = 80;

    /**
     * Returns true when two synaptic nodes are within standard 3D link radius, OR
     * vertically aligned along a subterranean-to-surface hyphal taproot shaft.
     */
    public static boolean isWithinLinkRange(BlockPos a, BlockPos b, int maxRadius) {
        double maxSq = (double) maxRadius * maxRadius;
        if (a.distSqr(b) <= maxSq) {
            return true;
        }
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        int absDy = Math.abs(a.getY() - b.getY());
        return absDy >= VERTICAL_TAPROOT_MIN_DY
                && absDy <= VERTICAL_TAPROOT_MAX_DY
                && (dx * dx + dz * dz) <= VERTICAL_TAPROOT_MAX_HORIZ * VERTICAL_TAPROOT_MAX_HORIZ;
    }

    public List<BlockPos> getLinkedNodes(BlockPos origin) {
        return getLinkedNodes(origin, MycelialConfig.SYNAPTIC_NODE_LINK_RADIUS.get());
    }

    public synchronized List<BlockPos> getLinkedNodes(BlockPos origin, int maxRadius) {
        List<BlockPos> linked = new ArrayList<>();
        if (isProtected(origin) || getNodeType(origin) == SynapticNodeType.SYMBIOTIC) {
            return linked;
        }
        for (BlockPos candidate : nodePositions) {
            if (candidate.equals(origin) || getNodeType(candidate) == SynapticNodeType.SYMBIOTIC) {
                continue;
            }
            if (isWithinLinkRange(origin, candidate, maxRadius) && !isSegmentBlockedByPylon(origin, candidate)) {
                linked.add(candidate);
            }
        }
        return linked;
    }

    public boolean hasNearbyUnprotectedNode(BlockPos origin, SynapticNodeType requiredType, int radius) {
        double maxSq = (double) radius * radius;
        for (BlockPos node : nodePositions) {
            if (getNodeType(node) == requiredType
                    && node.distSqr(origin) <= maxSq
                    && !isProtected(node)) {
                return true;
            }
        }
        return false;
    }

    public synchronized int countActiveNetworkLinks() {
        int maxRadius = MycelialConfig.SYNAPTIC_NODE_LINK_RADIUS.get();
        List<BlockPos> list = new ArrayList<>(nodePositions);
        int links = 0;
        for (int i = 0; i < list.size(); i++) {
            BlockPos a = list.get(i);
            if (getNodeType(a) == SynapticNodeType.SYMBIOTIC || isProtected(a)) {
                continue;
            }
            for (int j = i + 1; j < list.size(); j++) {
                BlockPos b = list.get(j);
                if (getNodeType(b) != SynapticNodeType.SYMBIOTIC
                        && !isProtected(b)
                        && isWithinLinkRange(a, b, maxRadius)
                        && !isSegmentBlockedByPylon(a, b)) {
                    links++;
                }
            }
        }
        return links;
    }

    public void alertNearbyNodes(ServerLevel level, BlockPos origin, int alertTicks) {
        if (level.getBlockEntity(origin) instanceof SynapticNodeBlockEntity originBe) {
            originBe.triggerAlert(alertTicks);
        }
        for (BlockPos firstHop : getLinkedNodes(origin)) {
            if (level.getBlockEntity(firstHop) instanceof SynapticNodeBlockEntity hopBe) {
                hopBe.triggerAlert(alertTicks);
            }
        }
    }

    @Nullable
    public synchronized BlockPos findNearestNode(BlockPos origin) {
        return findNearestNode(origin, null);
    }

    @Nullable
    public synchronized BlockPos findNearestNode(BlockPos origin, @Nullable SynapticNodeType filterType) {
        if (origin == null) {
            return null;
        }
        if (filterType == null) {
            return nearestNodeCache.get(origin.asLong(), k -> Optional.ofNullable(computeNearestNode(origin, null)))
                    .orElse(null);
        }
        return computeNearestNode(origin, filterType);
    }

    @Nullable
    private BlockPos computeNearestNode(BlockPos origin, @Nullable SynapticNodeType filterType) {
        BlockPos nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlockPos node : nodePositions) {
            if (filterType != null && getNodeType(node) != filterType) {
                continue;
            }
            double distSq = node.distSqr(origin);
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                nearest = node;
            }
        }
        return nearest;
    }

    public synchronized int countSymbioticNodes() {
        int count = 0;
        for (BlockPos pos : nodePositions) {
            if (getNodeType(pos) == SynapticNodeType.SYMBIOTIC) {
                count++;
            }
        }
        return count;
    }

    public synchronized List<BlockPos> getLinkedSymbioticNodes(BlockPos origin, int maxRange) {
        List<BlockPos> out = new ArrayList<>();
        double maxSq = (double) maxRange * maxRange;
        for (BlockPos pos : nodePositions) {
            if (!pos.equals(origin)
                    && getNodeType(pos) == SynapticNodeType.SYMBIOTIC
                    && pos.distSqr(origin) <= maxSq
                    && !isSegmentBlockedByPylon(origin, pos)) {
                out.add(pos);
            }
        }
        out.sort((a, b) -> Double.compare(a.distSqr(origin), b.distSqr(origin)));
        return out;
    }

    public synchronized List<BlockPos> getNearbySymbioticOutposts(BlockPos origin, int maxRange) {
        List<BlockPos> out = new ArrayList<>();
        double maxSq = (double) maxRange * maxRange;
        for (BlockPos pos : nodePositions) {
            if (!pos.equals(origin)
                    && getNodeType(pos) == SynapticNodeType.SYMBIOTIC
                    && pos.distSqr(origin) <= maxSq) {
                out.add(pos);
            }
        }
        out.sort((a, b) -> Double.compare(a.distSqr(origin), b.distSqr(origin)));
        return out;
    }

    public synchronized boolean hasNearbySymbioticNode(BlockPos origin, int radius) {
        double maxSq = (double) radius * radius;
        for (BlockPos pos : nodePositions) {
            if (getNodeType(pos) == SynapticNodeType.SYMBIOTIC && pos.distSqr(origin) <= maxSq) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public synchronized BlockPos findNearestEnemyNode(BlockPos origin) {
        return findNearestEnemyNode(origin, 4096);
    }

    @Nullable
    public synchronized BlockPos findNearestEnemyNode(BlockPos origin, int maxRadius) {
        return findNearestEnemyNode(origin, maxRadius, node -> true);
    }

    @Nullable
    public synchronized BlockPos findNearestEnemyNode(BlockPos origin, int maxRadius,
                                                       Predicate<BlockPos> eligible) {
        BlockPos nearest = null;
        double bestDistSq = (double) maxRadius * maxRadius;
        for (BlockPos node : nodePositions) {
            if (getNodeType(node) == SynapticNodeType.SYMBIOTIC || isProtected(node)) {
                continue;
            }
            double distSq = node.distSqr(origin);
            if (distSq <= bestDistSq && eligible.test(node)) {
                bestDistSq = distSq;
                nearest = node;
            }
        }
        return nearest;
    }

    public synchronized int countNodes() {
        return nodePositions.size();
    }

    public int countPylons() {
        return pylonPositions.size();
    }

    public synchronized boolean hasAnyNodeWithin(BlockPos origin, int radius) {
        double maxSq = (double) radius * radius;
        for (BlockPos node : nodePositions) {
            if (node.distSqr(origin) <= maxSq) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean tryReserveNodePosition(BlockPos pos, int minNodeSpacing, SynapticNodeType type) {
        if (hasAnyNodeWithin(pos, minNodeSpacing)) {
            return false;
        }
        addNode(pos, type);
        return true;
    }

    public synchronized boolean tryReserveSurfaceHive(BlockPos origin, int minHiveSpacing, int minNodeClearance) {
        return tryReserveSurfaceHive(origin, minHiveSpacing, minNodeClearance, null);
    }

    public synchronized boolean tryReserveSurfaceHive(BlockPos origin, int minHiveSpacing, int minNodeClearance,
                                                      @Nullable BlockPos sourceNode) {
        if (!validOutbreakSource(sourceNode) || hasSurfaceHiveWithin(origin, minHiveSpacing)
                || hasOtherNodeWithin(origin, minNodeClearance, sourceNode)) {
            return false;
        }
        addNode(origin, SynapticNodeType.INCUBATOR);
        addNode(origin.above(3), SynapticNodeType.SENTINEL);
        return true;
    }

    public synchronized boolean hasSurfaceHiveWithin(BlockPos origin, int radius) {
        double maxSq = (double) radius * radius;
        for (BlockPos node : nodePositions) {
            if (getNodeType(node) == SynapticNodeType.INCUBATOR
                    && getNodeType(node.above(3)) == SynapticNodeType.SENTINEL
                    && node.distSqr(origin) <= maxSq) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean hasSinkholeWithin(BlockPos origin, int radius) {
        double maxSq = (double) radius * radius;
        for (BlockPos sinkhole : sinkholePositions) {
            if (sinkhole.distSqr(origin) <= maxSq) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean tryReserveSinkhole(BlockPos origin, int minSinkholeSpacing, int minNodeClearance) {
        return tryReserveSinkhole(origin, minSinkholeSpacing, minNodeClearance, null);
    }

    public synchronized boolean tryReserveSinkhole(BlockPos origin, int minSinkholeSpacing, int minNodeClearance,
                                                   @Nullable BlockPos sourceNode) {
        if (!validOutbreakSource(sourceNode) || hasSinkholeWithin(origin, minSinkholeSpacing)
                || hasSurfaceHiveWithin(origin, Math.max(minNodeClearance, minSinkholeSpacing / 2))
                || hasOtherNodeWithin(origin, minNodeClearance, sourceNode)) {
            return false;
        }
        if (sinkholePositions.add(origin.immutable())) {
            setDirty();
        }
        return true;
    }

    private boolean validOutbreakSource(@Nullable BlockPos sourceNode) {
        return sourceNode == null || (nodePositions.contains(sourceNode)
                && getNodeType(sourceNode) != SynapticNodeType.SYMBIOTIC);
    }

    private boolean hasOtherNodeWithin(BlockPos origin, int radius, @Nullable BlockPos excluded) {
        double maxSq = (double) radius * radius;
        for (BlockPos node : nodePositions) {
            if (!node.equals(excluded) && node.distSqr(origin) <= maxSq) {
                return true;
            }
        }
        return false;
    }

    public synchronized void removeSinkhole(BlockPos origin) {
        if (sinkholePositions.remove(origin)) {
            setDirty();
        }
    }

    public List<Map.Entry<BlockPos, SynapticNodeType>> getNodesWithin(BlockPos center, int radius, int limit) {
        double maxSq = (double) radius * radius;
        List<Map.Entry<BlockPos, SynapticNodeType>> out = new ArrayList<>();
        for (BlockPos pos : nodePositions) {
            if (pos.distSqr(center) <= maxSq) {
                out.add(Map.entry(pos, getNodeType(pos)));
            }
        }
        out.sort((a, b) -> Double.compare(a.getKey().distSqr(center), b.getKey().distSqr(center)));
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    public List<BlockPos> getPylonsWithin(BlockPos center, int radius, int limit) {
        double maxSq = (double) radius * radius;
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos pos : pylonPositions) {
            if (pos.distSqr(center) <= maxSq) {
                out.add(pos);
            }
        }
        out.sort((a, b) -> Double.compare(a.distSqr(center), b.distSqr(center)));
        return out.size() > limit ? out.subList(0, limit) : out;
    }

    public SynapticNodeType getRecommendedPromotionType() {
        int incubators = 0;
        int sentinels = 0;
        for (BlockPos pos : nodePositions) {
            SynapticNodeType type = getNodeType(pos);
            if (type == SynapticNodeType.INCUBATOR) {
                incubators++;
            } else if (type == SynapticNodeType.SENTINEL) {
                sentinels++;
            }
        }
        return incubators <= sentinels ? SynapticNodeType.INCUBATOR : SynapticNodeType.SENTINEL;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag pylons = new ListTag();
        for (BlockPos pos : pylonPositions) {
            pylons.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(TAG_PYLONS, pylons);

        ListTag smothered = new ListTag();
        for (BlockPos pos : smotheredPylons) {
            smothered.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(TAG_SMOTHERED_PYLONS, smothered);

        ListTag depleted = new ListTag();
        for (BlockPos pos : depletedPylons) {
            depleted.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(TAG_DEPLETED_PYLONS, depleted);

        ListTag tiers = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : pylonTiers.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("Pos", entry.getKey().asLong());
            entryTag.putInt("Tier", entry.getValue());
            tiers.add(entryTag);
        }
        tag.put(TAG_PYLON_TIERS, tiers);

        ListTag nodes = new ListTag();
        for (BlockPos pos : nodePositions) {
            nodes.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(TAG_NODES, nodes);

        ListTag typed = new ListTag();
        for (Map.Entry<BlockPos, SynapticNodeType> entry : typedNodes.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("Pos", entry.getKey().asLong());
            entryTag.putString("Type", entry.getValue().id());
            typed.add(entryTag);
        }
        tag.put(TAG_TYPED_NODES, typed);

        ListTag radiusBonuses = new ListTag();
        for (Map.Entry<BlockPos, Integer> entry : pylonBonusRadii.entrySet()) {
            CompoundTag entryTag = new CompoundTag();
            entryTag.putLong("Pos", entry.getKey().asLong());
            entryTag.putInt("Bonus", entry.getValue());
            radiusBonuses.add(entryTag);
        }
        tag.put("PylonRadiusBonuses", radiusBonuses);

        ListTag sinkholes = new ListTag();
        for (BlockPos pos : sinkholePositions) {
            sinkholes.add(LongTag.valueOf(pos.asLong()));
        }
        tag.put(TAG_SINKHOLES, sinkholes);
        return tag;
    }

    public static PurificationZoneTracker load(CompoundTag tag, HolderLookup.Provider registries) {
        PurificationZoneTracker tracker = new PurificationZoneTracker();
        ListTag pylons = tag.getList(TAG_PYLONS, Tag.TAG_LONG);
        for (int i = 0; i < pylons.size(); i++) {
            tracker.pylonPositions.add(BlockPos.of(((LongTag) pylons.get(i)).getAsLong()));
        }
        ListTag smothered = tag.getList(TAG_SMOTHERED_PYLONS, Tag.TAG_LONG);
        for (int i = 0; i < smothered.size(); i++) {
            tracker.smotheredPylons.add(BlockPos.of(((LongTag) smothered.get(i)).getAsLong()));
        }
        ListTag depleted = tag.getList(TAG_DEPLETED_PYLONS, Tag.TAG_LONG);
        for (int i = 0; i < depleted.size(); i++) {
            tracker.depletedPylons.add(BlockPos.of(((LongTag) depleted.get(i)).getAsLong()));
        }
        ListTag tiers = tag.getList(TAG_PYLON_TIERS, Tag.TAG_COMPOUND);
        for (int i = 0; i < tiers.size(); i++) {
            CompoundTag entry = tiers.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            int t = Math.max(1, Math.min(3, entry.getInt("Tier")));
            if (t > 1) {
                tracker.pylonTiers.put(pos, t);
            }
        }
        ListTag nodes = tag.getList(TAG_NODES, Tag.TAG_LONG);
        for (int i = 0; i < nodes.size(); i++) {
            BlockPos pos = BlockPos.of(((LongTag) nodes.get(i)).getAsLong());
            tracker.nodePositions.add(pos);
            tracker.typedNodes.put(pos, SynapticNodeType.RELAY);
        }
        ListTag typed = tag.getList(TAG_TYPED_NODES, Tag.TAG_COMPOUND);
        for (int i = 0; i < typed.size(); i++) {
            CompoundTag entry = typed.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            tracker.nodePositions.add(pos);
            tracker.typedNodes.put(pos, SynapticNodeType.fromId(entry.getString("Type")));
        }
        ListTag radiusBonuses = tag.getList("PylonRadiusBonuses", Tag.TAG_COMPOUND);
        for (int i = 0; i < radiusBonuses.size(); i++) {
            CompoundTag entry = radiusBonuses.getCompound(i);
            BlockPos pos = BlockPos.of(entry.getLong("Pos"));
            int bonus = Math.max(0, Math.min(12, entry.getInt("Bonus")));
            if (bonus > 0) {
                tracker.pylonBonusRadii.put(pos, bonus);
            }
        }
        ListTag sinkholes = tag.getList(TAG_SINKHOLES, Tag.TAG_LONG);
        for (int i = 0; i < sinkholes.size(); i++) {
            tracker.sinkholePositions.add(BlockPos.of(((LongTag) sinkholes.get(i)).getAsLong()));
        }
        tracker.pylonChunkIndex.rebuildFromBlocks(tracker.pylonPositions);
        tracker.nodeChunkIndex.rebuildFromBlocks(tracker.nodePositions);
        return tracker;
    }
}
