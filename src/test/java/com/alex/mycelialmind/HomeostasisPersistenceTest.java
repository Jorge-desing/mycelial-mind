package com.alex.mycelialmind;

import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.level.ChunkPos;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HomeostasisPersistenceTest {
    @Test
    void chunkOrderMatchesLegacyHashMapAfterLoadSaveAndRemoval() {
        var data = new DimensionalHomeostasisSavedData();
        var expected = new java.util.HashMap<Long, Boolean>();
        for (int i = -30; i < 30; i++) {
            ChunkPos pos = new ChunkPos(i, i * 7);
            data.addStress(pos, 50.0, 1L);
            expected.put(pos.toLong(), Boolean.TRUE);
        }
        for (int i = -30; i < 30; i += 3) {
            ChunkPos pos = new ChunkPos(i, i * 7);
            data.resetChunk(pos);
            expected.remove(pos.toLong());
        }
        assertEquals(new java.util.ArrayList<>(expected.keySet()), data.getTrackedChunkKeys());
        var reloaded = DimensionalHomeostasisSavedData.load(data.save(new CompoundTag(), null), null);
        var legacyReloaded = new java.util.HashMap<Long, Boolean>();
        for (long key : expected.keySet()) legacyReloaded.put(key, Boolean.TRUE);
        assertEquals(new java.util.ArrayList<>(legacyReloaded.keySet()), reloaded.getTrackedChunkKeys());
    }

    @Test
    void expirationRemovesEmptyChunksAndKeepsLiveEntries() {
        var data = new DimensionalHomeostasisSavedData();
        ChunkPos expired = new ChunkPos(-4, 8);
        ChunkPos retained = new ChunkPos(3, 6);
        data.setLastLeukocyteWaveTick(expired, 100);
        data.setLastLeukocyteWaveTick(retained, 100);
        data.addStress(retained, 40, 100);
        data.pruneExpiredLeukocyteCooldowns(1);
        assertFalse(data.getTrackedChunkKeys().contains(expired.toLong()));
        assertTrue(data.getTrackedChunkKeys().contains(retained.toLong()));
        assertEquals(40, data.getStress(retained));
    }

    @Test
    void legacyChunksLoadAndSurviveSaveReload() {
        CompoundTag legacy = new CompoundTag();
        ListTag chunks = new ListTag();
        ChunkPos pos = new ChunkPos(-11, 23);
        CompoundTag chunk = new CompoundTag();
        chunk.putLong("Key", pos.toLong());
        chunk.putDouble("Stress", 72.5);
        chunk.putInt("Sensitization", 2);
        chunk.putInt("Stage", 2);
        chunk.putLong("LastTick", 1234);
        chunks.add(chunk);
        legacy.put("Chunks", chunks);
        var loaded = DimensionalHomeostasisSavedData.load(legacy, null);
        assertEquals(72.5, loaded.getStress(pos));
        var saved = loaded.save(new CompoundTag(), null);
        var reloaded = DimensionalHomeostasisSavedData.load(saved, null);
        assertEquals(72.5, reloaded.getStress(pos));
        assertEquals(2, reloaded.getSensitization(pos));
    }
}
