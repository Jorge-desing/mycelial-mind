package com.alex.mycelialmind.client;

import com.alex.mycelialmind.registry.ModDataMaps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * Appends live synchronized NeoForge {@code DataMapType} values ({@code pylon_fuel},
 * {@code turret_ammo}, {@code symbiotic_nutrient}) to item tooltips when Shift is held.
 */
public final class MycelialDataMapTooltips {
    private MycelialDataMapTooltips() {
    }

    @SuppressWarnings("deprecation")
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!Screen.hasShiftDown()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) {
            return;
        }
        var holder = BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
        ModDataMaps.PylonFuelData pylonFuel = holder.getData(ModDataMaps.PYLON_FUEL);
        ModDataMaps.TurretAmmoData turretAmmo = holder.getData(ModDataMaps.TURRET_AMMO);
        ModDataMaps.SymbioticNutrientData nutrient = holder.getData(ModDataMaps.SYMBIOTIC_NUTRIENT);

        if (pylonFuel != null && pylonFuel.chargeRestore() > 0) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.mycelialmind.datamap.pylon_fuel", pylonFuel.chargeRestore()
            ).withStyle(ChatFormatting.AQUA));
        }
        if (turretAmmo != null && turretAmmo.ammoRestore() > 0) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.mycelialmind.datamap.turret_ammo", turretAmmo.ammoRestore()
            ).withStyle(ChatFormatting.GOLD));
        }
        if (nutrient != null && nutrient.stabilityRestore() > 0) {
            event.getToolTip().add(Component.translatable(
                    "tooltip.mycelialmind.datamap.symbiotic_nutrient", nutrient.stabilityRestore()
            ).withStyle(ChatFormatting.GREEN));
        }
    }
}
