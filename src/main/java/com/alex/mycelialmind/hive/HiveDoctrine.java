package com.alex.mycelialmind.hive;

/**
 * Strategic macro posture of the Hive Mind evaluated from threat, Fungal Tide,
 * suppression state, and recent synaptic node destruction.
 */
public enum HiveDoctrine {
    DORMANT(HiveOrder.NONE),
    FORAGING(HiveOrder.FLANK_PERIMETER),
    ADAPTIVE_HUNT(HiveOrder.FLANK_PERIMETER),
    SIEGE_ASSAULT(HiveOrder.PRESSURE_TARGET),
    RECOVERY(HiveOrder.RALLY_NODE);

    private final HiveOrder defaultOrder;

    HiveDoctrine(HiveOrder defaultOrder) {
        this.defaultOrder = defaultOrder;
    }

    public HiveOrder defaultOrder() {
        return defaultOrder;
    }

    public static HiveDoctrine evaluate(HiveMindSavedData hive, long gameTime, boolean recentNodeLoss) {
        if (hive.isSuppressed(gameTime) || hive.getThreatLevel() < 15.0) {
            return DORMANT;
        }
        if (recentNodeLoss) {
            return RECOVERY;
        }
        if (hive.isFungalTideActive(gameTime) || hive.getThreatLevel() >= 75.0) {
            return SIEGE_ASSAULT;
        }
        if (hive.getThreatLevel() >= 40.0) {
            return ADAPTIVE_HUNT;
        }
        return FORAGING;
    }
}
