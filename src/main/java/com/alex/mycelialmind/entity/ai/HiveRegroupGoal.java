package com.alex.mycelialmind.entity.ai;

import com.alex.mycelialmind.compat.sbl.MycelialSblBridge;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveOrder;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Utility-driven survival goal: guides wounded or displaced MycelialMobs toward the
 * nearest Synaptic Node and away from Purification Pylons.
 */
public class HiveRegroupGoal extends Goal {
    private final MycelialMob mob;
    private final double speedModifier;
    private BlockPos destination;
    private boolean followingOrder;

    public HiveRegroupGoal(MycelialMob mob, double speedModifier) {
        this.mob = mob;
        this.speedModifier = speedModifier;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return validRallyNode() != null || HiveUtilityEvaluator.evaluateRegroupUtility(mob) >= 0.65F;
    }

    @Override
    public boolean canContinueToUse() {
        if (followingOrder) {
            return !mob.getNavigation().isDone() && destination != null && destination.equals(validRallyNode());
        }
        return !mob.getNavigation().isDone()
                && (destination == null || isValidNode(destination))
                && HiveUtilityEvaluator.evaluateRegroupUtility(mob) >= 0.45F;
    }

    @Override
    public void start() {
        BlockPos node = validRallyNode();
        followingOrder = node != null;
        if (node == null) {
            BlockPos candidate = MycelialSblBridge.getNearestNodePos(mob);
            node = isValidNode(candidate) ? candidate : null;
        }
        destination = node;
        if (node != null) {
            mob.getNavigation().moveTo(node.getX() + 0.5, node.getY(), node.getZ() + 0.5, speedModifier);
            return;
        }
        BlockPos pylon = MycelialSblBridge.getNearestPylonPos(mob);
        if (pylon != null) {
            Vec3 away = new Vec3(mob.getX() - (pylon.getX() + 0.5), 0.0, mob.getZ() - (pylon.getZ() + 0.5));
            Vec3 dir = away.lengthSqr() > 1.0E-4D ? away.normalize() : new Vec3(1.0, 0.0, 0.0);
            mob.getNavigation().moveTo(mob.getX() + dir.x * 8.0, mob.getY(), mob.getZ() + dir.z * 8.0, speedModifier);
        }
    }

    @Override
    public void stop() {
        mob.getNavigation().stop();
        destination = null;
        followingOrder = false;
    }

    private BlockPos validRallyNode() {
        if (!(mob.level() instanceof ServerLevel level) || mob.isDesynced()
                || MycelialSblBridge.getHiveOrder(mob) != HiveOrder.RALLY_NODE
                || HiveBlackboard.getOrder(level) != HiveOrder.RALLY_NODE
                || HiveBlackboard.getRallyNodePos(level) == null) {
            return null;
        }
        BlockPos node = MycelialSblBridge.getNearestNodePos(mob);
        return isValidNode(node) ? node : null;
    }

    private boolean isValidNode(BlockPos node) {
        if (!(mob.level() instanceof ServerLevel level)) {
            return false;
        }
        if (node == null || !level.hasChunkAt(node)
                || !(level.getBlockEntity(node) instanceof SynapticNodeBlockEntity)) {
            return false;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        return tracker.getNodeType(node) != SynapticNodeType.SYMBIOTIC && !tracker.isProtected(node);
    }
}
