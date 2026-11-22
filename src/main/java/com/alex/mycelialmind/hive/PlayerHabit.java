package com.alex.mycelialmind.hive;

/**
 * Non-lethal player tactical habits observed by the Hive Mind,
 * unlocking targeted Vanilla+ counter-adaptations above threshold.
 */
public enum PlayerHabit {
    COMBAT_MEDIC("combat_medic"),
    HIGH_MOBILITY("high_mobility"),
    BASTION_BUILDER("bastion_builder"),
    SHADOW_INFILTRATOR("shadow_infiltrator");

    private final String id;

    PlayerHabit(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static PlayerHabit fromId(String id) {
        for (PlayerHabit habit : values()) {
            if (habit.id.equals(id)) {
                return habit;
            }
        }
        return COMBAT_MEDIC;
    }
}
