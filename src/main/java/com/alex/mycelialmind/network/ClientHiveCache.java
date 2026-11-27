package com.alex.mycelialmind.network;

import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.HiveSynergy;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.StimulusType;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine.HomeostasisStage;
import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Client-side cache of the latest synchronized Hive Mind state, stimulus scores,
 * Tier 2 mutations, active synergies, zone protection, network links, nearest Node,
 * and nearby territory markers for the Bio-Scanner Codex radar.
 */
public final class ClientHiveCache {
    private static double threatLevel = 0.0;
    private static Set<AdaptationTrait> activeTraits = EnumSet.noneOf(AdaptationTrait.class);
    private static Set<AdaptationTrait> tier2Traits = EnumSet.noneOf(AdaptationTrait.class);
    private static Set<HiveSynergy> activeSynergies = EnumSet.noneOf(HiveSynergy.class);
    private static final EnumMap<StimulusType, Double> stimulusScores = new EnumMap<>(StimulusType.class);
    private static boolean inProtectedZone = false;
    @Nullable
    private static BlockPos nearestNodePos = null;
    private static int activeNetworkLinks = 0;
    private static int suppressionSecondsRemaining = 0;
    private static int totalNodes = 0;
    private static int totalPylons = 0;
    private static int pylonRadius = 24;
    private static int linkRadius = 28;
    private static BlockPos playerAnchorPos = BlockPos.ZERO;
    private static List<HiveSyncPayload.NodeMarker> nearbyNodes = List.of();
    private static List<BlockPos> nearbyPylons = List.of();
    private static int fungalTideSecondsRemaining = 0;
    private static final EnumMap<PlayerHabit, Double> habitScores = new EnumMap<>(PlayerHabit.class);
    private static int falseStimulusOrdinal = -1;
    private static int deceptionSecondsRemaining = 0;
    private static int deceptionCooldownSecondsRemaining = 0;
    private static double nearestVolatileBuffer = 0.0;
    private static int squadTacticalStatus = 0;
    private static int squadStatusSecondsRemaining = 0;
    private static double homeostasisStress = 0.0;
    private static int homeostasisStage = 0;
    private static boolean acidRainExposed = false;
    private static int smotheredPylonCount = 0;
    private static double traitActivationThreshold = 70.0;
    private static double tier2ActivationThreshold = 200.0;
    private static int homeostasisSensitization = 0;
    private static Set<BlockPos> smotheredPylons = Set.of();
    private static List<HiveSyncPayload.InflamedChunkMarker> inflamedChunks = List.of();
    private static boolean deepMyceliumBreached = false;
    private static boolean matriarchDefeated = false;
    private static int matriarchRemissionSecondsRemaining = 0;
    private static final CopyOnWriteArrayList<Runnable> UPDATE_LISTENERS = new CopyOnWriteArrayList<>();

    static {
        for (StimulusType type : StimulusType.values()) {
            stimulusScores.put(type, 0.0);
        }
        for (PlayerHabit habit : PlayerHabit.values()) {
            habitScores.put(habit, 0.0);
        }
    }

    private ClientHiveCache() {
    }

    public static void clear() {
        update(0.0, Set.of(), Set.of(), Set.of(), false, null, 0, 0,
                new double[StimulusType.values().length], 0, 0, 24, 28, BlockPos.ZERO,
                List.of(), List.of(), 0, new double[PlayerHabit.values().length],
                -1, 0, 0, 0.0, 0, 0, 0.0, 0, false, 0,
                70.0, 200.0, 0, List.of(), List.of(), false, false, 0);
    }

    public static void update(
            double threat,
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies,
            boolean protectedZone,
            @Nullable BlockPos nearestNode,
            int networkLinks,
            int suppressionSeconds,
            double[] scores
    ) {
        update(threat, traits, tier2, synergies, protectedZone, nearestNode, networkLinks, suppressionSeconds,
                scores, 0, 0, 24, 28, BlockPos.ZERO, List.of(), List.of(), 0);
    }

