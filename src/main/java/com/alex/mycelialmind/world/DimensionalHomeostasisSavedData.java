package com.alex.mycelialmind.world;

import com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Persists per-chunk dimensional immune stress, active antibody cyst counts,
 * and player-placed foreign blocks in deep_mycelium.
 */
public class DimensionalHomeostasisSavedData extends SavedData {
    private static final String DATA_NAME = "mycelialmind_dimensional_homeostasis";
    private static final String TAG_CHUNKS = "Chunks";
    private static final int MAX_FOREIGN_BLOCKS_PER_CHUNK = 96;

    private static final SavedData.Factory<DimensionalHomeostasisSavedData> FACTORY = new SavedData.Factory<>(
            DimensionalHomeostasisSavedData::new,
            DimensionalHomeostasisSavedData::load,
            null
    );

    public record InflamedChunkSnapshot(
            int chunkX,
            int chunkZ,
            double stress,
            HomeostasisStage stage,
            int sensitization
    ) {
    }

    public static final class ChunkState {
        private double stress;
        private int lastStageId;
        private long lastAggravatedTick;
        private final LinkedHashSet<BlockPos> foreignBlocks = new LinkedHashSet<>();
        private int activeCystCount;
        private int sensitizationLevel;
        private long lastLeukocyteWaveTick;

        public double getStress() {
            return stress;
        }

        public HomeostasisStage getLastStage() {
            return HomeostasisStage.byId(lastStageId);
        }

        public void setLastStage(HomeostasisStage stage) {
            this.lastStageId = stage.id();
        }

        public long getLastAggravatedTick() {
            return lastAggravatedTick;
        }

        public int getActiveCystCount() {
            return activeCystCount;
        }

        public int getSensitizationLevel() {
            return sensitizationLevel;
        }

        public long getLastLeukocyteWaveTick() {
            return lastLeukocyteWaveTick;
        }
    }

    private final Long2ObjectOpenHashMap<ChunkState> chunks = new Long2ObjectOpenHashMap<>();
    // Preserve the HashMap traversal order used by the existing diffusion loop.
    private final Map<Long, Boolean> chunkOrder = new HashMap<>();

    public static DimensionalHomeostasisSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    private ChunkState getOrCreate(long key) {
        ChunkState state = chunks.get(key);
        if (state == null) {
            state = new ChunkState();
            chunks.put(key, state);
            chunkOrder.put(key, Boolean.TRUE);
        }
        return state;
    }

    public synchronized double getStress(ChunkPos chunkPos) {
        ChunkState state = chunks.get(chunkPos.toLong());
        return state != null ? state.stress : 0.0;
    }

    public synchronized HomeostasisStage getStage(ChunkPos chunkPos) {
        return DimensionalHomeostasisEngine.stageForStress(getStress(chunkPos));
    }

    /**
     * Smooths chunk boundaries by blending 50% of adjacent orthogonal chunks' stress
     * so fog and HUD indicators do not abruptly flicker across chunk lines.
     */
    public synchronized double getEffectiveNeighborhoodStress(ChunkPos center) {
        double maxStress = getStress(center);
        int cx = center.x;
        int cz = center.z;
        long[] neighbors = {
                ChunkPos.asLong(cx + 1, cz),
                ChunkPos.asLong(cx - 1, cz),
                ChunkPos.asLong(cx, cz + 1),
                ChunkPos.asLong(cx, cz - 1)
        };
        for (long key : neighbors) {
            ChunkState n = chunks.get(key);
            if (n != null && n.stress > 0.0) {
                maxStress = Math.max(maxStress, n.stress * 0.5);
            }
        }
        return DimensionalHomeostasisEngine.clampStress(maxStress);
    }

    public synchronized HomeostasisStage getLastRecordedStage(ChunkPos chunkPos) {
        ChunkState state = chunks.get(chunkPos.toLong());
        return state != null ? state.getLastStage() : HomeostasisStage.STABLE;
    }

    public synchronized void setLastRecordedStage(ChunkPos chunkPos, HomeostasisStage stage) {
        long key = chunkPos.toLong();
        ChunkState state = chunks.get(key);
        if (state == null && stage == HomeostasisStage.STABLE) {
            return;
        }
        if (state == null) {
            state = getOrCreate(key);
        }
        if (state.lastStageId != stage.id()) {
            state.setLastStage(stage);
            pruneIfEmpty(key, state);
            setDirty();
        }
    }

    public synchronized double addSensitizedStress(ChunkPos chunkPos, double rawDelta, long gameTime) {
        int sens = getSensitization(chunkPos);
        double modulated = DimensionalHomeostasisEngine.computeSensitizedStressDelta(rawDelta, sens);
        return addStress(chunkPos, modulated, gameTime);
    }

