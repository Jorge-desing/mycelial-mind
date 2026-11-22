package com.alex.mycelialmind.block;

import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Incubator Synaptic Node: accumulates network biomass and dispatches tactical squads
 * toward nearby players or nodes under attack.
 */
public class IncubatorNodeBlock extends MycelialNodeBlock {
    public IncubatorNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected SynapticNodeType nodeType() {
        return SynapticNodeType.INCUBATOR;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.incubator_node").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
