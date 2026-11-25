package com.alex.mycelialmind.recipe;

import com.alex.mycelialmind.hive.AdaptationTrait;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModDataComponents;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared recipe descriptors matching SynapticBenchBlockEntity.computeSynthesisResult()
 * for JEI, EMI, and automated GameTest verification.
 */
public final class SynapticBenchRecipeDefinitions {
    public record SynapticRecipeDescriptor(
            String id,
            List<ItemStack> sampleInputs,
            List<ItemStack> biomassInputs,
            List<ItemStack> catalystInputs,
            ItemStack output,
            String noteKey
    ) {
        public List<ItemStack> outputAlternatives() {
            if (!output.is(ModItems.SYMBIOTIC_MUTAGEN.get())
                    && !output.is(ModItems.COUNTER_SERUM.get())) {
                return List.of(output.copy());
            }
            List<ItemStack> alternatives = new ArrayList<>();
            for (ItemStack sample : sampleInputs) {
                AdaptationTrait trait = com.alex.mycelialmind.item.GeneticSampleItem.getTrait(sample);
                if (trait == null) {
                    continue;
                }
                ItemStack result = output.copy();
                result.set(ModDataComponents.TRAIT_ID.get(), trait.id());
                alternatives.add(result);
            }
            return alternatives.isEmpty() ? List.of(output.copy()) : List.copyOf(alternatives);
        }
    }

    private SynapticBenchRecipeDefinitions() {
    }

    public static ItemStack sampleStack(AdaptationTrait trait) {
        ItemStack stack = new ItemStack(ModItems.GENETIC_SAMPLE.get());
        stack.set(ModDataComponents.TRAIT_ID.get(), trait.id());
        return stack;
    }

    public static List<ItemStack> allTypedSamples() {
        List<ItemStack> list = new ArrayList<>();
        for (AdaptationTrait trait : AdaptationTrait.values()) {
            list.add(sampleStack(trait));
        }
        return List.copyOf(list);
    }

    public static List<SynapticRecipeDescriptor> allRecipes() {
        ItemStack mutagenOut = new ItemStack(ModItems.SYMBIOTIC_MUTAGEN.get());
        mutagenOut.set(ModDataComponents.TRAIT_ID.get(), AdaptationTrait.MELEE_THORNS.id());

        ItemStack counterOut = new ItemStack(ModItems.COUNTER_SERUM.get());
        counterOut.set(ModDataComponents.TRAIT_ID.get(), AdaptationTrait.MELEE_THORNS.id());

        List<SynapticRecipeDescriptor> recipes = new ArrayList<>(List.of(
                new SynapticRecipeDescriptor(
                        "symbiotic_mutagen",
                        allTypedSamples(),
                        List.of(new ItemStack(ModItems.MYCELIAL_BIOMASS.get())),
                        List.of(new ItemStack(ModItems.SYNAPTIC_CORE.get())),
                        mutagenOut,
                        "recipe.mycelialmind.synaptic.mutagen"
                ),
                new SynapticRecipeDescriptor(
                        "counter_serum",
                        allTypedSamples(),
                        List.of(new ItemStack(ModItems.LATENT_SPORE.get())),
                        List.of(new ItemStack(ModItems.PURIFIER_GRENADE.get()), new ItemStack(Items.AMETHYST_SHARD)),
                        counterOut,
                        "recipe.mycelialmind.synaptic.counter_serum"
                ),
                new SynapticRecipeDescriptor(
                        "concentrated_antidote",
                        List.of(new ItemStack(ModItems.SPORE_ANTIDOTE.get())),
                        List.of(new ItemStack(ModItems.LATENT_SPORE.get())),
                        List.of(new ItemStack(Items.AMETHYST_SHARD)),
                        new ItemStack(ModItems.CONCENTRATED_ANTIDOTE.get()),
                        "recipe.mycelialmind.synaptic.concentrated_antidote"
                ),
                new SynapticRecipeDescriptor(
                        "chimera_ovum",
                        allTypedSamples(),
                        List.of(new ItemStack(ModItems.MYCELIAL_BIOMASS.get())),
                        List.of(new ItemStack(ModItems.NEURAL_ECHO_CORE.get())),
                        new ItemStack(ModItems.CHIMERA_OVUM.get()),
                        "recipe.mycelialmind.synaptic.chimera_ovum"
                ),
                new SynapticRecipeDescriptor(
                        "resonant_sap_elixir",
                        List.of(new ItemStack(ModItems.ROOT_SAP_VIAL.get())),
                        List.of(new ItemStack(ModItems.MYCELIAL_BIOMASS.get())),
                        List.of(new ItemStack(ModItems.ROOT_HEART_RESIN.get())),
                        new ItemStack(ModItems.RESONANT_SAP_ELIXIR.get()),
                        "recipe.mycelialmind.synaptic.resonant_sap_elixir"
                )
        ));
        List<ItemStack> blooms = List.of(
                new ItemStack(ModBlocks.RECLAIMED_BLOOM.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_MELEE.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_PROJECTILE.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_FIRE.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_EXPLOSION.get()),
                new ItemStack(ModItems.RECLAIMED_BLOOM_MAGIC.get()));
        String[] ids = {"base", "melee", "projectile", "fire", "explosion", "magic"};
        for (int i = 0; i < blooms.size(); i++) {
            ItemStack bloom = blooms.get(i);
            recipes.add(new SynapticRecipeDescriptor("reclaimed_bloom_" + ids[i], List.of(bloom.copy()),
                    List.of(new ItemStack(ModItems.MYCELIAL_BIOMASS.get())), List.of(),
                    bloom.copyWithCount(2), "recipe.mycelialmind.synaptic.reclaimed_bloom"));
        }
        return List.copyOf(recipes);
    }

