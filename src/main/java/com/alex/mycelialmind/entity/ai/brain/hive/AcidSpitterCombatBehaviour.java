package com.alex.mycelialmind.entity.ai.brain.hive;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.entity.AcidSpitterEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.ai.EvasiveStrafeGoal;
import com.alex.mycelialmind.entity.ai.HiveUtilityEvaluator;
import com.alex.mycelialmind.entity.ai.KitingRetreatGoal;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBehaviour;
import com.alex.mycelialmind.entity.ai.brain.core.HiveBrainUtils;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveOrder;
import com.alex.mycelialmind.hive.MycorrhizalAcousticTracker;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;

/** Owns the spitter's target, aim, and navigation for the lifetime of its Brain. */
public final class AcidSpitterCombatBehaviour extends HiveBehaviour<MycelialMob> {
    private static final double SHOT_RANGE_SQR = 16 * 16;
    private static final int SHOT_INTERVAL = 35;
    private static final int ROUTE_INTERVAL = 10;

    private enum Action { HOLD, RETREAT, EVADE, SEEK_SHOT, RALLY, PRESSURE, WANDER }

    private final EvasiveStrafeGoal evasiveStrafe;
    private Action action = Action.HOLD;
    private BlockPos objective;
    private long nextRouteAt;
    private long nextShotAt;

    public AcidSpitterCombatBehaviour(AcidSpitterEntity mob) {
        this.evasiveStrafe = new EvasiveStrafeGoal(mob);
        noTimeout();
    }

