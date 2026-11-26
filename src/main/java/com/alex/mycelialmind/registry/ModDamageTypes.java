package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Data-driven {@link DamageType} keys and safe {@link DamageSource} factories for Mycelial Mind
 * hazards, projectiles, boss psychic attacks, purification purges, and enzymatic weapons.
 */
public final class ModDamageTypes {
    public static final ResourceKey<DamageType> ACID_RAIN = key("acid_rain");
    public static final ResourceKey<DamageType> ACID_SPIT = key("acid_spit");
    public static final ResourceKey<DamageType> PSYCHIC_SURGE = key("psychic_surge");
    public static final ResourceKey<DamageType> PURIFICATION_PURGE = key("purification_purge");
    public static final ResourceKey<DamageType> BIO_ENZYME = key("bio_enzyme");

    public static final List<ResourceKey<DamageType>> ALL = List.of(
            ACID_RAIN,
            ACID_SPIT,
            PSYCHIC_SURGE,
            PURIFICATION_PURGE,
            BIO_ENZYME
    );

    private ModDamageTypes() {
    }

    public static DamageSource source(Level level, ResourceKey<DamageType> key) {
        return source(level, key, null, null);
    }

    public static DamageSource source(
            Level level,
            ResourceKey<DamageType> key,
            @Nullable Entity directEntity,
            @Nullable Entity causingEntity
    ) {
        Optional<Holder.Reference<DamageType>> holder = level.registryAccess()
                .registry(Registries.DAMAGE_TYPE)
                .flatMap(registry -> registry.getHolder(key));
        if (holder.isPresent()) {
            return new DamageSource(holder.get(), directEntity, causingEntity);
        }
        return directEntity != null
                ? level.damageSources().indirectMagic(directEntity, causingEntity)
                : level.damageSources().magic();
    }

    private static ResourceKey<DamageType> key(String path) {
        return ResourceKey.create(Registries.DAMAGE_TYPE,
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, path));
    }
}
