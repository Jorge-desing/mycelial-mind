package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.ModDimensions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Dimensional portal block connecting the Overworld and the Deep Mycelium dimension.
 */
public class MycelialRiftBlock extends Block {
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 14.0D, 14.0D);

    public MycelialRiftBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level instanceof ServerLevel serverLevel
                && entity instanceof ServerPlayer player
                && !player.isOnPortalCooldown()) {
            ModDimensions.teleportPlayer(player, serverLevel);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (int i = 0; i < 3; i++) {
            level.addParticle(ModParticles.ENGRAM.get(),
                    pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.4,
                    0.1,
                    (random.nextDouble() - 0.5) * 0.4);
        }
        if (random.nextInt(24) == 0) {
            level.playLocalSound(
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    ModSounds.MYCELIAL_RIFT_HUM.get(), SoundSource.BLOCKS,
                    0.55F, 0.95F + random.nextFloat() * 0.1F, false
            );
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.mycelial_rift").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
