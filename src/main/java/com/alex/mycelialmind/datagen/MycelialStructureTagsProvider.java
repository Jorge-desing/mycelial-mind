package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.world.ModWorldgenKeys;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates structure tags used by the Bio-Scanner to locate Hive Sanctums and Citadels.
 */
public final class MycelialStructureTagsProvider extends TagsProvider<Structure> {
    public MycelialStructureTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> datapackLookup,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, Registries.STRUCTURE, datapackLookup, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ModWorldgenKeys.SANCTUMS)
                .add(
                        ModWorldgenKeys.HIVE_SANCTUM,
                        ModWorldgenKeys.MATRIARCH_CITADEL
                );
    }
}
