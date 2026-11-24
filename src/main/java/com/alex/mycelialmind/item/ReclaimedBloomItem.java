package com.alex.mycelialmind.item;

import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Places the corresponding palette state of a reclaimed bloom and describes its garden aura. */
public final class ReclaimedBloomItem extends BlockItem {
    private final int palette;

    public ReclaimedBloomItem(ReclaimedBloomBlock block, int palette, Properties properties) {
        super(block, properties);
        this.palette = Math.max(0, Math.min(5, palette));
    }

    @Override
    protected BlockState getPlacementState(BlockPlaceContext context) {
        BlockState state = super.getPlacementState(context);
        return state == null ? null : state.setValue(ReclaimedBloomBlock.PALETTE, palette);
    }

    private String suffix() {
        return switch (palette) {
            case 1 -> "melee";
            case 2 -> "projectile";
            case 3 -> "fire";
            case 4 -> "explosion";
            case 5 -> "magic";
            default -> "base";
        };
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.mycelialmind.reclaimed_bloom." + suffix());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.mycelialmind.reclaimed_bloom." + suffix()).withStyle(ChatFormatting.GREEN));
        tooltip.add(Component.translatable("tooltip.mycelialmind.reclaimed_bloom.colony_aura").withStyle(ChatFormatting.AQUA));
        com.alex.mycelialmind.util.ClientTooltipHelper.appendShiftDetails(tooltip,
                "tooltip.mycelialmind.reclaimed_bloom.shift_upkeep");
    }
}
