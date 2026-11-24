package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.MobEpisodicMemory;
import com.alex.mycelialmind.hive.TacticalEvasionEngine;
import com.alex.mycelialmind.hive.TacticalEvasionEngine.KiteBand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Tactical kiting: retreats from close/high-utility threats using dual enter/exit
 * hysteresis bands while steering toward nearby MycelialNodes and away from PurificationPylons.
 */
public class KitingRetreatGoal extends Goal {
    private final MycelialMob mob;
    private final double speedModifier;
    private KiteBand activeBand = KiteBand.NONE;

    public KitingRetreatGoal(MycelialMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            activeBand = KiteBand.NONE;
            return false;
        }
        activeBand = evaluateCurrentBand(target);
        return activeBand != KiteBand.NONE;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            activeBand = KiteBand.NONE;
            return false;
        }
        activeBand = evaluateCurrentBand(target);
        return activeBand != KiteBand.NONE;
    }

    @Override
    public void stop() {
        activeBand = KiteBand.NONE;
    }

    private KiteBand evaluateCurrentBand(LivingEntity target) {
        float kiteUtility = HiveUtilityEvaluator.evaluateKiteUtility(mob, target);
        boolean hasTrauma = mob.getEpisodicMemory().hasMemory(MobEpisodicMemory.KEY_TRAUMA_GRENADE);
        return TacticalEvasionEngine.evaluateKiteBand(
                mob.distanceToSqr(target), activeBand, kiteUtility, hasTrauma);
    }

    public Vec3 computeTacticalRetreatDirection(LivingEntity target) {
        return computeTacticalRetreatDirection(mob, target, HiveBrainBridge.getNearestNodePos(mob));
    }

    public static Vec3 computeTacticalRetreatDirection(MycelialMob mob, LivingEntity target, BlockPos node) {
        Vec3 away = mob.position().subtract(target.position());
        Vec3 flat = new Vec3(away.x, 0.0, away.z);
        Vec3 dir = flat.lengthSqr() > 1.0E-4D ? flat.normalize() : new Vec3(1.0, 0.0, 0.0);

        if (node != null) {
            Vec3 toNode = new Vec3(node.getX() + 0.5 - mob.getX(), 0.0, node.getZ() + 0.5 - mob.getZ());
            if (toNode.lengthSqr() > 1.0E-4D) {
                dir = dir.add(toNode.normalize().scale(0.55));
            }
        }

        BlockPos pylon = HiveBrainBridge.getNearestPylonPos(mob);
        if (pylon != null) {
            Vec3 awayPylon = new Vec3(mob.getX() - (pylon.getX() + 0.5), 0.0, mob.getZ() - (pylon.getZ() + 0.5));
            if (awayPylon.lengthSqr() > 1.0E-4D) {
                double pylonWeight = mob.getEpisodicMemory().hasMemory(MobEpisodicMemory.KEY_TRAUMA_PYLON)
                        ? 1.05D : 0.75D;
                dir = dir.add(awayPylon.normalize().scale(pylonWeight));
            }
        }
        return dir.lengthSqr() > 1.0E-4D ? dir.normalize() : new Vec3(1.0, 0.0, 0.0);
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        Vec3 flat = computeTacticalRetreatDirection(target);
        double effectiveSpeed = speedModifier * TacticalEvasionEngine.kiteSpeedMultiplier(activeBand);
        mob.getNavigation().moveTo(
                mob.getX() + flat.x * 5.0,
                mob.getY(),
                mob.getZ() + flat.z * 5.0,
                effectiveSpeed
        );
    }
}
