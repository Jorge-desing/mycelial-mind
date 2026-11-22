package com.alex.mycelialmind.network;

import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.hive.HiveStrain;

/**
 * Client-side cache for the local player's Hive Strain Diplomacy state.
 */
public final class ClientDiplomacyCache {
    private static int amberRep = 0;
    private static int crimsonRep = 0;
    private static int truceTicks = 0;
    private static int betrayalTicks = 0;
    private static HiveStrain selectedStrain = HiveStrain.AMBER;

    private ClientDiplomacyCache() {
    }

    public static void clear() {
        amberRep = 0;
        crimsonRep = 0;
        truceTicks = 0;
        betrayalTicks = 0;
        selectedStrain = HiveStrain.AMBER;
    }

    public static void update(DiplomacySyncPayload payload) {
        amberRep = payload.amberRep();
        crimsonRep = payload.crimsonRep();
        truceTicks = payload.truceTicks();
        betrayalTicks = payload.betrayalTicks();
        HiveStrain s = HiveStrain.byId(payload.selectedStrainId());
        selectedStrain = s.isSchismStrain() ? s : HiveStrain.AMBER;
    }

    public static int getAmberRep() {
        return amberRep;
    }

    public static int getCrimsonRep() {
        return crimsonRep;
    }

    public static int getTruceSeconds() {
        return (truceTicks + 19) / 20;
    }

    public static int getBetrayalSeconds() {
        return (betrayalTicks + 19) / 20;
    }

    public static HiveStrain getSelectedStrain() {
        return selectedStrain;
    }

    public static PlayerDiplomacyData.Standing getStanding(HiveStrain strain) {
        if (strain == HiveStrain.ORTHODOX) {
            return PlayerDiplomacyData.Standing.HOSTILE;
        }
        int rep = strain == HiveStrain.AMBER ? amberRep : crimsonRep;
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
