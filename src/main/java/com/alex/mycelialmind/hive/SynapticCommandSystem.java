package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.ColonyWardenEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SynapseWeaverEntity;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

/**
 * Coordinates interruptible telepathic squad command chains, combined-arms Phalanx
 * and dorsal ambush geometry, bounded ballistic velocity prediction, retreat cutoff
 * vectors, and Synaptic Desync propagation.
 */
public final class SynapticCommandSystem {
    public static final double COMMAND_LINK_RADIUS = 20.0D;
    public static final int DESYNC_DURATION_TICKS = 100; // 5 seconds
    public static final int FRENZY_DURATION_TICKS = 160; // 8 seconds
    public static final int CASUALTY_WINDOW_TICKS = 200; // 10 seconds
    public static final double CASUALTY_RADIUS_SQ = 24.0D * 24.0D;
    public static final int CASUALTY_FRENZY_THRESHOLD = 2;
    public static final double MAX_SYNCED_LEAD_SECONDS = 0.75D;
    public static final double SOLO_BASE_LEAD_SECONDS = 0.35D;
    public static final double PHALANX_FRONT_DAMAGE_MULTIPLIER = 0.80F;

    public enum CommandRole {
        NONE((byte) 0),
        LEADER((byte) 1),
        LINKED((byte) 2);

        private final byte id;

        CommandRole(byte id) {
            this.id = id;
        }

        public byte id() {
            return id;
        }

        public static CommandRole byId(byte id) {
            for (CommandRole role : values()) {
                if (role.id == id) {
                    return role;
                }
            }
            return NONE;
        }
    }

    public enum SquadTacticalStatus {
        NONE(0, "NONE"),
        SYNCED(1, "SYNCED"),
        DESYNCED(2, "DESYNCED"),
        FRENZY(3, "FRENZY");

        private final int id;
        private final String label;

        SquadTacticalStatus(int id, String label) {
            this.id = id;
            this.label = label;
        }

        public int id() {
            return id;
        }

        public String label() {
            return label;
        }

        public static SquadTacticalStatus byId(int id) {
            for (SquadTacticalStatus status : values()) {
                if (status.id == id) {
                    return status;
                }
            }
            return NONE;
        }
    }

    private SynapticCommandSystem() {
    }

    public static int leaderPriority(boolean isSynapseWeaver, boolean isColonyWarden, EvolutionRank rank, boolean isBoss) {
        if (isBoss) {
            return -1;
        }
        if (isSynapseWeaver) {
            return 3;
        }
        if (isColonyWarden) {
            return 2;
        }
        if (rank == EvolutionRank.ELITE) {
            return 1;
        }
        return 0;
    }

    public static int leaderPriority(MycelialMob mob) {
        return leaderPriority(
                mob instanceof SynapseWeaverEntity,
                mob instanceof ColonyWardenEntity,
                mob.getEvolutionRank(),
                mob.isBossMob()
        );
    }

    @Nullable
    public static MycelialMob selectSquadLeader(List<MycelialMob> squad) {
        MycelialMob best = null;
        int bestPriority = 0;
        for (MycelialMob candidate : squad) {
            if (candidate == null || !candidate.isAlive() || candidate.isDesynced()) {
                continue;
            }
            int priority = leaderPriority(candidate);
            if (priority <= 0) {
                continue;
            }
            if (best == null || priority > bestPriority
                    || (priority == bestPriority && (candidate.getHealth() > best.getHealth()
                    || (candidate.getHealth() == best.getHealth() && candidate.getId() < best.getId())))) {
                best = candidate;
                bestPriority = priority;
            }
        }
        return best;
    }

    /**
     * Computes a Phalanx escort position guardDistance blocks in front of the ranged ally
     * along the vector toward targetPos.
     */
    public static Vec3 computePhalanxGuardPos(Vec3 rangedPos, Vec3 targetPos, double guardDistance) {
        Vec3 toTarget = new Vec3(targetPos.x - rangedPos.x, 0.0D, targetPos.z - rangedPos.z);
        double len = toTarget.length();
        if (len < 1.0E-4D) {
            return rangedPos;
        }
        double clampedDist = Math.min(Math.max(1.5D, guardDistance), Math.max(1.5D, len * 0.6D));
        Vec3 dir = toTarget.scale(1.0D / len);
        return new Vec3(rangedPos.x + dir.x * clampedDist, rangedPos.y, rangedPos.z + dir.z * clampedDist);
    }

