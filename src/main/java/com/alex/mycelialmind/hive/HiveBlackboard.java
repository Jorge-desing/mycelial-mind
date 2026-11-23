package com.alex.mycelialmind.hive;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight per-dimension Hive Mind blackboard sharing recent target sightings,
 * strategic HiveDoctrine, and active HiveOrder across scouts and units.
 */
public final class HiveBlackboard {
    private static final long TARGET_TTL_TICKS = 200L;
    private static final long NODE_LOSS_TTL_TICKS = 300L;

    // A dimension key is shared by every save. Scope transient reports to the
    // actual level instance so opening a different world cannot inherit them.
    private static final Map<ServerLevel, TargetReport> RECENT_TARGETS = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, StrategicState> STRATEGIC_STATES = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, HiveInfluenceMap> INFLUENCE_MAPS = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, DirectorStressState> DIRECTOR_STRESS = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<TargetReport>> RECENT_CASUALTIES = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<CarrionSite>> RECENT_CARRION = new ConcurrentHashMap<>();
    private static final Map<ServerLevel, List<TacticalAlarm>> TACTICAL_ALARMS = new ConcurrentHashMap<>();

    public enum PacingPhase {
        BUILD_UP,
        PEAK,
        RELAXATION
    }

    public record TacticalAlarm(BlockPos origin, double radiusSqr, long expiresAtTick) {
    }

    public record CarrionSite(Vec3 pos, long expiresAtTick) {
    }

    public record CarrionSnapshot(boolean existed, List<CarrionSite> sites) {
        public CarrionSnapshot {
            sites = List.copyOf(sites);
        }
    }

    private record TargetReport(BlockPos pos, long gameTime) {
    }

    private record DirectorStressState(float stress, long relaxationUntilTick) {
    }

    private record StrategicState(
            HiveDoctrine doctrine,
            HiveOrder order,
            @Nullable BlockPos rallyNodePos,
            long nodeLossGameTime
    ) {
    }

    private HiveBlackboard() {
    }

    public static void reportTarget(ServerLevel level, BlockPos targetPos) {
        RECENT_TARGETS.put(level, new TargetReport(targetPos.immutable(), level.getGameTime()));
        getInfluenceMap(level).addInfluence(targetPos, 0.0F, 0.25F, 0.35F);
        recordCombatStress(level, 0.08F);
    }

    public static HiveInfluenceMap getInfluenceMap(ServerLevel level) {
        return INFLUENCE_MAPS.computeIfAbsent(level, ignored -> new HiveInfluenceMap());
    }

    public static void recordCombatStress(ServerLevel level, float delta) {
        long now = level.getGameTime();
        DirectorStressState prev = DIRECTOR_STRESS.getOrDefault(level, new DirectorStressState(0.0F, 0L));
        float nextStress = Mth.clamp(prev.stress() + delta, 0.0F, 1.0F);
        long relaxUntil = prev.relaxationUntilTick();
        if (nextStress >= 0.90F && now >= relaxUntil) {
            relaxUntil = now + 400L;
        }
        DIRECTOR_STRESS.put(level, new DirectorStressState(nextStress, relaxUntil));
    }

    public static void stepDirectorAndInfluence(ServerLevel level) {
        getInfluenceMap(level).stepDecay(0.92F);
        DirectorStressState prev = DIRECTOR_STRESS.get(level);
        if (prev != null) {
            float decayed = Mth.clamp(prev.stress() * 0.94F, 0.0F, 1.0F);
            DIRECTOR_STRESS.put(level, new DirectorStressState(decayed, prev.relaxationUntilTick()));
        }
    }

    public static float getCombatStress(ServerLevel level) {
        DirectorStressState state = DIRECTOR_STRESS.get(level);
        return state != null ? state.stress() : 0.0F;
    }

    public static PacingPhase getPacingPhase(ServerLevel level) {
        DirectorStressState state = DIRECTOR_STRESS.get(level);
        if (state == null) {
            return PacingPhase.BUILD_UP;
        }
        if (level.getGameTime() < state.relaxationUntilTick()) {
            return PacingPhase.RELAXATION;
        }
        return state.stress() >= 0.65F ? PacingPhase.PEAK : PacingPhase.BUILD_UP;
    }

    @Nullable
    public static BlockPos getRecentTarget(ServerLevel level) {
        TargetReport report = RECENT_TARGETS.get(level);
        if (report == null) {
            return null;
        }
        if (level.getGameTime() - report.gameTime() > TARGET_TTL_TICKS) {
            RECENT_TARGETS.remove(level, report);
            return null;
        }
        return report.pos();
    }

