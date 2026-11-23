package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.colony.ColonyOrder;
import com.alex.mycelialmind.colony.ColonyOrderBoard;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.colony.SovereignTrait;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;

/**
 * Courier caste goal: chains INPUT chests -> Sovereign Nexus (biomass upkeep) / OUTPUT storage chests
 * using ColonyOrderBoard reservations and navigation-timeout recovery.
 */
public class SymbiontCourierGoal extends Goal {
    private enum Phase {
        PICKUP,
        DEPOSIT
    }

    private final ColonySymbiontEntity worker;
    private Phase phase = Phase.PICKUP;
    @Nullable
    private ColonyOrder activeOrder;
    private int navTicks = 0;
    private int waitTicks = 0;

    public SymbiontCourierGoal(ColonySymbiontEntity worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return worker.getCaste() == SymbiontCaste.COURIER
                && !worker.isStandby()
                && !worker.isResting()
                && worker.level() instanceof ServerLevel;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void stop() {
        if (activeOrder != null && worker.level() instanceof ServerLevel level) {
            SovereignColonyData colony = worker.getColonyData(level);
            if (colony != null) {
                colony.getOrderBoard().releaseWorkerReservations(worker.getUUID());
            }
        }
        activeOrder = null;
        worker.setTaskState("idle");
    }

    @Override
    public void tick() {
        if (!(worker.level() instanceof ServerLevel level)) {
            return;
        }
        SovereignColonyData colony = worker.getColonyData(level);
        if (colony == null) {
            worker.setTaskState("no_colony");
            return;
        }

        if (worker.isPouchFull() || (!worker.isPouchEmpty() && phase == Phase.DEPOSIT)) {
            phase = Phase.DEPOSIT;
            tickDeposit(level, colony);
            return;
        }

        phase = Phase.PICKUP;
        tickPickup(level, colony);
    }

    private void tickPickup(ServerLevel level, SovereignColonyData colony) {
        long now = level.getGameTime();
        if (activeOrder == null || !activeOrder.isReservedBy(worker.getUUID(), now)) {
            activeOrder = colony.getOrderBoard().claimBestOrder(
                    worker.getUUID(), SymbiontCaste.COURIER, worker.blockPosition(), now, ColonyOrderBoard.DEFAULT_LEASE_TICKS);
            navTicks = 0;
            waitTicks = 0;
        }

        BlockPos targetChest = activeOrder != null ? activeOrder.getTargetPos()
                : colony.getInputChests().stream()
                .min(Comparator.comparingDouble(p -> p.distSqr(worker.blockPosition())))
                .orElse(null);

        if (targetChest == null) {
            if (!worker.isPouchEmpty()) {
                phase = Phase.DEPOSIT;
                return;
            }
            worker.setTaskState("awaiting_cargo");
            BlockPos anchor = colony.getNexusPos();
            if (worker.blockPosition().distSqr(anchor) > 36.0D && worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 1.15D);
            }
            return;
        }

        worker.setTaskState("courier_pickup");
        if (worker.blockPosition().distSqr(targetChest) > 6.5D) {
            navTicks++;
            if (navTicks % 15 == 1 || worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(targetChest.getX() + 0.5D, targetChest.getY(), targetChest.getZ() + 0.5D, 1.22D);
            }
            if (navTicks > 200) {
                colony.getOrderBoard().releaseWorkerReservations(worker.getUUID());
                activeOrder = null;
                navTicks = 0;
            }
            return;
        }

        worker.getNavigation().stop();
        worker.getLookControl().setLookAt(targetChest.getX() + 0.5D, targetChest.getY() + 0.5D, targetChest.getZ() + 0.5D);
        int delay = SovereignColonyEngine.computeWorkIntervalTicks(
                14, worker.getRank(), worker.getTemperament(), colony.getColonyMorale(),
                colony.hasTrait(SovereignTrait.HYPHAL_LOGISTICS));
        waitTicks++;
        if (waitTicks < delay) {
            return;
        }

        boolean pulled = pullFromContainer(level, targetChest, colony.hasTrait(SovereignTrait.HYPHAL_LOGISTICS) ? 2 : 1);
        if (activeOrder != null) {
            colony.getOrderBoard().completeOrder(activeOrder.getOrderId(), worker.getUUID());
            activeOrder = null;
        }
        waitTicks = 0;
        navTicks = 0;
        if (pulled || !worker.isPouchEmpty()) {
            phase = Phase.DEPOSIT;
            level.sendParticles(ModParticles.BIO_LUME.get(),
                    targetChest.getX() + 0.5, targetChest.getY() + 1.0, targetChest.getZ() + 0.5,
                    4, 0.2, 0.2, 0.2, 0.02);
        }
    }

