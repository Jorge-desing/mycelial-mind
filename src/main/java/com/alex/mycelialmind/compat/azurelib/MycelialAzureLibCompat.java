package com.alex.mycelialmind.compat.azurelib;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;

/**
 * Optional AzureLib (MIT/MPL-2.0) compatibility bridge for Blockbench / .geo.json models.
 * Safely isolated so Mycelial Mind runs standalone or alongside AzureLib without hard dependency.
 */
public final class MycelialAzureLibCompat {
    public static final String MOD_ID = "azurelib";

    private MycelialAzureLibCompat() {
    }

    public static boolean isAzureLibLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    public static ResourceLocation getGeoModelLocation(String entityId) {
        return ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "geo/" + entityId + ".geo.json");
    }

    public static double easeInOutCubic(double t) {
        if (isAzureLibLoaded()) {
            return AzureLibHelper.easeInOutCubic(t);
        }
        double x = Math.max(0.0D, Math.min(1.0D, t));
        return x < 0.5D ? 4.0D * x * x * x : 1.0D - Math.pow(-2.0D * x + 2.0D, 3.0D) / 2.0D;
    }

    private static final class AzureLibHelper {
        private static double easeInOutCubic(double t) {
            if (mod.azure.azurelib.common.animation.easing.AzEasingTypes.EASE_IN_OUT_CUBIC != null) {
                double x = Math.max(0.0D, Math.min(1.0D, t));
                return x < 0.5D ? 4.0D * x * x * x : 1.0D - Math.pow(-2.0D * x + 2.0D, 3.0D) / 2.0D;
            }
            return t;
        }
    }
}
