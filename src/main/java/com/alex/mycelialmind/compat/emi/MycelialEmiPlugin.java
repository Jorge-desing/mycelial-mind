package com.alex.mycelialmind.compat.emi;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.recipe.SynapticBenchRecipeDefinitions;
import com.alex.mycelialmind.registry.ModBlocks;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.List;

/**
 * Optional EMI integration exposing Synaptic Bench 3->1 formulas and localized progression info.
 */
@EmiEntrypoint
public final class MycelialEmiPlugin implements EmiPlugin {
    public static final ResourceLocation CATEGORY_ID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "synaptic_research");
    public static final EmiRecipeCategory SYNAPTIC_CATEGORY =
            new EmiRecipeCategory(CATEGORY_ID, EmiStack.of(ModBlocks.SYNAPTIC_RESEARCH_BENCH.get()));

    @Override
    public void register(EmiRegistry registry) {
        registry.addCategory(SYNAPTIC_CATEGORY);
        registry.addWorkstation(SYNAPTIC_CATEGORY, EmiStack.of(ModBlocks.SYNAPTIC_RESEARCH_BENCH.get()));

        for (var descriptor : SynapticBenchRecipeDefinitions.allRecipes()) {
            registry.addRecipe(new SynapticBenchEmiRecipe(descriptor));
        }

        for (var entry : SynapticBenchRecipeDefinitions.allCompatInfoEntries()) {
            addInfo(registry, entry.item(), entry.idPath(), entry.translationKey());
        }
    }

    private static void addInfo(EmiRegistry registry, ItemLike item, String idPath, String translationKey) {
        registry.addRecipe(new EmiInfoRecipe(
                List.of(EmiIngredient.of(Ingredient.of(item))),
                List.of(Component.translatable(translationKey)),
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "/info/" + idPath)
        ));
    }
}
