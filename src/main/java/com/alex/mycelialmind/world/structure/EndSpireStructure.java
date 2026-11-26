package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.registry.ModStructures;
import com.alex.mycelialmind.world.TerrainPlacementPolicy;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Outer End-island fungal spire anchored on dry solid end-stone islands ({@code r >= 1000m},
 * {@code Y >= 50}, slope {@code <= 4} across >= 7/9 probed columns, zero void overhang).
 */
public class EndSpireStructure extends Structure {
    public static final MapCodec<EndSpireStructure> CODEC = simpleCodec(EndSpireStructure::new);

    public EndSpireStructure(StructureSettings settings) {
        super(settings);
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
        BlockPos center = new BlockPos(x, floorY.getAsInt(), z);
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new EndSpirePiece(center))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.END_SPORE_SPIRE.get();
    }
}
