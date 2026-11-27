package com.alex.mycelialmind.block;

import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.PlayerHabit;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.hive.SynapticMemoryRouter;
import com.alex.mycelialmind.registry.ModAdvancements;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.util.ClientTooltipHelper;
import com.alex.mycelialmind.util.MycelialRedstoneTelemetry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Autonomous base defense turret firing NeutralizingSporeProjectiles at MycelialMobs.
 */
public class PurificationTurretBlock extends Block implements ServerTickingEntityBlock {
    public PurificationTurretBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PurificationTurretBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return ServerTickingEntityBlock.createServerTicker(
                level, type, ModBlockEntities.PURIFICATION_TURRET.get(), PurificationTurretBlockEntity::serverTick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(ModItems.BIO_SCANNER.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof PurificationTurretBlockEntity turret) {
            if (turret.tryCleanJam(stack, player)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            if (turret.resolveNextTierForCatalyst(stack) > 0) {
                if (turret.tryUpgrade(stack, player)) {
                    if (level instanceof ServerLevel serverLevel && !player.isCreative()) {
                        SynapticMemoryRouter.routeHabit(
                                serverLevel, pos, PlayerHabit.BASTION_BUILDER, 4.0D * turret.getTier());
                    }
                    if (player instanceof ServerPlayer serverPlayer) {
                        PlayerDiscoveryData.unlockMilestone(
                                serverPlayer, ProgressionGoalEngine.MILESTONE_STATION_UPGRADED);
                        if (turret.getTier() >= 3) {
                            ModAdvancements.award(serverPlayer, ModAdvancements.BASTION_TIER3);
                        }
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
                if (level instanceof ServerLevel) {
                    return ItemInteractionResult.FAIL;
                }
            }
            if (turret.tryReload(stack, player)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PurificationTurretBlockEntity turret) {
            return MycelialRedstoneTelemetry.turretSignal(turret.getTier(), turret.isJammed());
        }
        return 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.purification_turret").withStyle(ChatFormatting.AQUA));
        ClientTooltipHelper.appendShiftDetails(tooltip,
                "tooltip.mycelialmind.block.purification_turret.shift_gate",
                "tooltip.mycelialmind.block.purification_turret.shift_ammo");
    }
}