    public synchronized double addStress(ChunkPos chunkPos, double delta, long gameTime) {
        if (delta == 0.0 || !Double.isFinite(delta)) {
            return getStress(chunkPos);
        }
        long key = chunkPos.toLong();
        ChunkState state = chunks.get(key);
        if (state == null && delta < 0.0) {
            return 0.0;
        }
        if (state == null) {
            state = getOrCreate(key);
        }
        double updated = DimensionalHomeostasisEngine.clampStress(state.stress + delta);
        if (updated != state.stress) {
            state.stress = updated;
            if (delta > 0.0) {
                state.lastAggravatedTick = gameTime;
            }
            pruneIfEmpty(key, state);
            setDirty();
        }
        return updated;
    }

    public synchronized int getSensitization(ChunkPos chunkPos) {
        ChunkState state = chunks.get(chunkPos.toLong());
        return state != null ? state.sensitizationLevel : 0;
    }

    public synchronized int getEffectiveNeighborhoodSensitization(ChunkPos center) {
        int maxSens = getSensitization(center);
        int cx = center.x;
        int cz = center.z;
        for (long key : new long[]{
                ChunkPos.asLong(cx + 1, cz),
                ChunkPos.asLong(cx - 1, cz),
                ChunkPos.asLong(cx, cz + 1),
                ChunkPos.asLong(cx, cz - 1)
        }) {
            ChunkState n = chunks.get(key);
            if (n != null && n.sensitizationLevel > maxSens) {
                maxSens = Math.max(maxSens, n.sensitizationLevel - 1);
            }
        }
        return DimensionalHomeostasisEngine.clampSensitization(maxSens);
    }

    public synchronized void setSensitization(ChunkPos chunkPos, int level) {
        long key = chunkPos.toLong();
        int clamped = DimensionalHomeostasisEngine.clampSensitization(level);
        ChunkState state = chunks.get(key);
        if (state == null && clamped == 0) {
            return;
        }
        if (state == null) {
            state = getOrCreate(key);
        }
        if (state.sensitizationLevel != clamped) {
            state.sensitizationLevel = clamped;
            pruneIfEmpty(key, state);
            setDirty();
        }
    }

    public synchronized int adjustSensitization(ChunkPos chunkPos, int delta) {
        int next = DimensionalHomeostasisEngine.clampSensitization(getSensitization(chunkPos) + delta);
        setSensitization(chunkPos, next);
        return next;
    }

    public synchronized long getLastLeukocyteWaveTick(ChunkPos chunkPos) {
        ChunkState state = chunks.get(chunkPos.toLong());
        return state != null ? state.lastLeukocyteWaveTick : 0L;
    }

    public synchronized void setLastLeukocyteWaveTick(ChunkPos chunkPos, long tick) {
        long key = chunkPos.toLong();
        ChunkState state = chunks.get(key);
        if (state == null && tick <= 0L) {
            return;
        }
        if (state == null) {
            state = getOrCreate(key);
        }
        if (state.lastLeukocyteWaveTick != tick) {
            state.lastLeukocyteWaveTick = tick;
            if (tick <= 0L) {
                pruneIfEmpty(key, state);
            }
            setDirty();
        }
    }

