package com.alex.mycelialmind.block;

import com.alex.mycelialmind.block.entity.SovereignNexusBlockEntity;
import com.alex.mycelialmind.colony.SovereignColonyData;
import com.alex.mycelialmind.colony.SovereignColonySavedData;
import com.alex.mycelialmind.network.SovereignColonySyncPayload;
import com.alex.mycelialmind.registry.ModBlockEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialRedstoneTelemetry;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Endgame Sovereign Hive Nexus block anchoring a player's cooperative colony,
 * worker incubation, biomass reserve, and Symbiotic Node outpost links.
 */
public class SovereignNexusBlock extends Block implements EntityBlock {
    public SovereignNexusBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SovereignNexusBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.SOVEREIGN_NEXUS.get()) {
            return null;
        }
        return (lvl, p, st, be) -> {
            if (be instanceof SovereignNexusBlockEntity nexus) {
                SovereignNexusBlockEntity.serverTick(lvl, p, st, nexus);
            }
        };
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof SovereignNexusBlockEntity be) {
            be.setOwner(player);
            level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return 0;
        }
        SovereignColonyData colony = SovereignColonySavedData.get(serverLevel).getByNexus(pos);
        if (colony == null) {
            return 0;
        }
        return MycelialRedstoneTelemetry.sovereignNexusSignal(
                colony.getTier(),
                colony.getRoster().size(),
                colony.getPopulationCap(),
                colony.getBiomassStored()
        );
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            SovereignColonySavedData.get(serverLevel).removeColonyAt(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (stack.is(ModItems.SOVEREIGN_COMMAND_STAFF.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ModItems.MYCELIAL_BIOMASS.get())) {
            if (level instanceof ServerLevel serverLevel
                    && level.getBlockEntity(pos) instanceof SovereignNexusBlockEntity be) {
                SovereignColonyData colony = be.getOrCreateColony(serverLevel);
                int toInsert = player.isSecondaryUseActive() ? Math.min(16, stack.getCount()) : 1;
                int added = colony.addBiomass(toInsert);
                if (added > 0) {
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(added);
                    }
                    colony.recalculateMorale();
                    SovereignColonySavedData.get(serverLevel).setDirty();
                    serverLevel.updateNeighbourForOutputSignal(pos, this);
                    serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                            pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 8, 0.35, 0.35, 0.35, 0.03);
                    serverLevel.playSound(null, pos, ModSounds.SOVEREIGN_NEXUS_PULSE.get(), SoundSource.BLOCKS, 0.85F, 1.2F);
                    player.displayClientMessage(Component.translatable(
                            "message.mycelialmind.sovereign_biomass_added",
                            added, colony.getBiomassStored()).withStyle(ChatFormatting.GREEN), true);
                    if (player instanceof ServerPlayer serverPlayer) {
                        SovereignColonySyncPayload.syncToPlayer(serverPlayer, pos, false);
                    }
                } else {
                    player.displayClientMessage(Component.translatable("message.mycelialmind.sovereign_biomass_full")
                            .withStyle(ChatFormatting.YELLOW), true);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof SovereignNexusBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (!SovereignNexusBlockEntity.isSovereignUnlocked(serverLevel) && !player.isCreative()) {
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.sovereign_nexus_locked").withStyle(ChatFormatting.DARK_PURPLE),
                    true);
            return InteractionResult.CONSUME;
        }
        SovereignColonyData colony = be.getOrCreateColony(serverLevel);
        if (be.getOwnerUuid() == null) {
            be.setOwner(player);
        }
        be.syncLiveWorkers(serverLevel, colony);
        colony.recalculateMorale();
        if (player instanceof ServerPlayer serverPlayer) {
            SovereignColonySyncPayload.syncToPlayer(serverPlayer, pos, true);
        }
        serverLevel.playSound(null, pos, ModSounds.SOVEREIGN_NEXUS_PULSE.get(), SoundSource.BLOCKS, 0.7F, 1.1F);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.sovereign_nexus").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.mycelialmind.sovereign_nexus.hint").withStyle(ChatFormatting.AQUA));
    }
}
