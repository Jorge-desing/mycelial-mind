package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.EntityLootSubProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemKilledByPlayerCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceWithEnchantedBonusCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.List;
import java.util.stream.Stream;

/**
 * Generates entity loot tables for all 19 Mycelial Mind mobs, allies, and bosses with drop tables.
 */
public final class MycelialEntityLootSubProvider extends EntityLootSubProvider {
    public MycelialEntityLootSubProvider(HolderLookup.Provider registries) {
        super(FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected Stream<EntityType<?>> getKnownEntityTypes() {
        return Stream.of(
                ModEntities.ACID_SPITTER.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.BURROWER.get(),
                ModEntities.CHITIN_BRUTE.get(),
                ModEntities.COLONY_WARDEN.get(),
                ModEntities.GOLIATH_CARRIER.get(),
                ModEntities.HIVE_CORE.get(),
                ModEntities.LEECH_PARASITE.get(),
                ModEntities.MYCELIAL_CHIMERA.get(),
                ModEntities.MYCELIAL_STALKER.get(),
                ModEntities.PURIFIER_DRONE.get(),
                ModEntities.PYRO_SHROOM.get(),
                ModEntities.ROOT_KEEPER.get(),
                ModEntities.SPORE_ARCHITECT.get(),
                ModEntities.SPORE_BEHEMOTH.get(),
                ModEntities.SPORE_GLIDER.get(),
                ModEntities.SPORELING.get(),
                ModEntities.SYNAPSE_WEAVER.get(),
                ModEntities.SYNAPTIC_MIMIC.get()
        );
    }

    @Override
    public void generate() {
        Holder<Enchantment> looting = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING);

        generateEarlyMonsters(looting);
        generateSpecialistsAndGuardians(looting);
        generateAlliesAndSinkholeFauna(looting);
        generateBosses();
    }

    private void generateEarlyMonsters(Holder<Enchantment> looting) {
        add(ModEntities.SPORELING.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(lootingChance(looting, 0.85F, 0.90F, 0.05F))
                        .add(lootingItem(ModItems.MYCELIAL_BIOMASS.get(), 1.0F, 2.0F)))
                .withPool(playerLootingPool(looting, 0.22F, 0.28F, 0.06F,
                        entry(ModItems.LATENT_SPORE.get(), 65),
                        entry(Items.BROWN_MUSHROOM, 20),
                        entry(ModBlocks.SPORE_BULB.get(), 15))));

        add(ModEntities.CHITIN_BRUTE.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(lootingChance(looting, 0.65F, 0.72F, 0.07F))
                        .add(lootingItem(ModItems.CHITIN_PLATE.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.MYCELIAL_BIOMASS.get(), 1.0F, 2.0F)))
                .withPool(playerLootingPool(looting, 0.14F, 0.20F, 0.06F,
                        entry(Items.BONE, 50, 1.0F, 3.0F),
                        entry(ModItems.CHITIN_HELMET.get(), 20),
                        entry(ModItems.CHITIN_BOOTS.get(), 20),
                        entry(ModItems.SPORE_ANTIDOTE.get(), 10))));

