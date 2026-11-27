package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;

/**
 * Data-driven 1.21.1 {@link Enchantment} keys and pure {@code Synaptic Harvest} scaling rules.
 */
public final class ModEnchantments {
    public static final ResourceKey<Enchantment> ANTIMYCOTIC = key("antimycotic");
    public static final ResourceKey<Enchantment> BIO_WARD = key("bio_ward");
    public static final ResourceKey<Enchantment> SYNAPTIC_HARVEST = key("synaptic_harvest");

    public static final List<ResourceKey<Enchantment>> ALL = List.of(
            ANTIMYCOTIC,
            BIO_WARD,
            SYNAPTIC_HARVEST
    );

    private ModEnchantments() {
    }

    public static float computeSynapticHarvestMaxHealthRatio(float baseRatio, int level) {
        if (level <= 0) {
            return Math.max(0.05F, Math.min(0.95F, baseRatio));
        }
        return Math.max(0.05F, Math.min(0.95F, baseRatio + level * 0.12F));
    }

    public static int computeSynapticHarvestCooldown(int baseTicks, int level) {
        int clampedBase = Math.max(1, baseTicks);
        if (level <= 0) {
            return clampedBase;
        }
        float multiplier = Math.max(0.40F, 1.0F - level * 0.20F);
        return Math.max(8, Math.round(clampedBase * multiplier));
    }

    public static float computeSynapticHarvestBonusChance(int level) {
        if (level <= 0) {
            return 0.0F;
        }
        return Math.min(0.75F, level * 0.35F);
    }

    private static ResourceKey<Enchantment> key(String path) {
        return ResourceKey.create(Registries.ENCHANTMENT,
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, path));
    }
}