    private void tickDeposit(ServerLevel level, SovereignColonyData colony) {
        boolean hasBiomassInPouch = false;
        for (int i = 0; i < worker.getPouch().getSlots(); i++) {
            if (worker.getPouch().getStackInSlot(i).is(ModItems.MYCELIAL_BIOMASS.get())) {
                hasBiomassInPouch = true;
                break;
            }
        }
        BlockPos destPos;
        boolean deliverToNexus = hasBiomassInPouch && colony.getBiomassStored() < 192;
        if (deliverToNexus) {
            destPos = colony.getNexusPos();
        } else {
            destPos = colony.getOutputChests().stream()
                    .min(Comparator.comparingDouble(p -> p.distSqr(worker.blockPosition())))
                    .orElse(colony.getNexusPos());
        }

        worker.setTaskState("courier_deliver");
        if (worker.blockPosition().distSqr(destPos) > 6.5D) {
            navTicks++;
            if (navTicks % 15 == 1 || worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(destPos.getX() + 0.5D, destPos.getY(), destPos.getZ() + 0.5D, 1.25D);
            }
            if (navTicks > 200) {
                phase = Phase.PICKUP;
                navTicks = 0;
            }
            return;
        }

        worker.getNavigation().stop();
        boolean moved = false;
        if (destPos.equals(colony.getNexusPos())) {
            for (int i = 0; i < worker.getPouch().getSlots(); i++) {
                ItemStack st = worker.getPouch().getStackInSlot(i);
                if (!st.isEmpty() && st.is(ModItems.MYCELIAL_BIOMASS.get())) {
                    int accepted = colony.addBiomass(st.getCount());
                    if (accepted > 0) {
                        st.shrink(accepted);
                        worker.getPouch().setStackInSlot(i, st);
                        moved = true;
                    }
                }
            }
            if (!worker.isPouchEmpty() && !colony.getOutputChests().isEmpty()) {
                // Keep non-biomass items to deliver to output chest next tick
                BlockPos altOut = colony.getOutputChests().iterator().next();
                if (altOut != null && !altOut.equals(destPos)) {
                    navTicks = 0;
                    if (moved) {
                        SovereignColonySavedData.get(level).setDirty();
                        worker.recordWorkCompleted(level, ColonyOrder.OrderType.COURIER_DEPOSIT, 6);
                    }
                    return;
                }
            }
        } else {
            moved = worker.depositPouchToContainer(level, destPos);
        }

        if (moved) {
            SovereignColonySavedData.get(level).setDirty();
            worker.recordWorkCompleted(level, ColonyOrder.OrderType.COURIER_DEPOSIT, 7);
            level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                    destPos.getX() + 0.5, destPos.getY() + 1.0, destPos.getZ() + 0.5,
                    5, 0.25, 0.25, 0.25, 0.02);
        }
        phase = Phase.PICKUP;
        navTicks = 0;
    }

    private boolean pullFromContainer(ServerLevel level, BlockPos pos, int maxTransfers) {
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null) {
            return false;
        }
        int transfers = 0;
        for (int i = 0; i < handler.getSlots() && transfers < maxTransfers; i++) {
            ItemStack sim = handler.extractItem(i, 64, true);
            if (sim.isEmpty()) {
                continue;
            }
            ItemStack leftover = ItemHandlerHelper.insertItem(worker.getPouch(), sim.copy(), true);
            int canTake = sim.getCount() - leftover.getCount();
            if (canTake > 0) {
                ItemStack extracted = handler.extractItem(i, canTake, false);
                if (!extracted.isEmpty()) {
                    worker.insertIntoPouch(extracted);
                    transfers++;
                }
            }
        }
        return transfers > 0;
    }
}
