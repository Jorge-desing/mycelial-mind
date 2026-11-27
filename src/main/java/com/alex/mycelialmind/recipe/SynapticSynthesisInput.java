package com.alex.mycelialmind.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

/**
 * Three-reagent container input view passed into {@link SynapticSynthesisRecipe} matching.
 */
public record SynapticSynthesisInput(ItemStack sample, ItemStack biomass, ItemStack catalyst) implements RecipeInput {
    @Override
    public ItemStack getItem(int index) {
        return switch (index) {
            case 0 -> sample;
            case 1 -> biomass;
            case 2 -> catalyst;
            default -> throw new IllegalArgumentException("Invalid synaptic synthesis slot: " + index);
        };
    }

    @Override
    public int size() {
        return 3;
    }

    @Override
    public boolean isEmpty() {
        return sample.isEmpty() && biomass.isEmpty() && catalyst.isEmpty();
    }
}
