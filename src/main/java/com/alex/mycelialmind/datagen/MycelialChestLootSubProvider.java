package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.hive.FieldEngramId;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModEnchantments;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.function.BiConsumer;

/**
 * Generates all 11 structure, vault, and surface-hive chest loot tables.
 */
public final class MycelialChestLootSubProvider implements LootTableSubProvider {
    private final HolderLookup.Provider registries;

    public MycelialChestLootSubProvider(HolderLookup.Provider registries) {
        this.registries = registries;
    }

    private LootPool.Builder treasureBookPool(float chance) {
        var enchLookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(Items.BOOK)
                        .apply(EnchantRandomlyFunction.randomEnchantment()
                                .withEnchantment(enchLookup.getOrThrow(ModEnchantments.ANTIMYCOTIC))))
                .add(LootItem.lootTableItem(Items.BOOK)
                        .apply(EnchantRandomlyFunction.randomEnchantment()
                                .withEnchantment(enchLookup.getOrThrow(ModEnchantments.BIO_WARD))))
                .add(LootItem.lootTableItem(Items.BOOK)
                        .apply(EnchantRandomlyFunction.randomEnchantment()
                                .withEnchantment(enchLookup.getOrThrow(ModEnchantments.SYNAPTIC_HARVEST))));
    }

    private static ResourceKey<LootTable> chest(String name) {
        return ResourceKey.create(
                Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "chests/" + name)
        );
    }

    private static LootPoolSingletonContainer.Builder<?> entry(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolSingletonContainer.Builder<?> entry(ItemLike item, int weight, float min, float max) {
        return LootItem.lootTableItem(item)
                .setWeight(weight)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }

    private static LootPoolSingletonContainer.Builder<?> traitSample(int weight, AdaptationTrait trait) {
        return entry(ModItems.GENETIC_SAMPLE.get(), weight)
                .apply(SetComponentsFunction.setComponent(ModDataComponents.TRAIT_ID.get(), trait.id()));
    }

    private static LootPoolSingletonContainer.Builder<?> fieldEngram(int weight, FieldEngramId engram) {
        return entry(ModItems.FIELD_ENGRAM.get(), weight)
                .apply(SetComponentsFunction.setComponent(ModDataComponents.ENGRAM_ID.get(), engram.id()));
    }

    @Override
    public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        generateCitadelTables(output);
        generateSanctumTables(output);
        generateDimensionalBreachTables(output);
        generateOutbreakAndSinkholeTables(output);
        generateSurfaceHiveTables(output);
    }

    private void generateCitadelTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(chest("citadel_amber_chapel"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 35, 4.0F, 7.0F))
                        .add(entry(ModBlocks.LUMINESCENT_AMBER_VEIN.get(), 35, 2.0F, 4.0F))
                        .add(entry(Items.AMETHYST_SHARD, 30, 3.0F, 6.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 35, 1.0F, 3.0F))
                        .add(entry(ModItems.QUORUM_SEAL.get(), 20))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 25))
                        .add(traitSample(20, AdaptationTrait.FIRE_RESISTANT)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 40, 1.0F, 2.0F))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 35))
                        .add(entry(ModItems.SYNAPTIC_GRAFT.get(), 25)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(entry(ModItems.CHIMERA_OVUM.get(), 35))
                        .add(entry(Items.ECHO_SHARD, 35))
                        .add(entry(Items.DIAMOND, 30, 1.0F, 3.0F))));

        output.accept(chest("citadel_crimson_chapel"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 38, 4.0F, 7.0F))
                        .add(entry(ModItems.LATENT_SPORE.get(), 32, 3.0F, 6.0F))
                        .add(entry(Items.CRYING_OBSIDIAN, 30, 2.0F, 4.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.SCHISM_INJECTOR.get(), 30))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 25, 2.0F, 3.0F))
                        .add(entry(ModItems.FALSE_ENGRAM_INJECTOR.get(), 22))
                        .add(entry(ModItems.VOLATILE_ENGRAM.get(), 23)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 40, 1.0F, 2.0F))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 35))
                        .add(entry(ModItems.CHITIN_CHESTPLATE.get(), 25)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(entry(ModItems.SYMBIOTIC_BLADE.get(), 35))
                        .add(entry(ModItems.ENZYME_LANCE.get(), 35))
                        .add(entry(ModItems.PURIFIER_DRONE_CORE.get(), 30))));

        output.accept(chest("citadel_royal_crypt"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 35, 2.0F, 3.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 40, 4.0F, 8.0F))
                        .add(entry(Items.DIAMOND, 25, 2.0F, 4.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 28, 1.0F, 2.0F))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 28, 2.0F, 3.0F))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 24, 1.0F, 2.0F))
                        .add(entry(Items.GOLDEN_APPLE, 20)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.QUORUM_SEAL.get(), 30))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 28))
                        .add(entry(ModItems.SYNAPTIC_GRAFT.get(), 22))
                        .add(entry(ModItems.FALSE_ENGRAM_INJECTOR.get(), 20)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.OVERMIND_ESSENCE.get(), 40))
                        .add(entry(ModItems.TIDE_BEACON.get(), 25))
                        .add(entry(ModItems.CHIMERA_OVUM.get(), 20))
                        .add(entry(Items.ECHO_SHARD, 15, 2.0F, 4.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.55F))
                        .add(fieldEngram(50, FieldEngramId.HOMEOSTASIS_TRAUMA))
                        .add(fieldEngram(50, FieldEngramId.PRIMORDIAL_LINEAGE)))
                .withPool(treasureBookPool(0.35F)));
    }

    private void generateSanctumTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(chest("sanctum_forbidden_lab"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.MYCELIAL_BIOMASS.get(), 35, 4.0F, 8.0F))
                        .add(entry(ModItems.LATENT_SPORE.get(), 30, 2.0F, 5.0F))
                        .add(entry(ModBlocks.LUMINESCENT_AMBER_VEIN.get(), 20, 1.0F, 3.0F))
                        .add(entry(Items.EXPERIENCE_BOTTLE, 15, 3.0F, 6.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 28, 1.0F, 2.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 26))
                        .add(entry(ModItems.VOLATILE_ENGRAM.get(), 26))
                        .add(traitSample(20, AdaptationTrait.MAGIC_CARAPACE)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 35))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 30))
                        .add(entry(ModItems.FALSE_ENGRAM_INJECTOR.get(), 20))
                        .add(entry(ModItems.SCHISM_INJECTOR.get(), 15)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(entry(ModItems.SYNAPTIC_GRAFT.get(), 32))
                        .add(entry(ModItems.QUORUM_SEAL.get(), 24))
                        .add(entry(ModItems.CHIMERA_OVUM.get(), 22))
                        .add(entry(Items.ECHO_SHARD, 22)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.45F))
                        .add(fieldEngram(55, FieldEngramId.SYNAPTIC_BLOOM))
                        .add(fieldEngram(45, FieldEngramId.HYPHAL_MEMORY))));

        output.accept(chest("sanctum_ossuary_crypt"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 40, 3.0F, 6.0F))
                        .add(entry(Items.BONE_BLOCK, 28, 2.0F, 5.0F))
                        .add(entry(Items.CRYING_OBSIDIAN, 18, 1.0F, 3.0F))
                        .add(entry(Items.AMETHYST_SHARD, 14, 2.0F, 4.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 30))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 28, 1.0F, 3.0F))
                        .add(entry(ModItems.SCHISM_INJECTOR.get(), 22))
                        .add(traitSample(20, AdaptationTrait.BLAST_SHIELDED)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 35))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 30))
                        .add(entry(ModItems.CHITIN_HELMET.get(), 18))
                        .add(entry(ModItems.CHITIN_CHESTPLATE.get(), 17)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.30F))
                        .add(entry(ModItems.SYMBIOTIC_BLADE.get(), 32))
                        .add(entry(ModItems.ENZYME_LANCE.get(), 32))
                        .add(entry(ModItems.CHITIN_LEGGINGS.get(), 18))
                        .add(entry(Items.DIAMOND, 18, 1.0F, 3.0F))));

        output.accept(chest("sanctum_spore_vault"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.LATENT_SPORE.get(), 35, 3.0F, 6.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 35, 3.0F, 5.0F))
                        .add(entry(ModItems.MYCELIAL_BIOMASS.get(), 30, 5.0F, 9.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 28, 2.0F, 3.0F))
                        .add(entry(ModItems.SPORE_ANTIDOTE.get(), 22, 1.0F, 3.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 20))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 15, 1.0F, 2.0F))
                        .add(entry(Items.PHANTOM_MEMBRANE, 15, 1.0F, 3.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 40))
                        .add(entry(ModItems.NEURAL_ECHO_DECOY.get(), 30))
                        .add(entry(Items.ENDER_PEARL, 30, 1.0F, 3.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.28F))
                        .add(entry(ModItems.PURIFIER_DRONE_CORE.get(), 28))
                        .add(entry(ModItems.NEUTRALIZING_SPORE_LAUNCHER.get(), 24))
                        .add(entry(ModItems.CHITIN_BOOTS.get(), 24))
                        .add(entry(Items.GOLDEN_APPLE, 24))));

        output.accept(chest("sanctum_quarantine_cache"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.MYCELIAL_BIOMASS.get(), 36, 4.0F, 8.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 34, 3.0F, 5.0F))
                        .add(entry(ModItems.LATENT_SPORE.get(), 30, 2.0F, 5.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 32))
                        .add(entry(ModItems.SPORE_ANTIDOTE.get(), 26, 2.0F, 4.0F))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 24, 2.0F, 3.0F))
                        .add(traitSample(18, AdaptationTrait.BLAST_SHIELDED)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 35))
                        .add(entry(ModItems.NEURAL_ECHO_DECOY.get(), 35))
                        .add(entry(ModItems.VOLATILE_ENGRAM.get(), 30)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.40F))
                        .add(fieldEngram(100, FieldEngramId.HOMEOSTASIS_TRAUMA)))
                .withPool(treasureBookPool(0.25F)));
    }

    private void generateDimensionalBreachTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(chest("nether_breach_reliquary"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 34, 3.0F, 6.0F))
                        .add(entry(Items.CRYING_OBSIDIAN, 26, 2.0F, 4.0F))
                        .add(entry(Items.MAGMA_CREAM, 20, 2.0F, 4.0F))
                        .add(entry(Items.BLAZE_POWDER, 20, 2.0F, 5.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 28))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 26, 2.0F, 4.0F))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 22, 1.0F, 2.0F))
                        .add(traitSample(24, AdaptationTrait.FIRE_RESISTANT)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 40, 1.0F, 2.0F))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 35))
                        .add(entry(Items.GOLD_INGOT, 25, 4.0F, 8.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(entry(ModItems.ENZYME_LANCE.get(), 35))
                        .add(entry(ModItems.SYMBIOTIC_BLADE.get(), 35))
                        .add(entry(ModItems.CHITIN_CHESTPLATE.get(), 30)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.45F))
                        .add(fieldEngram(100, FieldEngramId.TIDE_PRESSURE)))
                .withPool(treasureBookPool(0.30F)));

        output.accept(chest("end_spire_vault"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 32, 3.0F, 6.0F))
                        .add(entry(Items.AMETHYST_SHARD, 26, 3.0F, 6.0F))
                        .add(entry(Items.ENDER_PEARL, 24, 2.0F, 4.0F))
                        .add(entry(Items.CHORUS_FRUIT, 18, 2.0F, 5.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(traitSample(28, AdaptationTrait.MAGIC_CARAPACE))
                        .add(traitSample(24, AdaptationTrait.PROJECTILE_DEFLECT))
                        .add(entry(ModItems.FALSE_ENGRAM_INJECTOR.get(), 24))
                        .add(entry(ModItems.QUORUM_SEAL.get(), 24)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 38))
                        .add(entry(ModItems.SYNAPTIC_GRAFT.get(), 32))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 30, 1.0F, 2.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(entry(ModItems.CHIMERA_OVUM.get(), 36))
                        .add(entry(Items.ECHO_SHARD, 34, 1.0F, 2.0F))
                        .add(entry(Items.SHULKER_SHELL, 30, 1.0F, 2.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.45F))
                        .add(fieldEngram(100, FieldEngramId.MIMIC_DECEPTION)))
                .withPool(treasureBookPool(0.30F)));
    }

    private void generateOutbreakAndSinkholeTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(chest("outbreak_sealed_vault"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.MYCELIAL_BIOMASS.get(), 35, 3.0F, 6.0F))
                        .add(entry(ModItems.LATENT_SPORE.get(), 30, 2.0F, 4.0F))
                        .add(entry(ModBlocks.LUMINESCENT_AMBER_VEIN.get(), 20, 1.0F, 2.0F))
                        .add(entry(Items.AMETHYST_SHARD, 15, 1.0F, 3.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 32, 2.0F, 4.0F))
                        .add(entry(ModItems.SPORE_ANTIDOTE.get(), 26, 1.0F, 2.0F))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 22, 1.0F, 2.0F))
                        .add(entry(Items.EXPERIENCE_BOTTLE, 20, 2.0F, 4.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 25))
                        .add(entry(ModItems.CHITIN_HELMET.get(), 18))
                        .add(entry(ModItems.CHITIN_BOOTS.get(), 18))
                        .add(entry(ModItems.ENZYME_LANCE.get(), 14))
                        .add(entry(Items.IRON_INGOT, 15, 2.0F, 4.0F))
                        .add(entry(Items.GOLDEN_APPLE, 10)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.45F))
                        .add(fieldEngram(100, FieldEngramId.OUTBREAK_SIGNAL)))
                .withPool(treasureBookPool(0.20F)));

        output.accept(chest("sinkhole_root_vault"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 4.0F))
                        .add(entry(ModItems.ROOT_SAP_VIAL.get(), 35, 1.0F, 3.0F))
                        .add(entry(ModItems.CANOPY_MEMBRANE.get(), 28, 2.0F, 4.0F))
                        .add(entry(ModItems.ROOT_HEART_RESIN.get(), 22, 1.0F, 2.0F))
                        .add(entry(ModBlocks.LUMINESCENT_AMBER_VEIN.get(), 15, 1.0F, 3.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 30, 2.0F, 4.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 25)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 25)
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F))))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 20, 1.0F, 2.0F))));
    }

    private static void generateSurfaceHiveTables(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
        output.accept(chest("surface_hive_cache"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.MYCELIAL_BIOMASS.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(6.0F, 10.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.CHITIN_PLATE.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(3.0F, 6.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.LATENT_SPORE.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 5.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.SYNAPTIC_CORE.get())
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.SPORE_ANTIDOTE.get(), 30, 1.0F, 2.0F))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 25, 1.0F, 2.0F))
                        .add(entry(Items.AMETHYST_SHARD, 22, 2.0F, 5.0F))
                        .add(entry(Items.ENDER_PEARL, 13, 1.0F, 2.0F))
                        .add(entry(Items.GOLDEN_APPLE, 10)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.45F))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 26))
                        .add(entry(ModItems.VOLATILE_ENGRAM.get(), 22))
                        .add(entry(ModItems.NEURAL_ECHO_DECOY.get(), 20))
                        .add(entry(ModItems.CHITIN_HELMET.get(), 12))
                        .add(entry(ModItems.CHITIN_BOOTS.get(), 12))
                        .add(entry(Items.DIAMOND, 8, 1.0F, 2.0F))));

        output.accept(chest("surface_hive_crypt"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 35, 3.0F, 6.0F))
                        .add(entry(ModItems.LATENT_SPORE.get(), 30, 2.0F, 4.0F))
                        .add(entry(Items.BONE_BLOCK, 20, 2.0F, 4.0F))
                        .add(entry(Items.AMETHYST_SHARD, 15, 2.0F, 5.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 30, 1.0F, 2.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 25))
                        .add(entry(ModItems.VOLATILE_ENGRAM.get(), 25))
                        .add(traitSample(20, AdaptationTrait.MELEE_THORNS)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 45))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 35))
                        .add(entry(Items.ECHO_SHARD, 20)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.30F))
                        .add(entry(ModItems.SYNAPTIC_GRAFT.get(), 30))
                        .add(entry(ModItems.FALSE_ENGRAM_INJECTOR.get(), 25))
                        .add(entry(ModItems.CHITIN_CHESTPLATE.get(), 25))
                        .add(entry(Items.GOLDEN_APPLE, 20))));

        output.accept(chest("surface_hive_gallery"), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(2.0F, 3.0F))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 35, 2.0F, 4.0F))
                        .add(entry(ModItems.MYCELIAL_BIOMASS.get(), 30, 3.0F, 6.0F))
                        .add(entry(ModItems.LATENT_SPORE.get(), 25, 2.0F, 4.0F))
                        .add(entry(Items.EXPERIENCE_BOTTLE, 10, 2.0F, 5.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(UniformGenerator.between(1.0F, 2.0F))
                        .add(entry(ModItems.SPORE_ANTIDOTE.get(), 30, 1.0F, 2.0F))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 28, 1.0F, 2.0F))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 22))
                        .add(entry(Items.PHANTOM_MEMBRANE, 20, 1.0F, 3.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.55F))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 30))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 30))
                        .add(entry(ModItems.NEURAL_ECHO_DECOY.get(), 20))
                        .add(traitSample(20, AdaptationTrait.PROJECTILE_DEFLECT)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.22F))
                        .add(entry(ModItems.CHITIN_LEGGINGS.get(), 30))
                        .add(entry(ModItems.PURIFIER_DRONE_CORE.get(), 25))
                        .add(entry(ModItems.ENZYME_LANCE.get(), 20))
                        .add(entry(Items.GOLDEN_APPLE, 25))));
    }
}
