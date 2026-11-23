package com.alex.mycelialmind.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

/** Persistent, shared post-Matriarch expedition state for one dimension. */
public final class PostMatriarchExpeditionData extends SavedData {
    private static final String DATA_NAME = "mycelialmind_post_matriarch_expeditions";
    private static final String ACTIVE = "Active";
    private static final String ACTIVE_POS = "ActivePos";
    private static final String OUTCOME = "Outcome";
    private static final String PALETTE = "Palette";
    private static final String PULSES = "Pulses";
    private static final String LAST_PULSE = "LastPulse";
    private static final String LINKED_FLORA = "LinkedFlora";
    private static final String ENCLAVES = "Enclaves";

    private static final SavedData.Factory<PostMatriarchExpeditionData> FACTORY = new SavedData.Factory<>(
            PostMatriarchExpeditionData::new,
            PostMatriarchExpeditionData::load,
            null
    );

    public enum Outcome {
        NONE(0),
        ROUTE(1),
        GARDEN(2);

        private final int id;

        Outcome(int id) {
            this.id = id;
        }

        public int id() {
            return id;
        }

        public static Outcome byId(int id) {
            for (Outcome outcome : values()) {
                if (outcome.id == id) {
                    return outcome;
                }
            }
            return NONE;
        }
    }

    public record Enclave(BlockPos pos, Outcome outcome, int palette, List<BlockPos> floraPositions, String name, BlockPos pylon) {
        public Enclave {
            pos = pos.immutable();
            floraPositions = List.copyOf(floraPositions);
            name = name == null ? "" : name;
            pylon = pylon == null ? null : pylon.immutable();
        }

        public Enclave(BlockPos pos, Outcome outcome, int palette, List<BlockPos> floraPositions) {
            this(pos, outcome, palette, floraPositions, "", null);
        }
    }

    private boolean active;
    private long activePos;
    private Outcome activeOutcome = Outcome.NONE;
    private int activePalette;
    private int pulsesCompleted;
    private long lastPulseGameTime;
    private BlockPos defensePylon;
    private boolean pulseInProgress;
    private final List<UUID> pulseAttackers = new ArrayList<>();
    private final Set<UUID> retiredAttackers = new HashSet<>();
    private final List<BlockPos> linkedFlora = new ArrayList<>();
    private final List<Enclave> enclaves = new ArrayList<>();

    public static PostMatriarchExpeditionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public synchronized boolean hasActiveExpedition() {
        return active;
    }

    /** Resets the active objective only when it still refers to the supplied position. */
    public synchronized boolean abandon(BlockPos expectedPos) {
        if (!active || expectedPos == null || activePos != expectedPos.asLong()) return false;
        retiredAttackers.addAll(pulseAttackers);
        active = false;
        activePos = 0L;
        activeOutcome = Outcome.NONE;
        activePalette = PostMatriarchExpeditionRules.BASE_PALETTE;
        pulsesCompleted = 0;
        lastPulseGameTime = 0L;
        defensePylon = null;
        pulseInProgress = false;
        pulseAttackers.clear();
        linkedFlora.clear();
        setDirty();
        return true;
    }

    public synchronized BlockPos getActivePos() {
        return active ? BlockPos.of(activePos) : null;
    }

    public synchronized Outcome getActiveOutcome() {
        return activeOutcome;
    }

    public synchronized int getActivePalette() {
        return activePalette;
    }

    public synchronized int getPulsesCompleted() {
        return pulsesCompleted;
    }

    public synchronized long getLastPulseGameTime() {
        return lastPulseGameTime;
    }

    public synchronized BlockPos getDefensePylon() {
        return defensePylon;
    }

    public synchronized boolean isPulseInProgress() {
        return pulseInProgress;
    }

    public synchronized List<UUID> getPulseAttackers() {
        return List.copyOf(pulseAttackers);
    }

