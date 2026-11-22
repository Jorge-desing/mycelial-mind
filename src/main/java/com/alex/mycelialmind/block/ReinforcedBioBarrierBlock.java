package com.alex.mycelialmind.block;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/**
 * Acid- and burrower-resistant defensive block for base fortification.
 */
public class ReinforcedBioBarrierBlock extends Block {
    public ReinforcedBioBarrierBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.reinforced_bio_barrier").withStyle(ChatFormatting.AQUA));
    }
}
