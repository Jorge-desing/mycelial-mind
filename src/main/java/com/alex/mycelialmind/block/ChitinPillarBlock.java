package com.alex.mycelialmind.block;

import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;

import java.util.List;

/**
 * Heavy chitin structural column that damps mycorrhizal seismic footsteps nearby
 * and acts as a hard defensive bulwark capable of stunning charging Chitin Brutes.
 */
public class ChitinPillarBlock extends RotatedPillarBlock {
    public ChitinPillarBlock(Properties properties) {
        super(properties);
    }

    public static boolean isPlayerStealthDamped(Level level, BlockPos feetPos) {
        Block pillar = ModBlocks.CHITIN_PILLAR.get();
        if (level.getBlockState(feetPos).is(pillar) || level.getBlockState(feetPos.below()).is(pillar)) {
            return true;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (level.getBlockState(feetPos.relative(dir)).is(pillar)
                    || level.getBlockState(feetPos.below().relative(dir)).is(pillar)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.chitin_pillar").withStyle(ChatFormatting.GOLD));
    }
}