    public static void update(
            double threat,
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies,
            boolean protectedZone,
            @Nullable BlockPos nearestNode,
            int networkLinks,
            int suppressionSeconds,
            double[] scores,
            int nodesTotal,
            int pylonsTotal,
            int pylonRad,
            int linkRad,
            BlockPos anchorPos,
            List<HiveSyncPayload.NodeMarker> nodes,
            List<BlockPos> pylons
    ) {
        update(threat, traits, tier2, synergies, protectedZone, nearestNode, networkLinks, suppressionSeconds,
                scores, nodesTotal, pylonsTotal, pylonRad, linkRad, anchorPos, nodes, pylons, 0);
    }

    public static void update(
            double threat,
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies,
            boolean protectedZone,
            @Nullable BlockPos nearestNode,
            int networkLinks,
            int suppressionSeconds,
            double[] scores,
            int nodesTotal,
            int pylonsTotal,
            int pylonRad,
            int linkRad,
            BlockPos anchorPos,
            List<HiveSyncPayload.NodeMarker> nodes,
            List<BlockPos> pylons,
            int tideSeconds
    ) {
        update(threat, traits, tier2, synergies, protectedZone, nearestNode, networkLinks, suppressionSeconds,
                scores, nodesTotal, pylonsTotal, pylonRad, linkRad, anchorPos, nodes, pylons, tideSeconds,
                new double[4], -1, 0, 0, 0.0, 0, 0);
    }

    public static void update(
            double threat,
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies,
            boolean protectedZone,
            @Nullable BlockPos nearestNode,
            int networkLinks,
            int suppressionSeconds,
            double[] scores,
            int nodesTotal,
            int pylonsTotal,
            int pylonRad,
            int linkRad,
            BlockPos anchorPos,
            List<HiveSyncPayload.NodeMarker> nodes,
            List<BlockPos> pylons,
            int tideSeconds,
            double[] habits,
            int falseOrd,
            int decSec,
            int decCdSec,
            double nearestVol
    ) {
        update(threat, traits, tier2, synergies, protectedZone, nearestNode, networkLinks, suppressionSeconds,
                scores, nodesTotal, pylonsTotal, pylonRad, linkRad, anchorPos, nodes, pylons, tideSeconds,
                habits, falseOrd, decSec, decCdSec, nearestVol, 0, 0);
    }

    public static void update(
            double threat,
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies,
            boolean protectedZone,
            @Nullable BlockPos nearestNode,
            int networkLinks,
            int suppressionSeconds,
            double[] scores,
            int nodesTotal,
            int pylonsTotal,
            int pylonRad,
            int linkRad,
            BlockPos anchorPos,
            List<HiveSyncPayload.NodeMarker> nodes,
            List<BlockPos> pylons,
            int tideSeconds,
            double[] habits,
            int falseOrd,
            int decSec,
            int decCdSec,
            double nearestVol,
            int squadStatus,
            int squadSec
    ) {
        update(threat, traits, tier2, synergies, protectedZone, nearestNode, networkLinks, suppressionSeconds,
                scores, nodesTotal, pylonsTotal, pylonRad, linkRad, anchorPos, nodes, pylons, tideSeconds,
                habits, falseOrd, decSec, decCdSec, nearestVol, squadStatus, squadSec, 0.0, 0, false, 0);
    }

    public static void update(
            double threat,
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies,
            boolean protectedZone,
            @Nullable BlockPos nearestNode,
            int networkLinks,
            int suppressionSeconds,
            double[] scores,
            int nodesTotal,
            int pylonsTotal,
            int pylonRad,
            int linkRad,
            BlockPos anchorPos,
            List<HiveSyncPayload.NodeMarker> nodes,
            List<BlockPos> pylons,
            int tideSeconds,
            double[] habits,
            int falseOrd,
            int decSec,
            int decCdSec,
            double nearestVol,
            int squadStatus,
            int squadSec,
            double homeoStress,
            int homeoStage,
            boolean acidExposed,
            int smotheredPylons
    ) {
        update(threat, traits, tier2, synergies, protectedZone, nearestNode, networkLinks, suppressionSeconds,
                scores, nodesTotal, pylonsTotal, pylonRad, linkRad, anchorPos, nodes, pylons, tideSeconds,
                habits, falseOrd, decSec, decCdSec, nearestVol, squadStatus, squadSec,
                homeoStress, homeoStage, acidExposed, smotheredPylons, 70.0, 200.0);
    }

