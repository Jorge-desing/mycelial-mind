package com.alex.mycelialmind.network;

import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;

/**
 * Client-side cache of the latest synchronized Hive Mind state, progression Act tier,
 * Genetic Bestiary, experimental secrets, and Perfect Bastion defense rating.
 */
public final class ClientDiscoveryCache {
    private static int scannedSpeciesMask = 0;
    private static int discoveredTraitsMask = 0;
    private static int discoveredStrainsMask = 0;
    private static int discoveredSecretsMask = 0;
    private static int craftedMilestonesMask = 0;
    private static int rareVariantsFound = 0;
    private static int bastionScore = 0;
    private static int highestActTier = 0;

    private ClientDiscoveryCache() {
    }

    public static void clear() {
        scannedSpeciesMask = 0;
        discoveredTraitsMask = 0;
        discoveredStrainsMask = 0;
        discoveredSecretsMask = 0;
        craftedMilestonesMask = 0;
        rareVariantsFound = 0;
        bastionScore = 0;
        highestActTier = 0;
    }

    public static void update(DiscoverySyncPayload payload) {
        scannedSpeciesMask = payload.scannedSpeciesMask();
        discoveredTraitsMask = payload.discoveredTraitsMask();
        discoveredStrainsMask = payload.discoveredStrainsMask();
        discoveredSecretsMask = payload.discoveredSecretsMask();
        craftedMilestonesMask = payload.craftedMilestonesMask();
        rareVariantsFound = Math.max(0, payload.rareVariantsFound());
        bastionScore = Math.max(0, Math.min(100, payload.bastionScore()));
        highestActTier = Math.max(0, Math.min(HiveProgressionTier.OVERMIND.level(), payload.highestActTier()));
    }

    public static int getHighestActTier() {
        return highestActTier;
    }

    public static HiveProgressionTier getEffectiveActTier() {
        return HiveProgressionTier.resolveEffectiveTier(ClientHiveCache.getThreatLevel(), highestActTier);
    }

    public static int getScannedSpeciesMask() {
        return scannedSpeciesMask;
    }

    public static int getDiscoveredTraitsMask() {
        return discoveredTraitsMask;
    }

    public static int getDiscoveredStrainsMask() {
        return discoveredStrainsMask;
    }

    public static int getDiscoveredSecretsMask() {
        return discoveredSecretsMask;
    }

    public static int getCraftedMilestonesMask() {
        return craftedMilestonesMask;
    }

    public static int getRareVariantsFound() {
        return rareVariantsFound;
    }

    public static int getBastionScore() {
        return bastionScore;
    }

    public static int countScannedSpecies() {
        return Integer.bitCount(scannedSpeciesMask & ((1 << ProgressionGoalEngine.TOTAL_SPECIES) - 1));
    }

    public static int countDiscoveredTraits() {
        return Integer.bitCount(discoveredTraitsMask & 0x1F);
    }

    public static int countDiscoveredSecrets() {
        return Integer.bitCount(discoveredSecretsMask & 0xFF);
    }

    public static boolean hasSecret(int secretBit) {
        return secretBit >= 0 && secretBit < ProgressionGoalEngine.TOTAL_SECRETS
                && (discoveredSecretsMask & (1 << secretBit)) != 0;
    }

    public static ProgressionGoalEngine.ShortTermGoal getActiveShortTermGoal() {
        return ProgressionGoalEngine.resolveShortTermGoal(
                craftedMilestonesMask, discoveredTraitsMask, discoveredSecretsMask);
    }

    public static int getLongTermMasteryPercent() {
        return ProgressionGoalEngine.computeLongTermMasteryPercent(
                countScannedSpecies(), countDiscoveredTraits(), countDiscoveredSecrets(), bastionScore);
    }
}
