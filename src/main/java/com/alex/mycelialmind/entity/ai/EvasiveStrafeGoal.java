package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.AdaptationTrait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Tier 2 PROJECTILE_DEFLECT behavioral mutation: performs erratic lateral strafes
 * and evasive hops whenever the target player aims a ranged weapon.
 */
public class EvasiveStrafeGoal extends Goal {
    private final MycelialMob mob;
    private int strafeTicks = 0;
    private int strafeDir = 1;

    public EvasiveStrafeGoal(MycelialMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return canEvadeTarget(mob, mob.getTarget());
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return strafeTicks > 0
                && mob.hasTier2Trait(AdaptationTrait.PROJECTILE_DEFLECT)
                && target != null
                && target.isAlive()
                && mob.distanceToSqr(target) <= 400.0D;
    }

    @Override
    public void stop() {
        strafeTicks = 0;
        mob.getMoveControl().setWantedPosition(mob.getX(), mob.getY(), mob.getZ(), 0.0D);
    }

    @Override
    public void start() {
        this.strafeTicks = 20;
        this.strafeDir = mob.getRandom().nextBoolean() ? 1 : -1;
    }

    @Override
    public void tick() {
        strafeTicks--;
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        if (strafeTicks % 10 == 0) {
            strafeDir = -strafeDir;
            if (mob.onGround() && mob.getRandom().nextFloat() < 0.35F) {
                mob.jumpFromGround();
            }
        }
        Vec3 toTarget = new Vec3(target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ()).normalize();
        Vec3 lateral = new Vec3(-toTarget.z * strafeDir, 0.0, toTarget.x * strafeDir);
        mob.getMoveControl().setWantedPosition(
                mob.getX() + lateral.x * 3.5,
                mob.getY(),
                mob.getZ() + lateral.z * 3.5,
                1.25D
        );
    }

    static boolean canEvadeTarget(MycelialMob mob, LivingEntity target) {
        return target != null
                && target.isAlive()
                && mob.hasTier2Trait(AdaptationTrait.PROJECTILE_DEFLECT)
                && mob.distanceToSqr(target) <= 400.0D
                && isAimingRanged(target);
    }

    private static boolean isAimingRanged(LivingEntity target) {
        if (!target.isUsingItem()) {
            return false;
        }
        var item = target.getUseItem().getItem();
        return item instanceof BowItem || item instanceof CrossbowItem;
    }
}
