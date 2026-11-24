package com.alex.mycelialmind.colony;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.UUID;

/** Persisted evidence and vocabulary for a single colony site. */
public final class ColonyDialect {
    public record Episode(UUID id, UUID worker, long tick) { }

    private final EnumMap<ColonySiteHabit, Episode> episodes = new EnumMap<>(ColonySiteHabit.class);
    private long lastCallTick = Long.MIN_VALUE;
    private final EnumMap<ColonySiteHabit, Integer> calls = new EnumMap<>(ColonySiteHabit.class);

    public void recordEpisode(ColonySiteHabit habit, UUID worker, long tick) {
        if (habit != ColonySiteHabit.NONE && worker != null) {
            episodes.put(habit, new Episode(UUID.randomUUID(), worker, tick));
        }
    }

    @Nullable
    public Episode episode(ColonySiteHabit habit) {
        return episodes.get(habit);
    }

    public int call(ColonySiteHabit habit) {
        return calls.getOrDefault(habit, -1);
    }

    public boolean teach(ColonySiteHabit habit, int call) {
        if (habit == ColonySiteHabit.NONE || call < 0 || call >= 9) return false;
        for (var entry : calls.entrySet()) {
            if (entry.getKey() != habit && entry.getValue() == call) return false;
        }
        calls.put(habit, call);
        return true;
    }

    public boolean tryEmit(long gameTime) {
        if (lastCallTick != Long.MIN_VALUE && gameTime >= lastCallTick && gameTime - lastCallTick < 200L) return false;
        lastCallTick = gameTime;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("LastCallTick", lastCallTick);
        ListTag entries = new ListTag();
        for (ColonySiteHabit habit : ColonySiteHabit.values()) {
            if (habit == ColonySiteHabit.NONE) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("Habit", habit.ordinal());
            Episode episode = episodes.get(habit);
            if (episode != null) {
                entry.putUUID("Episode", episode.id());
                entry.putUUID("Worker", episode.worker());
                entry.putLong("Tick", episode.tick());
            }
            if (calls.containsKey(habit)) entry.putInt("Call", calls.get(habit));
            entries.add(entry);
        }
        tag.put("Entries", entries);
        return tag;
    }

    public static ColonyDialect load(CompoundTag tag) {
        ColonyDialect dialect = new ColonyDialect();
        if (tag.contains("LastCallTick")) dialect.lastCallTick = tag.getLong("LastCallTick");
        ListTag entries = tag.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            ColonySiteHabit habit = ColonySiteHabit.fromIndex(entry.getInt("Habit"));
            if (habit == ColonySiteHabit.NONE) continue;
            if (entry.hasUUID("Episode") && entry.hasUUID("Worker")) {
                dialect.episodes.put(habit, new Episode(entry.getUUID("Episode"), entry.getUUID("Worker"), entry.getLong("Tick")));
            }
            if (entry.contains("Call")) dialect.teach(habit, entry.getInt("Call"));
        }
        return dialect;
    }
}
