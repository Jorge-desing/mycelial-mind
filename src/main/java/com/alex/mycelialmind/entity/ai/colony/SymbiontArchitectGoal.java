package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity;
import com.alex.mycelialmind.colony.ColonyOrder;
import com.alex.mycelialmind.colony.ColonyOrderBoard;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.SovereignTrait;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * Architect caste goal: restores Symbiotic Node stability, clears jammed Purification Turrets,
 * purifies invasive Creeping Mycelium, and reinforces marked BUILD positions.
 */
public class SymbiontArchitectGoal extends AbstractSymbiontCasteGoal {
    @Nullable
    private ColonyOrder activeOrder;
    private int navTicks = 0;
    private int workTimer = 0;

    public SymbiontArchitectGoal(ColonySymbiontEntity worker) {
        super(worker, SymbiontCaste.ARCHITECT, EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public void stop() {
        releaseOrderReservationAndIdle(activeOrder != null);
        activeOrder = null;
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
        if (activeOrder == null || !activeOrder.isReservedBy(worker.getUUID(), now)) {
            activeOrder = colony.getOrderBoard().claimBestOrder(
                    worker.getUUID(), SymbiontCaste.ARCHITECT, worker.blockPosition(), now, ColonyOrderBoard.DEFAULT_LEASE_TICKS);
            navTicks = 0;
            workTimer = 0;
        }

        if (activeOrder == null) {
            worker.setTaskState("inspecting_bastion");
            BlockPos anchor = colony.getNexusPos();
            if (worker.blockPosition().distSqr(anchor) > 49.0D && worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 1.05D);
            }
            return;
        }

        BlockPos target = activeOrder.getTargetPos();
        if (!needsArchitectAction(level, colony, target)) {
            colony.getOrderBoard().completeOrder(activeOrder.getOrderId(), worker.getUUID());
            activeOrder = null;
            return;
        }

        worker.setTaskState("repairing");
        if (worker.blockPosition().distSqr(target) > 6.5D) {
            navTicks++;
            if (navTicks % 15 == 1 || worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.1D);
            }
            if (navTicks > 200) {
                colony.getOrderBoard().releaseWorkerReservations(worker.getUUID());
                activeOrder = null;
            }
            return;
        }

        worker.getNavigation().stop();
        worker.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        int interval = SovereignColonyEngine.computeWorkIntervalTicks(
                22, worker.getRank(), worker.getTemperament(), colony.getColonyMorale(),
                colony.hasTrait(SovereignTrait.LIVING_BULWARK));
        workTimer++;
        if (workTimer < interval) {
            return;
        }

        executeMaintenance(level, colony, target);
        colony.getOrderBoard().completeOrder(activeOrder.getOrderId(), worker.getUUID());
        activeOrder = null;
        workTimer = 0;
    }

    private void executeMaintenance(ServerLevel level, SovereignColonyData colony, BlockPos target) {
        BlockEntity be = level.getBlockEntity(target);
        if (be instanceof SynapticNodeBlockEntity node && node.getSymbioticStability() < 100) {
            node.setSymbioticStability(100);
            level.playSound(null, target, ModSounds.SYMBIOTIC_NODE_CHARGE.get(), worker.getSoundSource(), 0.7F, 1.2F);
        } else if (be instanceof PurificationTurretBlockEntity turret && turret.isJammed()) {
            turret.clearCorrosiveJam();
            level.playSound(null, target, ModSounds.PURIFICATION_TURRET_REPAIR.get(), worker.getSoundSource(), 0.8F, 1.15F);
        } else {
            BlockState state = level.getBlockState(target);
            if (state.is(ModBlocks.CREEPING_MYCELIUM.get())) {
                level.setBlockAndUpdate(target, Blocks.MOSS_BLOCK.defaultBlockState());
            } else if (colony.getWorkZones(SovereignColonyData.WorkZoneType.BUILD).contains(target)
                    && (state.isAir() || state.canBeReplaced())
                    && colony.consumeBiomass(1)) {
                level.setBlockAndUpdate(target, ModBlocks.REINFORCED_BIO_BARRIER.get().defaultBlockState());
            }
        }
        level.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                target.getX() + 0.5, target.getY() + 0.8, target.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.03);
        worker.recordWorkCompleted(level, ColonyOrder.OrderType.REPAIR_STRUCTURE, 10);
    }

    public static boolean needsArchitectAction(ServerLevel level, SovereignColonyData colony, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof SynapticNodeBlockEntity node && node.getSymbioticStability() < 90) {
            return true;
        }
        if (be instanceof PurificationTurretBlockEntity turret && turret.isJammed()) {
            return true;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.CREEPING_MYCELIUM.get())) {
            return true;
        }
        return colony.getWorkZones(SovereignColonyData.WorkZoneType.BUILD).contains(pos)
                && (state.isAir() || state.canBeReplaced())
                && colony.getBiomassStored() > 0;
    }
}
