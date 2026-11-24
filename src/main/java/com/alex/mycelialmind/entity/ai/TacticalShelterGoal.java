package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.SporeArchitectEntity;
import com.alex.mycelialmind.entity.SporelingEntity;
import com.alex.mycelialmind.entity.SynapseWeaverEntity;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.TacticalEvasionEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Clean-room event-driven shelter/retreat goal for fragile/support hive units
 * triggered by spatial tactical alarms on HiveBlackboard with bounded duration and cooldown.
 */
public class TacticalShelterGoal extends Goal {
    private static final int MAX_SHELTER_TICKS = 100;
    private static final long SHELTER_COOLDOWN_TICKS = 240L;
    private static final double SHELTER_SPEED = 1.22D;

    private final MycelialMob mob;
    @Nullable
    private HiveBlackboard.TacticalAlarm activeAlarm;
    private int ticksRemaining = 0;
    private long cooldownUntilTick = 0L;

    public TacticalShelterGoal(MycelialMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    public static boolean isEligibleMob(MycelialMob mob) {
        return mob instanceof SporeArchitectEntity
                || mob instanceof SynapseWeaverEntity
                || mob instanceof SporelingEntity;
    }

    @Override
    public boolean canUse() {
        if (!isEligibleMob(mob) || mob.isFrenzyActive() || mob.isDesynced() || mob.isAlliedEscort()) {
            return false;
        }
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        long now = serverLevel.getGameTime();
        if (now < cooldownUntilTick) {
            return false;
        }
        HiveBlackboard.TacticalAlarm alarm = HiveBlackboard.getNearestActiveAlarm(serverLevel, mob.blockPosition());
        if (alarm == null) {
            return false;
        }
        this.activeAlarm = alarm;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (ticksRemaining <= 0 || mob.isFrenzyActive() || mob.isDesynced() || mob.isAlliedEscort()) {
            return false;
        }
        if (activeAlarm == null || !(mob.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        return serverLevel.getGameTime() < activeAlarm.expiresAtTick();
    }

    @Override
    public void start() {
        this.ticksRemaining = MAX_SHELTER_TICKS;
    }

    @Override
    public void stop() {
        this.ticksRemaining = 0;
        this.activeAlarm = null;
        if (mob.level() instanceof ServerLevel serverLevel) {
            this.cooldownUntilTick = serverLevel.getGameTime() + SHELTER_COOLDOWN_TICKS;
        }
    }

    @Override
    public void tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--;
        }
        if (activeAlarm == null) {
            return;
        }
        BlockPos origin = activeAlarm.origin();
        BlockPos nearestNode = HiveBrainBridge.getNearestNodePos(mob);
        boolean safeRefugeNode = nearestNode != null
                && nearestNode.distSqr(origin) > 64.0D
                && nearestNode.distSqr(origin) > mob.blockPosition().distSqr(origin);

        if (safeRefugeNode && mob.blockPosition().distSqr(nearestNode) > 4.0D) {
            mob.getNavigation().moveTo(
                    nearestNode.getX() + 0.5D,
                    nearestNode.getY(),
                    nearestNode.getZ() + 0.5D,
                    SHELTER_SPEED
            );
            return;
        }

        Vec3 awayDir = TacticalEvasionEngine.computeHazardEvadeVector(
                mob.getX(), mob.getZ(), origin.getX() + 0.5D, origin.getZ() + 0.5D, null);
        mob.getNavigation().moveTo(
                mob.getX() + awayDir.x * 6.0D,
                mob.getY(),
                mob.getZ() + awayDir.z * 6.0D,
                SHELTER_SPEED
        );
    }
}