    public synchronized boolean beginPulse(BlockPos pylon, List<UUID> attackers, long gameTime) {
        if (!active || activeOutcome != Outcome.ROUTE || pulseInProgress || pylon == null
                || attackers.isEmpty() || pulsesCompleted >= PostMatriarchExpeditionRules.REQUIRED_ROUTE_PULSES) {
            return false;
        }
        defensePylon = pylon.immutable();
        pulseAttackers.clear();
        pulseAttackers.addAll(attackers);
        pulseInProgress = true;
        lastPulseGameTime = Math.max(0L, gameTime);
        setDirty();
        return true;
    }

    public synchronized boolean finishPulse(long gameTime) {
        if (!pulseInProgress || !pulseAttackers.isEmpty() || !active || activeOutcome != Outcome.ROUTE) {
            return false;
        }
        pulseInProgress = false;
        pulseAttackers.clear();
        pulsesCompleted++;
        lastPulseGameTime = Math.max(0L, gameTime);
        setDirty();
        return true;
    }

    public synchronized boolean resolveAttacker(UUID attacker) {
        if (!pulseInProgress || !pulseAttackers.remove(attacker)) {
            return false;
        }
        setDirty();
        return true;
    }

    public synchronized List<UUID> interruptPulse() {
        if (!pulseInProgress) return List.of();
        List<UUID> attackers = List.copyOf(pulseAttackers);
        retiredAttackers.addAll(pulseAttackers);
        pulseInProgress = false;
        pulseAttackers.clear();
        setDirty();
        return attackers;
    }

    public synchronized boolean consumeRetiredAttacker(UUID id) {
        if (!retiredAttackers.remove(id)) return false;
        setDirty();
        return true;
    }

    public synchronized boolean bindDefensePylon(BlockPos pylon) {
        if (!active || activeOutcome != Outcome.ROUTE || pulseInProgress || pylon == null) return false;
        defensePylon = pylon == null ? null : pylon.immutable();
        setDirty();
        return true;
    }

    public synchronized List<BlockPos> getLinkedFlora() {
        return List.copyOf(linkedFlora);
    }

    public synchronized List<Enclave> getEnclaves() {
        return List.copyOf(enclaves);
    }

    public synchronized boolean hasCompletedEnclave(BlockPos pos) {
        return enclaves.stream().anyMatch(enclave -> enclave.pos().equals(pos));
    }

