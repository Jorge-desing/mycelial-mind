package com.alex.mycelialmind.compat.emi;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.recipe.SynapticBenchRecipeDefinitions.SynapticRecipeDescriptor;
import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * EMI recipe wrapper representing a 3-reagent Synaptic Research Bench synthesis formula.
 */
public final class SynapticBenchEmiRecipe extends BasicEmiRecipe {
    private final SynapticRecipeDescriptor descriptor;

    public SynapticBenchEmiRecipe(SynapticRecipeDescriptor descriptor) {
        super(
                MycelialEmiPlugin.SYNAPTIC_CATEGORY,
                ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "/synaptic/" + descriptor.id()),
                146,
                46
        );
        this.descriptor = descriptor;
        this.inputs = List.of(
                EmiIngredient.of(descriptor.sampleInputs().stream().map(EmiStack::of).toList()),
                EmiIngredient.of(descriptor.biomassInputs().stream().map(EmiStack::of).toList()),
                EmiIngredient.of(descriptor.catalystInputs().stream().map(EmiStack::of).toList())
        );
        this.outputs = descriptor.outputAlternatives().stream().map(EmiStack::of).toList();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(this.inputs.get(0), 5, 7);
        widgets.addSlot(this.inputs.get(1), 27, 7);
        widgets.addSlot(this.inputs.get(2), 49, 7);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 77, 8);
        widgets.addSlot(this.outputs.get(0), 111, 3).large(true).recipeContext(this);
        widgets.addText(Component.translatable(this.descriptor.noteKey()), 4, 33, 0xFF3F3F3F, false);
    }
}
