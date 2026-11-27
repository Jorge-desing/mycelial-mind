package com.alex.mycelialmind.registry;

import com.alex.mycelialmind.MycelialMind;
import com.alex.mycelialmind.recipe.SynapticSynthesisRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom NeoForge RecipeTypes and RecipeSerializers for bio-synthesis stations.
 */
public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, MycelialMind.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, MycelialMind.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<SynapticSynthesisRecipe>> SYNAPTIC_SYNTHESIS_TYPE =
            RECIPE_TYPES.register("synaptic_synthesis", () -> RecipeType.simple(
                    ResourceLocation.fromNamespaceAndPath(MycelialMind.MODID, "synaptic_synthesis")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SynapticSynthesisRecipe>> SYNAPTIC_SYNTHESIS_SERIALIZER =
            RECIPE_SERIALIZERS.register("synaptic_synthesis", SynapticSynthesisRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
    }
}
