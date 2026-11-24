package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.world.loot.MycelialTraceLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.RaidHeroGift;

import java.util.concurrent.CompletableFuture;

/**
 * Registers NeoForge DataGen entrypoint and emits NeoForge Data Maps and Global Loot Modifiers.
 */
@EventBusSubscriber(modid = MycelialMind.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        generator.addProvider(true, PackMetadataGenerator.forFeaturePack(
                output, Component.literal("Mycelial Mind generated resources")));
        generator.addProvider(event.includeServer(), new MycelialDataMapProvider(output, lookupProvider));
        generator.addProvider(event.includeServer(), new MycelialGlobalLootModifierProvider(output, lookupProvider));
    }

    private static final class MycelialDataMapProvider extends DataMapProvider {
        private MycelialDataMapProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> lookupProvider) {
            super(packOutput, lookupProvider);
        }

        @Override
        protected void gather(HolderLookup.Provider provider) {
            builder(NeoForgeDataMaps.COMPOSTABLES)
                    .add(ModItems.MYCELIAL_BIOMASS.getId(), new Compostable(0.65F), false)
                    .add(ModItems.LATENT_SPORE.getId(), new Compostable(0.50F), false)
                    .add(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.getId(), new Compostable(0.65F), false)
                    .add(ModBlocks.SPORE_BULB.getId(), new Compostable(0.65F), false)
                    .add(ModBlocks.MYCELIAL_TENDRILS.getId(), new Compostable(0.50F), false)
                    .add(ModBlocks.HYPHAL_VEIN.getId(), new Compostable(0.30F), false)
                    .add(ModBlocks.ANTIBODY_CYST.getId(), new Compostable(0.50F), false)
                    .add(ModBlocks.RECLAIMED_BLOOM.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_MELEE.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_PROJECTILE.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_FIRE.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_EXPLOSION.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_MAGIC.getId(), new Compostable(0.65F), false);

            builder(NeoForgeDataMaps.FURNACE_FUELS)
                    .add(ModItems.MYCELIAL_BIOMASS.getId(), new FurnaceFuel(400), false)
                    .add(ModBlocks.LUMINESCENT_AMBER_VEIN.getId(), new FurnaceFuel(1200), false)
                    .add(ModItems.VOLATILE_ENGRAM.getId(), new FurnaceFuel(1600), false);

            ResourceKey<LootTable> clericGift = ResourceKey.create(
                    Registries.LOOT_TABLE,
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "gameplay/hero_of_the_village/cleric_mycelial_gift")
            );
            builder(NeoForgeDataMaps.RAID_HERO_GIFTS)
                    .add(ResourceLocation.withDefaultNamespace("cleric"), new RaidHeroGift(clericGift), false);
        }
    }

    private static final class MycelialGlobalLootModifierProvider extends GlobalLootModifierProvider {
        private MycelialGlobalLootModifierProvider(
                PackOutput output,
                CompletableFuture<HolderLookup.Provider> registries
        ) {
            super(output, registries, MycelialMind.MODID);
        }

        @Override
        protected void start() {
            add("mineshaft_underground_traces", new MycelialTraceLootModifier(
                    new LootItemCondition[]{
                            LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/abandoned_mineshaft")).build()
                    },
                    "underground_traces",
                    0.20F
            ));
            add("dungeon_underground_traces", new MycelialTraceLootModifier(
                    new LootItemCondition[]{
                            LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/simple_dungeon")).build()
                    },
                    "underground_traces",
                    0.20F
            ));
            add("ancient_city_deep_traces", new MycelialTraceLootModifier(
                    new LootItemCondition[]{
                            LootTableIdCondition.builder(ResourceLocation.withDefaultNamespace("chests/ancient_city")).build()
                    },
                    "deep_traces",
                    0.25F
            ));
        }
    }
}
