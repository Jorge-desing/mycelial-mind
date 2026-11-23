package com.alex.mycelialmind.hive;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Canonical Overworld-anchored SavedData persisting active and historical
 * Post-Matriarch Hive Cycles, active mutators, and score records across dimensions.
 */
public class HiveCycleSavedData extends SavedData {
    private static final String DATA_NAME = "mycelialmind_hive_cycles";

    private static final String TAG_CYCLE_ACTIVE = "CycleActive";
    private static final String TAG_ACTIVE_CYCLE_NUM = "ActiveCycleNumber";
    private static final String TAG_ACTIVE_MUTATOR_MASK = "ActiveMutatorMask";
    private static final String TAG_START_TIME = "CycleStartGameTime";
    private static final String TAG_DEATHS = "CycleDeaths";
    private static final String TAG_COMPLETED_CYCLES = "CompletedCycles";
    private static final String TAG_BEST_SCORE = "BestScore";
    private static final String TAG_BEST_CYCLE_NUM = "BestCycleNumber";
    private static final String TAG_BEST_MUTATOR_MASK = "BestMutatorMask";
    private static final String TAG_BEST_DURATION = "BestDurationTicks";
    private static final String TAG_LAST_SCORE = "LastCompletedScore";
    private static final String TAG_LAST_GRADE = "LastCompletedGrade";

    public record CycleCompletionResult(
            int cycleNumber,
            int mutatorMask,
            int score,
            String grade,
            long durationTicks,
            boolean newRecord
    ) {
    }

    private boolean cycleActive = false;
    private int activeCycleNumber = 1;
    private int activeMutatorMask = 0;
    private long cycleStartGameTime = 0L;
    private int cycleDeaths = 0;
    private int completedCycles = 0;
    private int bestScore = 0;
    private int bestCycleNumber = 0;
    private int bestMutatorMask = 0;
    private long bestDurationTicks = 0L;
    private int lastCompletedScore = 0;
    private String lastCompletedGrade = "";

