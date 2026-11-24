package com.alex.mycelialmind.colony;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/** One lesson whose apprentice travels, witnesses a new episode, and returns home. */
public final class ColonyDialectLesson {
    public enum Stage { TRAVELLING, OBSERVING, RETURNING }

    private final BlockPos source;
    private final BlockPos target;
    private final ColonySiteHabit meaning;
    private final int call;
    private final UUID apprentice;
    private final UUID teacher;
    private final UUID baselineEpisode;
    private final boolean coinage;
    private Stage stage;
    @Nullable private UUID observedEpisode;

    public ColonyDialectLesson(BlockPos source, BlockPos target, ColonySiteHabit meaning, int call,
                               UUID apprentice, UUID teacher, UUID baselineEpisode, boolean coinage) {
        this.source = source.immutable();
        this.target = target.immutable();
        this.meaning = meaning;
        this.call = call;
        this.apprentice = apprentice;
        this.teacher = teacher;
        this.baselineEpisode = baselineEpisode;
        this.coinage = coinage;
        this.stage = Stage.TRAVELLING;
    }

    public BlockPos source() { return source; }
    public BlockPos target() { return target; }
    public ColonySiteHabit meaning() { return meaning; }
    public int call() { return call; }
    public UUID apprentice() { return apprentice; }
    public UUID teacher() { return teacher; }
    public boolean coinage() { return coinage; }
    public Stage stage() { return stage; }

    public void arriveAtSource() {
        if (stage == Stage.TRAVELLING) stage = Stage.OBSERVING;
    }

    public boolean witness(UUID episodeId) {
        if (stage != Stage.OBSERVING || episodeId == null || episodeId.equals(baselineEpisode)
                || episodeId.equals(observedEpisode)) return false;
        observedEpisode = episodeId;
        stage = Stage.RETURNING;
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("Source", source.asLong());
        tag.putLong("Target", target.asLong());
        tag.putInt("Meaning", meaning.ordinal());
        tag.putInt("Call", call);
        tag.putUUID("Apprentice", apprentice);
        tag.putUUID("Teacher", teacher);
        tag.putUUID("Baseline", baselineEpisode);
        tag.putBoolean("Coinage", coinage);
        tag.putInt("Stage", stage.ordinal());
        if (observedEpisode != null) tag.putUUID("Observed", observedEpisode);
        return tag;
    }

    @Nullable
    public static ColonyDialectLesson load(CompoundTag tag) {
        return load(tag, null);
    }

    @Nullable
    public static ColonyDialectLesson load(CompoundTag tag, @Nullable UUID legacyTeacher) {
        ColonySiteHabit meaning = ColonySiteHabit.fromIndex(tag.getInt("Meaning"));
        int call = tag.getInt("Call");
        int stage = tag.getInt("Stage");
        if (meaning == ColonySiteHabit.NONE || call < 0 || call >= 9 || stage < 0 || stage >= Stage.values().length
                || !tag.hasUUID("Apprentice") || (!tag.hasUUID("Teacher") && legacyTeacher == null) || !tag.hasUUID("Baseline")) return null;
        BlockPos source = BlockPos.of(tag.getLong("Source"));
        BlockPos target = BlockPos.of(tag.getLong("Target"));
        if (source.equals(target)) return null;
        ColonyDialectLesson lesson = new ColonyDialectLesson(source, target, meaning, call,
                tag.getUUID("Apprentice"), tag.hasUUID("Teacher") ? tag.getUUID("Teacher") : legacyTeacher,
                tag.getUUID("Baseline"), tag.getBoolean("Coinage"));
        lesson.stage = Stage.values()[stage];
        if (tag.hasUUID("Observed")) lesson.observedEpisode = tag.getUUID("Observed");
        if (lesson.stage == Stage.RETURNING && lesson.observedEpisode == null) return null;
        return lesson;
    }
}
