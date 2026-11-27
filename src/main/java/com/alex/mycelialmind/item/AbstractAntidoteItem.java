package com.alex.mycelialmind.item;

import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Shared base for spore-purging antidote consumables (self-cure helpers, entity cleansing, and tooltips).
 */
public abstract class AbstractAntidoteItem extends Item {
    private final boolean concentrated;
    private final int selfRegenTicks;
    private final int selfRegenAmplifier;
    private final int targetRegenTicks;
    private final int targetRegenAmplifier;
    private final float targetPitch;
    private final String tooltipKey;

    protected AbstractAntidoteItem(
            Properties properties,
            boolean concentrated,
            int selfRegenTicks,
            int selfRegenAmplifier,
            int targetRegenTicks,
            int targetRegenAmplifier,
            float targetPitch,
            String tooltipKey
    ) {
        super(properties);
        this.concentrated = concentrated;
        this.selfRegenTicks = selfRegenTicks;
        this.selfRegenAmplifier = selfRegenAmplifier;
        this.targetRegenTicks = targetRegenTicks;
        this.targetRegenAmplifier = targetRegenAmplifier;
        this.targetPitch = targetPitch;
        this.tooltipKey = tooltipKey;
    }

    protected final void applyBaseSelfCure(ServerLevel serverLevel, Player player) {
        player.removeEffectsCuredBy(ModEffects.MYCELIAL_ANTIDOTE_CURE);
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WEAKNESS);
        player.removeEffect(MobEffects.HUNGER);
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, selfRegenTicks, selfRegenAmplifier));
        MycelialParticles.antidote(serverLevel, player, concentrated);
    }

    @Override
    public InteractionResult interactLivingEntity(
            ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (target instanceof MycelialMob
                || (!target.hasEffect(ModEffects.MYCELIAL_INFECTION) && !target.hasEffect(ModEffects.SPORE_ROT))) {
            return InteractionResult.PASS;
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            target.removeEffectsCuredBy(ModEffects.MYCELIAL_ANTIDOTE_CURE);
            MycelialParticles.antidote(serverLevel, target, concentrated);
            target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, targetRegenTicks, targetRegenAmplifier));
            player.level().playSound(null, target.blockPosition(), ModSounds.SPORE_ANTIDOTE_CURE.get(),
                    SoundSource.PLAYERS, concentrated ? 0.9F : 0.85F, targetPitch);
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(ChatFormatting.GREEN));
    }
}
