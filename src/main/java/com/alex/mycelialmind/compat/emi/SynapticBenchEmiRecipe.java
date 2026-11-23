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
import java.util.ArrayList;

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
                60
        );
        this.descriptor = descriptor;
        this.inputs = new ArrayList<>();
        this.inputs.add(EmiIngredient.of(descriptor.sampleInputs().stream().map(EmiStack::of).toList()));
        this.inputs.add(EmiIngredient.of(descriptor.biomassInputs().stream().map(EmiStack::of).toList()));
        if (!descriptor.catalystInputs().isEmpty()) {
            this.inputs.add(EmiIngredient.of(descriptor.catalystInputs().stream().map(EmiStack::of).toList()));
        }
        this.outputs = descriptor.outputAlternatives().stream().map(EmiStack::of).toList();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(this.inputs.get(0), 5, 7);
        widgets.addSlot(this.inputs.get(1), 27, 7);
        if (this.inputs.size() > 2) widgets.addSlot(this.inputs.get(2), 49, 7);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 77, 8);
        widgets.addSlot(this.outputs.get(0), 111, 3).large(true).recipeContext(this);
        var lines = net.minecraft.client.Minecraft.getInstance().font.split(Component.translatable(this.descriptor.noteKey()), 138);
        for (int i = 0; i < lines.size(); i++) {
            widgets.addText(lines.get(i), 4, 33 + i * 9, 0xFF3F3F3F, false);
        }
    }
}
