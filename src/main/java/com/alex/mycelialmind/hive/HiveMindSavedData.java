package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.config.MycelialConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Persistent per-dimension Hive Mind memory tracking player combat stimuli,
 * threat escalation, and active adaptive traits.
 */
public class HiveMindSavedData extends SavedData {
    private static final String DATA_NAME = "mycelialmind_hive_memory";
    private static final String TAG_THREAT = "ThreatLevel";
    private static final String TAG_STIMULI = "Stimuli";
    private static final String TAG_LAST_MILESTONE = "LastWaveMilestone";
    private static final String TAG_LAST_WAVE_TICK = "LastWaveTick";
    private static final String TAG_SUPPRESSED_UNTIL = "SuppressedUntilTick";
    private static final String TAG_FUNGAL_TIDE_UNTIL = "FungalTideUntilTick";
    private static final String TAG_LAST_FUNGAL_TIDE = "LastFungalTideTick";
    private static final String TAG_HOTSPOTS = "Hotspots";
    private static final String TAG_CASUALTIES = "Casualties";
    private static final String TAG_MASTERY = "TraitMastery";
    private static final String TAG_LAST_STIMULUS = "LastStimulus";
    private static final String TAG_STREAK = "StimulusStreak";
    private static final String TAG_LAST_STIMULUS_TICK = "LastStimulusTick";
    private static final String TAG_HABITS = "HabitScores";
    private static final String TAG_FALSE_STIMULUS = "FalseStimulus";
    private static final String TAG_DECEPTION_UNTIL = "DeceptionUntilTick";
    private static final String TAG_DECEPTION_CD_UNTIL = "DeceptionCooldownUntilTick";
    private static final String TAG_DECEPTION_BOOST = "DeceptionAppliedBoost";
    private static final String TAG_HIGHEST_TIER = "HighestTierReached";
    private static final String TAG_DEEP_BREACHED = "DeepMyceliumBreached";
    private static final String TAG_MATRIARCH_DEFEATED = "MatriarchDefeated";
    private static final String TAG_MATRIARCH_REMISSION_UNTIL = "MatriarchRemissionUntilTick";
    private static final String TAG_ACTIVE_TRAIT_REVISION = "ActiveTraitStateRevision";
    private static final String TAG_ACTIVE_TRAIT_MASK = "ActiveTraitMask";
    private static final String TAG_TIER2_TRAIT_MASK = "Tier2TraitMask";
    private static final String TAG_ACTIVE_SYNERGY_MASK = "ActiveSynergyMask";
    private static final String TAG_HYPHAL_CHUNKS = "HyphalChunks";
    private static final String TAG_ARCHITECTURAL_ACCENT = "ArchitecturalAccent";
    private static final String TAG_BLIND_SPOT_SITES = "BlindSpotSites";
    private static final String TAG_BASE_ANCHORS = "BaseAnchors";
    private static final int MAX_BASE_ANCHORS = 16;

    public record HyphalChunkSnapshot(
            int chunkX,
            int chunkZ,
            double density,
            int traversalCount,
            int veinCount
    ) {
    }

    public static final class HyphalChunkData {
        private double density;
        private int traversalCount;
        private long lastVisitTick;
        private long lastAmbushTick = -1L;
        private final LinkedHashSet<BlockPos> routeWaypoints = new LinkedHashSet<>();
        private final LinkedHashSet<BlockPos> activeVeins = new LinkedHashSet<>();
        private final List<BlockPos> orderedTrail = new ArrayList<>();
        private final Map<Long, Integer> blockTraversals = new LinkedHashMap<>();

        public double getDensity() {
            return density;
        }

        public int getTraversalCount() {
            return traversalCount;
        }

        public long getLastVisitTick() {
            return lastVisitTick;
        }

        public long getLastAmbushTick() {
            return lastAmbushTick;
        }

        public Set<BlockPos> getRouteWaypoints() {
            return Collections.unmodifiableSet(routeWaypoints);
        }

        public Set<BlockPos> getActiveVeins() {
            return Collections.unmodifiableSet(activeVeins);
        }

        public List<BlockPos> getOrderedTrail() {
            return Collections.unmodifiableList(orderedTrail);
        }

        public int getBlockTraversals(BlockPos pos) {
            return blockTraversals.getOrDefault(pos.asLong(), 0);
        }

        public Map<Long, Integer> getBlockTraversalMap() {
            return Collections.unmodifiableMap(blockTraversals);
        }

        void recordTrailPoint(BlockPos pos, boolean countTraversal) {
            BlockPos immutable = pos.immutable();
            int maxTrail = HyphalPhysics.getMaxTrailPointsPerChunk();
            if (orderedTrail.isEmpty() || !orderedTrail.get(orderedTrail.size() - 1).equals(immutable)) {
                while (orderedTrail.size() >= maxTrail) {
                    orderedTrail.remove(0);
                }
                orderedTrail.add(immutable);
            }
            if (countTraversal) {
                long packed = immutable.asLong();
                if (!blockTraversals.containsKey(packed)
                        && blockTraversals.size() >= HyphalPhysics.MAX_TRAVERSAL_ENTRIES_PER_CHUNK) {
                    Long lowestKey = null;
                    int lowestCount = Integer.MAX_VALUE;
                    for (Map.Entry<Long, Integer> entry : blockTraversals.entrySet()) {
                        if (entry.getValue() < lowestCount) {
                            lowestCount = entry.getValue();
                            lowestKey = entry.getKey();
                        }
                    }
                    if (lowestKey != null) {
                        blockTraversals.remove(lowestKey);
                    }
                }
                int next = Math.min(10_000, blockTraversals.getOrDefault(packed, 0) + 1);
                blockTraversals.put(packed, next);
            }
        }

        void addWaypoint(BlockPos pos) {
            BlockPos immutable = pos.immutable();
            routeWaypoints.remove(immutable);
            while (routeWaypoints.size() >= HyphalAdaptiveArchitectureEngine.MAX_WAYPOINTS_PER_CHUNK) {
                Iterator<BlockPos> it = routeWaypoints.iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                } else {
                    break;
                }
            }
            routeWaypoints.add(immutable);
        }

        boolean addVein(BlockPos pos) {
            BlockPos immutable = pos.immutable();
            if (activeVeins.contains(immutable)) {
                return false;
            }
            while (activeVeins.size() >= HyphalAdaptiveArchitectureEngine.MAX_VEINS_PER_CHUNK) {
                Iterator<BlockPos> it = activeVeins.iterator();
                if (it.hasNext()) {
                    it.next();
                    it.remove();
                } else {
                    break;
                }
            }
            return activeVeins.add(immutable);
        }

        boolean removeVein(BlockPos pos) {
            return activeVeins.remove(pos);
        }

        boolean isEmpty() {
            return density <= 0.0
                    && activeVeins.isEmpty()
                    && traversalCount <= 0
                    && orderedTrail.isEmpty()
                    && blockTraversals.isEmpty();
        }

        CompoundTag toTag(long chunkKey) {
            CompoundTag ct = new CompoundTag();
            ct.putLong("Key", chunkKey);
            ct.putDouble("Density", density);
            ct.putInt("Traversals", traversalCount);
            ct.putLong("LastVisit", lastVisitTick);
            ct.putLong("LastAmbush", lastAmbushTick);
            if (!routeWaypoints.isEmpty()) {
                ListTag wpList = new ListTag();
                for (BlockPos wp : routeWaypoints) {
                    wpList.add(LongTag.valueOf(wp.asLong()));
                }
                ct.put("Waypoints", wpList);
            }
            if (!activeVeins.isEmpty()) {
                ListTag veinList = new ListTag();
                for (BlockPos v : activeVeins) {
                    veinList.add(LongTag.valueOf(v.asLong()));
                }
                ct.put("Veins", veinList);
            }
            if (!orderedTrail.isEmpty()) {
                ListTag trailList = new ListTag();
                for (BlockPos tp : orderedTrail) {
                    trailList.add(LongTag.valueOf(tp.asLong()));
                }
                ct.put("TrailPoints", trailList);
            }
            if (!blockTraversals.isEmpty()) {
                ListTag travList = new ListTag();
                for (Map.Entry<Long, Integer> entry : blockTraversals.entrySet()) {
                    CompoundTag row = new CompoundTag();
                    row.putLong("Pos", entry.getKey());
                    row.putInt("Count", entry.getValue());
                    travList.add(row);
                }
                ct.put("BlockTraversals", travList);
            }
            return ct;
        }

