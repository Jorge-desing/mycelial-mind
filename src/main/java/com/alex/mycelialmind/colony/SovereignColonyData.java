package com.alex.mycelialmind.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persistent state snapshot for a single player-owned Sovereign Hive Colony anchored at a Sovereign Nexus.
 */
public final class SovereignColonyData {
    public enum WorkZoneType {
        HARVEST,
        MINE,
        PATROL,
        BUILD;

        public static WorkZoneType fromOrdinalSafe(int ord) {
            WorkZoneType[] vals = values();
            if (ord < 0 || ord >= vals.length) {
                return HARVEST;
            }
            return vals[ord];
        }
    }

    public record WorkerRosterEntry(
            UUID workerId,
            SymbiontCaste caste,
            SymbiontRank rank,
            SymbiontTemperament temperament,
            double nutrition,
            double fatigue,
            double morale,
            boolean standby,
            int xp
    ) {
    }

    public record SiteMemorySnapshot(
            BlockPos sitePos,
            ColonySiteHabit learnedHabit,
            ColonySiteHabit trialHabit,
            int incidentCount,
            int successfulTrials,
            int failedTrials,
            int lastIncidentCode,
            int lastOutcomeCode
    ) {
    }

    public record MemoryTransferSnapshot(
            BlockPos sourcePos,
            BlockPos targetPos,
            ColonySiteHabit habit,
            boolean inTransit
    ) {
    }

    public record ReconstructionWitnessSnapshot(
            UUID witnessId,
            ColonyWitnessMemory.Type type,
            BlockPos memoryPos,
            ColonyReconstructionStatus status
    ) {
    }

    public record ReconstructionSnapshot(
            BlockPos ruinedSite,
            @Nullable BlockPos successorSite,
            long createdTick,
            int requiredMask,
            int restoredMask,
            @Nullable UUID activeWitness,
            int activeType,
            @Nullable UUID lastFailedWitness,
            int lastFailedType,
            int lastFailure,
            List<ReconstructionWitnessSnapshot> witnesses
    ) {
    }

    private static final class MemoryTransfer {
        private final BlockPos sourcePos;
        private final BlockPos targetPos;
        private final ColonySiteHabit habit;
        @Nullable
        private UUID courierId;
        private boolean collected;
        private long lastProgressTick;

        private MemoryTransfer(BlockPos sourcePos, BlockPos targetPos, ColonySiteHabit habit, long gameTime) {
            this.sourcePos = sourcePos.immutable();
            this.targetPos = targetPos.immutable();
            this.habit = habit;
            this.lastProgressTick = gameTime;
        }
    }

    private final UUID colonyId;
    private UUID ownerId;
    private String ownerName;
    private BlockPos nexusPos;
    private boolean administrativeOverride;
    private int tier = 1;
    private int biomassStored = 16;
    private int domainXp = 0;
    private int unlockedTraitMask = 0;
    private double colonyMorale = 70.0D;
    private int recentRaidVictories = 0;
    private int recentWorkerLosses = 0;
    private long lastRaidTick = -10000L;
    private long activeRaidUntilTick = 0L;
    private int activeRaidRemaining = 0;

    private final LinkedHashSet<BlockPos> linkedOutposts = new LinkedHashSet<>();
    private final LinkedHashSet<BlockPos> knownOutposts = new LinkedHashSet<>();
    private final LinkedHashSet<BlockPos> inputChests = new LinkedHashSet<>();
    private final LinkedHashSet<BlockPos> outputChests = new LinkedHashSet<>();
    private final EnumMap<WorkZoneType, LinkedHashSet<BlockPos>> workZones = new EnumMap<>(WorkZoneType.class);
    private final Map<UUID, WorkerRosterEntry> roster = new LinkedHashMap<>();
    private final ColonyOrderBoard orderBoard = new ColonyOrderBoard();
    private final Map<BlockPos, ColonySiteMemory> siteMemories = new LinkedHashMap<>();
    private final Map<BlockPos, ColonyDialect> dialects = new LinkedHashMap<>();
    @Nullable private ColonyDialectLesson dialectLesson;
    private final Map<ColonyWitnessMemory.Key, ColonyWitnessMemory> witnessMemories = new LinkedHashMap<>();
    private final Map<BlockPos, ColonyReconstruction> reconstructions = new LinkedHashMap<>();
    private int archivedReconstructionCount;
    @Nullable
    private MemoryTransfer memoryTransfer;

    public SovereignColonyData(UUID colonyId, UUID ownerId, String ownerName, BlockPos nexusPos) {
        this.colonyId = colonyId;
        this.ownerId = ownerId;
        this.ownerName = ownerName == null || ownerName.isBlank() ? "Sovereign" : ownerName;
        this.nexusPos = nexusPos.immutable();
        for (WorkZoneType type : WorkZoneType.values()) {
            this.workZones.put(type, new LinkedHashSet<>());
        }
    }

    public boolean hasAdministrativeOverride() {
        return administrativeOverride;
    }

    public void enableAdministrativeOverride() {
        administrativeOverride = true;
    }