    public static void update(
            double threat,
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies,
            boolean protectedZone,
            @Nullable BlockPos nearestNode,
            int networkLinks,
            int suppressionSeconds,
            double[] scores,
            int nodesTotal,
            int pylonsTotal,
            int pylonRad,
            int linkRad,
            BlockPos anchorPos,
            List<HiveSyncPayload.NodeMarker> nodes,
            List<BlockPos> pylons,
            int tideSeconds,
            double[] habits,
            int falseOrd,
            int decSec,
            int decCdSec,
            double nearestVol,
            int squadStatus,
            int squadSec,
            double homeoStress,
            int homeoStage,
            boolean acidExposed,
            int smotheredPylonsCount,
            double traitThreshold,
            double tier2Threshold
    ) {
        update(threat, traits, tier2, synergies, protectedZone, nearestNode, networkLinks, suppressionSeconds,
                scores, nodesTotal, pylonsTotal, pylonRad, linkRad, anchorPos, nodes, pylons, tideSeconds,
                habits, falseOrd, decSec, decCdSec, nearestVol, squadStatus, squadSec,
                homeoStress, homeoStage, acidExposed, smotheredPylonsCount, traitThreshold, tier2Threshold,
                0, List.of(), List.of());
    }

    public static void update(
            double threat,
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies,
            boolean protectedZone,
            @Nullable BlockPos nearestNode,
            int networkLinks,
            int suppressionSeconds,
            double[] scores,
            int nodesTotal,
            int pylonsTotal,
            int pylonRad,
            int linkRad,
            BlockPos anchorPos,
            List<HiveSyncPayload.NodeMarker> nodes,
            List<BlockPos> pylons,
            int tideSeconds,
            double[] habits,
            int falseOrd,
            int decSec,
            int decCdSec,
            double nearestVol,
            int squadStatus,
            int squadSec,
            double homeoStress,
            int homeoStage,
            boolean acidExposed,
            int smotheredPylonsCount,
            double traitThreshold,
            double tier2Threshold,
            int homeoSensitization,
            List<BlockPos> smotheredPylonList,
            List<HiveSyncPayload.InflamedChunkMarker> inflamedChunkList
    ) {
        update(threat, traits, tier2, synergies, protectedZone, nearestNode, networkLinks, suppressionSeconds,
                scores, nodesTotal, pylonsTotal, pylonRad, linkRad, anchorPos, nodes, pylons, tideSeconds,
                habits, falseOrd, decSec, decCdSec, nearestVol, squadStatus, squadSec,
                homeoStress, homeoStage, acidExposed, smotheredPylonsCount, traitThreshold, tier2Threshold,
                homeoSensitization, smotheredPylonList, inflamedChunkList, false, false, 0);
    }