    /**
     * Returns true when flankerPos lies outside the target's frontal field-of-view cone.
     */
    public static boolean isOutsideTargetFov(Vec3 targetPos, Vec3 targetLook, Vec3 flankerPos, double cosThreshold) {
        Vec3 flatLook = new Vec3(targetLook.x, 0.0D, targetLook.z);
        Vec3 flatToFlanker = new Vec3(flankerPos.x - targetPos.x, 0.0D, flankerPos.z - targetPos.z);
        if (flatLook.lengthSqr() < 1.0E-5D || flatToFlanker.lengthSqr() < 1.0E-5D) {
            return false;
        }
        double dot = flatLook.normalize().dot(flatToFlanker.normalize());
        return dot < cosThreshold;
    }

    /**
     * Computes an ambush waypoint behind the target's facing direction.
     */
    public static Vec3 computeDorsalAmbushPos(Vec3 targetPos, Vec3 targetLook, double distance) {
        Vec3 flatLook = new Vec3(targetLook.x, 0.0D, targetLook.z);
        Vec3 dir = flatLook.lengthSqr() > 1.0E-5D ? flatLook.normalize() : new Vec3(1.0D, 0.0D, 0.0D);
        return new Vec3(targetPos.x - dir.x * distance, targetPos.y, targetPos.z - dir.z * distance);
    }

    /**
     * Computes a retreat-cutoff intercept point between targetPos and safeAnchorPos
     * (Purification Pylon or Symbiotic Node) to block the target's escape route.
     */
    public static Vec3 computeRetreatCutoffPos(Vec3 targetPos, Vec3 targetVelocity, Vec3 safeAnchorPos, double interceptAhead) {
        Vec3 toAnchor = new Vec3(safeAnchorPos.x - targetPos.x, 0.0D, safeAnchorPos.z - targetPos.z);
        double distToAnchor = toAnchor.length();
        if (distToAnchor < 1.5D) {
            return safeAnchorPos;
        }
        Vec3 anchorDir = toAnchor.scale(1.0D / distToAnchor);
        double step = Math.min(Math.max(2.0D, interceptAhead), Math.max(2.0D, distToAnchor * 0.65D));
        // Slight lead along target velocity if moving toward anchor
        Vec3 flatVel = new Vec3(targetVelocity.x, 0.0D, targetVelocity.z);
        double movingToward = flatVel.dot(anchorDir);
        if (movingToward > 0.05D) {
            step = Math.min(distToAnchor * 0.75D, step + movingToward * 4.0D);
        }
        return new Vec3(targetPos.x + anchorDir.x * step, targetPos.y, targetPos.z + anchorDir.z * step);
    }

    /**
     * Computes bounded predictive aim target position using target velocity.
     */
    public static Vec3 computeBallisticLeadPos(
            Vec3 shooterPos,
            Vec3 targetPos,
            Vec3 targetVel,
            double projectileSpeed,
            double maxLeadSeconds
    ) {
        if (maxLeadSeconds <= 0.0D || projectileSpeed <= 0.05D) {
            return targetPos;
        }
        double dist = shooterPos.distanceTo(targetPos);
        // Convert blocks/tick velocity to blocks/second (20 ticks/sec) or use tick-aligned time
        double ticksFlight = dist / Math.max(0.25D, projectileSpeed);
        double leadTicks = Mth.clamp(ticksFlight, 0.0D, maxLeadSeconds * 20.0D);
        double leadX = Mth.clamp(targetVel.x * leadTicks, -6.5D, 6.5D);
        double leadY = Mth.clamp(targetVel.y * leadTicks * 0.45D, -0.75D, 1.0D);
        double leadZ = Mth.clamp(targetVel.z * leadTicks, -6.5D, 6.5D);
        return new Vec3(targetPos.x + leadX, targetPos.y + leadY, targetPos.z + leadZ);
    }

