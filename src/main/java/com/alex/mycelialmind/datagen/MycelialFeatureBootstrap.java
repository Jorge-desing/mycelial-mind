package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.registry.ModFeatures;
import com.alex.mycelialmind.world.DeepMyceliumTerrainFeature;
import com.alex.mycelialmind.world.HiveTierRarityPlacement;
import com.alex.mycelialmind.world.ModWorldgenKeys;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.BiomeFilter;
import net.minecraft.world.level.levelgen.placement.CountPlacement;
import net.minecraft.world.level.levelgen.placement.HeightRangePlacement;
import net.minecraft.world.level.levelgen.placement.HeightmapPlacement;
import net.minecraft.world.level.levelgen.placement.InSquarePlacement;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

import java.util.List;

/**
 * Datapack bootstrap definitions for all ConfiguredFeature and PlacedFeature worldgen entries.
 */
public final class MycelialFeatureBootstrap {
    private MycelialFeatureBootstrap() {
    }

    public static void bootstrapConfiguredFeatures(BootstrapContext<ConfiguredFeature<?, ?>> context) {
        context.register(
                ModWorldgenKeys.MYCELIAL_OUTBREAK_CF,
                new ConfiguredFeature<>(ModFeatures.MYCELIAL_OUTBREAK.get(), NoneFeatureConfiguration.INSTANCE)
        );
        context.register(
                ModWorldgenKeys.MYCELIAL_SINKHOLE_CF,
                new ConfiguredFeature<>(ModFeatures.MYCELIAL_SINKHOLE.get(), NoneFeatureConfiguration.INSTANCE)
        );
        context.register(
                ModWorldgenKeys.SURFACE_HIVE_CF,
                new ConfiguredFeature<>(ModFeatures.SURFACE_HIVE.get(), NoneFeatureConfiguration.INSTANCE)
        );
        context.register(
                ModWorldgenKeys.DEEP_MYCELIUM_STALACTITES_CF,
                new ConfiguredFeature<>(
                        ModFeatures.DEEP_MYCELIUM_TERRAIN.get(),
                        new DeepMyceliumTerrainFeature.Config(DeepMyceliumTerrainFeature.Variant.CAVERN_STALACTITES)
                )
        );
        context.register(
                ModWorldgenKeys.LATENT_SPORE_CANOPY_CF,
                new ConfiguredFeature<>(
                        ModFeatures.DEEP_MYCELIUM_TERRAIN.get(),
                        new DeepMyceliumTerrainFeature.Config(DeepMyceliumTerrainFeature.Variant.SPORE_CANOPY_ARCH)
                )
        );
        context.register(
                ModWorldgenKeys.SANCTUM_AMBER_MONOLITH_CF,
                new ConfiguredFeature<>(
                        ModFeatures.DEEP_MYCELIUM_TERRAIN.get(),
                        new DeepMyceliumTerrainFeature.Config(DeepMyceliumTerrainFeature.Variant.SANCTUM_AMBER_MONOLITH)
                )
        );
    }

    public static void bootstrapPlacedFeatures(BootstrapContext<PlacedFeature> context) {
        HolderGetter<ConfiguredFeature<?, ?>> configured = context.lookup(Registries.CONFIGURED_FEATURE);

        context.register(ModWorldgenKeys.DEEP_MYCELIUM_OUTBREAK_PF, new PlacedFeature(
                configured.getOrThrow(ModWorldgenKeys.MYCELIAL_OUTBREAK_CF),
                List.of(
                        new HiveTierRarityPlacement(32, 24, 18, 12),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(28), VerticalAnchor.absolute(168)),
                        BiomeFilter.biome()
                )
        ));

        context.register(ModWorldgenKeys.DEEP_MYCELIUM_STALACTITES_PF, new PlacedFeature(
                configured.getOrThrow(ModWorldgenKeys.DEEP_MYCELIUM_STALACTITES_CF),
                List.of(
                        CountPlacement.of(4),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(30), VerticalAnchor.absolute(156)),
                        BiomeFilter.biome()
                )
        ));

        context.register(ModWorldgenKeys.LATENT_SPORE_CANOPY_PF, new PlacedFeature(
                configured.getOrThrow(ModWorldgenKeys.LATENT_SPORE_CANOPY_CF),
                List.of(
                        CountPlacement.of(4),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(30), VerticalAnchor.absolute(156)),
                        BiomeFilter.biome()
                )
        ));

        context.register(ModWorldgenKeys.SANCTUM_AMBER_MONOLITH_PF, new PlacedFeature(
                configured.getOrThrow(ModWorldgenKeys.SANCTUM_AMBER_MONOLITH_CF),
                List.of(
                        CountPlacement.of(3),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(30), VerticalAnchor.absolute(156)),
                        BiomeFilter.biome()
                )
        ));

        context.register(ModWorldgenKeys.MYCELIAL_OUTBREAK_PF, new PlacedFeature(
                configured.getOrThrow(ModWorldgenKeys.MYCELIAL_OUTBREAK_CF),
                List.of(
                        new HiveTierRarityPlacement(28, 20, 14, 10),
                        InSquarePlacement.spread(),
                        HeightRangePlacement.uniform(VerticalAnchor.absolute(-48), VerticalAnchor.absolute(40)),
                        BiomeFilter.biome()
                )
        ));

        context.register(ModWorldgenKeys.MYCELIAL_SINKHOLE_PF, new PlacedFeature(
                configured.getOrThrow(ModWorldgenKeys.MYCELIAL_SINKHOLE_CF),
                List.of(
                        new HiveTierRarityPlacement(280, 220, 170, 130),
                        InSquarePlacement.spread(),
                        HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                        BiomeFilter.biome()
                )
        ));

        context.register(ModWorldgenKeys.SURFACE_HIVE_PF, new PlacedFeature(
                configured.getOrThrow(ModWorldgenKeys.SURFACE_HIVE_CF),
                List.of(
                        new HiveTierRarityPlacement(220, 150, 104, 72),
                        InSquarePlacement.spread(),
                        HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                        BiomeFilter.biome()
                )
        ));

        context.register(ModWorldgenKeys.SURFACE_SPORE_PATCH_PF, new PlacedFeature(
                configured.getOrThrow(ModWorldgenKeys.MYCELIAL_OUTBREAK_CF),
                List.of(
                        new HiveTierRarityPlacement(192, 128, 88, 56),
                        InSquarePlacement.spread(),
                        HeightmapPlacement.onHeightmap(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES),
                        BiomeFilter.biome()
                )
        ));
    }
}
