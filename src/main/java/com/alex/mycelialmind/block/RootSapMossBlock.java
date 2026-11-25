package com.alex.mycelialmind.block;

import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.SinkholeEcosystemEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Bioluminescent root-cavern moss substrate that accumulates golden-cyan sap.
 * Players can bottle Root Sap with a Glass Bottle or Bio-Extractor; Spore Gliders
 * and progression random ticks replenish it over time.
 */
public class RootSapMossBlock extends Block {
    public static final BooleanProperty SAP_READY = BooleanProperty.create("sap_ready");

    public RootSapMossBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SAP_READY, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SAP_READY);
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
        boolean isBottle = stack.is(Items.GLASS_BOTTLE);
        boolean isExtractor = stack.is(ModItems.BIO_EXTRACTOR.get());
        if (!isBottle && !isExtractor) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (!state.getValue(SAP_READY)) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.root_sap_moss_depleted")
                        .withStyle(ChatFormatting.YELLOW), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.setBlockAndUpdate(pos, state.setValue(SAP_READY, false));
            serverLevel.updateNeighbourForOutputSignal(pos, this);

            if (isBottle && !player.getAbilities().instabuild) {
                stack.shrink(1);
            } else if (isExtractor && !player.getAbilities().instabuild) {
                EquipmentSlot slot = hand == InteractionHand.OFF_HAND ? EquipmentSlot.OFFHAND : EquipmentSlot.MAINHAND;
                stack.hurtAndBreak(1, player, slot);
            }

            ItemStack vial = new ItemStack(ModItems.ROOT_SAP_VIAL.get());
            if (!player.getInventory().add(vial)) {
                Block.popResource(serverLevel, pos.above(), vial);
            }
            serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                    pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                    12, 0.3D, 0.2D, 0.3D, 0.03D);
            serverLevel.playSound(null, pos, isExtractor ? ModSounds.BIO_EXTRACTOR_DRAW.get() : ModSounds.ROOT_SAP_HARVEST.get(),
                    SoundSource.BLOCKS, 0.85F, 1.05F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return !state.getValue(SAP_READY);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SAP_READY)) {
            HiveMindSavedData hive = HiveMindSavedData.get(level);
            HiveProgressionTier tier = HiveProgressionTier.resolveEffectiveTier(
                    hive.getThreatLevel(), hive.getHighestTierReached());
            float chance = SinkholeEcosystemEngine.computeSapRegrowthChance(tier);
            if (random.nextFloat() < chance) {
                level.setBlockAndUpdate(pos, state.setValue(SAP_READY, true));
                level.updateNeighbourForOutputSignal(pos, this);
                level.sendParticles(ModParticles.BIO_LUME.get(),
                        pos.getX() + 0.5D, pos.getY() + 1.05D, pos.getZ() + 0.5D,
                        6, 0.25D, 0.15D, 0.25D, 0.01D);
            }
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return state.getValue(SAP_READY) ? 15 : 0;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.root_sap_moss")
                .withStyle(ChatFormatting.GREEN));
    }
}
