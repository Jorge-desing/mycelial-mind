package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.AdaptationTrait;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Tier 2 MELEE_THORNS behavioral mutation: when the target is looking directly at this mob
 * from mid-range, calculates an angled flanking vector to encircle the player.
 */
public class FlankingAmbushGoal extends Goal {
    private final MycelialMob mob;
    private final double speedModifier;
    private int flankTicks = 0;
    private int flankSide = 1;

    public FlankingAmbushGoal(MycelialMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.isDesynced() || !mob.hasTier2Trait(AdaptationTrait.MELEE_THORNS)) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive()) {
            return false;
        }
        double distSq = mob.distanceToSqr(target);
        if (distSq < 9.0D || distSq > 144.0D) {
            return false;
        }
        Vec3 look = target.getLookAngle().normalize();
        Vec3 toMob = mob.position().subtract(target.position()).normalize();
        return look.dot(toMob) > 0.65D;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        if (flankTicks <= 0 || mob.isDesynced() || !mob.hasTier2Trait(AdaptationTrait.MELEE_THORNS)
                || target == null || !target.isAlive()) {
            return false;
        }
        double distanceSqr = mob.distanceToSqr(target);
        return distanceSqr > 6.25D && distanceSqr <= 144.0D;
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }

    @Override
    public void start() {
        this.flankTicks = 24;
        this.flankSide = (mob.getId() % 2 == 0) ? 1 : -1;
    }

    @Override
    public void tick() {
        flankTicks--;
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        Vec3 fromTarget = mob.position().subtract(target.position());
        Vec3 flat = new Vec3(fromTarget.x, 0.0, fromTarget.z).normalize();
        Vec3 rotated = flat.yRot((float) (flankSide * Math.PI / 3.0));
        Vec3 dest = target.position().add(rotated.scale(3.0));
        mob.getNavigation().moveTo(dest.x, target.getY(), dest.z, speedModifier);
    }
}
