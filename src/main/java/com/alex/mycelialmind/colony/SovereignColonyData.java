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

    private final UUID colonyId;
    private UUID ownerId;
    private String ownerName;
    private BlockPos nexusPos;
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
    private final LinkedHashSet<BlockPos> inputChests = new LinkedHashSet<>();
    private final LinkedHashSet<BlockPos> outputChests = new LinkedHashSet<>();
    private final EnumMap<WorkZoneType, LinkedHashSet<BlockPos>> workZones = new EnumMap<>(WorkZoneType.class);
    private final Map<UUID, WorkerRosterEntry> roster = new LinkedHashMap<>();
    private final ColonyOrderBoard orderBoard = new ColonyOrderBoard();

    public SovereignColonyData(UUID colonyId, UUID ownerId, String ownerName, BlockPos nexusPos) {
        this.colonyId = colonyId;
        this.ownerId = ownerId;
        this.ownerName = ownerName == null || ownerName.isBlank() ? "Sovereign" : ownerName;
        this.nexusPos = nexusPos.immutable();
        for (WorkZoneType type : WorkZoneType.values()) {
            this.workZones.put(type, new LinkedHashSet<>());
        }
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
        this.linkedOutposts.clear();
        for (BlockPos pos : found) {
            if (this.linkedOutposts.size() >= SovereignColonyEngine.MAX_LINKED_OUTPOSTS) {
                break;
            }
            this.linkedOutposts.add(pos.immutable());
        }
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
        return tag;
    }

    public static SovereignColonyData load(CompoundTag tag) {
        UUID colId = tag.hasUUID("ColonyId") ? tag.getUUID("ColonyId") : UUID.randomUUID();
        UUID owner = tag.hasUUID("OwnerId") ? tag.getUUID("OwnerId") : new UUID(0L, 0L);
        String name = tag.getString("OwnerName");
        BlockPos pos = BlockPos.of(tag.getLong("NexusPos"));
        SovereignColonyData data = new SovereignColonyData(colId, owner, name, pos);
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
