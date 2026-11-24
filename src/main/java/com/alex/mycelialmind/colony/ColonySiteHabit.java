package com.alex.mycelialmind.colony;

/** Behaviors a Sovereign Colony outpost can learn from local work incidents. */
public enum ColonySiteHabit {
    NONE,
    ESCORTED_COURIER,
    REST_BEFORE_RETURN,
    STAGGERED_PICKUP;

    public static ColonySiteHabit fromIndex(int index) {
        ColonySiteHabit[] habits = values();
        return index >= 0 && index < habits.length ? habits[index] : NONE;
    }

    public String translationKey() {
        return "gui.mycelialmind.colony.habit." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
