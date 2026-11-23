package com.alex.mycelialmind.compat.jei;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.recipe.SynapticBenchRecipeDefinitions.SynapticRecipeDescriptor;
import com.alex.mycelialmind.registry.ModBlocks;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * JEI recipe category displaying the 3-reagent synthesis formulas of the Synaptic Research Bench.
 */
public final class SynapticBenchJeiCategory implements IRecipeCategory<SynapticRecipeDescriptor> {
    public static final RecipeType<SynapticRecipeDescriptor> RECIPE_TYPE =
            RecipeType.create(MycelialMind.MODID, "synaptic_research", SynapticRecipeDescriptor.class);

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public SynapticBenchJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(ModBlocks.SYNAPTIC_RESEARCH_BENCH.get());
        this.arrow = guiHelper.getRecipeArrow();
    }

    @Override
    public RecipeType<SynapticRecipeDescriptor> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.mycelialmind.synaptic_research");
    }

    @Override
    public int getWidth() {
        return 146;
    }

    @Override
    public int getHeight() {
        return 46;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, SynapticRecipeDescriptor recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 8)
                .setStandardSlotBackground()
                .addItemStacks(recipe.sampleInputs());
        builder.addSlot(RecipeIngredientRole.INPUT, 28, 8)
                .setStandardSlotBackground()
                .addItemStacks(recipe.biomassInputs());
        if (!recipe.catalystInputs().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 50, 8)
                    .setStandardSlotBackground()
                    .addItemStacks(recipe.catalystInputs());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 116, 8)
                .setOutputSlotBackground()
                .addItemStacks(recipe.outputAlternatives());
    }

    @Override
    public void draw(SynapticRecipeDescriptor recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        arrow.draw(guiGraphics, 78, 8);
        guiGraphics.drawString(Minecraft.getInstance().font,
                Component.translatable(recipe.noteKey()),
                4, 33, 0xFF3F3F3F, false);
    }
}
