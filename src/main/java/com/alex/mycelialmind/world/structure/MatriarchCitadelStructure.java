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
 * Legendary Deep Mycelium mega-structure: the Cathedral-Citadel of the Apex Matriarch.
 */
public class MatriarchCitadelStructure extends Structure {
    public static final MapCodec<MatriarchCitadelStructure> CODEC = simpleCodec(MatriarchCitadelStructure::new);

    public MatriarchCitadelStructure(StructureSettings settings) {
        super(settings);
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
                28,
                128,
                36,
                48,
                12
        );
        if (floorY.isEmpty()) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(x, floorY.getAsInt() + 1, z);
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new MatriarchCitadelPiece(center))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.MATRIARCH_CITADEL.get();
    }
}
