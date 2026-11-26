package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModStructures;
import com.alex.mycelialmind.world.TerrainPlacementPolicy;
import com.alex.mycelialmind.world.TerrainPlacementPolicy.CaveBiomeDegradeStyle;
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
 * Weathering processor that adapts subterranean mycelial structure shells to their surrounding
 * cave biome ({@code lush_caves}, {@code dripstone_caves}, {@code deep_dark}, or {@code deep_mycelium})
 * without altering functional blocks (altars, chests, stairs, nodes, or jigsaws).
 */
public class MycelialBiomeDegradeProcessor extends StructureProcessor {
    public static final MapCodec<MycelialBiomeDegradeProcessor> CODEC =
            MapCodec.unit(MycelialBiomeDegradeProcessor::new);

    @Override
    protected StructureProcessorType<?> getType() {
        return ModStructures.BIOME_DEGRADE_PROCESSOR.get();
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
        if (!(level instanceof WorldGenLevel wgl)) {
            return relativeBlockInfo;
        }
        BlockState existingState = relativeBlockInfo.state();
        if (!isDegradableShellBlock(existingState)) {
            return relativeBlockInfo;
        }
        BlockPos worldPos = relativeBlockInfo.pos();
        if (wgl.isOutsideBuildHeight(worldPos) || !wgl.ensureCanWrite(worldPos)) {
            return relativeBlockInfo;
        }
        CaveBiomeDegradeStyle style = TerrainPlacementPolicy.classifyCaveBiomeStyle(wgl.getBiome(worldPos));
        if (style == CaveBiomeDegradeStyle.TEMPERATE) {
            return relativeBlockInfo;
        }

        int hash = Math.floorMod(worldPos.getX() * 31 + worldPos.getZ() * 17 + worldPos.getY() * 13, 8);
        BlockState adapted = selectAdaptedBlock(style, existingState, blockInfo.pos().getY(), hash);
        if (adapted == existingState) {
            return relativeBlockInfo;
        }
        return new StructureTemplate.StructureBlockInfo(worldPos, adapted, relativeBlockInfo.nbt());
    }

    public static boolean isDegradableShellBlock(BlockState state) {
        return state.is(Blocks.DEEPSLATE_BRICKS)
                || state.is(Blocks.CRACKED_DEEPSLATE_BRICKS)
                || state.is(Blocks.DEEPSLATE_TILES)
                || state.is(ModBlocks.MYCELIAL_STONE.get());
    }

    public static String selectAdaptedBlockId(
            CaveBiomeDegradeStyle style,
            String fallbackId,
            int templateLocalY,
            int hashMod8
    ) {
        if (style == null || hashMod8 >= 2 || hashMod8 < 0) {
            return fallbackId;
        }
        return switch (style) {
            case LUSH -> hashMod8 == 0
                    ? (templateLocalY == 0 ? "minecraft:moss_block" : "minecraft:mossy_cobblestone")
                    : "minecraft:mossy_cobblestone";
            case DRIPSTONE -> hashMod8 == 0
                    ? "minecraft:dripstone_block"
                    : "minecraft:calcite";
            case DEEP_DARK -> hashMod8 == 0
                    ? (templateLocalY == 0 ? "minecraft:sculk" : "minecraft:polished_deepslate")
                    : "minecraft:cobbled_deepslate";
            case DEEP_MYCELIUM -> hashMod8 == 0 && templateLocalY == 0
                    ? "mycelialmind:creeping_mycelium"
                    : "mycelialmind:mycelial_stone";
            case TEMPERATE -> fallbackId;
        };
    }

    public static BlockState selectAdaptedBlock(
            CaveBiomeDegradeStyle style,
            BlockState fallback,
            int templateLocalY,
            int hashMod8
    ) {
        if (hashMod8 >= 2) {
            return fallback;
        }
        return switch (style) {
            case LUSH -> hashMod8 == 0
                    ? (templateLocalY == 0 ? Blocks.MOSS_BLOCK.defaultBlockState() : Blocks.MOSSY_COBBLESTONE.defaultBlockState())
                    : Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case DRIPSTONE -> hashMod8 == 0
                    ? Blocks.DRIPSTONE_BLOCK.defaultBlockState()
                    : Blocks.CALCITE.defaultBlockState();
            case DEEP_DARK -> hashMod8 == 0
                    ? (templateLocalY == 0 ? Blocks.SCULK.defaultBlockState() : Blocks.POLISHED_DEEPSLATE.defaultBlockState())
                    : Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            case DEEP_MYCELIUM -> hashMod8 == 0 && templateLocalY == 0
                    ? ModBlocks.CREEPING_MYCELIUM.get().defaultBlockState()
                    : ModBlocks.MYCELIAL_STONE.get().defaultBlockState();
            case TEMPERATE -> fallback;
        };
    }
}
