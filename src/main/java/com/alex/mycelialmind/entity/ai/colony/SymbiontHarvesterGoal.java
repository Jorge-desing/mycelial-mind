package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.block.BioluminescentMycelialCapBlock;
import com.alex.mycelialmind.block.MycelialTendrilsBlock;
import com.alex.mycelialmind.colony.ColonyOrder;
import com.alex.mycelialmind.colony.ColonyOrderBoard;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.SovereignTrait;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Harvester caste goal: claims HARVEST orders from ColonyOrderBoard, harvests mature
 * vanilla crops or bioluminescent mycelial flora, replants crops, collects drops, and
 * deposits yields into marked INPUT chests or Nexus biomass buffer.
 */
public class SymbiontHarvesterGoal extends Goal {
    private final ColonySymbiontEntity worker;
    @Nullable
    private ColonyOrder activeOrder;
    private int navTicks = 0;
    private int workTimer = 0;
    private boolean depositing = false;

    public SymbiontHarvesterGoal(ColonySymbiontEntity worker) {
        this.worker = worker;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        return worker.getCaste() == SymbiontCaste.HARVESTER
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
        depositing = false;
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

        if (worker.isPouchFull()) {
            depositing = true;
        }
        if (depositing) {
            tickDeposit(level, colony);
            return;
        }

        long now = level.getGameTime();
        if (activeOrder == null || !activeOrder.isReservedBy(worker.getUUID(), now)) {
            activeOrder = colony.getOrderBoard().claimBestOrder(
                    worker.getUUID(), SymbiontCaste.HARVESTER, worker.blockPosition(), now, ColonyOrderBoard.DEFAULT_LEASE_TICKS);
            navTicks = 0;
            workTimer = 0;
        }

        if (activeOrder == null) {
            if (!worker.isPouchEmpty()) {
                depositing = true;
                return;
            }
            worker.setTaskState("patrol_fields");
            orbitHomeIdle(colony);
            return;
        }

        BlockPos target = activeOrder.getTargetPos();
        if (!isHarvestable(level.getBlockState(target))) {
            colony.getOrderBoard().completeOrder(activeOrder.getOrderId(), worker.getUUID());
            activeOrder = null;
            return;
        }

        worker.setTaskState("harvesting");
        double distSq = worker.blockPosition().distSqr(target);
        if (distSq > 5.0D) {
            navTicks++;
            if (navTicks % 15 == 1 || worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(target.getX() + 0.5D, target.getY(), target.getZ() + 0.5D, 1.05D);
            }
            if (navTicks > 200) {
                colony.getOrderBoard().releaseWorkerReservations(worker.getUUID());
                activeOrder = null;
            }
            return;
        }

        worker.getNavigation().stop();
        worker.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.4D, target.getZ() + 0.5D);
        int interval = SovereignColonyEngine.computeWorkIntervalTicks(
                24, worker.getRank(), worker.getTemperament(), colony.getColonyMorale(),
                colony.hasTrait(SovereignTrait.SYMBIOTIC_HARVEST));
        workTimer++;
        if (workTimer < interval) {
            return;
        }

        executeHarvest(level, colony, target);
        colony.getOrderBoard().completeOrder(activeOrder.getOrderId(), worker.getUUID());
        activeOrder = null;
        workTimer = 0;
        if (!worker.isPouchEmpty()) {
            depositing = true;
        }
    }

    private void tickDeposit(ServerLevel level, SovereignColonyData colony) {
        worker.setTaskState("depositing");
        BlockPos chestPos = colony.getInputChests().stream()
                .min(Comparator.comparingDouble(p -> p.distSqr(worker.blockPosition())))
                .orElse(colony.getNexusPos());

        if (worker.blockPosition().distSqr(chestPos) > 6.5D) {
            navTicks++;
            if (navTicks % 15 == 1 || worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(chestPos.getX() + 0.5D, chestPos.getY(), chestPos.getZ() + 0.5D, 1.1D);
            }
            if (navTicks > 200) {
                depositing = false;
                navTicks = 0;
            }
            return;
        }

        worker.getNavigation().stop();
        if (!chestPos.equals(colony.getNexusPos())) {
            worker.depositPouchToContainer(level, chestPos);
        } else {
            // Convert any mycelial biomass in pouch directly into Nexus reserve if no INPUT chest exists
            for (int i = 0; i < worker.getPouch().getSlots(); i++) {
                ItemStack st = worker.getPouch().getStackInSlot(i);
                if (!st.isEmpty() && st.is(ModItems.MYCELIAL_BIOMASS.get())) {
                    int accepted = colony.addBiomass(st.getCount());
                    if (accepted > 0) {
                        st.shrink(accepted);
                        worker.getPouch().setStackInSlot(i, st);
                    }
                }
            }
        }
        depositing = false;
        navTicks = 0;
    }

    private void executeHarvest(ServerLevel level, SovereignColonyData colony, BlockPos target) {
        BlockState state = level.getBlockState(target);
        boolean bonusTrait = colony.hasTrait(SovereignTrait.SYMBIOTIC_HARVEST);
        if (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            List<ItemStack> drops = Block.getDrops(state, level, target, null, worker, ItemStack.EMPTY);
            for (ItemStack drop : drops) {
                if (drop.isEmpty()) continue;
                ItemStack rem = worker.insertIntoPouch(drop.copy());
                if (!rem.isEmpty()) {
                    Block.popResource(level, target, rem);
                }
            }
            if (bonusTrait) {
                worker.insertIntoPouch(new ItemStack(Items.WHEAT, 1));
            }
            level.setBlockAndUpdate(target, crop.getStateForAge(0));
        } else if (state.getBlock() instanceof BioluminescentMycelialCapBlock
                && state.getValue(BioluminescentMycelialCapBlock.MATURE)) {
            level.setBlockAndUpdate(target, state.setValue(BioluminescentMycelialCapBlock.MATURE, false));
            int yield = bonusTrait ? 2 : 1;
            ItemStack rem = worker.insertIntoPouch(new ItemStack(ModItems.MYCELIAL_BIOMASS.get(), yield));
            if (!rem.isEmpty()) {
                Block.popResource(level, target, rem);
            }
        } else if (state.getBlock() instanceof MycelialTendrilsBlock
                && state.getValue(MycelialTendrilsBlock.MATURE)) {
            level.setBlockAndUpdate(target, state.setValue(MycelialTendrilsBlock.MATURE, false));
            int yield = bonusTrait ? 2 : 1;
            ItemStack rem = worker.insertIntoPouch(new ItemStack(ModItems.MYCELIAL_BIOMASS.get(), yield));
            if (!rem.isEmpty()) {
                Block.popResource(level, target, rem);
            }
        }
        worker.pickupNearbyDrops(level, 2.5D);
        level.sendParticles(ModParticles.BIO_LUME.get(),
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.02);
        worker.recordWorkCompleted(level, ColonyOrder.OrderType.HARVEST, 8);
    }

    public static boolean isHarvestable(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        if (state.getBlock() instanceof BioluminescentMycelialCapBlock) {
            return state.getValue(BioluminescentMycelialCapBlock.MATURE);
        }
        if (state.getBlock() instanceof MycelialTendrilsBlock) {
            return state.getValue(MycelialTendrilsBlock.MATURE);
        }
        return false;
    }

    private void orbitHomeIdle(SovereignColonyData colony) {
        BlockPos anchor = colony.getNexusPos();
        if (worker.blockPosition().distSqr(anchor) > 49.0D && worker.getNavigation().isDone()) {
            worker.getNavigation().moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 1.0D);
        }
    }
}
