package com.alex.mycelialmind.hive;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Clean-room bounded per-mob episodic memory persisted to entity NBT.
 * Captures salient survival trauma (grenade blasts, pylon suppression, desync recovery)
 * and personal attacker grudges without retaining unbounded state.
 */
public final class MobEpisodicMemory {
    public static final int MAX_ENTRIES = 16;

    public static final String KEY_TRAUMA_GRENADE = "trauma:grenade";
    public static final String KEY_TRAUMA_PYLON = "trauma:pylon";
    public static final String KEY_SURVIVED_DESYNC = "event:survived_desync";
    private static final String GRUDGE_PREFIX = "grudge:";

    private static final String NBT_LAST_TICK = "t";
    private static final String NBT_COUNT = "c";

    private record MemoryEntry(long lastTick, int count) {
    }

    private final Map<String, MemoryEntry> memories = new HashMap<>();

    public static String grudgeKey(UUID attackerId) {
        return GRUDGE_PREFIX + attackerId;
    }

    public void remember(@Nullable String key, long gameTime) {
        if (key == null || key.isBlank()) {
            return;
        }
        MemoryEntry existing = memories.get(key);
        if (existing != null) {
            memories.put(key, new MemoryEntry(gameTime, existing.count() + 1));
            return;
        }
        if (memories.size() >= MAX_ENTRIES) {
            memories.entrySet().stream()
                    .min(Comparator.comparingLong(e -> e.getValue().lastTick()))
                    .ifPresent(oldest -> memories.remove(oldest.getKey()));
        }
        memories.put(key, new MemoryEntry(gameTime, 1));
    }

    public boolean hasMemory(@Nullable String key) {
        return key != null && memories.containsKey(key);
    }

    public boolean hasRecentMemory(@Nullable String key, long nowTick, long windowTicks) {
        if (key == null || windowTicks < 0L) {
            return false;
        }
        MemoryEntry entry = memories.get(key);
        if (entry == null) {
            return false;
        }
        long elapsed = nowTick - entry.lastTick();
        return elapsed >= 0L && elapsed <= windowTicks;
    }

    public int getMemoryCount(@Nullable String key) {
        if (key == null) {
            return 0;
        }
        MemoryEntry entry = memories.get(key);
        return entry != null ? entry.count() : 0;
    }

    public long getLastTick(@Nullable String key) {
        if (key == null) {
            return -1L;
        }
        MemoryEntry entry = memories.get(key);
        return entry != null ? entry.lastTick() : -1L;
    }

    public int size() {
        return memories.size();
    }

    public void clear() {
        memories.clear();
    }

    public void writeToNbt(CompoundTag parentTag, String tagName) {
        if (memories.isEmpty()) {
            return;
        }
        CompoundTag memoryTag = new CompoundTag();
        for (Map.Entry<String, MemoryEntry> entry : memories.entrySet()) {
            CompoundTag item = new CompoundTag();
            item.putLong(NBT_LAST_TICK, entry.getValue().lastTick());
            item.putInt(NBT_COUNT, entry.getValue().count());
            memoryTag.put(entry.getKey(), item);
        }
        parentTag.put(tagName, memoryTag);
    }

    public void readFromNbt(CompoundTag parentTag, String tagName) {
        memories.clear();
        if (!parentTag.contains(tagName, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag memoryTag = parentTag.getCompound(tagName);
        for (String key : memoryTag.getAllKeys()) {
            if (memories.size() >= MAX_ENTRIES) {
                break;
            }
            if (!memoryTag.contains(key, Tag.TAG_COMPOUND)) {
                continue;
            }
            CompoundTag item = memoryTag.getCompound(key);
            long tick = item.getLong(NBT_LAST_TICK);
            int count = Math.max(1, item.getInt(NBT_COUNT));
            memories.put(key, new MemoryEntry(tick, count));
        }
    }
}
