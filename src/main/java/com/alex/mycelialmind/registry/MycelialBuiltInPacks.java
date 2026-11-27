package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.block.entity.MonumentFocusBlockEntity;
import com.alex.mycelialmind.block.entity.PurificationTurretBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.gametest.GameTestHooks;

import java.util.Locale;

/**
 * Exposes optional built-in server datapacks ({@code hardcore_overmind} and {@code relaxed_research})
 * via {@link AddPackFindersEvent}, disabled by default so vanilla balance remains unchanged unless opted in.
 * Skipped under {@link GameTestHooks#isGametestServer()} because {@code GameTestServer} forcibly activates
 * every discovered {@code PackType.SERVER_DATA} pack simultaneously.
 */
public final class MycelialBuiltInPacks {
    public enum ActivePreset {
        DEFAULT,
        HARDCORE_OVERMIND,
        RELAXED_RESEARCH,
        CUSTOM;

        public String translationKey() {
            return "pack.mycelialmind.preset." + name().toLowerCase(Locale.ROOT);
        }
    }

    private MycelialBuiltInPacks() {
    }

    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA || GameTestHooks.isGametestServer()) {
            return;
        }
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "packs/hardcore_overmind"),
                PackType.SERVER_DATA,
                Component.translatable("pack.mycelialmind.hardcore_overmind.title"),
                PackSource.FEATURE,
                false,
                Pack.Position.TOP
        );
        event.addPackFinders(
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "packs/relaxed_research"),
                PackType.SERVER_DATA,
                Component.translatable("pack.mycelialmind.relaxed_research.title"),
                PackSource.FEATURE,
                false,
                Pack.Position.TOP
        );
    }

    public static ActivePreset detectPresetFromValues(
            int amethystPylonFuel,
            int concentratedAntidoteAmmo,
            int biomassNutrient
    ) {
        if (amethystPylonFuel == 25 && concentratedAntidoteAmmo == 64 && biomassNutrient == 15) {
            return ActivePreset.DEFAULT;
        }
        if (amethystPylonFuel == 16 && concentratedAntidoteAmmo == 42 && biomassNutrient == 10) {
            return ActivePreset.HARDCORE_OVERMIND;
        }
        if (amethystPylonFuel == 38 && concentratedAntidoteAmmo == 64 && biomassNutrient == 24) {
            return ActivePreset.RELAXED_RESEARCH;
        }
        return ActivePreset.CUSTOM;
    }

    @SuppressWarnings("deprecation")
    public static int resolveCurrentBiomassNutrient() {
        ModDataMaps.SymbioticNutrientData data = BuiltInRegistries.ITEM
                .wrapAsHolder(ModItems.MYCELIAL_BIOMASS.get())
                .getData(ModDataMaps.SYMBIOTIC_NUTRIENT);
        return data != null ? data.stabilityRestore() : 15;
    }

    public static ActivePreset detectActivePreset() {
        int pylonFuel = MonumentFocusBlockEntity.resolvePylonRechargeAmount(new ItemStack(Items.AMETHYST_SHARD));
        int turretAmmo = PurificationTurretBlockEntity.resolveTurretAmmoGain(
                new ItemStack(ModItems.CONCENTRATED_ANTIDOTE.get()));
        int nutrient = resolveCurrentBiomassNutrient();
        return detectPresetFromValues(pylonFuel, turretAmmo, nutrient);
    }
}
