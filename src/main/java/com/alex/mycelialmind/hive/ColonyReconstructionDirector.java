package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.block.SymbioticNodeBlock;
import com.alex.mycelialmind.colony.ColonyReconstruction;
import com.alex.mycelialmind.colony.ColonyReconstructionStatus;
import com.alex.mycelialmind.colony.ColonyReconstructionRules;
import com.alex.mycelialmind.colony.ColonySiteHabit;
import com.alex.mycelialmind.colony.ColonyWitnessMemory;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.network.SovereignColonySyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** Tracks confirmed post-Matriarch outpost losses and applies memories carried by survivors. */
public final class ColonyReconstructionDirector {
    private ColonyReconstructionDirector() {
    }

    public static void recordOutpostDestroyed(ServerLevel level, BlockPos outpostPos) {
        SovereignColonySavedData saved = SovereignColonySavedData.get(level);
        for (SovereignColonyData colony : saved.getAllColonies()) {
            var lesson = colony.getDialectLesson();
            if (lesson != null && (lesson.source().equals(outpostPos) || lesson.target().equals(outpostPos))) {
                colony.cancelDialectLesson();
                saved.setDirty();
                notifyOwner(level, colony, Component.translatable("message.mycelialmind.colony.dialect_site_destroyed")
                        .withStyle(ChatFormatting.YELLOW));
            }
            if (!com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity.isColonyActive(level, colony.getNexusPos())
                    || !colony.isKnownOutpost(outpostPos) || colony.getReconstruction(outpostPos) != null) {
                continue;
            }
            if (colony.beginOutpostReconstruction(outpostPos, level.getGameTime())) {
                saved.setDirty();
                notifyOwner(level, colony,
                        Component.translatable("message.mycelialmind.colony.reconstruction_opened",
                                outpostPos.getX(), outpostPos.getY(), outpostPos.getZ())
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
            } else {
                notifyOwner(level, colony,
                        Component.translatable("message.mycelialmind.colony.reconstruction_capacity")
                                .withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    public static ColonyReconstructionStatus getCandidateStatus(
            ServerLevel level,
            SovereignColonyData colony,
            ColonyReconstruction entry,
            ColonyWitnessMemory memory
    ) {
        ColonyReconstruction assignment = colony.findReconstructionForWitness(memory.witnessId());
        if (assignment != null && assignment != entry) {
            return ColonyReconstructionStatus.IN_TRANSIT;
        }
        ColonyReconstructionStatus status = evaluateMemoryLocation(level, colony, entry, memory, false);
        if (status == ColonyReconstructionStatus.READY
                && memory.witnessId().equals(entry.lastFailedWitness())
                && memory.type() == entry.lastFailedType()) {
            return entry.lastFailure();
        }
        return status;
    }

    public static ColonyReconstructionStatus evaluateMemoryLocation(
            ServerLevel level,
            SovereignColonyData colony,
            ColonyReconstruction entry,
            ColonyWitnessMemory memory,
            boolean includeActiveTrip
    ) {
        if (entry.isRestored(memory.type())) {
            return ColonyReconstructionStatus.ALREADY_RESTORED;
        }
        if (entry.isClosed()) {
            return ColonyReconstructionStatus.ALREADY_RESTORED;
        }
        if (entry.successorSite() == null) {
            return ColonyReconstructionStatus.SUCCESSOR_REQUIRED;
        }
        if (!includeActiveTrip && entry.isAssignedTo(memory.witnessId())) {
            return ColonyReconstructionStatus.IN_TRANSIT;
        }
        if (!colony.getLinkedOutposts().contains(entry.successorSite())) {
            return ColonyReconstructionStatus.SUCCESSOR_UNAVAILABLE;
        }
        if (!level.isLoaded(entry.successorSite())) {
            return ColonyReconstructionStatus.TARGET_UNLOADED;
        }
        if (!level.getBlockState(entry.successorSite()).is(ModBlocks.SYMBIOTIC_NODE.get())) {
            return ColonyReconstructionStatus.SUCCESSOR_UNAVAILABLE;
        }
        if (!(level.getEntity(memory.witnessId()) instanceof ColonySymbiontEntity witness) || !witness.isAlive()) {
            return ColonyReconstructionStatus.WITNESS_UNAVAILABLE;
        }

        if (memory.type() == ColonyWitnessMemory.Type.COURIER_INPUT
                || memory.type() == ColonyWitnessMemory.Type.COURIER_OUTPUT) {
            return validateChestMemory(level, colony, entry, memory);
        }
        if (memory.type() == ColonyWitnessMemory.Type.SENTINEL_PATROL) {
            BlockPos patrolPoint = colony.reconstructedPatrolPoint(entry, memory);
            boolean inBounds = patrolPoint != null && level.isInWorldBounds(patrolPoint);
            boolean loaded = inBounds && level.isLoaded(patrolPoint);
            boolean walkable = loaded && isWalkablePatrolPoint(level, patrolPoint);
            boolean hasCapacity = patrolPoint != null
                    && (colony.getWorkZones(SovereignColonyData.WorkZoneType.PATROL).contains(patrolPoint)
                    || colony.getWorkZones(SovereignColonyData.WorkZoneType.PATROL).size()
                    < com.alex.mycelialmind.colony.SovereignColonyEngine.MAX_WORK_ZONES);
            return ColonyReconstructionRules.patrolCandidateStatus(inBounds, loaded, walkable, hasCapacity);
        }
        ColonySiteHabit habit = memory.type().habit();
        var successorMemory = colony.getExistingSiteMemory(entry.successorSite());
        ColonySiteHabit learned = successorMemory == null ? ColonySiteHabit.NONE : successorMemory.learnedHabit();
        ColonySiteHabit trial = successorMemory == null ? ColonySiteHabit.NONE : successorMemory.trialHabit();
        return ColonyReconstructionRules.habitSlotStatus(learned, trial, habit);
    }

    private static ColonyReconstructionStatus validateChestMemory(
            ServerLevel level,
            SovereignColonyData colony,
            ColonyReconstruction entry,
            ColonyWitnessMemory memory
    ) {
        BlockPos chest = memory.memoryPos();
        boolean loaded = level.isLoaded(chest);
        boolean hasItemHandler = loaded && hasItemHandler(level, chest);
        boolean input = memory.type() == ColonyWitnessMemory.Type.COURIER_INPUT;
        boolean alreadyAssigned = input
                ? colony.getInputChests().contains(chest)
                : colony.getOutputChests().contains(chest);
        boolean oppositeAssigned = input
                ? colony.getOutputChests().contains(chest)
                : colony.getInputChests().contains(chest);
        int count = input ? colony.getInputChests().size() : colony.getOutputChests().size();
        int max = input
                ? com.alex.mycelialmind.colony.SovereignColonyEngine.MAX_INPUT_CHESTS
                : com.alex.mycelialmind.colony.SovereignColonyEngine.MAX_OUTPUT_CHESTS;
        boolean hasCapacity = alreadyAssigned || oppositeAssigned || count < max;
        return ColonyReconstructionRules.routeCandidateStatus(entry.successorSite(), chest,
                colony.getEffectiveRadius(), loaded, hasItemHandler, hasCapacity);
    }

    private static boolean hasItemHandler(ServerLevel level, BlockPos pos) {
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) {
            return true;
        }
        for (Direction side : Direction.values()) {
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
            if (handler != null) {
                return true;
            }
        }
        return false;
    }
    private static boolean isWalkablePatrolPoint(ServerLevel level, BlockPos pos) {
        BlockState feet = level.getBlockState(pos);
        BlockState head = level.getBlockState(pos.above());
        BlockState floor = level.getBlockState(pos.below());
        return feet.getCollisionShape(level, pos).isEmpty()
                && head.getCollisionShape(level, pos.above()).isEmpty()
                && floor.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    public static boolean completeWitnessTrip(
            ServerLevel level,
            SovereignColonyData colony,
            ColonyReconstruction entry,
            ColonySymbiontEntity witness
    ) {
        ColonyWitnessMemory.Type type = entry.activeType();
        if (type == null || !entry.isAssignedTo(witness.getUUID())) {
            return false;
        }
        ColonyWitnessMemory memory = colony.getWitnessMemory(witness.getUUID(), entry.ruinedSite(), type);
        if (memory == null) {
            colony.recordReconstructionFailure(entry.ruinedSite(), witness.getUUID(),
                    ColonyReconstructionStatus.WITNESS_UNAVAILABLE);
            SovereignColonySavedData.get(level).setDirty();
            return false;
        }
        ColonyReconstructionStatus status = evaluateMemoryLocation(level, colony, entry, memory, true);
        if (status == ColonyReconstructionStatus.TARGET_UNLOADED) {
            witness.setTaskState("reconstruction_waiting_chunk");
            return false;
        }
        if (status == ColonyReconstructionStatus.NO_PATH) {
            colony.recordReconstructionFailure(entry.ruinedSite(), witness.getUUID(), status);
            SovereignColonySavedData.get(level).setDirty();
            return false;
        }
        boolean restored = false;
        if (status == ColonyReconstructionStatus.HABIT_ALREADY_PRESENT) {
            restored = true;
        } else if (status == ColonyReconstructionStatus.READY) {
            if (type == ColonyWitnessMemory.Type.COURIER_INPUT
                    || type == ColonyWitnessMemory.Type.COURIER_OUTPUT) {
                var path = witness.getNavigation().createPath(memory.memoryPos(), 0);
                if (path == null || !path.canReach()) {
                    status = ColonyReconstructionStatus.NO_PATH;
                } else {
                    restored = type == ColonyWitnessMemory.Type.COURIER_INPUT
                            ? colony.addInputChestRole(memory.memoryPos())
                            : colony.addOutputChestRole(memory.memoryPos());
                    if (!restored) {
                        status = ColonyReconstructionStatus.CAPACITY_REACHED;
                    }
                }
            } else if (type == ColonyWitnessMemory.Type.SENTINEL_PATROL) {
                BlockPos patrolPoint = colony.reconstructedPatrolPoint(entry, memory);
                var path = witness.getNavigation().createPath(patrolPoint, 0);
                if (path == null || !path.canReach()) {
                    status = ColonyReconstructionStatus.PATROL_INACCESSIBLE;
                } else {
                    restored = colony.addWorkZoneIfAbsent(SovereignColonyData.WorkZoneType.PATROL, patrolPoint);
                    if (!restored) {
                        status = ColonyReconstructionStatus.CAPACITY_REACHED;
                    }
                }
            } else {
                var successorMemory = colony.getSiteMemory(entry.successorSite());
                restored = successorMemory != null && successorMemory.receiveTransferredHabit(type.habit());
                if (!restored) {
                    status = ColonyReconstructionStatus.HABIT_OCCUPIED;
                }
            }
        }
        if (!restored) {
            colony.recordReconstructionFailure(entry.ruinedSite(), witness.getUUID(), status);
            SovereignColonySavedData.get(level).setDirty();
            return false;
        }

        colony.completeReconstructionMemory(entry.ruinedSite(), witness.getUUID(), type);
        SovereignColonySavedData.get(level).setDirty();
        level.sendParticles(com.alex.mycelialmind.registry.ModParticles.PSYCHIC_WAVE.get(),
                entry.successorSite().getX() + 0.5D, entry.successorSite().getY() + 1.0D,
                entry.successorSite().getZ() + 0.5D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.playSound(null, entry.successorSite(), com.alex.mycelialmind.registry.ModSounds.COLONY_RECONSTRUCTION_PULSE.get(),
                witness.getSoundSource(), 0.85F, 1.0F);
        witness.setTaskState("idle");
        notifyOwner(level, colony,
                Component.translatable("message.mycelialmind.colony.reconstruction_restored",
                        Component.translatable(type.translationKey()),
                        entry.successorSite().getX(), entry.successorSite().getY(), entry.successorSite().getZ())
                        .withStyle(ChatFormatting.GREEN));
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(colony.getOwnerId());
        if (owner != null) {
            SovereignColonySyncPayload.syncToPlayer(owner, colony.getNexusPos(), false);
        }
        return true;
    }

    private static void notifyOwner(ServerLevel level, SovereignColonyData colony, Component message) {
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(colony.getOwnerId());
        if (owner != null) {
            owner.displayClientMessage(message, false);
            SovereignColonySyncPayload.syncToPlayer(owner, colony.getNexusPos(), false);
        }
    }
}
