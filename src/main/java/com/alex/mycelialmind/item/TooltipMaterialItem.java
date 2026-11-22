package com.alex.mycelialmind.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Simple progression material item that renders a localized descriptive tooltip.
 */
public class TooltipMaterialItem extends Item {
    private final String tooltipKey;
    private final ChatFormatting color;
    @Nullable
    private final String secondaryTooltipKey;
    @Nullable
    private final ChatFormatting secondaryColor;

    public TooltipMaterialItem(Properties properties, String tooltipKey, ChatFormatting color) {
        this(properties, tooltipKey, color, null, null);
    }

    public TooltipMaterialItem(
            Properties properties,
            String tooltipKey,
            ChatFormatting color,
            @Nullable String secondaryTooltipKey,
            @Nullable ChatFormatting secondaryColor
    ) {
        super(properties);
        this.tooltipKey = tooltipKey;
        this.color = color;
        this.secondaryTooltipKey = secondaryTooltipKey;
        this.secondaryColor = secondaryColor;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(tooltipKey).withStyle(color));
        if (secondaryTooltipKey != null && !secondaryTooltipKey.isEmpty()) {
            tooltip.add(Component.translatable(secondaryTooltipKey)
                    .withStyle(secondaryColor != null ? secondaryColor : ChatFormatting.AQUA));
        }
    }
}
