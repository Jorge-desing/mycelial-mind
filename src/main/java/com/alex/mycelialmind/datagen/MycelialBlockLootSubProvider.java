package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.ReclaimedBloomBlock;
import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.AlternativesEntry;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.ApplyExplosionDecay;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Generates block loot tables for all Mycelial Mind blocks with drop tables,
 * plus the explicit {@code blocks/reclaimed_bloom} table for {@link ReclaimedBloomBlock}.
 */
public final class MycelialBlockLootSubProvider extends BlockLootSubProvider {
    private static final Set<Block> NO_DROP_BLOCKS = Set.of(
            ModBlocks.RECLAIMED_BLOOM.get(),
            ModBlocks.EPHEMERAL_CHITIN.get(),
            ModBlocks.MYCELIAL_RIFT.get()
    );

    public MycelialBlockLootSubProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream()
                .<Block>map(DeferredHolder::value)
                .filter(block -> !NO_DROP_BLOCKS.contains(block))
                .toList();
    }

    /**
     * Emits {@code blocks/reclaimed_bloom} separately because {@link ModBlocks#RECLAIMED_BLOOM}
     * declares {@code noLootTable()} in its block properties while harvesting uses palette drops.
     */
    public static LootTableSubProvider reclaimedBloomSubProvider() {
        return new ReclaimedBloomLootSubProvider();
    }

    @Override
    protected void generate() {
        Holder<Enchantment> fortune = fortuneHolder();

        // Simple self-drops (12 blocks)
        dropSelf(ModBlocks.BIO_CHITIN_LATTICE.get());
        dropSelf(ModBlocks.CHITIN_PILLAR.get());
        dropSelf(ModBlocks.MATRIARCH_ALTAR.get());
        dropSelf(ModBlocks.MYCELIAL_ARCH_BRICK.get());
        dropSelf(ModBlocks.MYCELIAL_STONE.get());
        dropSelf(ModBlocks.PURIFICATION_PYLON.get());
        dropSelf(ModBlocks.PURIFICATION_TURRET.get());
        dropSelf(ModBlocks.REINFORCED_BIO_BARRIER.get());
        dropSelf(ModBlocks.ROOT_CAP_SHELF.get());
        dropSelf(ModBlocks.SOVEREIGN_NEXUS.get());
        dropSelf(ModBlocks.SPORE_UPDRAFT_VENT.get());
        dropSelf(ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());

        // Fixed replacement drops (2 blocks)
        dropOther(ModBlocks.HIVE_ALTAR.get(), ModItems.SYNAPTIC_CORE.get());
        dropOther(ModBlocks.SYMBIOTIC_NODE.get(), ModItems.SYNAPTIC_CORE.get());

        // Silk-touch terrain / vein drops (3 blocks)
        add(ModBlocks.CREEPING_MYCELIUM.get(), block -> LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(LootItem.lootTableItem(block)
                                .when(hasSilkTouch())
                                .otherwise(LootItem.lootTableItem(ModItems.MYCELIAL_BIOMASS.get())
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))))));

        add(ModBlocks.ROOT_SAP_MOSS.get(), block -> LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(LootItem.lootTableItem(block)
                                .when(hasSilkTouch())
                                .otherwise(LootItem.lootTableItem(ModItems.MYCELIAL_BIOMASS.get())
                                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F)))
                                        .apply(ApplyExplosionDecay.explosionDecay())))));

        add(ModBlocks.HYPHAL_VEIN.get(), block -> LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(LootItem.lootTableItem(block)
                                .when(hasSilkTouch())
                                .otherwise(LootItem.lootTableItem(ModItems.MYCELIAL_BIOMASS.get())
                                        .when(LootItemRandomChanceCondition.randomChance(0.35F))))));

        // Silk-touch flora & veins with Fortune + rare bonus pool (5 blocks)
        addFloraWithBonus(
                ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get(),
                fortuneDrop(ModItems.MYCELIAL_BIOMASS.get(), 1.0F, 2.0F, fortune),
                0.30F,
                LootItem.lootTableItem(ModItems.LATENT_SPORE.get()).setWeight(45),
                LootItem.lootTableItem(Items.GLOWSTONE_DUST).setWeight(35)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))),
                LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(20)
        );

        addFloraWithBonus(
                ModBlocks.LUMINESCENT_AMBER_VEIN.get(),
                fortuneDrop(Items.AMETHYST_SHARD, 1.0F, 3.0F, fortune),
                0.35F,
                LootItem.lootTableItem(Items.GLOWSTONE_DUST).setWeight(45)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(2.0F, 4.0F)))
                        .apply(ApplyBonusCount.addUniformBonusCount(fortune, 1)),
                LootItem.lootTableItem(ModItems.LATENT_SPORE.get()).setWeight(35)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))),
                LootItem.lootTableItem(ModItems.TRUCE_PHEROMONE.get()).setWeight(20)
        );

        addFloraWithBonus(
                ModBlocks.MYCELIAL_TENDRILS.get(),
                fortuneDrop(ModItems.MYCELIAL_BIOMASS.get(), 1.0F, 2.0F, fortune),
                0.30F,
                LootItem.lootTableItem(ModItems.LATENT_SPORE.get()).setWeight(55),
                LootItem.lootTableItem(Items.GLOWSTONE_DUST).setWeight(25),
                LootItem.lootTableItem(ModItems.VOLATILE_ENGRAM.get()).setWeight(20)
        );

        addFloraWithBonus(
                ModBlocks.SPORE_BULB.get(),
                fortuneDrop(ModItems.LATENT_SPORE.get(), 1.0F, 2.0F, fortune),
                0.34F,
                LootItem.lootTableItem(ModItems.MYCELIAL_BIOMASS.get()).setWeight(50),
                LootItem.lootTableItem(ModItems.TRUCE_PHEROMONE.get()).setWeight(28),
                LootItem.lootTableItem(Items.AMETHYST_SHARD).setWeight(22)
        );

        addFloraWithBonus(
                ModBlocks.SPORE_VENT.get(),
                fortuneDrop(ModItems.LATENT_SPORE.get(), 1.0F, 2.0F, fortune),
                0.32F,
                LootItem.lootTableItem(ModItems.MYCELIAL_BIOMASS.get()).setWeight(55)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0F, 2.0F))),
                LootItem.lootTableItem(ModItems.SPORE_ANTIDOTE.get()).setWeight(25),
                LootItem.lootTableItem(ModItems.TRUCE_PHEROMONE.get()).setWeight(20)
        );

        // Antibody cyst (3 pools: silk touch, biomass/spore fortune, rare drop)
        add(ModBlocks.ANTIBODY_CYST.get(), block -> LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .when(hasSilkTouch())
                        .add(LootItem.lootTableItem(block)))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .when(doesNotHaveSilkTouch())
                        .add(fortuneWeightedDrop(ModItems.MYCELIAL_BIOMASS.get(), 60, 1.0F, 2.0F, fortune)
                                .apply(ApplyExplosionDecay.explosionDecay()))
                        .add(fortuneWeightedDrop(ModItems.LATENT_SPORE.get(), 40, 1.0F, 2.0F, fortune)
                                .apply(ApplyExplosionDecay.explosionDecay())))
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .when(doesNotHaveSilkTouch())
                        .when(LootItemRandomChanceCondition.randomChance(0.25F))
                        .add(LootItem.lootTableItem(ModItems.CHITIN_PLATE.get()).setWeight(50))
                        .add(LootItem.lootTableItem(ModItems.VOLATILE_ENGRAM.get()).setWeight(30))
                        .add(LootItem.lootTableItem(ModItems.NEURAL_ECHO_CORE.get()).setWeight(20))));

        // Synaptic nodes (3 blocks)
        addNodeTable(
                ModBlocks.MYCELIAL_NODE.get(),
                LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(fortuneWeightedDrop(ModItems.MYCELIAL_BIOMASS.get(), 50, 1.0F, 3.0F, fortune))
                        .add(fortuneWeightedDrop(ModItems.LATENT_SPORE.get(), 35, 1.0F, 2.0F, fortune))
                        .add(LootItem.lootTableItem(ModBlocks.LUMINESCENT_AMBER_VEIN.get()).setWeight(15)),
                0.20F,
                LootItem.lootTableItem(ModItems.VOLATILE_ENGRAM.get()).setWeight(60),
                LootItem.lootTableItem(ModItems.NEURAL_ECHO_CORE.get()).setWeight(40)
        );

        addNodeTable(
                ModBlocks.INCUBATOR_NODE.get(),
                LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(fortuneWeightedDrop(ModItems.MYCELIAL_BIOMASS.get(), 45, 2.0F, 4.0F, fortune))
                        .add(fortuneWeightedDrop(ModItems.LATENT_SPORE.get(), 35, 2.0F, 3.0F, fortune))
                        .add(fortuneWeightedDrop(ModItems.CHITIN_PLATE.get(), 20, 1.0F, 2.0F, fortune)),
                0.30F,
                LootItem.lootTableItem(ModItems.GENETIC_SAMPLE.get()).setWeight(50)
                        .apply(SetComponentsFunction.setComponent(
                                ModDataComponents.TRAIT_ID.get(), AdaptationTrait.MELEE_THORNS.id())),
                LootItem.lootTableItem(ModItems.VOLATILE_ENGRAM.get()).setWeight(30),
                LootItem.lootTableItem(ModItems.CHIMERA_OVUM.get()).setWeight(20)
        );

        addNodeTable(
                ModBlocks.SENTINEL_NODE.get(),
                LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(fortuneWeightedDrop(ModItems.CHITIN_PLATE.get(), 50, 2.0F, 4.0F, fortune))
                        .add(fortuneWeightedDrop(ModItems.LATENT_SPORE.get(), 30, 1.0F, 3.0F, fortune))
                        .add(fortuneWeightedDrop(ModItems.MYCELIAL_BIOMASS.get(), 20, 1.0F, 2.0F, fortune)),
                0.35F,
                LootItem.lootTableItem(ModItems.NEURAL_ECHO_CORE.get()).setWeight(50),
                LootItem.lootTableItem(ModItems.VOLATILE_ENGRAM.get()).setWeight(30),
                LootItem.lootTableItem(ModItems.SYNAPTIC_GRAFT.get()).setWeight(20)
        );
    }

    private Holder<Enchantment> fortuneHolder() {
        return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE);
    }

    private LootPoolSingletonContainer.Builder<?> fortuneDrop(ItemLike item, float min, float max, Holder<Enchantment> fortune) {
        return LootItem.lootTableItem(item)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                .apply(ApplyBonusCount.addUniformBonusCount(fortune, 1))
                .apply(ApplyExplosionDecay.explosionDecay());
    }

    private static LootPoolSingletonContainer.Builder<?> fortuneWeightedDrop(
            ItemLike item,
            int weight,
            float min,
            float max,
            Holder<Enchantment> fortune
    ) {
        return LootItem.lootTableItem(item)
                .setWeight(weight)
                .apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)))
                .apply(ApplyBonusCount.addUniformBonusCount(fortune, 1));
    }

    private void addFloraWithBonus(
            Block block,
            LootPoolEntryContainer.Builder<?> nonSilkDrop,
            float bonusChance,
            LootPoolEntryContainer.Builder<?>... bonusEntries
    ) {
        LootPool.Builder bonusPool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(ExplosionCondition.survivesExplosion())
                .when(doesNotHaveSilkTouch())
                .when(LootItemRandomChanceCondition.randomChance(bonusChance));
        for (LootPoolEntryContainer.Builder<?> entry : bonusEntries) {
            bonusPool.add(entry);
        }
        add(block, b -> LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(LootItem.lootTableItem(b)
                                .when(hasSilkTouch())
                                .otherwise(nonSilkDrop)))
                .withPool(bonusPool));
    }

    private void addNodeTable(
            Block block,
            LootPool.Builder mainPool,
            float rareChance,
            LootPoolEntryContainer.Builder<?>... rareEntries
    ) {
        LootPool.Builder rarePool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1.0F))
                .when(ExplosionCondition.survivesExplosion())
                .when(LootItemRandomChanceCondition.randomChance(rareChance));
        for (LootPoolEntryContainer.Builder<?> entry : rareEntries) {
            rarePool.add(entry);
        }
        add(block, b -> LootTable.lootTable()
                .withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1.0F))
                        .when(ExplosionCondition.survivesExplosion())
                        .add(LootItem.lootTableItem(ModItems.SYNAPTIC_CORE.get())))
                .withPool(mainPool)
                .withPool(rarePool));
    }

    private static final class ReclaimedBloomLootSubProvider implements LootTableSubProvider {
        private static final ResourceKey<LootTable> RECLAIMED_BLOOM_TABLE = ResourceKey.create(
                Registries.LOOT_TABLE,
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "blocks/reclaimed_bloom")
        );

        @Override
        public void generate(BiConsumer<ResourceKey<LootTable>, LootTable.Builder> output) {
            Block bloom = ModBlocks.RECLAIMED_BLOOM.get();
            LootPoolEntryContainer.Builder<?>[] alternatives = {
                    paletteVariant(bloom, 1, ModItems.RECLAIMED_BLOOM_MELEE.get()),
                    paletteVariant(bloom, 2, ModItems.RECLAIMED_BLOOM_PROJECTILE.get()),
                    paletteVariant(bloom, 3, ModItems.RECLAIMED_BLOOM_FIRE.get()),
                    paletteVariant(bloom, 4, ModItems.RECLAIMED_BLOOM_EXPLOSION.get()),
                    paletteVariant(bloom, 5, ModItems.RECLAIMED_BLOOM_MAGIC.get()),
                    LootItem.lootTableItem(bloom)
            };
            output.accept(RECLAIMED_BLOOM_TABLE, LootTable.lootTable()
                    .withPool(LootPool.lootPool()
                            .setRolls(ConstantValue.exactly(1.0F))
                            .when(ExplosionCondition.survivesExplosion())
                            .add(AlternativesEntry.alternatives(alternatives))));
        }

        private static LootPoolEntryContainer.Builder<?> paletteVariant(Block bloom, int palette, Item dropItem) {
            return LootItem.lootTableItem(dropItem)
                    .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(bloom)
                            .setProperties(StatePropertiesPredicate.Builder.properties()
                                    .hasProperty(ReclaimedBloomBlock.PALETTE, palette)));
        }
    }
}
