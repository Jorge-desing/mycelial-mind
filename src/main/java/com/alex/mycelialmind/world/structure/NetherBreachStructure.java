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
 * Subterranean Nether structure anchored onto dry dry-rock ledges above the Y=32 lava sea
 * ({@code Y >= 33}, zero chunk loads during placement).
 */
public class NetherBreachStructure extends Structure {
    public static final MapCodec<NetherBreachStructure> CODEC = simpleCodec(NetherBreachStructure::new);

    public NetherBreachStructure(StructureSettings settings) {
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
        BlockPos center = new BlockPos(x, anchorY + 1, z);
        return Optional.of(new GenerationStub(center, builder -> builder.addPiece(new NetherBreachPiece(center))));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.NETHER_MYCELIAL_BREACH.get();
    }
}
