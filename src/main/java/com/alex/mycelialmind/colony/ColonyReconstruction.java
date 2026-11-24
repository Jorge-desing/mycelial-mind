package com.alex.mycelialmind.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/** Persistent state for rebuilding one destroyed post-Matriarch outpost from witness memories. */
public final class ColonyReconstruction {
    private final BlockPos ruinedSite;
    private final long createdTick;
    private final int requiredMask;
    private int restoredMask;
    @Nullable
    private BlockPos successorSite;
    @Nullable
    private UUID activeWitness;
    @Nullable
    private ColonyWitnessMemory.Type activeType;
    @Nullable
    private UUID lastFailedWitness;
    @Nullable
    private ColonyWitnessMemory.Type lastFailedType;
    private ColonyReconstructionStatus lastFailure = ColonyReconstructionStatus.READY;

    public ColonyReconstruction(BlockPos ruinedSite, long createdTick, int requiredMask) {
        this.ruinedSite = ruinedSite.immutable();
        this.createdTick = createdTick;
        this.requiredMask = requiredMask & ((1 << ColonyWitnessMemory.Type.values().length) - 1);
    }

    private ColonyReconstruction(BlockPos ruinedSite, long createdTick, int requiredMask, int restoredMask) {
        this(ruinedSite, createdTick, requiredMask);
        this.restoredMask = restoredMask & this.requiredMask;
    }

    public BlockPos ruinedSite() {
        return ruinedSite;
    }

    public long createdTick() {
        return createdTick;
    }

    public int requiredMask() {
        return requiredMask;
    }

    public int restoredMask() {
        return restoredMask;
    }

    @Nullable
    public BlockPos successorSite() {
        return successorSite;
    }

    public boolean hasSuccessor() {
        return successorSite != null;
    }

    public boolean requires(ColonyWitnessMemory.Type type) {
        return (requiredMask & ColonyReconstructionRules.restoreBit(type)) != 0;
    }

    public boolean isRestored(ColonyWitnessMemory.Type type) {
        return (restoredMask & ColonyReconstructionRules.restoreBit(type)) != 0;
    }

    public boolean isClosed() {
        return (restoredMask & requiredMask) == requiredMask;
    }

    public boolean setSuccessor(BlockPos successor) {
        if (isClosed() || successor == null || successor.equals(ruinedSite)) {
            return false;
        }
        if (successorSite != null && successorSite.equals(successor)) {
            return false;
        }
        successorSite = successor.immutable();
        clearActiveTrip();
        clearLastFailure();
        return true;
    }

    public void clearSuccessor() {
        successorSite = null;
        clearActiveTrip();
    }

    public boolean beginTrip(UUID witnessId, ColonyWitnessMemory.Type type) {
        if (isClosed() || !requires(type) || isRestored(type) || activeWitness != null || successorSite == null) {
            return false;
        }
        activeWitness = witnessId;
        activeType = type;
        clearLastFailure();
        return true;
    }

    public boolean isAssignedTo(UUID witnessId) {
        return witnessId != null && witnessId.equals(activeWitness);
    }

    @Nullable
    public UUID activeWitness() {
        return activeWitness;
    }

    @Nullable
    public ColonyWitnessMemory.Type activeType() {
        return activeType;
    }

    public boolean hasActiveTrip() {
        return activeWitness != null && activeType != null;
    }

    public void completeTrip(UUID witnessId, ColonyWitnessMemory.Type type) {
        if (isAssignedTo(witnessId) && activeType == type) {
            restoredMask |= ColonyReconstructionRules.restoreBit(type);
            clearActiveTrip();
            clearLastFailure();
        }
    }

    public void failTrip(UUID witnessId, ColonyReconstructionStatus status) {
        if (isAssignedTo(witnessId) && activeType != null) {
            lastFailedWitness = witnessId;
            lastFailedType = activeType;
            lastFailure = status;
            clearActiveTrip();
        }
    }

    public void cancelTrip() {
        clearActiveTrip();
    }

    public void cancelTripFor(UUID witnessId) {
        if (isAssignedTo(witnessId)) {
            clearActiveTrip();
        }
    }

    @Nullable
    public UUID lastFailedWitness() {
        return lastFailedWitness;
    }

    @Nullable
    public ColonyWitnessMemory.Type lastFailedType() {
        return lastFailedType;
    }

    public ColonyReconstructionStatus lastFailure() {
        return lastFailure;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("RuinedSite", ruinedSite.asLong());
        tag.putLong("CreatedTick", createdTick);
        tag.putInt("RequiredMask", requiredMask);
        tag.putInt("RestoredMask", restoredMask);
        if (successorSite != null) {
            tag.putLong("SuccessorSite", successorSite.asLong());
        }
        if (activeWitness != null && activeType != null) {
            tag.putUUID("ActiveWitness", activeWitness);
            tag.putInt("ActiveType", activeType.ordinal());
        }
        if (lastFailedWitness != null && lastFailedType != null) {
            tag.putUUID("LastFailedWitness", lastFailedWitness);
            tag.putInt("LastFailedType", lastFailedType.ordinal());
            tag.putInt("LastFailure", lastFailure.ordinal());
        }
        return tag;
    }

    @Nullable
    public static ColonyReconstruction load(CompoundTag tag) {
        ColonyReconstruction entry = new ColonyReconstruction(BlockPos.of(tag.getLong("RuinedSite")),
                tag.getLong("CreatedTick"), tag.getInt("RequiredMask"), tag.getInt("RestoredMask"));
        if (tag.contains("SuccessorSite")) {
            BlockPos successor = BlockPos.of(tag.getLong("SuccessorSite"));
            if (!successor.equals(entry.ruinedSite)) {
                entry.successorSite = successor;
            }
        }
        if (tag.hasUUID("ActiveWitness")) {
            ColonyWitnessMemory.Type active = ColonyWitnessMemory.Type.fromIndex(tag.getInt("ActiveType"));
            if (active != null && entry.requires(active) && !entry.isRestored(active) && !entry.isClosed()) {
                entry.activeWitness = tag.getUUID("ActiveWitness");
                entry.activeType = active;
            }
        }
        if (tag.hasUUID("LastFailedWitness")) {
            ColonyWitnessMemory.Type failed = ColonyWitnessMemory.Type.fromIndex(tag.getInt("LastFailedType"));
            int status = tag.getInt("LastFailure");
            if (failed != null && status >= 0 && status < ColonyReconstructionStatus.values().length) {
                entry.lastFailedWitness = tag.getUUID("LastFailedWitness");
                entry.lastFailedType = failed;
                entry.lastFailure = ColonyReconstructionStatus.values()[status];
            }
        }
        return entry;
    }

    private void clearActiveTrip() {
        activeWitness = null;
        activeType = null;
    }

    private void clearLastFailure() {
        lastFailedWitness = null;
        lastFailedType = null;
        lastFailure = ColonyReconstructionStatus.READY;
    }
}
