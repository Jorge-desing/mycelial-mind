package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.List;

/**
 * Generates the 61 boilerplate item models under {@code assets/mycelialmind/models/item/*.json}
 * (29 block-parented items, 20 auto-discovered spawn eggs, 11 flat generated items, and 1 handheld
 * staff), while preserving the 35 custom 3D voxel {@code elements} item models in {@code src/main/resources/}.
 */
public final class MycelialItemModelProvider extends ItemModelProvider {
    private static final List<DeferredBlock<?>> BLOCK_PARENTED_BLOCKS = List.of(
            ModBlocks.ANTIBODY_CYST,
            ModBlocks.BIO_CHITIN_LATTICE,
            ModBlocks.CHITIN_PILLAR,
            ModBlocks.CREEPING_MYCELIUM,
            ModBlocks.EPHEMERAL_CHITIN,
            ModBlocks.HIVE_ALTAR,
            ModBlocks.INCUBATOR_NODE,
            ModBlocks.LUMINESCENT_AMBER_VEIN,
            ModBlocks.MATRIARCH_ALTAR,
            ModBlocks.MYCELIAL_ARCH_BRICK,
            ModBlocks.MYCELIAL_NODE,
            ModBlocks.MYCELIAL_STONE,
            ModBlocks.PURIFICATION_PYLON,
            ModBlocks.PURIFICATION_TURRET,
            ModBlocks.RECLAIMED_BLOOM,
            ModBlocks.REINFORCED_BIO_BARRIER,
            ModBlocks.ROOT_CAP_SHELF,
            ModBlocks.ROOT_SAP_MOSS,
            ModBlocks.SENTINEL_NODE,
            ModBlocks.SOVEREIGN_NEXUS,
            ModBlocks.SPORE_UPDRAFT_VENT,
            ModBlocks.SPORE_VENT,
            ModBlocks.SYMBIOTIC_NODE,
            ModBlocks.SYNAPTIC_RESEARCH_BENCH
    );

    private static final List<DeferredItem<?>> BLOCK_PARENTED_VARIANTS = List.of(
            ModItems.RECLAIMED_BLOOM_EXPLOSION,
            ModItems.RECLAIMED_BLOOM_FIRE,
            ModItems.RECLAIMED_BLOOM_MAGIC,
            ModItems.RECLAIMED_BLOOM_MELEE,
            ModItems.RECLAIMED_BLOOM_PROJECTILE
    );

    private static final List<DeferredItem<?>> GENERATED_ITEMS = List.of(
            ModItems.CANOPY_MEMBRANE,
            ModItems.FIELD_ENGRAM,
            ModItems.MYCELIAL_CODEX,
            ModItems.RESONANT_SAP_ELIXIR,
            ModItems.ROOT_HEART_RESIN,
            ModItems.ROOT_SAP_VIAL
    );

    private static final List<DeferredBlock<?>> FLAT_BLOCK_ITEMS = List.of(
            ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP,
            ModBlocks.MYCELIAL_RIFT,
            ModBlocks.MYCELIAL_TENDRILS,
            ModBlocks.SPORE_BULB
    );

    public MycelialItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (DeferredBlock<?> block : BLOCK_PARENTED_BLOCKS) {
            withBlockParent(block.getId().getPath());
        }
        for (DeferredItem<?> item : BLOCK_PARENTED_VARIANTS) {
            withBlockParent(item.getId().getPath());
        }

        ModItems.ITEMS.getEntries().stream()
                .filter(entry -> entry.get() instanceof DeferredSpawnEggItem)
                .forEach(entry -> spawnEggItem(entry.get()));

        for (DeferredItem<?> item : GENERATED_ITEMS) {
            String path = item.getId().getPath();
            generatedItem(path, "item/" + path);
        }
        for (DeferredBlock<?> block : FLAT_BLOCK_ITEMS) {
            String path = block.getId().getPath();
            generatedItem(path, "block/" + path);
        }
        generatedItem(ModBlocks.HYPHAL_VEIN.getId().getPath(), "block/hyphal_vein_dot");

        handheldItem(ModItems.SOVEREIGN_COMMAND_STAFF.get());
    }

    private void withBlockParent(String path) {
        withExistingParent(path, modLoc("block/" + path));
    }

    private void generatedItem(String itemPath, String texturePath) {
        withExistingParent(itemPath, mcLoc("item/generated"))
                .texture("layer0", modLoc(texturePath));
    }
}
