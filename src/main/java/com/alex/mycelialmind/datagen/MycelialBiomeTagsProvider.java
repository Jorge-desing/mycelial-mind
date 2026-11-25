package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.world.ModWorldgenKeys;
import com.alex.mycelialmind.world.TerrainPlacementPolicy;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates biome tags for Overworld placement exclusions and Deep Mycelium structure sets.
 */
public final class MycelialBiomeTagsProvider extends BiomeTagsProvider {
    public MycelialBiomeTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> datapackLookup,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, datapackLookup, MycelialMind.MODID, existingFileHelper);
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

        tag(ModWorldgenKeys.HAS_MATRIARCH_CITADEL)
                .add(ModWorldgenKeys.HIVE_HEART_SANCTUM);

        tag(ModWorldgenKeys.HAS_TWISTED_CITADEL)
                .add(
                        ModWorldgenKeys.DEEP_MYCELIUM_CAVERNS,
                        ModWorldgenKeys.HIVE_HEART_SANCTUM,
                        ModWorldgenKeys.LATENT_SPORE_FOREST
                );
    }
}
