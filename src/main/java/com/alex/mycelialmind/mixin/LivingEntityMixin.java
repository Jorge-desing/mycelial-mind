package com.alex.mycelialmind.mixin;

import com.alex.mycelialmind.util.MycelialMixinHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import javax.annotation.Nullable;

/**
 * MixinExtras hooks on LivingEntity for pheromone Chitin camouflage and cushioned mycelial landings
 * with strict anti-cheese guardrails.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyReturnValue(method = "getVisibilityPercent", at = @At("RETURN"))
    private double mycelialmind$adjustVisibilityPercent(double original, @Nullable Entity lookingEntity) {
        return MycelialMixinHooks.adjustVisibilityPercent((LivingEntity) (Object) this, lookingEntity, original);
    }

    @ModifyReturnValue(method = "calculateFallDamage", at = @At("RETURN"))
    private int mycelialmind$softenMycelialFallDamage(int original, float fallDistance, float damageMultiplier) {
        return MycelialMixinHooks.adjustFallDamage((LivingEntity) (Object) this, original, fallDistance);
    }
}
