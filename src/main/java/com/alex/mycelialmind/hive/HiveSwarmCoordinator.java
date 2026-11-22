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
import com.alex.mycelialmind.world.MycelialFastNoise;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import com.badlogic.gdx.ai.steer.Proximity;
import com.badlogic.gdx.ai.steer.Steerable;
import com.badlogic.gdx.ai.steer.SteerableAdapter;
import com.badlogic.gdx.ai.steer.SteeringAcceleration;
import com.badlogic.gdx.ai.steer.behaviors.Alignment;
import com.badlogic.gdx.ai.steer.behaviors.Arrive;
import com.badlogic.gdx.ai.steer.behaviors.BlendedSteering;
import com.badlogic.gdx.ai.steer.behaviors.Cohesion;
import com.badlogic.gdx.ai.steer.behaviors.Separation;
import com.badlogic.gdx.math.Vector2;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Coordinates dynamic swarm roles, pincer flanking geometry, and 20-block AlertCalls.
 */
public final class HiveSwarmCoordinator {
    public static final double ALERT_CALL_RADIUS = 20.0D;

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

    /**
     * gdxAI Arrive + BlendedSteering (Separation, Cohesion, Alignment) producing a smooth
     * deceleration vector toward targetPos while avoiding ally clumping.
     */
    public static Vec3 computeTacticalArriveSteering(MycelialMob mob, Vec3 targetPos, ServerLevel level) {
        final double originX = mob.getX();
        final double originZ = mob.getZ();
        Steerable<Vector2> ownerAgent = toSteerable(mob, originX, originZ);
        Steerable<Vector2> targetAgent = new SteerableAdapter<>() {
            private final Vector2 pos = new Vector2((float) (targetPos.x - originX), (float) (targetPos.z - originZ));

            @Override
            public Vector2 getPosition() {
                return pos;
            }
        };

        Arrive<Vector2> arrive = new Arrive<>(ownerAgent, targetAgent)
                .setArrivalTolerance(0.6F)
                .setDecelerationRadius(3.5F)
                .setTimeToTarget(0.15F);

        BlendedSteering<Vector2> blended = new BlendedSteering<>(ownerAgent);
        blended.add(arrive, 1.00F);

        AABB box = mob.getBoundingBox().inflate(5.5D);
        List<MycelialMob> allies = level.getEntitiesOfClass(MycelialMob.class, box,
                ally -> ally != mob && ally.isAlive());
        if (!allies.isEmpty()) {
            List<Steerable<Vector2>> neighbors = new java.util.ArrayList<>(allies.size());
            for (MycelialMob ally : allies) {
                neighbors.add(toSteerable(ally, originX, originZ));
            }
            Proximity<Vector2> proximity = buildProximity(ownerAgent, neighbors);
            Separation<Vector2> separation = new Separation<>(ownerAgent, proximity);
            separation.setDecayCoefficient(8.0F);
            blended.add(separation, 1.35F);
            blended.add(new Cohesion<>(ownerAgent, proximity), 0.20F);
            blended.add(new Alignment<>(ownerAgent, proximity), 0.20F);
        }

        SteeringAcceleration<Vector2> out = new SteeringAcceleration<>(new Vector2());
        blended.calculateSteering(out);
        Vec3 combined = new Vec3(out.linear.x, 0.0D, out.linear.y);
        double len = combined.length();
        return len > 2.2D ? combined.scale(2.2D / len) : combined;
    }

    /**
     * gdxAI (com.badlogic.gdx.ai.steer) BlendedSteering combining Separation,
     * Cohesion, and Alignment on a RadiusProximity to prevent swarm stacking.
     */
    public static Vec3 computeSteeringOffset(MycelialMob mob, ServerLevel level) {
        AABB box = mob.getBoundingBox().inflate(5.5D);
        List<MycelialMob> allies = level.getEntitiesOfClass(MycelialMob.class, box,
                ally -> ally != mob && ally.isAlive());
        if (allies.isEmpty()) {
            return Vec3.ZERO;
        }
        final double originX = mob.getX();
        final double originZ = mob.getZ();
        Steerable<Vector2> ownerAgent = toSteerable(mob, originX, originZ);
        List<Steerable<Vector2>> neighbors = new java.util.ArrayList<>(allies.size());
        for (MycelialMob ally : allies) {
            neighbors.add(toSteerable(ally, originX, originZ));
        }

        Proximity<Vector2> proximity = buildProximity(ownerAgent, neighbors);

        Separation<Vector2> separation = new Separation<>(ownerAgent, proximity);
        separation.setDecayCoefficient(8.0F);
        BlendedSteering<Vector2> blended = new BlendedSteering<>(ownerAgent);
        blended.add(separation, 1.50F);
        blended.add(new Cohesion<>(ownerAgent, proximity), 0.20F);
        blended.add(new Alignment<>(ownerAgent, proximity), 0.20F);

        SteeringAcceleration<Vector2> out = new SteeringAcceleration<>(new Vector2());
        blended.calculateSteering(out);
        Vec3 combined = new Vec3(out.linear.x, 0.0D, out.linear.y);
        double len = combined.length();
        return len > 2.2D ? combined.scale(2.2D / len) : combined;
    }

    private static Proximity<Vector2> buildProximity(Steerable<Vector2> ownerAgent, List<Steerable<Vector2>> neighbors) {
        return new Proximity<>() {
            @Override
            public Steerable<Vector2> getOwner() {
                return ownerAgent;
            }

            @Override
            public void setOwner(Steerable<Vector2> owner) {
            }

            @Override
            public int findNeighbors(ProximityCallback<Vector2> callback) {
                int count = 0;
                for (Steerable<Vector2> neighbor : neighbors) {
                    if (callback.reportNeighbor(neighbor)) {
                        count++;
                    }
                }
                return count;
            }
        };
    }

    private static Steerable<Vector2> toSteerable(MycelialMob mob, double originX, double originZ) {
        final Vector2 pos = new Vector2((float) (mob.getX() - originX), (float) (mob.getZ() - originZ));
        final Vector2 vel = new Vector2((float) mob.getDeltaMovement().x, (float) mob.getDeltaMovement().z);
        return new SteerableAdapter<>() {
            @Override
            public Vector2 getPosition() {
                return pos;
            }

            @Override
            public Vector2 getLinearVelocity() {
                return vel;
            }

            @Override
            public float getMaxLinearSpeed() {
                return 2.2F;
            }

            @Override
            public float getMaxLinearAcceleration() {
                return 4.0F;
            }

            @Override
            public float getBoundingRadius() {
                return 0.6F;
            }
        };
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
            alerted++;
        }
        return alerted;
    }

    private static Vec3 rotateY(Vec3 v, double angleRad) {
        double cos = Math.cos(angleRad);
        double sin = Math.sin(angleRad);
        return new Vec3(v.x * cos - v.z * sin, 0.0, v.x * sin + v.z * cos);
    }
}
