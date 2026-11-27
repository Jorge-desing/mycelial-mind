package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModAttributes;
import com.alex.mycelialmind.registry.ModEnchantments;
import com.alex.mycelialmind.registry.ModEntities;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.AddValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;

/**
 * Registers Mycelial Mind's 3 data-driven treasure enchantments for 1.21.1:
 * {@code antimycotic}, {@code bio_ward}, and {@code synaptic_harvest}.
 */
public final class MycelialEnchantmentBootstrap {
    private MycelialEnchantmentBootstrap() {
    }

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        HolderGetter<Item> items = context.lookup(Registries.ITEM);
        HolderGetter<Enchantment> enchantments = context.lookup(Registries.ENCHANTMENT);

        context.register(
                ModEnchantments.ANTIMYCOTIC,
                Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ItemTags.WEAPON_ENCHANTABLE),
                        items.getOrThrow(ItemTags.SWORD_ENCHANTABLE),
                        5,
                        3,
                        Enchantment.dynamicCost(10, 9),
                        Enchantment.dynamicCost(35, 9),
                        2,
                        EquipmentSlotGroup.MAINHAND
                ))
                        .exclusiveWith(enchantments.getOrThrow(EnchantmentTags.DAMAGE_EXCLUSIVE))
                        .withEffect(
                                EnchantmentEffectComponents.DAMAGE,
                                new AddValue(LevelBasedValue.perLevel(2.5F)),
                                LootItemEntityPropertyCondition.hasProperties(
                                        LootContext.EntityTarget.THIS,
                                        EntityPredicate.Builder.entity().of(ModEntities.MYCELIAL_MOBS)
                                )
                        )
                        .build(ModEnchantments.ANTIMYCOTIC.location())
        );

        context.register(
                ModEnchantments.BIO_WARD,
                Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE),
                        4,
                        3,
                        Enchantment.dynamicCost(12, 10),
                        Enchantment.dynamicCost(38, 10),
                        2,
                        EquipmentSlotGroup.ARMOR
                ))
                        .withEffect(
                                EnchantmentEffectComponents.ATTRIBUTES,
                                new EnchantmentAttributeEffect(
                                        ResourceLocation.fromNamespaceAndPath(
                                                MycelialMind.MODID, "enchantment.bio_ward.infection_resistance"),
                                        ModAttributes.INFECTION_RESISTANCE,
                                        LevelBasedValue.perLevel(0.10F),
                                        AttributeModifier.Operation.ADD_VALUE
                                )
                        )
                        .withEffect(
                                EnchantmentEffectComponents.ATTRIBUTES,
                                new EnchantmentAttributeEffect(
                                        ResourceLocation.fromNamespaceAndPath(
                                                MycelialMind.MODID, "enchantment.bio_ward.acid_resistance"),
                                        ModAttributes.ACID_RESISTANCE,
                                        LevelBasedValue.perLevel(0.08F),
                                        AttributeModifier.Operation.ADD_VALUE
                                )
                        )
                        .build(ModEnchantments.BIO_WARD.location())
        );

        context.register(
                ModEnchantments.SYNAPTIC_HARVEST,
                Enchantment.enchantment(Enchantment.definition(
                        items.getOrThrow(ModItems.BIO_EXTRACTOR_ENCHANTABLE),
                        3,
                        2,
                        Enchantment.dynamicCost(15, 12),
                        Enchantment.dynamicCost(45, 12),
                        4,
                        EquipmentSlotGroup.MAINHAND
                ))
                        .build(ModEnchantments.SYNAPTIC_HARVEST.location())
        );
    }
}
