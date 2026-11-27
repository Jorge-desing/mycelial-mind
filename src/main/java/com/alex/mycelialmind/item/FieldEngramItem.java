package com.alex.mycelialmind.item;

import com.alex.mycelialmind.hive.EcosystemPurposeEngine;
import com.alex.mycelialmind.hive.FieldEngramId;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.item.tooltip.MycelialTooltipData;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.registry.ModAttachments;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Collectible diegetic Field Engram carrying a unique FieldEngramId.
 * Right-clicking absorbs its chronicle directly into the player's Codex without cluttering inventory.
 */
public class FieldEngramItem extends Item {
    public FieldEngramItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createStack(FieldEngramId id) {
        ItemStack stack = new ItemStack(ModItems.FIELD_ENGRAM.get());
        if (id != null) {
            stack.set(ModDataComponents.ENGRAM_ID.get(), id.id());
        }
        return stack;
    }

    public static FieldEngramId getEngramId(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return FieldEngramId.OUTBREAK_SIGNAL;
        }
        return FieldEngramId.byId(stack.get(ModDataComponents.ENGRAM_ID.get()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        FieldEngramId engram = getEngramId(stack);
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            PlayerDiscoveryData discovery = serverPlayer.getData(ModAttachments.DISCOVERY);
            boolean newlyUnlocked = discovery.markEngramDiscovered(engram);
            if (newlyUnlocked) {
                DiscoverySyncPayload.syncToPlayer(serverPlayer);
                serverLevel.playSound(null, player.blockPosition(), ModSounds.BIO_SCANNER_PING.get(),
                        SoundSource.PLAYERS, 0.9F, 1.35F);
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                        player.getX(), player.getY(0.6D), player.getZ(),
                        16, 0.35D, 0.4D, 0.35D, 0.04D);
                player.displayClientMessage(Component.translatable(
                        "message.mycelialmind.engram_absorbed",
                        Component.translatable(engram.titleKey())
                ).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
            } else {
                player.giveExperiencePoints(EcosystemPurposeEngine.DUPLICATE_ENGRAM_XP);
                player.addEffect(new MobEffectInstance(
                        ModEffects.SYNAPTIC_INSIGHT, EcosystemPurposeEngine.DUPLICATE_ENGRAM_INSIGHT_TICKS, 0));
                HiveMindSavedData.get(serverLevel).reduceThreat(EcosystemPurposeEngine.DUPLICATE_ENGRAM_THREAT_REDUCTION);
                serverLevel.playSound(null, player.blockPosition(), ModSounds.SYNAPTIC_PULSE.get(),
                        SoundSource.PLAYERS, 0.8F, 1.3F);
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                        player.getX(), player.getY(0.6D), player.getZ(),
                        12, 0.3D, 0.35D, 0.3D, 0.03D);
                player.displayClientMessage(Component.translatable(
                        "message.mycelialmind.engram_absorbed_duplicate",
                        Component.translatable(engram.titleKey()),
                        EcosystemPurposeEngine.DUPLICATE_ENGRAM_XP
                ).withStyle(ChatFormatting.GREEN), true);
            }
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public Component getName(ItemStack stack) {
        FieldEngramId engram = getEngramId(stack);
        return Component.translatable("item.mycelialmind.field_engram.named",
                Component.translatable(engram.titleKey()));
    }

    @Override
    public java.util.Optional<net.minecraft.world.inventory.tooltip.TooltipComponent> getTooltipImage(ItemStack stack) {
        FieldEngramId engram = getEngramId(stack);
        return java.util.Optional.of(new MycelialTooltipData.TraitBadgeTooltip(
                MycelialTooltipData.colorForEngram(engram),
                Component.translatable(engram.titleKey()),
                Component.translatable("tooltip.mycelialmind.field_engram.act", engram.actNumber())
        ));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FieldEngramId engram = getEngramId(stack);
        tooltip.add(Component.translatable("tooltip.mycelialmind.field_engram.act", engram.actNumber())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable(engram.hintKey()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.mycelialmind.field_engram.use").withStyle(ChatFormatting.AQUA));
    }
}