    public static void reportNodeLoss(ServerLevel level, BlockPos lostNodePos) {
        long now = level.getGameTime();
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        HiveDoctrine doctrine = HiveDoctrine.evaluate(hive, now, true);
        getInfluenceMap(level).addInfluence(lostNodePos, -0.35F, 0.50F, 0.65F);
        recordCombatStress(level, 0.18F);
        STRATEGIC_STATES.put(level, new StrategicState(
                doctrine,
                doctrine.defaultOrder(),
                lostNodePos.immutable(),
                now
        ));
    }

    public static void updateStrategicState(ServerLevel level, HiveMindSavedData hive) {
        long now = level.getGameTime();
        StrategicState prev = STRATEGIC_STATES.get(level);
        boolean recentLoss = prev != null && prev.nodeLossGameTime() > 0L
                && (now - prev.nodeLossGameTime()) <= NODE_LOSS_TTL_TICKS;
        HiveDoctrine doctrine = HiveDoctrine.evaluate(hive, now, recentLoss);
        STRATEGIC_STATES.put(level, new StrategicState(
                doctrine,
                doctrine.defaultOrder(),
                recentLoss && prev != null ? prev.rallyNodePos() : null,
                recentLoss && prev != null ? prev.nodeLossGameTime() : 0L
        ));
    }

    public static HiveDoctrine getDoctrine(ServerLevel level) {
        StrategicState state = STRATEGIC_STATES.get(level);
        return state != null ? state.doctrine() : HiveDoctrine.DORMANT;
    }

    public static HiveOrder getOrder(ServerLevel level) {
        StrategicState state = STRATEGIC_STATES.get(level);
        return state != null ? state.order() : HiveOrder.NONE;
    }

    @Nullable
    public static BlockPos getRallyNodePos(ServerLevel level) {
        StrategicState state = STRATEGIC_STATES.get(level);
        if (state == null || state.rallyNodePos() == null) {
            return null;
        }
        if (level.getGameTime() - state.nodeLossGameTime() > NODE_LOSS_TTL_TICKS) {
            return null;
        }
        return state.rallyNodePos();
    }

    public static void recordSquadCasualty(ServerLevel level, BlockPos pos) {
        long now = level.getGameTime();
        List<TargetReport> list = RECENT_CASUALTIES.computeIfAbsent(level,
                ignored -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.removeIf(r -> (now - r.gameTime()) > SynapticCommandSystem.CASUALTY_WINDOW_TICKS);
            if (list.size() >= 32) {
                list.remove(0);
            }
            list.add(new TargetReport(pos.immutable(), now));
        }
    }

    public static int countRecentCasualtiesNear(ServerLevel level, BlockPos center, double radiusSq, long windowTicks) {
        List<TargetReport> list = RECENT_CASUALTIES.get(level);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        long now = level.getGameTime();
        int count = 0;
        synchronized (list) {
            list.removeIf(r -> (now - r.gameTime()) > Math.max(windowTicks, SynapticCommandSystem.CASUALTY_WINDOW_TICKS));
            for (TargetReport report : list) {
                if ((now - report.gameTime()) <= windowTicks && report.pos().distSqr(center) <= radiusSq) {
                    count++;
                }
            }
        }
        return count;
    }

    public static void registerCarrion(ServerLevel level, Vec3 pos, int ttlTicks) {
        long now = level.getGameTime();
        List<CarrionSite> list = RECENT_CARRION.computeIfAbsent(level,
                ignored -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.removeIf(s -> now >= s.expiresAtTick());
            if (list.size() >= 24) {
                list.remove(0);
            }
            list.add(new CarrionSite(pos, now + Math.max(20, ttlTicks)));
        }
    }

    public static CarrionSnapshot snapshotCarrionState(ServerLevel level) {
        List<CarrionSite> sites = RECENT_CARRION.get(level);
        if (sites == null) {
            return new CarrionSnapshot(false, List.of());
        }
        synchronized (sites) {
            return new CarrionSnapshot(true, sites);
        }
    }

    public static void clearCarrionState(ServerLevel level) {
        RECENT_CARRION.remove(level);
    }

    public static void restoreCarrionState(ServerLevel level, CarrionSnapshot snapshot) {
        if (!snapshot.existed()) {
            RECENT_CARRION.remove(level);
            return;
        }
        RECENT_CARRION.put(level,
                Collections.synchronizedList(new ArrayList<>(snapshot.sites())));
    }

