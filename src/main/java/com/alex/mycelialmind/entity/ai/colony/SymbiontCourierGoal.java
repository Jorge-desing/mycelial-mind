package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.colony.ColonyOrder;
import com.alex.mycelialmind.colony.ColonyOrderBoard;
import com.alex.mycelialmind.colony.ColonySiteHabit;
import com.alex.mycelialmind.colony.ColonySiteMemory;
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
import java.util.UUID;

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
    private int transferNavTicks;
    private long memoryTransferRetryAfter;
    private int escortWaitTicks;
    @Nullable
    private BlockPos activePickupSite;
    @Nullable
    private BlockPos activeEscortSite;
    @Nullable
    private BlockPos pickupTurnSite;
    private boolean waitedForPickupTurn;
    private boolean escortRequested;
    private boolean escortReady;
    private boolean damagedDuringCurrentTrip;

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
        if (worker.level() instanceof ServerLevel level) {
            SovereignColonyData colony = worker.getColonyData(level);
            if (colony != null) {
                if (activeOrder != null) {
                    colony.getOrderBoard().releaseWorkerReservations(worker.getUUID());
                }
                if (pickupTurnSite != null) {
                    ColonySiteMemory memory = colony.getSiteMemory(pickupTurnSite);
                    if (memory != null) {
                        memory.releasePickupTurn(worker.getUUID());
                    }
                }
                if (activeEscortSite != null) {
                    ColonySiteMemory memory = colony.getExistingSiteMemory(activeEscortSite);
                    if (memory != null) {
                        memory.clearEscort(worker.getUUID());
                    }
                }
                colony.releaseMemoryTransferCourier(worker.getUUID());
                SovereignColonySavedData.get(level).setDirty();
            }
        }
        activeOrder = null;
        pickupTurnSite = null;
        activePickupSite = null;
        activeEscortSite = null;
        waitedForPickupTurn = false;
        escortRequested = false;
        escortReady = false;
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

        long now = level.getGameTime();
        if (worker.isPouchEmpty() && !colony.isMemoryTransferAssignedTo(worker.getUUID())
                && now >= memoryTransferRetryAfter) {
            colony.claimMemoryTransfer(worker.getUUID(), now);
        }
        SovereignColonyData.MemoryTransferSnapshot transfer = colony.getMemoryTransferFor(worker.getUUID());
        if (transfer != null && worker.isPouchEmpty()) {
            tickMemoryTransfer(level, colony, transfer);
            return;
        }

        recordCourierDamage(level, colony);

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

        BlockPos site = colony.findNearestMemorySite(targetChest, 16.0D);
        if (site != null && !site.equals(activePickupSite)) {
            damagedDuringCurrentTrip = false;
        }
        activePickupSite = site;

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
        ColonySiteMemory siteMemory = site != null ? colony.getSiteMemory(site) : null;
        if (siteMemory != null && siteMemory.observePickupArrival(worker.getUUID(), now)) {
            level.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                    site.getX() + 0.5D, site.getY() + 1.0D, site.getZ() + 0.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            SovereignColonySavedData.get(level).setDirty();
        }
        if (siteMemory != null && siteMemory.usesHabit(ColonySiteHabit.STAGGERED_PICKUP)) {
            if (!siteMemory.tryAcquirePickupTurn(worker.getUUID(), now, ColonyOrderBoard.DEFAULT_LEASE_TICKS)) {
                waitedForPickupTurn = true;
                worker.setTaskState("memory_waiting_turn");
                waitTicks = 0;
                return;
            }
            pickupTurnSite = site;
        }
        int delay = SovereignColonyEngine.computeWorkIntervalTicks(
                14, worker.getRank(), worker.getTemperament(), colony.getColonyMorale(),
                colony.hasTrait(SovereignTrait.HYPHAL_LOGISTICS));
        waitTicks++;
        if (waitTicks < delay) {
            return;
        }

        boolean pulled = pullFromContainer(level, targetChest, colony.hasTrait(SovereignTrait.HYPHAL_LOGISTICS) ? 2 : 1);
        if (pulled && site != null && colony.getInputChests().contains(targetChest)) {
            worker.rememberCourierChest(level, site, targetChest, true);
        }
        if (pickupTurnSite != null) {
            ColonySiteMemory memory = colony.getSiteMemory(pickupTurnSite);
            if (memory != null) {
                memory.releasePickupTurn(worker.getUUID());
                if (waitedForPickupTurn) {
                    if (pulled) {
                        boolean learned = memory.recordTrialSuccess(ColonySiteHabit.STAGGERED_PICKUP, 3);
                        colony.recordDialectEpisode(level, pickupTurnSite, ColonySiteHabit.STAGGERED_PICKUP, worker);
                        SovereignColonySavedData.get(level).setDirty();
                        if (memory.usesHabit(ColonySiteHabit.STAGGERED_PICKUP)) {
                            worker.rememberSuccessfulHabit(level, pickupTurnSite, ColonySiteHabit.STAGGERED_PICKUP);
                        }
                        if (learned) {
                            emitMemoryPulse(level, pickupTurnSite);
                        }
                    } else {
                        memory.recordTrialFailure(ColonySiteHabit.STAGGERED_PICKUP, 3);
                    }
                    SovereignColonySavedData.get(level).setDirty();
                }
            }
            pickupTurnSite = null;
        }
        waitedForPickupTurn = false;
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
        if (activePickupSite == null) {
            activePickupSite = colony.findNearestMemorySite(worker.blockPosition(), 16.0D);
        }
        if (prepareCourierEscort(level, colony)) {
            return;
        }
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
            if (colony.getOutputChests().contains(destPos)) {
                BlockPos outputSite = colony.findNearestMemorySite(destPos, 16.0D);
                if (outputSite != null) {
                    worker.rememberCourierChest(level, outputSite, destPos, false);
                }
            }
            if (activeEscortSite != null) {
                ColonySiteMemory memory = colony.getSiteMemory(activeEscortSite);
                if (memory != null) {
                    if (escortReady && !damagedDuringCurrentTrip) {
                        boolean learned = memory.recordTrialSuccess(ColonySiteHabit.ESCORTED_COURIER, 1);
                        colony.recordDialectEpisode(level, activeEscortSite, ColonySiteHabit.ESCORTED_COURIER, worker);
                        SovereignColonySavedData.get(level).setDirty();
                        if (memory.usesHabit(ColonySiteHabit.ESCORTED_COURIER)) {
                            worker.rememberSuccessfulHabit(level, activeEscortSite, ColonySiteHabit.ESCORTED_COURIER);
                        }
                        if (learned) {
                            emitMemoryPulse(level, activeEscortSite);
                        }
                    } else if (memory.trialHabit() == ColonySiteHabit.ESCORTED_COURIER) {
                        memory.recordTrialFailure(ColonySiteHabit.ESCORTED_COURIER, 1);
                    }
                    memory.clearEscort(worker.getUUID());
                }
            }
            level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                    destPos.getX() + 0.5, destPos.getY() + 1.0, destPos.getZ() + 0.5,
                    5, 0.25, 0.25, 0.25, 0.02);
        }
        phase = Phase.PICKUP;
        navTicks = 0;
        activePickupSite = null;
        activeEscortSite = null;
        escortRequested = false;
        escortReady = false;
        escortWaitTicks = 0;
        damagedDuringCurrentTrip = false;
    }

    private boolean prepareCourierEscort(ServerLevel level, SovereignColonyData colony) {
        if (activePickupSite == null || !colony.usesSiteHabit(activePickupSite, ColonySiteHabit.ESCORTED_COURIER)) {
            return false;
        }
        ColonySiteMemory memory = colony.getSiteMemory(activePickupSite);
        if (memory == null) {
            return false;
        }
        activeEscortSite = activePickupSite;
        if (!escortRequested) {
            escortRequested = memory.beginCourierEscort(worker.getUUID(), level.getGameTime());
            escortWaitTicks = 0;
            escortReady = false;
            if (!escortRequested) {
                activeEscortSite = null;
                return false;
            }
        }
        UUID sentinelId = memory.escortSentinel(level.getGameTime());
        if (sentinelId != null && level.getEntity(sentinelId) instanceof ColonySymbiontEntity sentinel
                && sentinel.getCaste() == SymbiontCaste.SENTINEL && sentinel.isAlive()
                && sentinel.distanceToSqr(worker) <= 16.0D) {
            escortReady = true;
            return false;
        }
        if (escortWaitTicks++ < 100) {
            worker.getNavigation().stop();
            worker.setTaskState("courier_awaiting_escort");
            return true;
        }
        if (memory.trialHabit() == ColonySiteHabit.ESCORTED_COURIER) {
            memory.recordTrialFailure(ColonySiteHabit.ESCORTED_COURIER, 1);
        }
        memory.clearEscort(worker.getUUID());
        escortRequested = false;
        activeEscortSite = null;
        SovereignColonySavedData.get(level).setDirty();
        return false;
    }

    private void recordCourierDamage(ServerLevel level, SovereignColonyData colony) {
        if (!worker.takeCourierDamageForMemory()) {
            return;
        }
        BlockPos site = activePickupSite != null ? activePickupSite
                : colony.findNearestMemorySite(worker.blockPosition(), 16.0D);
        if (site == null) {
            return;
        }
        ColonySiteMemory memory = colony.getSiteMemory(site);
        if (memory != null) {
            boolean trialStarted = memory.recordIncident(ColonySiteHabit.ESCORTED_COURIER, 1, level.getGameTime());
            if (!trialStarted) {
                memory.recordTrialFailure(ColonySiteHabit.ESCORTED_COURIER, 1);
            }
            memory.clearEscort(worker.getUUID());
            damagedDuringCurrentTrip = true;
            level.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                    site.getX() + 0.5D, site.getY() + 1.0D, site.getZ() + 0.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.0D);
            SovereignColonySavedData.get(level).setDirty();
        }
        activeEscortSite = null;
        escortRequested = false;
        escortReady = false;
    }

    private void tickMemoryTransfer(ServerLevel level, SovereignColonyData colony,
                                    SovereignColonyData.MemoryTransferSnapshot transfer) {
        BlockPos target = transfer.inTransit() ? transfer.targetPos() : transfer.sourcePos();
        worker.setTaskState(transfer.inTransit() ? "memory_teaching" : "memory_learning_route");
        if (!level.isLoaded(target)) {
            worker.getNavigation().stop();
            if (++transferNavTicks > 400) {
                colony.releaseMemoryTransferCourier(worker.getUUID());
                memoryTransferRetryAfter = level.getGameTime() + 600L;
                worker.setTaskState("idle");
                transferNavTicks = 0;
                SovereignColonySavedData.get(level).setDirty();
            }
            return;
        }
        colony.touchMemoryTransfer(worker.getUUID(), level.getGameTime());
        if (transfer.inTransit() && level.getGameTime() % 20L == 0L) {
            level.sendParticles(ModParticles.BIO_LUME.get(), worker.getX(), worker.getY() + 0.65D,
                    worker.getZ(), 1, 0.04D, 0.08D, 0.04D, 0.0D);
        }
        if (worker.blockPosition().distSqr(target) > 6.5D) {
            transferNavTicks++;
            if (transferNavTicks % 15 == 1 || worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.15D);
            }
            if (transferNavTicks > 400) {
                colony.releaseMemoryTransferCourier(worker.getUUID());
                memoryTransferRetryAfter = level.getGameTime() + 600L;
                worker.getNavigation().stop();
                worker.setTaskState("idle");
                transferNavTicks = 0;
                SovereignColonySavedData.get(level).setDirty();
            }
            return;
        }
        worker.getNavigation().stop();
        if (!transfer.inTransit()) {
            colony.markMemoryTransferCollected(worker.getUUID(), level.getGameTime());
            worker.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            level.sendParticles(ModParticles.BIO_LUME.get(), target.getX() + 0.5D, target.getY() + 1.0D,
                    target.getZ() + 0.5D, 5, 0.2D, 0.2D, 0.2D, 0.02D);
        } else {
            boolean delivered = colony.deliverMemoryTransfer(worker.getUUID(), level.getGameTime());
            if (delivered) {
                emitMemoryPulse(level, target);
                level.sendParticles(ModParticles.PURIFIER_CROSS.get(), target.getX() + 0.5D,
                        target.getY() + 1.0D, target.getZ() + 0.5D, 7, 0.25D, 0.25D, 0.25D, 0.02D);
                worker.recordWorkCompleted(level, ColonyOrder.OrderType.COURIER_DEPOSIT, 5);
            } else {
                colony.cancelMemoryTransfer();
            }
            worker.setTaskState("idle");
        }
        transferNavTicks = 0;
        SovereignColonySavedData.get(level).setDirty();
    }

    private static void emitMemoryPulse(ServerLevel level, BlockPos sitePos) {
        level.sendParticles(ModParticles.PSYCHIC_WAVE.get(),
                sitePos.getX() + 0.5D, sitePos.getY() + 1.0D, sitePos.getZ() + 0.5D,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
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
