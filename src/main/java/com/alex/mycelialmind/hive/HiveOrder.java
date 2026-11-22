package com.alex.mycelialmind.hive;

/**
 * Internal coordinated tactical order issued by the Hive Mind to active units
 * via HiveBlackboard and SBL Brain memory.
 */
public enum HiveOrder {
    NONE,
    RALLY_NODE,
    FLANK_PERIMETER,
    PRESSURE_TARGET;

    public static HiveOrder fromName(String name) {
        if (name == null || name.isBlank()) {
            return NONE;
        }
        for (HiveOrder order : values()) {
            if (order.name().equals(name)) {
                return order;
            }
        }
        return NONE;
    }
}
