package com.alex.mycelialmind.hive;

import net.minecraft.util.Mth;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Clean-room per-mob ranked multi-target threat table combining damage attribution,
 * proximity, target vulnerability, episodic grudges, recency decay, and target-switch hysteresis.
 */
public final class MobThreatTable {
    public static final int MAX_ENTRIES = 12;
    public static final long DEFAULT_MAX_TRACKING_TICKS = 400L;
    public static final long RECENCY_FULL_TICKS = 160L;
    public static final double CURRENT_TARGET_HYSTERESIS = 1.15D;
    public static final double GRUDGE_BONUS_MULTIPLIER = 1.25D;

    private static final double DAMAGE_THREAT_SCALE = 2.5D;
    private static final double MAX_PROXIMITY_SCORE = 12.0D;
    private static final double MAX_VULNERABILITY_SCORE = 5.0D;

    private static final class ThreatRecord {
        double accumulatedThreat;
        long lastSeenTick;
        double lastDistSqr;
        float lastHealthRatio;

        ThreatRecord(double accumulatedThreat, long lastSeenTick, double lastDistSqr, float lastHealthRatio) {
            this.accumulatedThreat = accumulatedThreat;
            this.lastSeenTick = lastSeenTick;
            this.lastDistSqr = lastDistSqr;
            this.lastHealthRatio = lastHealthRatio;
        }
    }

    private final Map<UUID, ThreatRecord> entries = new HashMap<>();

    public void recordDamage(@Nullable UUID attackerId, float damage, double distSqr, float healthRatio, long nowTick) {
        if (attackerId == null || !Float.isFinite(damage) || damage <= 0.0F) {
            return;
        }
        double safeDistSqr = sanitizeDistSqr(distSqr);
        float safeHealth = sanitizeHealthRatio(healthRatio);
        ThreatRecord record = entries.get(attackerId);
        if (record == null) {
            evictIfFull(nowTick);
            entries.put(attackerId, new ThreatRecord(damage * DAMAGE_THREAT_SCALE, nowTick, safeDistSqr, safeHealth));
        } else {
            record.accumulatedThreat += damage * DAMAGE_THREAT_SCALE;
            record.lastSeenTick = nowTick;
            record.lastDistSqr = safeDistSqr;
            record.lastHealthRatio = safeHealth;
        }
    }

    public void recordProximity(@Nullable UUID entityId, double distSqr, float healthRatio, long nowTick) {
        if (entityId == null) {
            return;
        }
        double safeDistSqr = sanitizeDistSqr(distSqr);
        float safeHealth = sanitizeHealthRatio(healthRatio);
        ThreatRecord record = entries.get(entityId);
        if (record == null) {
            evictIfFull(nowTick);
            entries.put(entityId, new ThreatRecord(0.0D, nowTick, safeDistSqr, safeHealth));
        } else {
            record.lastSeenTick = nowTick;
            record.lastDistSqr = safeDistSqr;
            record.lastHealthRatio = safeHealth;
        }
    }

    public double computeEffectiveScore(
            @Nullable UUID entityId,
            @Nullable UUID currentTargetId,
            boolean hasGrudge,
            long nowTick
    ) {
        if (entityId == null) {
            return 0.0D;
        }
        ThreatRecord record = entries.get(entityId);
        if (record == null) {
            return 0.0D;
        }
        long age = Math.max(0L, nowTick - record.lastSeenTick);
        if (age >= DEFAULT_MAX_TRACKING_TICKS) {
            return 0.0D;
        }
        double recency = age <= RECENCY_FULL_TICKS
                ? 1.0D
                : 1.0D - ((double) (age - RECENCY_FULL_TICKS)
                / (double) (DEFAULT_MAX_TRACKING_TICKS - RECENCY_FULL_TICKS));

        double dist = Math.sqrt(record.lastDistSqr);
        double proximityScore = Math.max(0.0D, MAX_PROXIMITY_SCORE - dist * 0.5D);
        double vulnerabilityScore = (1.0D - record.lastHealthRatio) * MAX_VULNERABILITY_SCORE;
        double base = (record.accumulatedThreat + proximityScore + vulnerabilityScore) * recency;
        if (hasGrudge) {
            base *= GRUDGE_BONUS_MULTIPLIER;
        }
        if (entityId.equals(currentTargetId)) {
            base *= CURRENT_TARGET_HYSTERESIS;
        }
        return base;
    }

    @Nullable
    public UUID selectHighestThreat(
            @Nullable UUID currentTargetId,
            long nowTick,
            Predicate<UUID> isValid,
            Predicate<UUID> hasGrudge
    ) {
        decayAndPrune(nowTick, DEFAULT_MAX_TRACKING_TICKS);
        UUID bestId = null;
        double bestScore = 0.0D;
        Iterator<Map.Entry<UUID, ThreatRecord>> it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, ThreatRecord> entry = it.next();
            UUID candidateId = entry.getKey();
            if (isValid != null && !isValid.test(candidateId)) {
                it.remove();
                continue;
            }
            boolean grudge = hasGrudge != null && hasGrudge.test(candidateId);
            double score = computeEffectiveScore(candidateId, currentTargetId, grudge, nowTick);
            if (score > bestScore) {
                bestScore = score;
                bestId = candidateId;
            }
        }
        return bestId;
    }

    public void decayAndPrune(long nowTick, long maxTrackingTicks) {
        long limit = Math.max(1L, maxTrackingTicks);
        entries.entrySet().removeIf(e -> Math.max(0L, nowTick - e.getValue().lastSeenTick) >= limit);
    }

    public void invalidate(@Nullable UUID entityId) {
        if (entityId != null) {
            entries.remove(entityId);
        }
    }

    public void reset() {
        entries.clear();
    }

    public double getThreatFor(@Nullable UUID entityId) {
        if (entityId == null) {
            return 0.0D;
        }
        ThreatRecord record = entries.get(entityId);
        return record != null ? record.accumulatedThreat : 0.0D;
    }

    public int size() {
        return entries.size();
    }

    private void evictIfFull(long nowTick) {
        if (entries.size() < MAX_ENTRIES) {
            return;
        }
        decayAndPrune(nowTick, DEFAULT_MAX_TRACKING_TICKS);
        if (entries.size() < MAX_ENTRIES) {
            return;
        }
        entries.entrySet().stream()
                .min(Comparator.comparingLong((Map.Entry<UUID, ThreatRecord> e) -> e.getValue().lastSeenTick)
                        .thenComparingDouble(e -> e.getValue().accumulatedThreat))
                .ifPresent(oldest -> entries.remove(oldest.getKey()));
    }

    private static double sanitizeDistSqr(double distSqr) {
        return Double.isFinite(distSqr) ? Math.max(0.0D, distSqr) : 0.0D;
    }

    private static float sanitizeHealthRatio(float healthRatio) {
        return Float.isFinite(healthRatio) ? Mth.clamp(healthRatio, 0.0F, 1.0F) : 1.0F;
    }
}
