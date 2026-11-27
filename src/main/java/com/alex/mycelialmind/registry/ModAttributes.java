package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom syncable player attributes for biological and corrosive resistances.
 */
public final class ModAttributes {
    public static final double MAX_RESISTANCE = 0.90D;

    public static final DeferredRegister<Attribute> ATTRIBUTES =
            DeferredRegister.create(Registries.ATTRIBUTE, MycelialMind.MODID);

    public static final DeferredHolder<Attribute, Attribute> INFECTION_RESISTANCE = ATTRIBUTES.register(
            "infection_resistance",
            () -> new RangedAttribute(
                    "attribute.name.mycelialmind.infection_resistance",
                    0.0D,
                    0.0D,
                    MAX_RESISTANCE
            ).setSyncable(true)
    );

    public static final DeferredHolder<Attribute, Attribute> ACID_RESISTANCE = ATTRIBUTES.register(
            "acid_resistance",
            () -> new RangedAttribute(
                    "attribute.name.mycelialmind.acid_resistance",
                    0.0D,
                    0.0D,
                    MAX_RESISTANCE
            ).setSyncable(true)
    );

    private ModAttributes() {
    }

    public static void register(IEventBus modEventBus) {
        ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(ModAttributes::onEntityAttributeModification);
    }

    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, INFECTION_RESISTANCE);
        event.add(EntityType.PLAYER, ACID_RESISTANCE);
    }

    public static double computeMitigatedValue(double baseValue, double resistance, double minFloor) {
        return com.alex.mycelialmind.world.DimensionalHomeostasisEngine.computeAttributeMitigatedValue(
                baseValue, resistance, minFloor);
    }
}
