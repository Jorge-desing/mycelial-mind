package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.entity.ApexMatriarchEntity;
import com.alex.mycelialmind.hive.EvolutionRank;
import com.alex.mycelialmind.hive.HiveMindSavedData;
import com.alex.mycelialmind.hive.HiveProgressionTier;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.DimensionalHomeostasisDirector;
import com.alex.mycelialmind.world.DimensionalHomeostasisSavedData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Legendary ritual altar at the heart of the Matriarch Citadel in Deep Mycelium.
 * Summons the Apex Matriarch when activated with Overmind Essence or a Synaptic Core.
 */
public class MatriarchAltarBlock extends Block implements EntityBlock {
    public MatriarchAltarBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MonumentFocusBlockEntity(pos, state);
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
        boolean validOffering = stack.is(ModItems.OVERMIND_ESSENCE.get())
                || stack.is(ModItems.HIVE_HEART.get())
                || stack.is(ModItems.TIDE_BEACON.get());
        if (!validOffering) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.matriarch_altar_requires_offering")
                        .withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (level instanceof ServerLevel serverLevel) {
            boolean alreadyActive = !serverLevel.getEntitiesOfClass(
                    ApexMatriarchEntity.class, new AABB(pos).inflate(64.0D)).isEmpty();
            if (alreadyActive) {
                player.displayClientMessage(Component.translatable("message.mycelialmind.matriarch_already_active")
                        .withStyle(ChatFormatting.RED), true);
                return ItemInteractionResult.FAIL;
            }

            ApexMatriarchEntity matriarch = ModEntities.APEX_MATRIARCH.get().create(serverLevel);
            if (matriarch != null) {
                matriarch.moveTo(pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, 0.0F, 0.0F);
                matriarch.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(pos), MobSpawnType.TRIGGERED, null);
                matriarch.setPersistenceRequired();
                if (stack.is(ModItems.OVERMIND_ESSENCE.get())) {
                    matriarch.promoteRank(EvolutionRank.ELITE);
                }
                if (!serverLevel.addFreshEntity(matriarch)) {
                    return ItemInteractionResult.FAIL;
                }

                HiveMindSavedData.get(serverLevel).promoteHighestTier(HiveProgressionTier.OVERMIND);
                HiveMindSavedData.get(serverLevel.getServer().overworld()).promoteHighestTier(HiveProgressionTier.OVERMIND);
                if (DimensionalHomeostasisDirector.isHomeostasisDimension(serverLevel)) {
                    DimensionalHomeostasisSavedData.get(serverLevel)
                            .addStress(new ChunkPos(pos), 35.0, serverLevel.getGameTime());
                }

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
                serverLevel.destroyBlock(pos, false);
                serverLevel.playSound(null, pos, ModSounds.MATRIARCH_ALTAR_AWAKEN.get(), SoundSource.HOSTILE, 1.85F, 1.0F);
                serverLevel.sendParticles(ModParticles.PSYCHIC_WAVE.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5,
                        1, 0.0, 0.0, 0.0, 0.0);
                serverLevel.sendParticles(ModParticles.HIVE_PULSE.get(), pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5,
                        1, 0.0, 0.0, 0.0, 0.0);
                serverLevel.sendParticles(ModParticles.TIDE_MOTE.get(), pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5,
                        24, 1.2, 1.4, 1.2, 0.06);
                serverLevel.sendParticles(ModParticles.DESYNC_FRAGMENT.get(), pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5,
                        8, 1.0, 1.2, 1.0, 0.04);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.matriarch_altar").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
