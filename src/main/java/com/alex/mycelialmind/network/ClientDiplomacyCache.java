package com.alex.mycelialmind.network;

import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.hive.HiveStrain;

/**
 * Client-side cache for the local player's Hive Strain Diplomacy state.
 */
public final class ClientDiplomacyCache {
    private record Snapshot(
            int amberRep,
            int crimsonRep,
            int truceTicks,
            int betrayalTicks,
            HiveStrain selectedStrain
    ) {
    }

    private static final Snapshot EMPTY = new Snapshot(0, 0, 0, 0, HiveStrain.AMBER);
    private static Snapshot state = EMPTY;

    private ClientDiplomacyCache() {
    }

    public static void clear() {
        state = EMPTY;
    }

    public static void update(DiplomacySyncPayload payload) {
        HiveStrain strain = HiveStrain.byId(payload.selectedStrainId());
        state = new Snapshot(
                payload.amberRep(),
                payload.crimsonRep(),
                payload.truceTicks(),
                payload.betrayalTicks(),
                strain.isSchismStrain() ? strain : HiveStrain.AMBER
        );
    }

    public static int getAmberRep() {
        return state.amberRep();
    }

    public static int getCrimsonRep() {
        return state.crimsonRep();
    }

    public static int getTruceSeconds() {
        return (state.truceTicks() + 19) / 20;
    }

    public static int getBetrayalSeconds() {
        return (state.betrayalTicks() + 19) / 20;
    }

    public static HiveStrain getSelectedStrain() {
        return state.selectedStrain();
    }

    public static PlayerDiplomacyData.Standing getStanding(HiveStrain strain) {
        if (strain == HiveStrain.ORTHODOX) {
            return PlayerDiplomacyData.Standing.HOSTILE;
        }
        int rep = strain == HiveStrain.AMBER ? state.amberRep() : state.crimsonRep();
        if (rep >= PlayerDiplomacyData.ALLIED_THRESHOLD) {
            return PlayerDiplomacyData.Standing.ALLIED;
        }
        if (rep >= PlayerDiplomacyData.PACT_THRESHOLD) {
            return PlayerDiplomacyData.Standing.PACT;
        }
        if (rep <= PlayerDiplomacyData.HOSTILE_THRESHOLD) {
            return PlayerDiplomacyData.Standing.HOSTILE;
        }
        return PlayerDiplomacyData.Standing.NEUTRAL;
    }
}
