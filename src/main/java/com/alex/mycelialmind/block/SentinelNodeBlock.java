package com.alex.mycelialmind.block;

import com.alex.mycelialmind.world.SynapticNodeType;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Sentinel Synaptic Node: projects defensive auras to nearby MycelialMobs
 * and acts as an invulnerability anchor during Hive Core Phase 2.
 */
public class SentinelNodeBlock extends MycelialNodeBlock {
    public SentinelNodeBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected SynapticNodeType nodeType() {
        return SynapticNodeType.SENTINEL;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.block.sentinel_node").withStyle(ChatFormatting.AQUA));
    }
}
