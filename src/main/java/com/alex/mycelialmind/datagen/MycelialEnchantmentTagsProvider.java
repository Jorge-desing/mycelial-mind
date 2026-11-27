package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModEnchantments;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EnchantmentTagsProvider;
import net.minecraft.tags.EnchantmentTags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates {@link EnchantmentTags} classifying Mycelial Mind enchantments as treasure/tradeable/loot
 * (excluded from the enchanting table) and marking {@code antimycotic} under {@code DAMAGE_EXCLUSIVE}.
 */
public final class MycelialEnchantmentTagsProvider extends EnchantmentTagsProvider {
    public MycelialEnchantmentTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(EnchantmentTags.TREASURE).add(
                ModEnchantments.ANTIMYCOTIC,
                ModEnchantments.BIO_WARD,
                ModEnchantments.SYNAPTIC_HARVEST
        );
        tag(EnchantmentTags.TRADEABLE).add(
                ModEnchantments.ANTIMYCOTIC,
                ModEnchantments.BIO_WARD,
                ModEnchantments.SYNAPTIC_HARVEST
        );
        tag(EnchantmentTags.ON_RANDOM_LOOT).add(
                ModEnchantments.ANTIMYCOTIC,
                ModEnchantments.BIO_WARD,
                ModEnchantments.SYNAPTIC_HARVEST
        );
        tag(EnchantmentTags.DAMAGE_EXCLUSIVE).add(ModEnchantments.ANTIMYCOTIC);
    }
}
