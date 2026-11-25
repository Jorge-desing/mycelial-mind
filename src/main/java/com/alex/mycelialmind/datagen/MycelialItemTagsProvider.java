package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates item tags (armor slots, swords, enchantability, and Curios belt slot).
 */
public final class MycelialItemTagsProvider extends ItemTagsProvider {
    private static final TagKey<Item> CURIOS_BELT =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "belt"));

    public MycelialItemTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            CompletableFuture<TagLookup<Block>> blockTags,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, blockTags, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(ItemTags.HEAD_ARMOR).add(ModItems.CHITIN_HELMET.get());
        tag(ItemTags.CHEST_ARMOR).add(ModItems.CHITIN_CHESTPLATE.get());
        tag(ItemTags.LEG_ARMOR).add(ModItems.CHITIN_LEGGINGS.get());
        tag(ItemTags.FOOT_ARMOR).add(ModItems.CHITIN_BOOTS.get());

        tag(ItemTags.SWORDS).add(
                ModItems.SYMBIOTIC_BLADE.get(),
                ModItems.ENZYME_LANCE.get()
        );

        tag(ItemTags.DURABILITY_ENCHANTABLE).add(
                ModItems.SYMBIOTIC_BLADE.get(),
                ModItems.ENZYME_LANCE.get(),
                ModItems.CHITIN_HELMET.get(),
                ModItems.CHITIN_CHESTPLATE.get(),
                ModItems.CHITIN_LEGGINGS.get(),
                ModItems.CHITIN_BOOTS.get(),
                ModItems.NEUTRALIZING_SPORE_LAUNCHER.get(),
                ModItems.BIO_EXTRACTOR.get(),
                ModItems.SYNAPTIC_SUPPRESSOR.get(),
                ModItems.RIFT_CATALYST.get(),
                ModItems.SCHISM_INJECTOR.get(),
                ModItems.FALSE_ENGRAM_INJECTOR.get()
        );

        tag(CURIOS_BELT).add(ModItems.BIO_SCANNER.get());
    }
}
