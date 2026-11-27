package com.alex.mycelialmind.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.roaringbitmap.longlong.Roaring64NavigableMap;

/**
 * Compressed 64-bit spatial chunk index backed by {@link Roaring64NavigableMap}.
 * Supports fast membership checks and set-intersection queries over signed chunk coordinates.
 */
public final class MycelialSpatialBitset {
    private final Roaring64NavigableMap bitmap = new Roaring64NavigableMap();

    public synchronized void addChunk(int chunkX, int chunkZ) {
        bitmap.addLong(ChunkPos.asLong(chunkX, chunkZ));
    }

    public synchronized void addBlockChunk(BlockPos pos) {
        if (pos != null) {
            bitmap.addLong(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
        }
    }

    public synchronized void removeChunk(int chunkX, int chunkZ) {
        bitmap.removeLong(ChunkPos.asLong(chunkX, chunkZ));
    }

    public synchronized boolean containsChunk(int chunkX, int chunkZ) {
        return bitmap.contains(ChunkPos.asLong(chunkX, chunkZ));
    }

    public synchronized boolean containsBlockChunk(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        return bitmap.contains(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
    }

    public synchronized long cardinality() {
        return bitmap.getLongCardinality();
    }

    public synchronized void clear() {
        bitmap.clear();
    }

    public synchronized void rebuildFromBlocks(Iterable<BlockPos> positions) {
        bitmap.clear();
        if (positions == null) {
            return;
        }
        for (BlockPos pos : positions) {
            if (pos != null) {
                bitmap.addLong(ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
            }
        }
    }

    public synchronized long intersectionCardinality(MycelialSpatialBitset other) {
        if (other == null) {
            return 0L;
        }
        synchronized (other) {
            return Roaring64NavigableMap.andCardinality(this.bitmap, other.bitmap);
        }
    }
}
