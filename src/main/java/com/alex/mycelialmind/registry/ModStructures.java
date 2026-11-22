package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.world.structure.ArchitecturalAccentProcessor;
import com.alex.mycelialmind.world.structure.MatriarchCitadelPiece;
import com.alex.mycelialmind.world.structure.MatriarchCitadelStructure;
import com.alex.mycelialmind.world.structure.TwistedCitadelPiece;
import com.alex.mycelialmind.world.structure.TwistedCitadelStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom procedural structures, structure pieces, and template processors for Mycelial Mind.
 */
public final class ModStructures {
    public static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, MycelialMind.MODID);

    public static final DeferredRegister<StructurePieceType> STRUCTURE_PIECES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, MycelialMind.MODID);

    public static final DeferredRegister<StructureProcessorType<?>> STRUCTURE_PROCESSORS =
            DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, MycelialMind.MODID);

    public static final DeferredHolder<StructureType<?>, StructureType<MatriarchCitadelStructure>> MATRIARCH_CITADEL =
            STRUCTURE_TYPES.register("matriarch_citadel", () -> () -> MatriarchCitadelStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> MATRIARCH_CITADEL_PIECE =
            STRUCTURE_PIECES.register("matriarch_citadel_piece",
                    () -> (StructurePieceType.ContextlessType) MatriarchCitadelPiece::new);

    public static final DeferredHolder<StructureType<?>, StructureType<TwistedCitadelStructure>> TWISTED_CITADEL =
            STRUCTURE_TYPES.register("twisted_citadel", () -> () -> TwistedCitadelStructure.CODEC);

    public static final DeferredHolder<StructurePieceType, StructurePieceType> TWISTED_CITADEL_PIECE =
            STRUCTURE_PIECES.register("twisted_citadel_piece",
                    () -> (StructurePieceType.ContextlessType) TwistedCitadelPiece::new);

    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<ArchitecturalAccentProcessor>> ARCHITECTURAL_ACCENT_PROCESSOR =
            STRUCTURE_PROCESSORS.register("architectural_accent", () -> () -> ArchitecturalAccentProcessor.CODEC);

    private ModStructures() {
    }

    public static void register(IEventBus modEventBus) {
        STRUCTURE_TYPES.register(modEventBus);
        STRUCTURE_PIECES.register(modEventBus);
        STRUCTURE_PROCESSORS.register(modEventBus);
    }
}
