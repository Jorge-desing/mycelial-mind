package com.alex.mycelialmind.colony;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
import java.util.UUID;

/** Saved local history and temporary coordination state for one colony site. */
public final class ColonySiteMemory {
    private static final long OBSERVATION_WINDOW_TICKS = 24_000L;
    private static final long PICKUP_WINDOW_TICKS = 20L;

    private ColonySiteHabit learnedHabit = ColonySiteHabit.NONE;
    private ColonySiteHabit trialHabit = ColonySiteHabit.NONE;
    private ColonySiteHabit observedHabit = ColonySiteHabit.NONE;
    private int incidentCount;
    private int successfulTrials;
    private int failedTrials;
    private int lastIncidentCode;
    private int lastOutcomeCode;
    private long lastIncidentTick;

    @Nullable
    private UUID firstPickupWorker;
    @Nullable
    private UUID lastPickupConflictWorker;
    private long firstPickupTick;

    @Nullable
    private UUID pickupLeaseWorker;
    private long pickupLeaseUntil;

    @Nullable
    private UUID escortCourier;
    @Nullable
    private UUID escortSentinel;
    private long escortUntil;

    public ColonySiteHabit learnedHabit() {
        return learnedHabit;
    }

    public ColonySiteHabit trialHabit() {
        return trialHabit;
    }

    public ColonySiteHabit observedHabit() {
        return observedHabit;
    }

    public int incidentCount() {
        return incidentCount;
    }

    public int successfulTrials() {
        return successfulTrials;
    }

    public int failedTrials() {
        return failedTrials;
    }

    public int lastIncidentCode() {
        return lastIncidentCode;
    }

    public int lastOutcomeCode() {
        return lastOutcomeCode;
    }

    public boolean usesHabit(ColonySiteHabit habit) {
        return learnedHabit == habit || trialHabit == habit;
    }

    /** Returns true when this incident starts a new local trial. */
    public boolean recordIncident(ColonySiteHabit habit, int incidentCode, long gameTime) {
        if (habit == ColonySiteHabit.NONE || learnedHabit == habit || trialHabit != ColonySiteHabit.NONE) {
            return false;
        }
        if (observedHabit != habit || gameTime - lastIncidentTick > OBSERVATION_WINDOW_TICKS) {
            observedHabit = habit;
            incidentCount = 1;
        } else {
            incidentCount = Math.min(2, incidentCount + 1);
        }
        lastIncidentCode = incidentCode;
        lastIncidentTick = gameTime;
        if (incidentCount < 2) {
            return false;
        }
        trialHabit = habit;
        successfulTrials = 0;
        failedTrials = 0;
        lastOutcomeCode = 0;
        return true;
    }

    /** Returns true when the first local memory is learned or a new one is ready to adopt. */
    public boolean recordTrialSuccess(ColonySiteHabit habit, int outcomeCode) {
        if (trialHabit != habit || habit == ColonySiteHabit.NONE) {
            return false;
        }
        successfulTrials = Math.min(2, successfulTrials + 1);
        lastOutcomeCode = outcomeCode;
        if (successfulTrials < 2 || learnedHabit != ColonySiteHabit.NONE) {
            return false;
        }
        learnedHabit = trialHabit;
        trialHabit = ColonySiteHabit.NONE;
        observedHabit = ColonySiteHabit.NONE;
        incidentCount = 0;
        successfulTrials = 0;
        failedTrials = 0;
        return true;
    }

    public void recordTrialFailure(ColonySiteHabit habit, int outcomeCode) {
        if (trialHabit == habit) {
            successfulTrials = 0;
            failedTrials = Math.min(99, failedTrials + 1);
            lastOutcomeCode = outcomeCode;
        }
    }

    public boolean adoptTrialHabit() {
        if (trialHabit == ColonySiteHabit.NONE || successfulTrials < 2) {
            return false;
        }
        learnedHabit = trialHabit;
        trialHabit = ColonySiteHabit.NONE;
        observedHabit = ColonySiteHabit.NONE;
        incidentCount = 0;
        successfulTrials = 0;
        failedTrials = 0;
        return true;
    }

    public void forget() {
        learnedHabit = ColonySiteHabit.NONE;
        trialHabit = ColonySiteHabit.NONE;
        observedHabit = ColonySiteHabit.NONE;
        incidentCount = 0;
        successfulTrials = 0;
        failedTrials = 0;
        lastIncidentCode = 0;
        lastOutcomeCode = 0;
        lastIncidentTick = 0L;
        firstPickupWorker = null;
        lastPickupConflictWorker = null;
        firstPickupTick = 0L;
        pickupLeaseWorker = null;
        pickupLeaseUntil = 0L;
        escortCourier = null;
        escortSentinel = null;
        escortUntil = 0L;
    }

    /** Begins a transferred trial at a destination site. */
    public boolean receiveTransferredHabit(ColonySiteHabit habit) {
        if (habit == ColonySiteHabit.NONE || learnedHabit == habit || trialHabit == habit) {
            return false;
        }
        if (trialHabit != ColonySiteHabit.NONE) {
            return false;
        }
        observedHabit = habit;
        incidentCount = 2;
        trialHabit = habit;
        successfulTrials = 0;
        failedTrials = 0;
        lastIncidentCode = 4;
        lastOutcomeCode = 0;
        return true;
    }

