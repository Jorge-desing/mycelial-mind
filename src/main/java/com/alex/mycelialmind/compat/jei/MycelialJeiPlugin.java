package com.alex.mycelialmind.compat.jei;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.recipe.SynapticBenchRecipeDefinitions;
import com.alex.mycelialmind.registry.ModBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Optional JEI integration exposing Synaptic Research Bench synthesis recipes
 * and localized progression item info.
 */
@JeiPlugin
public final class MycelialJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new SynapticBenchJeiCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(ModBlocks.SYNAPTIC_RESEARCH_BENCH.get(), SynapticBenchJeiCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(SynapticBenchJeiCategory.RECIPE_TYPE, SynapticBenchRecipeDefinitions.allRecipes());

        for (var entry : SynapticBenchRecipeDefinitions.allCompatInfoEntries()) {
            registration.addIngredientInfo(entry.item(), Component.translatable(entry.translationKey()));
        }
    }
}
