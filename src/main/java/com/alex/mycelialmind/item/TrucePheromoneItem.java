package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.diplomacy.PlayerDiplomacyData;
import com.alex.mycelialmind.network.DiplomacySyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * Grants 20s of Cryptic Truce camouflage against BASE and MUTATED MycelialMobs
 * (never ELITE or bosses), broken immediately with a betrayal penalty if the player attacks.
 */
public class TrucePheromoneItem extends Item {
    public static final int TRUCE_DURATION_TICKS = 400;
    public static final int COOLDOWN_TICKS = 300;

    public TrucePheromoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer sp) {
            PlayerDiplomacyData dip = sp.getData(ModAttachments.DIPLOMACY);
            if (dip.getBetrayalCooldownTicks() > 0) {
                sp.displayClientMessage(Component.translatable("message.mycelialmind.diplomacy_betrayal_locked",
                        (dip.getBetrayalCooldownTicks() + 19) / 20).withStyle(ChatFormatting.RED), true);
                return InteractionResultHolder.fail(stack);
            }
            dip.setTruceTicks(TRUCE_DURATION_TICKS);
            com.alex.mycelialmind.compat.curios.MycelialCuriosCompat.adjustReputationWithSeal(
                    dip, sp, dip.getSelectedStrain(), 4);
            DiplomacySyncPayload.syncToPlayer(sp);

            if (DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)) {
                DimensionalHomeostasisDirector.sedateArea(
                        serverLevel,
                        sp.blockPosition(),
                        1,
                        DimensionalHomeostasisEngine.TRUCE_PHEROMONE_SEDATION,
                        DimensionalHomeostasisEngine.TRUCE_PHEROMONE_SENSITIZATION_REDUCTION,
                        sp
                );
            }

            if (!sp.getAbilities().instabuild) {
                stack.shrink(1);
            }
            sp.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
            serverLevel.sendParticles(ModParticles.HISTAMINE_PULSE.get(), sp.getX(), sp.getY(0.5), sp.getZ(),
                    20, 0.45, 0.5, 0.45, 0.04);
            serverLevel.playSound(null, sp.blockPosition(), ModSounds.TRUCE_PHEROMONE_SPRAY.get(), SoundSource.PLAYERS, 0.95F, 1.0F);
            sp.displayClientMessage(Component.translatable("message.mycelialmind.truce_activated")
                    .withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.truce_pheromone").withStyle(ChatFormatting.AQUA));
    }
}
