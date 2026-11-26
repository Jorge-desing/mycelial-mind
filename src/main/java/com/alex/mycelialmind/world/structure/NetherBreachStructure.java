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
 * Multi-piece Jigsaw Nether breach structure anchored onto dry cavern rock above the
 * Y=32 lava sea ({@code Y >= 33}, zero chunk loads during placement).
 */
public class NetherBreachStructure extends Structure {
    public static final MapCodec<NetherBreachStructure> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    settingsCodec(instance),
                    StructureTemplatePool.CODEC.fieldOf("start_pool").forGetter(s -> s.startPool),
                    Codec.intRange(1, 20).fieldOf("size").forGetter(s -> s.maxDepth),
                    Codec.intRange(1, 128).fieldOf("max_distance_from_center").forGetter(s -> s.maxDistanceFromCenter),
                    DimensionPadding.CODEC.optionalFieldOf("dimension_padding", JigsawStructure.DEFAULT_DIMENSION_PADDING)
                            .forGetter(s -> s.dimensionPadding),
                    LiquidSettings.CODEC.optionalFieldOf("liquid_settings", LiquidSettings.IGNORE_WATERLOGGING)
                            .forGetter(s -> s.liquidSettings)
            ).apply(instance, NetherBreachStructure::new)
    );

    private final Holder<StructureTemplatePool> startPool;
    private final int maxDepth;
    private final int maxDistanceFromCenter;
    private final DimensionPadding dimensionPadding;
    private final LiquidSettings liquidSettings;

    public NetherBreachStructure(
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
        OptionalInt floorY = TerrainPlacementPolicy.findCavernPlacementY(
                context.chunkGenerator(),
                context.heightAccessor(),
                context.randomState(),
                x,
                z,
                34,
                104,
                15,
                56,
                8,
                10
        );
        if (floorY.isEmpty()) {
            return Optional.empty();
        }
        int anchorY = Math.max(33, floorY.getAsInt());
        BlockPos anchor = new BlockPos(x, anchorY + 1, z);
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
        return ModStructures.NETHER_MYCELIAL_BREACH.get();
    }
}
