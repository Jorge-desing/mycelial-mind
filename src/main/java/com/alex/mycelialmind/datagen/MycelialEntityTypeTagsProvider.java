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
    private static final TagKey<EntityType<?>> IF_MOB_IMPRISONMENT_BLACKLIST =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("industrialforegoing", "mob_imprisonment_tool_blacklist"));
    private static final TagKey<EntityType<?>> IF_MOB_DUPLICATOR_BLACKLIST =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath("industrialforegoing", "mob_duplicator_blacklist"));

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

        EntityType<?>[] protectedEntities = {
                ModEntities.HIVE_CORE.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.SPORE_BEHEMOTH.get(),
                ModEntities.PURIFIER_DRONE.get(),
                ModEntities.MYCELIAL_CHIMERA.get(),
                ModEntities.COLONY_SYMBIONT.get()
        };
        tag(CARRYON_ENTITY_BLACKLIST).add(
                ModEntities.HIVE_CORE.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.SPORE_BEHEMOTH.get(),
                ModEntities.PURIFIER_DRONE.get(),
                ModEntities.MYCELIAL_CHIMERA.get()
        );
        tag(IF_MOB_IMPRISONMENT_BLACKLIST).add(protectedEntities);
        tag(IF_MOB_DUPLICATOR_BLACKLIST).add(protectedEntities);

        tag(ModEntities.MYCELIAL_MOBS).add(
                ModEntities.SPORELING.get(),
                ModEntities.CHITIN_BRUTE.get(),
                ModEntities.MYCELIAL_STALKER.get(),
                ModEntities.ACID_SPITTER.get(),
                ModEntities.BURROWER.get(),
                ModEntities.PYRO_SHROOM.get(),
                ModEntities.SYNAPSE_WEAVER.get(),
                ModEntities.GOLIATH_CARRIER.get(),
                ModEntities.LEECH_PARASITE.get(),
                ModEntities.COLONY_WARDEN.get(),
                ModEntities.SPORE_ARCHITECT.get(),
                ModEntities.SPORE_BEHEMOTH.get(),
                ModEntities.SYNAPTIC_MIMIC.get(),
                ModEntities.HIVE_CORE.get(),
                ModEntities.APEX_MATRIARCH.get(),
                ModEntities.SPORE_GLIDER.get(),
                ModEntities.ROOT_KEEPER.get()
        );
    }
}
