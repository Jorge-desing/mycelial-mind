package com.alex.mycelialmind.block;

import com.alex.mycelialmind.block.entity.SynapticBenchBlockEntity;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.util.MycelialRedstoneTelemetry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Biotechnology workbench for synthesizing Symbiotic Mutagens and targeted Counter-Serums.
 */
public class SynapticResearchBenchBlock extends Block implements EntityBlock {
    public SynapticResearchBenchBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (player.isSecondaryUseActive()
                && level.getBlockEntity(pos) instanceof SynapticBenchBlockEntity bench
                && bench.tryUpgradeLab(stack, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.getMainHandItem().is(com.alex.mycelialmind.registry.ModItems.BIO_SCANNER.get())
                || player.getOffhandItem().is(com.alex.mycelialmind.registry.ModItems.BIO_SCANNER.get())) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof SynapticBenchBlockEntity bench) {
            PlayerDiscoveryData.unlockMilestone(serverPlayer, ProgressionGoalEngine.MILESTONE_SYNAPTIC_BENCH);
            serverPlayer.openMenu(bench, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SynapticBenchBlockEntity bench) {
            bench.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SynapticBenchBlockEntity(pos, state);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof SynapticBenchBlockEntity bench) {
            var inv = bench.getInventory();
            int filledInputs = 0;
            for (int i = 0; i < SynapticBenchBlockEntity.SLOT_OUTPUT; i++) {
                if (!inv.getStackInSlot(i).isEmpty()) {
                    filledInputs++;
                }
            }
            boolean hasOutput = !inv.getStackInSlot(SynapticBenchBlockEntity.SLOT_OUTPUT).isEmpty();
            return MycelialRedstoneTelemetry.benchSignal(hasOutput, filledInputs);
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.synaptic_research_bench").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