    public record CompatInfoEntry(String idPath, ItemLike item, String translationKey) {
    }

    public static List<CompatInfoEntry> allCompatInfoEntries() {
        return List.of(
                new CompatInfoEntry("bio_scanner", ModItems.BIO_SCANNER.get(), "compat.mycelialmind.info.bio_scanner"),
                new CompatInfoEntry("bio_extractor", ModItems.BIO_EXTRACTOR.get(), "compat.mycelialmind.info.bio_extractor"),
                new CompatInfoEntry("tide_beacon", ModItems.TIDE_BEACON.get(), "compat.mycelialmind.info.tide_beacon"),
                new CompatInfoEntry("purifier_drone_core", ModItems.PURIFIER_DRONE_CORE.get(), "compat.mycelialmind.info.purifier_drone_core"),
                new CompatInfoEntry("counter_serum", ModItems.COUNTER_SERUM.get(), "compat.mycelialmind.info.counter_serum"),
                new CompatInfoEntry("chimera_ovum", ModItems.CHIMERA_OVUM.get(), "compat.mycelialmind.info.chimera_ovum"),
                new CompatInfoEntry("synaptic_graft", ModItems.SYNAPTIC_GRAFT.get(), "compat.mycelialmind.info.synaptic_graft"),
                new CompatInfoEntry("false_engram_injector", ModItems.FALSE_ENGRAM_INJECTOR.get(), "compat.mycelialmind.info.false_engram_injector"),
                new CompatInfoEntry("rift_catalyst", ModItems.RIFT_CATALYST.get(), "compat.mycelialmind.info.rift_catalyst"),
                new CompatInfoEntry("antibody_cyst", ModBlocks.ANTIBODY_CYST.get(), "compat.mycelialmind.info.antibody_cyst"),
                new CompatInfoEntry("volatile_engram", ModItems.VOLATILE_ENGRAM.get(), "compat.mycelialmind.info.volatile_engram"),
                new CompatInfoEntry("schism_injector", ModItems.SCHISM_INJECTOR.get(), "compat.mycelialmind.info.schism_injector"),
                new CompatInfoEntry("truce_pheromone", ModItems.TRUCE_PHEROMONE.get(), "compat.mycelialmind.info.truce_pheromone"),
                new CompatInfoEntry("quorum_seal", ModItems.QUORUM_SEAL.get(), "compat.mycelialmind.info.quorum_seal"),
                new CompatInfoEntry("symbiotic_node", ModBlocks.SYMBIOTIC_NODE.get(), "compat.mycelialmind.info.symbiotic_node"),
                new CompatInfoEntry("overmind_crown", ModItems.OVERMIND_CROWN.get(), "compat.mycelialmind.info.overmind_crown"),
                new CompatInfoEntry("ephemeral_chitin", ModBlocks.EPHEMERAL_CHITIN.get(), "compat.mycelialmind.info.ephemeral_chitin"),
                new CompatInfoEntry("purification_pylon", ModBlocks.PURIFICATION_PYLON.get(), "compat.mycelialmind.info.purification_pylon"),
                new CompatInfoEntry("synaptic_research_bench", ModBlocks.SYNAPTIC_RESEARCH_BENCH.get(), "compat.mycelialmind.info.synaptic_research_bench"),
                new CompatInfoEntry("bioluminescent_mycelial_cap", ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get(), "compat.mycelialmind.info.bioluminescent_mycelial_cap"),
                new CompatInfoEntry("mycelial_tendrils", ModBlocks.MYCELIAL_TENDRILS.get(), "compat.mycelialmind.info.mycelial_tendrils"),
                new CompatInfoEntry("spore_bulb", ModBlocks.SPORE_BULB.get(), "compat.mycelialmind.info.spore_bulb"),
                new CompatInfoEntry("luminescent_amber_vein", ModBlocks.LUMINESCENT_AMBER_VEIN.get(), "compat.mycelialmind.info.luminescent_amber_vein"),
                new CompatInfoEntry("hyphal_vein", ModItems.HYPHAL_VEIN.get(), "compat.mycelialmind.info.hyphal_vein"),
                new CompatInfoEntry("mycelial_codex", ModItems.MYCELIAL_CODEX.get(), "compat.mycelialmind.info.mycelial_codex"),
                new CompatInfoEntry("sovereign_nexus", ModBlocks.SOVEREIGN_NEXUS.get(), "compat.mycelialmind.info.sovereign_nexus"),
                new CompatInfoEntry("sovereign_command_staff", ModItems.SOVEREIGN_COMMAND_STAFF.get(), "compat.mycelialmind.info.sovereign_command_staff"),
                new CompatInfoEntry("field_engram", ModItems.FIELD_ENGRAM.get(), "compat.mycelialmind.info.field_engram"),
                new CompatInfoEntry("reclaimed_bloom", ModBlocks.RECLAIMED_BLOOM.get(), "compat.mycelialmind.info.reclaimed_bloom"),
                new CompatInfoEntry("spore_updraft_vent", ModBlocks.SPORE_UPDRAFT_VENT.get(), "compat.mycelialmind.info.spore_updraft_vent"),
                new CompatInfoEntry("root_sap_moss", ModBlocks.ROOT_SAP_MOSS.get(), "compat.mycelialmind.info.root_sap_moss"),
                new CompatInfoEntry("root_cap_shelf", ModBlocks.ROOT_CAP_SHELF.get(), "compat.mycelialmind.info.root_cap_shelf"),
                new CompatInfoEntry("root_sap_vial", ModItems.ROOT_SAP_VIAL.get(), "compat.mycelialmind.info.root_sap_vial"),
                new CompatInfoEntry("canopy_membrane", ModItems.CANOPY_MEMBRANE.get(), "compat.mycelialmind.info.canopy_membrane"),
                new CompatInfoEntry("root_heart_resin", ModItems.ROOT_HEART_RESIN.get(), "compat.mycelialmind.info.root_heart_resin"),
                new CompatInfoEntry("resonant_sap_elixir", ModItems.RESONANT_SAP_ELIXIR.get(), "compat.mycelialmind.info.resonant_sap_elixir")
        );
    }
}
