package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModSounds;
import com.alex.mycelialmind.world.ModWorldgenKeys;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.data.worldgen.Carvers;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.AmbientAdditionsSettings;
import net.minecraft.world.level.biome.AmbientMoodSettings;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeGenerationSettings;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;

import java.util.List;

/**
 * Datapack bootstrap definitions for all 3 Deep Mycelium biomes and both NeoForge Overworld
 * biome modifiers (features and natural mob spawns).
 */
public final class MycelialBiomeBootstrap {
    private MycelialBiomeBootstrap() {
    }

    public static void bootstrapBiomes(BootstrapContext<Biome> context) {
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderGetter<ConfiguredWorldCarver<?>> carvers = context.lookup(Registries.CONFIGURED_CARVER);

        context.register(ModWorldgenKeys.DEEP_MYCELIUM_CAVERNS, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.6F)
                .downfall(0.5F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(3482438)
                        .fogColor(2758716)
                        .waterColor(4159204)
                        .waterFogColor(2302743)
                        .ambientLoopSound(ModSounds.BIOME_CAVERNS_LOOP.getDelegate())
                        .ambientMoodSound(new AmbientMoodSettings(ModSounds.BIOME_CAVERNS_MOOD.getDelegate(), 3600, 8, 2.0D))
                        .ambientAdditionsSound(new AmbientAdditionsSettings(ModSounds.BIOME_CAVERNS_ADDITIONS.getDelegate(), 0.0111D))
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder()
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.BURROWER.get(), 35, 1, 2))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.SYNAPSE_WEAVER.get(), 20, 1, 2))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.LEECH_PARASITE.get(), 40, 2, 4))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.SPORE_ARCHITECT.get(), 10, 1, 1))
                        .build())
                .generationSettings(new BiomeGenerationSettings.Builder(placedFeatures, carvers)
                        .addCarver(GenerationStep.Carving.AIR, Carvers.CAVE)
                        .addCarver(GenerationStep.Carving.AIR, Carvers.CAVE_EXTRA_UNDERGROUND)
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, ModWorldgenKeys.DEEP_MYCELIUM_STALACTITES_PF)
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, ModWorldgenKeys.DEEP_MYCELIUM_SPORE_POOL_PF)
                        .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModWorldgenKeys.DEEP_MYCELIUM_OUTBREAK_PF)
                        .build())
                .build());

        context.register(ModWorldgenKeys.HIVE_HEART_SANCTUM, new Biome.BiomeBuilder()
                .hasPrecipitation(false)
                .temperature(0.8F)
                .downfall(0.4F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(2493748)
                        .fogColor(1968684)
                        .waterColor(5317720)
                        .waterFogColor(2758716)
                        .ambientLoopSound(ModSounds.BIOME_SANCTUM_LOOP.getDelegate())
                        .ambientMoodSound(new AmbientMoodSettings(ModSounds.BIOME_SANCTUM_MOOD.getDelegate(), 3400, 8, 2.0D))
                        .ambientAdditionsSound(new AmbientAdditionsSettings(ModSounds.BIOME_SANCTUM_ADDITIONS.getDelegate(), 0.0125D))
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder()
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.COLONY_WARDEN.get(), 15, 1, 1))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.GOLIATH_CARRIER.get(), 20, 1, 1))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.PYRO_SHROOM.get(), 25, 1, 2))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.CHITIN_BRUTE.get(), 15, 1, 2))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.SYNAPTIC_MIMIC.get(), 6, 1, 1))
                        .build())
                .generationSettings(new BiomeGenerationSettings.Builder(placedFeatures, carvers)
                        .addCarver(GenerationStep.Carving.AIR, Carvers.CAVE)
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, ModWorldgenKeys.SANCTUM_AMBER_MONOLITH_PF)
                        .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModWorldgenKeys.DEEP_MYCELIUM_OUTBREAK_PF)
                        .build())
                .build());

        context.register(ModWorldgenKeys.LATENT_SPORE_FOREST, new Biome.BiomeBuilder()
                .hasPrecipitation(true)
                .temperature(0.7F)
                .downfall(0.8F)
                .specialEffects(new BiomeSpecialEffects.Builder()
                        .skyColor(5849459)
                        .fogColor(4599640)
                        .waterColor(4159204)
                        .waterFogColor(2302743)
                        .grassColorOverride(6717291)
                        .foliageColorOverride(6258791)
                        .ambientLoopSound(ModSounds.BIOME_FOREST_LOOP.getDelegate())
                        .ambientMoodSound(new AmbientMoodSettings(ModSounds.BIOME_FOREST_MOOD.getDelegate(), 3600, 8, 2.0D))
                        .ambientAdditionsSound(new AmbientAdditionsSettings(ModSounds.BIOME_FOREST_ADDITIONS.getDelegate(), 0.0111D))
                        .build())
                .mobSpawnSettings(new MobSpawnSettings.Builder()
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.SPORELING.get(), 60, 2, 5))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.MYCELIAL_STALKER.get(), 25, 1, 2))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.ACID_SPITTER.get(), 15, 1, 2))
                        .addSpawn(MobCategory.MONSTER, new MobSpawnSettings.SpawnerData(ModEntities.CHITIN_BRUTE.get(), 15, 1, 2))
                        .build())
                .generationSettings(new BiomeGenerationSettings.Builder(placedFeatures, carvers)
                        .addCarver(GenerationStep.Carving.AIR, Carvers.CAVE)
                        .addCarver(GenerationStep.Carving.AIR, Carvers.CAVE_EXTRA_UNDERGROUND)
                        .addCarver(GenerationStep.Carving.AIR, Carvers.CANYON)
                        .addFeature(GenerationStep.Decoration.UNDERGROUND_DECORATION, ModWorldgenKeys.DEEP_MYCELIUM_SPORE_POOL_PF)
                        .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModWorldgenKeys.LATENT_SPORE_CANOPY_PF)
                        .addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, ModWorldgenKeys.DEEP_MYCELIUM_OUTBREAK_PF)
                        .build())
                .build());
    }

    public static void bootstrapBiomeModifiers(BootstrapContext<BiomeModifier> context) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        HolderGetter<PlacedFeature> placedFeatures = context.lookup(Registries.PLACED_FEATURE);
        HolderSet<Biome> overworld = biomes.getOrThrow(BiomeTags.IS_OVERWORLD);

        context.register(ModWorldgenKeys.ADD_MYCELIAL_FEATURES, new BiomeModifiers.AddFeaturesBiomeModifier(
                overworld,
                HolderSet.direct(
                        placedFeatures.getOrThrow(ModWorldgenKeys.MYCELIAL_OUTBREAK_PF),
                        placedFeatures.getOrThrow(ModWorldgenKeys.SURFACE_SPORE_PATCH_PF),
                        placedFeatures.getOrThrow(ModWorldgenKeys.SURFACE_HIVE_PF),
                        placedFeatures.getOrThrow(ModWorldgenKeys.MYCELIAL_SINKHOLE_PF)
                ),
                GenerationStep.Decoration.VEGETAL_DECORATION
        ));

        context.register(ModWorldgenKeys.ADD_MYCELIAL_SPAWNS, new BiomeModifiers.AddSpawnsBiomeModifier(
                overworld,
                List.of(
                        new MobSpawnSettings.SpawnerData(ModEntities.SPORELING.get(), 16, 2, 4),
                        new MobSpawnSettings.SpawnerData(ModEntities.MYCELIAL_STALKER.get(), 6, 1, 2),
                        new MobSpawnSettings.SpawnerData(ModEntities.CHITIN_BRUTE.get(), 2, 1, 1)
                )
        ));
    }
}
