package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/**
 * Opens a Mycelial Rift portal when used on top of a Reinforced Bio-Barrier block.
 */
public class RiftCatalystItem extends Item {
    public RiftCatalystItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos clicked = context.getClickedPos();
        if (context.getLevel() instanceof ServerLevel serverLevel) {
            if (!ModDimensions.supportsRiftTravel(serverLevel)) {
                player.sendSystemMessage(Component.translatable("message.mycelialmind.rift_unsupported_dimension")
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }
            if (!serverLevel.getBlockState(clicked).is(ModBlocks.REINFORCED_BIO_BARRIER.get())) {
                player.sendSystemMessage(Component.translatable("message.mycelialmind.rift_requires_barrier")
                        .withStyle(ChatFormatting.YELLOW));
                return InteractionResult.FAIL;
            }
            BlockPos riftPos = clicked.above();
            if (!serverLevel.isEmptyBlock(riftPos)) {
                return InteractionResult.FAIL;
            }
            if (!serverLevel.setBlockAndUpdate(riftPos, ModBlocks.MYCELIAL_RIFT.get().defaultBlockState())) {
                return InteractionResult.FAIL;
            }
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            hive.markDeepMyceliumBreached();
            HiveProgressionTier tier = HiveProgressionTier.fromThreat(
                    Math.max(hive.getThreatLevel(), hive.getHighestProgressionTier().minThreat())
            );
            if (HiveProgressionTier.isRiftResonantTier(tier)) {
                player.sendSystemMessage(Component.translatable("message.mycelialmind.rift_resonant_opened")
                        .withStyle(ChatFormatting.AQUA));
            } else {
                player.sendSystemMessage(Component.translatable("message.mycelialmind.rift_premature_warning")
                        .withStyle(ChatFormatting.GOLD));
            }
            serverLevel.sendParticles(ModParticles.ENGRAM.get(),
                    riftPos.getX() + 0.5, riftPos.getY() + 0.5, riftPos.getZ() + 0.5,
                    24, 0.4, 0.4, 0.4, 0.1);
            serverLevel.playSound(null, riftPos, ModSounds.RIFT_CATALYST_IGNITE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            context.getItemInHand().hurtAndBreak(1, player,
                    context.getHand() == net.minecraft.world.InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.SYNAPTIC_CORE.get()) || repair.is(Items.ENDER_PEARL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.rift_catalyst").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
