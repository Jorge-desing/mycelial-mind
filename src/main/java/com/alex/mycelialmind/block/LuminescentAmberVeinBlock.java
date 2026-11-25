package com.alex.mycelialmind.block;

import com.alex.mycelialmind.hive.EcosystemPurposeEngine;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModParticles;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
 * Bio-energetic amber conduit crystallized from purified mycelium.
 * Accelerates nearby flora/Reclaimed Bloom maturation, boosts adjacent Purification Pylons
 * (+4m radius) and Synaptic Research Benches (+1 effective lab tier), and emits comparator telemetry.
 */
public class LuminescentAmberVeinBlock extends Block {
    public LuminescentAmberVeinBlock(Properties properties) {
        super(properties);
    }

    public static boolean hasAdjacentConduit(Level level, BlockPos pos) {
        Block amber = ModBlocks.LUMINESCENT_AMBER_VEIN.get();
        for (Direction dir : Direction.values()) {
            if (level.getBlockState(pos.relative(dir)).is(amber)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasAdjacentSymbioticAnchor(Level level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockState st = level.getBlockState(pos.relative(dir));
            if (st.is(ModBlocks.PURIFICATION_PYLON.get())
                    || st.is(ModBlocks.SYNAPTIC_RESEARCH_BENCH.get())
                    || st.is(ModBlocks.SOVEREIGN_NEXUS.get())
                    || (st.is(ModBlocks.RECLAIMED_BLOOM.get()) && st.getValue(ReclaimedBloomBlock.MATURE))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean accelerated = false;
        for (BlockPos target : BlockPos.betweenClosed(pos.offset(-2, -1, -2), pos.offset(2, 2, 2))) {
            BlockState st = level.getBlockState(target);
            if (st.is(ModBlocks.RECLAIMED_BLOOM.get()) && !st.getValue(ReclaimedBloomBlock.MATURE)) {
                level.setBlockAndUpdate(target, st.setValue(ReclaimedBloomBlock.MATURE, true));
                level.updateNeighbourForOutputSignal(target, st.getBlock());
                level.scheduleTick(target, ModBlocks.RECLAIMED_BLOOM.get(), EcosystemPurposeEngine.BLOOM_AURA_INTERVAL_TICKS);
                accelerated = true;
            } else if (st.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())
                    && !st.getValue(BioluminescentMycelialCapBlock.MATURE)) {
                level.setBlockAndUpdate(target, st.setValue(BioluminescentMycelialCapBlock.MATURE, true));
                level.updateNeighbourForOutputSignal(target, st.getBlock());
                accelerated = true;
            } else if (st.is(ModBlocks.MYCELIAL_TENDRILS.get())
                    && !st.getValue(MycelialTendrilsBlock.MATURE)) {
                level.setBlockAndUpdate(target, st.setValue(MycelialTendrilsBlock.MATURE, true));
                level.updateNeighbourForOutputSignal(target, st.getBlock());
                accelerated = true;
            }
        }
        if (accelerated) {
            level.sendParticles(ModParticles.AMBER_GLINT.get(),
                    pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                    8, 0.45D, 0.35D, 0.45D, 0.02D);
            level.playSound(null, pos, ModSounds.AMBER_VEIN_RESONATE.get(), SoundSource.BLOCKS, 0.55F, 1.05F);
        }
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide()) {
            level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return hasAdjacentSymbioticAnchor(level, pos) ? 15 : 7;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.luminescent_amber_vein").withStyle(ChatFormatting.GOLD));
    }
}
