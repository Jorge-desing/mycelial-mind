package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.BurrowerEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.MycelialStalkerEntity;
import com.alex.mycelialmind.hive.HiveSwarmCoordinator;
import com.alex.mycelialmind.hive.SwarmRole;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Coordinates pincer flanking, dorsal ambush cueing, retreat cutoff,
 * Phalanx escort positioning, and desync scatter for MycelialMobs.
 */
public class SwarmFlankGoal extends Goal {
    private final MycelialMob mob;
    private final double speedModifier;
    private long lastAmbushCueTick = -100L;

    public SwarmFlankGoal(MycelialMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        LivingEntity target = mob.getTarget();
        if (target == null || !target.isAlive() || !mob.canAttack(target)) {
            return false;
        }
        double distSq = mob.distanceToSqr(target);
        if (mob.isDesynced()) {
            return distSq <= 256.0D;
        }
        SwarmRole role = HiveSwarmCoordinator.assignRole(mob);
        if (role == SwarmRole.FRONT_ANCHOR) {
            return mob.isPhalanxActive() && distSq > 9.0D && distSq <= 256.0D;
        }
        if (role != SwarmRole.FLANK_LEFT && role != SwarmRole.FLANK_RIGHT) {
            return false;
        }
        return distSq > 16.0D && distSq <= 256.0D;
    }

    @Override
    public void tick() {
        LivingEntity target = mob.getTarget();
        if (target == null) {
            return;
        }
        if (!mob.isDesynced()
                && mob.getCommandRole() != SynapticCommandSystem.CommandRole.NONE
                && (mob instanceof MycelialStalkerEntity || mob instanceof BurrowerEntity)
                && SynapticCommandSystem.isOutsideTargetFov(target.position(), target.getLookAngle(), mob.position(), 0.15D)) {
            long now = mob.level().getGameTime();
            if (now - lastAmbushCueTick >= 60L) {
                lastAmbushCueTick = now;
                mob.level().playSound(null, mob.blockPosition(),
                        ModSounds.SWARM_FLANK_CALL.get(), SoundSource.HOSTILE, 0.75F, 1.35F);
            }
        }
        Vec3 flankPos = mob instanceof MycelialStalkerEntity
                ? mob.getBrain().getMemory(ModMemoryTypes.STALKER_FLANK_POS.get())
                    .map(Vec3::atCenterOf).orElseGet(() -> HiveSwarmCoordinator.computeSwarmApproachPos(mob, target))
                : HiveSwarmCoordinator.computeSwarmApproachPos(mob, target);
        mob.getNavigation().moveTo(flankPos.x, target.getY(), flankPos.z, speedModifier);
    }
}
