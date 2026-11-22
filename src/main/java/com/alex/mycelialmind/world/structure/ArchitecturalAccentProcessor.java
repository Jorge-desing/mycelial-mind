package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.hive.HyphalAdaptiveArchitectureEngine.ArchitecturalAccentSnapshot;
import com.alex.mycelialmind.registry.ModStructures;
import com.alex.mycelialmind.world.structure.ArchitecturalStructureAdapter.ResolvedPalette;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import javax.annotation.Nullable;

/**
 * Worldgen and template StructureProcessor that substitutes template palette blocks
 * with the player's measured Architectural Accent while preserving block orientations and states.
 */
public class ArchitecturalAccentProcessor extends StructureProcessor {
    public static final MapCodec<ArchitecturalAccentProcessor> CODEC =
            MapCodec.unit(ArchitecturalAccentProcessor::new);

    @Nullable
    private final ArchitecturalAccentSnapshot explicitAccent;

    public ArchitecturalAccentProcessor() {
        this.explicitAccent = null;
    }

    public ArchitecturalAccentProcessor(@Nullable ArchitecturalAccentSnapshot explicitAccent) {
        this.explicitAccent = explicitAccent;
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.ARCHITECTURAL_ACCENT_PROCESSOR.get();
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
        ArchitecturalAccentSnapshot accent = explicitAccent != null
                ? explicitAccent
                : (level instanceof WorldGenLevel wgl
                        ? ArchitecturalStructureAdapter.resolveAccentForLevel(wgl)
                        : ArchitecturalAccentSnapshot.EMPTY);
        if (!accent.hasCustomAccent()) {
            return relativeBlockInfo;
        }
        ResolvedPalette palette = ArchitecturalStructureAdapter.resolvePalette(accent);
        BlockState src = relativeBlockInfo.state();
        Block block = src.getBlock();
        BlockState replacement = null;

        if (block == Blocks.DEEPSLATE_BRICKS || block == Blocks.DEEPSLATE_TILES) {
            replacement = copySharedProperties(src, palette.primaryWall());
        } else if (block == Blocks.POLISHED_BASALT) {
            replacement = copySharedProperties(src, palette.structuralPillar());
        } else if (block == Blocks.DEEPSLATE_BRICK_STAIRS) {
            replacement = copySharedProperties(src, palette.stairBase());
        } else if (block == Blocks.DEEPSLATE_BRICK_SLAB) {
            replacement = copySharedProperties(src, palette.slab());
        } else if (block == Blocks.DEEPSLATE_BRICK_WALL) {
            replacement = copySharedProperties(src, palette.wallOrFence());
        } else if (block == Blocks.SHROOMLIGHT) {
            replacement = copySharedProperties(src, palette.glowBlock());
        }

        if (replacement == null) {
            return relativeBlockInfo;
        }
        return new StructureTemplate.StructureBlockInfo(
                relativeBlockInfo.pos(),
                replacement,
                relativeBlockInfo.nbt()
        );
    }

    private static BlockState copySharedProperties(BlockState source, BlockState target) {
        BlockState out = target;
        for (Property<?> prop : source.getProperties()) {
            if (out.hasProperty(prop)) {
                out = copySingleProperty(out, source, prop);
            }
        }
        return out;
    }

    private static <T extends Comparable<T>> BlockState copySingleProperty(
            BlockState target,
            BlockState source,
            Property<T> prop
    ) {
        T val = source.getValue(prop);
        return prop.getPossibleValues().contains(val) ? target.setValue(prop, val) : target;
    }
}
