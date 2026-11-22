package com.alex.mycelialmind.world;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.config.MycelialConfig;
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
import java.util.Set;
import java.util.function.Predicate;

/**
 * Tracks active Purification Pylons and typed Synaptic Nodes per dimension,
 * computing synaptic network links and Pylon link-severing geometry.
 */
public class PurificationZoneTracker extends SavedData {
    private static final String DATA_NAME = "mycelialmind_territory_tracker";
    private static final String TAG_PYLONS = "Pylons";
    private static final String TAG_SMOTHERED_PYLONS = "SmotheredPylons";
    private static final String TAG_PYLON_TIERS = "PylonTiers";
    private static final String TAG_NODES = "Nodes";
    private static final String TAG_TYPED_NODES = "TypedNodes";

    private static final SavedData.Factory<PurificationZoneTracker> FACTORY = new SavedData.Factory<>(
            PurificationZoneTracker::new,
            PurificationZoneTracker::load,
            null
    );

    private final Set<BlockPos> pylonPositions = new HashSet<>();
    private final Set<BlockPos> smotheredPylons = new HashSet<>();
    private final Map<BlockPos, Integer> pylonTiers = new HashMap<>();
    private final Set<BlockPos> nodePositions = new HashSet<>();
    private final Map<BlockPos, SynapticNodeType> typedNodes = new HashMap<>();

    public static PurificationZoneTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public synchronized void addPylon(BlockPos pos) {
        if (pylonPositions.add(pos.immutable())) {
            setDirty();
        }
    }

    public synchronized void removePylon(BlockPos pos) {
        boolean removed = pylonPositions.remove(pos);
        boolean unsmothered = smotheredPylons.remove(pos);
        boolean removedTier = pylonTiers.remove(pos) != null;
        if (removed || unsmothered || removedTier) {
            setDirty();
        }
    }

    public synchronized void clearAll() {
        pylonPositions.clear();
        smotheredPylons.clear();
        pylonTiers.clear();
        nodePositions.clear();
        typedNodes.clear();
        setDirty();
    }

    public synchronized void restoreFromSnapshot(CompoundTag snapshot, HolderLookup.Provider registries) {
        PurificationZoneTracker restored = load(snapshot, registries);
        pylonPositions.clear();
        pylonPositions.addAll(restored.pylonPositions);
        smotheredPylons.clear();
        smotheredPylons.addAll(restored.smotheredPylons);
        pylonTiers.clear();
        pylonTiers.putAll(restored.pylonTiers);
        nodePositions.clear();
        nodePositions.addAll(restored.nodePositions);
        typedNodes.clear();
        typedNodes.putAll(restored.typedNodes);
        setDirty();
    }

    public synchronized void setPylonTier(BlockPos pos, int tier) {
        BlockPos immutable = pos.immutable();
        int clamped = Math.max(1, Math.min(3, tier));
        if (!pylonPositions.contains(immutable) || clamped <= 1) {
            if (pylonTiers.remove(immutable) != null) {
                setDirty();
            }
            return;
        }
        Integer prev = pylonTiers.put(immutable, clamped);
        if (prev == null || prev != clamped) {
            setDirty();
        }
    }

    public synchronized int getPylonTier(BlockPos pos) {
        return pylonTiers.getOrDefault(pos, 1);
    }

    public synchronized int getPylonRadius(BlockPos pos) {
        int base = MycelialConfig.PYLON_PURIFICATION_RADIUS.get();
        int tier = pylonTiers.getOrDefault(pos, 1);
        return tier >= 3 ? base + 12 : (tier == 2 ? base + 6 : base);
    }

    public synchronized void setPylonSmothered(BlockPos pos, boolean smothered) {
        BlockPos immutable = pos.immutable();
        if (!pylonPositions.contains(immutable)) {
            if (smotheredPylons.remove(immutable)) {
                setDirty();
            }
            return;
        }
        boolean changed = smothered ? smotheredPylons.add(immutable) : smotheredPylons.remove(immutable);
        if (changed) {
            setDirty();
        }
    }

    public synchronized boolean isPylonSmothered(BlockPos pos) {
        return smotheredPylons.contains(pos);
    }

    public synchronized int countSmotheredPylons() {
        return smotheredPylons.size();
    }

    public synchronized int countActivePylonsWithin(BlockPos center, int radius) {
        double maxSq = (double) radius * radius;
        int count = 0;
        for (BlockPos pylon : pylonPositions) {
            if (!smotheredPylons.contains(pylon) && pylon.distSqr(center) <= maxSq) {
                count++;
            }
        }
        return count;
    }

    public synchronized int getMaxActivePylonTierWithin(BlockPos center, int baseRadius) {
        int maxTier = 0;
        for (BlockPos pylon : pylonPositions) {
            if (smotheredPylons.contains(pylon)) {
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
            setDirty();
        }
    }

    public synchronized void removeNode(BlockPos pos) {
        boolean removed = nodePositions.remove(pos);
        boolean removedTyped = typedNodes.remove(pos) != null;
        if (removed || removedTyped) {
            setDirty();
        }
    }

    public synchronized SynapticNodeType getNodeType(BlockPos pos) {
        return typedNodes.getOrDefault(pos, SynapticNodeType.RELAY);
    }

    public synchronized boolean isProtected(BlockPos pos) {
        for (BlockPos pylon : pylonPositions) {
            if (!smotheredPylons.contains(pylon)) {
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
            if (smotheredPylons.contains(pylon)) {
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

    public List<BlockPos> getLinkedNodes(BlockPos origin) {
        return getLinkedNodes(origin, MycelialConfig.SYNAPTIC_NODE_LINK_RADIUS.get());
    }

    public synchronized List<BlockPos> getLinkedNodes(BlockPos origin, int maxRadius) {
        List<BlockPos> linked = new ArrayList<>();
        if (isProtected(origin) || getNodeType(origin) == SynapticNodeType.SYMBIOTIC) {
            return linked;
        }
        double maxSq = (double) maxRadius * maxRadius;
        for (BlockPos candidate : nodePositions) {
            if (candidate.equals(origin) || getNodeType(candidate) == SynapticNodeType.SYMBIOTIC) {
                continue;
            }
            if (origin.distSqr(candidate) <= maxSq && !isSegmentBlockedByPylon(origin, candidate)) {
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
        double maxSq = (double) maxRadius * maxRadius;
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
                        && a.distSqr(b) <= maxSq
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
    public BlockPos findNearestNode(BlockPos origin) {
        return findNearestNode(origin, null);
    }

    @Nullable
    public BlockPos findNearestNode(BlockPos origin, @Nullable SynapticNodeType filterType) {
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
        if (hasSurfaceHiveWithin(origin, minHiveSpacing) || hasAnyNodeWithin(origin, minNodeClearance)) {
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
        return tracker;
    }
}
