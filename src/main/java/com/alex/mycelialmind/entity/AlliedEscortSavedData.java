package com.alex.mycelialmind.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Tracks temporary allied escorts across dimensions and unloaded chunks until their global expiry.
 */
public final class AlliedEscortSavedData extends SavedData {
    private static final String DATA_NAME = "mycelialmind_allied_escorts";
    private static final String TAG_ENTRIES = "Escorts";
    private static final String TAG_ENTITY = "Entity";
    private static final String TAG_OWNER = "Owner";
    private static final String TAG_EXPIRY = "Expiry";

    private static final SavedData.Factory<AlliedEscortSavedData> FACTORY = new SavedData.Factory<>(
            AlliedEscortSavedData::new,
            AlliedEscortSavedData::load,
            null
    );

    private final Map<UUID, EscortRecord> escortsByEntity = new HashMap<>();

    public static AlliedEscortSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public synchronized void register(UUID entityUuid, UUID ownerUuid, long expiryTick) {
        EscortRecord next = new EscortRecord(ownerUuid, expiryTick);
        EscortRecord previous = escortsByEntity.put(entityUuid, next);
        if (!next.equals(previous)) {
            setDirty();
        }
    }

    public synchronized void unregister(UUID entityUuid) {
        if (escortsByEntity.remove(entityUuid) != null) {
            setDirty();
        }
    }

    public synchronized int countActiveForOwner(UUID ownerUuid, long currentTick) {
        boolean pruned = false;
        Iterator<Map.Entry<UUID, EscortRecord>> iterator = escortsByEntity.entrySet().iterator();
        int count = 0;
        while (iterator.hasNext()) {
            EscortRecord escort = iterator.next().getValue();
            if (escort.expiryTick() <= currentTick) {
                iterator.remove();
                pruned = true;
            } else if (ownerUuid.equals(escort.ownerUuid())) {
                count++;
            }
        }
        if (pruned) {
            setDirty();
        }
        return count;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, EscortRecord> entry : escortsByEntity.entrySet()) {
            CompoundTag record = new CompoundTag();
            record.putUUID(TAG_ENTITY, entry.getKey());
            record.putUUID(TAG_OWNER, entry.getValue().ownerUuid());
            record.putLong(TAG_EXPIRY, entry.getValue().expiryTick());
            entries.add(record);
        }
        tag.put(TAG_ENTRIES, entries);
        return tag;
    }

    public static AlliedEscortSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        AlliedEscortSavedData data = new AlliedEscortSavedData();
        ListTag entries = tag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag record = entries.getCompound(i);
            if (record.hasUUID(TAG_ENTITY) && record.hasUUID(TAG_OWNER) && record.contains(TAG_EXPIRY, Tag.TAG_LONG)) {
                UUID entityUuid = record.getUUID(TAG_ENTITY);
                UUID ownerUuid = record.getUUID(TAG_OWNER);
                long expiryTick = record.getLong(TAG_EXPIRY);
                if (expiryTick > 0L) {
                    data.escortsByEntity.put(entityUuid, new EscortRecord(ownerUuid, expiryTick));
                }
            }
        }
        return data;
    }

    private record EscortRecord(UUID ownerUuid, long expiryTick) {
        private EscortRecord {
            Objects.requireNonNull(ownerUuid);
        }
    }
}
