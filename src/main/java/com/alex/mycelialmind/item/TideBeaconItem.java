package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModDamageTypes;
import com.alex.mycelialmind.registry.ModEffects;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Relic dropped by the Spore Behemoth.
 * Aborts an active Fungal Tide, reduces threat by 10%, and emits an 18-block purifying pulse.
 */
public class TideBeaconItem extends Item {
    public TideBeaconItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            long gameTime = serverLevel.getGameTime();
            boolean abortedTide = hive.isFungalTideActive(gameTime);
            if (abortedTide) {
                hive.endFungalTide(gameTime);
            }
            hive.reduceThreat(10.0);

            for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(18.0D))) {
                if (entity.distanceToSqr(player) > 324.0D) {
                    continue;
                }
                if (entity instanceof MycelialMob mob) {
                    if (mob.isAlliedEscort()) {
                        continue;
                    }
                    mob.hurt(ModDamageTypes.source(serverLevel, ModDamageTypes.PURIFICATION_PURGE, player, player), 10.0F);
                    mob.triggerDesync(SynapticCommandSystem.DESYNC_DURATION_TICKS);
                    mob.removeEffect(net.minecraft.world.effect.MobEffects.ABSORPTION);
                    mob.removeEffect(net.minecraft.world.effect.MobEffects.REGENERATION);
                } else if (entity instanceof PurifierDroneEntity drone) {
                    drone.clearJam();
                } else {
                    entity.removeEffect(ModEffects.MYCELIAL_INFECTION);
                    entity.removeEffect(ModEffects.SPORE_ROT);
                }
            }
            HiveBlackboard.purgeCarrionWithin(serverLevel, player.position(), 18.0D);
            if (DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)) {
                DimensionalHomeostasisDirector.sedateArea(
                        serverLevel,
                        player.blockPosition(),
                        1,
                        DimensionalHomeostasisEngine.SUPPRESSOR_SEDATION * 0.5D,
                        0,
                        null
                );
            }

            serverLevel.sendParticles(ModParticles.TIDE_MOTE.get(), player.getX(), player.getY() + 1.0, player.getZ(),
                    48, 3.0, 1.5, 3.0, 0.08);
            serverLevel.playSound(null, player.blockPosition(), ModSounds.TIDE_BEACON_ACTIVATE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);

            for (ServerPlayer sp : serverLevel.players()) {
                PacketDistributor.sendToPlayer(sp, HiveSyncPayload.forPlayer(serverLevel, sp));
                if (abortedTide) {
                    sp.sendSystemMessage(Component.translatable("message.mycelialmind.fungal_tide_cleared")
                            .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                }
            }
            player.getCooldowns().addCooldown(this, 100);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.tide_beacon").withStyle(ChatFormatting.AQUA));
    }
}
