package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.compat.sbl.MycelialSblBridge;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.EcosystemPurposeEngine;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * ChitinBrute active ability: charges forward modulated by HiveUtilityEvaluator
 * and the shared Hive Mind blackboard (BLAST_SHIELDED / alert / shared target).
 */
public class BruteChargeGoal extends Goal {
    private final MycelialMob mob;
    private int cooldown = 60;
    private int chargeTicks = 0;

    public BruteChargeGoal(MycelialMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (cooldown > 0) {
            cooldown--;
            return false;
        }
        if (!mob.onGround()) {
            return false;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            double maxDistSq = mob.hasTrait(AdaptationTrait.BLAST_SHIELDED) ? 256.0D : 144.0D;
            double distSq = mob.distanceToSqr(target);
            float utility = HiveUtilityEvaluator.evaluateChargeUtility(mob, target);
            return utility >= 0.25F && distSq >= 16.0D && distSq <= maxDistSq && mob.hasLineOfSight(target);
        }
        BlockPos sharedTarget = MycelialSblBridge.getSharedTargetPos(mob);
        if (sharedTarget != null) {
            double distSq = mob.blockPosition().distSqr(sharedTarget);
            return distSq >= 16.0D && distSq <= 256.0D;
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        LivingEntity target = mob.getTarget();
        return chargeTicks > 0 && (target != null
                ? target.isAlive()
                : MycelialSblBridge.getSharedTargetPos(mob) != null);
    }

    @Override
    public void start() {
        LivingEntity target = mob.getTarget();
        if (target != null && !target.isAlive()) {
            target = null;
        }
        BlockPos sharedTarget = target == null ? MycelialSblBridge.getSharedTargetPos(mob) : null;
        if (target == null && sharedTarget == null) {
            this.chargeTicks = 0;
            return;
        }

        this.chargeTicks = 16;
        float utility = HiveUtilityEvaluator.evaluateChargeUtility(mob, target);
        this.cooldown = utility >= 0.55F ? 80 : 120;
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 35, utility >= 0.65F ? 1 : 0, false, false));

        Vec3 dest = target != null
                ? target.position()
                : Vec3.atBottomCenterOf(sharedTarget);
        Vec3 flat = new Vec3(dest.x - mob.getX(), 0.0, dest.z - mob.getZ());
        if (flat.lengthSqr() > 1.0E-4D) {
            Vec3 dir = flat.normalize();
            double boost = 0.85 + utility * 0.25;
            mob.setDeltaMovement(dir.x * boost, 0.25, dir.z * boost);
            mob.hasImpulse = true;
        }
    }

    @Override
    public void tick() {
        chargeTicks--;
        if (mob.level() instanceof ServerLevel serverLevel && checkAndApplyBarrierCollision(serverLevel, mob)) {
            chargeTicks = 0;
            return;
        }
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive() && mob.distanceToSqr(target) <= 5.0D) {
            mob.doHurtTarget(target);
            float utility = HiveUtilityEvaluator.evaluateChargeUtility(mob, target);
            target.knockback(1.2F + utility * 0.5F, mob.getX() - target.getX(), mob.getZ() - target.getZ());
            chargeTicks = 0;
        }
    }

    public static boolean isStunBarrier(BlockState state) {
        return state.is(ModBlocks.CHITIN_PILLAR.get())
                || state.is(ModBlocks.REINFORCED_BIO_BARRIER.get());
    }

    public static boolean checkAndApplyBarrierCollision(ServerLevel level, MycelialMob mob) {
        Vec3 horiz = mob.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D);
        Vec3 forward = horiz.lengthSqr() > 1.0E-4D
                ? horiz.normalize()
                : mob.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize();
        if (forward.lengthSqr() > 1.0E-4D) {
            BlockPos frontFeet = BlockPos.containing(
                    mob.getX() + forward.x * 1.15D,
                    mob.getY() + 0.3D,
                    mob.getZ() + forward.z * 1.15D
            );
            if (isStunBarrier(level.getBlockState(frontFeet)) || isStunBarrier(level.getBlockState(frontFeet.above()))) {
                return applyWallImpactStun(level, mob);
            }
        }
        if (mob.horizontalCollision) {
            for (BlockPos scan : BlockPos.betweenClosed(mob.blockPosition().offset(-1, 0, -1), mob.blockPosition().offset(1, 1, 1))) {
                if (isStunBarrier(level.getBlockState(scan))) {
                    return applyWallImpactStun(level, mob);
                }
            }
        }
        return false;
    }

    public static boolean applyWallImpactStun(ServerLevel level, MycelialMob mob) {
        mob.setDeltaMovement(Vec3.ZERO);
        mob.hasImpulse = true;
        mob.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN, EcosystemPurposeEngine.BRUTE_WALL_STUN_SLOW_TICKS, 3));
        mob.addEffect(new MobEffectInstance(
                MobEffects.WEAKNESS, EcosystemPurposeEngine.BRUTE_WALL_STUN_WEAK_TICKS, 1));
        mob.hurt(level.damageSources().flyIntoWall(), EcosystemPurposeEngine.BRUTE_WALL_SELF_DAMAGE);
        level.sendParticles(ModParticles.CHITIN_SHARD.get(),
                mob.getX(), mob.getY(0.6D), mob.getZ(),
                12, 0.45D, 0.35D, 0.45D, 0.06D);
        mob.playSound(ModSounds.CHITIN_BRUTE_HURT.get(), 1.0F, 0.72F);
        return true;
    }
}