    public static HiveCycleSavedData get(ServerLevel level) {
        ServerLevel overworld = level.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                new Factory<>(HiveCycleSavedData::new, HiveCycleSavedData::load),
                DATA_NAME
        );
    }

    public synchronized boolean isCycleActive() {
        return cycleActive;
    }

    public synchronized int getActiveCycleNumber() {
        return cycleActive ? activeCycleNumber : (completedCycles + 1);
    }

    public synchronized int getActiveMutatorMask() {
        return cycleActive ? activeMutatorMask : 0;
    }

    public synchronized long getCycleStartGameTime() {
        return cycleStartGameTime;
    }

    public synchronized int getCycleDeaths() {
        return cycleDeaths;
    }

    public synchronized int getCompletedCycles() {
        return completedCycles;
    }

    public synchronized int getBestScore() {
        return bestScore;
    }

    public synchronized int getBestCycleNumber() {
        return bestCycleNumber;
    }

    public synchronized int getBestMutatorMask() {
        return bestMutatorMask;
    }

    public synchronized long getBestDurationTicks() {
        return bestDurationTicks;
    }

    public synchronized int getLastCompletedScore() {
        return lastCompletedScore;
    }

    public synchronized String getLastCompletedGrade() {
        return lastCompletedGrade;
    }

    public synchronized boolean startCycle(int mutatorMask, long gameTime) {
        int cleanMask = mutatorMask & HiveCycleMutator.ALL_MASK;
        if (!HiveCycleEngine.isValidMutatorSelection(cleanMask)) {
            return false;
        }
        this.cycleActive = true;
        this.activeCycleNumber = this.completedCycles + 1;
        this.activeMutatorMask = cleanMask;
        this.cycleStartGameTime = Math.max(0L, gameTime);
        this.cycleDeaths = 0;
        setDirty();
        return true;
    }

    public synchronized boolean abortCycle() {
        if (!this.cycleActive) {
            return false;
        }
        this.cycleActive = false;
        this.activeMutatorMask = 0;
        this.cycleStartGameTime = 0L;
        this.cycleDeaths = 0;
        setDirty();
        return true;
    }

    public synchronized void recordDeath() {
        if (this.cycleActive) {
            this.cycleDeaths++;
            setDirty();
        }
    }

    public synchronized CycleCompletionResult completeCycle(long gameTime) {
        if (!this.cycleActive) {
            return null;
        }
        int cycleNum = Math.max(1, this.activeCycleNumber);
        int mask = this.activeMutatorMask & HiveCycleMutator.ALL_MASK;
        long duration = Math.max(1L, Math.max(0L, gameTime) - this.cycleStartGameTime);
        int score = HiveCycleEngine.computeCycleScore(cycleNum, mask, duration, this.cycleDeaths);
        String grade = HiveCycleEngine.resolveRankGrade(score);

        this.completedCycles++;
        this.lastCompletedScore = score;
        this.lastCompletedGrade = grade;
        boolean newRecord = score > this.bestScore;
        if (newRecord) {
            this.bestScore = score;
            this.bestCycleNumber = cycleNum;
            this.bestMutatorMask = mask;
            this.bestDurationTicks = duration;
        }

        this.cycleActive = false;
        this.activeMutatorMask = 0;
        this.cycleStartGameTime = 0L;
        this.cycleDeaths = 0;
        this.activeCycleNumber = this.completedCycles + 1;
        setDirty();
        return new CycleCompletionResult(cycleNum, mask, score, grade, duration, newRecord);
    }

    public synchronized void resetAll() {
        this.cycleActive = false;
        this.activeCycleNumber = 1;
        this.activeMutatorMask = 0;
        this.cycleStartGameTime = 0L;
        this.cycleDeaths = 0;
        this.completedCycles = 0;
        this.bestScore = 0;
        this.bestCycleNumber = 0;
        this.bestMutatorMask = 0;
        this.bestDurationTicks = 0L;
        this.lastCompletedScore = 0;
        this.lastCompletedGrade = "";
        setDirty();
    }

    public synchronized void restoreFromSnapshot(CompoundTag tag, HolderLookup.Provider registries) {
        HiveCycleSavedData restored = load(tag, registries);
        this.cycleActive = restored.cycleActive;
        this.activeCycleNumber = restored.activeCycleNumber;
        this.activeMutatorMask = restored.activeMutatorMask;
        this.cycleStartGameTime = restored.cycleStartGameTime;
        this.cycleDeaths = restored.cycleDeaths;
        this.completedCycles = restored.completedCycles;
        this.bestScore = restored.bestScore;
        this.bestCycleNumber = restored.bestCycleNumber;
        this.bestMutatorMask = restored.bestMutatorMask;
        this.bestDurationTicks = restored.bestDurationTicks;
        this.lastCompletedScore = restored.lastCompletedScore;
        this.lastCompletedGrade = restored.lastCompletedGrade;
        setDirty();
    }

    @Override
    public synchronized CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(TAG_CYCLE_ACTIVE, cycleActive);
        tag.putInt(TAG_ACTIVE_CYCLE_NUM, activeCycleNumber);
        tag.putInt(TAG_ACTIVE_MUTATOR_MASK, activeMutatorMask);
        tag.putLong(TAG_START_TIME, cycleStartGameTime);
        tag.putInt(TAG_DEATHS, cycleDeaths);
        tag.putInt(TAG_COMPLETED_CYCLES, completedCycles);
        tag.putInt(TAG_BEST_SCORE, bestScore);
        tag.putInt(TAG_BEST_CYCLE_NUM, bestCycleNumber);
        tag.putInt(TAG_BEST_MUTATOR_MASK, bestMutatorMask);
        tag.putLong(TAG_BEST_DURATION, bestDurationTicks);
        tag.putInt(TAG_LAST_SCORE, lastCompletedScore);
        tag.putString(TAG_LAST_GRADE, lastCompletedGrade != null ? lastCompletedGrade : "");
        return tag;
    }

    public static HiveCycleSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        HiveCycleSavedData data = new HiveCycleSavedData();
        data.cycleActive = tag.getBoolean(TAG_CYCLE_ACTIVE);
        data.activeCycleNumber = Math.max(1, tag.getInt(TAG_ACTIVE_CYCLE_NUM));
        data.activeMutatorMask = tag.getInt(TAG_ACTIVE_MUTATOR_MASK) & HiveCycleMutator.ALL_MASK;
        data.cycleStartGameTime = Math.max(0L, tag.getLong(TAG_START_TIME));
        data.cycleDeaths = Math.max(0, tag.getInt(TAG_DEATHS));
        data.completedCycles = Math.max(0, tag.getInt(TAG_COMPLETED_CYCLES));
        data.bestScore = Math.max(0, tag.getInt(TAG_BEST_SCORE));
        data.bestCycleNumber = Math.max(0, tag.getInt(TAG_BEST_CYCLE_NUM));
        data.bestMutatorMask = tag.getInt(TAG_BEST_MUTATOR_MASK) & HiveCycleMutator.ALL_MASK;
        data.bestDurationTicks = Math.max(0L, tag.getLong(TAG_BEST_DURATION));
        data.lastCompletedScore = Math.max(0, tag.getInt(TAG_LAST_SCORE));
        data.lastCompletedGrade = tag.getString(TAG_LAST_GRADE);
        return data;
    }
}