    /** Records one distinct pair of couriers converging on the same site. */
    public boolean observePickupArrival(UUID workerId, long gameTime) {
        if (firstPickupWorker == null || gameTime - firstPickupTick > PICKUP_WINDOW_TICKS) {
            firstPickupWorker = workerId;
            firstPickupTick = gameTime;
            lastPickupConflictWorker = null;
            return false;
        }
        if (firstPickupWorker.equals(workerId) || workerId.equals(lastPickupConflictWorker)) {
            return false;
        }
        lastPickupConflictWorker = workerId;
        recordIncident(ColonySiteHabit.STAGGERED_PICKUP, 3, gameTime);
        return true;
    }

    public boolean tryAcquirePickupTurn(UUID workerId, long gameTime, long leaseTicks) {
        if (pickupLeaseWorker == null || gameTime >= pickupLeaseUntil || pickupLeaseWorker.equals(workerId)) {
            pickupLeaseWorker = workerId;
            pickupLeaseUntil = gameTime + leaseTicks;
            return true;
        }
        return false;
    }

    public void releasePickupTurn(UUID workerId) {
        if (workerId.equals(pickupLeaseWorker)) {
            pickupLeaseWorker = null;
            pickupLeaseUntil = 0L;
        }
    }

    public boolean beginCourierEscort(UUID courierId, long gameTime) {
        if (escortCourier != null && gameTime < escortUntil && !escortCourier.equals(courierId)) {
            return false;
        }
        escortCourier = courierId;
        escortSentinel = null;
        escortUntil = gameTime + 600L;
        return true;
    }

    @Nullable
    public UUID escortCourier(long gameTime) {
        if (gameTime >= escortUntil) {
            clearEscort();
            return null;
        }
        return escortCourier;
    }

    @Nullable
    public UUID escortSentinel(long gameTime) {
        if (gameTime >= escortUntil) {
            clearEscort();
            return null;
        }
        return escortSentinel;
    }

    public boolean tryClaimEscortSentinel(UUID sentinelId, long gameTime) {
        if (escortCourier(gameTime) == null) {
            return false;
        }
        if (escortSentinel == null || escortSentinel.equals(sentinelId)) {
            escortSentinel = sentinelId;
            escortUntil = gameTime + 600L;
            return true;
        }
        return false;
    }

    public boolean hasEscortRequest(long gameTime) {
        return escortCourier(gameTime) != null;
    }

    public void clearEscort(UUID courierId) {
        if (courierId.equals(escortCourier)) {
            clearEscort();
        }
    }

    private void clearEscort() {
        escortCourier = null;
        escortSentinel = null;
        escortUntil = 0L;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Learned", learnedHabit.ordinal());
        tag.putInt("Trial", trialHabit.ordinal());
        tag.putInt("Observed", observedHabit.ordinal());
        tag.putInt("Incidents", incidentCount);
        tag.putInt("Successes", successfulTrials);
        tag.putInt("Failures", failedTrials);
        tag.putInt("IncidentCode", lastIncidentCode);
        tag.putInt("OutcomeCode", lastOutcomeCode);
        tag.putLong("IncidentTick", lastIncidentTick);
        putUuid(tag, "FirstPickup", firstPickupWorker);
        putUuid(tag, "PickupConflict", lastPickupConflictWorker);
        tag.putLong("FirstPickupTick", firstPickupTick);
        putUuid(tag, "PickupLease", pickupLeaseWorker);
        tag.putLong("PickupLeaseUntil", pickupLeaseUntil);
        putUuid(tag, "EscortCourier", escortCourier);
        putUuid(tag, "EscortSentinel", escortSentinel);
        tag.putLong("EscortUntil", escortUntil);
        return tag;
    }

    public static ColonySiteMemory load(CompoundTag tag) {
        ColonySiteMemory memory = new ColonySiteMemory();
        memory.learnedHabit = ColonySiteHabit.fromIndex(tag.getInt("Learned"));
        memory.trialHabit = ColonySiteHabit.fromIndex(tag.getInt("Trial"));
        memory.observedHabit = ColonySiteHabit.fromIndex(tag.getInt("Observed"));
        memory.incidentCount = Math.max(0, Math.min(2, tag.getInt("Incidents")));
        memory.successfulTrials = Math.max(0, Math.min(2, tag.getInt("Successes")));
        memory.failedTrials = Math.max(0, Math.min(99, tag.getInt("Failures")));
        memory.lastIncidentCode = Math.max(0, tag.getInt("IncidentCode"));
        memory.lastOutcomeCode = Math.max(0, tag.getInt("OutcomeCode"));
        memory.lastIncidentTick = tag.getLong("IncidentTick");
        memory.firstPickupWorker = getUuid(tag, "FirstPickup");
        memory.lastPickupConflictWorker = getUuid(tag, "PickupConflict");
        memory.firstPickupTick = tag.getLong("FirstPickupTick");
        memory.pickupLeaseWorker = getUuid(tag, "PickupLease");
        memory.pickupLeaseUntil = tag.getLong("PickupLeaseUntil");
        memory.escortCourier = getUuid(tag, "EscortCourier");
        memory.escortSentinel = getUuid(tag, "EscortSentinel");
        memory.escortUntil = tag.getLong("EscortUntil");
        return memory;
    }

    private static void putUuid(CompoundTag tag, String key, @Nullable UUID value) {
        if (value != null) {
            tag.putUUID(key, value);
        }
    }

    @Nullable
    private static UUID getUuid(CompoundTag tag, String key) {
        return tag.hasUUID(key) ? tag.getUUID(key) : null;
    }
}
