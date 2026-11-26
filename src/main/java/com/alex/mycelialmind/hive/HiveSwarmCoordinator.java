package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.entity.AcidSpitterEntity;
import com.alex.mycelialmind.entity.BurrowerEntity;
import com.alex.mycelialmind.entity.ChitinBruteEntity;
import com.alex.mycelialmind.entity.ColonyWardenEntity;
import com.alex.mycelialmind.entity.GoliathCarrierEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.MycelialStalkerEntity;
import com.alex.mycelialmind.entity.SporeBehemothEntity;
import com.alex.mycelialmind.entity.SynapseWeaverEntity;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainSignal;
import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.ai.brain.steering.HiveSteeringEngine;
import com.alex.mycelialmind.world.MycelialFastNoise;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Coordinates dynamic swarm roles, angular attack-slot distribution, pincer flanking geometry,
 * clean-room {@link HiveSteeringEngine} vectors, and 20-block AlertCalls.
 */
public final class HiveSwarmCoordinator {
    public static final double ALERT_CALL_RADIUS = 20.0D;
    private static final int MAX_STEERING_NEIGHBORS = 24;
    private static final ThreadLocal<NeighborScratch> NEIGHBOR_SCRATCH =
            ThreadLocal.withInitial(NeighborScratch::new);

    private HiveSwarmCoordinator() {
    }

    public static SwarmRole assignRole(MycelialMob mob) {
        if (mob instanceof ChitinBruteEntity || mob instanceof ColonyWardenEntity || mob instanceof SporeBehemothEntity) {
            return SwarmRole.FRONT_ANCHOR;
        }
        if (mob instanceof SynapseWeaverEntity || mob instanceof GoliathCarrierEntity) {
            return SwarmRole.SUPPORT_REAR;
        }
        return (mob.getId() & 1) == 0 ? SwarmRole.FLANK_LEFT : SwarmRole.FLANK_RIGHT;
    }