    public synchronized void pruneExpiredLeukocyteCooldowns(long currentGameTime) {
        boolean changed = false;
        Iterator<Map.Entry<Long, ChunkState>> iterator = chunks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, ChunkState> entry = iterator.next();
            ChunkState state = entry.getValue();
            long lastWaveTick = state.lastLeukocyteWaveTick;
            if (lastWaveTick > 0L
                    && (currentGameTime < lastWaveTick
                    || currentGameTime - lastWaveTick >= DimensionalHomeostasisEngine.LEUKOCYTE_PATROL_COOLDOWN_TICKS)) {
                state.lastLeukocyteWaveTick = 0L;
                changed = true;
            }
            if (isEmpty(state)) {
                long removedKey = entry.getKey();
                iterator.remove();
                chunkOrder.remove(removedKey);
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    public synchronized boolean hasForeignBlocks(ChunkPos chunkPos) {
        ChunkState state = chunks.get(chunkPos.toLong());
        return state != null && !state.foreignBlocks.isEmpty();
    }

    public synchronized List<InflamedChunkSnapshot> getInflamedChunksWithin(ChunkPos center, int chunkRadius, int limit) {
        if (chunkRadius < 0 || limit <= 0) {
            return List.of();
        }
        List<InflamedChunkSnapshot> out = new ArrayList<>();
        for (Map.Entry<Long, ChunkState> entry : chunks.entrySet()) {
            ChunkState cs = entry.getValue();
            if (cs.stress < DimensionalHomeostasisEngine.IRRITATED_THRESHOLD && cs.sensitizationLevel <= 0) {
                continue;
            }
            ChunkPos cp = new ChunkPos(entry.getKey());
            if (Math.abs(cp.x - center.x) <= chunkRadius && Math.abs(cp.z - center.z) <= chunkRadius) {
                out.add(new InflamedChunkSnapshot(
                        cp.x, cp.z, cs.stress,
                        DimensionalHomeostasisEngine.stageForStress(cs.stress),
                        cs.sensitizationLevel
                ));
            }
        }
        out.sort(Comparator
                .comparingLong((InflamedChunkSnapshot snapshot) -> {
                    long dx = (long) snapshot.chunkX() - center.x;
                    long dz = (long) snapshot.chunkZ() - center.z;
                    return dx * dx + dz * dz;
                })
                .thenComparingInt(InflamedChunkSnapshot::chunkX)
                .thenComparingInt(InflamedChunkSnapshot::chunkZ));
        return out.size() <= limit ? out : new ArrayList<>(out.subList(0, limit));
    }

    public synchronized void setStress(ChunkPos chunkPos, double value, long gameTime) {
        long key = chunkPos.toLong();
        double clamped = DimensionalHomeostasisEngine.clampStress(value);
        ChunkState state = chunks.get(key);
        if (state == null && clamped == 0.0) {
            return;
        }
        if (state == null) {
            state = getOrCreate(key);
        }
        if (state.stress != clamped) {
            state.stress = clamped;
            if (clamped > 0.0) {
                state.lastAggravatedTick = gameTime;
            }
            pruneIfEmpty(key, state);
            setDirty();
        }
    }

    public synchronized boolean sedateArea(ChunkPos center, int chunkRadius, double amount, long gameTime) {
        return sedateArea(center, chunkRadius, amount, 0, gameTime);
    }

    /**
     * Calms chunks in the square radius around center (100% at center, 50% in outer ring)
     * and optionally lowers Antigenic Sensitization.
     * Returns true if at least one chunk dropped from IMMUNE_RESPONSE below threshold.
     */
    public synchronized boolean sedateArea(
            ChunkPos center,
            int chunkRadius,
            double amount,
            int sensitizationReduction,
            long gameTime
    ) {
        if (amount <= 0.0 && sensitizationReduction <= 0) {
            return false;
        }
        boolean calmedFromImmune = false;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                ChunkPos target = new ChunkPos(center.x + dx, center.z + dz);
                if (sensitizationReduction > 0 && getSensitization(target) > 0) {
                    int sensDrop = (dx == 0 && dz == 0) ? sensitizationReduction : Math.max(1, sensitizationReduction - 1);
                    adjustSensitization(target, -sensDrop);
                }
                double before = getStress(target);
                if (before <= 0.0 || amount <= 0.0) {
                    continue;
                }
                double factor = (dx == 0 && dz == 0) ? 1.0 : 0.5;
                double after = addStress(target, -(amount * factor), gameTime);
                if (before >= DimensionalHomeostasisEngine.IMMUNE_THRESHOLD
                        && after < DimensionalHomeostasisEngine.IMMUNE_THRESHOLD) {
                    calmedFromImmune = true;
                }
            }
        }
        return calmedFromImmune;
    }

