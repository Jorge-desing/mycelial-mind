package com.alex.mycelialmind.hive;

import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Routes incoming combat stimuli and observed player habits through nearby enemy Synaptic Nodes:
 * 40% goes directly to HiveMindSavedData and 60% is buffered in the nearest enemy node (<= 48m)
 * for 30s before consolidation, allowing players to intercept/purge short-term memory.
 */
public final class SynapticMemoryRouter {
    public static final int BUFFER_NODE_RANGE = 48;
    public static final double DIRECT_RATIO = 0.40;
    public static final double BUFFER_RATIO = 0.60;

    private SynapticMemoryRouter() {
    }

    public static boolean routeStimulus(ServerLevel level, BlockPos origin, StimulusType type, double amount) {
        if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return false;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        long now = level.getGameTime();
        BlockPos nearestNode = PurificationZoneTracker.get(level).findNearestEnemyNode(origin, BUFFER_NODE_RANGE);
        if (nearestNode != null && level.getBlockEntity(nearestNode) instanceof SynapticNodeBlockEntity nodeBe) {
            hive.recordStimulus(type, amount * DIRECT_RATIO, now);
            nodeBe.addVolatileStimulus(type, amount * BUFFER_RATIO);
            return true;
        }
        hive.recordStimulus(type, amount, now);
        return false;
    }

    public static boolean routeHabit(ServerLevel level, BlockPos origin, PlayerHabit habit, double amount) {
        if (amount <= 0.0 || Double.isNaN(amount) || Double.isInfinite(amount)) {
            return false;
        }
        HiveMindSavedData hive = HiveMindSavedData.get(level);
        BlockPos nearestNode = PurificationZoneTracker.get(level).findNearestEnemyNode(origin, BUFFER_NODE_RANGE);
        if (nearestNode != null && level.getBlockEntity(nearestNode) instanceof SynapticNodeBlockEntity nodeBe) {
            hive.recordHabit(habit, amount * DIRECT_RATIO);
            nodeBe.addVolatileHabit(habit, amount * BUFFER_RATIO);
            return true;
        }
        hive.recordHabit(habit, amount);
        return false;
    }
}
