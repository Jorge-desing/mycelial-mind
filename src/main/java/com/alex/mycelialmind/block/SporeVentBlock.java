package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.entity.MycelialChimeraEntity;
import com.alex.mycelialmind.entity.MycelialMob;
import com.alex.mycelialmind.entity.PurifierDroneEntity;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.item.ChitinArmorItem;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModEffects;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.util.MycelialParticles;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * Emits toxic mycelial spores to nearby non-mycelial entities unless suppressed by a Purification Pylon.
 */
public class SporeVentBlock extends Block {
    public SporeVentBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (stack.is(ModItems.SPORE_ANTIDOTE.get()) || stack.is(ModItems.CONCENTRATED_ANTIDOTE.get())) {
            if (level instanceof ServerLevel serverLevel) {
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                serverLevel.setBlockAndUpdate(pos, ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get().defaultBlockState());
                ItemStack reward = new ItemStack(ModItems.LATENT_SPORE.get(), 2);
                if (!player.getInventory().add(reward)) {
                    Block.popResource(serverLevel, pos, reward);
                }
                MycelialParticles.antidote(serverLevel, player, stack.is(ModItems.CONCENTRATED_ANTIDOTE.get()));
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                        pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5,
                        16, 0.35, 0.35, 0.35, 0.04);
                serverLevel.playSound(null, pos, ModSounds.SPORE_ANTIDOTE_CURE.get(), SoundSource.BLOCKS, 0.9F, 1.15F);
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerDiscoveryData.unlockSecret(serverPlayer, ProgressionGoalEngine.SECRET_WORLD_VENT_PURIFICATION);
                }
                player.displayClientMessage(Component.translatable("message.mycelialmind.vent_purified")
                        .withStyle(ChatFormatting.AQUA), true);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, 40);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!PurificationZoneTracker.get(level).isProtected(pos)) {
            AABB area = new AABB(pos).inflate(4.5D);
            List<LivingEntity> targets = level.getEntitiesOfClass(LivingEntity.class, area,
                    e -> !(e instanceof MycelialMob)
                            && !(e instanceof MycelialChimeraEntity)
                            && !(e instanceof PurifierDroneEntity)
                            && e.isAlive());
            for (LivingEntity target : targets) {
                if (target instanceof Player player) {
                    if (ChitinArmorItem.hasFullSet(player)) {
                        continue;
                    }
                    int pieces = ChitinArmorItem.countEquippedPieces(player);
                    int infectionDuration = Math.max(20, (int) Math.round(100 * (1.0 - 0.20 * pieces)));
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    target.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, infectionDuration, 0));
                } else {
                    target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                    target.addEffect(new MobEffectInstance(ModEffects.MYCELIAL_INFECTION, 100, 0));
                }
            }
            level.sendParticles(ModParticles.SPORE_CLOUD.get(),
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5,
                    6, 0.25, 0.3, 0.25, 0.02);
            if (!targets.isEmpty() || random.nextInt(3) == 0) {
                level.playSound(null, pos, ModSounds.SPORE_VENT_PUFF.get(), SoundSource.BLOCKS, 0.55F, 0.9F + random.nextFloat() * 0.2F);
            }
        }
        level.scheduleTick(pos, this, 40);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.spore_vent").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
