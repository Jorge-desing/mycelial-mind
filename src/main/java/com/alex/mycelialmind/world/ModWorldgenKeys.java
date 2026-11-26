package com.alex.mycelialmind.world;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Central typed {@link ResourceKey} and {@link TagKey} catalog shared between
 * {@code DatapackBuiltinEntriesProvider} generation and runtime worldgen/dimension systems.
 */
public final class ModWorldgenKeys {
    // Dimension & Noise
    public static final ResourceKey<DimensionType> DEEP_MYCELIUM_TYPE =
            key(Registries.DIMENSION_TYPE, "deep_mycelium");
    public static final ResourceKey<NoiseGeneratorSettings> DEEP_MYCELIUM_NOISE =
            key(Registries.NOISE_SETTINGS, "deep_mycelium");
    public static final ResourceKey<LevelStem> DEEP_MYCELIUM_STEM =
            key(Registries.LEVEL_STEM, "deep_mycelium");

    // Biomes
    public static final ResourceKey<Biome> DEEP_MYCELIUM_CAVERNS =
            key(Registries.BIOME, "deep_mycelium_caverns");
    public static final ResourceKey<Biome> HIVE_HEART_SANCTUM =
            key(Registries.BIOME, "hive_heart_sanctum");
    public static final ResourceKey<Biome> LATENT_SPORE_FOREST =
            key(Registries.BIOME, "latent_spore_forest");

    // Configured Features
    public static final ResourceKey<ConfiguredFeature<?, ?>> MYCELIAL_OUTBREAK_CF =
            key(Registries.CONFIGURED_FEATURE, "mycelial_outbreak");
    public static final ResourceKey<ConfiguredFeature<?, ?>> MYCELIAL_SINKHOLE_CF =
            key(Registries.CONFIGURED_FEATURE, "mycelial_sinkhole");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SURFACE_HIVE_CF =
            key(Registries.CONFIGURED_FEATURE, "surface_hive");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEP_MYCELIUM_STALACTITES_CF =
            key(Registries.CONFIGURED_FEATURE, "deep_mycelium_stalactites");
    public static final ResourceKey<ConfiguredFeature<?, ?>> LATENT_SPORE_CANOPY_CF =
            key(Registries.CONFIGURED_FEATURE, "latent_spore_canopy");
    public static final ResourceKey<ConfiguredFeature<?, ?>> SANCTUM_AMBER_MONOLITH_CF =
            key(Registries.CONFIGURED_FEATURE, "sanctum_amber_monolith");
    public static final ResourceKey<ConfiguredFeature<?, ?>> DEEP_MYCELIUM_SPORE_POOL_CF =
            key(Registries.CONFIGURED_FEATURE, "deep_mycelium_spore_pool");

    // Placed Features
    public static final ResourceKey<PlacedFeature> DEEP_MYCELIUM_OUTBREAK_PF =
            key(Registries.PLACED_FEATURE, "deep_mycelium_outbreak");
    public static final ResourceKey<PlacedFeature> DEEP_MYCELIUM_STALACTITES_PF =
            key(Registries.PLACED_FEATURE, "deep_mycelium_stalactites");
    public static final ResourceKey<PlacedFeature> LATENT_SPORE_CANOPY_PF =
            key(Registries.PLACED_FEATURE, "latent_spore_canopy");
    public static final ResourceKey<PlacedFeature> SANCTUM_AMBER_MONOLITH_PF =
            key(Registries.PLACED_FEATURE, "sanctum_amber_monolith");
    public static final ResourceKey<PlacedFeature> DEEP_MYCELIUM_SPORE_POOL_PF =
            key(Registries.PLACED_FEATURE, "deep_mycelium_spore_pool");
    public static final ResourceKey<PlacedFeature> MYCELIAL_OUTBREAK_PF =
            key(Registries.PLACED_FEATURE, "mycelial_outbreak");
    public static final ResourceKey<PlacedFeature> MYCELIAL_SINKHOLE_PF =
            key(Registries.PLACED_FEATURE, "mycelial_sinkhole");
    public static final ResourceKey<PlacedFeature> SURFACE_HIVE_PF =
            key(Registries.PLACED_FEATURE, "surface_hive");
    public static final ResourceKey<PlacedFeature> SURFACE_SPORE_PATCH_PF =
            key(Registries.PLACED_FEATURE, "surface_spore_patch");

