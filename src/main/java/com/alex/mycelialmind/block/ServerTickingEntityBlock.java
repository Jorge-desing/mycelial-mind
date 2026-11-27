package com.alex.mycelialmind.block;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;

import javax.annotation.Nullable;

/**
 * Shared EntityBlock contract providing a type-safe helper for server-only BlockEntity tickers.
 */
public interface ServerTickingEntityBlock extends EntityBlock {
    @Nullable
    @SuppressWarnings("unchecked")
    static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createServerTicker(
            Level level,
            BlockEntityType<A> actualType,
            BlockEntityType<E> expectedType,
            BlockEntityTicker<? super E> ticker
    ) {
        return !level.isClientSide() && actualType == expectedType
                ? (BlockEntityTicker<A>) ticker
                : null;
    }
}
