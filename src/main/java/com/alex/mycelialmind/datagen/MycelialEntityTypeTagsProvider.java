package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.tags.EntityTypeTags;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates entity type tags (NeoForge common bosses tag and fall damage immunity).
 */
public final class MycelialEntityTypeTagsProvider extends EntityTypeTagsProvider {
    public MycelialEntityTypeTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(Tags.EntityTypes.BOSSES).add(
                ModEntities.HIVE_CORE.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.SPORE_BEHEMOTH.get()
        );

        tag(EntityTypeTags.FALL_DAMAGE_IMMUNE).add(
                ModEntities.HIVE_CORE.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.SPORE_BEHEMOTH.get(),
                ModEntities.PURIFIER_DRONE.get()
        );
    }
}