        add(ModEntities.MYCELIAL_STALKER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.MYCELIAL_BIOMASS.get(), 1.0F, 3.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(lootingChance(looting, 0.45F, 0.52F, 0.07F))
                        .add(entry(ModItems.LATENT_SPORE.get(), 60).apply(lootingBonus()))
                        .add(entry(Items.ENDER_PEARL, 25))
                        .add(entry(ModItems.NEURAL_ECHO_DECOY.get(), 15))));

        add(ModEntities.ACID_SPITTER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(lootingChance(looting, 0.90F, 0.94F, 0.03F))
                        .add(lootingItem(ModItems.LATENT_SPORE.get(), 1.0F, 2.0F)))
                .withPool(playerLootingPool(looting, 0.25F, 0.32F, 0.07F,
                        entry(Items.SLIME_BALL, 45, 1.0F, 2.0F),
                        entry(ModItems.SPORE_ANTIDOTE.get(), 35),
                        entry(ModItems.PURIFIER_GRENADE.get(), 20))));

        add(ModEntities.BURROWER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(lootingChance(looting, 0.85F, 0.90F, 0.05F))
                        .add(lootingItem(ModItems.CHITIN_PLATE.get(), 1.0F, 2.0F)))
                .withPool(playerLootingPool(looting, 0.25F, 0.32F, 0.07F,
                        entry(ModItems.MYCELIAL_BIOMASS.get(), 45, 1.0F, 2.0F),
                        entry(Items.AMETHYST_SHARD, 35, 1.0F, 2.0F),
                        entry(ModBlocks.LUMINESCENT_AMBER_VEIN.get(), 20))));

        add(ModEntities.PYRO_SHROOM.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(lootingChance(looting, 0.95F, 0.98F, 0.02F))
                        .add(lootingItem(ModItems.LATENT_SPORE.get(), 1.0F, 3.0F)))
                .withPool(playerLootingPool(looting, 0.25F, 0.32F, 0.07F,
                        entry(Items.BLAZE_POWDER, 45),
                        entry(Items.MAGMA_CREAM, 30),
                        entry(ModBlocks.LUMINESCENT_AMBER_VEIN.get(), 25))));
    }

    private void generateSpecialistsAndGuardians(Holder<Enchantment> looting) {
        add(ModEntities.SYNAPSE_WEAVER.get(), LootTable.lootTable()
                .withPool(playerLootingPool(looting, 0.25F, 0.30F, 0.05F,
                        LootItem.lootTableItem(ModItems.SYNAPTIC_CORE.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.LATENT_SPORE.get(), 1.0F, 2.0F)))
                .withPool(playerLootingPool(looting, 0.22F, 0.28F, 0.06F,
                        entry(ModItems.VOLATILE_ENGRAM.get(), 40),
                        entry(ModItems.NEURAL_ECHO_CORE.get(), 25),
                        entry(ModItems.TRUCE_PHEROMONE.get(), 20),
                        entry(Items.AMETHYST_SHARD, 15, 1.0F, 3.0F))));

        add(ModEntities.GOLIATH_CARRIER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.CHITIN_PLATE.get(), 2.0F, 4.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.MYCELIAL_BIOMASS.get(), 2.0F, 4.0F)))
                .withPool(playerLootingPool(looting, 0.25F, 0.32F, 0.07F,
                        entry(ModItems.LATENT_SPORE.get(), 40, 1.0F, 3.0F),
                        entry(ModBlocks.SPORE_BULB.get(), 25),
                        entry(ModItems.CHITIN_CHESTPLATE.get(), 15),
                        entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 20))));

        add(ModEntities.LEECH_PARASITE.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(lootingChance(looting, 0.75F, 0.85F, 0.05F))
                        .add(lootingItem(ModItems.MYCELIAL_BIOMASS.get())))
                .withPool(playerLootingPool(looting, 0.18F, 0.24F, 0.06F,
                        entry(ModItems.VOLATILE_ENGRAM.get(), 45),
                        entry(ModItems.LATENT_SPORE.get(), 35),
                        entry(Items.FERMENTED_SPIDER_EYE, 20))));

        add(ModEntities.COLONY_WARDEN.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.SYNAPTIC_CORE.get(), 1.0F, 2.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.CHITIN_PLATE.get(), 2.0F, 4.0F)))
                .withPool(playerLootingPool(looting, 0.30F, 0.38F, 0.08F,
                        entry(ModItems.NEURAL_ECHO_CORE.get(), 35),
                        entry(ModItems.CHITIN_LEGGINGS.get(), 20),
                        entry(ModItems.CHITIN_HELMET.get(), 20),
                        entry(Items.ECHO_SHARD, 15),
                        entry(ModItems.PURIFIER_GRENADE.get(), 10))));

        add(ModEntities.SPORE_ARCHITECT.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.MYCELIAL_BIOMASS.get(), 1.0F, 2.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.LATENT_SPORE.get(), 1.0F, 2.0F)))
                .withPool(playerLootingPool(looting, 0.35F, 0.42F, 0.08F,
                        entry(ModItems.SYNAPTIC_CORE.get(), 45),
                        entry(ModBlocks.MYCELIAL_NODE.get(), 25),
                        entry(ModItems.VOLATILE_ENGRAM.get(), 30))));

        add(ModEntities.SYNAPTIC_MIMIC.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.NEURAL_ECHO_CORE.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.MYCELIAL_BIOMASS.get(), 1.0F, 3.0F)))
                .withPool(playerLootingPool(looting, 0.45F, 0.55F, 0.08F,
                        entry(ModItems.SYNAPTIC_CORE.get(), 45),
                        entry(ModItems.NEURAL_ECHO_DECOY.get(), 30),
                        entry(ModItems.FALSE_ENGRAM_INJECTOR.get(), 15),
                        entry(ModItems.SYNAPTIC_GRAFT.get(), 10))));
    }

    private void generateAlliesAndSinkholeFauna(Holder<Enchantment> looting) {
        add(ModEntities.PURIFIER_DRONE.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.CHITIN_PLATE.get(), 1.0F, 2.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(Items.IRON_INGOT, 1.0F, 2.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 45))
                        .add(entry(ModItems.SPORE_ANTIDOTE.get(), 35))
                        .add(entry(Items.REDSTONE, 20, 1.0F, 3.0F))));

        add(ModEntities.MYCELIAL_CHIMERA.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.MYCELIAL_BIOMASS.get(), 2.0F, 3.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.CHITIN_PLATE.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.NEURAL_ECHO_CORE.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.40F))
                        .add(entry(ModItems.VOLATILE_ENGRAM.get(), 45))
                        .add(entry(ModItems.LATENT_SPORE.get(), 35, 1.0F, 2.0F))
                        .add(entry(ModItems.GENETIC_SAMPLE.get(), 20)
                                .apply(SetComponentsFunction.setComponent(
                                        ModDataComponents.TRAIT_ID.get(), AdaptationTrait.MELEE_THORNS.id())))));

        add(ModEntities.SPORE_GLIDER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.CANOPY_MEMBRANE.get(), 1.0F, 2.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemRandomChanceCondition.randomChance(0.45F))
                        .add(LootItem.lootTableItem(ModItems.LATENT_SPORE.get()))));

        add(ModEntities.ROOT_KEEPER.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.ROOT_HEART_RESIN.get())
                                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(1.0F)))
                                .apply(lootingBonus())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.CHITIN_PLATE.get(), 1.0F, 3.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(LootItemKilledByPlayerCondition.killedByPlayer())
                        .when(LootItemRandomChanceCondition.randomChance(0.40F))
                        .add(LootItem.lootTableItem(ModItems.SYNAPTIC_CORE.get()))));
    }

    private void generateBosses() {
        add(ModEntities.SPORE_BEHEMOTH.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.CHITIN_PLATE.get(), 3.0F, 5.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.MYCELIAL_BIOMASS.get(), 3.0F, 6.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(2.0F))
                        .add(entry(ModItems.SYNAPTIC_CORE.get(), 28))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 22))
                        .add(entry(ModItems.PURIFIER_GRENADE.get(), 18, 1.0F, 2.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 14))
                        .add(entry(ModItems.TRUCE_PHEROMONE.get(), 10))
                        .add(entry(ModItems.CHITIN_CHESTPLATE.get(), 8))));

        add(ModEntities.HIVE_CORE.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.HIVE_HEART.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.SYNAPTIC_CORE.get(), 2.0F, 4.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(2.0F))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 28))
                        .add(entry(ModItems.CHITIN_PLATE.get(), 24, 3.0F, 6.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 16))
                        .add(entry(ModItems.SYNAPTIC_GRAFT.get(), 12))
                        .add(entry(ModItems.FALSE_ENGRAM_INJECTOR.get(), 10))
                        .add(entry(Items.DIAMOND, 10, 2.0F, 4.0F))));

        add(ModEntities.APEX_MATRIARCH.get(), LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.OVERMIND_ESSENCE.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(LootItem.lootTableItem(ModItems.HIVE_HEART.get())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .add(lootingItem(ModItems.SYNAPTIC_CORE.get(), 3.0F, 5.0F)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(2.0F))
                        .add(entry(ModItems.QUORUM_SEAL.get(), 20))
                        .add(entry(ModItems.CHIMERA_OVUM.get(), 20))
                        .add(entry(ModItems.NEURAL_ECHO_CORE.get(), 22, 1.0F, 2.0F))
                        .add(entry(ModItems.CONCENTRATED_ANTIDOTE.get(), 14, 1.0F, 2.0F))
                        .add(entry(Items.ECHO_SHARD, 14, 2.0F, 4.0F))
                        .add(entry(Items.NETHERITE_SCRAP, 10))));
    }

    private static LootItemCondition.Builder lootingChance(
            Holder<Enchantment> looting,
            float unenchantedChance,
            float enchantedBase,
            float perLevelAboveFirst
    ) {
        return () -> new LootItemRandomChanceWithEnchantedBonusCondition(
                unenchantedChance,
                LevelBasedValue.perLevel(enchantedBase, perLevelAboveFirst),
                looting
        );
    }

    private EnchantedCountIncreaseFunction.Builder lootingBonus() {
        return EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(0.0F, 1.0F));
    }

    private LootPoolSingletonContainer.Builder<?> lootingItem(ItemLike item) {
        return LootItem.lootTableItem(item).apply(lootingBonus());
    }

    private LootPoolSingletonContainer.Builder<?> lootingItem(ItemLike item, float min, float max) {
        return LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                .apply(lootingBonus());
    }

    private static LootPoolSingletonContainer.Builder<?> entry(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolSingletonContainer.Builder<?> entry(ItemLike item, int weight, float min, float max) {
        return LootItem.lootTableItem(item)
                .setWeight(weight)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
    }

    private static LootPool.Builder playerLootingPool(
            Holder<Enchantment> looting,
            float unenchantedChance,
            float enchantedBase,
            float perLevelAboveFirst,
            LootPoolSingletonContainer.Builder<?>... entries
    ) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemKilledByPlayerCondition.killedByPlayer())
                .when(lootingChance(looting, unenchantedChance, enchantedBase, perLevelAboveFirst));
        for (LootPoolSingletonContainer.Builder<?> e : List.of(entries)) {
            pool.add(e);
        }
        return pool;
    }
}
