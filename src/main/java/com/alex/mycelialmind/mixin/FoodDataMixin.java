package com.alex.mycelialmind.mixin;

import com.alex.mycelialmind.util.MycelialMixinHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Scales passive food-based natural regeneration by 50% when the player has SPORE_ROT outside active
 * Purification Pylons, without affecting potions, enchanted apples, or antidotes.
 */
@Mixin(FoodData.class)
public abstract class FoodDataMixin {

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void mycelialmind$scaleSporeRotNaturalHeal(Player player, float healAmount, Operation<Void> original) {
        float adjustedHeal = MycelialMixinHooks.adjustNaturalFoodHeal(player, healAmount);
        if (adjustedHeal > 0.0F) {
            original.call(player, adjustedHeal);
        }
    }
}
