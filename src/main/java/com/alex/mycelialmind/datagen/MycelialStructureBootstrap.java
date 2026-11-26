package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.world.ModWorldgenKeys;
import com.alex.mycelialmind.world.structure.ArchitecturalAccentProcessor;
import com.alex.mycelialmind.world.structure.EndSpireStructure;
import com.alex.mycelialmind.world.structure.HiveSanctumStructure;
import com.alex.mycelialmind.world.structure.MatriarchCitadelStructure;
import com.alex.mycelialmind.world.structure.MycelialBiomeDegradeProcessor;
import com.alex.mycelialmind.world.structure.MycelialFluidClearProcessor;
import com.alex.mycelialmind.world.structure.MycelialFoundationPillarProcessor;
import com.alex.mycelialmind.world.structure.NetherBreachStructure;
import com.alex.mycelialmind.world.structure.TwistedCitadelStructure;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Pools;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool.Projection;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;

import java.util.List;

/**
 * Datapack bootstrap definitions for processor lists, Jigsaw template pools, structures,
 * and structure sets.
 */
public final class MycelialStructureBootstrap {
    private MycelialStructureBootstrap() {
    }

    public static void bootstrapProcessorLists(BootstrapContext<StructureProcessorList> context) {
        RuleProcessor organicWeathering = new RuleProcessor(List.of(
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.DEEPSLATE_BRICKS, 0.18F),
                        AlwaysTrueTest.INSTANCE,
                        Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState()
                ),
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.DEEPSLATE_BRICKS, 0.12F),
                        AlwaysTrueTest.INSTANCE,
                        ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState()
                ),
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.DEEPSLATE_TILES, 0.18F),
                        AlwaysTrueTest.INSTANCE,
                        Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState()
                ),
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.DEEPSLATE_TILES, 0.10F),
                        AlwaysTrueTest.INSTANCE,
                        ModBlocks.MYCELIAL_STONE.get().defaultBlockState()
                )
        ));

        context.register(
                ModWorldgenKeys.ARCHITECTURAL_REFLECTION,
                new StructureProcessorList(List.of(
                        new MycelialFluidClearProcessor(),
                        new MycelialFoundationPillarProcessor(),
                        new MycelialBiomeDegradeProcessor(),
                        new ArchitecturalAccentProcessor(),
                        organicWeathering,
                        new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)
                ))
        );

        RuleProcessor netherWeathering = new RuleProcessor(List.of(
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.POLISHED_BLACKSTONE_BRICKS, 0.22F),
                        AlwaysTrueTest.INSTANCE,
                        Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.defaultBlockState()
                ),
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.NETHER_BRICKS, 0.18F),
                        AlwaysTrueTest.INSTANCE,
                        Blocks.CRACKED_NETHER_BRICKS.defaultBlockState()
                ),
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.NETHER_BRICKS, 0.08F),
                        AlwaysTrueTest.INSTANCE,
                        ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState()
                )
        ));
        context.register(
                ModWorldgenKeys.NETHER_BREACH_DEGRADE,
                new StructureProcessorList(List.of(
                        new MycelialFluidClearProcessor(),
                        new MycelialFoundationPillarProcessor(),
                        netherWeathering,
                        new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)
                ))
        );

        RuleProcessor endWeathering = new RuleProcessor(List.of(
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.END_STONE_BRICKS, 0.14F),
                        AlwaysTrueTest.INSTANCE,
                        ModBlocks.MYCELIAL_STONE.get().defaultBlockState()
                ),
                new ProcessorRule(
                        new RandomBlockMatchTest(Blocks.PURPUR_BLOCK, 0.12F),
                        AlwaysTrueTest.INSTANCE,
                        ModBlocks.MYCELIAL_ARCH_BRICK.get().defaultBlockState()
                )
        ));
        context.register(
                ModWorldgenKeys.END_SPIRE_DEGRADE,
                new StructureProcessorList(List.of(
                        new MycelialFluidClearProcessor(),
                        new MycelialFoundationPillarProcessor(),
                        endWeathering,
                        new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE)
                ))
        );
    }

    public static void bootstrapTemplatePools(BootstrapContext<StructureTemplatePool> context) {
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        HolderGetter<StructureProcessorList> processors = context.lookup(Registries.PROCESSOR_LIST);

        Holder<StructureTemplatePool> empty = pools.getOrThrow(Pools.EMPTY);
        Holder<StructureTemplatePool> caps = pools.getOrThrow(ModWorldgenKeys.HIVE_SANCTUM_CAPS);
        Holder<StructureProcessorList> reflection = processors.getOrThrow(ModWorldgenKeys.ARCHITECTURAL_REFLECTION);
        Holder<StructureProcessorList> netherDegrade = processors.getOrThrow(ModWorldgenKeys.NETHER_BREACH_DEGRADE);
        Holder<StructureProcessorList> endDegrade = processors.getOrThrow(ModWorldgenKeys.END_SPIRE_DEGRADE);

        context.register(ModWorldgenKeys.HIVE_SANCTUM_CAPS, new StructureTemplatePool(
                empty,
                List.of(Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/cap_end", reflection), 1)),
                Projection.RIGID
        ));

        context.register(ModWorldgenKeys.HIVE_SANCTUM_CORRIDORS, new StructureTemplatePool(
                caps,
                List.of(
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/corridor_straight", reflection), 4),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/corridor_turn", reflection), 3),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/corridor_cross", reflection), 2),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/vertical_shaft", reflection), 2),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/tactical_gallery", reflection), 2),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/ribbed_nave", reflection), 2)
                ),
                Projection.RIGID
        ));

        context.register(ModWorldgenKeys.HIVE_SANCTUM_ROOMS, new StructureTemplatePool(
                caps,
                List.of(
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/spore_vault", reflection), 3),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/incubation_wing", reflection), 3),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/forbidden_lab", reflection), 2),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/ossuary_crypt", reflection), 2),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/quarantine_ward", reflection), 2),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/secret_reliquary", reflection), 1),
                        Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/corridor_straight", reflection), 2)
                ),
                Projection.RIGID
        ));

        context.register(ModWorldgenKeys.HIVE_SANCTUM_START, new StructureTemplatePool(
                empty,
                List.of(Pair.of(StructurePoolElement.single("mycelialmind:hive_sanctum/sanctum_atrium", reflection), 1)),
                Projection.RIGID
        ));

        Holder<StructureTemplatePool> netherCaps = pools.getOrThrow(ModWorldgenKeys.NETHER_BREACH_CAPS);
        context.register(ModWorldgenKeys.NETHER_BREACH_CAPS, new StructureTemplatePool(
                empty,
                List.of(Pair.of(StructurePoolElement.single("mycelialmind:nether_breach/breach_cap", netherDegrade), 1)),
                Projection.RIGID
        ));
        context.register(ModWorldgenKeys.NETHER_BREACH_BRIDGES, new StructureTemplatePool(
                netherCaps,
                List.of(
                        Pair.of(StructurePoolElement.single("mycelialmind:nether_breach/basalt_bridge", netherDegrade), 4),
                        Pair.of(StructurePoolElement.single("mycelialmind:nether_breach/magma_conduit", netherDegrade), 2)
                ),
                Projection.RIGID
        ));
        context.register(ModWorldgenKeys.NETHER_BREACH_WINGS, new StructureTemplatePool(
                netherCaps,
                List.of(
                        Pair.of(StructurePoolElement.single("mycelialmind:nether_breach/pyro_barracks", netherDegrade), 3),
                        Pair.of(StructurePoolElement.single("mycelialmind:nether_breach/watchtower_keep", netherDegrade), 2),
                        Pair.of(StructurePoolElement.single("mycelialmind:nether_breach/magma_conduit", netherDegrade), 2)
                ),
                Projection.RIGID
        ));
        context.register(ModWorldgenKeys.NETHER_BREACH_START, new StructureTemplatePool(
                empty,
                List.of(Pair.of(StructurePoolElement.single("mycelialmind:nether_breach/breach_sanctum", netherDegrade), 1)),
                Projection.RIGID
        ));

        Holder<StructureTemplatePool> endCaps = pools.getOrThrow(ModWorldgenKeys.END_SPIRE_CAPS);
        context.register(ModWorldgenKeys.END_SPIRE_CAPS, new StructureTemplatePool(
                empty,
                List.of(Pair.of(StructurePoolElement.single("mycelialmind:end_spore_spire/spire_cap", endDegrade), 1)),
                Projection.RIGID
        ));
        context.register(ModWorldgenKeys.END_SPIRE_BRIDGES, new StructureTemplatePool(
                endCaps,
                List.of(
                        Pair.of(StructurePoolElement.single("mycelialmind:end_spore_spire/void_bridge", endDegrade), 4)
                ),
                Projection.RIGID
        ));
        context.register(ModWorldgenKeys.END_SPIRE_WINGS, new StructureTemplatePool(
                endCaps,
                List.of(
                        Pair.of(StructurePoolElement.single("mycelialmind:end_spore_spire/resonance_observatory", endDegrade), 3),
                        Pair.of(StructurePoolElement.single("mycelialmind:end_spore_spire/void_crypt", endDegrade), 2),
                        Pair.of(StructurePoolElement.single("mycelialmind:end_spore_spire/purpur_bastion", endDegrade), 2)
                ),
                Projection.RIGID
        ));
        context.register(ModWorldgenKeys.END_SPIRE_START, new StructureTemplatePool(
                empty,
                List.of(Pair.of(StructurePoolElement.single("mycelialmind:end_spore_spire/spire_keep", endDegrade), 1)),
                Projection.RIGID
        ));
    }

    public static void bootstrapStructures(BootstrapContext<Structure> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);

        context.register(ModWorldgenKeys.HIVE_SANCTUM, new HiveSanctumStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(BiomeTags.IS_OVERWORLD))
                        .generationStep(GenerationStep.Decoration.UNDERGROUND_STRUCTURES)
                        .terrainAdapation(TerrainAdjustment.ENCAPSULATE)
                        .build(),
                pools.getOrThrow(ModWorldgenKeys.HIVE_SANCTUM_START),
                6,
                UniformHeight.of(VerticalAnchor.absolute(-36), VerticalAnchor.absolute(-4)),
                64,
                new DimensionPadding(8, 224),
                LiquidSettings.IGNORE_WATERLOGGING
        ));

        context.register(ModWorldgenKeys.MATRIARCH_CITADEL, new MatriarchCitadelStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(ModWorldgenKeys.HAS_MATRIARCH_CITADEL))
                        .generationStep(GenerationStep.Decoration.UNDERGROUND_STRUCTURES)
                        .terrainAdapation(TerrainAdjustment.BEARD_THIN)
                        .build()
        ));

        context.register(ModWorldgenKeys.TWISTED_CITADEL, new TwistedCitadelStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(ModWorldgenKeys.HAS_TWISTED_CITADEL))
                        .generationStep(GenerationStep.Decoration.UNDERGROUND_STRUCTURES)
                        .terrainAdapation(TerrainAdjustment.BEARD_THIN)
                        .build()
        ));

        context.register(ModWorldgenKeys.NETHER_MYCELIAL_BREACH, new NetherBreachStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(ModWorldgenKeys.HAS_NETHER_BREACH))
                        .generationStep(GenerationStep.Decoration.UNDERGROUND_STRUCTURES)
                        .terrainAdapation(TerrainAdjustment.BEARD_THIN)
                        .build(),
                pools.getOrThrow(ModWorldgenKeys.NETHER_BREACH_START),
                4,
                48,
                new DimensionPadding(4, 14),
                LiquidSettings.IGNORE_WATERLOGGING
        ));

        context.register(ModWorldgenKeys.END_SPORE_SPIRE, new EndSpireStructure(
                new Structure.StructureSettings.Builder(biomes.getOrThrow(ModWorldgenKeys.HAS_END_SPIRE))
                        .generationStep(GenerationStep.Decoration.SURFACE_STRUCTURES)
                        .terrainAdapation(TerrainAdjustment.BEARD_THIN)
                        .build(),
                pools.getOrThrow(ModWorldgenKeys.END_SPIRE_START),
                4,
                44,
                DimensionPadding.ZERO,
                LiquidSettings.IGNORE_WATERLOGGING
        ));
    }

    public static void bootstrapStructureSets(BootstrapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);

        context.register(ModWorldgenKeys.HIVE_SANCTUM_SET, new StructureSet(
                structures.getOrThrow(ModWorldgenKeys.HIVE_SANCTUM),
                new RandomSpreadStructurePlacement(64, 32, RandomSpreadType.LINEAR, 73194218)
        ));

        context.register(ModWorldgenKeys.MATRIARCH_CITADEL_SET, new StructureSet(
                structures.getOrThrow(ModWorldgenKeys.MATRIARCH_CITADEL),
                new RandomSpreadStructurePlacement(72, 36, RandomSpreadType.LINEAR, 94182736)
        ));

        context.register(ModWorldgenKeys.TWISTED_CITADEL_SET, new StructureSet(
                structures.getOrThrow(ModWorldgenKeys.TWISTED_CITADEL),
                new RandomSpreadStructurePlacement(24, 12, RandomSpreadType.LINEAR, 91472835)
        ));

        context.register(ModWorldgenKeys.NETHER_MYCELIAL_BREACH_SET, new StructureSet(
                structures.getOrThrow(ModWorldgenKeys.NETHER_MYCELIAL_BREACH),
                new RandomSpreadStructurePlacement(28, 14, RandomSpreadType.LINEAR, 58291403)
        ));

        context.register(ModWorldgenKeys.END_SPORE_SPIRE_SET, new StructureSet(
                structures.getOrThrow(ModWorldgenKeys.END_SPORE_SPIRE),
                new RandomSpreadStructurePlacement(32, 16, RandomSpreadType.LINEAR, 69412751)
        ));
    }
}
