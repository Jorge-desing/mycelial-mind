package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Shared base goal for Sovereign Colony symbiont caste behaviors.
 */
public abstract class AbstractSymbiontCasteGoal extends Goal {
    protected final ColonySymbiontEntity worker;
    private final SymbiontCaste requiredCaste;

    protected AbstractSymbiontCasteGoal(
            ColonySymbiontEntity worker, SymbiontCaste requiredCaste, EnumSet<Flag> flags) {
        this.worker = worker;
        this.requiredCaste = requiredCaste;
        this.setFlags(flags);
    }

    @Override
    public boolean canUse() {
        return worker.getCaste() == requiredCaste
                && !worker.isStandby()
                && !worker.isResting()
                && worker.level() instanceof ServerLevel;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    protected final void releaseOrderReservationAndIdle(boolean hasActiveOrder) {
        ColonyGoalSupport.releaseOrderReservationAndReset(worker, hasActiveOrder);
    }
}
