package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.entity.ai.brain.hive.HiveBrainBridge;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.projectile.PurifierGrenadeProjectile;
import com.alex.mycelialmind.hive.MobEpisodicMemory;
import com.alex.mycelialmind.hive.TacticalEvasionEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

/**
 * Clean-room immediate hazard evasion goal.
 * Detects imminent purifier grenades, primed TNT, and swelling creepers before detonation
 * and steers away using TacticalEvasionEngine, extending awareness radius if grenade trauma exists.
 */
public class ImminentHazardEvadeGoal extends Goal {
    private static final double BASE_HAZARD_RADIUS = 6.5D;
    private static final double TRAUMA_HAZARD_RADIUS = 8.5D;
    private static final double EVADE_SPEED = 1.30D;
    private static final long SCAN_INTERVAL_TICKS = 4L;

    private final MycelialMob mob;
    @Nullable
    private Entity activeHazard;
    private long nextScanTick = Long.MIN_VALUE;

    public ImminentHazardEvadeGoal(MycelialMob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (mob.isBossMob() || mob.isDesynced() || mob.isAlliedEscort()) {
            return false;
        }
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        long now = serverLevel.getGameTime();
        double radius = getEffectiveRadius();
        if (activeHazard != null && isImminentHazard(activeHazard) && mob.distanceToSqr(activeHazard) <= radius * radius) {
            return true;
        }
        if (now < nextScanTick) {
            return false;
        }
        nextScanTick = now + SCAN_INTERVAL_TICKS;
        activeHazard = findNearestHazard(serverLevel, radius);
        return activeHazard != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (mob.isBossMob() || mob.isDesynced() || mob.isAlliedEscort() || activeHazard == null) {
            return false;
        }
        if (!isImminentHazard(activeHazard)) {
            return false;
        }
        double exitRadius = getEffectiveRadius() + 2.0D;
        return mob.distanceToSqr(activeHazard) <= exitRadius * exitRadius;
    }

    @Override
    public void stop() {
        activeHazard = null;
    }

    @Override
    public void tick() {
        if (activeHazard == null || !isImminentHazard(activeHazard)) {
            return;
        }
        BlockPos nearestNode = HiveBrainBridge.getNearestNodePos(mob);
        Vec3 evadeDir = TacticalEvasionEngine.computeHazardEvadeVector(
                mob.getX(), mob.getZ(), activeHazard.getX(), activeHazard.getZ(), nearestNode);
        mob.getNavigation().moveTo(
                mob.getX() + evadeDir.x * 6.0D,
                mob.getY(),
                mob.getZ() + evadeDir.z * 6.0D,
                EVADE_SPEED
        );
    }

    private double getEffectiveRadius() {
        return mob.getEpisodicMemory().hasMemory(MobEpisodicMemory.KEY_TRAUMA_GRENADE)
                ? TRAUMA_HAZARD_RADIUS
                : BASE_HAZARD_RADIUS;
    }

    @Nullable
    private Entity findNearestHazard(ServerLevel level, double radius) {
        AABB box = mob.getBoundingBox().inflate(radius);
        List<Entity> candidates = level.getEntities(mob, box, ImminentHazardEvadeGoal::isImminentHazard);
        Entity best = null;
        double bestDistSq = radius * radius;
        for (Entity candidate : candidates) {
            double dSq = mob.distanceToSqr(candidate);
            if (dSq <= bestDistSq) {
                bestDistSq = dSq;
                best = candidate;
            }
        }
        return best;
    }

    private static boolean isImminentHazard(Entity entity) {
        if (entity == null || !entity.isAlive()) {
            return false;
        }
        if (entity instanceof PurifierGrenadeProjectile) {
            return true;
        }
        if (entity instanceof PrimedTnt tnt) {
            return tnt.getFuse() > 0;
        }
        if (entity instanceof Creeper creeper) {
            return creeper.isIgnited() || creeper.getSwellDir() > 0;
        }
        return false;
    }
}
