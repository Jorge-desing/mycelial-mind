package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates structure tags used by the Bio-Scanner to locate Hive Sanctums and Citadels.
 */
public final class MycelialStructureTagsProvider extends TagsProvider<Structure> {
    private static final TagKey<Structure> SANCTUMS = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "sanctums")
    );

    public MycelialStructureTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, Registries.STRUCTURE, lookupProvider, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(SANCTUMS)
                .addOptional(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "hive_sanctum"))
                .addOptional(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "matriarch_citadel"));
    }
}
