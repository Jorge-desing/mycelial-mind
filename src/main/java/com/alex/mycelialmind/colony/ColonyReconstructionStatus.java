package com.alex.mycelialmind.colony;

import java.util.Locale;

/** Server-evaluated reason shown next to each remembered reconstruction candidate. */
public enum ColonyReconstructionStatus {
    READY,
    SUCCESSOR_REQUIRED,
    WITNESS_UNAVAILABLE,
    TARGET_UNLOADED,
    ROUTE_INVALID,
    PATROL_INACCESSIBLE,
    HABIT_OCCUPIED,
    HABIT_ALREADY_PRESENT,
    CAPACITY_REACHED,
    IN_TRANSIT,
    ALREADY_RESTORED,
    NO_PATH,
    SUCCESSOR_UNAVAILABLE;

    public String translationKey() {
        return "gui.mycelialmind.colony.reconstruction.status." + name().toLowerCase(Locale.ROOT);
    }
}