    // Processor Lists & Template Pools
    public static final ResourceKey<StructureProcessorList> ARCHITECTURAL_REFLECTION =
            key(Registries.PROCESSOR_LIST, "architectural_reflection");
    public static final ResourceKey<StructureProcessorList> NETHER_BREACH_DEGRADE =
            key(Registries.PROCESSOR_LIST, "nether_breach_degrade");
    public static final ResourceKey<StructureProcessorList> END_SPIRE_DEGRADE =
            key(Registries.PROCESSOR_LIST, "end_spire_degrade");
    public static final ResourceKey<StructureTemplatePool> HIVE_SANCTUM_START =
            key(Registries.TEMPLATE_POOL, "hive_sanctum/start_pool");
    public static final ResourceKey<StructureTemplatePool> HIVE_SANCTUM_CORRIDORS =
            key(Registries.TEMPLATE_POOL, "hive_sanctum/corridors");
    public static final ResourceKey<StructureTemplatePool> HIVE_SANCTUM_ROOMS =
            key(Registries.TEMPLATE_POOL, "hive_sanctum/rooms");
    public static final ResourceKey<StructureTemplatePool> HIVE_SANCTUM_CAPS =
            key(Registries.TEMPLATE_POOL, "hive_sanctum/caps");
    public static final ResourceKey<StructureTemplatePool> NETHER_BREACH_START =
            key(Registries.TEMPLATE_POOL, "nether_breach/start_pool");
    public static final ResourceKey<StructureTemplatePool> NETHER_BREACH_BRIDGES =
            key(Registries.TEMPLATE_POOL, "nether_breach/bridges");
    public static final ResourceKey<StructureTemplatePool> NETHER_BREACH_WINGS =
            key(Registries.TEMPLATE_POOL, "nether_breach/wings");
    public static final ResourceKey<StructureTemplatePool> NETHER_BREACH_CAPS =
            key(Registries.TEMPLATE_POOL, "nether_breach/caps");
    public static final ResourceKey<StructureTemplatePool> END_SPIRE_START =
            key(Registries.TEMPLATE_POOL, "end_spore_spire/start_pool");
    public static final ResourceKey<StructureTemplatePool> END_SPIRE_BRIDGES =
            key(Registries.TEMPLATE_POOL, "end_spore_spire/bridges");
    public static final ResourceKey<StructureTemplatePool> END_SPIRE_WINGS =
            key(Registries.TEMPLATE_POOL, "end_spore_spire/wings");
    public static final ResourceKey<StructureTemplatePool> END_SPIRE_CAPS =
            key(Registries.TEMPLATE_POOL, "end_spore_spire/caps");

    // Structures & Structure Sets
    public static final ResourceKey<Structure> HIVE_SANCTUM =
            key(Registries.STRUCTURE, "hive_sanctum");
    public static final ResourceKey<Structure> MATRIARCH_CITADEL =
            key(Registries.STRUCTURE, "matriarch_citadel");
    public static final ResourceKey<Structure> TWISTED_CITADEL =
            key(Registries.STRUCTURE, "twisted_citadel");
    public static final ResourceKey<Structure> NETHER_MYCELIAL_BREACH =
            key(Registries.STRUCTURE, "nether_mycelial_breach");
    public static final ResourceKey<Structure> END_SPORE_SPIRE =
            key(Registries.STRUCTURE, "end_spore_spire");

    public static final ResourceKey<StructureSet> HIVE_SANCTUM_SET =
            key(Registries.STRUCTURE_SET, "hive_sanctum");
    public static final ResourceKey<StructureSet> MATRIARCH_CITADEL_SET =
            key(Registries.STRUCTURE_SET, "matriarch_citadel");
    public static final ResourceKey<StructureSet> TWISTED_CITADEL_SET =
            key(Registries.STRUCTURE_SET, "twisted_citadel");
    public static final ResourceKey<StructureSet> NETHER_MYCELIAL_BREACH_SET =
            key(Registries.STRUCTURE_SET, "nether_mycelial_breach");
    public static final ResourceKey<StructureSet> END_SPORE_SPIRE_SET =
            key(Registries.STRUCTURE_SET, "end_spore_spire");

    // NeoForge Biome Modifiers
    public static final ResourceKey<BiomeModifier> ADD_MYCELIAL_FEATURES =
            key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, "mycelial_features");
    public static final ResourceKey<BiomeModifier> ADD_MYCELIAL_SPAWNS =
            key(NeoForgeRegistries.Keys.BIOME_MODIFIERS, "mycelial_spawns");

    // Worldgen Tags
    public static final TagKey<Biome> HAS_MATRIARCH_CITADEL =
            TagKey.create(Registries.BIOME, id("has_structure/matriarch_citadel"));
    public static final TagKey<Biome> HAS_TWISTED_CITADEL =
            TagKey.create(Registries.BIOME, id("has_structure/twisted_citadel"));
    public static final TagKey<Biome> HAS_NETHER_BREACH =
            TagKey.create(Registries.BIOME, id("has_structure/nether_mycelial_breach"));
    public static final TagKey<Biome> HAS_END_SPIRE =
            TagKey.create(Registries.BIOME, id("has_structure/end_spore_spire"));
    public static final TagKey<Structure> SANCTUMS =
            TagKey.create(Registries.STRUCTURE, id("sanctums"));

    private ModWorldgenKeys() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, path);
    }

    private static <T> ResourceKey<T> key(ResourceKey<? extends Registry<T>> registry, String path) {
        return ResourceKey.create(registry, id(path));
    }
}
