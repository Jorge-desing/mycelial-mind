package com.alex.mycelialmind.item;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.network.SymbiosisSyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.symbiosis.SymbiosisAttributeSync;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.util.MycelialParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Concentrated biological purge that cures Mycelial Infection, clears debuffs,
 * and purges all implanted Symbiotic Mutagens so new ones can be equipped.
 */
public class ConcentratedAntidoteItem extends Item {
    public ConcentratedAntidoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.removeEffectsCuredBy(ModEffects.MYCELIAL_ANTIDOTE_CURE);
            serverPlayer.removeEffect(MobEffects.POISON);
            serverPlayer.removeEffect(MobEffects.WEAKNESS);
            serverPlayer.removeEffect(MobEffects.HUNGER);
            serverPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1));
            MycelialParticles.antidote(serverLevel, serverPlayer, true);

            serverPlayer.getData(ModAttachments.SYMBIOSIS).clearMutagens();
            SymbiosisAttributeSync.sync(serverPlayer);
            SymbiosisSyncPayload.syncToPlayer(serverPlayer);

            level.playSound(null, serverPlayer.blockPosition(), ModSounds.SPORE_ANTIDOTE_CURE.get(), SoundSource.PLAYERS, 1.0F, 1.15F);
            serverPlayer.sendSystemMessage(Component.translatable("message.mycelialmind.symbiosis_purged")
                    .withStyle(ChatFormatting.GREEN));

            if (!serverPlayer.getAbilities().instabuild) {
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
            MycelialParticles.antidote(serverLevel, target, true);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 140, 1));
            player.level().playSound(null, target.blockPosition(), ModSounds.SPORE_ANTIDOTE_CURE.get(), SoundSource.PLAYERS, 0.9F, 1.2F);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.concentrated_antidote").withStyle(ChatFormatting.GREEN));
    }
}
