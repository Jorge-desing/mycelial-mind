package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;

/**
 * Registers custom ArmorMaterials for Mycelial Mind.
 */
public final class ModArmorMaterials {
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, MycelialMind.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CHITIN = ARMOR_MATERIALS.register("chitin",
            () -> new ArmorMaterial(
                    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
                        map.put(ArmorItem.Type.BOOTS, 2);
                        map.put(ArmorItem.Type.LEGGINGS, 5);
                        map.put(ArmorItem.Type.CHESTPLATE, 6);
                        map.put(ArmorItem.Type.HELMET, 2);
                        map.put(ArmorItem.Type.BODY, 9);
                    }),
                    14,
                    ModSounds.CHITIN_ARMOR_EQUIP,
                    () -> Ingredient.of(ModItems.CHITIN_PLATE.get()),
                    List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "chitin"))),
                    1.0F,
                    0.0F
            ));

    private ModArmorMaterials() {
    }

    public static void register(IEventBus modEventBus) {
        ARMOR_MATERIALS.register(modEventBus);
    }
}
