package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModParticles;
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
 * Refined subterranean potion synthesized from Root Sap Vial and Root Heart Resin.
 * Grants long subterranean exploration buffs, purges Mycelial Infection and Spore Rot,
 * and shields users against toxic Spore Updraft Vent exposure.
 */
public class ResonantSapElixirItem extends Item {
    public ResonantSapElixirItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel) {
            entity.removeEffectsCuredBy(ModEffects.MYCELIAL_ANTIDOTE_CURE);
            entity.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 4800, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 1800, 0));
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 240, 0));
            entity.addEffect(new MobEffectInstance(ModEffects.SYNAPTIC_INSIGHT, 1200, 0));

            MycelialParticles.antidote(serverLevel, entity, true);
            serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                    entity.getX(), entity.getY(0.5D), entity.getZ(),
                    14, 0.35D, 0.35D, 0.35D, 0.04D);
            serverLevel.playSound(null, entity.blockPosition(), ModSounds.RESONANT_ELIXIR_DRINK.get(),
                    SoundSource.PLAYERS, 0.90F, 1.05F);
        }

        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
            if (!player.getInventory().add(bottle)) {
                player.drop(bottle, false);
            }
        } else if (!(entity instanceof Player)) {
            stack.shrink(1);
        }
        return stack;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 28;
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
        tooltip.add(Component.translatable("tooltip.mycelialmind.item.resonant_sap_elixir")
                .withStyle(ChatFormatting.GOLD));
    }
}
