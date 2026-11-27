package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.hive.StimulusType;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import java.util.Locale;

/**
 * Registers custom NeoForge {@link DataMapType} definitions enabling modpacks and datapacks to
 * map arbitrary {@link DamageType}s to Hive {@link StimulusType} weights and arbitrary {@link Item}s
 * to Symbiotic Node stability nutrients.
 */
public final class ModDataMaps {
    public static final Codec<StimulusType> STIMULUS_TYPE_CODEC = DamageStimulusData.STIMULUS_TYPE_CODEC;

    public static final DataMapType<DamageType, DamageStimulusData> DAMAGE_STIMULUS =
            DataMapType.builder(
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "damage_stimulus"),
                    Registries.DAMAGE_TYPE,
                    DamageStimulusData.CODEC
            ).build();

    public static final DataMapType<Item, SymbioticNutrientData> SYMBIOTIC_NUTRIENT =
            DataMapType.builder(
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "symbiotic_nutrient"),
                    Registries.ITEM,
                    SymbioticNutrientData.CODEC
            ).synced(SymbioticNutrientData.CODEC, false).build();

    public static final DataMapType<Item, PylonFuelData> PYLON_FUEL =
            DataMapType.builder(
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "pylon_fuel"),
                    Registries.ITEM,
                    PylonFuelData.CODEC
            ).synced(PylonFuelData.CODEC, false).build();

    public static final DataMapType<Item, TurretAmmoData> TURRET_AMMO =
            DataMapType.builder(
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "turret_ammo"),
                    Registries.ITEM,
                    TurretAmmoData.CODEC
            ).synced(TurretAmmoData.CODEC, false).build();

    private ModDataMaps() {
    }

    public static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(DAMAGE_STIMULUS);
        event.register(SYMBIOTIC_NUTRIENT);
        event.register(PYLON_FUEL);
        event.register(TURRET_AMMO);
    }

    public record DamageStimulusData(StimulusType stimulus, float weightMultiplier) {
        public static final Codec<StimulusType> STIMULUS_TYPE_CODEC = Codec.STRING.comapFlatMap(
                rawId -> {
                    String normalized = rawId.trim().toLowerCase(Locale.ROOT);
                    for (StimulusType value : StimulusType.values()) {
                        if (value.id().equals(normalized)) {
                            return DataResult.success(value);
                        }
                    }
                    return DataResult.error(() -> "Unknown Mycelial StimulusType: " + rawId);
                },
                StimulusType::id
        );

        public static final Codec<DamageStimulusData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                STIMULUS_TYPE_CODEC.fieldOf("stimulus").forGetter(DamageStimulusData::stimulus),
                Codec.floatRange(0.0F, 10.0F)
                        .optionalFieldOf("weight_multiplier", 1.0F)
                        .forGetter(DamageStimulusData::weightMultiplier)
        ).apply(instance, DamageStimulusData::new));
    }

    public record SymbioticNutrientData(int stabilityRestore) {
        public static final Codec<SymbioticNutrientData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 100)
                        .fieldOf("stability_restore")
                        .forGetter(SymbioticNutrientData::stabilityRestore)
        ).apply(instance, SymbioticNutrientData::new));
    }

    public record PylonFuelData(int chargeRestore) {
        public static final Codec<PylonFuelData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 100)
                        .fieldOf("charge_restore")
                        .forGetter(PylonFuelData::chargeRestore)
        ).apply(instance, PylonFuelData::new));
    }

    public record TurretAmmoData(int ammoRestore) {
        public static final Codec<TurretAmmoData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.intRange(1, 64)
                        .fieldOf("ammo_restore")
                        .forGetter(TurretAmmoData::ammoRestore)
        ).apply(instance, TurretAmmoData::new));
    }
}
