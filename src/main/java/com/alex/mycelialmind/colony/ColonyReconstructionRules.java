package com.alex.mycelialmind.colony;

import net.minecraft.core.BlockPos;

import java.util.Collection;

/** Pure spatial rules for safe, coordinate-preserving colony reconstruction. */
public final class ColonyReconstructionRules {
    public static final int MAX_PATROL_OFFSET = 48;

    private ColonyReconstructionRules() {
    }

    public static boolean isValidSuccessor(BlockPos ruinedSite, BlockPos candidate, Collection<BlockPos> linkedOutposts) {
        return ruinedSite != null && candidate != null && !ruinedSite.equals(candidate)
                && linkedOutposts != null && linkedOutposts.contains(candidate);
    }

    public static boolean isSafePatrolOffset(BlockPos relativeOffset) {
        return relativeOffset != null
                && relativeOffset.getX() >= -MAX_PATROL_OFFSET && relativeOffset.getX() <= MAX_PATROL_OFFSET
                && relativeOffset.getY() >= -MAX_PATROL_OFFSET && relativeOffset.getY() <= MAX_PATROL_OFFSET
                && relativeOffset.getZ() >= -MAX_PATROL_OFFSET && relativeOffset.getZ() <= MAX_PATROL_OFFSET;
    }

    public static BlockPos resolvePatrolPoint(BlockPos successor, BlockPos relativeOffset) {
        if (successor == null || !isSafePatrolOffset(relativeOffset)) {
            return null;
        }
        return successor.offset(relativeOffset.getX(), relativeOffset.getY(), relativeOffset.getZ()).immutable();
    }

    public static ColonyReconstructionStatus routeCandidateStatus(
            BlockPos successor,
            BlockPos chest,
            double maxRange,
            boolean targetLoaded,
            boolean hasItemHandler,
            boolean hasCapacity
    ) {
        if (!targetLoaded) {
            return ColonyReconstructionStatus.TARGET_UNLOADED;
        }
        if (successor == null || chest == null || successor.distSqr(chest) > maxRange * maxRange || !hasItemHandler) {
            return ColonyReconstructionStatus.ROUTE_INVALID;
        }
        return hasCapacity ? ColonyReconstructionStatus.READY : ColonyReconstructionStatus.CAPACITY_REACHED;
    }

    public static ColonyReconstructionStatus patrolCandidateStatus(
            boolean inWorldBounds,
            boolean targetLoaded,
            boolean walkable,
            boolean hasCapacity
    ) {
        if (!inWorldBounds) {
            return ColonyReconstructionStatus.PATROL_INACCESSIBLE;
        }
        if (!targetLoaded) {
            return ColonyReconstructionStatus.TARGET_UNLOADED;
        }
        if (!walkable) {
            return ColonyReconstructionStatus.PATROL_INACCESSIBLE;
        }
        return hasCapacity ? ColonyReconstructionStatus.READY : ColonyReconstructionStatus.CAPACITY_REACHED;
    }

    public static ColonyReconstructionStatus habitSlotStatus(
            ColonySiteHabit learned,
            ColonySiteHabit trial,
            ColonySiteHabit candidate
    ) {
        if (learned == candidate || trial == candidate) {
            return ColonyReconstructionStatus.HABIT_ALREADY_PRESENT;
        }
        if (learned != ColonySiteHabit.NONE || trial != ColonySiteHabit.NONE) {
            return ColonyReconstructionStatus.HABIT_OCCUPIED;
        }
        return ColonyReconstructionStatus.READY;
    }

    public static int restoreBit(ColonyWitnessMemory.Type type) {
        return 1 << type.ordinal();
    }
}
