package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.config.MycelialConfig;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.hive.PlayerDiscoveryData;
import com.alex.mycelialmind.hive.ProgressionGoalEngine;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Invasive mycelial ground block that spreads to adjacent natural soil/stone
 * unless blocked by a Purification Pylon.
 */
public class CreepingMyceliumBlock extends Block {
    public CreepingMyceliumBlock(Properties properties) {
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
        if (stack.is(Items.AMETHYST_SHARD)) {
            if (level instanceof ServerLevel serverLevel) {
                if (!PurificationZoneTracker.get(serverLevel).isProtected(pos)) {
                    player.displayClientMessage(Component.translatable("message.mycelialmind.amber_requires_pylon")
                            .withStyle(ChatFormatting.YELLOW), true);
                    return ItemInteractionResult.FAIL;
                }
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                serverLevel.setBlockAndUpdate(pos, ModBlocks.LUMINESCENT_AMBER_VEIN.get().defaultBlockState());
                HiveMindSavedData hive = HiveMindSavedData.get(serverLevel);
                HiveProgressionTier effectiveAct = HiveProgressionTier.resolveEffectiveTier(
                        hive.getThreatLevel(), hive.getHighestTierReached());
                if (effectiveAct.level() >= HiveProgressionTier.ADAPTIVE.level()) {
                    ItemStack bonusSpore = new ItemStack(ModItems.LATENT_SPORE.get());
                    if (!player.getInventory().add(bonusSpore)) {
                        Block.popResource(serverLevel, pos, bonusSpore);
                    }
                    ExperienceOrb.award(serverLevel, Vec3.atCenterOf(pos), 2);
                }
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(),
                        pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5,
                        16, 0.35, 0.35, 0.35, 0.04);
                serverLevel.playSound(null, pos, ModSounds.SYNAPTIC_PULSE.get(), SoundSource.BLOCKS, 0.85F, 1.35F);
                if (player instanceof ServerPlayer serverPlayer) {
                    PlayerDiscoveryData.unlockSecret(serverPlayer, ProgressionGoalEngine.SECRET_WORLD_AMBER_CRYSTALLIZATION);
                }
                player.displayClientMessage(Component.translatable("message.mycelialmind.amber_crystallized")
                        .withStyle(ChatFormatting.GOLD), true);
                return ItemInteractionResult.sidedSuccess(false);
            }
            return ItemInteractionResult.sidedSuccess(true);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!MycelialConfig.MYCELIUM_SPREAD_ENABLED.get()) {
            return;
        }
        if (!MyceliumSpreadPolicy.shouldAttempt(
                random.nextDouble(), MycelialConfig.MYCELIUM_SPREAD_CHANCE.get())) {
            return;
        }
        PurificationZoneTracker tracker = PurificationZoneTracker.get(level);
        if (tracker.isProtected(pos)) {
            return;
        }

        for (int attempt = 0; attempt < 3; attempt++) {
            BlockPos target = pos.offset(
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1,
                    random.nextInt(3) - 1
            );
            if (target.equals(pos) || tracker.isProtected(target)) {
                continue;
            }
            BlockState targetState = level.getBlockState(target);
            if (canInfect(level, target, targetState)) {
                level.setBlockAndUpdate(target, ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState());
                return;
            }
        }
    }

    public static boolean canInfect(ServerLevel level, BlockPos pos, BlockState state) {
        if (level.getBlockEntity(pos) != null) {
            return false;
        }
        if (isAssimilableVillageBlock(state)) {
            if (!MycelialConfig.AGGRESSIVE_VILLAGE_ASSIMILATION.get()) {
                return false;
            }
            HiveMindSavedData hive = HiveMindSavedData.get(level);
            return hive.getThreatLevel() >= 50.0 || hive.isFungalTideActive(level.getGameTime());
        }
        return canInfect(state);
    }

    public static boolean canInfect(BlockState state) {
        if (state.is(ModBlocks.MYCELIAL_ARCH_BRICK.get())
                || state.is(ModBlocks.REINFORCED_BIO_BARRIER.get())) {
            return false;
        }
        return state.is(BlockTags.DIRT)
                || state.is(Blocks.GRASS_BLOCK)
                || state.is(Blocks.MOSS_BLOCK)
                || state.is(Blocks.MYCELIUM)
                || (MycelialConfig.AGGRESSIVE_VILLAGE_ASSIMILATION.get() && isAssimilableVillageBlock(state));
    }

    public static boolean isAssimilableVillageBlock(BlockState state) {
        return state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.DIRT_PATH)
                || state.is(BlockTags.PLANKS)
                || state.is(BlockTags.LOGS)
                || state.is(BlockTags.WOODEN_SLABS)
                || state.is(BlockTags.WOODEN_STAIRS);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.creeping_mycelium").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
