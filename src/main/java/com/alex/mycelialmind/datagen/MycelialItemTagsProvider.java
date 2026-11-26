package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates item tags (armor slots, swords, enchantability, and Curios belt slot).
 */
public final class MycelialItemTagsProvider extends ItemTagsProvider {
    private static final TagKey<Item> CURIOS_BELT =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "belt"));
    private static final TagKey<Item> CURIOS_HEAD =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "head"));
    private static final TagKey<Item> CURIOS_CHARM =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("curios", "charm"));
    private static final TagKey<Item> COMMON_DRINKS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "drinks"));
    private static final TagKey<Item> COMMON_RANGED_WEAPONS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "tools/ranged_weapon"));
    private static final TagKey<Item> SERENE_SPRING_CROPS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("sereneseasons", "spring_crops"));
    private static final TagKey<Item> SERENE_SUMMER_CROPS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("sereneseasons", "summer_crops"));
    private static final TagKey<Item> SERENE_AUTUMN_CROPS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("sereneseasons", "autumn_crops"));
    private static final TagKey<Item> SERENE_WINTER_CROPS =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("sereneseasons", "winter_crops"));
    private static final TagKey<Item> TAN_THIRST_2 =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("toughasnails", "thirst/2_thirst_drinks"));
    private static final TagKey<Item> TAN_THIRST_4 =
            ItemTags.create(ResourceLocation.fromNamespaceAndPath("toughasnails", "thirst/4_thirst_drinks"));

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
        tag(CURIOS_HEAD).add(ModItems.OVERMIND_CROWN.get());
        tag(CURIOS_CHARM).add(ModItems.QUORUM_SEAL.get());

        tag(Tags.Items.MUSHROOMS).add(
                ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.asItem(),
                ModBlocks.SPORE_BULB.asItem()
        );

        tag(Tags.Items.STONES).add(ModBlocks.MYCELIAL_STONE.asItem());

        tag(Tags.Items.MELEE_WEAPON_TOOLS).add(
                ModItems.SYMBIOTIC_BLADE.get(),
                ModItems.ENZYME_LANCE.get()
        );

        tag(COMMON_RANGED_WEAPONS).add(ModItems.NEUTRALIZING_SPORE_LAUNCHER.get());

        tag(Tags.Items.ARMORS).add(
                ModItems.CHITIN_HELMET.get(),
                ModItems.CHITIN_CHESTPLATE.get(),
                ModItems.CHITIN_LEGGINGS.get(),
                ModItems.CHITIN_BOOTS.get()
        );

        tag(COMMON_DRINKS).add(
                ModItems.SPORE_ANTIDOTE.get(),
                ModItems.CONCENTRATED_ANTIDOTE.get(),
                ModItems.RESONANT_SAP_ELIXIR.get(),
                ModItems.ROOT_SAP_VIAL.get()
        );

        tag(TAN_THIRST_2).add(
                ModItems.SPORE_ANTIDOTE.get(),
                ModItems.ROOT_SAP_VIAL.get()
        );
        tag(TAN_THIRST_4).add(
                ModItems.CONCENTRATED_ANTIDOTE.get(),
                ModItems.RESONANT_SAP_ELIXIR.get()
        );

        Item[] fungalFloraItems = {
                ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.asItem(),
                ModBlocks.SPORE_BULB.asItem(),
                ModBlocks.MYCELIAL_TENDRILS.asItem(),
                ModBlocks.ROOT_SAP_MOSS.asItem()
        };
        tag(SERENE_SPRING_CROPS).add(fungalFloraItems);
        tag(SERENE_SUMMER_CROPS).add(fungalFloraItems);
        tag(SERENE_AUTUMN_CROPS).add(fungalFloraItems);
        tag(SERENE_WINTER_CROPS).add(fungalFloraItems);
    }
}
