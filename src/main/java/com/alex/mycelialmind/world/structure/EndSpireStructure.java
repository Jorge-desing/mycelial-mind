package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.registry.ModStructures;
import com.alex.mycelialmind.world.TerrainPlacementPolicy;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Multi-piece Jigsaw Outer End fungal spire anchored on dry solid end-stone islands
 * ({@code r >= 1000m}, {@code Y >= 50}, slope {@code <= 4} across {@code >= 7/9} columns).
 */
public class EndSpireStructure extends Structure {
    public static final MapCodec<EndSpireStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    Codec.intRange(1, 20).fieldOf("size").forGetter(s -> s.maxDepth),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                    DimensionPadding.CODEC.optionalFieldOf("dimension_padding", JigsawStructure.DEFAULT_DIMENSION_PADDING)
                            .forGetter(s -> s.dimensionPadding),
                    LiquidSettings.CODEC.optionalFieldOf("liquid_settings", LiquidSettings.IGNORE_WATERLOGGING)
                            .forGetter(s -> s.liquidSettings)
            ).apply(instance, EndSpireStructure::new)
    );

    private final Holder<StructureTemplatePool> startPool;
    private final int maxDepth;
    private final int maxDistanceFromCenter;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;

    public EndSpireStructure(
            StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            int maxDepth,
            int maxDistanceFromCenter,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings
    ) {
        super(settings);
        this.startPool = startPool;
        this.maxDepth = maxDepth;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        OptionalInt floorY = TerrainPlacementPolicy.findEndIslandPlacementY(
                context.chunkGenerator(),
                context.heightAccessor(),
                context.randomState(),
                x,
                z,
                50,
                7,
                4
        );
        if (floorY.isEmpty()) {
            return Optional.empty();
        }
        BlockPos anchor = new BlockPos(x, floorY.getAsInt(), z);
        return JigsawPlacement.addPieces(
                context,
                startPool,
                Optional.empty(),
                maxDepth,
                anchor,
                false,
                Optional.empty(),
                maxDistanceFromCenter,
                PoolAliasLookup.EMPTY,
                dimensionPadding,
                liquidSettings
        );
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.END_SPORE_SPIRE.get();
    }
}
