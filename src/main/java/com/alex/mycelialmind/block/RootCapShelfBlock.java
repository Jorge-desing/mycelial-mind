package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModSounds;
import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * Elastic fungal wall-shelf forming spiral ledges along the Twilight Throat of a Mycelial Sinkhole.
 * Dampens fall damage by 80% and provides a gentle upward rebound when not sneaking.
 */
public class RootCapShelfBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<RootCapShelfBlock> CODEC = simpleCodec(RootCapShelfBlock::new);

    private static final VoxelShape SHAPE_NORTH = Block.box(1.0D, 6.0D, 2.0D, 15.0D, 13.0D, 16.0D);
    private static final VoxelShape SHAPE_SOUTH = Block.box(1.0D, 6.0D, 0.0D, 15.0D, 13.0D, 14.0D);
    private static final VoxelShape SHAPE_WEST = Block.box(2.0D, 6.0D, 1.0D, 16.0D, 13.0D, 15.0D);
    private static final VoxelShape SHAPE_EAST = Block.box(0.0D, 6.0D, 1.0D, 14.0D, 13.0D, 15.0D);

    public RootCapShelfBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction facing = clickedFace.getAxis().isHorizontal()
                ? clickedFace
                : context.getHorizontalDirection().getOpposite();
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST -> SHAPE_WEST;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 0.2F, level.damageSources().fall());
        if (!level.isClientSide() && !entity.isSuppressingBounce() && fallDistance >= 1.0F) {
            float pitch = 0.90F + Math.min(0.28F, fallDistance * 0.03F);
            level.playSound(null, pos, ModSounds.ROOT_CAP_BOUNCE.get(), SoundSource.BLOCKS, 0.85F, pitch);
        }
    }

    @Override
    public void updateEntityAfterFallOn(BlockGetter level, Entity entity) {
        if (entity.isSuppressingBounce()) {
            super.updateEntityAfterFallOn(level, entity);
        } else {
            Vec3 delta = entity.getDeltaMovement();
            if (delta.y < 0.0D) {
                double rebound = Math.max(0.42D, -delta.y * 0.65D);
                entity.setDeltaMovement(delta.x, Math.min(0.68D, rebound), delta.z);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.root_cap_shelf")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
