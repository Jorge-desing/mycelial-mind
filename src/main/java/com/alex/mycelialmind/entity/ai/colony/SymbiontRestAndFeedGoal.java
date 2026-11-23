package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Clean-room upkeep AI goal: when a Symbiont's fatigue rises or nutrition/HP drops,
 * it releases active order leases, returns to the Sovereign Nexus (or nearest linked outpost),
 * consumes 1 biomass if needed, rests, and recovers before resuming colony duties.
 */
public class SymbiontRestAndFeedGoal extends Goal {
    private final ColonySymbiontEntity worker;
    private BlockPos restAnchor;
    private int restTicks = 0;

    public SymbiontRestAndFeedGoal(ColonySymbiontEntity worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(worker.level() instanceof ServerLevel level)) {
            return false;
        }
        if (worker.isStandby()) {
            return false;
        }
        return worker.canRetreatToRest() && worker.getColonyData(level) != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(worker.level() instanceof ServerLevel level)) {
            return false;
        }
        if (worker.isStandby()) {
            return false;
        }
        boolean recovered = SovereignColonyEngine.isRecoveredFromRest(worker.getFatigue(), worker.getNutrition())
                && worker.getHealth() >= worker.getMaxHealth() * 0.85F;
        return !recovered && restTicks < 240 && worker.getColonyData(level) != null;
    }

    @Override
    public void start() {
        restTicks = 0;
        worker.setResting(true);
        worker.setTaskState("resting");
        if (worker.level() instanceof ServerLevel level) {
            SovereignColonyData colony = worker.getColonyData(level);
            if (colony != null) {
                colony.getOrderBoard().releaseWorkerReservations(worker.getUUID());
                restAnchor = findNearestAnchor(colony);
            } else {
                restAnchor = worker.getHomeAnchor();
            }
        }
    }

    @Override
    public void stop() {
        worker.setResting(false);
        worker.setTaskState("idle");
        restAnchor = null;
    }

    @Override
    public void tick() {
        if (!(worker.level() instanceof ServerLevel level) || restAnchor == null) {
            return;
        }
        restTicks++;
        double distSq = worker.blockPosition().distSqr(restAnchor);
        if (distSq > 9.0D) {
            if (restTicks % 15 == 1 || worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(
                        restAnchor.getX() + 0.5D,
                        restAnchor.getY() + 0.2D,
                        restAnchor.getZ() + 0.5D,
                        1.15D
                );
            }
            return;
        }

        worker.getNavigation().stop();
        worker.getLookControl().setLookAt(restAnchor.getX() + 0.5D, restAnchor.getY() + 0.5D, restAnchor.getZ() + 0.5D);
        worker.setFatigue(Math.max(0.0F, worker.getFatigue() - 4.0F));
        worker.heal(1.5F);

        if (worker.getNutrition() < 75.0F && restTicks % 20 == 0) {
            SovereignColonyData colony = worker.getColonyData(level);
            if (colony != null && colony.consumeBiomass(SovereignColonyEngine.getRestBiomassCost())) {
                worker.setNutrition(100.0F);
                worker.setMorale(worker.getMorale() + 6.0F);
                worker.markRestedAt(level.getGameTime());
                SovereignColonySavedData.get(level).setDirty();
                level.sendParticles(ModParticles.BIO_LUME.get(),
                        worker.getX(), worker.getY() + 0.6, worker.getZ(), 8, 0.25, 0.25, 0.25, 0.02);
                level.playSound(null, worker.blockPosition(), ModSounds.SYMBIOTIC_NODE_CHARGE.get(),
                        worker.getSoundSource(), 0.45F, 1.3F);
            } else {
                worker.setNutrition(Math.min(75.0F, worker.getNutrition() + 5.0F));
            }
            worker.syncRosterToColony();
        }
    }

    private BlockPos findNearestAnchor(SovereignColonyData colony) {
        BlockPos best = colony.getNexusPos();
        double bestDist = worker.blockPosition().distSqr(best);
        for (BlockPos outpost : colony.getLinkedOutposts()) {
            double d = worker.blockPosition().distSqr(outpost);
            if (d < bestDist) {
                bestDist = d;
                best = outpost;
            }
        }
        return best;
    }
}
