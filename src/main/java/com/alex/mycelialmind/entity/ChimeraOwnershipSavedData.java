package com.alex.mycelialmind.entity;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps owner assignments for persistent Chimeras, including entities in unloaded chunks.
 */
public final class ChimeraOwnershipSavedData extends SavedData {
    private static final String DATA_NAME = "mycelialmind_chimera_owners";
    private static final String TAG_ENTRIES = "Chimeras";
    private static final String TAG_ENTITY = "Entity";
    private static final String TAG_OWNER = "Owner";

    private static final SavedData.Factory<ChimeraOwnershipSavedData> FACTORY = new SavedData.Factory<>(
            ChimeraOwnershipSavedData::new,
            ChimeraOwnershipSavedData::load,
            null
    );

    private final Map<UUID, UUID> ownerByEntity = new HashMap<>();

    public static ChimeraOwnershipSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public synchronized void register(UUID entityUuid, UUID ownerUuid) {
        UUID previousOwner = ownerByEntity.put(entityUuid, ownerUuid);
        if (!ownerUuid.equals(previousOwner)) {
            setDirty();
        }
    }

    public synchronized void unregister(UUID entityUuid) {
        if (ownerByEntity.remove(entityUuid) != null) {
            setDirty();
        }
    }

    public synchronized int countForOwner(UUID ownerUuid) {
        int count = 0;
        for (UUID registeredOwner : ownerByEntity.values()) {
            if (ownerUuid.equals(registeredOwner)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, UUID> entry : ownerByEntity.entrySet()) {
            CompoundTag record = new CompoundTag();
            record.putUUID(TAG_ENTITY, entry.getKey());
            record.putUUID(TAG_OWNER, entry.getValue());
            entries.add(record);
        }
        tag.put(TAG_ENTRIES, entries);
        return tag;
    }

    public static ChimeraOwnershipSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        ChimeraOwnershipSavedData data = new ChimeraOwnershipSavedData();
        ListTag entries = tag.getList(TAG_ENTRIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag record = entries.getCompound(i);
            if (record.hasUUID(TAG_ENTITY) && record.hasUUID(TAG_OWNER)) {
                data.ownerByEntity.put(record.getUUID(TAG_ENTITY), record.getUUID(TAG_OWNER));
            }
        }
        return data;
    }
}