    @Nullable
    public static Vec3 findNearestCarrion(ServerLevel level, Vec3 origin, double maxRadius) {
        List<CarrionSite> list = RECENT_CARRION.get(level);
        if (list == null || list.isEmpty()) {
            return null;
        }
        long now = level.getGameTime();
        double maxDistSq = maxRadius * maxRadius;
        Vec3 best = null;
        double bestSq = Double.MAX_VALUE;
        synchronized (list) {
            list.removeIf(s -> now >= s.expiresAtTick());
            for (CarrionSite site : list) {
                double dSq = site.pos().distanceToSqr(origin);
                if (dSq <= maxDistSq && dSq < bestSq) {
                    bestSq = dSq;
                    best = site.pos();
                }
            }
        }
        return best;
    }

    @Nullable
    public static Vec3 consumeNearestCarrion(ServerLevel level, Vec3 origin, double maxRadius) {
        List<CarrionSite> list = RECENT_CARRION.get(level);
        if (list == null || list.isEmpty()) {
            return null;
        }
        long now = level.getGameTime();
        double maxDistSq = maxRadius * maxRadius;
        int bestIdx = -1;
        double bestSq = Double.MAX_VALUE;
        synchronized (list) {
            list.removeIf(s -> now >= s.expiresAtTick());
            for (int i = 0; i < list.size(); i++) {
                double dSq = list.get(i).pos().distanceToSqr(origin);
                if (dSq <= maxDistSq && dSq < bestSq) {
                    bestSq = dSq;
                    bestIdx = i;
                }
            }
            if (bestIdx >= 0) {
                return list.remove(bestIdx).pos();
            }
        }
        return null;
    }

    public static int purgeCarrionWithin(ServerLevel level, Vec3 center, double radius) {
        if (radius <= 0.0D) {
            return 0;
        }
        List<CarrionSite> list = RECENT_CARRION.get(level);
        if (list == null || list.isEmpty()) {
            return 0;
        }
        long now = level.getGameTime();
        double maxDistSq = radius * radius;
        int removed = 0;
        synchronized (list) {
            var it = list.iterator();
            while (it.hasNext()) {
                CarrionSite site = it.next();
                if (now >= site.expiresAtTick()) {
                    it.remove();
                    continue;
                }
                if (site.pos().distanceToSqr(center) <= maxDistSq) {
                    it.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    public static void broadcastTacticalAlarm(ServerLevel level, BlockPos origin, double radius, int durationTicks) {
        if (level == null || origin == null || radius <= 0.0D || durationTicks <= 0) {
            return;
        }
        long now = level.getGameTime();
        List<TacticalAlarm> list = TACTICAL_ALARMS.computeIfAbsent(level,
                ignored -> Collections.synchronizedList(new ArrayList<>()));
        synchronized (list) {
            list.removeIf(alarm -> now >= alarm.expiresAtTick());
            while (list.size() >= 8) {
                list.remove(0);
            }
            list.add(new TacticalAlarm(origin.immutable(), radius * radius, now + durationTicks));
        }
    }

    @Nullable
    public static TacticalAlarm getNearestActiveAlarm(ServerLevel level, BlockPos mobPos) {
        if (level == null || mobPos == null) {
            return null;
        }
        List<TacticalAlarm> list = TACTICAL_ALARMS.get(level);
        if (list == null || list.isEmpty()) {
            return null;
        }
        long now = level.getGameTime();
        TacticalAlarm best = null;
        double bestDistSq = Double.MAX_VALUE;
        synchronized (list) {
            list.removeIf(alarm -> now >= alarm.expiresAtTick());
            for (TacticalAlarm alarm : list) {
                double distSq = alarm.origin().distSqr(mobPos);
                if (distSq <= alarm.radiusSqr() && distSq < bestDistSq) {
                    bestDistSq = distSq;
                    best = alarm;
                }
            }
        }
        return best;
    }

    public static void clear(ServerLevel level) {
        RECENT_TARGETS.remove(level);
        STRATEGIC_STATES.remove(level);
        INFLUENCE_MAPS.remove(level);
        DIRECTOR_STRESS.remove(level);
        RECENT_CASUALTIES.remove(level);
        RECENT_CARRION.remove(level);
        TACTICAL_ALARMS.remove(level);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            clear(level);
        }
    }
}
