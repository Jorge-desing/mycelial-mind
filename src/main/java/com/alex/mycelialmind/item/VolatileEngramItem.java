package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModEffects;
import net.minecraft.ChatFormatting;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Intercepted volatile Hive memory engram dropped when purging a node's short-term buffer.
 * Can be consumed for 45s of Synaptic Insight (+15% dmg vs active traits) or crafted into a False Engram Injector.
 */
public class VolatileEngramItem extends Item {
    public static final int INSIGHT_DURATION_TICKS = 900; // 45s

    public VolatileEngramItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            player.addEffect(new MobEffectInstance(ModEffects.SYNAPTIC_INSIGHT, INSIGHT_DURATION_TICKS, 0));
            serverLevel.sendParticles(ModParticles.ENGRAM.get(),
                    player.getX(), player.getEyeY() - 0.25, player.getZ(),
                    1, 0, 0, 0, 0);
            serverLevel.playSound(null, player.blockPosition(),
                    ModSounds.VOLATILE_ENGRAM_PULSE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("message.mycelialmind.synaptic_insight_gained")
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.mycelialmind.volatile_engram").withStyle(ChatFormatting.AQUA));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
