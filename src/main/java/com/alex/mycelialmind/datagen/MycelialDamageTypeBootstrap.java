package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModDamageTypes;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

/**
 * Registers the 5 data-driven {@link DamageType} definitions for Mycelial Mind.
 */
public final class MycelialDamageTypeBootstrap {
    private MycelialDamageTypeBootstrap() {
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        register(context, ModDamageTypes.ACID_RAIN, DamageScaling.ALWAYS, 0.1F);
        register(context, ModDamageTypes.ACID_SPIT, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F);
        register(context, ModDamageTypes.PSYCHIC_SURGE, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F);
        register(context, ModDamageTypes.PURIFICATION_PURGE, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.0F);
        register(context, ModDamageTypes.BIO_ENZYME, DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F);
    }

    private static void register(
            BootstrapContext<DamageType> context,
            ResourceKey<DamageType> key,
            DamageScaling scaling,
            float exhaustion
    ) {
        String msgId = MycelialMind.MODID + "." + key.location().getPath();
        context.register(key, new DamageType(msgId, scaling, exhaustion, DamageEffects.HURT));
    }
}
