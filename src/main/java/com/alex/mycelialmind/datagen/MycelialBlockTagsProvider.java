package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Generates block tags (boss immunities, tool tier requirements, and mineable tools).
 */
public final class MycelialBlockTagsProvider extends BlockTagsProvider {
    private static final TagKey<Block> CARRYON_BLOCK_BLACKLIST =
            BlockTags.create(ResourceLocation.fromNamespaceAndPath("carryon", "block_blacklist"));

    public MycelialBlockTagsProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            @Nullable ExistingFileHelper existingFileHelper
    ) {
        super(output, lookupProvider, MycelialMind.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(BlockTags.DRAGON_IMMUNE).add(
                ModBlocks.REINFORCED_BIO_BARRIER.get(),
                ModBlocks.MATRIARCH_ALTAR.get(),
                ModBlocks.HIVE_ALTAR.get()
        );

        tag(BlockTags.WITHER_IMMUNE).add(
                ModBlocks.REINFORCED_BIO_BARRIER.get(),
                ModBlocks.MATRIARCH_ALTAR.get(),
                ModBlocks.HIVE_ALTAR.get()
        );

        tag(BlockTags.NEEDS_DIAMOND_TOOL).add(
                ModBlocks.REINFORCED_BIO_BARRIER.get(),
                ModBlocks.MATRIARCH_ALTAR.get(),
                ModBlocks.HIVE_ALTAR.get()
        );

        tag(BlockTags.NEEDS_IRON_TOOL).add(
                ModBlocks.PURIFICATION_PYLON.get(),
                ModBlocks.PURIFICATION_TURRET.get(),
                ModBlocks.SYNAPTIC_RESEARCH_BENCH.get(),
                ModBlocks.CHITIN_PILLAR.get(),
                ModBlocks.MYCELIAL_ARCH_BRICK.get(),
                ModBlocks.BIO_CHITIN_LATTICE.get(),
                ModBlocks.LUMINESCENT_AMBER_VEIN.get()
        );

        tag(BlockTags.MINEABLE_WITH_HOE).add(
                ModBlocks.SPORE_VENT.get(),
                ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get(),
                ModBlocks.SPORE_BULB.get(),
                ModBlocks.MYCELIAL_TENDRILS.get(),
                ModBlocks.CREEPING_MYCELIUM.get(),
                ModBlocks.ANTIBODY_CYST.get(),
                ModBlocks.EPHEMERAL_CHITIN.get(),
                ModBlocks.ROOT_SAP_MOSS.get(),
                ModBlocks.ROOT_CAP_SHELF.get()
        );

        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                ModBlocks.PURIFICATION_PYLON.get(),
                ModBlocks.PURIFICATION_TURRET.get(),
                ModBlocks.REINFORCED_BIO_BARRIER.get(),
                ModBlocks.MYCELIAL_NODE.get(),
                ModBlocks.INCUBATOR_NODE.get(),
                ModBlocks.SENTINEL_NODE.get(),
                ModBlocks.SYMBIOTIC_NODE.get(),
                ModBlocks.SOVEREIGN_NEXUS.get(),
                ModBlocks.SYNAPTIC_RESEARCH_BENCH.get(),
                ModBlocks.MYCELIAL_STONE.get(),
                ModBlocks.HIVE_ALTAR.get(),
                ModBlocks.CHITIN_PILLAR.get(),
                ModBlocks.MYCELIAL_ARCH_BRICK.get(),
                ModBlocks.BIO_CHITIN_LATTICE.get(),
                ModBlocks.LUMINESCENT_AMBER_VEIN.get(),
                ModBlocks.MATRIARCH_ALTAR.get(),
                ModBlocks.EPHEMERAL_CHITIN.get(),
                ModBlocks.SPORE_UPDRAFT_VENT.get()
        );

        tag(BlockTags.MINEABLE_WITH_SHOVEL).add(
                ModBlocks.CREEPING_MYCELIUM.get()
        );

        Block[] immovableBlocks = {
                ModBlocks.MYCELIAL_NODE.get(),
                ModBlocks.INCUBATOR_NODE.get(),
                ModBlocks.SENTINEL_NODE.get(),
                ModBlocks.SYMBIOTIC_NODE.get(),
                ModBlocks.SOVEREIGN_NEXUS.get(),
                ModBlocks.PURIFICATION_PYLON.get(),
                ModBlocks.PURIFICATION_TURRET.get(),
                ModBlocks.SYNAPTIC_RESEARCH_BENCH.get(),
                ModBlocks.HIVE_ALTAR.get(),
                ModBlocks.MATRIARCH_ALTAR.get(),
                ModBlocks.MYCELIAL_RIFT.get()
        };
        tag(Tags.Blocks.RELOCATION_NOT_SUPPORTED).add(immovableBlocks);
        tag(CARRYON_BLOCK_BLACKLIST).add(immovableBlocks);
    }
}