    /**
     * Checks whether an incoming attack originates from the defender's frontal cone.
     */
    public static boolean isPhalanxFrontHit(Vec3 defenderLook, Vec3 attackerPos, Vec3 defenderPos) {
        Vec3 flatLook = new Vec3(defenderLook.x, 0.0D, defenderLook.z);
        Vec3 flatToAttacker = new Vec3(attackerPos.x - defenderPos.x, 0.0D, attackerPos.z - defenderPos.z);
        if (flatLook.lengthSqr() < 1.0E-5D || flatToAttacker.lengthSqr() < 1.0E-5D) {
            return false;
        }
        return flatLook.normalize().dot(flatToAttacker.normalize()) > 0.35D;
    }

    /**
     * Breaks command synchronization for allies linked to a fallen squad leader.
     */
    public static int propagateLeaderDesync(ServerLevel level, MycelialMob fallenLeader, int desyncTicks) {
        return propagateLeaderDesync(level, fallenLeader, desyncTicks, ally -> true);
    }

    public static int propagateLeaderDesync(ServerLevel level, MycelialMob fallenLeader, int desyncTicks,
                                            Predicate<MycelialMob> allyFilter) {
        if (fallenLeader.isBossMob()) {
            return 0;
        }
        AABB box = fallenLeader.getBoundingBox().inflate(COMMAND_LINK_RADIUS);
        List<MycelialMob> allies = level.getEntitiesOfClass(MycelialMob.class, box,
                ally -> ally != fallenLeader && ally.isAlive() && !ally.isBossMob() && allyFilter.test(ally));
        int affected = 0;
        for (MycelialMob ally : allies) {
            ally.triggerDesync(desyncTicks);
            affected++;
        }
        if (affected > 0) {
            level.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                    fallenLeader.getX(), fallenLeader.getY(0.6), fallenLeader.getZ(),
                    18, 0.7, 0.5, 0.7, 0.08);
            level.playSound(null, fallenLeader.blockPosition(),
                    ModSounds.SQUAD_DESYNC.get(), SoundSource.HOSTILE, 0.9F, 0.95F);
        }
        return affected;
    }

    /**
     * Summarizes tactical squad state near a player for Bio-Scanner HUD sync:
     * returns {statusId, remainingSeconds}.
     */
    public static int[] summarizeSquadAround(ServerLevel level, BlockPos center, double radius) {
        AABB box = new AABB(center).inflate(radius);
        List<MycelialMob> mobs = level.getEntitiesOfClass(MycelialMob.class, box,
                m -> m.isAlive() && !m.isBossMob());
        if (mobs.isEmpty()) {
            return new int[]{SquadTacticalStatus.NONE.id(), 0};
        }
        int maxFrenzy = 0;
        int maxDesync = 0;
        boolean hasSynced = false;
        for (MycelialMob mob : mobs) {
            if (mob.isDesynced()) {
                maxDesync = Math.max(maxDesync, mob.getDesyncTicks());
            } else {
                if (mob.isFrenzyActive()) {
                    maxFrenzy = Math.max(maxFrenzy, mob.getFrenzyTicks());
                }
                if (mob.getCommandRole() != CommandRole.NONE) {
                    hasSynced = true;
                }
            }
        }
        if (maxFrenzy > 0) {
            return new int[]{SquadTacticalStatus.FRENZY.id(), (maxFrenzy + 19) / 20};
        }
        if (maxDesync > 0) {
            return new int[]{SquadTacticalStatus.DESYNCED.id(), (maxDesync + 19) / 20};
        }
        if (hasSynced) {
            return new int[]{SquadTacticalStatus.SYNCED.id(), 0};
        }
        return new int[]{SquadTacticalStatus.NONE.id(), 0};
    }

    public static final float BOSS_WEAK_POINT_MULTIPLIER = 1.35F;
    public static final float BOSS_CARAPACE_MULTIPLIER = 0.75F;

    /**
     * Pure damage multiplier calculation for multi-part boss sub-hitboxes.
     */
    public static float computeBossPartDamage(
            boolean isWeakPoint,
            boolean isArmoredCarapace,
            boolean bypassesArmor,
            boolean isMagic,
            float baseAmount
    ) {
        if (Float.isNaN(baseAmount) || baseAmount <= 0.0F) {
            return 0.0F;
        }
        if (isWeakPoint) {
            return baseAmount * BOSS_WEAK_POINT_MULTIPLIER;
        }
        if (isArmoredCarapace && !bypassesArmor && !isMagic) {
            return baseAmount * BOSS_CARAPACE_MULTIPLIER;
        }
        return baseAmount;
    }
}
