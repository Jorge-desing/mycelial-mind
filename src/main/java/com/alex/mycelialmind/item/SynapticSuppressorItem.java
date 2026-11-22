package com.alex.mycelialmind.item;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.HiveBlackboard;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.SynapticCommandSystem;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisEngine;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Endgame relic forged from the Hive Heart.
 * Discharges a 20-block synaptic shockwave that weakens nearby MycelialMobs
 * and temporarily suppresses all Tier 2 behavioral mutations and HiveSynergies.
 */
public class SynapticSuppressorItem extends Item {
    public SynapticSuppressorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel serverLevel) {
            long gameTime = serverLevel.getGameTime();
            long durationTicks = MycelialConfig.SUPPRESSOR_COOLDOWN_SECONDS.get() * 20L;
            HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
            hive.suppressHigherAdaptations(gameTime + durationTicks, gameTime);

            com.alex.mycelialmind.world.PurificationZoneTracker tracker =
                    com.alex.mycelialmind.world.PurificationZoneTracker.get(serverLevel);
            for (var entry : tracker.getNodesWithin(player.blockPosition(), 20, 16)) {
                if (entry.getValue() != com.alex.mycelialmind.world.SynapticNodeType.SYMBIOTIC
                        && serverLevel.getBlockEntity(entry.getKey()) instanceof com.alex.mycelialmind.block.entity.SynapticNodeBlockEntity nodeBe) {
                    nodeBe.purgeVolatileBuffer(serverLevel, entry.getKey(), true);
                }
            }

            for (MycelialMob mob : serverLevel.getEntitiesOfClass(MycelialMob.class, player.getBoundingBox().inflate(20.0D))) {
                if (mob.distanceToSqr(player) > 400.0D) {
                    continue;
                }
                if (mob.isAlliedEscort()) {
                    continue;
                }
                mob.triggerDesync(SynapticCommandSystem.DESYNC_DURATION_TICKS);
                mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
                mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
                if (!mob.isBossMob()) {
                    mob.applyGlobalHiveState(serverLevel, hive);
                }
            }
            for (PurifierDroneEntity drone : serverLevel.getEntitiesOfClass(PurifierDroneEntity.class, player.getBoundingBox().inflate(20.0D))) {
                if (drone.distanceToSqr(player) <= 400.0D) {
                    drone.clearJam();
                }
            }

            BlockPos center = player.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(center.offset(-12, -4, -12), center.offset(12, 4, 12))) {
                if (p.distSqr(center) <= 144.0D && serverLevel.getBlockState(p).is(ModBlocks.EPHEMERAL_CHITIN.get())) {
                    serverLevel.removeBlock(p, false);
                }
            }
            HiveBlackboard.purgeCarrionWithin(serverLevel, player.position(), 20.0D);

            player.removeEffect(MobEffects.POISON);
            player.removeEffect(MobEffects.WEAKNESS);
            player.removeEffect(MobEffects.BLINDNESS);
            player.removeEffect(ModEffects.MYCELIAL_INFECTION);
            player.removeEffect(ModEffects.SPORE_ROT);

            if (DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)) {
                DimensionalHomeostasisDirector.sedateArea(
                        serverLevel,
                        player.blockPosition(),
                        1,
                        DimensionalHomeostasisEngine.SUPPRESSOR_SEDATION,
                        DimensionalHomeostasisEngine.SUPPRESSOR_SENSITIZATION_REDUCTION,
                        player instanceof ServerPlayer sp ? sp : null
                );
            }

            serverLevel.sendParticles(ModParticles.PURIFIER_CROSS.get(),
                    player.getX(), player.getY() + 1.0, player.getZ(),
                    48, 4.0, 1.0, 4.0, 0.12);
            serverLevel.playSound(null, player.blockPosition(),
                    ModSounds.SUPPRESSOR_DISCHARGE.get(), SoundSource.PLAYERS, 1.2F, 1.0F);

            for (ServerPlayer sp : serverLevel.players()) {
                PacketDistributor.sendToPlayer(sp, HiveSyncPayload.forPlayer(serverLevel, sp));
            }

            player.sendSystemMessage(Component.translatable(
                    "message.mycelialmind.suppressor_activated",
                    MycelialConfig.SUPPRESSOR_COOLDOWN_SECONDS.get()
            ).withStyle(ChatFormatting.AQUA));

            player.getCooldowns().addCooldown(this, 200);
            stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return repair.is(ModItems.SYNAPTIC_CORE.get()) || repair.is(ModItems.HIVE_HEART.get());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.synaptic_suppressor").withStyle(ChatFormatting.AQUA));
    }
}
