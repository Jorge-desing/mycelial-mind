package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.hive.MycorrhizalAcousticTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Tactical bio-acoustic decoy that emits false seismic vibrations for 12 seconds,
 * luring nearby Hive patrols away and masking the player's footsteps.
 */
public class NeuralEchoDecoyItem extends Item {
    public static final int DECOY_DURATION_TICKS = 240;

    public NeuralEchoDecoyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player != null && player.getCooldowns().isOnCooldown(this)) {
            return InteractionResult.FAIL;
        }
        Level level = context.getLevel();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (player == null) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        deployDecoy(serverLevel, pos, player, context.getItemInHand());
        return InteractionResult.CONSUME;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        deployDecoy(serverLevel, player.blockPosition(), player, stack);
        return InteractionResultHolder.consume(stack);
    }

    private static void deployDecoy(ServerLevel level, BlockPos pos, Player player, ItemStack stack) {
        MycorrhizalAcousticTracker.registerDecoy(level, pos, DECOY_DURATION_TICKS);
        level.sendParticles(ModParticles.ENGRAM.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                10, 0.4, 0.3, 0.4, 0.03);
        level.playSound(null, pos, ModSounds.NEURAL_DECOY_DEPLOY.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
        if (player != null) {
            player.getCooldowns().addCooldown(stack.getItem(), 200);
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.decoy_deployed")
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.mycelialmind.neural_echo_decoy").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
