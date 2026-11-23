package com.alex.mycelialmind.hive;

import java.util.Map;

/** Pure rules used by post-Matriarch expeditions. */
public final class PostMatriarchExpeditionRules {
    public static final int BASE_PALETTE = 0;
    public static final int MELEE_PALETTE = 1;
    public static final int PROJECTILE_PALETTE = 2;
    public static final int FIRE_PALETTE = 3;
    public static final int EXPLOSION_PALETTE = 4;
    public static final int MAGIC_PALETTE = 5;
    public static final int REQUIRED_ROUTE_PULSES = 3;
    public static final int REQUIRED_GARDEN_FOCI = 3;
    public static final long ROUTE_PULSE_COOLDOWN_TICKS = 100L;

    private PostMatriarchExpeditionRules() {
    }

    /** The original Matriarch flag is the migration path for worlds created before expeditions existed. */
    public static boolean isUnlocked(HiveMindSavedData hive) {
        return hive != null && hive.isMatriarchDefeated();
    }

    public static int paletteForStimuli(Map<StimulusType, Double> scores) {
        StimulusType dominant = dominantStimulus(scores);
        if (dominant == null) {
            return BASE_PALETTE;
        }
        return switch (dominant) {
            case MELEE -> MELEE_PALETTE;
            case PROJECTILE -> PROJECTILE_PALETTE;
            case FIRE -> FIRE_PALETTE;
            case EXPLOSION -> EXPLOSION_PALETTE;
            case MAGIC -> MAGIC_PALETTE;
        };
    }

    public static StimulusType dominantStimulus(Map<StimulusType, Double> scores) {
        StimulusType dominant = null;
        double highestScore = 0.0;
        for (StimulusType type : StimulusType.values()) {
            double score = scores.getOrDefault(type, 0.0);
            if (Double.isFinite(score) && score > highestScore) {
                dominant = type;
                highestScore = score;
            }
        }
        return dominant;
    }

    public static boolean canStartNextRoutePulse(
            boolean hasActivePylon,
            int nearbyHostiles,
            int pulsesCompleted,
            long gameTime,
            long lastPulseGameTime
    ) {
        return hasActivePylon
                && Math.max(0, nearbyHostiles) == 0
                && pulsesCompleted < REQUIRED_ROUTE_PULSES
                && (pulsesCompleted == 0 || gameTime - lastPulseGameTime >= ROUTE_PULSE_COOLDOWN_TICKS);
    }

    public static boolean routeIsComplete(int pulsesCompleted, int nearbyHostiles) {
        return pulsesCompleted >= REQUIRED_ROUTE_PULSES && Math.max(0, nearbyHostiles) == 0;
    }

    public static boolean gardenIsComplete(int linkedFocusCount) {
        return linkedFocusCount >= REQUIRED_GARDEN_FOCI;
    }
}
