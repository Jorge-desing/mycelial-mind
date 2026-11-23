package com.alex.mycelialmind.network;

import com.alex.mycelialmind.hive.HiveCycleMutator;

import java.util.List;

/**
 * Client-side cache for Post-Matriarch Hive Cycle status, active mutators, and score records.
 */
public final class ClientCycleCache {
    private static boolean initialized = false;
    private static boolean unlocked = false;
    private static boolean cycleActive = false;
    private static int activeCycleNumber = 1;
    private static int activeMutatorMask = 0;
    private static int elapsedSeconds = 0;
    private static int cycleDeaths = 0;
    private static int completedCycles = 0;
    private static int bestScore = 0;
    private static int bestCycleNumber = 0;
    private static int lastCompletedScore = 0;
    private static String lastCompletedGrade = "";

    private ClientCycleCache() {
    }

    public static void clear() {
        initialized = false;
        unlocked = false;
        cycleActive = false;
        activeCycleNumber = 1;
        activeMutatorMask = 0;
        elapsedSeconds = 0;
        cycleDeaths = 0;
        completedCycles = 0;
        bestScore = 0;
        bestCycleNumber = 0;
        lastCompletedScore = 0;
        lastCompletedGrade = "";
    }

    public static void update(CycleProtocolSyncPayload payload) {
        boolean wasInitialized = initialized;
        boolean prevUnlocked = unlocked;
        boolean prevActive = cycleActive;
        int prevCompleted = completedCycles;

        unlocked = payload.unlocked();
        cycleActive = payload.cycleActive();
        activeCycleNumber = Math.max(1, payload.activeCycleNumber());
        activeMutatorMask = payload.activeMutatorMask() & HiveCycleMutator.ALL_MASK;
        elapsedSeconds = Math.max(0, payload.elapsedSeconds());
        cycleDeaths = Math.max(0, payload.cycleDeaths());
        completedCycles = Math.max(0, payload.completedCycles());
        bestScore = Math.max(0, payload.bestScore());
        bestCycleNumber = Math.max(0, payload.bestCycleNumber());
        lastCompletedScore = Math.max(0, payload.lastCompletedScore());
        lastCompletedGrade = payload.lastCompletedGrade() != null ? payload.lastCompletedGrade() : "";
        initialized = true;

        if (!wasInitialized) {
            return;
        }
        if (!prevUnlocked && unlocked) {
            com.alex.mycelialmind.client.screen.ClientUnreadCodexTracker.markUnread(
                    "mundo_dimension", "ciclos_de_la_colmena");
        }
        if (!prevActive && cycleActive) {
            com.alex.mycelialmind.client.hud.DiegeticToastManager.enqueue(
                    com.alex.mycelialmind.client.hud.DiegeticToastManager.ToastCategory.CYCLE_EVENT,
                    net.minecraft.network.chat.Component.translatable("toast.mycelialmind.cycle_started.title", activeCycleNumber),
                    net.minecraft.network.chat.Component.translatable("toast.mycelialmind.cycle_started.sub",
                            HiveCycleMutator.countActive(activeMutatorMask))
            );
        } else if (completedCycles > prevCompleted) {
            com.alex.mycelialmind.client.hud.DiegeticToastManager.enqueue(
                    com.alex.mycelialmind.client.hud.DiegeticToastManager.ToastCategory.CYCLE_EVENT,
                    net.minecraft.network.chat.Component.translatable("toast.mycelialmind.cycle_completed.title",
                            Math.max(1, completedCycles), lastCompletedGrade),
                    net.minecraft.network.chat.Component.translatable("toast.mycelialmind.cycle_completed.sub",
                            lastCompletedScore)
            );
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static boolean isCycleActive() {
        return cycleActive;
    }

    public static int getActiveCycleNumber() {
        return activeCycleNumber;
    }

    public static int getActiveMutatorMask() {
        return activeMutatorMask;
    }

    public static List<HiveCycleMutator> getActiveMutators() {
        return HiveCycleMutator.decodeMask(activeMutatorMask);
    }

    public static int getElapsedSeconds() {
        return elapsedSeconds;
    }

    public static int getCycleDeaths() {
        return cycleDeaths;
    }

    public static int getCompletedCycles() {
        return completedCycles;
    }

    public static int getBestScore() {
        return bestScore;
    }

    public static int getBestCycleNumber() {
        return bestCycleNumber;
    }

    public static int getLastCompletedScore() {
        return lastCompletedScore;
    }

    public static String getLastCompletedGrade() {
        return lastCompletedGrade;
    }
}
