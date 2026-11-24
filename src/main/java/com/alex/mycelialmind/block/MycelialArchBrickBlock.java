package com.alex.mycelialmind.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Refined mycelial vault masonry treated against biological assimilation:
 * immune to Creeping Mycelium spread, adaptive Hyphal Vein sprouting, and Antibody Cyst encapsulation.
 */
public class MycelialArchBrickBlock extends Block {
    public MycelialArchBrickBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.mycelial_arch_brick").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
