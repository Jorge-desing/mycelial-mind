package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.registry.ModStructures;
import com.alex.mycelialmind.world.TerrainPlacementPolicy;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import java.util.Optional;

/**
 * Custom subterranean Jigsaw structure for {@code hive_sanctum} that enforces spawn-safe
 * distance and probes {@code NoiseColumn} rock/fluid envelopes before committing Jigsaw layout.
 */
public class HiveSanctumStructure extends Structure {
    public static final MapCodec<HiveSanctumStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    Codec.intRange(1, 20).fieldOf("size").forGetter(s -> s.maxDepth),
                    HeightProvider.CODEC.fieldOf("start_height").forGetter(s -> s.startHeight),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                    DimensionPadding.CODEC.optionalFieldOf("dimension_padding", JigsawStructure.DEFAULT_DIMENSION_PADDING)
                            .forGetter(s -> s.dimensionPadding),
                    LiquidSettings.CODEC.optionalFieldOf("liquid_settings", LiquidSettings.IGNORE_WATERLOGGING)
                            .forGetter(s -> s.liquidSettings)
            ).apply(instance, HiveSanctumStructure::new)
    );

    private final Holder<StructureTemplatePool> startPool;
    private final int maxDepth;
    private final HeightProvider startHeight;
    private final int maxDistanceFromCenter;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;

    public HiveSanctumStructure(
            StructureSettings settings,
            Holder<StructureTemplatePool> startPool,
            int maxDepth,
            HeightProvider startHeight,
            int maxDistanceFromCenter,
            DimensionPadding dimensionPadding,
            LiquidSettings liquidSettings
    ) {
        super(settings);
        this.startPool = startPool;
        this.maxDepth = maxDepth;
        this.startHeight = startHeight;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.dimensionPadding = dimensionPadding;
        this.liquidSettings = liquidSettings;
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        if (TerrainPlacementPolicy.isWithinSpawnSafeZone(x, z, 0, 0, 160)) {
            return Optional.empty();
        }

        WorldGenerationContext worldContext = new WorldGenerationContext(
                context.chunkGenerator(), context.heightAccessor());
        int sampledY = startHeight.sample(context.random(), worldContext);
        int minAllowedY = context.heightAccessor().getMinBuildHeight() + 12;
        int maxAllowedY = Math.min(context.heightAccessor().getMaxBuildHeight() - 16, 12);
        int clampedBaseY = Math.max(minAllowedY, Math.min(maxAllowedY, sampledY));

        int[] candidateYs = {
                clampedBaseY,
                Math.max(minAllowedY, Math.min(maxAllowedY, clampedBaseY + 10)),
                Math.max(minAllowedY, Math.min(maxAllowedY, clampedBaseY - 10))
        };
        Integer chosenY = null;
        for (int candidateY : candidateYs) {
            if (TerrainPlacementPolicy.hasDrySolidSubterraneanEnvelope(
                    context.chunkGenerator(),
                    context.heightAccessor(),
                    context.randomState(),
                    x,
                    z,
                    candidateY,
                    12)) {
                chosenY = candidateY;
                break;
            }
        }
        if (chosenY == null) {
            return Optional.empty();
        }

        BlockPos anchor = new BlockPos(x, chosenY, z);
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
        return ModStructures.HIVE_SANCTUM.get();
    }
}
