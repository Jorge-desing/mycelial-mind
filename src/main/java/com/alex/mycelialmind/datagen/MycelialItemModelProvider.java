package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/**
 * Generates the 61 boilerplate item models under {@code assets/mycelialmind/models/item/*.json}
 * (29 block-parented items, 20 spawn eggs, 11 flat generated items, and 1 handheld staff), while
 * preserving the 35 custom 3D voxel {@code elements} item models in {@code src/main/resources/}.
 */
public final class MycelialItemModelProvider extends ItemModelProvider {
    private static final List<String> BLOCK_PARENTED_ITEMS = List.of(
            "antibody_cyst",
            "bio_chitin_lattice",
            "chitin_pillar",
            "creeping_mycelium",
            "ephemeral_chitin",
            "hive_altar",
            "incubator_node",
            "luminescent_amber_vein",
            "matriarch_altar",
            "mycelial_arch_brick",
            "mycelial_node",
            "mycelial_stone",
            "purification_pylon",
            "purification_turret",
            "reclaimed_bloom",
            "reclaimed_bloom_explosion",
            "reclaimed_bloom_fire",
            "reclaimed_bloom_magic",
            "reclaimed_bloom_melee",
            "reclaimed_bloom_projectile",
            "reinforced_bio_barrier",
            "root_cap_shelf",
            "root_sap_moss",
            "sentinel_node",
            "sovereign_nexus",
            "spore_updraft_vent",
            "spore_vent",
            "symbiotic_node",
            "synaptic_research_bench"
    );

    private static final List<DeferredItem<Item>> SPAWN_EGGS = List.of(
            ModItems.ACID_SPITTER_SPAWN_EGG,
            ModItems.APEX_MATRIARCH_SPAWN_EGG,
            ModItems.BURROWER_SPAWN_EGG,
            ModItems.CHITIN_BRUTE_SPAWN_EGG,
            ModItems.COLONY_SYMBIONT_SPAWN_EGG,
            ModItems.COLONY_WARDEN_SPAWN_EGG,
            ModItems.GOLIATH_CARRIER_SPAWN_EGG,
            ModItems.HIVE_CORE_SPAWN_EGG,
            ModItems.LEECH_PARASITE_SPAWN_EGG,
            ModItems.MYCELIAL_CHIMERA_SPAWN_EGG,
            ModItems.MYCELIAL_STALKER_SPAWN_EGG,
            ModItems.PURIFIER_DRONE_SPAWN_EGG,
            ModItems.PYRO_SHROOM_SPAWN_EGG,
            ModItems.ROOT_KEEPER_SPAWN_EGG,
            ModItems.SPORE_ARCHITECT_SPAWN_EGG,
            ModItems.SPORE_BEHEMOTH_SPAWN_EGG,
            ModItems.SPORE_GLIDER_SPAWN_EGG,
            ModItems.SPORELING_SPAWN_EGG,
            ModItems.SYNAPSE_WEAVER_SPAWN_EGG,
            ModItems.SYNAPTIC_MIMIC_SPAWN_EGG
    );

    public MycelialItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (String name : BLOCK_PARENTED_ITEMS) {
            withExistingParent(name, modLoc("block/" + name));
        }

        for (DeferredItem<Item> spawnEgg : SPAWN_EGGS) {
            spawnEggItem(spawnEgg.get());
        }

        generatedItem(ModItems.CANOPY_MEMBRANE.getId().getPath(), "item/canopy_membrane");
        generatedItem(ModItems.FIELD_ENGRAM.getId().getPath(), "item/field_engram");
        generatedItem(ModItems.MYCELIAL_CODEX.getId().getPath(), "item/mycelial_codex");
        generatedItem(ModItems.RESONANT_SAP_ELIXIR.getId().getPath(), "item/resonant_sap_elixir");
        generatedItem(ModItems.ROOT_HEART_RESIN.getId().getPath(), "item/root_heart_resin");
        generatedItem(ModItems.ROOT_SAP_VIAL.getId().getPath(), "item/root_sap_vial");

        generatedItem(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.getId().getPath(), "block/bioluminescent_mycelial_cap");
        generatedItem(ModBlocks.HYPHAL_VEIN.getId().getPath(), "block/hyphal_vein_dot");
        generatedItem(ModBlocks.MYCELIAL_RIFT.getId().getPath(), "block/mycelial_rift");
        generatedItem(ModBlocks.MYCELIAL_TENDRILS.getId().getPath(), "block/mycelial_tendrils");
        generatedItem(ModBlocks.SPORE_BULB.getId().getPath(), "block/spore_bulb");

        handheldItem(ModItems.SOVEREIGN_COMMAND_STAFF.get());
    }

    private void generatedItem(String itemPath, String texturePath) {
        withExistingParent(itemPath, mcLoc("item/generated"))
                .texture("layer0", modLoc(texturePath));
    }
}