    public UUID getColonyId() {
        return colonyId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        if (ownerName != null && !ownerName.isBlank()) {
            this.ownerName = ownerName;
        }
    }

    public BlockPos getNexusPos() {
        return nexusPos;
    }

    public void setNexusPos(BlockPos nexusPos) {
        this.nexusPos = nexusPos.immutable();
    }

    public int getTier() {
        return tier;
    }

    public void setTier(int tier) {
        this.tier = SovereignColonyEngine.clampTier(tier);
    }

    public int getBiomassStored() {
        return biomassStored;
    }

    public void setBiomassStored(int biomassStored) {
        this.biomassStored = Math.max(0, Math.min(SovereignColonyEngine.MAX_BIOMASS, biomassStored));
    }

    public int addBiomass(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int before = this.biomassStored;
        setBiomassStored(before + amount);
        return this.biomassStored - before;
    }

    public boolean consumeBiomass(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (this.biomassStored < amount) {
            return false;
        }
        this.biomassStored -= amount;
        return true;
    }

    public int getDomainXp() {
        return domainXp;
    }

    public void addDomainXp(int amount) {
        if (amount > 0) {
            this.domainXp = Math.max(0, this.domainXp + amount);
            this.unlockedTraitMask |= SovereignTrait.computeUnlockedMask(this.domainXp);
        }
    }

    public void setDomainXp(int domainXp) {
        this.domainXp = Math.max(0, domainXp);
        this.unlockedTraitMask |= SovereignTrait.computeUnlockedMask(this.domainXp);
    }

    public int getUnlockedTraitMask() {
        return unlockedTraitMask;
    }

    public boolean hasTrait(SovereignTrait trait) {
        return trait.isUnlockedIn(this.unlockedTraitMask);
    }

    public double getColonyMorale() {
        return colonyMorale;
    }

    public void setColonyMorale(double colonyMorale) {
        this.colonyMorale = Double.isFinite(colonyMorale)
                ? Math.max(0.0D, Math.min(100.0D, colonyMorale))
                : 50.0D;
    }

    public int getEffectiveRadius() {
        return SovereignColonyEngine.getEffectiveRadius(this.tier, this.linkedOutposts.size());
    }

    public int getPopulationCap() {
        return SovereignColonyEngine.getPopulationCap(this.tier);
    }

    public int getRecentRaidVictories() {
        return recentRaidVictories;
    }

    public void recordRaidVictory() {
        this.recentRaidVictories = Math.min(10, this.recentRaidVictories + 1);
        this.activeRaidRemaining = 0;
        this.activeRaidUntilTick = 0L;
    }

    public int getRecentWorkerLosses() {
        return recentWorkerLosses;
    }

    public void recordWorkerLoss() {
        this.recentWorkerLosses = Math.min(10, this.recentWorkerLosses + 1);
    }

    public long getLastRaidTick() {
        return lastRaidTick;
    }

    public void setLastRaidTick(long lastRaidTick) {
        this.lastRaidTick = lastRaidTick;
    }

    public long getActiveRaidUntilTick() {
        return activeRaidUntilTick;
    }

    public void setActiveRaidUntilTick(long activeRaidUntilTick) {
        this.activeRaidUntilTick = activeRaidUntilTick;
    }

    public int getActiveRaidRemaining() {
        return activeRaidRemaining;
    }

    public void setActiveRaidRemaining(int activeRaidRemaining) {
        this.activeRaidRemaining = Math.max(0, activeRaidRemaining);
    }

    public ColonyOrderBoard getOrderBoard() {
        return orderBoard;
    }

    public Set<BlockPos> getLinkedOutposts() {
        return Collections.unmodifiableSet(linkedOutposts);
    }

    public void replaceLinkedOutposts(List<BlockPos> found) {
        List<BlockPos> selected = new ArrayList<>();
        for (BlockPos pos : found) {
            if (selected.size() >= SovereignColonyEngine.MAX_LINKED_OUTPOSTS) {
                break;
            }
            selected.add(pos.immutable());
        }
        this.linkedOutposts.clear();
        this.linkedOutposts.addAll(selected);
        for (BlockPos pos : selected) {
            rememberKnownOutpost(pos);
        }
        if (memoryTransfer != null
                && (!isMemorySite(memoryTransfer.sourcePos) || !isMemorySite(memoryTransfer.targetPos))) {
            memoryTransfer = null;
        }
    }

    private boolean isMemorySite(BlockPos pos) {
        return nexusPos.equals(pos) || linkedOutposts.contains(pos);
    }

    public boolean isKnownOutpost(BlockPos pos) {
        return knownOutposts.contains(pos);
    }

    private void rememberKnownOutpost(BlockPos pos) {
        knownOutposts.add(pos.immutable());
        while (knownOutposts.size() > 32) {
            BlockPos removable = knownOutposts.stream().filter(known -> !linkedOutposts.contains(known))
                    .findFirst().orElse(null);
            if (removable == null) {
                break;
            }
            knownOutposts.remove(removable);
        }
    }

    public List<BlockPos> getMemorySitePositions() {
        List<BlockPos> sites = new ArrayList<>(1 + linkedOutposts.size());
        sites.add(nexusPos);
        for (BlockPos outpost : linkedOutposts) {
            if (!outpost.equals(nexusPos)) {
                sites.add(outpost);
            }
        }
        return List.copyOf(sites);
    }

