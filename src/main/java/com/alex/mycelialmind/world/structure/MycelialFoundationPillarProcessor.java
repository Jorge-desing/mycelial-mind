package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

/**
 * Grounds overhanging bottom-layer template floor blocks onto solid dry cavern bedrock/stone
 * via deterministic checkerboard support piers within the active worldgen write region.
 */
public class MycelialFoundationPillarProcessor extends StructureProcessor {
    public static final MapCodec<MycelialFoundationPillarProcessor> CODEC =
            MapCodec.unit(MycelialFoundationPillarProcessor::new);

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.FOUNDATION_PILLAR_PROCESSOR.get();
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
        if (blockInfo.pos().getY() != 0 || !(level instanceof WorldGenLevel wgl)) {
            return relativeBlockInfo;
        }
        BlockState state = relativeBlockInfo.state();
        if (!isStructuralFloorBlock(state)) {
            return relativeBlockInfo;
        }
        BlockPos worldPos = relativeBlockInfo.pos();
        if (((worldPos.getX() ^ worldPos.getZ()) & 1) != 0) {
            return relativeBlockInfo;
        }

        BlockState archBrick = ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState();
        BlockState mycelialStone = ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
        for (int dy = 1; dy <= 10; dy++) {
            BlockPos probe = worldPos.below(dy);
            if (wgl.isOutsideBuildHeight(probe) || !wgl.ensureCanWrite(probe)) {
                break;
            }
            BlockState existing = wgl.getBlockState(probe);
            if (!existing.isAir() && existing.isSolid() && existing.getFluidState().isEmpty()) {
                break;
            }
            wgl.setBlock(probe, (dy % 2 == 0) ? archBrick : mycelialStone, 2);
        }
        return relativeBlockInfo;
    }

    private static boolean isStructuralFloorBlock(BlockState state) {
        return state.is(Blocks.DEEPSLATE_BRICKS)
                || state.is(Blocks.DEEPSLATE_TILES)
                || state.is(ModBlocks.MYCELIAL_ARCH_BRICK.get())
                || state.is(Blocks.POLISHED_BASALT)
                || state.is(Blocks.NETHER_BRICKS)
                || state.is(Blocks.POLISHED_BLACKSTONE_BRICKS)
                || state.is(Blocks.END_STONE_BRICKS)
                || state.is(Blocks.PURPUR_BLOCK);
    }
}
