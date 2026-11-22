package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.world.loot.MycelialTraceLootModifier;
import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Registers NeoForge Global Loot Modifier serializers for Mycelial Mind.
 */
public final class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIER_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MycelialMind.MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<MycelialTraceLootModifier>> MYCELIAL_TRACE_LOOT =
            LOOT_MODIFIER_SERIALIZERS.register("mycelial_trace_loot", () -> MycelialTraceLootModifier.CODEC);

    private ModLootModifiers() {
    }

    public static void register(IEventBus modEventBus) {
        LOOT_MODIFIER_SERIALIZERS.register(modEventBus);
    }
}