    public synchronized void recordForeignBlock(BlockPos pos) {
        BlockPos immutable = pos.immutable();
        long key = new ChunkPos(immutable).toLong();
        ChunkState state = getOrCreate(key);
        if (state.foreignBlocks.contains(immutable)) {
            return;
        }
        if (state.foreignBlocks.size() >= MAX_FOREIGN_BLOCKS_PER_CHUNK) {
            Iterator<BlockPos> it = state.foreignBlocks.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        state.foreignBlocks.add(immutable);
        setDirty();
    }

    public synchronized void removeForeignBlock(BlockPos pos) {
        long key = new ChunkPos(pos).toLong();
        ChunkState state = chunks.get(key);
        if (state != null && state.foreignBlocks.remove(pos)) {
            pruneIfEmpty(key, state);
            setDirty();
        }
    }

    public synchronized List<BlockPos> getForeignBlocksSnapshot(ChunkPos chunkPos) {
        ChunkState state = chunks.get(chunkPos.toLong());
        return state != null ? new ArrayList<>(state.foreignBlocks) : List.of();
    }

    public synchronized void adjustCystCount(ChunkPos chunkPos, int delta) {
        long key = chunkPos.toLong();
        ChunkState state = chunks.get(key);
        if (state == null && delta <= 0) {
            return;
        }
        if (state == null) {
            state = getOrCreate(key);
        }
        int next = Math.max(0, state.activeCystCount + delta);
        if (next != state.activeCystCount) {
            state.activeCystCount = next;
            pruneIfEmpty(key, state);
            setDirty();
        }
    }

    public synchronized int getActiveCystCount(ChunkPos chunkPos) {
        ChunkState state = chunks.get(chunkPos.toLong());
        return state != null ? state.activeCystCount : 0;
    }

    public synchronized List<Long> getTrackedChunkKeys() {
        return new ArrayList<>(chunkOrder.keySet());
    }

    public synchronized void resetAll() {
        if (!chunks.isEmpty()) {
            chunks.clear();
            chunkOrder.clear();
            setDirty();
        }
    }

    public synchronized void restoreFromSnapshot(CompoundTag snapshot, HolderLookup.Provider registries) {
        DimensionalHomeostasisSavedData restored = load(snapshot, registries);
        chunks.clear();
        chunkOrder.clear();
        for (long key : restored.chunkOrder.keySet()) {
            chunks.put(key, restored.chunks.get(key));
            chunkOrder.put(key, Boolean.TRUE);
        }
        setDirty();
    }

    public synchronized void resetChunk(ChunkPos chunkPos) {
        if (chunks.remove(chunkPos.toLong()) != null) {
            chunkOrder.remove(chunkPos.toLong());
            setDirty();
        }
    }

    private void pruneIfEmpty(long key, ChunkState state) {
        if (isEmpty(state)) {
            chunks.remove(key);
            chunkOrder.remove(key);
        }
    }

    private static boolean isEmpty(ChunkState state) {
        return state.stress <= 0.0 && state.foreignBlocks.isEmpty()
                && state.activeCystCount <= 0 && state.sensitizationLevel <= 0
                && state.lastLeukocyteWaveTick <= 0L
                && state.lastStageId == HomeostasisStage.STABLE.id();
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (long key : chunkOrder.keySet()) {
            ChunkState cs = chunks.get(key);
            if (isEmpty(cs)) {
                continue;
            }
            CompoundTag ct = new CompoundTag();
            ct.putLong("Key", key);
            ct.putDouble("Stress", cs.stress);
            ct.putInt("Stage", cs.lastStageId);
            ct.putLong("LastTick", cs.lastAggravatedTick);
            ct.putInt("Cysts", cs.activeCystCount);
            ct.putInt("Sensitization", cs.sensitizationLevel);
            ct.putLong("LastLeukocyteTick", cs.lastLeukocyteWaveTick);
            if (!cs.foreignBlocks.isEmpty()) {
                ListTag fList = new ListTag();
                for (BlockPos p : cs.foreignBlocks) {
                    fList.add(LongTag.valueOf(p.asLong()));
                }
                ct.put("Foreign", fList);
            }
            list.add(ct);
        }
        tag.put(TAG_CHUNKS, list);
        return tag;
    }

    public static DimensionalHomeostasisSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        DimensionalHomeostasisSavedData data = new DimensionalHomeostasisSavedData();
        ListTag list = tag.getList(TAG_CHUNKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ct = list.getCompound(i);
            long key = ct.getLong("Key");
            ChunkState cs = new ChunkState();
            cs.stress = DimensionalHomeostasisEngine.clampStress(ct.getDouble("Stress"));
            cs.lastStageId = ct.getInt("Stage");
            cs.lastAggravatedTick = ct.getLong("LastTick");
            cs.activeCystCount = Math.max(0, ct.getInt("Cysts"));
            cs.sensitizationLevel = DimensionalHomeostasisEngine.clampSensitization(ct.getInt("Sensitization"));
            cs.lastLeukocyteWaveTick = Math.max(0L, ct.getLong("LastLeukocyteTick"));
            if (ct.contains("Foreign", Tag.TAG_LIST)) {
                ListTag fList = ct.getList("Foreign", Tag.TAG_LONG);
                for (int j = 0; j < fList.size() && cs.foreignBlocks.size() < MAX_FOREIGN_BLOCKS_PER_CHUNK; j++) {
                    cs.foreignBlocks.add(BlockPos.of(((LongTag) fList.get(j)).getAsLong()));
                }
            }
            data.chunks.put(key, cs);
            data.chunkOrder.put(key, Boolean.TRUE);
        }
        return data;
    }
}
