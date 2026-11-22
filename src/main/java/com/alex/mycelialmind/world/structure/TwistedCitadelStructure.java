package com.alex.mycelialmind.world.structure;

import com.alex.mycelialmind.registry.ModStructures;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

import java.util.Optional;

/**
 * Dimensional Twisted Citadel structure in deep_mycelium that generates clusters of
 * fungal-overgrown structures built from the player's block palette and scale proportions.
 */
public class TwistedCitadelStructure extends Structure {
    public static final MapCodec<TwistedCitadelStructure> CODEC = simpleCodec(TwistedCitadelStructure::new);

    public TwistedCitadelStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    protected Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        int x = context.chunkPos().getMiddleBlockX();
        int z = context.chunkPos().getMiddleBlockZ();
        BlockPos center = new BlockPos(x, 32, z);
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new TwistedCitadelPiece(center))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.TWISTED_CITADEL.get();
    }
}
