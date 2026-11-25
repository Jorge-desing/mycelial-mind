package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import com.alex.mycelialmind.world.loot.MycelialTraceLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.AdvancementProvider;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.neoforge.registries.datamaps.builtin.Compostable;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;
import net.neoforged.neoforge.registries.datamaps.builtin.RaidHeroGift;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Registers NeoForge DataGen entrypoint and emits server datapack entries (Worldgen, Dimensions,
 * Structures, Biome Modifiers, Tags, Recipes, Progression Advancements, Loot Tables, NeoForge
 * Data Maps, and Global Loot Modifiers) as well as client assets (Blockstates, Boilerplate Item
 * Models, and Blocks Atlas). Python studios remain the source of truth for lang, particles, and sounds.
 */
@EventBusSubscriber(modid = MycelialMind.MODID)
public final class ModDataGenerators {
    private ModDataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        generator.addProvider(true, PackMetadataGenerator.forFeaturePack(
                output, Component.literal("Mycelial Mind generated resources")));

        registerServerProviders(generator, output, lookupProvider, existingFileHelper, event.includeServer());
        registerClientProviders(generator, output, lookupProvider, existingFileHelper, event.includeClient());
    }

    private static void registerServerProviders(
            DataGenerator generator,
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper,
            boolean includeServer
    ) {
        MycelialDatapackProvider datapackProvider = generator.addProvider(
                includeServer, new MycelialDatapackProvider(output, lookupProvider));
        CompletableFuture<HolderLookup.Provider> datapackLookup = datapackProvider.getRegistryProvider();

        BlockTagsProvider blockTagsProvider = generator.addProvider(
                includeServer, new MycelialBlockTagsProvider(output, lookupProvider, existingFileHelper));
        generator.addProvider(includeServer, new MycelialItemTagsProvider(
                output, lookupProvider, blockTagsProvider.contentsGetter(), existingFileHelper));
        generator.addProvider(includeServer, new MycelialEntityTypeTagsProvider(
                output, lookupProvider, existingFileHelper));
        generator.addProvider(includeServer, new MycelialBiomeTagsProvider(
                output, datapackLookup, existingFileHelper));
        generator.addProvider(includeServer, new MycelialStructureTagsProvider(
                output, datapackLookup, existingFileHelper));

        generator.addProvider(includeServer, new MycelialRecipeProvider(output, lookupProvider));
        generator.addProvider(includeServer, new AdvancementProvider(
                output, lookupProvider, existingFileHelper, List.of(new MycelialAdvancementProvider())));

        generator.addProvider(includeServer, new LootTableProvider(
                output,
                Set.of(),
                List.of(
                        new LootTableProvider.SubProviderEntry(
                                MycelialBlockLootSubProvider::new, LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(
                                unused -> MycelialBlockLootSubProvider.reclaimedBloomSubProvider(),
                                LootContextParamSets.BLOCK),
                        new LootTableProvider.SubProviderEntry(
                                MycelialEntityLootSubProvider::new, LootContextParamSets.ENTITY),
                        new LootTableProvider.SubProviderEntry(
                                MycelialChestLootSubProvider::new, LootContextParamSets.CHEST),
                        new LootTableProvider.SubProviderEntry(
                                MycelialGameplayLootSubProvider::new, LootContextParamSets.GIFT)
                ),
                lookupProvider
        ));

        generator.addProvider(includeServer, new MycelialDataMapProvider(output, lookupProvider));
        generator.addProvider(includeServer, new MycelialGlobalLootModifierProvider(output, lookupProvider));
    }

    private static void registerClientProviders(
            DataGenerator generator,
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper,
            boolean includeClient
    ) {
        generator.addProvider(includeClient, new MycelialSpriteSourceProvider(
                output, lookupProvider, existingFileHelper));
        generator.addProvider(includeClient, new MycelialBlockStateProvider(output, existingFileHelper));
        generator.addProvider(includeClient, new MycelialItemModelProvider(output, existingFileHelper));
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
                    .add(ModItems.HYPHAL_VEIN.getId(), new Compostable(0.30F), false)
                    .add(ModBlocks.ANTIBODY_CYST.getId(), new Compostable(0.50F), false)
                    .add(ModBlocks.ROOT_SAP_MOSS.getId(), new Compostable(0.65F), false)
                    .add(ModBlocks.ROOT_CAP_SHELF.getId(), new Compostable(0.50F), false)
                    .add(ModItems.CANOPY_MEMBRANE.getId(), new Compostable(0.65F), false)
                    .add(ModBlocks.RECLAIMED_BLOOM.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_MELEE.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_PROJECTILE.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_FIRE.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_EXPLOSION.getId(), new Compostable(0.65F), false)
                    .add(ModItems.RECLAIMED_BLOOM_MAGIC.getId(), new Compostable(0.65F), false);

            builder(NeoForgeDataMaps.FURNACE_FUELS)
                    .add(ModItems.MYCELIAL_BIOMASS.getId(), new FurnaceFuel(400), false)
                    .add(ModBlocks.LUMINESCENT_AMBER_VEIN.getId(), new FurnaceFuel(1200), false)
                    .add(ModItems.VOLATILE_ENGRAM.getId(), new FurnaceFuel(1600), false)
                    .add(ModItems.ROOT_HEART_RESIN.getId(), new FurnaceFuel(1600), false);

            builder(NeoForgeDataMaps.RAID_HERO_GIFTS)
                    .add(ResourceLocation.withDefaultNamespace("cleric"),
                            new RaidHeroGift(MycelialGameplayLootSubProvider.CLERIC_MYCELIAL_GIFT), false);
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
