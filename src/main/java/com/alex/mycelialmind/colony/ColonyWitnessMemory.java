package com.alex.mycelialmind.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Locale;
import java.util.UUID;

/** A learned, site-specific experience carried by one surviving colony symbiont. */
public record ColonyWitnessMemory(UUID witnessId, BlockPos sitePos, Type type, BlockPos memoryPos) {
    public enum Type {
        COURIER_INPUT,
        COURIER_OUTPUT,
        SENTINEL_PATROL,
        HABIT_ESCORTED_COURIER,
        HABIT_REST_BEFORE_RETURN,
        HABIT_STAGGERED_PICKUP;

        public static Type fromIndex(int index) {
            Type[] values = values();
            return index >= 0 && index < values.length ? values[index] : null;
        }

        public boolean isHabit() {
            return switch (this) {
            case HABIT_ESCORTED_COURIER, HABIT_REST_BEFORE_RETURN, HABIT_STAGGERED_PICKUP -> true;
            default -> false;
            };
        }

        public ColonySiteHabit habit() {
            return switch (this) {
            case HABIT_ESCORTED_COURIER -> ColonySiteHabit.ESCORTED_COURIER;
            case HABIT_REST_BEFORE_RETURN -> ColonySiteHabit.REST_BEFORE_RETURN;
            case HABIT_STAGGERED_PICKUP -> ColonySiteHabit.STAGGERED_PICKUP;
            default -> ColonySiteHabit.NONE;
            };
        }

        public String translationKey() {
            return "gui.mycelialmind.colony.reconstruction.memory." + name().toLowerCase(Locale.ROOT);
        }
    }

    public record Key(UUID witnessId, BlockPos sitePos, Type type) {
        public Key {
            sitePos = sitePos.immutable();
        }
    }

    public ColonyWitnessMemory {
        if (witnessId == null || sitePos == null || type == null || memoryPos == null) {
            throw new IllegalArgumentException("A colony witness memory requires an id, site, type and position");
        }
        sitePos = sitePos.immutable();
        memoryPos = memoryPos.immutable();
        if (type.isHabit()) {
            memoryPos = BlockPos.ZERO;
        }
        if (type == Type.SENTINEL_PATROL && !ColonyReconstructionRules.isSafePatrolOffset(memoryPos)) {
            throw new IllegalArgumentException("Patrol memory offset exceeds the safe reconstruction radius");
        }
    }

    public Key key() {
        return new Key(witnessId, sitePos, type);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Witness", witnessId);
        tag.putLong("Site", sitePos.asLong());
        tag.putInt("Type", type.ordinal());
        tag.putLong("MemoryPos", memoryPos.asLong());
        return tag;
    }

    public static ColonyWitnessMemory load(CompoundTag tag) {
        if (!tag.hasUUID("Witness")) {
            return null;
        }
        Type type = Type.fromIndex(tag.getInt("Type"));
        if (type == null) {
            return null;
        }
        BlockPos site = BlockPos.of(tag.getLong("Site"));
        BlockPos memoryPos = BlockPos.of(tag.getLong("MemoryPos"));
        if (type == Type.SENTINEL_PATROL && !ColonyReconstructionRules.isSafePatrolOffset(memoryPos)) {
            return null;
        }
        return new ColonyWitnessMemory(tag.getUUID("Witness"), site, type, memoryPos);
    }
}