        static HyphalChunkData fromTag(CompoundTag ct) {
            HyphalChunkData data = new HyphalChunkData();
            data.density = Mth.clamp(ct.getDouble("Density"), 0.0, HyphalAdaptiveArchitectureEngine.MAX_NETWORK_DENSITY);
            data.traversalCount = Math.max(0, ct.getInt("Traversals"));
            data.lastVisitTick = Math.max(0L, ct.getLong("LastVisit"));
            data.lastAmbushTick = ct.contains("LastAmbush") ? ct.getLong("LastAmbush") : -1L;
            if (ct.contains("Waypoints", Tag.TAG_LIST)) {
                ListTag wpList = ct.getList("Waypoints", Tag.TAG_LONG);
                for (int i = 0; i < wpList.size() && i < HyphalAdaptiveArchitectureEngine.MAX_WAYPOINTS_PER_CHUNK; i++) {
                    data.routeWaypoints.add(BlockPos.of(((LongTag) wpList.get(i)).getAsLong()));
                }
            }
            if (ct.contains("Veins", Tag.TAG_LIST)) {
                ListTag veinList = ct.getList("Veins", Tag.TAG_LONG);
                for (int i = 0; i < veinList.size() && i < HyphalAdaptiveArchitectureEngine.MAX_VEINS_PER_CHUNK; i++) {
                    data.activeVeins.add(BlockPos.of(((LongTag) veinList.get(i)).getAsLong()));
                }
            }
            if (ct.contains("TrailPoints", Tag.TAG_LIST)) {
                ListTag trailList = ct.getList("TrailPoints", Tag.TAG_LONG);
                int maxTrail = HyphalPhysics.getMaxTrailPointsPerChunk();
                int start = Math.max(0, trailList.size() - maxTrail);
                for (int i = start; i < trailList.size(); i++) {
                    data.orderedTrail.add(BlockPos.of(((LongTag) trailList.get(i)).getAsLong()));
                }
            }
            if (ct.contains("BlockTraversals", Tag.TAG_LIST)) {
                ListTag travList = ct.getList("BlockTraversals", Tag.TAG_COMPOUND);
                for (int i = 0; i < travList.size() && i < HyphalPhysics.MAX_TRAVERSAL_ENTRIES_PER_CHUNK; i++) {
                    CompoundTag row = travList.getCompound(i);
                    int count = Math.max(0, row.getInt("Count"));
                    if (count > 0) {
                        data.blockTraversals.put(row.getLong("Pos"), count);
                    }
                }
            }
            return data;
        }
    }

    public static final class BlindSpotOutbreakSite {
        private final BlockPos corePos;
        private final String primaryBlockId;
        private final String pillarBlockId;
        private final HyphalAdaptiveArchitectureEngine.ArchitecturalProportion proportion;
        private final double roomSpan;
        private final double heightToWidthRatio;
        private final double lightDensity;
        private int placedSteps;
        private int totalSteps;
        private int photophobicStrikes;
        private long lastStepTick;
        private boolean completed;
        private final LinkedHashSet<BlockPos> placedBlocks = new LinkedHashSet<>();

        public BlindSpotOutbreakSite(
                BlockPos corePos,
                String primaryBlockId,
                String pillarBlockId,
                HyphalAdaptiveArchitectureEngine.ArchitecturalProportion proportion,
                double roomSpan,
                double heightToWidthRatio,
                double lightDensity,
                int totalSteps,
                long createdTick
        ) {
            this.corePos = corePos.immutable();
            this.primaryBlockId = primaryBlockId != null ? primaryBlockId : "";
            this.pillarBlockId = pillarBlockId != null ? pillarBlockId : "";
            this.proportion = proportion != null ? proportion : HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.HALL_GALLERY;
            this.roomSpan = roomSpan;
            this.heightToWidthRatio = heightToWidthRatio;
            this.lightDensity = lightDensity;
            this.totalSteps = Math.max(1, totalSteps);
            this.lastStepTick = createdTick;
        }

        public BlockPos getCorePos() {
            return corePos;
        }

        public String getPrimaryBlockId() {
            return primaryBlockId;
        }

        public String getPillarBlockId() {
            return pillarBlockId;
        }

        public HyphalAdaptiveArchitectureEngine.ArchitecturalProportion getProportion() {
            return proportion;
        }

        public double getRoomSpan() {
            return roomSpan;
        }

        public double getHeightToWidthRatio() {
            return heightToWidthRatio;
        }

        public double getLightDensity() {
            return lightDensity;
        }

        public int getPlacedSteps() {
            return placedSteps;
        }

        public void setPlacedSteps(int placedSteps) {
            this.placedSteps = Math.max(0, placedSteps);
        }

        public int incrementPlacedSteps() {
            return ++this.placedSteps;
        }

        public int getTotalSteps() {
            return totalSteps;
        }

        public void setTotalSteps(int totalSteps) {
            this.totalSteps = Math.max(1, totalSteps);
        }

        public int getPhotophobicStrikes() {
            return photophobicStrikes;
        }

        public int incrementPhotophobicStrikes() {
            return ++this.photophobicStrikes;
        }

        public void resetPhotophobicStrikes() {
            this.photophobicStrikes = 0;
        }

        public long getLastStepTick() {
            return lastStepTick;
        }

        public void setLastStepTick(long lastStepTick) {
            this.lastStepTick = lastStepTick;
        }

        public boolean isCompleted() {
            return completed;
        }

        public void setCompleted(boolean completed) {
            this.completed = completed;
        }

        public Set<BlockPos> getPlacedBlocks() {
            return Collections.unmodifiableSet(placedBlocks);
        }

        public void addPlacedBlock(BlockPos pos) {
            this.placedBlocks.add(pos.immutable());
        }

        CompoundTag toTag() {
            CompoundTag tag = new CompoundTag();
            tag.putLong("CorePos", corePos.asLong());
            tag.putString("PrimaryBlock", primaryBlockId);
            tag.putString("PillarBlock", pillarBlockId);
            tag.putString("Proportion", proportion.id());
            tag.putDouble("RoomSpan", roomSpan);
            tag.putDouble("HWRatio", heightToWidthRatio);
            tag.putDouble("LightDensity", lightDensity);
            tag.putInt("PlacedSteps", placedSteps);
            tag.putInt("TotalSteps", totalSteps);
            tag.putInt("PhotoStrikes", photophobicStrikes);
            tag.putLong("LastStepTick", lastStepTick);
            tag.putBoolean("Completed", completed);
            if (!placedBlocks.isEmpty()) {
                ListTag blocksList = new ListTag();
                for (BlockPos p : placedBlocks) {
                    blocksList.add(LongTag.valueOf(p.asLong()));
                }
                tag.put("PlacedBlocks", blocksList);
            }
            return tag;
        }

        static BlindSpotOutbreakSite fromTag(CompoundTag tag) {
            BlindSpotOutbreakSite site = new BlindSpotOutbreakSite(
                    BlockPos.of(tag.getLong("CorePos")),
                    tag.getString("PrimaryBlock"),
                    tag.getString("PillarBlock"),
                    HyphalAdaptiveArchitectureEngine.ArchitecturalProportion.fromId(tag.getString("Proportion")),
                    tag.contains("RoomSpan") ? tag.getDouble("RoomSpan") : 5.0,
                    tag.contains("HWRatio") ? tag.getDouble("HWRatio") : 1.0,
                    tag.contains("LightDensity") ? tag.getDouble("LightDensity") : 0.5,
                    Math.max(1, tag.getInt("TotalSteps")),
                    tag.getLong("LastStepTick")
            );
            site.placedSteps = Math.max(0, tag.getInt("PlacedSteps"));
            site.photophobicStrikes = Math.max(0, tag.getInt("PhotoStrikes"));
            site.completed = tag.getBoolean("Completed");
            if (tag.contains("PlacedBlocks", Tag.TAG_LIST)) {
                ListTag blocksList = tag.getList("PlacedBlocks", Tag.TAG_LONG);
                for (int i = 0; i < blocksList.size(); i++) {
                    site.placedBlocks.add(BlockPos.of(((LongTag) blocksList.get(i)).getAsLong()));
                }
            }
            return site;
        }
    }

    public static final int MAX_HOTSPOTS = 24;
    public static final double HABIT_ACTIVATION_THRESHOLD = 60.0;
    public static final double HABIT_MAX_SCORE = 500.0;
    public static final int DECEPTION_DURATION_TICKS = 1800; // 90s
    public static final int DECEPTION_COOLDOWN_TICKS = 1200; // 60s on reveal
    public static final int DECEPTION_NATURAL_COOLDOWN_TICKS = 400; // 20s on natural expiry
    public static final double DECEPTION_FALSE_BOOST = 150.0;
    public static final int MATRIARCH_REMISSION_DURATION_TICKS = 24000; // 20 minutes

    private static final SavedData.Factory<HiveMindSavedData> FACTORY = new SavedData.Factory<>(
            HiveMindSavedData::new,
            HiveMindSavedData::load,
            null
    );

    private double threatLevel;
    private int lastWaveMilestone;
    private int highestTierReached;
    private boolean deepMyceliumBreached;
    private boolean matriarchDefeated;
    private long matriarchRemissionUntilGameTime;
    private long lastWaveGameTime;
    private long suppressedUntilGameTime;
    private long fungalTideUntilGameTime;
    private long lastFungalTideGameTime;
    private StimulusType lastStimulusType;
    private int stimulusStreak;
    private long lastStimulusGameTime;
    @Nullable
    private StimulusType falseStimulus;
    private long deceptionUntilGameTime;
    private long deceptionCooldownUntilGameTime;
    private double deceptionAppliedBoost;
    private final EnumMap<StimulusType, Double> stimulusScores = new EnumMap<>(StimulusType.class);
    private final EnumMap<PlayerHabit, Double> habitScores = new EnumMap<>(PlayerHabit.class);
    private final EnumMap<StimulusType, Integer> casualtiesByStimulus = new EnumMap<>(StimulusType.class);
    private final EnumMap<AdaptationTrait, Double> traitMastery = new EnumMap<>(AdaptationTrait.class);
    private final List<HiveHotspot> hotspots = new ArrayList<>();
    private final EnumSet<AdaptationTrait> activeTraits = EnumSet.noneOf(AdaptationTrait.class);
    private final EnumSet<AdaptationTrait> tier2Traits = EnumSet.noneOf(AdaptationTrait.class);
    private final EnumSet<HiveSynergy> activeSynergies = EnumSet.noneOf(HiveSynergy.class);
    private long activeTraitStateRevision;
    private long synchronizedMobTraitStateRevision = Long.MIN_VALUE;
    private boolean activeTraitStateInitialized;
    private final Map<Long, HyphalChunkData> hyphalChunks = new HashMap<>();
    private final Map<String, Integer> architecturalBlockCounts = new LinkedHashMap<>();
    private String primaryArchitecturalBlockId = "";
    private String pillarArchitecturalBlockId = "";
    private double typicalRoomSpan = 5.0;
    private double heightToWidthRatio = 1.0;
    private double architecturalLightDensity = 0.5;
    private int architecturalSampleCount = 0;
    private int architecturalLightBlockCount = 0;
    private int architecturalStructuralBlockCount = 0;
    private final List<BlockPos> baseAnchors = new ArrayList<>();
    private final Map<BlockPos, BlindSpotOutbreakSite> blindSpotSites = new LinkedHashMap<>();

    public HiveMindSavedData() {
        for (StimulusType type : StimulusType.values()) {
            stimulusScores.put(type, 0.0);
            casualtiesByStimulus.put(type, 0);
        }
        for (PlayerHabit habit : PlayerHabit.values()) {
            habitScores.put(habit, 0.0);
        }
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            traitMastery.put(trait, 0.0);
        }
    }

    public static HiveMindSavedData get(ServerLevel level) {
        HiveMindSavedData data = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        if (!data.activeTraitStateInitialized) {
            data.recomputeActiveTraits(level.getGameTime());
        }
        return data;
    }

    public void recordStimulus(StimulusType type, double rawAmount) {
        recordStimulus(type, rawAmount, -1L);
    }

    public void recordStimulus(StimulusType type, double rawAmount, long currentGameTime) {
        if (rawAmount <= 0.0 || Double.isNaN(rawAmount) || Double.isInfinite(rawAmount)) {
            return;
        }
        double threshold = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();
        double momentumMult = 1.0;
        double pivotMult = 1.0;

        if (lastStimulusType == type) {
            boolean withinWindow = currentGameTime < 0L || lastStimulusGameTime < 0L
                    || (currentGameTime - lastStimulusGameTime) <= 100L;
            stimulusStreak = withinWindow ? Math.min(6, stimulusStreak + 1) : 1;
            momentumMult = 1.0 + Math.min(0.45, stimulusStreak * 0.075);
        } else {
            boolean realTimeAdvance = currentGameTime < 0L || lastStimulusGameTime < 0L || currentGameTime > lastStimulusGameTime;
            if (realTimeAdvance && lastStimulusType != null && stimulusScores.getOrDefault(lastStimulusType, 0.0) >= threshold) {
                pivotMult = 1.35;
            }
            stimulusStreak = 0;
        }
        lastStimulusType = type;
        lastStimulusGameTime = currentGameTime;

        double scaled = rawAmount * MycelialConfig.LEARNING_MULTIPLIER.get() * momentumMult * pivotMult;
        double current = stimulusScores.getOrDefault(type, 0.0);
        double updated = Mth.clamp(current + scaled, 0.0, 1000.0);
        stimulusScores.put(type, updated);
        threatLevel = Mth.clamp(threatLevel + (scaled * 0.10), 0.0, 100.0);
        recomputeActiveTraits(currentGameTime);
        setDirty();
    }

    public void tickDecay(double amount) {
        tickDecay(amount, -1L);
    }

    public void tickDecay(double amount, long currentGameTime) {
        if (amount <= 0.0) {
            return;
        }
        double tier1Threshold = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();
        double tier2Mult = MycelialConfig.TIER2_DECAY_MULTIPLIER.get();
        boolean changed = false;
        for (StimulusType type : StimulusType.values()) {
            double current = stimulusScores.getOrDefault(type, 0.0);
            AdaptationTrait trait = traitFor(type);
            double mastery = getTraitMastery(trait);
            double nextScore = current;
            if (current > 0.0) {
                double baseDecay = current > tier1Threshold ? amount * tier2Mult : amount;
                double effectiveDecay = baseDecay * (1.0 - 0.50 * mastery);
                nextScore = Math.max(0.0, current - effectiveDecay);
                stimulusScores.put(type, nextScore);
                changed = true;
            }
            if (nextScore < tier1Threshold && mastery > 0.0) {
                double nextMastery = Math.max(0.0, mastery - 0.05);
                if (nextMastery < mastery) {
                    traitMastery.put(trait, nextMastery);
                    changed = true;
                }
            }
        }
        Iterator<HiveHotspot> it = hotspots.iterator();
        List<HiveHotspot> decayed = new ArrayList<>();
        while (it.hasNext()) {
            HiveHotspot hs = it.next();
            double nextInt = hs.intensity() * 0.85;
            it.remove();
            changed = true;
            if (nextInt >= 1.0) {
                decayed.add(new HiveHotspot(hs.pos(), nextInt, hs.lastUpdatedTick(), hs.dominantThreat()));
            }
        }
        hotspots.addAll(decayed);

        for (StimulusType type : StimulusType.values()) {
            int cas = casualtiesByStimulus.getOrDefault(type, 0);
            if (cas > 0) {
                casualtiesByStimulus.put(type, Math.max(0, cas - 1));
                changed = true;
            }
        }

        for (PlayerHabit habit : PlayerHabit.values()) {
            double hScore = habitScores.getOrDefault(habit, 0.0);
            if (hScore > 0.0) {
                double hDecay = hScore > HABIT_ACTIVATION_THRESHOLD ? amount * 1.25 : amount;
                habitScores.put(habit, Math.max(0.0, hScore - hDecay));
                changed = true;
            }
        }

        if (falseStimulus != null && currentGameTime >= 0L && currentGameTime >= deceptionUntilGameTime) {
            endDeceptionInternal(currentGameTime);
            this.deceptionCooldownUntilGameTime = currentGameTime + DECEPTION_NATURAL_COOLDOWN_TICKS;
            changed = true;
        }

        if (!hyphalChunks.isEmpty()) {
            Iterator<Map.Entry<Long, HyphalChunkData>> hyphalIt = hyphalChunks.entrySet().iterator();
            double baseHyphalDecay = Math.max(0.5, amount * 0.75);
            while (hyphalIt.hasNext()) {
                HyphalChunkData hc = hyphalIt.next().getValue();
                if (hc.density > 0.0) {
                    long ticksSince = currentGameTime >= 0L && currentGameTime >= hc.lastVisitTick
                            ? (currentGameTime - hc.lastVisitTick)
                            : 1200L;
                    double nextDensity = HyphalAdaptiveArchitectureEngine.computeDecayedRouteDensity(
                            hc.density, ticksSince, baseHyphalDecay);
                    if (nextDensity != hc.density) {
                        hc.density = nextDensity;
                        if (hc.density < HyphalAdaptiveArchitectureEngine.VANILLA_CALM_THRESHOLD) {
                            hc.traversalCount = Math.max(0, hc.traversalCount - 1);
                        }
                        changed = true;
                    }
                }
                if (hc.isEmpty()) {
                    hyphalIt.remove();
                    changed = true;
                }
            }
        }

        if (changed || (currentGameTime >= 0L && suppressedUntilGameTime > 0L && currentGameTime >= suppressedUntilGameTime)) {
            if (currentGameTime >= 0L && suppressedUntilGameTime > 0L && currentGameTime >= suppressedUntilGameTime) {
                suppressedUntilGameTime = 0L;
            }
            recomputeActiveTraits(currentGameTime);
            setDirty();
        }
    }

    public void applyNodeDestructionBleed(double stimulusBleed, double threatDrop, long currentGameTime) {
        if (stimulusBleed > 0.0) {
            for (StimulusType type : StimulusType.values()) {
                double current = stimulusScores.getOrDefault(type, 0.0);
                if (current > 0.0) {
                    stimulusScores.put(type, Math.max(0.0, current - stimulusBleed));
                }
            }
        }
        if (threatDrop > 0.0) {
            threatLevel = Mth.clamp(threatLevel - threatDrop, 0.0, 100.0);
        }
        recomputeActiveTraits(currentGameTime);
        setDirty();
    }

    public double bleedStimulusRatio(StimulusType type, double bleedRatio, long currentGameTime) {
        double current = stimulusScores.getOrDefault(type, 0.0);
        double clampedRatio = Mth.clamp(bleedRatio, 0.0, 1.0);
        double remaining = Math.max(0.0, current * (1.0 - clampedRatio));
        stimulusScores.put(type, remaining);
        AdaptationTrait trait = traitFor(type);
        traitMastery.put(trait, Math.max(0.0, getTraitMastery(trait) * (1.0 - clampedRatio)));
        if (lastStimulusType == type) {
            stimulusStreak = 0;
        }
        recomputeActiveTraits(currentGameTime);
        setDirty();
        return current - remaining;
    }

    public void reduceThreat(double amount) {
        if (amount <= 0.0) {
            return;
        }
        this.threatLevel = Mth.clamp(this.threatLevel - amount, 0.0, 100.0);
        setDirty();
    }

    public void addThreatPenalty(double amount) {
        if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return;
        }
        this.threatLevel = Mth.clamp(this.threatLevel + amount, 0.0, 100.0);
        tryAdvanceHighestTier();
        setDirty();
    }

    public int getHighestTierReached() {
        return highestTierReached;
    }

    @Nullable
    public HiveProgressionTier tryAdvanceHighestTier() {
        HiveProgressionTier current = HiveProgressionTier.fromThreat(this.threatLevel);
        if (current.level() > this.highestTierReached) {
            this.highestTierReached = current.level();
            setDirty();
            return current;
        }
        return null;
    }

    public void suppressHigherAdaptations(long untilGameTime, long currentGameTime) {
        this.suppressedUntilGameTime = Math.max(this.suppressedUntilGameTime, untilGameTime);
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            traitMastery.put(trait, Math.max(0.0, getTraitMastery(trait) - 0.25));
        }
        this.stimulusStreak = 0;
        recomputeActiveTraits(currentGameTime);
        setDirty();
    }

    public boolean isSuppressed(long currentGameTime) {
        return suppressedUntilGameTime > 0L && (currentGameTime < 0L || currentGameTime < suppressedUntilGameTime);
    }

    public int getSuppressionRemainingSeconds(long currentGameTime) {
        if (suppressedUntilGameTime <= 0L || currentGameTime < 0L || currentGameTime >= suppressedUntilGameTime) {
            return 0;
        }
        return (int) Math.ceil((suppressedUntilGameTime - currentGameTime) / 20.0);
    }

    public void recomputeActiveTraits() {
        recomputeActiveTraits(-1L);
    }

    public void recomputeActiveTraits(long currentGameTime) {
        EnumSet<AdaptationTrait> previousActiveTraits = EnumSet.copyOf(activeTraits);
        EnumSet<AdaptationTrait> previousTier2Traits = EnumSet.copyOf(tier2Traits);
        EnumSet<HiveSynergy> previousSynergies = EnumSet.copyOf(activeSynergies);
        activeTraits.clear();
        tier2Traits.clear();
        activeSynergies.clear();

        double threshold = MycelialConfig.TRAIT_ACTIVATION_THRESHOLD.get();
        double tier2Threshold = MycelialConfig.getEffectiveTier2ActivationThreshold();
        boolean suppressed = isSuppressed(currentGameTime);

        for (AdaptationTrait trait : AdaptationTrait.values()) {
            double score = stimulusScores.getOrDefault(trait.trigger(), 0.0);
            if (score >= threshold) {
                activeTraits.add(trait);
            }
            if (!suppressed && score >= tier2Threshold) {
                tier2Traits.add(trait);
            }
        }

        if (!suppressed && MycelialConfig.SYNERGIES_ENABLED.get()) {
            for (HiveSynergy synergy : HiveSynergy.values()) {
                if (synergy.isSatisfiedBy(activeTraits, tier2Traits)) {
                    activeSynergies.add(synergy);
                }
            }
        }

        if (!activeTraits.equals(previousActiveTraits)
                || !tier2Traits.equals(previousTier2Traits)
                || !activeSynergies.equals(previousSynergies)) {
            activeTraitStateRevision++;
            setDirty();
        }
        activeTraitStateInitialized = true;
    }

    public long getActiveTraitStateRevision() {
        return activeTraitStateRevision;
    }

    public boolean needsMobTraitSynchronization() {
        return synchronizedMobTraitStateRevision != activeTraitStateRevision;
    }

    public void markMobTraitsSynchronized() {
        synchronizedMobTraitStateRevision = activeTraitStateRevision;
    }

    public boolean hasTrait(AdaptationTrait trait) {
        return activeTraits.contains(trait);
    }

    public boolean hasTier2Trait(AdaptationTrait trait) {
        return tier2Traits.contains(trait);
    }

    public boolean hasSynergy(HiveSynergy synergy) {
        return activeSynergies.contains(synergy);
    }

    public Set<AdaptationTrait> getActiveTraits() {
        return Collections.unmodifiableSet(activeTraits);
    }

    public Set<AdaptationTrait> getTier2Traits() {
        return Collections.unmodifiableSet(tier2Traits);
    }

    public Set<HiveSynergy> getActiveSynergies() {
        return Collections.unmodifiableSet(activeSynergies);
    }

    public double getStimulusScore(StimulusType type) {
        return stimulusScores.getOrDefault(type, 0.0);
    }

    public Map<StimulusType, Double> getStimulusScores() {
        return Collections.unmodifiableMap(stimulusScores);
    }

    public double getThreatLevel() {
        return threatLevel;
    }

    public int getLastWaveMilestone() {
        return lastWaveMilestone;
    }

    public void setLastWaveMilestone(int milestone, long gameTime) {
        this.lastWaveMilestone = milestone;
        this.lastWaveGameTime = gameTime;
        setDirty();
    }

    public long getLastWaveGameTime() {
        return lastWaveGameTime;
    }

    public boolean isFungalTideActive(long currentGameTime) {
        return currentGameTime >= 0 && currentGameTime < fungalTideUntilGameTime;
    }

    public int getFungalTideRemainingSeconds(long currentGameTime) {
        if (!isFungalTideActive(currentGameTime)) {
            return 0;
        }
        return (int) Math.ceil((fungalTideUntilGameTime - currentGameTime) / 20.0);
    }

    public long getLastFungalTideGameTime() {
        return lastFungalTideGameTime;
    }

    public void triggerFungalTide(long currentGameTime, int durationTicks) {
        this.fungalTideUntilGameTime = currentGameTime + Math.max(20, durationTicks);
        this.lastFungalTideGameTime = currentGameTime;
        setDirty();
    }

    public void endFungalTide(long currentGameTime) {
        if (this.fungalTideUntilGameTime > 0) {
            this.fungalTideUntilGameTime = 0L;
            if (currentGameTime > 0) {
                this.lastFungalTideGameTime = currentGameTime;
            }
            setDirty();
        }
    }

    public boolean isDeepMyceliumBreached() {
        return deepMyceliumBreached;
    }

    public void markDeepMyceliumBreached() {
        if (!this.deepMyceliumBreached) {
            this.deepMyceliumBreached = true;
            setDirty();
        }
    }

    public boolean isMatriarchDefeated() {
        return matriarchDefeated;
    }

    public boolean isMatriarchRemissionActive(long currentGameTime) {
        return currentGameTime >= 0L && currentGameTime < matriarchRemissionUntilGameTime;
    }

    public int getMatriarchRemissionRemainingSeconds(long currentGameTime) {
        if (!isMatriarchRemissionActive(currentGameTime)) {
            return 0;
        }
        return (int) Math.ceil((matriarchRemissionUntilGameTime - currentGameTime) / 20.0);
    }

    public void promoteHighestTier(HiveProgressionTier tier) {
        if (tier != null && tier.level() > this.highestTierReached) {
            this.highestTierReached = tier.level();
            setDirty();
        }
    }

    public HiveProgressionTier getHighestProgressionTier() {
        HiveProgressionTier[] vals = HiveProgressionTier.values();
        return vals[Mth.clamp(this.highestTierReached, 0, vals.length - 1)];
    }

    public void recordMatriarchDefeat(long currentGameTime) {
        this.matriarchDefeated = true;
        this.deepMyceliumBreached = true;
        this.highestTierReached = HiveProgressionTier.OVERMIND.level();
        this.matriarchRemissionUntilGameTime = Math.max(0L, currentGameTime) + MATRIARCH_REMISSION_DURATION_TICKS;
        this.threatLevel = HiveProgressionTier.computePostMatriarchThreat(this.threatLevel);
        endFungalTide(currentGameTime);
        setDirty();
    }

    /**
     * Synchronizes progression floor, unlocked traits/synergies, and dimensional milestones
     * from a source dimension (typically Overworld -> deep_mycelium) without lowering existing progress.
     */
    public void syncProgressionFrom(HiveMindSavedData source) {
        syncProgressionFrom(source, -1L, -1L);
    }

    public void syncProgressionFrom(HiveMindSavedData source, long sourceGameTime, long targetGameTime) {
        if (source == null || source == this) {
            return;
        }
        boolean changed = false;
        double syncedThreat = HiveProgressionTier.computeSyncedDeepThreat(source.threatLevel, this.threatLevel);
        if (syncedThreat > this.threatLevel) {
            this.threatLevel = syncedThreat;
            changed = true;
        }
        if (source.highestTierReached > this.highestTierReached) {
            this.highestTierReached = source.highestTierReached;
            changed = true;
        }
        if (source.deepMyceliumBreached && !this.deepMyceliumBreached) {
            this.deepMyceliumBreached = true;
            changed = true;
        }
        if (source.matriarchDefeated && !this.matriarchDefeated) {
            this.matriarchDefeated = true;
            changed = true;
        }
        if (source.matriarchDefeated && sourceGameTime >= 0L && targetGameTime >= 0L) {
            long remainingTicks = Math.max(0L, source.matriarchRemissionUntilGameTime - sourceGameTime);
            long targetDeadline = targetGameTime + remainingTicks;
            if (targetDeadline != this.matriarchRemissionUntilGameTime) {
                this.matriarchRemissionUntilGameTime = targetDeadline;
                changed = true;
            }
        }
        for (StimulusType type : StimulusType.values()) {
            double srcScore = source.getStimulusScore(type);
            if (srcScore > this.stimulusScores.getOrDefault(type, 0.0)) {
                this.stimulusScores.put(type, srcScore);
                changed = true;
            }
        }
        if (source.getArchitecturalAccent().hasCustomAccent()) {
            this.architecturalBlockCounts.clear();
            this.architecturalBlockCounts.putAll(source.architecturalBlockCounts);
            this.primaryArchitecturalBlockId = source.primaryArchitecturalBlockId;
            this.pillarArchitecturalBlockId = source.pillarArchitecturalBlockId;
            this.typicalRoomSpan = source.typicalRoomSpan;
            this.heightToWidthRatio = source.heightToWidthRatio;
            this.architecturalLightDensity = source.architecturalLightDensity;
            this.architecturalSampleCount = source.architecturalSampleCount;
            this.architecturalLightBlockCount = source.architecturalLightBlockCount;
            this.architecturalStructuralBlockCount = source.architecturalStructuralBlockCount;
            changed = true;
        }
        if (changed) {
            tryAdvanceHighestTier();
            recomputeActiveTraits(targetGameTime);
            setDirty();
        }
    }

    public void resetAll() {
        this.threatLevel = 0.0;
        this.lastWaveMilestone = 0;
        this.highestTierReached = 0;
        this.deepMyceliumBreached = false;
        this.matriarchDefeated = false;
        this.matriarchRemissionUntilGameTime = 0L;
        this.lastWaveGameTime = 0L;
        this.suppressedUntilGameTime = 0L;
        this.fungalTideUntilGameTime = 0L;
        this.lastFungalTideGameTime = 0L;
        for (StimulusType type : StimulusType.values()) {
            this.stimulusScores.put(type, 0.0);
            this.casualtiesByStimulus.put(type, 0);
        }
        for (PlayerHabit habit : PlayerHabit.values()) {
            this.habitScores.put(habit, 0.0);
        }
        this.falseStimulus = null;
        this.deceptionUntilGameTime = 0L;
        this.deceptionCooldownUntilGameTime = 0L;
        this.deceptionAppliedBoost = 0.0;
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            this.traitMastery.put(trait, 0.0);
        }
        this.lastStimulusType = null;
        this.stimulusStreak = 0;
        this.lastStimulusGameTime = 0L;
        this.hotspots.clear();
        this.hyphalChunks.clear();
        this.architecturalBlockCounts.clear();
        this.primaryArchitecturalBlockId = "";
        this.pillarArchitecturalBlockId = "";
        this.typicalRoomSpan = 5.0;
        this.heightToWidthRatio = 1.0;
        this.architecturalLightDensity = 0.5;
        this.architecturalSampleCount = 0;
        this.architecturalLightBlockCount = 0;
        this.architecturalStructuralBlockCount = 0;
        this.baseAnchors.clear();
        this.blindSpotSites.clear();
        recomputeActiveTraits();
        setDirty();
    }

    public void restoreFromSnapshot(CompoundTag snapshot, HolderLookup.Provider registries) {
        HiveMindSavedData restored = load(snapshot, registries);
        threatLevel = restored.threatLevel;
        lastWaveMilestone = restored.lastWaveMilestone;
        highestTierReached = restored.highestTierReached;
        deepMyceliumBreached = restored.deepMyceliumBreached;
        matriarchDefeated = restored.matriarchDefeated;
        matriarchRemissionUntilGameTime = restored.matriarchRemissionUntilGameTime;
        lastWaveGameTime = restored.lastWaveGameTime;
        suppressedUntilGameTime = restored.suppressedUntilGameTime;
        fungalTideUntilGameTime = restored.fungalTideUntilGameTime;
        lastFungalTideGameTime = restored.lastFungalTideGameTime;
        lastStimulusType = restored.lastStimulusType;
        stimulusStreak = restored.stimulusStreak;
        lastStimulusGameTime = restored.lastStimulusGameTime;
        falseStimulus = restored.falseStimulus;
        deceptionUntilGameTime = restored.deceptionUntilGameTime;
        deceptionCooldownUntilGameTime = restored.deceptionCooldownUntilGameTime;
        deceptionAppliedBoost = restored.deceptionAppliedBoost;
        stimulusScores.clear();
        stimulusScores.putAll(restored.stimulusScores);
        habitScores.clear();
        habitScores.putAll(restored.habitScores);
        casualtiesByStimulus.clear();
        casualtiesByStimulus.putAll(restored.casualtiesByStimulus);
        traitMastery.clear();
        traitMastery.putAll(restored.traitMastery);
        hotspots.clear();
        hotspots.addAll(restored.hotspots);
        hyphalChunks.clear();
        hyphalChunks.putAll(restored.hyphalChunks);
        architecturalBlockCounts.clear();
        architecturalBlockCounts.putAll(restored.architecturalBlockCounts);
        primaryArchitecturalBlockId = restored.primaryArchitecturalBlockId;
        pillarArchitecturalBlockId = restored.pillarArchitecturalBlockId;
        typicalRoomSpan = restored.typicalRoomSpan;
        heightToWidthRatio = restored.heightToWidthRatio;
        architecturalLightDensity = restored.architecturalLightDensity;
        architecturalSampleCount = restored.architecturalSampleCount;
        architecturalLightBlockCount = restored.architecturalLightBlockCount;
        architecturalStructuralBlockCount = restored.architecturalStructuralBlockCount;
        baseAnchors.clear();
        baseAnchors.addAll(restored.baseAnchors);
        blindSpotSites.clear();
        blindSpotSites.putAll(restored.blindSpotSites);
        activeTraits.clear();
        activeTraits.addAll(restored.activeTraits);
        tier2Traits.clear();
        tier2Traits.addAll(restored.tier2Traits);
        activeSynergies.clear();
        activeSynergies.addAll(restored.activeSynergies);
        activeTraitStateRevision = restored.activeTraitStateRevision;
        synchronizedMobTraitStateRevision = Long.MIN_VALUE;
        activeTraitStateInitialized = false;
        setDirty();
    }

    public static AdaptationTrait traitFor(StimulusType type) {
        return switch (type) {
            case MELEE -> AdaptationTrait.MELEE_THORNS;
            case PROJECTILE -> AdaptationTrait.PROJECTILE_DEFLECT;
            case FIRE -> AdaptationTrait.FIRE_RESISTANT;
            case EXPLOSION -> AdaptationTrait.BLAST_SHIELDED;
            case MAGIC -> AdaptationTrait.MAGIC_CARAPACE;
        };
    }

    public void reinforceTraitMastery(AdaptationTrait trait, double delta) {
        if (delta <= 0.0) {
            return;
        }
        traitMastery.put(trait, Mth.clamp(getTraitMastery(trait) + delta, 0.0, 1.0));
        setDirty();
    }

    public double getTraitMastery(AdaptationTrait trait) {
        return traitMastery.getOrDefault(trait, 0.0);
    }

    public int getLastStimulusStreak() {
        return stimulusStreak;
    }

    public void recordCombatHotspot(BlockPos pos, double intensity, long gameTime) {
        recordCombatHotspot(pos, intensity, gameTime, StimulusType.MELEE);
    }

    public void recordCombatHotspot(BlockPos pos, double intensity, long gameTime, StimulusType threatType) {
        if (intensity <= 0.0) {
            return;
        }
        BlockPos immutable = pos.immutable();
        for (int i = 0; i < hotspots.size(); i++) {
            HiveHotspot existing = hotspots.get(i);
            if (existing.pos().distSqr(immutable) <= 256.0D) {
                double merged = Math.min(100.0, existing.intensity() + intensity);
                StimulusType dominant = intensity >= existing.intensity() * 0.5 ? threatType : existing.dominantThreat();
                hotspots.set(i, new HiveHotspot(immutable, merged, gameTime, dominant));
                setDirty();
                return;
            }
        }
        if (hotspots.size() >= MAX_HOTSPOTS) {
            hotspots.sort(Comparator.comparingDouble(HiveHotspot::intensity));
            hotspots.remove(0);
        }
        hotspots.add(new HiveHotspot(immutable, Math.min(100.0, intensity), gameTime, threatType));
        setDirty();
    }

    @Nullable
    public HiveHotspot getHottestSpot() {
        return hotspots.stream().max(Comparator.comparingDouble(HiveHotspot::intensity)).orElse(null);
    }

    public List<HiveHotspot> getHotspots() {
        return Collections.unmodifiableList(hotspots);
    }

    public void recordMobCasualty(StimulusType cause) {
        casualtiesByStimulus.merge(cause, 1, Integer::sum);
        setDirty();
    }

    public int getCasualties(StimulusType cause) {
        return casualtiesByStimulus.getOrDefault(cause, 0);
    }

    public StimulusType getMostLethalStimulus() {
        StimulusType best = StimulusType.MELEE;
        int bestCount = -1;
        for (StimulusType type : StimulusType.values()) {
            int c = casualtiesByStimulus.getOrDefault(type, 0);
            if (c > bestCount) {
                bestCount = c;
                best = type;
            }
        }
        return best;
    }

    public double getEffectiveThreatScore(StimulusType type) {
        return stimulusScores.getOrDefault(type, 0.0) + casualtiesByStimulus.getOrDefault(type, 0) * 25.0;
    }

    public double getEffectivenessPressure() {
        int total = 0;
        for (int c : casualtiesByStimulus.values()) {
            total += c;
        }
        return Mth.clamp(total * 0.035, 0.0, 0.35);
    }

    public void recordHabit(PlayerHabit habit, double rawAmount) {
        if (rawAmount <= 0.0 || Double.isNaN(rawAmount) || Double.isInfinite(rawAmount)) {
            return;
        }
        double scaled = rawAmount * MycelialConfig.LEARNING_MULTIPLIER.get();
        double current = habitScores.getOrDefault(habit, 0.0);
        habitScores.put(habit, Mth.clamp(current + scaled, 0.0, HABIT_MAX_SCORE));
        setDirty();
    }

    public double getHabitScore(PlayerHabit habit) {
        return habitScores.getOrDefault(habit, 0.0);
    }

    public boolean hasHabitAdaptation(PlayerHabit habit) {
        return getHabitScore(habit) >= HABIT_ACTIVATION_THRESHOLD;
    }

    public boolean canStartDeception(long currentGameTime) {
        return !isDeceptionActive(currentGameTime) && currentGameTime >= deceptionCooldownUntilGameTime;
    }

    public void startDeception(StimulusType type, long currentGameTime) {
        if (falseStimulus != null) {
            endDeceptionInternal(currentGameTime);
        }
        this.falseStimulus = type;
        this.deceptionUntilGameTime = currentGameTime + DECEPTION_DURATION_TICKS;
        double current = stimulusScores.getOrDefault(type, 0.0);
        double boosted = Math.min(1000.0, current + DECEPTION_FALSE_BOOST);
        this.deceptionAppliedBoost = boosted - current;
        stimulusScores.put(type, boosted);
        recomputeActiveTraits(currentGameTime);
        setDirty();
    }

    public boolean isDeceptionActive(long currentGameTime) {
        if (falseStimulus == null || currentGameTime < 0L) {
            return false;
        }
        if (currentGameTime >= deceptionUntilGameTime) {
            endDeceptionInternal(currentGameTime);
            this.deceptionCooldownUntilGameTime = currentGameTime + DECEPTION_NATURAL_COOLDOWN_TICKS;
            return false;
        }
        return true;
    }

    @Nullable
    public StimulusType getFalseStimulus() {
        return falseStimulus;
    }

    public int getDeceptionRemainingSeconds(long currentGameTime) {
        if (!isDeceptionActive(currentGameTime)) {
            return 0;
        }
        return (int) Math.ceil((deceptionUntilGameTime - currentGameTime) / 20.0);
    }

    public int getDeceptionCooldownRemainingSeconds(long currentGameTime) {
        if (currentGameTime < 0L || currentGameTime >= deceptionCooldownUntilGameTime) {
            return 0;
        }
        return (int) Math.ceil((deceptionCooldownUntilGameTime - currentGameTime) / 20.0);
    }

    @Nullable
    private HyphalChunkData getOrCreateHyphalChunk(ChunkPos chunkPos) {
        long key = chunkPos.toLong();
        HyphalChunkData chunk = hyphalChunks.get(key);
        if (chunk != null) {
            return chunk;
        }
        if (hyphalChunks.size() >= HyphalAdaptiveArchitectureEngine.MAX_TRACKED_ROUTE_CHUNKS) {
            Long evictKey = null;
            double lowestDensity = Double.MAX_VALUE;
            long oldestVisit = Long.MAX_VALUE;
            for (Map.Entry<Long, HyphalChunkData> entry : hyphalChunks.entrySet()) {
                HyphalChunkData candidate = entry.getValue();
                if (!candidate.activeVeins.isEmpty()) {
                    continue;
                }
                if (candidate.density < lowestDensity
                        || (candidate.density == lowestDensity && candidate.lastVisitTick < oldestVisit)) {
                    lowestDensity = candidate.density;
                    oldestVisit = candidate.lastVisitTick;
                    evictKey = entry.getKey();
                }
            }
            if (evictKey == null) {
                return null;
            }
            hyphalChunks.remove(evictKey);
        }
        chunk = new HyphalChunkData();
        hyphalChunks.put(key, chunk);
        return chunk;
    }

    public synchronized double recordRouteStep(
            ChunkPos chunkPos,
            @Nullable BlockPos groundPos,
            boolean changedChunk,
            boolean inProtectedZone,
            boolean nearSymbioticNode,
            boolean isCrouching,
            long gameTime
    ) {
        return recordRouteStep(chunkPos, groundPos, changedChunk, inProtectedZone, nearSymbioticNode, isCrouching, gameTime, true);
    }

    public synchronized double recordRouteStep(
            ChunkPos chunkPos,
            @Nullable BlockPos groundPos,
            boolean changedChunk,
            boolean inProtectedZone,
            boolean nearSymbioticNode,
            boolean isCrouching,
            long gameTime,
            boolean recordTrailTraversal
    ) {
        long key = chunkPos.toLong();
        HyphalChunkData chunk = hyphalChunks.get(key);
        if (chunk == null) {
            if (inProtectedZone || nearSymbioticNode) {
                return 0.0;
            }
            chunk = getOrCreateHyphalChunk(chunkPos);
            if (chunk == null) {
                return 0.0;
            }
        }

        double delta = HyphalAdaptiveArchitectureEngine.computeRouteDensityDelta(
                chunk.density, chunk.traversalCount, inProtectedZone, nearSymbioticNode, isCrouching);
        chunk.density = Mth.clamp(chunk.density + delta, 0.0, HyphalAdaptiveArchitectureEngine.MAX_NETWORK_DENSITY);
        if (changedChunk && !inProtectedZone && !nearSymbioticNode) {
            chunk.traversalCount = Math.min(1000, chunk.traversalCount + 1);
        }
        chunk.lastVisitTick = Math.max(0L, gameTime);
        if (!inProtectedZone && !nearSymbioticNode && groundPos != null) {
            chunk.addWaypoint(groundPos);
            if (recordTrailTraversal) {
                chunk.recordTrailPoint(groundPos, true);
            }
        }
        if (chunk.isEmpty()) {
            hyphalChunks.remove(key);
            setDirty();
            return 0.0;
        }
        setDirty();
        return chunk.density;
    }

    public synchronized void recordTrailPoint(
            ChunkPos chunkPos,
            BlockPos groundPos,
            boolean countTraversal,
            long gameTime
    ) {
        if (groundPos == null) {
            return;
        }
        HyphalChunkData chunk = getOrCreateHyphalChunk(chunkPos);
        if (chunk == null) {
            return;
        }
        chunk.recordTrailPoint(groundPos, countTraversal);
        chunk.lastVisitTick = Math.max(chunk.lastVisitTick, Math.max(0L, gameTime));
        setDirty();
    }

    public synchronized void setHyphalDensity(ChunkPos chunkPos, double density, long gameTime) {
        long key = chunkPos.toLong();
        double clamped = Mth.clamp(Double.isFinite(density) ? density : 0.0,
                0.0, HyphalAdaptiveArchitectureEngine.MAX_NETWORK_DENSITY);
        HyphalChunkData chunk = hyphalChunks.get(key);
        if (chunk == null && clamped <= 0.0) {
            return;
        }
        if (chunk == null) {
            if (hyphalChunks.size() >= HyphalAdaptiveArchitectureEngine.MAX_TRACKED_ROUTE_CHUNKS) {
                return;
            }
            chunk = new HyphalChunkData();
            hyphalChunks.put(key, chunk);
        }
        chunk.density = clamped;
        chunk.lastVisitTick = Math.max(0L, gameTime);
        if (chunk.isEmpty()) {
            hyphalChunks.remove(key);
        }
        setDirty();
    }

    public synchronized double getHyphalDensity(ChunkPos chunkPos) {
        HyphalChunkData chunk = hyphalChunks.get(chunkPos.toLong());
        return chunk != null ? chunk.density : 0.0;
    }

    public synchronized double getHyphalDensityAt(BlockPos pos) {
        return getHyphalDensity(new ChunkPos(pos));
    }

    public synchronized int getHyphalTraversalCount(ChunkPos chunkPos) {
        HyphalChunkData chunk = hyphalChunks.get(chunkPos.toLong());
        return chunk != null ? chunk.traversalCount : 0;
    }

    public synchronized List<BlockPos> getRouteWaypoints(ChunkPos chunkPos) {
        HyphalChunkData chunk = hyphalChunks.get(chunkPos.toLong());
        return chunk != null ? new ArrayList<>(chunk.routeWaypoints) : List.of();
    }

    public synchronized List<BlockPos> getOrderedTrail(ChunkPos chunkPos) {
        HyphalChunkData chunk = hyphalChunks.get(chunkPos.toLong());
        return chunk != null ? new ArrayList<>(chunk.orderedTrail) : List.of();
    }

    public synchronized int getBlockTraversals(BlockPos groundPos) {
        if (groundPos == null) {
            return 0;
        }
        HyphalChunkData chunk = hyphalChunks.get(new ChunkPos(groundPos).toLong());
        return chunk != null ? chunk.getBlockTraversals(groundPos) : 0;
    }

    public synchronized Map<BlockPos, Integer> getBlockTraversalsInChunk(ChunkPos chunkPos) {
        HyphalChunkData chunk = hyphalChunks.get(chunkPos.toLong());
        if (chunk == null || chunk.blockTraversals.isEmpty()) {
            return Map.of();
        }
        Map<BlockPos, Integer> out = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : chunk.blockTraversals.entrySet()) {
            out.put(BlockPos.of(entry.getKey()), entry.getValue());
        }
        return out;
    }

    public synchronized boolean hasRootCandidateInChunk(ChunkPos chunkPos, int threshold) {
        HyphalChunkData chunk = hyphalChunks.get(chunkPos.toLong());
        if (chunk == null || chunk.blockTraversals.isEmpty()) {
            return false;
        }
        for (int count : chunk.blockTraversals.values()) {
            if (HyphalPhysics.isRootCandidate(count, threshold)) {
                return true;
            }
        }
        return false;
    }

    public synchronized boolean registerHyphalVein(BlockPos pos) {
        long key = new ChunkPos(pos).toLong();
        HyphalChunkData chunk = hyphalChunks.get(key);
        if (chunk == null) {
            if (hyphalChunks.size() >= HyphalAdaptiveArchitectureEngine.MAX_TRACKED_ROUTE_CHUNKS) {
                return false;
            }
            chunk = new HyphalChunkData();
            hyphalChunks.put(key, chunk);
        }
        boolean added = chunk.addVein(pos);
        if (added) {
            setDirty();
        }
        return added;
    }

    public synchronized boolean isTrackedHyphalVein(BlockPos pos) {
        HyphalChunkData chunk = hyphalChunks.get(new ChunkPos(pos).toLong());
        return chunk != null && chunk.activeVeins.contains(pos);
    }

    public synchronized List<BlockPos> getActiveVeins(ChunkPos chunkPos) {
        HyphalChunkData chunk = hyphalChunks.get(chunkPos.toLong());
        return chunk != null ? new ArrayList<>(chunk.activeVeins) : List.of();
    }

    public synchronized double severHyphalVein(BlockPos pos, double densityDrop, long gameTime) {
        long key = new ChunkPos(pos).toLong();
        HyphalChunkData chunk = hyphalChunks.get(key);
        if (chunk == null) {
            return 0.0;
        }
        boolean removed = chunk.removeVein(pos);
        double safeDrop = Double.isFinite(densityDrop) && densityDrop > 0.0 ? densityDrop : 0.0;
        if (safeDrop > 0.0) {
            chunk.density = Math.max(0.0, chunk.density - safeDrop);
            chunk.lastVisitTick = Math.max(chunk.lastVisitTick, gameTime);
        }
        double remaining = chunk.density;
        if (chunk.isEmpty()) {
            hyphalChunks.remove(key);
        }
        if (removed || safeDrop > 0.0) {
            setDirty();
        }
        return remaining;
    }

    public synchronized boolean canTriggerRouteAmbush(ChunkPos chunkPos, long gameTime) {
        HyphalChunkData chunk = hyphalChunks.get(chunkPos.toLong());
        if (chunk == null) {
            return false;
        }
        long ticksSince = chunk.lastAmbushTick < 0L
                ? HyphalAdaptiveArchitectureEngine.AMBUSH_COOLDOWN_TICKS
                : Math.max(0L, gameTime - chunk.lastAmbushTick);
        return chunk.density >= HyphalAdaptiveArchitectureEngine.AMBUSH_DENSITY_THRESHOLD
                && ticksSince >= HyphalAdaptiveArchitectureEngine.AMBUSH_COOLDOWN_TICKS;
    }

    public synchronized void markRouteAmbushTriggered(ChunkPos chunkPos, long gameTime) {
        HyphalChunkData chunk = hyphalChunks.get(chunkPos.toLong());
        if (chunk != null) {
            chunk.lastAmbushTick = Math.max(0L, gameTime);
            setDirty();
        }
    }

    public synchronized List<HyphalChunkSnapshot> getDenseHyphalChunksWithin(ChunkPos center, int chunkRadius, int limit) {
        if (chunkRadius < 0 || limit <= 0) {
            return List.of();
        }
        List<HyphalChunkSnapshot> out = new ArrayList<>();
        for (Map.Entry<Long, HyphalChunkData> entry : hyphalChunks.entrySet()) {
            HyphalChunkData hc = entry.getValue();
            if (hc.density < HyphalAdaptiveArchitectureEngine.VANILLA_CALM_THRESHOLD && hc.activeVeins.isEmpty()) {
                continue;
            }
            ChunkPos cp = new ChunkPos(entry.getKey());
            if (Math.abs(cp.x - center.x) <= chunkRadius && Math.abs(cp.z - center.z) <= chunkRadius) {
                out.add(new HyphalChunkSnapshot(cp.x, cp.z, hc.density, hc.traversalCount, hc.activeVeins.size()));
            }
        }
        out.sort(Comparator
                .comparingLong((HyphalChunkSnapshot snap) -> {
                    long dx = (long) snap.chunkX() - center.x;
                    long dz = (long) snap.chunkZ() - center.z;
                    return dx * dx + dz * dz;
                })
                .thenComparingInt(HyphalChunkSnapshot::chunkX)
                .thenComparingInt(HyphalChunkSnapshot::chunkZ));
        return out.size() <= limit ? out : new ArrayList<>(out.subList(0, limit));
    }

    public synchronized int countTrackedHyphalChunks() {
        return hyphalChunks.size();
    }

    public synchronized int countTotalActiveVeins() {
        int total = 0;
        for (HyphalChunkData chunk : hyphalChunks.values()) {
            total += chunk.activeVeins.size();
        }
        return total;
    }

    public synchronized boolean hasDenseHyphalRoutes() {
        for (HyphalChunkData chunk : hyphalChunks.values()) {
            if (chunk.density >= HyphalAdaptiveArchitectureEngine.VEIN_GROWTH_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    public synchronized void recordBaseAnchor(@Nullable BlockPos anchor) {
        if (anchor == null) {
            return;
        }
        BlockPos immutable = anchor.immutable();
        for (BlockPos existing : baseAnchors) {
            if (existing.distSqr(immutable) < 144.0D) {
                return;
            }
        }
        if (baseAnchors.size() >= MAX_BASE_ANCHORS) {
            baseAnchors.remove(0);
        }
        baseAnchors.add(immutable);
        setDirty();
    }

    public synchronized List<BlockPos> getBaseAnchors() {
        return List.copyOf(baseAnchors);
    }

    public synchronized int getArchitecturalLightBlockCount() {
        return architecturalLightBlockCount;
    }

    public synchronized int getArchitecturalStructuralBlockCount() {
        return architecturalStructuralBlockCount;
    }

    public synchronized void recordArchitecturalBlockPlacement(
            @Nullable String blockId,
            boolean isPillarLike,
            boolean isLightSource
    ) {
        if (blockId == null || blockId.isBlank()) {
            return;
        }
        architecturalSampleCount++;
        if (isLightSource) {
            architecturalLightBlockCount++;
        } else {
            architecturalStructuralBlockCount++;
            architecturalBlockCounts.merge(blockId, 1, Integer::sum);
            if (architecturalBlockCounts.size() > 48) {
                String lowestKey = null;
                int lowestVal = Integer.MAX_VALUE;
                for (Map.Entry<String, Integer> entry : architecturalBlockCounts.entrySet()) {
                    if (entry.getValue() < lowestVal) {
                        lowestVal = entry.getValue();
                        lowestKey = entry.getKey();
                    }
                }
                if (lowestKey != null) {
                    architecturalBlockCounts.remove(lowestKey);
                }
            }
            String bestId = primaryArchitecturalBlockId;
            int bestCount = -1;
            for (Map.Entry<String, Integer> entry : architecturalBlockCounts.entrySet()) {
                if (entry.getValue() > bestCount) {
                    bestCount = entry.getValue();
                    bestId = entry.getKey();
                }
            }
            if (bestId != null && !bestId.isBlank()) {
                primaryArchitecturalBlockId = bestId;
            }
            if (isPillarLike) {
                pillarArchitecturalBlockId = blockId;
            } else if (pillarArchitecturalBlockId.isBlank()) {
                pillarArchitecturalBlockId = HyphalAdaptiveArchitectureEngine
                        .resolveCompanionPillarBlockId(primaryArchitecturalBlockId);
            }
        }
        setDirty();
    }

    public synchronized void recordArchitecturalGeometry(double roomSpan, double hToWRatio, double lightDensity) {
        int sample = Math.max(1, architecturalSampleCount);
        typicalRoomSpan = HyphalAdaptiveArchitectureEngine.updateRunningMetric(
                typicalRoomSpan, roomSpan, sample, 2.0, 16.0);
        heightToWidthRatio = HyphalAdaptiveArchitectureEngine.updateRunningMetric(
                heightToWidthRatio, hToWRatio, sample, 0.4, 3.0);
        architecturalLightDensity = HyphalAdaptiveArchitectureEngine.updateRunningMetric(
                architecturalLightDensity, lightDensity, sample, 0.0, 1.0);
        setDirty();
    }

    public synchronized HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot getArchitecturalAccent() {
        String resolvedPillar = (pillarArchitecturalBlockId == null || pillarArchitecturalBlockId.isBlank())
                ? HyphalAdaptiveArchitectureEngine.resolveCompanionPillarBlockId(primaryArchitecturalBlockId)
                : pillarArchitecturalBlockId;
        return new HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot(
                primaryArchitecturalBlockId,
                resolvedPillar,
                typicalRoomSpan,
                heightToWidthRatio,
                architecturalLightDensity,
                architecturalSampleCount
        );
    }

    public synchronized void registerBlindSpotSite(BlindSpotOutbreakSite site) {
        if (site == null) {
            return;
        }
        blindSpotSites.put(site.getCorePos(), site);
        setDirty();
    }

    @Nullable
    public synchronized BlindSpotOutbreakSite getBlindSpotSite(BlockPos corePos) {
        return corePos != null ? blindSpotSites.get(corePos) : null;
    }

    public synchronized boolean isBlindSpotCore(BlockPos pos) {
        if (pos == null) {
            return false;
        }
        BlindSpotOutbreakSite site = blindSpotSites.get(pos);
        return site != null && !site.isCompleted();
    }

    @Nullable
    public synchronized BlindSpotOutbreakSite findBlindSpotSiteNear(BlockPos pos, double maxRadius) {
        if (pos == null || maxRadius < 0.0) {
            return null;
        }
        double maxSq = maxRadius * maxRadius;
        BlindSpotOutbreakSite best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (BlindSpotOutbreakSite site : blindSpotSites.values()) {
            double distSq = site.getCorePos().distSqr(pos);
            if (distSq <= maxSq && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = site;
            }
        }
        return best;
    }

    public synchronized Collection<BlindSpotOutbreakSite> getBlindSpotSites() {
        return List.copyOf(blindSpotSites.values());
    }

    public synchronized boolean removeBlindSpotSite(BlockPos corePos) {
        if (corePos != null && blindSpotSites.remove(corePos) != null) {
            setDirty();
            return true;
        }
        return false;
    }

    public synchronized void markBlindSpotDirty() {
        setDirty();
    }

    public void revealDeception(long currentGameTime) {
        if (falseStimulus == null) {
            return;
        }
        endDeceptionInternal(currentGameTime);
        this.deceptionCooldownUntilGameTime = currentGameTime + DECEPTION_COOLDOWN_TICKS;
        setDirty();
    }

    private void endDeceptionInternal(long currentGameTime) {
        if (falseStimulus != null && deceptionAppliedBoost > 0.0) {
            double current = stimulusScores.getOrDefault(falseStimulus, 0.0);
            stimulusScores.put(falseStimulus, Math.max(0.0, current - deceptionAppliedBoost));
        }
        this.falseStimulus = null;
        this.deceptionUntilGameTime = 0L;
        this.deceptionAppliedBoost = 0.0;
        recomputeActiveTraits(currentGameTime);
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putDouble(TAG_THREAT, threatLevel);
        tag.putInt(TAG_LAST_MILESTONE, lastWaveMilestone);
        tag.putLong(TAG_LAST_WAVE_TICK, lastWaveGameTime);
        tag.putLong(TAG_SUPPRESSED_UNTIL, suppressedUntilGameTime);
        tag.putLong(TAG_FUNGAL_TIDE_UNTIL, fungalTideUntilGameTime);
        tag.putLong(TAG_LAST_FUNGAL_TIDE, lastFungalTideGameTime);
        CompoundTag stimuliTag = new CompoundTag();
        for (Map.Entry<StimulusType, Double> entry : stimulusScores.entrySet()) {
            stimuliTag.putDouble(entry.getKey().id(), entry.getValue());
        }
        tag.put(TAG_STIMULI, stimuliTag);
        CompoundTag casualtiesTag = new CompoundTag();
        for (Map.Entry<StimulusType, Integer> entry : casualtiesByStimulus.entrySet()) {
            casualtiesTag.putInt(entry.getKey().id(), entry.getValue());
        }
        tag.put(TAG_CASUALTIES, casualtiesTag);
        CompoundTag masteryTag = new CompoundTag();
        for (Map.Entry<AdaptationTrait, Double> entry : traitMastery.entrySet()) {
            masteryTag.putDouble(entry.getKey().id(), entry.getValue());
        }
        tag.put(TAG_MASTERY, masteryTag);
        if (lastStimulusType != null) {
            tag.putString(TAG_LAST_STIMULUS, lastStimulusType.id());
        }
        tag.putInt(TAG_STREAK, stimulusStreak);
        tag.putLong(TAG_LAST_STIMULUS_TICK, lastStimulusGameTime);
        ListTag hotspotList = new ListTag();
        for (HiveHotspot hs : hotspots) {
            hotspotList.add(hs.toTag());
        }
        tag.put(TAG_HOTSPOTS, hotspotList);
        CompoundTag habitsTag = new CompoundTag();
        for (Map.Entry<PlayerHabit, Double> entry : habitScores.entrySet()) {
            habitsTag.putDouble(entry.getKey().id(), entry.getValue());
        }
        tag.put(TAG_HABITS, habitsTag);
        if (falseStimulus != null) {
            tag.putString(TAG_FALSE_STIMULUS, falseStimulus.id());
        }
        tag.putLong(TAG_DECEPTION_UNTIL, deceptionUntilGameTime);
        tag.putLong(TAG_DECEPTION_CD_UNTIL, deceptionCooldownUntilGameTime);
        tag.putDouble(TAG_DECEPTION_BOOST, deceptionAppliedBoost);
        tag.putInt(TAG_HIGHEST_TIER, highestTierReached);
        tag.putBoolean(TAG_DEEP_BREACHED, deepMyceliumBreached);
        tag.putBoolean(TAG_MATRIARCH_DEFEATED, matriarchDefeated);
        tag.putLong(TAG_MATRIARCH_REMISSION_UNTIL, matriarchRemissionUntilGameTime);
        tag.putLong(TAG_ACTIVE_TRAIT_REVISION, activeTraitStateRevision);
        tag.putInt(TAG_ACTIVE_TRAIT_MASK, encodeTraitMask(activeTraits));
        tag.putInt(TAG_TIER2_TRAIT_MASK, encodeTraitMask(tier2Traits));
        tag.putInt(TAG_ACTIVE_SYNERGY_MASK, encodeSynergyMask(activeSynergies));

        if (!hyphalChunks.isEmpty()) {
            ListTag hyphalList = new ListTag();
            for (Map.Entry<Long, HyphalChunkData> entry : hyphalChunks.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    hyphalList.add(entry.getValue().toTag(entry.getKey()));
                }
            }
            tag.put(TAG_HYPHAL_CHUNKS, hyphalList);
        }

        if (architecturalSampleCount > 0 || !architecturalBlockCounts.isEmpty()) {
            CompoundTag archTag = new CompoundTag();
            archTag.putString("PrimaryBlock", primaryArchitecturalBlockId);
            archTag.putString("PillarBlock", pillarArchitecturalBlockId);
            archTag.putDouble("RoomSpan", typicalRoomSpan);
            archTag.putDouble("HWRatio", heightToWidthRatio);
            archTag.putDouble("LightDensity", architecturalLightDensity);
            archTag.putInt("SampleCount", architecturalSampleCount);
            archTag.putInt("LightBlocks", architecturalLightBlockCount);
            archTag.putInt("StructuralBlocks", architecturalStructuralBlockCount);
            CompoundTag countsTag = new CompoundTag();
            for (Map.Entry<String, Integer> entry : architecturalBlockCounts.entrySet()) {
                countsTag.putInt(entry.getKey(), entry.getValue());
            }
            archTag.put("BlockCounts", countsTag);
            tag.put(TAG_ARCHITECTURAL_ACCENT, archTag);
        }

        if (!baseAnchors.isEmpty()) {
            ListTag anchorsTag = new ListTag();
            for (BlockPos anchor : baseAnchors) {
                anchorsTag.add(LongTag.valueOf(anchor.asLong()));
            }
            tag.put(TAG_BASE_ANCHORS, anchorsTag);
        }

        if (!blindSpotSites.isEmpty()) {
            ListTag blindList = new ListTag();
            for (BlindSpotOutbreakSite site : blindSpotSites.values()) {
                blindList.add(site.toTag());
            }
            tag.put(TAG_BLIND_SPOT_SITES, blindList);
        }
        return tag;
    }

    public static HiveMindSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HiveMindSavedData data = new HiveMindSavedData();
        data.threatLevel = Mth.clamp(tag.getDouble(TAG_THREAT), 0.0, 100.0);
        data.lastWaveMilestone = tag.getInt(TAG_LAST_MILESTONE);
        data.lastWaveGameTime = tag.getLong(TAG_LAST_WAVE_TICK);
        data.suppressedUntilGameTime = tag.getLong(TAG_SUPPRESSED_UNTIL);
        data.fungalTideUntilGameTime = tag.getLong(TAG_FUNGAL_TIDE_UNTIL);
        data.lastFungalTideGameTime = tag.getLong(TAG_LAST_FUNGAL_TIDE);
        if (tag.contains(TAG_STIMULI)) {
            CompoundTag stimuliTag = tag.getCompound(TAG_STIMULI);
            for (StimulusType type : StimulusType.values()) {
                if (stimuliTag.contains(type.id())) {
                    data.stimulusScores.put(type, Math.max(0.0, stimuliTag.getDouble(type.id())));
                }
            }
        }
        if (tag.contains(TAG_CASUALTIES)) {
            CompoundTag casTag = tag.getCompound(TAG_CASUALTIES);
            for (StimulusType type : StimulusType.values()) {
                if (casTag.contains(type.id())) {
                    data.casualtiesByStimulus.put(type, Math.max(0, casTag.getInt(type.id())));
                }
            }
        }
        if (tag.contains(TAG_MASTERY)) {
            CompoundTag mastTag = tag.getCompound(TAG_MASTERY);
            for (AdaptationTrait trait : AdaptationTrait.values()) {
                if (mastTag.contains(trait.id())) {
                    data.traitMastery.put(trait, Mth.clamp(mastTag.getDouble(trait.id()), 0.0, 1.0));
                }
            }
        }
        if (tag.contains(TAG_LAST_STIMULUS)) {
            String raw = tag.getString(TAG_LAST_STIMULUS);
            for (StimulusType st : StimulusType.values()) {
                if (st.id().equals(raw)) {
                    data.lastStimulusType = st;
                    break;
                }
            }
        }
        data.stimulusStreak = Math.max(0, tag.getInt(TAG_STREAK));
        data.lastStimulusGameTime = tag.getLong(TAG_LAST_STIMULUS_TICK);
        if (tag.contains(TAG_HOTSPOTS, Tag.TAG_LIST)) {
            ListTag list = tag.getList(TAG_HOTSPOTS, Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(MAX_HOTSPOTS, list.size()); i++) {
                data.hotspots.add(HiveHotspot.fromTag(list.getCompound(i)));
            }
        }
        if (tag.contains(TAG_HABITS)) {
            CompoundTag habitsTag = tag.getCompound(TAG_HABITS);
            for (PlayerHabit habit : PlayerHabit.values()) {
                if (habitsTag.contains(habit.id())) {
                    data.habitScores.put(habit, Mth.clamp(habitsTag.getDouble(habit.id()), 0.0, HABIT_MAX_SCORE));
                }
            }
        }
        if (tag.contains(TAG_FALSE_STIMULUS)) {
            String rawFalse = tag.getString(TAG_FALSE_STIMULUS);
            for (StimulusType st : StimulusType.values()) {
                if (st.id().equals(rawFalse)) {
                    data.falseStimulus = st;
                    break;
                }
            }
        }
        data.deceptionUntilGameTime = tag.getLong(TAG_DECEPTION_UNTIL);
        data.deceptionCooldownUntilGameTime = tag.getLong(TAG_DECEPTION_CD_UNTIL);
        data.deceptionAppliedBoost = Math.max(0.0, tag.getDouble(TAG_DECEPTION_BOOST));
        data.highestTierReached = Mth.clamp(tag.getInt(TAG_HIGHEST_TIER), 0, HiveProgressionTier.OVERMIND.level());
        data.deepMyceliumBreached = tag.getBoolean(TAG_DEEP_BREACHED);
        data.matriarchDefeated = tag.getBoolean(TAG_MATRIARCH_DEFEATED);
        data.matriarchRemissionUntilGameTime = Math.max(0L, tag.getLong(TAG_MATRIARCH_REMISSION_UNTIL));
        data.activeTraitStateRevision = Math.max(0L, tag.getLong(TAG_ACTIVE_TRAIT_REVISION));
        decodeTraitMask(tag.getInt(TAG_ACTIVE_TRAIT_MASK), data.activeTraits);
        decodeTraitMask(tag.getInt(TAG_TIER2_TRAIT_MASK), data.tier2Traits);
        decodeSynergyMask(tag.getInt(TAG_ACTIVE_SYNERGY_MASK), data.activeSynergies);

        if (tag.contains(TAG_HYPHAL_CHUNKS, Tag.TAG_LIST)) {
            ListTag hyphalList = tag.getList(TAG_HYPHAL_CHUNKS, Tag.TAG_COMPOUND);
            for (int i = 0; i < hyphalList.size() && data.hyphalChunks.size() < HyphalAdaptiveArchitectureEngine.MAX_TRACKED_ROUTE_CHUNKS; i++) {
                CompoundTag hcTag = hyphalList.getCompound(i);
                HyphalChunkData hc = HyphalChunkData.fromTag(hcTag);
                if (!hc.isEmpty()) {
                    data.hyphalChunks.put(hcTag.getLong("Key"), hc);
                }
            }
        }

        if (tag.contains(TAG_ARCHITECTURAL_ACCENT, Tag.TAG_COMPOUND)) {
            CompoundTag archTag = tag.getCompound(TAG_ARCHITECTURAL_ACCENT);
            data.primaryArchitecturalBlockId = archTag.getString("PrimaryBlock");
            data.pillarArchitecturalBlockId = archTag.getString("PillarBlock");
            data.typicalRoomSpan = archTag.contains("RoomSpan")
                    ? Mth.clamp(archTag.getDouble("RoomSpan"), 2.0, 16.0)
                    : 5.0;
            data.heightToWidthRatio = archTag.contains("HWRatio")
                    ? Mth.clamp(archTag.getDouble("HWRatio"), 0.4, 3.0)
                    : 1.0;
            data.architecturalLightDensity = archTag.contains("LightDensity")
                    ? Mth.clamp(archTag.getDouble("LightDensity"), 0.0, 1.0)
                    : 0.5;
            data.architecturalSampleCount = Math.max(0, archTag.getInt("SampleCount"));
            data.architecturalLightBlockCount = Math.max(0, archTag.getInt("LightBlocks"));
            data.architecturalStructuralBlockCount = Math.max(0, archTag.getInt("StructuralBlocks"));
            if (archTag.contains("BlockCounts", Tag.TAG_COMPOUND)) {
                CompoundTag countsTag = archTag.getCompound("BlockCounts");
                for (String key : countsTag.getAllKeys()) {
                    int val = countsTag.getInt(key);
                    if (val > 0) {
                        data.architecturalBlockCounts.put(key, val);
                    }
                }
            }
        }

        if (tag.contains(TAG_BASE_ANCHORS, Tag.TAG_LIST)) {
            ListTag anchorsTag = tag.getList(TAG_BASE_ANCHORS, Tag.TAG_LONG);
            for (int i = 0; i < anchorsTag.size() && data.baseAnchors.size() < MAX_BASE_ANCHORS; i++) {
                data.baseAnchors.add(BlockPos.of(((LongTag) anchorsTag.get(i)).getAsLong()));
            }
        }

        if (tag.contains(TAG_BLIND_SPOT_SITES, Tag.TAG_LIST)) {
            ListTag blindList = tag.getList(TAG_BLIND_SPOT_SITES, Tag.TAG_COMPOUND);
            for (int i = 0; i < blindList.size() && data.blindSpotSites.size() < HyphalAdaptiveArchitectureEngine.MAX_ACTIVE_BLIND_SPOTS; i++) {
                BlindSpotOutbreakSite site = BlindSpotOutbreakSite.fromTag(blindList.getCompound(i));
                data.blindSpotSites.put(site.getCorePos(), site);
            }
        }

        data.tryAdvanceHighestTier();
        return data;
    }

    private static int encodeTraitMask(Set<AdaptationTrait> traits) {
        int mask = 0;
        for (AdaptationTrait trait : traits) {
            mask |= 1 << trait.ordinal();
        }
        return mask;
    }

    private static void decodeTraitMask(int mask, EnumSet<AdaptationTrait> traits) {
        traits.clear();
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            if ((mask & (1 << trait.ordinal())) != 0) {
                traits.add(trait);
            }
        }
    }

    private static int encodeSynergyMask(Set<HiveSynergy> synergies) {
        int mask = 0;
        for (HiveSynergy synergy : synergies) {
            mask |= 1 << synergy.ordinal();
        }
        return mask;
    }

    private static void decodeSynergyMask(int mask, EnumSet<HiveSynergy> synergies) {
        synergies.clear();
        for (HiveSynergy synergy : HiveSynergy.values()) {
            if ((mask & (1 << synergy.ordinal())) != 0) {
                synergies.add(synergy);
            }
        }
    }
}
