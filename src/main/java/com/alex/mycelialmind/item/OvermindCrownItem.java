package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Mythic endgame relic forged from Overmind Essence.
 * Overcharges Purification Pylons (cleansing 12-block radius of mycelium) or purges 25% dimensional threat.
 */
public class OvermindCrownItem extends Item {
    public OvermindCrownItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        BlockPos pos = context.getClickedPos();
        if (context.getLevel() instanceof ServerLevel serverLevel
                && serverLevel.getBlockState(pos).is(ModBlocks.PURIFICATION_PYLON.get())) {
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResult.FAIL;
            }
            int cleansed = cleanseMyceliumRadius(serverLevel, pos, 12);
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
            player.removeEffect(ModEffects.MYCELIAL_INFECTION);
            player.removeEffect(ModEffects.SPORE_ROT);
            serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                    40, 4.0, 1.0, 4.0, 0.08);
            serverLevel.playSound(null, pos, ModSounds.OVERMIND_CROWN_SURGE.get(), SoundSource.PLAYERS, 1.2F, 1.15F);
            player.sendSystemMessage(Component.translatable("message.mycelialmind.pylon_overcharged", cleansed)
                    .withStyle(ChatFormatting.AQUA));
            player.getCooldowns().addCooldown(this, 400);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }
        if (level instanceof ServerLevel serverLevel) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            hive.reduceThreat(15.0);
            player.removeEffect(ModEffects.MYCELIAL_INFECTION);
            player.removeEffect(ModEffects.SPORE_ROT);
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));

            for (ServerPlayer sp : serverLevel.players()) {
                PacketDistributor.sendToPlayer(sp, HiveSyncPayload.forPlayer(serverLevel, sp));
            }
            serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(), player.getX(), player.getY() + 1.0, player.getZ(),
                    32, 2.0, 1.0, 2.0, 0.06);
            serverLevel.playSound(null, player.blockPosition(), ModSounds.OVERMIND_CROWN_SURGE.get(), SoundSource.PLAYERS, 1.1F, 1.0F);
            player.sendSystemMessage(Component.translatable("message.mycelialmind.overmind_purge",
                    String.format("%.1f", hive.getThreatLevel())).withStyle(ChatFormatting.LIGHT_PURPLE));
            player.getCooldowns().addCooldown(this, 1200);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static int cleanseMyceliumRadius(ServerLevel level, BlockPos center, int radius) {
        int cleansed = 0;
        int rSq = radius * radius;
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-radius, -4, -radius), center.offset(radius, 4, radius))) {
            if (p.distSqr(center) <= rSq) {
                var state = level.getBlockState(p);
                if (state.is(ModBlocks.CREEPING_MYCELIUM.get())) {
                    level.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState());
                    cleansed++;
                } else if (state.is(ModBlocks.SPORE_VENT.get()) || state.is(ModBlocks.SPORE_BULB.get())) {
                    level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
                    cleansed++;
                } else if (state.is(ModBlocks.EPHEMERAL_CHITIN.get()) || state.is(ModBlocks.ANTIBODY_CYST.get())) {
                    level.removeBlock(p, false);
                    cleansed++;
                } else if (state.is(ModBlocks.HYPHAL_VEIN.get())
                        && com.alex.mycelialmind.hive.HyphalSiegeDirector.cleanseHyphalVeinAt(level, p, 8.0D)) {
                    cleansed++;
                }
            }
        }
        HiveBlackboard.purgeCarrionWithin(level, Vec3.atCenterOf(center), radius);
        return cleansed;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.overmind_crown").withStyle(ChatFormatting.GOLD));
    }
}
