package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.PurificationZoneTracker;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Temporary biodegradable chitin bridge/barricade grown during combat.
 * Automatically decays after 12 seconds (240 ticks), drops nothing,
 * and cannot be placed inside Purification Pylon zones or over TileEntities.
 */
public class EphemeralChitinBlock extends Block {
    public static final int DECAY_TICKS = 240;

    public EphemeralChitinBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.scheduleTick(pos, this, DECAY_TICKS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos).is(this)) {
            level.removeBlock(pos, false);
            level.sendParticles(ModParticles.SPORE_CLOUD.get(),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.25, 0.25, 0.25, 0.02);
            level.playSound(null, pos, ModSounds.EPHEMERAL_CHITIN_CRUMBLE.get(), SoundSource.BLOCKS, 0.7F, 1.0F);
        }
    }

    /**
     * Safely places Ephemeral Chitin only in air/replaceable positions outside Pylon protection.
     */
    public static boolean tryPlaceTemporary(ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos)) {
            return false;
        }
        if (PurificationZoneTracker.get(level).isProtected(pos)) {
            return false;
        }
        if (level.getBlockEntity(pos) != null) {
            return false;
        }
        BlockState existing = level.getBlockState(pos);
        if (!existing.isAir() && !existing.canBeReplaced()) {
            return false;
        }
        if (!existing.getFluidState().isEmpty()) {
            return false;
        }
        level.setBlock(pos, ModBlocks.EPHEMERAL_CHITIN.get().defaultBlockState(), 3);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.ephemeral_chitin").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
