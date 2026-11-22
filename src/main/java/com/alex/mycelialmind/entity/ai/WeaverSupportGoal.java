package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.registry.ModMemoryTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.AABB;

import java.util.EnumSet;

/** Keeps support in range while allowing the weaver to defend itself up close. */
public final class WeaverSupportGoal extends Goal {
    private final MycelialMob mob;
    private int nextRepath;

    public WeaverSupportGoal(MycelialMob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return shouldFollow(64);
    }

    @Override
    public boolean canContinueToUse() {
        return shouldFollow(36);
    }

    private boolean shouldFollow(double minDistanceSqr) {
        BlockPos ally = mob.getBrain().getMemory(ModMemoryTypes.WEAVER_SUPPORT_POS.get()).orElse(null);
        LivingEntity target = mob.getTarget();
        boolean evasionCanRun = EvasiveStrafeGoal.canEvadeTarget(mob, target);
        if (mob.isDesynced() || evasionCanRun || ally == null || !hasValidAlly(ally)) {
            return false;
        }
        return mob.blockPosition().distSqr(ally) > minDistanceSqr
                && (target == null || !target.isAlive() || mob.distanceToSqr(target) > 16);
    }

    private boolean hasValidAlly(BlockPos position) {
        if (!(mob.level() instanceof ServerLevel level) || !level.hasChunkAt(position)) {
            return false;
        }
        return !level.getEntitiesOfClass(MycelialMob.class, new AABB(position).inflate(3),
                ally -> ally != mob && ally.isAlive() && !mob.isRivalStrainTarget(ally)).isEmpty();
    }

    @Override
    public void start() {
        nextRepath = 0;
        tick();
    }

    @Override
    public void tick() {
        if (--nextRepath > 0) {
            return;
        }
        nextRepath = 20;
        mob.getBrain().getMemory(ModMemoryTypes.WEAVER_SUPPORT_POS.get()).ifPresent(pos -> {
            if (mob.blockPosition().distSqr(pos) > 36) {
                mob.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
            } else {
                mob.getNavigation().stop();
            }
        });
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
    }
}
