package com.alex.mycelialmind.datagen;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.registry.ModBlocks;
import com.alex.mycelialmind.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.SingleItemRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;

import java.util.concurrent.CompletableFuture;

/**
 * Generates all 43 Mycelial Mind crafting, smelting, smoking, blasting, and stonecutting recipes
 * along with standard recipe-book unlock advancements.
 * <p>
 * Note: optional Farmer's Delight integration ({@code compat/farmersdelight/cutting_tendrils.json})
 * remains in static resources because its third-party {@code farmersdelight:cutting} serializer
 * is not on the compile classpath.
 */
public final class MycelialRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public MycelialRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    private static ResourceLocation modId(String path) {
        return ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, path);
    }

    @Override
    protected void buildRecipes(RecipeOutput output, HolderLookup.Provider holderLookup) {
        buildEarlyProgressionRecipes(output);
        buildArmorAndArsenalRecipes(output);
        buildEndgameAndDiplomacyRecipes(output);
        buildSinkholeAndConstructionRecipes(output);
        buildCookingAndStonecuttingRecipes(output);
    }

    private static void buildEarlyProgressionRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BIO_SCANNER.get())
                .pattern(" R ")
                .pattern("ICI")
                .pattern(" S ")
                .define('R', Items.REDSTONE)
                .define('I', Items.COPPER_INGOT)
                .define('C', Items.COMPASS)
                .define('S', ModItems.LATENT_SPORE.get())
                .unlockedBy(getHasName(ModItems.LATENT_SPORE.get()), has(ModItems.LATENT_SPORE.get()))
                .save(output, modId("bio_scanner"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MYCELIAL_CODEX.get())
                .requires(Items.BOOK)
                .requires(ModItems.LATENT_SPORE.get())
                .requires(ModItems.MYCELIAL_BIOMASS.get())
                .unlockedBy(getHasName(ModItems.MYCELIAL_BIOMASS.get()), has(ModItems.MYCELIAL_BIOMASS.get()))
                .save(output, modId("mycelial_codex"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BIO_EXTRACTOR.get())
                .pattern(" A ")
                .pattern("IBI")
                .pattern(" S ")
                .define('A', Items.AMETHYST_SHARD)
                .define('I', Items.IRON_INGOT)
                .define('B', ModItems.MYCELIAL_BIOMASS.get())
                .define('S', Items.STICK)
                .unlockedBy(getHasName(ModItems.MYCELIAL_BIOMASS.get()), has(ModItems.MYCELIAL_BIOMASS.get()))
                .save(output, modId("bio_extractor"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SYNAPTIC_RESEARCH_BENCH.get())
                .pattern("ASA")
                .pattern("ICI")
                .pattern("BBB")
                .define('A', Items.AMETHYST_SHARD)
                .define('S', ModItems.MYCELIAL_BIOMASS.get())
                .define('I', Items.IRON_INGOT)
                .define('C', Items.CRAFTING_TABLE)
                .define('B', ModItems.CHITIN_PLATE.get())
                .unlockedBy(getHasName(ModItems.MYCELIAL_BIOMASS.get()), has(ModItems.MYCELIAL_BIOMASS.get()))
                .save(output, modId("synaptic_research_bench"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.SPORE_ANTIDOTE.get())
                .requires(Items.GLASS_BOTTLE)
                .requires(ModItems.LATENT_SPORE.get())
                .requires(Items.SUGAR)
                .unlockedBy(getHasName(ModItems.MYCELIAL_BIOMASS.get()), has(ModItems.MYCELIAL_BIOMASS.get()))
                .save(output, modId("spore_antidote"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CONCENTRATED_ANTIDOTE.get())
                .requires(ModItems.SPORE_ANTIDOTE.get())
                .requires(ModItems.LATENT_SPORE.get())
                .requires(Items.AMETHYST_SHARD)
                .requires(ModItems.MYCELIAL_BIOMASS.get())
                .requires(Items.GLOWSTONE_DUST)
                .unlockedBy(getHasName(ModItems.LATENT_SPORE.get()), has(ModItems.LATENT_SPORE.get()))
                .save(output, modId("concentrated_antidote"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.CONCENTRATED_ANTIDOTE.get())
                .requires(ModItems.ROOT_SAP_VIAL.get())
                .requires(ModItems.LATENT_SPORE.get())
                .requires(ModItems.MYCELIAL_BIOMASS.get())
                .unlockedBy(getHasName(ModItems.ROOT_SAP_VIAL.get()), has(ModItems.ROOT_SAP_VIAL.get()))
                .save(output, modId("spore_antidote_from_root_sap"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.PURIFIER_GRENADE.get())
                .pattern(" A ")
                .pattern("LGL")
                .pattern("GBG")
                .define('A', Items.AMETHYST_SHARD)
                .define('L', ModItems.LATENT_SPORE.get())
                .define('G', Items.GUNPOWDER)
                .define('B', ModItems.MYCELIAL_BIOMASS.get())
                .unlockedBy(getHasName(ModItems.MYCELIAL_BIOMASS.get()), has(ModItems.MYCELIAL_BIOMASS.get()))
                .save(output, modId("purifier_grenade"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PURIFICATION_PYLON.get())
                .pattern(" A ")
                .pattern("CSC")
                .pattern("BBB")
                .define('A', Items.AMETHYST_SHARD)
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .define('B', Items.POLISHED_DEEPSLATE)
                .unlockedBy(getHasName(ModItems.CHITIN_PLATE.get()), has(ModItems.CHITIN_PLATE.get()))
                .save(output, modId("purification_pylon"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.PURIFICATION_TURRET.get())
                .pattern(" A ")
                .pattern("ILI")
                .pattern(" P ")
                .define('A', Items.AMETHYST_SHARD)
                .define('I', Items.IRON_INGOT)
                .define('L', ModItems.NEUTRALIZING_SPORE_LAUNCHER.get())
                .define('P', ModBlocks.PURIFICATION_PYLON.get())
                .unlockedBy(getHasName(ModItems.SYNAPTIC_CORE.get()), has(ModItems.SYNAPTIC_CORE.get()))
                .save(output, modId("purification_turret"));
    }

    private static void buildArmorAndArsenalRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.CHITIN_HELMET.get())
                .pattern("CCC")
                .pattern("CAC")
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(ModItems.CHITIN_PLATE.get()), has(ModItems.CHITIN_PLATE.get()))
                .save(output, modId("chitin_helmet"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.CHITIN_CHESTPLATE.get())
                .pattern("C C")
                .pattern("CSC")
                .pattern("CCC")
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .unlockedBy(getHasName(ModItems.CHITIN_PLATE.get()), has(ModItems.CHITIN_PLATE.get()))
                .save(output, modId("chitin_chestplate"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.CHITIN_LEGGINGS.get())
                .pattern("CCC")
                .pattern("CIC")
                .pattern("C C")
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(ModItems.CHITIN_PLATE.get()), has(ModItems.CHITIN_PLATE.get()))
                .save(output, modId("chitin_leggings"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.CHITIN_BOOTS.get())
                .pattern("C C")
                .pattern("CLC")
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('L', Items.LEATHER)
                .unlockedBy(getHasName(ModItems.CHITIN_PLATE.get()), has(ModItems.CHITIN_PLATE.get()))
                .save(output, modId("chitin_boots"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.ENZYME_LANCE.get())
                .pattern(" CS")
                .pattern("CSC")
                .pattern("II ")
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('S', ModItems.LATENT_SPORE.get())
                .define('I', Items.IRON_INGOT)
                .unlockedBy(getHasName(ModItems.CHITIN_PLATE.get()), has(ModItems.CHITIN_PLATE.get()))
                .save(output, modId("enzyme_lance"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.NEUTRALIZING_SPORE_LAUNCHER.get())
                .pattern("LSL")
                .pattern(" C ")
                .pattern(" B ")
                .define('L', ModItems.LATENT_SPORE.get())
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('B', Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(ModItems.CHITIN_PLATE.get()), has(ModItems.CHITIN_PLATE.get()))
                .save(output, modId("neutralizing_spore_launcher"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SYMBIOTIC_BLADE.get())
                .pattern(" H ")
                .pattern(" C ")
                .pattern(" D ")
                .define('H', ModItems.HIVE_HEART.get())
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('D', Items.DIAMOND_SWORD)
                .unlockedBy(getHasName(ModItems.HIVE_HEART.get()), has(ModItems.HIVE_HEART.get()))
                .save(output, modId("symbiotic_blade"));
    }

    private static void buildEndgameAndDiplomacyRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SYNAPTIC_SUPPRESSOR.get())
                .pattern(" S ")
                .pattern("PHP")
                .pattern(" D ")
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .define('P', ModBlocks.PURIFICATION_PYLON.get())
                .define('H', ModItems.HIVE_HEART.get())
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(ModItems.HIVE_HEART.get()), has(ModItems.HIVE_HEART.get()))
                .save(output, modId("synaptic_suppressor"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.RIFT_CATALYST.get())
                .pattern(" E ")
                .pattern("SHS")
                .pattern(" C ")
                .define('E', Items.ENDER_PEARL)
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('H', ModItems.HIVE_HEART.get())
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .unlockedBy(getHasName(ModItems.HIVE_HEART.get()), has(ModItems.HIVE_HEART.get()))
                .save(output, modId("rift_catalyst"));

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.OVERMIND_CROWN.get())
                .pattern(" O ")
                .pattern("DHD")
                .pattern(" S ")
                .define('O', ModItems.OVERMIND_ESSENCE.get())
                .define('D', Items.DIAMOND)
                .define('H', ModItems.HIVE_HEART.get())
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .unlockedBy(getHasName(ModItems.HIVE_HEART.get()), has(ModItems.HIVE_HEART.get()))
                .save(output, modId("overmind_crown"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.PURIFIER_DRONE_CORE.get())
                .pattern(" G ")
                .pattern("CPC")
                .pattern(" S ")
                .define('G', Items.GOLD_INGOT)
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('P', ModBlocks.PURIFICATION_PYLON.get())
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .unlockedBy(getHasName(ModItems.SYNAPTIC_CORE.get()), has(ModItems.SYNAPTIC_CORE.get()))
                .save(output, modId("purifier_drone_core"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.TIDE_BEACON.get())
                .requires(ModItems.TIDE_BEACON.get())
                .requires(ModItems.SYNAPTIC_CORE.get())
                .requires(ModItems.CHITIN_PLATE.get())
                .requires(ModItems.CHITIN_PLATE.get())
                .requires(Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(ModItems.SYNAPTIC_CORE.get()), has(ModItems.SYNAPTIC_CORE.get()))
                .save(output, modId("tide_beacon_recharge"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SYNAPTIC_GRAFT.get())
                .pattern(" E ")
                .pattern("CSC")
                .pattern(" L ")
                .define('E', ModItems.NEURAL_ECHO_CORE.get())
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .define('C', Items.COPPER_INGOT)
                .define('L', ModItems.LATENT_SPORE.get())
                .unlockedBy(getHasName(ModItems.SYNAPTIC_CORE.get()), has(ModItems.SYNAPTIC_CORE.get()))
                .save(output, modId("synaptic_graft"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.NEURAL_ECHO_DECOY.get(), 2)
                .pattern(" L ")
                .pattern("CEC")
                .pattern(" L ")
                .define('E', ModItems.NEURAL_ECHO_CORE.get())
                .define('C', Items.COPPER_INGOT)
                .define('L', ModItems.LATENT_SPORE.get())
                .unlockedBy(getHasName(ModItems.MYCELIAL_BIOMASS.get()), has(ModItems.MYCELIAL_BIOMASS.get()))
                .save(output, modId("neural_echo_decoy"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.TRUCE_PHEROMONE.get())
                .requires(ModItems.MYCELIAL_BIOMASS.get())
                .requires(ModItems.LATENT_SPORE.get())
                .requires(Items.GLASS_BOTTLE)
                .requires(Items.FERMENTED_SPIDER_EYE)
                .requires(Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(ModItems.MYCELIAL_BIOMASS.get()), has(ModItems.MYCELIAL_BIOMASS.get()))
                .save(output, modId("truce_pheromone"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SCHISM_INJECTOR.get())
                .pattern(" L ")
                .pattern("CEC")
                .pattern(" B ")
                .define('L', ModItems.LATENT_SPORE.get())
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('E', ModItems.NEURAL_ECHO_CORE.get())
                .define('B', Items.BLAZE_POWDER)
                .unlockedBy(getHasName(ModItems.LATENT_SPORE.get()), has(ModItems.LATENT_SPORE.get()))
                .save(output, modId("schism_injector"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.QUORUM_SEAL.get())
                .pattern(" C ")
                .pattern("ESA")
                .pattern(" C ")
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('E', ModItems.NEURAL_ECHO_CORE.get())
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .define('A', Items.AMETHYST_SHARD)
                .unlockedBy(getHasName(ModItems.SYNAPTIC_CORE.get()), has(ModItems.SYNAPTIC_CORE.get()))
                .save(output, modId("quorum_seal"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.CHIMERA_OVUM.get())
                .pattern(" B ")
                .pattern("GEG")
                .pattern(" S ")
                .define('B', ModItems.MYCELIAL_BIOMASS.get())
                .define('G', ModItems.GENETIC_SAMPLE.get())
                .define('E', ModItems.NEURAL_ECHO_CORE.get())
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .unlockedBy(getHasName(ModItems.SYNAPTIC_CORE.get()), has(ModItems.SYNAPTIC_CORE.get()))
                .save(output, modId("chimera_ovum"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.FALSE_ENGRAM_INJECTOR.get())
                .pattern(" V ")
                .pattern("CVC")
                .pattern(" L ")
                .define('V', ModItems.VOLATILE_ENGRAM.get())
                .define('C', Items.COPPER_INGOT)
                .define('L', ModItems.LATENT_SPORE.get())
                .unlockedBy(getHasName(ModItems.LATENT_SPORE.get()), has(ModItems.LATENT_SPORE.get()))
                .save(output, modId("false_engram_injector"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SOVEREIGN_NEXUS.get())
                .pattern("OCO")
                .pattern("HSN")
                .pattern("CPC")
                .define('O', ModItems.OVERMIND_ESSENCE.get())
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('H', ModItems.HIVE_HEART.get())
                .define('S', ModBlocks.SYMBIOTIC_NODE.get())
                .define('N', ModItems.NEURAL_ECHO_CORE.get())
                .define('P', ModBlocks.PURIFICATION_PYLON.get())
                .unlockedBy(getHasName(ModItems.HIVE_HEART.get()), has(ModItems.HIVE_HEART.get()))
                .save(output, modId("sovereign_nexus"));

        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.SOVEREIGN_COMMAND_STAFF.get())
                .pattern(" OG")
                .pattern(" NO")
                .pattern("C  ")
                .define('O', ModItems.OVERMIND_ESSENCE.get())
                .define('G', ModItems.SYNAPTIC_GRAFT.get())
                .define('N', ModItems.NEURAL_ECHO_CORE.get())
                .define('C', ModItems.CHITIN_PLATE.get())
                .unlockedBy(getHasName(ModItems.OVERMIND_ESSENCE.get()), has(ModItems.OVERMIND_ESSENCE.get()))
                .save(output, modId("sovereign_command_staff"));
    }

    private static void buildSinkholeAndConstructionRecipes(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.REINFORCED_BIO_BARRIER.get(), 2)
                .pattern("CPC")
                .pattern("PDP")
                .pattern("CPC")
                .define('C', ModItems.CHITIN_PLATE.get())
                .define('P', Items.OBSIDIAN)
                .define('D', ModItems.MYCELIAL_BIOMASS.get())
                .unlockedBy(getHasName(ModItems.CHITIN_PLATE.get()), has(ModItems.CHITIN_PLATE.get()))
                .save(output, modId("reinforced_bio_barrier"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.LUMINESCENT_AMBER_VEIN.get())
                .pattern("MLM")
                .pattern("GSG")
                .pattern("MLM")
                .define('M', ModBlocks.MYCELIAL_STONE.get())
                .define('L', ModItems.LATENT_SPORE.get())
                .define('G', Items.GLOWSTONE_DUST)
                .define('S', ModItems.SYNAPTIC_CORE.get())
                .unlockedBy(getHasName(ModItems.SYNAPTIC_CORE.get()), has(ModItems.SYNAPTIC_CORE.get()))
                .save(output, modId("luminescent_amber_vein"));

        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.ROOT_CAP_SHELF.get(), 2)
                .pattern("CMC")
                .pattern(" B ")
                .define('C', ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get())
                .define('M', ModItems.CANOPY_MEMBRANE.get())
                .define('B', ModItems.MYCELIAL_BIOMASS.get())
                .unlockedBy(getHasName(ModItems.CANOPY_MEMBRANE.get()), has(ModItems.CANOPY_MEMBRANE.get()))
                .save(output, modId("root_cap_shelf"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.SPORE_UPDRAFT_VENT.get())
                .pattern(" M ")
                .pattern("RVR")
                .pattern("SMS")
                .define('M', ModItems.CANOPY_MEMBRANE.get())
                .define('R', ModItems.ROOT_HEART_RESIN.get())
                .define('V', ModBlocks.SPORE_VENT.get())
                .define('S', ModBlocks.MYCELIAL_STONE.get())
                .unlockedBy(getHasName(ModItems.ROOT_HEART_RESIN.get()), has(ModItems.ROOT_HEART_RESIN.get()))
                .save(output, modId("spore_updraft_vent"));
    }

    private static void buildCookingAndStonecuttingRecipes(RecipeOutput output) {
        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()),
                        RecipeCategory.MISC,
                        ModItems.LATENT_SPORE.get(),
                        0.15F,
                        200
                )
                .unlockedBy(getHasName(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()), has(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()))
                .save(output, modId("smelt_cap_to_latent_spore"));

        SimpleCookingRecipeBuilder.smoking(
                        Ingredient.of(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()),
                        RecipeCategory.MISC,
                        ModItems.LATENT_SPORE.get(),
                        0.15F,
                        100
                )
                .unlockedBy(getHasName(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()), has(ModBlocks.BIOLUMINESCENT_MYCELIAL_CAP.get()))
                .save(output, modId("smoke_cap_to_latent_spore"));

        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ModBlocks.CREEPING_MYCELIUM.get()),
                        RecipeCategory.BUILDING_BLOCKS,
                        ModBlocks.MYCELIAL_STONE.get(),
                        0.10F,
                        200
                )
                .unlockedBy(getHasName(ModBlocks.CREEPING_MYCELIUM.get()), has(ModBlocks.CREEPING_MYCELIUM.get()))
                .save(output, modId("smelt_creeping_mycelium_to_stone"));

        SimpleCookingRecipeBuilder.smelting(
                        Ingredient.of(ModBlocks.ANTIBODY_CYST.get()),
                        RecipeCategory.MISC,
                        ModItems.CHITIN_PLATE.get(),
                        0.20F,
                        200
                )
                .unlockedBy(getHasName(ModBlocks.ANTIBODY_CYST.get()), has(ModBlocks.ANTIBODY_CYST.get()))
                .save(output, modId("smelt_cyst_to_chitin_plate"));

        SimpleCookingRecipeBuilder.blasting(
                        Ingredient.of(ModBlocks.ANTIBODY_CYST.get()),
                        RecipeCategory.MISC,
                        ModItems.CHITIN_PLATE.get(),
                        0.20F,
                        100
                )
                .unlockedBy(getHasName(ModBlocks.ANTIBODY_CYST.get()), has(ModBlocks.ANTIBODY_CYST.get()))
                .save(output, modId("blast_cyst_to_chitin_plate"));

        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(ModBlocks.MYCELIAL_STONE.get()),
                        RecipeCategory.BUILDING_BLOCKS,
                        ModBlocks.MYCELIAL_ARCH_BRICK.get()
                )
                .unlockedBy(getHasName(ModBlocks.MYCELIAL_STONE.get()), has(ModBlocks.MYCELIAL_STONE.get()))
                .save(output, modId("stonecut_mycelial_stone_to_arch_brick"));

        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(ModBlocks.REINFORCED_BIO_BARRIER.get()),
                        RecipeCategory.BUILDING_BLOCKS,
                        ModBlocks.CHITIN_PILLAR.get()
                )
                .unlockedBy(getHasName(ModBlocks.REINFORCED_BIO_BARRIER.get()), has(ModBlocks.REINFORCED_BIO_BARRIER.get()))
                .save(output, modId("stonecut_arch_brick_to_chitin_pillar"));

        SingleItemRecipeBuilder.stonecutting(
                        Ingredient.of(ModItems.CHITIN_PLATE.get()),
                        RecipeCategory.BUILDING_BLOCKS,
                        ModBlocks.BIO_CHITIN_LATTICE.get()
                )
                .unlockedBy(getHasName(ModItems.CHITIN_PLATE.get()), has(ModItems.CHITIN_PLATE.get()))
                .save(output, modId("stonecut_chitin_plate_to_lattice"));
    }
}