    public synchronized boolean renameEnclave(BlockPos pos, String name) {
        if (name == null || name.length() > 32 || name.chars().anyMatch(Character::isISOControl)) {
            return false;
        }
        for (int i = 0; i < enclaves.size(); i++) {
            Enclave enclave = enclaves.get(i);
            if (enclave.pos().equals(pos)) {
                enclaves.set(i, new Enclave(enclave.pos(), enclave.outcome(), enclave.palette(),
                        enclave.floraPositions(), name.strip(), enclave.pylon()));
                setDirty();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isGardenFloraPosition(BlockPos pos) {
        return (active && activeOutcome == Outcome.GARDEN && linkedFlora.contains(pos))
                || enclaves.stream().anyMatch(enclave -> enclave.outcome() == Outcome.GARDEN
                && enclave.floraPositions().contains(pos));
    }

    public synchronized boolean start(BlockPos pos, Outcome outcome, int palette) {
        if (active || outcome == null || outcome == Outcome.NONE || hasCompletedEnclave(pos)) {
            return false;
        }
        active = true;
        activePos = pos.asLong();
        activeOutcome = outcome;
        activePalette = Math.max(PostMatriarchExpeditionRules.BASE_PALETTE,
                Math.min(PostMatriarchExpeditionRules.MAGIC_PALETTE, palette));
        pulsesCompleted = 0;
        lastPulseGameTime = 0L;
        defensePylon = null;
        pulseInProgress = false;
        pulseAttackers.clear();
        linkedFlora.clear();
        setDirty();
        return true;
    }

    public synchronized boolean switchOutcome(Outcome outcome) {
        if (!active || outcome == null || outcome == Outcome.NONE
                || pulsesCompleted > 0 || pulseInProgress || !linkedFlora.isEmpty()) {
            return false;
        }
        if (activeOutcome == outcome) {
            return false;
        }
        activeOutcome = outcome;
        setDirty();
        return true;
    }

    public synchronized boolean recordPulse(long gameTime) {
        if (!active || activeOutcome != Outcome.ROUTE || pulsesCompleted >= PostMatriarchExpeditionRules.REQUIRED_ROUTE_PULSES) {
            return false;
        }
        pulsesCompleted++;
        lastPulseGameTime = Math.max(0L, gameTime);
        setDirty();
        return true;
    }

    public synchronized boolean addFlora(BlockPos pos) {
        if (!active || activeOutcome != Outcome.GARDEN || linkedFlora.size() >= PostMatriarchExpeditionRules.REQUIRED_GARDEN_FOCI
                || linkedFlora.contains(pos)) {
            return false;
        }
        linkedFlora.add(pos.immutable());
        setDirty();
        return true;
    }

    public synchronized Enclave complete() {
        if (!active || activeOutcome == Outcome.NONE
                || (activeOutcome == Outcome.ROUTE
                && pulsesCompleted < PostMatriarchExpeditionRules.REQUIRED_ROUTE_PULSES)
                || (activeOutcome == Outcome.GARDEN
                && !PostMatriarchExpeditionRules.gardenIsComplete(linkedFlora.size()))) {
            return null;
        }
        Enclave enclave = new Enclave(
                BlockPos.of(activePos), activeOutcome, activePalette, linkedFlora, "", defensePylon
        );
        enclaves.add(enclave);
        active = false;
        activePos = 0L;
        activeOutcome = Outcome.NONE;
        activePalette = PostMatriarchExpeditionRules.BASE_PALETTE;
        pulsesCompleted = 0;
        lastPulseGameTime = 0L;
        defensePylon = null;
        pulseInProgress = false;
        pulseAttackers.clear();
        linkedFlora.clear();
        setDirty();
        return enclave;
    }

    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(ACTIVE, active);
        if (active) {
            tag.putLong(ACTIVE_POS, activePos);
            tag.putInt(OUTCOME, activeOutcome.id());
            tag.putInt(PALETTE, activePalette);
            tag.putInt(PULSES, pulsesCompleted);
            tag.putLong(LAST_PULSE, lastPulseGameTime);
            if (defensePylon != null) {
                tag.putLong("DefensePylon", defensePylon.asLong());
            }
            tag.putBoolean("PulseInProgress", pulseInProgress);
            ListTag attackers = new ListTag();
            for (UUID attacker : pulseAttackers) {
                CompoundTag entry = new CompoundTag();
                entry.putUUID("Id", attacker);
                attackers.add(entry);
            }
            tag.put("PulseAttackers", attackers);
            ListTag flora = new ListTag();
            for (BlockPos pos : linkedFlora) {
                flora.add(LongTag.valueOf(pos.asLong()));
            }
            tag.put(LINKED_FLORA, flora);
        }
        ListTag savedEnclaves = new ListTag();
        for (Enclave enclave : enclaves) {
            CompoundTag entry = new CompoundTag();
            entry.putLong(ACTIVE_POS, enclave.pos().asLong());
            entry.putInt(OUTCOME, enclave.outcome().id());
            entry.putInt(PALETTE, enclave.palette());
            entry.putString("Name", enclave.name());
            if (enclave.pylon() != null) entry.putLong("DefensePylon", enclave.pylon().asLong());
            ListTag flora = new ListTag();
            for (BlockPos pos : enclave.floraPositions()) {
                flora.add(LongTag.valueOf(pos.asLong()));
            }
            entry.put(LINKED_FLORA, flora);
            savedEnclaves.add(entry);
        }
        tag.put(ENCLAVES, savedEnclaves);
        ListTag retired = new ListTag();
        for (UUID id : retiredAttackers) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", id);
            retired.add(entry);
        }
        tag.put("RetiredAttackers", retired);
        return tag;
    }

    public static PostMatriarchExpeditionData load(CompoundTag tag, HolderLookup.Provider registries) {
        PostMatriarchExpeditionData data = new PostMatriarchExpeditionData();
        ListTag retired = tag.getList("RetiredAttackers", Tag.TAG_COMPOUND);
        for (int i = 0; i < retired.size(); i++) {
            CompoundTag entry = retired.getCompound(i);
            if (entry.hasUUID("Id")) data.retiredAttackers.add(entry.getUUID("Id"));
        }
        data.active = tag.getBoolean(ACTIVE);
        if (data.active) {
            data.activePos = tag.getLong(ACTIVE_POS);
            data.activeOutcome = Outcome.byId(tag.getInt(OUTCOME));
            data.activePalette = Math.max(0, Math.min(PostMatriarchExpeditionRules.MAGIC_PALETTE, tag.getInt(PALETTE)));
            data.pulsesCompleted = Math.max(0, Math.min(PostMatriarchExpeditionRules.REQUIRED_ROUTE_PULSES, tag.getInt(PULSES)));
            data.lastPulseGameTime = Math.max(0L, tag.getLong(LAST_PULSE));
            if (tag.contains("DefensePylon", Tag.TAG_LONG)) {
                data.defensePylon = BlockPos.of(tag.getLong("DefensePylon"));
            }
            data.pulseInProgress = tag.getBoolean("PulseInProgress");
            ListTag attackers = tag.getList("PulseAttackers", Tag.TAG_COMPOUND);
            for (int i = 0; i < attackers.size(); i++) {
                CompoundTag entry = attackers.getCompound(i);
                if (entry.hasUUID("Id")) {
                    data.pulseAttackers.add(entry.getUUID("Id"));
                }
            }
            readPositions(tag.getList(LINKED_FLORA, Tag.TAG_LONG), data.linkedFlora,
                    PostMatriarchExpeditionRules.REQUIRED_GARDEN_FOCI);
            if (data.activeOutcome == Outcome.NONE) {
                data.active = false;
            }
        }
        ListTag savedEnclaves = tag.getList(ENCLAVES, Tag.TAG_COMPOUND);
        for (int i = 0; i < savedEnclaves.size(); i++) {
            CompoundTag entry = savedEnclaves.getCompound(i);
            Outcome outcome = Outcome.byId(entry.getInt(OUTCOME));
            if (outcome == Outcome.NONE) {
                continue;
            }
            List<BlockPos> flora = new ArrayList<>();
            readPositions(entry.getList(LINKED_FLORA, Tag.TAG_LONG), flora,
                    PostMatriarchExpeditionRules.REQUIRED_GARDEN_FOCI);
            int palette = Math.max(0, Math.min(PostMatriarchExpeditionRules.MAGIC_PALETTE, entry.getInt(PALETTE)));
            String name = entry.getString("Name");
            if (name.length() > 32 || name.chars().anyMatch(Character::isISOControl)) name = "";
            BlockPos pylon = entry.contains("DefensePylon", Tag.TAG_LONG) ? BlockPos.of(entry.getLong("DefensePylon")) : null;
            data.enclaves.add(new Enclave(BlockPos.of(entry.getLong(ACTIVE_POS)), outcome, palette, flora, name, pylon));
        }
        return data;
    }

    private static void readPositions(ListTag tag, List<BlockPos> target, int limit) {
        for (int i = 0; i < tag.size() && target.size() < limit; i++) {
            target.add(BlockPos.of(((LongTag) tag.get(i)).getAsLong()).immutable());
        }
    }
}
