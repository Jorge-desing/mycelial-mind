package com.alex.mycelialmind.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-dimension SavedData persisting all Sovereign Hive Colonies anchored by Sovereign Nexus blocks.
 */
public class SovereignColonySavedData extends SavedData {
    private static final String DATA_NAME = "mycelialmind_sovereign_colony";
    private static final String TAG_COLONIES = "Colonies";

    private final Map<BlockPos, SovereignColonyData> coloniesByNexus = new LinkedHashMap<>();

    public static SovereignColonySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new Factory<>(SovereignColonySavedData::new, SovereignColonySavedData::load),
                DATA_NAME
        );
    }

    public Collection<SovereignColonyData> getAllColonies() {
        return Collections.unmodifiableCollection(coloniesByNexus.values());
    }

    @Nullable
    public SovereignColonyData getByNexus(BlockPos pos) {
        return coloniesByNexus.get(pos.immutable());
    }

    @Nullable
    public SovereignColonyData getById(UUID colonyId) {
        for (SovereignColonyData data : coloniesByNexus.values()) {
            if (data.getColonyId().equals(colonyId)) {
                return data;
            }
        }
        return null;
    }

    public SovereignColonyData getOrCreateColony(BlockPos nexusPos, UUID ownerId, String ownerName) {
        BlockPos imm = nexusPos.immutable();
        SovereignColonyData existing = coloniesByNexus.get(imm);
        if (existing != null) {
            return existing;
        }
        SovereignColonyData created = new SovereignColonyData(UUID.randomUUID(), ownerId, ownerName, imm);
        coloniesByNexus.put(imm, created);
        setDirty();
        return created;
    }

    public void removeColonyAt(BlockPos nexusPos) {
        if (coloniesByNexus.remove(nexusPos.immutable()) != null) {
            setDirty();
        }
    }

    @Nullable
    public SovereignColonyData findNearestColony(BlockPos pos, @Nullable UUID preferredOwnerId, double maxDistance) {
        SovereignColonyData bestOwned = null;
        double bestOwnedDistSq = maxDistance * maxDistance;
        SovereignColonyData bestAny = null;
        double bestAnyDistSq = maxDistance * maxDistance;

        for (SovereignColonyData data : coloniesByNexus.values()) {
            double distSq = data.getNexusPos().distSqr(pos);
            if (distSq <= bestAnyDistSq) {
                bestAnyDistSq = distSq;
                bestAny = data;
            }
            if (preferredOwnerId != null && preferredOwnerId.equals(data.getOwnerId()) && distSq <= bestOwnedDistSq) {
                bestOwnedDistSq = distSq;
                bestOwned = data;
            }
        }
        return bestOwned != null ? bestOwned : bestAny;
    }

    @Nullable
    public SovereignColonyData findContainingColony(BlockPos pos) {
        SovereignColonyData best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (SovereignColonyData data : coloniesByNexus.values()) {
            int radius = data.getEffectiveRadius();
            double distSq = data.getNexusPos().distSqr(pos);
            if (distSq <= (double) radius * radius && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = data;
            }
        }
        return best;
    }

    public void resetAll() {
        coloniesByNexus.clear();
        setDirty();
    }

    public void restoreFromSnapshot(CompoundTag snapshot, HolderLookup.Provider registries) {
        SovereignColonySavedData restored = load(snapshot, registries);
        coloniesByNexus.clear();
        coloniesByNexus.putAll(restored.coloniesByNexus);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (SovereignColonyData colony : coloniesByNexus.values()) {
            list.add(colony.save());
        }
        tag.put(TAG_COLONIES, list);
        return tag;
    }

    public static SovereignColonySavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SovereignColonySavedData saved = new SovereignColonySavedData();
        ListTag list = tag.getList(TAG_COLONIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            SovereignColonyData col = SovereignColonyData.load(list.getCompound(i));
            saved.coloniesByNexus.put(col.getNexusPos(), col);
        }
        return saved;
    }
}
