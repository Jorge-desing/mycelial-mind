package com.alex.mycelialmind.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Simple progression material item that renders a localized descriptive tooltip.
 */
public class TooltipMaterialItem extends Item {
    private final String tooltipKey;
    private final ChatFormatting color;

    public TooltipMaterialItem(Properties properties, String tooltipKey, ChatFormatting color) {
        super(properties);
        this.tooltipKey = tooltipKey;
        this.color = color;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(color));
    }
}
