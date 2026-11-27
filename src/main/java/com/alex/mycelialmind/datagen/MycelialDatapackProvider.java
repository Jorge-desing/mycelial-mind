package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Emits all Mycelial Mind datapack-driven worldgen, dimension, structure, and biome modifier entries.
 */
public final class MycelialDatapackProvider extends DatapackBuiltinEntriesProvider {
    public static final RegistrySetBuilder BUILDER = new RegistrySetBuilder()
            .add(Registries.DIMENSION_TYPE, MycelialDimensionBootstrap::bootstrapDimensionTypes)
            .add(Registries.NOISE_SETTINGS, MycelialDimensionBootstrap::bootstrapNoiseSettings)
            .add(Registries.CONFIGURED_FEATURE, MycelialFeatureBootstrap::bootstrapConfiguredFeatures)
            .add(Registries.PLACED_FEATURE, MycelialFeatureBootstrap::bootstrapPlacedFeatures)
            .add(Registries.BIOME, MycelialBiomeBootstrap::bootstrapBiomes)
            .add(Registries.LEVEL_STEM, MycelialDimensionBootstrap::bootstrapLevelStems)
            .add(Registries.PROCESSOR_LIST, MycelialStructureBootstrap::bootstrapProcessorLists)
            .add(Registries.TEMPLATE_POOL, MycelialStructureBootstrap::bootstrapTemplatePools)
            .add(Registries.STRUCTURE, MycelialStructureBootstrap::bootstrapStructures)
            .add(Registries.STRUCTURE_SET, MycelialStructureBootstrap::bootstrapStructureSets)
            .add(Registries.DAMAGE_TYPE, MycelialDamageTypeBootstrap::bootstrap)
            .add(Registries.ENCHANTMENT, MycelialEnchantmentBootstrap::bootstrap)
            .add(NeoForgeRegistries.Keys.BIOME_MODIFIERS, MycelialBiomeBootstrap::bootstrapBiomeModifiers);

    public MycelialDatapackProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, BUILDER, Set.of(MycelialMind.MODID));
    }
}
