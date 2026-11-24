package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.world.HiveTierRarityPlacement;
import com.alex.mycelialmind.world.MycelialOutbreakFeature;
import com.alex.mycelialmind.world.MycelialSinkholeFeature;
import com.alex.mycelialmind.world.SurfaceHiveFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacementModifierType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers worldgen features and placement modifiers for Mycelial Mind.
 */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, MycelialMind.MODID);

    public static final DeferredRegister<PlacementModifierType<?>> PLACEMENT_MODIFIER_TYPES =
            DeferredRegister.create(Registries.PLACEMENT_MODIFIER_TYPE, MycelialMind.MODID);

    public static final DeferredHolder<Feature<?>, MycelialOutbreakFeature> MYCELIAL_OUTBREAK =
            FEATURES.register("mycelial_outbreak", () -> new MycelialOutbreakFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, SurfaceHiveFeature> SURFACE_HIVE =
            FEATURES.register("surface_hive", () -> new SurfaceHiveFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<Feature<?>, MycelialSinkholeFeature> MYCELIAL_SINKHOLE =
            FEATURES.register("mycelial_sinkhole", () -> new MycelialSinkholeFeature(NoneFeatureConfiguration.CODEC));

    public static final DeferredHolder<PlacementModifierType<?>, PlacementModifierType<HiveTierRarityPlacement>> HIVE_TIER_RARITY =
            PLACEMENT_MODIFIER_TYPES.register("hive_tier_rarity", () -> () -> HiveTierRarityPlacement.CODEC);

    private ModFeatures() {
    }

    public static void register(IEventBus modEventBus) {
        FEATURES.register(modEventBus);
        PLACEMENT_MODIFIER_TYPES.register(modEventBus);
    }
}