    public List<SiteMemorySnapshot> getSiteMemorySnapshots() {
        List<SiteMemorySnapshot> snapshots = new ArrayList<>();
        for (BlockPos site : getMemorySitePositions()) {
            ColonySiteMemory memory = siteMemories.get(site);
            if (memory == null) {
                snapshots.add(new SiteMemorySnapshot(site, ColonySiteHabit.NONE, ColonySiteHabit.NONE,
                        0, 0, 0, 0, 0));
            } else {
                snapshots.add(new SiteMemorySnapshot(site, memory.learnedHabit(), memory.trialHabit(),
                        memory.incidentCount(), memory.successfulTrials(), memory.failedTrials(),
                        memory.lastIncidentCode(), memory.lastOutcomeCode()));
            }
        }
        return List.copyOf(snapshots);
    }

    @Nullable
    public BlockPos findNearestMemorySite(BlockPos pos, double maxDistance) {
        BlockPos nearest = null;
        double bestDistance = maxDistance * maxDistance;
        for (BlockPos site : getMemorySitePositions()) {
            double distance = site.distSqr(pos);
            if (distance <= bestDistance) {
                nearest = site;
                bestDistance = distance;
            }
        }
        return nearest;
    }

    @Nullable
    public ColonySiteMemory getSiteMemory(BlockPos sitePos) {
        BlockPos site = sitePos.immutable();
        if (!site.equals(nexusPos) && !linkedOutposts.contains(site)) {
            return null;
        }
        return siteMemories.computeIfAbsent(site, ignored -> new ColonySiteMemory());
    }

    public boolean usesSiteHabit(BlockPos sitePos, ColonySiteHabit habit) {
        ColonySiteMemory memory = getSiteMemory(sitePos);
        return memory != null && memory.usesHabit(habit);
    }

    public boolean startMemoryTransfer(BlockPos sourcePos, BlockPos targetPos, long gameTime) {
        BlockPos source = sourcePos.immutable();
        BlockPos target = targetPos.immutable();
        ColonySiteMemory sourceMemory = getSiteMemory(source);
        ColonySiteMemory targetMemory = getSiteMemory(target);
        if (sourceMemory == null || targetMemory == null || source.equals(target)
                || memoryTransfer != null || sourceMemory.learnedHabit() == ColonySiteHabit.NONE
                || targetMemory.trialHabit() != ColonySiteHabit.NONE) {
            return false;
        }
        if (targetMemory.learnedHabit() == sourceMemory.learnedHabit()) {
            return false;
        }
        memoryTransfer = new MemoryTransfer(source, target, sourceMemory.learnedHabit(), gameTime);
        return true;
    }

    @Nullable
    public MemoryTransferSnapshot getMemoryTransferSnapshot() {
        if (memoryTransfer == null) {
            return null;
        }
        return new MemoryTransferSnapshot(memoryTransfer.sourcePos, memoryTransfer.targetPos,
                memoryTransfer.habit, memoryTransfer.collected);
    }

    public boolean claimMemoryTransfer(UUID courierId, long gameTime) {
        if (memoryTransfer == null) {
            return false;
        }
        if (memoryTransfer.courierId != null && !memoryTransfer.courierId.equals(courierId)) {
            if (gameTime - memoryTransfer.lastProgressTick < 600L) {
                return false;
            }
            memoryTransfer.courierId = null;
            memoryTransfer.collected = false;
        }
        memoryTransfer.courierId = courierId;
        memoryTransfer.lastProgressTick = gameTime;
        return true;
    }

    public boolean isMemoryTransferAssignedTo(UUID courierId) {
        return memoryTransfer != null && courierId.equals(memoryTransfer.courierId);
    }

    @Nullable
    public MemoryTransferSnapshot getMemoryTransferFor(UUID courierId) {
        return isMemoryTransferAssignedTo(courierId) ? getMemoryTransferSnapshot() : null;
    }

    public void touchMemoryTransfer(UUID courierId, long gameTime) {
        if (isMemoryTransferAssignedTo(courierId)) {
            memoryTransfer.lastProgressTick = gameTime;
        }
    }

    public void markMemoryTransferCollected(UUID courierId, long gameTime) {
        if (isMemoryTransferAssignedTo(courierId)) {
            memoryTransfer.collected = true;
            memoryTransfer.lastProgressTick = gameTime;
        }
    }

    public boolean deliverMemoryTransfer(UUID courierId, long gameTime) {
        if (!isMemoryTransferAssignedTo(courierId) || !memoryTransfer.collected
                || gameTime - memoryTransfer.lastProgressTick >= 600L) {
            return false;
        }
        ColonySiteMemory destination = getSiteMemory(memoryTransfer.targetPos);
        boolean delivered = destination != null && destination.receiveTransferredHabit(memoryTransfer.habit);
        memoryTransfer = null;
        return delivered;
    }

