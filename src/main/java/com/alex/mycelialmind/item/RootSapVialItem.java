package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Bottled Root Sap harvested from Root Sap Moss inside Mycelial Sinkholes.
 * Grants Slow Falling and Night Vision while soothing mild Mycelial Infection.
 */
public class RootSapVialItem extends Item {
    public RootSapVialItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 600, 0));
            MobEffectInstance infection = entity.getEffect(ModEffects.MYCELIAL_INFECTION);
            if (infection != null) {
                if (infection.getAmplifier() <= 0) {
                    entity.removeEffect(ModEffects.MYCELIAL_INFECTION);
                } else {
                    entity.removeEffect(ModEffects.MYCELIAL_INFECTION);
                    entity.addEffect(new MobEffectInstance(
                            ModEffects.MYCELIAL_INFECTION,
                            Math.max(40, infection.getDuration() / 2),
                            infection.getAmplifier() - 1
                    ));
                }
            }
            MycelialParticles.antidote(serverLevel, entity, false);
            serverLevel.playSound(null, entity.blockPosition(), ModSounds.RESONANT_ELIXIR_DRINK.get(),
                    SoundSource.PLAYERS, 0.78F, 1.12F);
        }

        if (entity instanceof Player player) {
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
                if (stack.isEmpty()) {
                    return new ItemStack(Items.GLASS_BOTTLE);
                }
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().add(bottle)) {
                    player.drop(bottle, false);
                }
            }
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 24;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public SoundEvent getDrinkingSound() {
        return ModSounds.RESONANT_ELIXIR_DRINK.get();
    }

    @Override
    public SoundEvent getEatingSound() {
        return ModSounds.RESONANT_ELIXIR_DRINK.get();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.root_sap_vial")
                .withStyle(ChatFormatting.AQUA));
    }
}