    public static void update(
            double threat,
            Set<AdaptationTrait> traits,
            Set<AdaptationTrait> tier2,
            Set<HiveSynergy> synergies,
            boolean protectedZone,
            @Nullable BlockPos nearestNode,
            int networkLinks,
            int suppressionSeconds,
            double[] scores,
            int nodesTotal,
            int pylonsTotal,
            int pylonRad,
            int linkRad,
            BlockPos anchorPos,
            List<HiveSyncPayload.NodeMarker> nodes,
            List<BlockPos> pylons,
            int tideSeconds,
            double[] habits,
            int falseOrd,
            int decSec,
            int decCdSec,
            double nearestVol,
            int squadStatus,
            int squadSec,
            double homeoStress,
            int homeoStage,
            boolean acidExposed,
            int smotheredPylonsCount,
            double traitThreshold,
            double tier2Threshold,
            int homeoSensitization,
            List<BlockPos> smotheredPylonList,
            List<HiveSyncPayload.InflamedChunkMarker> inflamedChunkList,
            boolean breached,
            boolean defeated,
            int remissionSeconds
    ) {
        threatLevel = threat;
        activeTraits = traits.isEmpty() ? EnumSet.noneOf(AdaptationTrait.class) : EnumSet.copyOf(traits);
        tier2Traits = tier2.isEmpty() ? EnumSet.noneOf(AdaptationTrait.class) : EnumSet.copyOf(tier2);
        activeSynergies = synergies.isEmpty() ? EnumSet.noneOf(HiveSynergy.class) : EnumSet.copyOf(synergies);
        inProtectedZone = protectedZone;
        nearestNodePos = nearestNode;
        activeNetworkLinks = networkLinks;
        suppressionSecondsRemaining = suppressionSeconds;
        StimulusType[] types = StimulusType.values();
        for (int i = 0; i < types.length && i < scores.length; i++) {
            stimulusScores.put(types[i], scores[i]);
        }
        totalNodes = nodesTotal;
        totalPylons = pylonsTotal;
        pylonRadius = pylonRad;
        linkRadius = linkRad;
        playerAnchorPos = anchorPos;
        nearbyNodes = List.copyOf(nodes);
        nearbyPylons = List.copyOf(pylons);
        fungalTideSecondsRemaining = tideSeconds;
        PlayerHabit[] habitVals = PlayerHabit.values();
        for (int i = 0; i < habitVals.length && i < habits.length; i++) {
            habitScores.put(habitVals[i], habits[i]);
        }
        falseStimulusOrdinal = falseOrd;
        deceptionSecondsRemaining = decSec;
        deceptionCooldownSecondsRemaining = decCdSec;
        nearestVolatileBuffer = nearestVol;
        squadTacticalStatus = squadStatus;
        squadStatusSecondsRemaining = squadSec;
        homeostasisStress = homeoStress;
        homeostasisStage = homeoStage;
        acidRainExposed = acidExposed;
        smotheredPylonCount = smotheredPylonsCount;
        double safeN1 = Double.isFinite(traitThreshold) ? Math.max(1.0, traitThreshold) : 70.0;
        double safeN2 = Double.isFinite(tier2Threshold) ? Math.max(safeN1 + 1.0, tier2Threshold) : Math.max(safeN1 + 1.0, 200.0);
        traitActivationThreshold = safeN1;
        tier2ActivationThreshold = safeN2;
        homeostasisSensitization = Math.max(0, Math.min(3, homeoSensitization));
        smotheredPylons = smotheredPylonList == null || smotheredPylonList.isEmpty()
                ? Set.of()
                : Set.copyOf(new HashSet<>(smotheredPylonList));
        inflamedChunks = inflamedChunkList == null ? List.of() : List.copyOf(inflamedChunkList);
        deepMyceliumBreached = breached;
        matriarchDefeated = defeated;
        matriarchRemissionSecondsRemaining = Math.max(0, remissionSeconds);
        notifyUpdateListeners();
    }

    public static void addUpdateListener(Runnable listener) {
        if (listener != null && !UPDATE_LISTENERS.contains(listener)) {
            UPDATE_LISTENERS.add(listener);
        }
    }

    public static void removeUpdateListener(Runnable listener) {
        if (listener != null) {
            UPDATE_LISTENERS.remove(listener);
        }
    }

    private static void notifyUpdateListeners() {
        for (Runnable listener : UPDATE_LISTENERS) {
            try {
                listener.run();
            } catch (RuntimeException ignored) {
                // Keep cache updates resilient against optional integration listener failures.
            }
        }
    }

    public static double getThreatLevel() {
        return threatLevel;
    }

