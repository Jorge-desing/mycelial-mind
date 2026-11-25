package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.world.TerrainPlacementPolicy;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates biome tags for Overworld placement exclusions and Deep Mycelium structure sets.
 */
public final class MycelialBiomeTagsProvider extends BiomeTagsProvider {
    private static final TagKey<Biome> HAS_MATRIARCH_CITADEL = biomeTag("has_structure/matriarch_citadel");
    private static final TagKey<Biome> HAS_TWISTED_CITADEL = biomeTag("has_structure/twisted_citadel");

    public MycelialBiomeTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, MycelialMind.MODID, existingFileHelper);
    }

    private static TagKey<Biome> biomeTag(String path) {
        return TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, path));
    }

    private static ResourceLocation modId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, path);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(TerrainPlacementPolicy.SURFACE_EXCLUDED_BIOMES)
                .addTag(BiomeTags.IS_OCEAN)
                .addTag(BiomeTags.IS_RIVER)
                .addTag(BiomeTags.IS_BEACH)
                .add(
                        Biomes.MUSHROOM_FIELDS,
                        Biomes.JAGGED_PEAKS,
                        Biomes.FROZEN_PEAKS,
                        Biomes.STONY_PEAKS,
                        Biomes.ICE_SPIKES
                );

        tag(HAS_MATRIARCH_CITADEL)
                .addOptional(modId("hive_heart_sanctum"));

        tag(HAS_TWISTED_CITADEL)
                .addOptional(modId("deep_mycelium_caverns"))
                .addOptional(modId("hive_heart_sanctum"))
                .addOptional(modId("latent_spore_forest"));
    }
}
