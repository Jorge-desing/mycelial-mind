package com.alex.mycelialmind.entity.ai.colony;

import com.alex.mycelialmind.colony.ColonyOrder;
import com.alex.mycelialmind.colony.ColonyOrderBoard;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonyEngine;
import com.alex.mycelialmind.colony.SovereignTrait;
import com.alex.mycelialmind.colony.SymbiontCaste;
import com.alex.mycelialmind.entity.ColonySymbiontEntity;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/**
 * Excavator caste goal: claims EXCAVATE orders from ColonyOrderBoard within marked MINE zones,
 * mines natural ores and stone safely (never breaking containers, altars, pylons, or nodes),
 * and deposits spoils into INPUT chests.
 */
public class SymbiontExcavatorGoal extends AbstractSymbiontCasteGoal {
    @Nullable
    private ColonyOrder activeOrder;
    private int navTicks = 0;
    private int workTimer = 0;
    private boolean depositing = false;

    public SymbiontExcavatorGoal(ColonySymbiontEntity worker) {
        super(worker, SymbiontCaste.EXCAVATOR, EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public void stop() {
        releaseOrderReservationAndIdle(activeOrder != null);
        activeOrder = null;
        depositing = false;
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
                    worker.getUUID(), SymbiontCaste.EXCAVATOR, worker.blockPosition(), now, ColonyOrderBoard.DEFAULT_LEASE_TICKS);
            navTicks = 0;
            workTimer = 0;
        }

        if (activeOrder == null) {
            if (!worker.isPouchEmpty()) {
                depositing = true;
                return;
            }
            worker.setTaskState("standby_quarry");
            BlockPos anchor = colony.getNexusPos();
            if (worker.blockPosition().distSqr(anchor) > 64.0D && worker.getNavigation().isDone()) {
                worker.getNavigation().moveTo(anchor.getX() + 0.5D, anchor.getY(), anchor.getZ() + 0.5D, 1.0D);
            }
            return;
        }

        BlockPos target = activeOrder.getTargetPos();
        BlockState state = level.getBlockState(target);
        if (!isSafeExcavatable(level, target, state)) {
            colony.getOrderBoard().completeOrder(activeOrder.getOrderId(), worker.getUUID());
            activeOrder = null;
            return;
        }

        worker.setTaskState("excavating");
        if (worker.blockPosition().distSqr(target) > 6.5D) {
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
        worker.getLookControl().setLookAt(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D);
        int interval = SovereignColonyEngine.computeWorkIntervalTicks(
                28, worker.getRank(), worker.getTemperament(), colony.getColonyMorale(),
                colony.hasTrait(SovereignTrait.DEEP_QUARRY_SENSE));
        workTimer++;
        if (workTimer < interval) {
            return;
        }

        ItemStack tool = new ItemStack(Items.NETHERITE_PICKAXE);
        List<ItemStack> drops = Block.getDrops(state, level, target, null, worker, tool);
        level.destroyBlock(target, false);
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;
            if (colony.hasTrait(SovereignTrait.DEEP_QUARRY_SENSE) && worker.getRandom().nextFloat() < 0.25F) {
                drop.grow(1);
            }
            ItemStack rem = worker.insertIntoPouch(drop.copy());
            if (!rem.isEmpty()) {
                Block.popResource(level, target, rem);
            }
        }
        worker.pickupNearbyDrops(level, 2.5D);
        level.sendParticles(ModParticles.SYNAPSE_SPARK.get(),
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.03);
        worker.recordWorkCompleted(level, ColonyOrder.OrderType.EXCAVATE, 10);
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
                .orElse(null);
        if (chestPos == null) {
            depositing = false;
            return;
        }
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
        worker.depositPouchToContainer(level, chestPos);
        depositing = false;
        navTicks = 0;
    }

    public static boolean isSafeExcavatable(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getBlock() instanceof EntityBlock) {
            return false;
        }
        if (state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (state.is(ModBlocks.SOVEREIGN_NEXUS.get())
                || state.is(ModBlocks.SYMBIOTIC_NODE.get())
                || state.is(ModBlocks.PURIFICATION_PYLON.get())
                || state.is(ModBlocks.PURIFICATION_TURRET.get())
                || state.is(ModBlocks.REINFORCED_BIO_BARRIER.get())) {
            return false;
        }
        return state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.REDSTONE_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(ModBlocks.MYCELIAL_STONE.get());
    }
}