    public static Set<AdaptationTrait> getActiveTraits() {
        return Collections.unmodifiableSet(activeTraits);
    }

    public static Set<AdaptationTrait> getTier2Traits() {
        return Collections.unmodifiableSet(tier2Traits);
    }

    public static Set<HiveSynergy> getActiveSynergies() {
        return Collections.unmodifiableSet(activeSynergies);
    }

    public static double getStimulusScore(StimulusType type) {
        return stimulusScores.getOrDefault(type, 0.0);
    }

    public static Map<StimulusType, Double> getStimulusScores() {
        return Collections.unmodifiableMap(stimulusScores);
    }

    public static boolean isInProtectedZone() {
        return inProtectedZone;
    }

    @Nullable
    public static BlockPos getNearestNodePos() {
        return nearestNodePos;
    }

    public static int getActiveNetworkLinks() {
        return activeNetworkLinks;
    }

    public static int getSuppressionSecondsRemaining() {
        return suppressionSecondsRemaining;
    }

    public static int getTotalNodes() {
        return totalNodes;
    }

    public static int getTotalPylons() {
        return totalPylons;
    }

    public static int getPylonRadius() {
        return pylonRadius;
    }

    public static int getLinkRadius() {
        return linkRadius;
    }

    public static BlockPos getPlayerAnchorPos() {
        return playerAnchorPos;
    }

    public static List<HiveSyncPayload.NodeMarker> getNearbyNodes() {
        return nearbyNodes;
    }

    public static List<BlockPos> getNearbyPylons() {
        return nearbyPylons;
    }

    public static int getFungalTideSecondsRemaining() {
        return fungalTideSecondsRemaining;
    }

    public static double getHabitScore(PlayerHabit habit) {
        return habitScores.getOrDefault(habit, 0.0);
    }

    @Nullable
    public static StimulusType getFalseStimulus() {
        StimulusType[] vals = StimulusType.values();
        return (falseStimulusOrdinal >= 0 && falseStimulusOrdinal < vals.length) ? vals[falseStimulusOrdinal] : null;
    }

    public static int getDeceptionSecondsRemaining() {
        return deceptionSecondsRemaining;
    }

    public static int getDeceptionCooldownSecondsRemaining() {
        return deceptionCooldownSecondsRemaining;
    }

    public static double getNearestVolatileBuffer() {
        return nearestVolatileBuffer;
    }

    public static SynapticCommandSystem.SquadTacticalStatus getSquadTacticalStatus() {
        return SynapticCommandSystem.SquadTacticalStatus.byId(squadTacticalStatus);
    }

    public static int getSquadStatusSecondsRemaining() {
        return squadStatusSecondsRemaining;
    }

    public static double getHomeostasisStress() {
        return homeostasisStress;
    }

    public static HomeostasisStage getHomeostasisStage() {
        return HomeostasisStage.byId(homeostasisStage);
    }

    public static boolean isAcidRainExposed() {
        return acidRainExposed;
    }

    public static int getSmotheredPylonCount() {
        return smotheredPylonCount;
    }

    public static double getTraitActivationThreshold() {
        return traitActivationThreshold;
    }

    public static double getTier2ActivationThreshold() {
        return tier2ActivationThreshold;
    }

    public static int getHomeostasisSensitization() {
        return homeostasisSensitization;
    }

    public static Set<BlockPos> getSmotheredPylons() {
        return smotheredPylons;
    }

    public static boolean isPylonSmothered(BlockPos pos) {
        return pos != null && smotheredPylons.contains(pos);
    }

    public static List<HiveSyncPayload.InflamedChunkMarker> getInflamedChunks() {
        return inflamedChunks;
    }

    public static boolean isDeepMyceliumBreached() {
        return deepMyceliumBreached;
    }

    public static boolean isMatriarchDefeated() {
        return matriarchDefeated;
    }

    public static int getMatriarchRemissionSecondsRemaining() {
        return matriarchRemissionSecondsRemaining;
    }
}
