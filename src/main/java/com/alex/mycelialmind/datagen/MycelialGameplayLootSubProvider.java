package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.function.BiConsumer;

/**
 * Generates Hero of the Village and other gameplay gift loot tables referenced by Data Maps.
 */
public final class MycelialGameplayLootSubProvider implements LootTableSubProvider {
    private static final ResourceKey<LootTable> CLERIC_MYCELIAL_GIFT = ResourceKey.create(
            Registries.LOOT_TABLE,
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "gameplay/hero_of_the_village/cleric_mycelial_gift")
    );

    public MycelialGameplayLootSubProvider(HolderLookup.Provider registries) {
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(CLERIC_MYCELIAL_GIFT, LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(Items.REDSTONE).setWeight(35))
                        .add(LootItem.lootTableItem(Items.LAPIS_LAZULI).setWeight(25))
                        .add(LootItem.lootTableItem(ModItems.SPORE_ANTIDOTE.get()).setWeight(22))
                        .add(LootItem.lootTableItem(ModItems.LATENT_SPORE.get()).setWeight(18))));
    }
}