    public static Vec3 computeSwarmApproachPos(MycelialMob mob, LivingEntity target) {
        if (mob.isDesynced()) {
            double jitterAngle = ((mob.getId() * 73 + mob.tickCount / 10) % 360) * (Math.PI / 180.0D);
            return mob.position().add(Math.cos(jitterAngle) * 3.5D, 0.0D, Math.sin(jitterAngle) * 3.5D);
        }

        SwarmRole role = assignRole(mob);
        Vec3 offset = mob.position().subtract(target.position());
        Vec3 flat = new Vec3(offset.x, 0.0, offset.z);
        Vec3 radial = flat.lengthSqr() > 1.0E-4D ? flat.normalize() : new Vec3(1.0, 0.0, 0.0);

        Vec3 basePos = switch (role) {
            case FRONT_ANCHOR -> {
                if (mob.isPhalanxActive() && mob.level() instanceof ServerLevel sl) {
                    List<MycelialMob> ranged = sl.getEntitiesOfClass(
                            MycelialMob.class,
                            mob.getBoundingBox().inflate(12.0D),
                            ally -> ally.isAlive() && (ally instanceof AcidSpitterEntity || ally instanceof SynapseWeaverEntity));
                    if (!ranged.isEmpty()) {
                        yield SynapticCommandSystem.computePhalanxGuardPos(
                                ranged.get(0).position(), target.position(), 3.0D);
                    }
                }
                if (mob.level() instanceof ServerLevel sl) {
                    Vec3 slotted = computeSlotTargetForSquad(mob, target, sl, 1.8D);
                    if (slotted != null) {
                        yield slotted;
                    }
                }
                yield target.position();
            }
            case SUPPORT_REAR -> target.position().add(radial.scale(6.0D));
            case FLANK_LEFT, FLANK_RIGHT -> {
                Vec3 tactical = null;
                if (mob.getCommandRole() != SynapticCommandSystem.CommandRole.NONE
                        && mob.level() instanceof ServerLevel sl) {
                    PurificationZoneTracker tracker = PurificationZoneTracker.get(sl);
                    var pylons = tracker.getPylonsWithin(target.blockPosition(), 48, 1);
                    BlockPos safeAnchor = pylons.isEmpty() ? null : pylons.get(0);
                    if (safeAnchor == null) {
                        for (var entry : tracker.getNodesWithin(target.blockPosition(), 48, 8)) {
                            if (entry.getValue() == SynapticNodeType.SYMBIOTIC) {
                                safeAnchor = entry.getKey();
                                break;
                            }
                        }
                    }
                    Vec3 targetVel = target.getDeltaMovement();
                    boolean woundedOrRetreating = target.getHealth() < target.getMaxHealth() * 0.55F
                            || targetVel.horizontalDistanceSqr() > 0.008D;
                    if (safeAnchor != null && woundedOrRetreating) {
                        tactical = SynapticCommandSystem.computeRetreatCutoffPos(
                                target.position(), targetVel, Vec3.atCenterOf(safeAnchor), 4.0D);
                    } else if ((mob instanceof MycelialStalkerEntity || mob instanceof BurrowerEntity)
                            && SynapticCommandSystem.isOutsideTargetFov(
                                    target.position(), target.getLookAngle(), mob.position(), 0.15D)) {
                        tactical = SynapticCommandSystem.computeDorsalAmbushPos(
                                target.position(), target.getLookAngle(), 3.2D);
                    }
                }
                if (tactical != null) {
                    yield tactical;
                }
                double deg = role == SwarmRole.FLANK_LEFT ? 60.0 : -60.0;
                Vec3 rot = rotateY(radial, Math.toRadians(deg));
                yield target.position().add(rot.scale(4.5D));
            }
        };
        if (mob.level() instanceof ServerLevel serverLevel) {
            var map = HiveBlackboard.getInfluenceMap(serverLevel);
            var cell = map.sample(mob.blockPosition());
            boolean isSapper = LivingBattlefieldSystem.isSapperUnit(
                    mob instanceof BurrowerEntity,
                    mob instanceof ColonyWardenEntity,
                    mob.getEvolutionRank(),
                    mob.hasTrait(AdaptationTrait.BLAST_SHIELDED)
            );
            if (!mob.isDesynced() && LivingBattlefieldSystem.shouldAvoidTraumaZone(cell.trauma(), isSapper)) {
                Vec3 escape = map.sampleTraumaEscapeVector(mob.blockPosition());
                Vec3 detour = LivingBattlefieldSystem.computeTraumaDetourVector(
                        target.position().subtract(mob.position()), escape, cell.trauma());
                basePos = basePos.add(detour);
            }
            Vec3 steering = computeSteeringOffset(mob, serverLevel);
            Vec3 gradient = map.samplePheromoneGradient(mob.blockPosition());
            Vec3 organicFlow = MycelialFastNoise.organicFlow2D(
                    mob.getX(),
                    mob.getZ(),
                    serverLevel.getSeed() + mob.getId(),
                    serverLevel.getGameTime() * 0.05D
            );
            return basePos.add(steering.scale(0.5D))
                    .add(gradient.scale(0.65D))
                    .add(organicFlow.scale(1.15D));
        }
        return basePos;
    }

    @Nullable
    private static Vec3 computeSlotTargetForSquad(
            MycelialMob mob, LivingEntity target, ServerLevel level, double ringRadius) {
        AABB box = target.getBoundingBox().inflate(10.0D);
        List<MycelialMob> peers = level.getEntitiesOfClass(MycelialMob.class, box,
                ally -> ally.isAlive()
                        && !mob.isRivalStrainTarget(ally)
                        && (ally.getTarget() == target || ally == mob));
        if (peers.size() <= 1) {
            return null;
        }
        int slotIndex = 0;
        int myId = mob.getId();
        for (int i = 0, size = peers.size(); i < size; i++) {
            if (peers.get(i).getId() < myId) {
                slotIndex++;
            }
        }
        double baseAngle = Math.atan2(mob.getZ() - target.getZ(), mob.getX() - target.getX());
        return HiveSteeringEngine.computeAttackSlotPosition(
                target.getX(), target.getY(), target.getZ(),
                slotIndex, peers.size(), ringRadius, baseAngle);
    }