    public void releaseMemoryTransferCourier(UUID courierId) {
        if (isMemoryTransferAssignedTo(courierId)) {
            memoryTransfer.courierId = null;
            memoryTransfer.collected = false;
            memoryTransfer.lastProgressTick = 0L;
        }
    }

    public void cancelMemoryTransfer() {
        memoryTransfer = null;
    }

    @Nullable
    public BlockPos findEscortSiteForSentinel(UUID sentinelId, long gameTime) {
        for (Map.Entry<BlockPos, ColonySiteMemory> entry : siteMemories.entrySet()) {
            if (sentinelId.equals(entry.getValue().escortSentinel(gameTime))) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Nullable
    public BlockPos claimNearestEscortForSentinel(BlockPos sentinelPos, UUID sentinelId, long gameTime,
                                                   double maxDistance) {
        BlockPos nearest = null;
        double nearestDistance = maxDistance * maxDistance;
        for (BlockPos site : getMemorySitePositions()) {
            ColonySiteMemory memory = siteMemories.get(site);
            if (memory == null || !memory.hasEscortRequest(gameTime)) {
                continue;
            }
            double distance = site.distSqr(sentinelPos);
            if (distance <= nearestDistance && memory.tryClaimEscortSentinel(sentinelId, gameTime)) {
                nearest = site;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    @Nullable
    public ColonySiteMemory getExistingSiteMemory(BlockPos sitePos) {
        return siteMemories.get(sitePos);
    }

    @Nullable
    public ColonyDialect getDialect(BlockPos sitePos) {
        if (!isMemorySite(sitePos)) return null;
        return dialects.computeIfAbsent(sitePos.immutable(), ignored -> new ColonyDialect());
    }

    public void recordDialectEpisode(net.minecraft.server.level.ServerLevel level, BlockPos sitePos,
                                     ColonySiteHabit habit, com.alex.mycelialmind.entity.ColonySymbiontEntity actor) {
        ColonyDialect dialect = getDialect(sitePos);
        if (dialect == null) return;
        dialect.recordEpisode(habit, actor.getUUID(), level.getGameTime());
        if (dialectLesson != null && dialectLesson.stage() == ColonyDialectLesson.Stage.OBSERVING
                && dialectLesson.source().equals(sitePos) && dialectLesson.meaning() == habit
                && dialectLesson.teacher().equals(actor.getUUID())
                && level.getEntity(dialectLesson.apprentice()) instanceof com.alex.mycelialmind.entity.ColonySymbiontEntity apprentice
                && apprentice.isAlive() && !apprentice.getUUID().equals(actor.getUUID())
                && apprentice.blockPosition().distSqr(sitePos) <= 9.0D
                && apprentice.distanceToSqr(actor) <= 64.0D) {
            dialectLesson.witness(dialect.episode(habit).id());
        }
    }

    @Nullable
    public ColonyDialectLesson getDialectLesson() {
        return dialectLesson;
    }

    public boolean startDialectLesson(BlockPos source, BlockPos target, ColonySiteHabit habit, int call,
                                      UUID apprentice, UUID teacher, boolean coinage) {
        if (dialectLesson != null || source.equals(target) || !linkedOutposts.contains(source) || !linkedOutposts.contains(target)
                || habit == ColonySiteHabit.NONE || call < 0 || call >= 9 || apprentice == null
                || teacher == null || teacher.equals(apprentice) || !roster.containsKey(apprentice)
                || !roster.containsKey(teacher)) return false;
        ColonyDialect sourceDialect = getDialect(source);
        ColonyDialect targetDialect = getDialect(target);
        ColonyDialect.Episode episode = sourceDialect.episode(habit);
        if (episode == null || (coinage && !episode.worker().equals(teacher))
                || (!coinage && sourceDialect.call(habit) != call)
                || (coinage && sourceDialect.call(habit) >= 0)) return false;
        for (ColonySiteHabit other : ColonySiteHabit.values()) {
            if (other != habit && (sourceDialect.call(other) == call || targetDialect.call(other) == call)) return false;
        }
        dialectLesson = new ColonyDialectLesson(source, target, habit, call, apprentice, teacher, episode.id(), coinage);
        return true;
    }

    public boolean finishDialectLesson(UUID apprentice) {
        if (dialectLesson == null || !dialectLesson.apprentice().equals(apprentice)
                || dialectLesson.stage() != ColonyDialectLesson.Stage.RETURNING) return false;
        ColonyDialect source = getDialect(dialectLesson.source());
        ColonyDialect target = getDialect(dialectLesson.target());
        if (source == null || target == null) return false;
        for (ColonySiteHabit other : ColonySiteHabit.values()) {
            if (other != dialectLesson.meaning()
                    && (source.call(other) == dialectLesson.call() || target.call(other) == dialectLesson.call())) return false;
        }
        if (dialectLesson.coinage() && !source.teach(dialectLesson.meaning(), dialectLesson.call())) return false;
        if (!target.teach(dialectLesson.meaning(), dialectLesson.call())) return false;
        dialectLesson = null;
        return true;
    }

    public void cancelDialectLesson() {
        dialectLesson = null;
    }

    public boolean recordWitnessMemory(ColonyWitnessMemory memory) {
        if (memory == null || !isMemorySite(memory.sitePos())) {
            return false;
        }
        ColonyWitnessMemory previous = witnessMemories.put(memory.key(), memory);
        return !memory.equals(previous);
    }

    @Nullable
    public ColonyWitnessMemory getWitnessMemory(UUID witnessId, BlockPos sitePos, ColonyWitnessMemory.Type type) {
        return witnessMemories.get(new ColonyWitnessMemory.Key(witnessId, sitePos, type));
    }

    public List<ColonyWitnessMemory> getWitnessMemories(BlockPos sitePos) {
        List<ColonyWitnessMemory> memories = new ArrayList<>();
        for (ColonyWitnessMemory memory : witnessMemories.values()) {
            if (memory.sitePos().equals(sitePos)) {
                memories.add(memory);
            }
        }
        return List.copyOf(memories);
    }

    public boolean beginOutpostReconstruction(BlockPos ruinedSite, long gameTime) {
        BlockPos ruined = ruinedSite.immutable();
        if (!knownOutposts.contains(ruined) || reconstructions.containsKey(ruined)) {
            return false;
        }
        int requiredMask = 0;
        for (ColonyWitnessMemory memory : getWitnessMemories(ruined)) {
            requiredMask |= ColonyReconstructionRules.restoreBit(memory.type());
        }
        if (reconstructions.size() >= 4) {
            BlockPos closed = reconstructions.values().stream()
                    .filter(ColonyReconstruction::isClosed)
                    .map(ColonyReconstruction::ruinedSite)
                    .findFirst()
                    .orElse(null);
            if (closed == null) {
                return false;
            }
            reconstructions.remove(closed);
            archivedReconstructionCount++;
        }
        linkedOutposts.remove(ruined);
        knownOutposts.remove(ruined);
        ColonyReconstruction entry = new ColonyReconstruction(ruined, gameTime, requiredMask);
        reconstructions.put(ruined, entry);
        if (memoryTransfer != null
                && (memoryTransfer.sourcePos.equals(ruined) || memoryTransfer.targetPos.equals(ruined))) {
            memoryTransfer = null;
        }
        return true;
    }

    public List<ColonyReconstruction> getReconstructionEntries() {
        return List.copyOf(reconstructions.values());
    }

    public int getArchivedReconstructionCount() {
        return archivedReconstructionCount;
    }

    @Nullable
    public ColonyReconstruction getReconstruction(BlockPos ruinedSite) {
        return reconstructions.get(ruinedSite);
    }

    public boolean setReconstructionSuccessor(BlockPos ruinedSite, BlockPos successorSite) {
        ColonyReconstruction entry = reconstructions.get(ruinedSite);
        if (entry == null || !ColonyReconstructionRules.isValidSuccessor(ruinedSite, successorSite, linkedOutposts)) {
            return false;
        }
        return entry.setSuccessor(successorSite);
    }

    public boolean beginReconstructionTrip(BlockPos ruinedSite, UUID witnessId, ColonyWitnessMemory.Type type) {
        ColonyReconstruction entry = reconstructions.get(ruinedSite);
        if (entry == null || getWitnessMemory(witnessId, ruinedSite, type) == null) {
            return false;
        }
        for (ColonyReconstruction other : reconstructions.values()) {
            if (other != entry && other.isAssignedTo(witnessId)) {
                return false;
            }
        }
        return entry.beginTrip(witnessId, type);
    }

    @Nullable
    public ColonyReconstruction findReconstructionForWitness(UUID witnessId) {
        for (ColonyReconstruction entry : reconstructions.values()) {
            if (entry.isAssignedTo(witnessId)) {
                return entry;
            }
        }
        return null;
    }

    public void removeWitnessMemories(UUID witnessId) {
        witnessMemories.entrySet().removeIf(entry -> entry.getKey().witnessId().equals(witnessId));
        for (ColonyReconstruction entry : reconstructions.values()) {
            entry.cancelTripFor(witnessId);
        }
    }

    public boolean abandonReconstruction(BlockPos ruinedSite) {
        return reconstructions.remove(ruinedSite) != null;
    }

    public void recordReconstructionFailure(BlockPos ruinedSite, UUID witnessId, ColonyReconstructionStatus status) {
        ColonyReconstruction entry = reconstructions.get(ruinedSite);
        if (entry != null) {
            entry.failTrip(witnessId, status);
        }
    }

    public void completeReconstructionMemory(BlockPos ruinedSite, UUID witnessId, ColonyWitnessMemory.Type type) {
        ColonyReconstruction entry = reconstructions.get(ruinedSite);
        if (entry != null) {
            entry.completeTrip(witnessId, type);
        }
    }

    @Nullable
    public BlockPos reconstructedPatrolPoint(ColonyReconstruction entry, ColonyWitnessMemory memory) {
        if (entry.successorSite() == null || memory.type() != ColonyWitnessMemory.Type.SENTINEL_PATROL) {
            return null;
        }
        return ColonyReconstructionRules.resolvePatrolPoint(entry.successorSite(), memory.memoryPos());
    }

    public boolean addInputChestRole(BlockPos pos) {
        BlockPos imm = pos.immutable();
        if (inputChests.contains(imm)) {
            return true;
        }
        if (!outputChests.contains(imm) && inputChests.size() >= SovereignColonyEngine.MAX_INPUT_CHESTS) {
            return false;
        }
        outputChests.remove(imm);
        inputChests.add(imm);
        return true;
    }

    public boolean addOutputChestRole(BlockPos pos) {
        BlockPos imm = pos.immutable();
        if (outputChests.contains(imm)) {
            return true;
        }
        if (!inputChests.contains(imm) && outputChests.size() >= SovereignColonyEngine.MAX_OUTPUT_CHESTS) {
            return false;
        }
        inputChests.remove(imm);
        outputChests.add(imm);
        return true;
    }

    public boolean addWorkZoneIfAbsent(WorkZoneType type, BlockPos pos) {
        LinkedHashSet<BlockPos> set = workZones.get(type);
        BlockPos immutable = pos.immutable();
        if (set.contains(immutable)) {
            return true;
        }
        if (set.size() >= SovereignColonyEngine.MAX_WORK_ZONES) {
            return false;
        }
        set.add(immutable);
        return true;
    }

    public Set<BlockPos> getInputChests() {
        return Collections.unmodifiableSet(inputChests);
    }

    public boolean toggleInputChest(BlockPos pos) {
        BlockPos imm = pos.immutable();
        if (inputChests.remove(imm)) {
            return false;
        }
        outputChests.remove(imm);
        if (inputChests.size() >= SovereignColonyEngine.MAX_INPUT_CHESTS) {
            var it = inputChests.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        inputChests.add(imm);
        return true;
    }

    public Set<BlockPos> getOutputChests() {
        return Collections.unmodifiableSet(outputChests);
    }

    public boolean toggleOutputChest(BlockPos pos) {
        BlockPos imm = pos.immutable();
        if (outputChests.remove(imm)) {
            return false;
        }
        inputChests.remove(imm);
        if (outputChests.size() >= SovereignColonyEngine.MAX_OUTPUT_CHESTS) {
            var it = outputChests.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        outputChests.add(imm);
        return true;
    }

    public Set<BlockPos> getWorkZones(WorkZoneType type) {
        return Collections.unmodifiableSet(workZones.get(type));
    }

    public int getTotalWorkZoneCount() {
        int total = 0;
        for (LinkedHashSet<BlockPos> set : workZones.values()) {
            total += set.size();
        }
        return total;
    }

    public boolean toggleWorkZone(WorkZoneType type, BlockPos pos) {
        BlockPos imm = pos.immutable();
        LinkedHashSet<BlockPos> set = workZones.get(type);
        if (set.remove(imm)) {
            return false;
        }
        if (set.size() >= SovereignColonyEngine.MAX_WORK_ZONES) {
            var it = set.iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
            }
        }
        set.add(imm);
        return true;
    }

    public void removeChestOrZoneAt(BlockPos pos) {
        inputChests.remove(pos);
        outputChests.remove(pos);
        for (LinkedHashSet<BlockPos> set : workZones.values()) {
            set.remove(pos);
        }
    }

    public Map<UUID, WorkerRosterEntry> getRoster() {
        return Collections.unmodifiableMap(roster);
    }

    public void upsertWorker(WorkerRosterEntry entry) {
        this.roster.put(entry.workerId(), entry);
    }

    public void removeWorker(UUID workerId) {
        this.roster.remove(workerId);
        this.orderBoard.releaseWorkerReservations(workerId);
    }

    public void onWorkerDeath(UUID workerId) {
        if (dialectLesson != null && (dialectLesson.apprentice().equals(workerId)
                || dialectLesson.teacher().equals(workerId))) cancelDialectLesson();
        removeWitnessMemories(workerId);
        removeWorker(workerId);
    }

    @Nullable
    public WorkerRosterEntry getWorker(UUID workerId) {
        return roster.get(workerId);
    }

    public void recalculateMorale() {
        double avgNut = 80.0D;
        double avgFat = 20.0D;
        if (!roster.isEmpty()) {
            double sumNut = 0.0D;
            double sumFat = 0.0D;
            for (WorkerRosterEntry e : roster.values()) {
                sumNut += e.nutrition();
                sumFat += e.fatigue();
            }
            avgNut = sumNut / roster.size();
            avgFat = sumFat / roster.size();
        }
        this.colonyMorale = SovereignColonyEngine.computeColonyMorale(
                this.biomassStored,
                avgNut,
                avgFat,
                this.linkedOutposts.size(),
                this.recentRaidVictories,
                this.recentWorkerLosses
        );
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("AdministrativeOverride", administrativeOverride);
        tag.putUUID("ColonyId", colonyId);
        tag.putUUID("OwnerId", ownerId);
        tag.putString("OwnerName", ownerName);
        tag.putLong("NexusPos", nexusPos.asLong());
        tag.putInt("Tier", tier);
        tag.putInt("Biomass", biomassStored);
        tag.putInt("DomainXp", domainXp);
        tag.putInt("TraitMask", unlockedTraitMask);
        tag.putDouble("Morale", colonyMorale);
        tag.putInt("RaidVictories", recentRaidVictories);
        tag.putInt("WorkerLosses", recentWorkerLosses);
        tag.putLong("LastRaidTick", lastRaidTick);
        tag.putLong("ActiveRaidUntil", activeRaidUntilTick);
        tag.putInt("ActiveRaidRemaining", activeRaidRemaining);

        tag.put("Outposts", savePosList(linkedOutposts));
        tag.put("KnownOutposts", savePosList(knownOutposts));
        tag.put("InputChests", savePosList(inputChests));
        tag.put("OutputChests", savePosList(outputChests));

        CompoundTag zonesTag = new CompoundTag();
        for (WorkZoneType type : WorkZoneType.values()) {
            zonesTag.put(type.name(), savePosList(workZones.get(type)));
        }
        tag.put("WorkZones", zonesTag);

        ListTag rosterTag = new ListTag();
        for (WorkerRosterEntry entry : roster.values()) {
            CompoundTag w = new CompoundTag();
            w.putUUID("Id", entry.workerId());
            w.putInt("Caste", entry.caste().index());
            w.putInt("Rank", entry.rank().index());
            w.putInt("Temp", entry.temperament().index());
            w.putDouble("Nut", entry.nutrition());
            w.putDouble("Fat", entry.fatigue());
            w.putDouble("Mor", entry.morale());
            w.putBoolean("Standby", entry.standby());
            w.putInt("Xp", entry.xp());
            rosterTag.add(w);
        }
        tag.put("Roster", rosterTag);

        ListTag memoriesTag = new ListTag();
        for (Map.Entry<BlockPos, ColonySiteMemory> entry : siteMemories.entrySet()) {
            CompoundTag memoryTag = entry.getValue().save();
            memoryTag.putLong("SitePos", entry.getKey().asLong());
            memoriesTag.add(memoryTag);
        }
        tag.put("SiteMemories", memoriesTag);
        ListTag dialectTag = new ListTag();
        for (var entry : dialects.entrySet()) {
            CompoundTag site = entry.getValue().save();
            site.putLong("SitePos", entry.getKey().asLong());
            dialectTag.add(site);
        }
        tag.put("Dialects", dialectTag);
        if (dialectLesson != null) tag.put("DialectLesson", dialectLesson.save());
        ListTag witnessTag = new ListTag();
        for (ColonyWitnessMemory memory : witnessMemories.values()) {
            witnessTag.add(memory.save());
        }
        tag.put("SiteWitnessMemories", witnessTag);
        ListTag reconstructionsTag = new ListTag();
        for (ColonyReconstruction reconstruction : reconstructions.values()) {
            reconstructionsTag.add(reconstruction.save());
        }
        tag.put("SiteReconstructions", reconstructionsTag);
        tag.putInt("ArchivedReconstructions", archivedReconstructionCount);
        if (memoryTransfer != null) {
            CompoundTag transferTag = new CompoundTag();
            transferTag.putLong("SourcePos", memoryTransfer.sourcePos.asLong());
            transferTag.putLong("TargetPos", memoryTransfer.targetPos.asLong());
            transferTag.putInt("Habit", memoryTransfer.habit.ordinal());
            transferTag.putBoolean("Collected", memoryTransfer.collected);
            transferTag.putLong("LastProgressTick", memoryTransfer.lastProgressTick);
            if (memoryTransfer.courierId != null) {
                transferTag.putUUID("Courier", memoryTransfer.courierId);
            }
            tag.put("MemoryTransfer", transferTag);
        }
        return tag;
    }

    public static SovereignColonyData load(CompoundTag tag) {
        UUID colId = tag.hasUUID("ColonyId") ? tag.getUUID("ColonyId") : UUID.randomUUID();
        UUID owner = tag.hasUUID("OwnerId") ? tag.getUUID("OwnerId") : new UUID(0L, 0L);
        String name = tag.getString("OwnerName");
        BlockPos pos = BlockPos.of(tag.getLong("NexusPos"));
        SovereignColonyData data = new SovereignColonyData(colId, owner, name, pos);
        data.administrativeOverride = tag.getBoolean("AdministrativeOverride");
        data.setTier(tag.getInt("Tier"));
        data.setBiomassStored(tag.getInt("Biomass"));
        data.setDomainXp(tag.getInt("DomainXp"));
        data.unlockedTraitMask |= (tag.getInt("TraitMask") & SovereignTrait.ALL_MASK);
        data.setColonyMorale(tag.contains("Morale") ? tag.getDouble("Morale") : 70.0D);
        data.recentRaidVictories = Math.max(0, tag.getInt("RaidVictories"));
        data.recentWorkerLosses = Math.max(0, tag.getInt("WorkerLosses"));
        data.lastRaidTick = tag.getLong("LastRaidTick");
        data.activeRaidUntilTick = tag.getLong("ActiveRaidUntil");
        data.activeRaidRemaining = Math.max(0, tag.getInt("ActiveRaidRemaining"));

        loadPosList(tag.getList("Outposts", Tag.TAG_LONG), data.linkedOutposts);
        loadPosList(tag.getList("KnownOutposts", Tag.TAG_LONG), data.knownOutposts);
        data.knownOutposts.addAll(data.linkedOutposts);
        loadPosList(tag.getList("InputChests", Tag.TAG_LONG), data.inputChests);
        loadPosList(tag.getList("OutputChests", Tag.TAG_LONG), data.outputChests);

        if (tag.contains("WorkZones", Tag.TAG_COMPOUND)) {
            CompoundTag zonesTag = tag.getCompound("WorkZones");
            for (WorkZoneType type : WorkZoneType.values()) {
                loadPosList(zonesTag.getList(type.name(), Tag.TAG_LONG), data.workZones.get(type));
            }
        }
        ListTag rosterTag = tag.getList("Roster", Tag.TAG_COMPOUND);
        for (int i = 0; i < rosterTag.size(); i++) {
            CompoundTag w = rosterTag.getCompound(i);
            if (!w.hasUUID("Id")) {
                continue;
            }
            UUID wId = w.getUUID("Id");
            data.roster.put(wId, new WorkerRosterEntry(
                    wId,
                    SymbiontCaste.fromIndex(w.getInt("Caste")),
                    SymbiontRank.fromIndex(w.getInt("Rank")),
                    SymbiontTemperament.fromIndex(w.getInt("Temp")),
                    w.getDouble("Nut"),
                    w.getDouble("Fat"),
                    w.getDouble("Mor"),
                    w.getBoolean("Standby"),
                    w.getInt("Xp")
            ));
        }
        ListTag memoriesTag = tag.getList("SiteMemories", Tag.TAG_COMPOUND);
        for (int i = 0; i < memoriesTag.size(); i++) {
            CompoundTag memoryTag = memoriesTag.getCompound(i);
            data.siteMemories.put(BlockPos.of(memoryTag.getLong("SitePos")), ColonySiteMemory.load(memoryTag));
        }
        ListTag dialectTag = tag.getList("Dialects", Tag.TAG_COMPOUND);
        for (int i = 0; i < dialectTag.size(); i++) {
            CompoundTag site = dialectTag.getCompound(i);
            data.dialects.put(BlockPos.of(site.getLong("SitePos")), ColonyDialect.load(site));
        }
        if (tag.contains("DialectLesson", Tag.TAG_COMPOUND)) {
            CompoundTag lessonTag = tag.getCompound("DialectLesson");
            ColonyDialect sourceDialect = data.dialects.get(BlockPos.of(lessonTag.getLong("Source")));
            ColonySiteHabit meaning = ColonySiteHabit.fromIndex(lessonTag.getInt("Meaning"));
            ColonyDialect.Episode episode = sourceDialect == null ? null : sourceDialect.episode(meaning);
            UUID legacyTeacher = episode == null ? null : episode.worker();
            data.dialectLesson = ColonyDialectLesson.load(lessonTag, legacyTeacher);
        }
        ListTag witnessTag = tag.getList("SiteWitnessMemories", Tag.TAG_COMPOUND);
        for (int i = 0; i < witnessTag.size(); i++) {
            ColonyWitnessMemory memory = ColonyWitnessMemory.load(witnessTag.getCompound(i));
            if (memory != null) {
                data.witnessMemories.put(memory.key(), memory);
            }
        }
        ListTag reconstructionsTag = tag.getList("SiteReconstructions", Tag.TAG_COMPOUND);
        for (int i = 0; i < reconstructionsTag.size() && data.reconstructions.size() < 4; i++) {
            ColonyReconstruction reconstruction = ColonyReconstruction.load(reconstructionsTag.getCompound(i));
            if (reconstruction != null) {
                data.reconstructions.put(reconstruction.ruinedSite(), reconstruction);
            }
        }
        data.archivedReconstructionCount = Math.max(0, tag.getInt("ArchivedReconstructions"));
        if (tag.contains("MemoryTransfer", Tag.TAG_COMPOUND)) {
            CompoundTag transferTag = tag.getCompound("MemoryTransfer");
            BlockPos source = BlockPos.of(transferTag.getLong("SourcePos"));
            BlockPos target = BlockPos.of(transferTag.getLong("TargetPos"));
            ColonySiteHabit habit = ColonySiteHabit.fromIndex(transferTag.getInt("Habit"));
            if (!source.equals(target) && habit != ColonySiteHabit.NONE) {
                MemoryTransfer transfer = new MemoryTransfer(source, target, habit,
                        transferTag.getLong("LastProgressTick"));
                transfer.collected = transferTag.getBoolean("Collected");
                transfer.courierId = transferTag.hasUUID("Courier") ? transferTag.getUUID("Courier") : null;
                data.memoryTransfer = transfer;
            }
        }
        return data;
    }

    private static ListTag savePosList(Set<BlockPos> positions) {
        ListTag list = new ListTag();
        for (BlockPos pos : positions) {
            list.add(LongTag.valueOf(pos.asLong()));
        }
        return list;
    }

    private static void loadPosList(ListTag list, Set<BlockPos> out) {
        for (int i = 0; i < list.size(); i++) {
            Tag elem = list.get(i);
            if (elem instanceof LongTag lt) {
                out.add(BlockPos.of(lt.getAsLong()));
            }
        }
    }
}
