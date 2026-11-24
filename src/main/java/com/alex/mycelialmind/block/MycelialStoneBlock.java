package com.alex.mycelialmind.block;

import com.alex.mycelialmind.hive.EcosystemPurposeEngine;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModParticles;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Living subterranean mineral substrate enriched with latent mycelium.
 * Nourishes Reclaimed Blooms and native mycelial flora planted above it into rapid maturation,
 * and can be cut into assimilation-proof Mycelial Arch Bricks.
 */
public class MycelialStoneBlock extends Block {
    public MycelialStoneBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos abovePos = pos.above();
        BlockState above = level.getBlockState(abovePos);
        boolean matured = false;
        if (above.is(ModBlocks.RECLAIMED_BLOOM.get()) && !above.getValue(ReclaimedBloomBlock.MATURE)) {
            level.setBlockAndUpdate(abovePos, above.setValue(ReclaimedBloomBlock.MATURE, true));
            level.scheduleTick(abovePos, ModBlocks.RECLAIMED_BLOOM.get(), EcosystemPurposeEngine.BLOOM_AURA_INTERVAL_TICKS);
            matured = true;
        } else if (above.is(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())
                && !above.getValue(BioluminescentMycelialCapBlock.MATURE)) {
            level.setBlockAndUpdate(abovePos, above.setValue(BioluminescentMycelialCapBlock.MATURE, true));
            matured = true;
        } else if (above.is(ModBlocks.MYCELIAL_TENDRILS.get())
                && !above.getValue(MycelialTendrilsBlock.MATURE)) {
            level.setBlockAndUpdate(abovePos, above.setValue(MycelialTendrilsBlock.MATURE, true));
            matured = true;
        }
        if (matured) {
            level.updateNeighbourForOutputSignal(abovePos, above.getBlock());
            level.sendParticles(ModParticles.BIO_LUME.get(),
                    abovePos.getX() + 0.5D, abovePos.getY() + 0.45D, abovePos.getZ() + 0.5D,
                    6, 0.25D, 0.2D, 0.25D, 0.02D);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.mycelial_stone").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