    /**
     * Clean-room Arrive + Blended Steering (Separation, Cohesion, Alignment, Pylon repulsion)
     * producing a smooth deceleration vector toward targetPos while avoiding ally clumping.
     */
    public static Vec3 computeTacticalArriveSteering(MycelialMob mob, Vec3 targetPos, ServerLevel level) {
        return computeSwarmSteeringInternal(mob, level, true, targetPos);
    }

    /**
     * Clean-room Blended Steering combining Separation, Cohesion, Alignment, and Pylon repulsion
     * without external steering libraries or neighbor-loop heap allocations.
     */
    public static Vec3 computeSteeringOffset(MycelialMob mob, ServerLevel level) {
        return computeSwarmSteeringInternal(mob, level, false, mob.position());
    }

    private static Vec3 computeSwarmSteeringInternal(
            MycelialMob mob, ServerLevel level, boolean includeArrive, Vec3 targetPos) {
        AABB box = mob.getBoundingBox().inflate(5.5D);
        List<MycelialMob> allies = level.getEntitiesOfClass(MycelialMob.class, box,
                ally -> ally != mob && ally.isAlive());
        BlockPos pylonPos = mob.isDesynced() ? null : HiveBrainBridge.getNearestPylonPos(mob);
        boolean hasPylon = pylonPos != null && mob.blockPosition().distSqr(pylonPos) < 100.0D;

        if (allies.isEmpty() && !includeArrive && !hasPylon) {
            return Vec3.ZERO;
        }

        NeighborScratch scratch = NEIGHBOR_SCRATCH.get();
        int count = Math.min(allies.size(), MAX_STEERING_NEIGHBORS);
        for (int i = 0; i < count; i++) {
            MycelialMob ally = allies.get(i);
            scratch.nx[i] = ally.getX();
            scratch.nz[i] = ally.getZ();
            Vec3 vel = ally.getDeltaMovement();
            scratch.nvx[i] = vel.x;
            scratch.nvz[i] = vel.z;
        }

        Vec3 mobVel = mob.getDeltaMovement();
        return HiveSteeringEngine.computeBlendedSwarmSteering(
                includeArrive,
                mob.getX(), mob.getZ(),
                mobVel.x, mobVel.z,
                targetPos.x, targetPos.z,
                scratch.nx, scratch.nz,
                scratch.nvx, scratch.nvz,
                count,
                hasPylon,
                pylonPos != null ? pylonPos.getX() + 0.5D : 0.0D,
                pylonPos != null ? pylonPos.getZ() + 0.5D : 0.0D,
                2.2D
        );
    }

    public static int broadcastAlertCall(ServerLevel level, MycelialMob caller, @Nullable LivingEntity attacker) {
        if (attacker == null || !attacker.isAlive()) {
            return 0;
        }
        if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return 0;
        }
        if (attacker instanceof MycelialMob) {
            return 0;
        }

        HiveBlackboard.reportTarget(level, attacker.blockPosition());
        AABB box = caller.getBoundingBox().inflate(ALERT_CALL_RADIUS);
        List<MycelialMob> allies = level.getEntitiesOfClass(MycelialMob.class, box,
                ally -> ally != caller
                        && ally.isAlive()
                        && !caller.isRivalStrainTarget(ally)
                        && (ally.getTarget() == null || !ally.getTarget().isAlive()));

        int alerted = 0;
        for (MycelialMob ally : allies) {
            ally.setTarget(attacker);
            ally.dispatchBrainSignal(
                    level,
                    ally,
                    HiveBrainSignal.allyAlert(attacker.blockPosition(), attacker, level.getGameTime()));
            alerted++;
        }
        return alerted;
    }

    private static Vec3 rotateY(Vec3 v, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        return new Vec3(v.x * cos - v.z * sin, 0.0, v.x * sin + v.z * cos);
    }

    private static final class NeighborScratch {
        private final double[] nx = new double[MAX_STEERING_NEIGHBORS];
        private final double[] nz = new double[MAX_STEERING_NEIGHBORS];
        private final double[] nvx = new double[MAX_STEERING_NEIGHBORS];
        private final double[] nvz = new double[MAX_STEERING_NEIGHBORS];
    }
}
