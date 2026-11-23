package com.alex.mycelialmind.item;

import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Places the corresponding palette state of a reclaimed bloom. */
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

    @Override
    public Component getName(ItemStack stack) {
        String suffix = switch (palette) {
            case 1 -> "melee";
            case 2 -> "projectile";
            case 3 -> "fire";
            case 4 -> "explosion";
            case 5 -> "magic";
            default -> "base";
        };
        return Component.translatable("item.mycelialmind.reclaimed_bloom." + suffix);
    }
}
