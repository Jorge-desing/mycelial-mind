package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

/**
 * Strips waterlogging from placed structural blocks, clears interior aquifer fluid pockets,
 * and seals exposed fluid neighbors around solid structural walls/floors/ceilings within
 * the active worldgen region.
 */
public class MycelialFluidClearProcessor extends StructureProcessor {
    public static final MapCodec<MycelialFluidClearProcessor> CODEC =
            MapCodec.unit(MycelialFluidClearProcessor::new);

    private static final Direction[] SEAL_DIRECTIONS = {
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.UP
    };

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.FLUID_CLEAR_PROCESSOR.get();
    }

    @Override
    @Nullable
    public StructureTemplate.StructureBlockInfo processBlock(
            LevelReader level,
            BlockPos offset,
            BlockPos pos,
            StructureTemplate.StructureBlockInfo blockInfo,
            StructureTemplate.StructureBlockInfo relativeBlockInfo,
            StructurePlaceSettings settings
    ) {
        BlockState state = relativeBlockInfo.state();
        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                && state.getValue(BlockStateProperties.WATERLOGGED)) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, false);
        }

        BlockPos worldPos = relativeBlockInfo.pos();
        if (level instanceof WorldGenLevel wgl) {
            if (state.isSolid()) {
                BlockState sealState = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
                for (Direction dir : SEAL_DIRECTIONS) {
                    BlockPos neighbor = worldPos.relative(dir);
                    if (!wgl.isOutsideBuildHeight(neighbor)
                            && wgl.ensureCanWrite(neighbor)
                            && !wgl.getFluidState(neighbor).isEmpty()) {
                        wgl.setBlock(neighbor, sealState, 2);
                    }
                }
            } else if (state.isAir()
                    && !wgl.isOutsideBuildHeight(worldPos)
                    && wgl.ensureCanWrite(worldPos)
                    && !wgl.getFluidState(worldPos).isEmpty()) {
                state = Blocks.AIR.defaultBlockState();
            }
        }

        if (state == relativeBlockInfo.state()) {
            return relativeBlockInfo;
        }
        return new StructureTemplate.StructureBlockInfo(worldPos, state, relativeBlockInfo.nbt());
    }
}
