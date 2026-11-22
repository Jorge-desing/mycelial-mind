package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.compat.sbl.MycelialSblBridge;
import com.alex.mycelialmind.entity.MycelialMob;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Tactical kiting: retreats from close/high-utility threats while steering toward
 * nearby MycelialNodes and away from PurificationPylons via the SBL blackboard.
 */
public class KitingRetreatGoal extends Goal {
    private final MycelialMob mob;
    private final double speedModifier;

    public KitingRetreatGoal(MycelialMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        return mob.distanceToSqr(target) < 25.0D || HiveUtilityEvaluator.evaluateKiteUtility(mob, target) >= 0.60F;
    }

    public Vec3 computeTacticalRetreatDirection(LivingEntity target) {
        return computeTacticalRetreatDirection(mob, target, MycelialSblBridge.getNearestNodePos(mob));
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

        BlockPos pylon = MycelialSblBridge.getNearestPylonPos(mob);
        if (pylon != null) {
            Vec3 awayPylon = new Vec3(mob.getX() - (pylon.getX() + 0.5), 0.0, mob.getZ() - (pylon.getZ() + 0.5));
            if (awayPylon.lengthSqr() > 1.0E-4D) {
                dir = dir.add(awayPylon.normalize().scale(0.75));
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
        mob.getNavigation().moveTo(
                mob.getX() + flat.x * 5.0,
                mob.getY(),
                mob.getZ() + flat.z * 5.0,
                speedModifier
        );
    }
}