    @Override
    protected List<Pair<MemoryModuleType<?>, MemoryStatus>> getMemoryRequirements() {
        return List.of(Pair.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryStatus.REGISTERED));
    }

    @Override
    protected boolean shouldKeepRunning(MycelialMob mob) {
        return true;
    }

    @Override
    protected void tick(ServerLevel level, MycelialMob entity, long gameTime) {
        AcidSpitterEntity mob = (AcidSpitterEntity) entity;
        LivingEntity target = updateTarget(mob);
        if (mob.getFluidHeight(FluidTags.WATER) > mob.getFluidJumpThreshold() || mob.isInLava()) {
            setAction(mob, Action.HOLD, null, gameTime);
            return;
        }

        HiveOrder order = HiveBrainBridge.getHiveOrder(mob);
        if (order != HiveBlackboard.getOrder(level)) {
            order = HiveOrder.NONE;
        }
        BlockPos rally = order == HiveOrder.RALLY_NODE && !mob.isDesynced()
                && HiveBlackboard.getRallyNodePos(level) != null
                ? validNode(level, HiveBrainBridge.getNearestNodePos(mob)) : null;
        if (target != null) {
            double distanceSqr = mob.distanceToSqr(target);
            mob.getLookControl().setLookAt(target, 30, 30);
            if (distanceSqr < 25 || HiveUtilityEvaluator.evaluateKiteUtility(mob, target) >= 0.60F) {
                setAction(mob, Action.RETREAT, null, gameTime);
                BlockPos currentDest = mob.getNavigation().getTargetPos();
                boolean needsRetreatRoute = mob.getNavigation().isDone()
                        || currentDest == null
                        || Vec3.atBottomCenterOf(currentDest).distanceToSqr(target.position()) <= distanceSqr + 0.5D;
                if (needsRetreatRoute && gameTime >= nextRouteAt) {
                    BlockPos refuge = rally != null ? rally : validNode(level, HiveBrainBridge.getNearestNodePos(mob));
                    Vec3 direction = KitingRetreatGoal.computeTacticalRetreatDirection(mob, target, refuge);
                    Vec3 destination = findRetreatDestination(mob, target, direction, distanceSqr);
                    if (destination != null) {
                        route(mob, destination, 1.15, gameTime);
                    } else {
                        nextRouteAt = gameTime + ROUTE_INTERVAL;
                    }
                }
            } else if (evade(mob, gameTime)) {
                return;
            } else if (distanceSqr <= SHOT_RANGE_SQR && mob.getSensing().hasLineOfSight(target)) {
                setAction(mob, Action.HOLD, null, gameTime);
                if (gameTime >= nextShotAt) {
                    mob.performRangedAttack(target, (float) (Math.sqrt(distanceSqr) / 16.0));
                    nextShotAt = gameTime + SHOT_INTERVAL;
                }
            } else {
                setAction(mob, Action.SEEK_SHOT, null, gameTime);
                if (gameTime >= nextRouteAt) {
                    Vec3 position = findShotPosition(level, mob, target);
                    if (position != null) {
                        route(mob, position, 1.0, gameTime);
                    } else {
                        nextRouteAt = gameTime + ROUTE_INTERVAL;
                        mob.getNavigation().moveTo(target, 1.0);
                    }
                }
            }
            return;
        }

        if (rally != null) {
            travelTo(mob, Action.RALLY, rally, gameTime);
            return;
        }
        if (!mob.isAlliedEscort()) {
            BlockPos decoy = MycorrhizalAcousticTracker.getNearestActiveDecoy(level, mob.blockPosition(), 20);
            if (decoy != null && level.hasChunkAt(decoy)) {
                travelTo(mob, Action.PRESSURE, decoy, gameTime);
                return;
            }
        }
        BlockPos regroupNode = validNode(level, HiveBrainBridge.getNearestNodePos(mob));
        if (regroupNode != null && HiveUtilityEvaluator.evaluateRegroupUtility(mob) >= 0.65F) {
            travelTo(mob, Action.RALLY, regroupNode, gameTime);
            return;
        }
        BlockPos pressure = HiveBrainBridge.getSharedTargetPos(mob);
        if (order == HiveOrder.PRESSURE_TARGET && !mob.isDesynced() && pressure != null
                && pressure.equals(HiveBlackboard.getRecentTarget(level))
                && level.hasChunkAt(pressure)
                && !PurificationZoneTracker.get(level).isProtected(pressure)) {
            travelTo(mob, Action.PRESSURE, pressure, gameTime);
            return;
        }
        setAction(mob, Action.WANDER, null, gameTime);
        if (mob.getNavigation().isDone() && gameTime >= nextRouteAt) {
            route(mob, LandRandomPos.getPos(mob, 8, 4), 0.9, gameTime);
            nextRouteAt = gameTime + 60 + mob.getRandom().nextInt(40);
        }
    }

    private boolean evade(AcidSpitterEntity mob, long gameTime) {
        if (action != Action.EVADE || !evasiveStrafe.canContinueToUse()) {
            if (!evasiveStrafe.canUse()) {
                return false;
            }
            setAction(mob, Action.EVADE, null, gameTime);
            evasiveStrafe.start();
        }
        evasiveStrafe.tick();
        return true;
    }

    private LivingEntity updateTarget(AcidSpitterEntity mob) {
        LivingEntity current = mob.getTarget();
        LivingEntity attacker = mob.getLastHurtByMob();
        LivingEntity chosen = current;
        if (mob.tickCount - mob.getLastHurtByMobTimestamp() <= 100 && isValidTarget(mob, attacker)) {
            chosen = attacker;
        } else if (!isValidTarget(mob, current)
                || current instanceof MycelialMob && !mob.isRivalStrainTarget(current)) {
            chosen = null;
            for (LivingEntity nearby : mob.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES)
                    .orElse(List.of())) {
                if (isValidTarget(mob, nearby)
                        && (nearby instanceof Player || mob.isRivalStrainTarget(nearby))
                        && mob.getSensing().hasLineOfSight(nearby)) {
                    chosen = nearby;
                    break;
                }
            }
        }
        if (chosen != current) {
            setAction(mob, Action.HOLD, null, mob.level().getGameTime());
            mob.setTarget(chosen);
            LivingEntity accepted = mob.getTarget();
            LivingEntity resolved = isValidTarget(mob, accepted) ? accepted : null;
            HiveBrainUtils.setTargetOfEntity(mob, resolved);
            return resolved;
        }
        if (chosen != null && !HiveBrainUtils.hasMemory(mob, MemoryModuleType.ATTACK_TARGET)) {
            HiveBrainUtils.setTargetOfEntity(mob, chosen);
        } else if (chosen == null && HiveBrainUtils.hasMemory(mob, MemoryModuleType.ATTACK_TARGET)) {
            HiveBrainUtils.clearMemory(mob, MemoryModuleType.ATTACK_TARGET);
        }
        return chosen;
    }

    private static boolean isValidTarget(AcidSpitterEntity mob, LivingEntity target) {
        double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE);
        return target != null && target != mob && target.level() == mob.level() && target.isAlive()
                && mob.distanceToSqr(target) <= range * range && mob.canAttack(target)
                && (!(target instanceof Player player) || !player.isSpectator() && !player.isCreative());
    }

    private static BlockPos validNode(ServerLevel level, BlockPos position) {
        if (position == null || !level.hasChunkAt(position)
                || !(level.getBlockEntity(position) instanceof SynapticNodeBlockEntity)) {
            return null;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        return tracker.getNodeType(position) != SynapticNodeType.SYMBIOTIC && !tracker.isProtected(position)
                ? position : null;
    }

    private static Vec3 findRetreatDestination(
            AcidSpitterEntity mob,
            LivingEntity target,
            Vec3 direction,
            double currentDistSqr
    ) {
        Vec3 best = null;
        double bestDistSqr = currentDistSqr;

        for (double step : new double[]{6.0D, 4.5D}) {
            Vec3 candidate = mob.position().add(direction.scale(step));
            BlockPos candidatePos = BlockPos.containing(candidate.x, mob.getY(), candidate.z);
            var path = mob.getNavigation().createPath(candidatePos, 0);
            if (path != null && path.getEndNode() != null) {
                var endNode = path.getEndNode();
                Vec3 end = Vec3.atBottomCenterOf(new BlockPos(endNode.x, endNode.y, endNode.z));
                double endDistSqr = end.distanceToSqr(target.position());
                if (endDistSqr > bestDistSqr && endDistSqr >= 25.0D) {
                    return end;
                }
                if (endDistSqr > bestDistSqr) {
                    bestDistSqr = endDistSqr;
                    best = end;
                }
            }
        }

        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3 candidate = LandRandomPos.getPosTowards(mob, 7, 3, mob.position().add(direction.scale(6.0D)));
            if (candidate != null) {
                double distSqr = candidate.distanceToSqr(target.position());
                if (distSqr > bestDistSqr) {
                    bestDistSqr = distSqr;
                    best = candidate;
                }
            }
        }
        return best;
    }

    private static Vec3 findShotPosition(ServerLevel level, AcidSpitterEntity mob, LivingEntity target) {
        for (int attempt = 0; attempt < 4; attempt++) {
            Vec3 candidate = LandRandomPos.getPosTowards(mob, 8, 4, target.position());
            if (candidate != null && candidate.distanceToSqr(target.position()) <= SHOT_RANGE_SQR
                    && candidate.distanceToSqr(target.position()) >= 25
                    && level.clip(new ClipContext(candidate.add(0, mob.getEyeHeight(), 0), target.getEyePosition(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob)).getType() == HitResult.Type.MISS) {
                return candidate;
            }
        }
        return null;
    }

    private void travelTo(AcidSpitterEntity mob, Action next, BlockPos destination, long gameTime) {
        setAction(mob, next, destination, gameTime);
        if (gameTime < nextRouteAt || !mob.getNavigation().isDone()
                || destination.distSqr(mob.blockPosition()) <= 4) {
            return;
        }
        nextRouteAt = gameTime + ROUTE_INTERVAL;
        var path = mob.getNavigation().createPath(destination, 1);
        if (path != null && (next != Action.RALLY || path.canReach())) {
            mob.getNavigation().moveTo(path, 1.0);
        }
    }

    private void setAction(AcidSpitterEntity mob, Action next, BlockPos destination, long gameTime) {
        if (next != action || !Objects.equals(destination, objective)) {
            if (action == Action.EVADE && next != Action.EVADE) {
                evasiveStrafe.stop();
            }
            mob.getNavigation().stop();
            action = next;
            objective = destination;
            nextRouteAt = gameTime;
        }
    }

    private void route(AcidSpitterEntity mob, Vec3 destination, double speed, long gameTime) {
        nextRouteAt = gameTime + ROUTE_INTERVAL;
        if (destination != null) {
            mob.getNavigation().moveTo(destination.x, destination.y, destination.z, speed);
        }
    }
}
