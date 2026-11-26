package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModDamageTypes;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates {@link DamageTypeTags} entries preserving armor bypass and magic classification for
 * Mycelial Mind's custom damage types.
 */
public final class MycelialDamageTypeTagsProvider extends DamageTypeTagsProvider {
    public MycelialDamageTypeTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var bypassesArmor = tag(DamageTypeTags.BYPASSES_ARMOR);
        var witchResistantTo = tag(DamageTypeTags.WITCH_RESISTANT_TO);
        for (ResourceKey<DamageType> key : ModDamageTypes.ALL) {
            bypassesArmor.add(key);
            witchResistantTo.add(key);
        }
    }
}
