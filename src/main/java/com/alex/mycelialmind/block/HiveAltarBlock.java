package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.entity.HiveCoreBossEntity;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.network.DiscoverySyncPayload;
import com.alex.mycelialmind.network.HiveSyncPayload;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Sanctum altar block that anchors the Hive Heart Sanctum and summons the Hive Core Boss
 * when activated with a Synaptic Core.
 */
public class HiveAltarBlock extends Block implements EntityBlock {
    public HiveAltarBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MonumentFocusBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel serverLevel) {
            PurificationZoneTracker.get(serverLevel).addNode(pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            PurificationZoneTracker.get(serverLevel).removeNode(pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
        if (!stack.is(ModItems.SYNAPTIC_CORE.get())) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.altar_requires_core")
                        .withStyle(ChatFormatting.LIGHT_PURPLE), true);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level instanceof ServerLevel serverLevel) {
            boolean bossAlreadyNearby = !serverLevel.getEntitiesOfClass(
                    HiveCoreBossEntity.class, new AABB(pos).inflate(48.0D)).isEmpty();
            if (bossAlreadyNearby) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.boss_already_active")
                        .withStyle(ChatFormatting.RED), true);
                return ItemInteractionResult.FAIL;
            }

            HiveCoreBossEntity boss = ModEntities.HIVE_CORE.get().create(serverLevel);
            if (boss != null) {
                boss.moveTo(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 0.0F, 0.0F);
                boss.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null);
                if (!serverLevel.addFreshEntity(boss)) {
                    return ItemInteractionResult.FAIL;
                }

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                serverLevel.destroyBlock(pos, false);
                serverLevel.playSound(null, pos, ModSounds.HIVE_ALTAR_AWAKEN.get(), SoundSource.HOSTILE, 1.65F, 1.0F);
                serverLevel.sendParticles(ModParticles.HIVE_PULSE.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        1, 0.0, 0.0, 0.0, 0.0);
                serverLevel.sendParticles(ModParticles.SPORE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                        24, 1.2, 1.2, 1.2, 0.05);
                serverLevel.sendParticles(ModParticles.BIO_LUME.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        8, 1.0, 1.0, 1.0, 0.03);
                HiveMindSavedData.get(serverLevel).promoteHighestTier(HiveProgressionTier.AWAKENED);
                for (ServerPlayer sp : serverLevel.players()) {
                    PacketDistributor.sendToPlayer(sp, HiveSyncPayload.forPlayer(serverLevel, sp));
                    DiscoverySyncPayload.syncToPlayer(sp);
                }
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.hive_altar").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
