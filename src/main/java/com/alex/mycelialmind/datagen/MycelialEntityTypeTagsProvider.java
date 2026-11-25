package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModEntities;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates entity type tags (NeoForge common bosses tag and fall damage immunity).
 */
public final class MycelialEntityTypeTagsProvider extends EntityTypeTagsProvider {
    private static final TagKey<EntityType<?>> CARRYON_ENTITY_BLACKLIST =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("carryon", "entity_blacklist"));

    public MycelialEntityTypeTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(Tags.EntityTypes.BOSSES).add(
                ModEntities.HIVE_CORE.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.SPORE_BEHEMOTH.get()
        );

        tag(EntityTypeTags.FALL_DAMAGE_IMMUNE).add(
                ModEntities.HIVE_CORE.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.SPORE_BEHEMOTH.get(),
                ModEntities.PURIFIER_DRONE.get()
        );

        EntityType<?>[] bossEntities = {
                ModEntities.HIVE_CORE.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.SPORE_BEHEMOTH.get()
        };
        tag(Tags.EntityTypes.CAPTURING_NOT_SUPPORTED).add(bossEntities);
        tag(Tags.EntityTypes.TELEPORTING_NOT_SUPPORTED).add(bossEntities);

        tag(CARRYON_ENTITY_BLACKLIST).add(
                ModEntities.HIVE_CORE.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.SPORE_BEHEMOTH.get(),
                ModEntities.PURIFIER_DRONE.get(),
                ModEntities.MYCELIAL_CHIMERA.get()
        );
    }
}
