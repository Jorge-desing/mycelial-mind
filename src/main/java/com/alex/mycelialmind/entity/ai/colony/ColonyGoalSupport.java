package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * Shared colony worker goal cleanup utilities.
 */
final class ColonyGoalSupport {
    private ColonyGoalSupport() {
    }

    static void releaseOrderReservationAndReset(ColonySymbiontEntity worker, boolean hasActiveOrder) {
        if (hasActiveOrder && worker.level() instanceof ServerLevel level) {
            SovereignColonyData colony = worker.getColonyData(level);
            if (colony != null) {
                colony.getOrderBoard().releaseWorkerReservations(worker.getUUID());
            }
        }
        worker.setTaskState("idle");
    }
}
