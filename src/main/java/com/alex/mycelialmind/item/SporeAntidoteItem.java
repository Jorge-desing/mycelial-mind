package com.alex.mycelialmind.item;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.util.MycelialParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Consumable antidote that clears spore-induced debuffs and grants brief regeneration.
 */
public class SporeAntidoteItem extends Item {
    public SporeAntidoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            player.removeEffectsCuredBy(ModEffects.MYCELIAL_ANTIDOTE_CURE);
            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.HUNGER);
            player.removeEffect(MobEffects.DIG_SLOWDOWN);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            MycelialParticles.antidote(serverLevel, player, false);
            serverLevel.playSound(null, player.blockPosition(), ModSounds.SPORE_ANTIDOTE_CURE.get(), SoundSource.PLAYERS, 0.85F, 1.0F);
            if (!serverLevel.getEntitiesOfClass(MycelialMob.class, player.getBoundingBox().inflate(16.0D)).isEmpty()) {
                com.alex.mycelialmind.hive.SynapticMemoryRouter.routeHabit(
                        serverLevel, player.blockPosition(), com.alex.mycelialmind.hive.PlayerHabit.COMBAT_MEDIC, 2.5);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof MycelialMob
                || (!target.hasEffect(ModEffects.MYCELIAL_INFECTION) && !target.hasEffect(ModEffects.SPORE_ROT))) {
            return InteractionResult.PASS;
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            target.removeEffectsCuredBy(ModEffects.MYCELIAL_ANTIDOTE_CURE);
            MycelialParticles.antidote(serverLevel, target, false);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            player.level().playSound(null, target.blockPosition(), ModSounds.SPORE_ANTIDOTE_CURE.get(), SoundSource.PLAYERS, 0.85F, 1.05F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.spore_antidote").withStyle(ChatFormatting.GREEN));
    }
}
