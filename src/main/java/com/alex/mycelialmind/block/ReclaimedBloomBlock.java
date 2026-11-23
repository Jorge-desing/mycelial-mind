package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A harvestable ornamental bloom whose color records the adaptation of a recovered enclave. */
public final class ReclaimedBloomBlock extends Block {
    public static final IntegerProperty PALETTE = IntegerProperty.create("palette", 0, 5);
    public static final BooleanProperty MATURE = BooleanProperty.create("mature");
    private static final VoxelShape SHAPE = Block.box(3.0D, 0.0D, 3.0D, 13.0D, 12.0D, 13.0D);

    public ReclaimedBloomBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PALETTE, 0).setValue(MATURE, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PALETTE, MATURE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isSolid();
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return false;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(MATURE)) {
            level.setBlockAndUpdate(pos, state.setValue(MATURE, true));
        }
    }

    public void harvest(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        if (!state.getValue(MATURE)) {
            return;
        }
        if (!player.isCreative()) {
            popResource(level, pos, itemForPalette(state.getValue(PALETTE)));
        }
        level.setBlockAndUpdate(pos, state.setValue(MATURE, false));
        level.scheduleTick(pos, this, 1200);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (level instanceof ServerLevel serverLevel && !player.isCreative()) {
            popResource(serverLevel, pos, itemForPalette(state.getValue(PALETTE)));
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    public static int colorForPalette(int palette) {
        return switch (palette) {
            case 1 -> 0xFFE5646F;
            case 2 -> 0xFF54D6E8;
            case 3 -> 0xFFFFA64D;
            case 4 -> 0xFFFFD35A;
            case 5 -> 0xFFB691FF;
            default -> 0xFF8EDB78;
        };
    }

    private static ItemStack itemForPalette(int palette) {
        return switch (palette) {
            case 1 -> new ItemStack(ModItems.RECLAIMED_BLOOM_MELEE.get());
            case 2 -> new ItemStack(ModItems.RECLAIMED_BLOOM_PROJECTILE.get());
            case 3 -> new ItemStack(ModItems.RECLAIMED_BLOOM_FIRE.get());
            case 4 -> new ItemStack(ModItems.RECLAIMED_BLOOM_EXPLOSION.get());
            case 5 -> new ItemStack(ModItems.RECLAIMED_BLOOM_MAGIC.get());
            default -> new ItemStack(ModBlocks.RECLAIMED_BLOOM.get().asItem());
        };
    }
}
