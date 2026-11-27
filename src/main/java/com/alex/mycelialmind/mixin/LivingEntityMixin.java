package com.alex.mycelialmind.mixin;

import com.alex.mycelialmind.util.MixinRuntimeProbe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Base Mixin + MixinExtras verification hook confirming runtime bytecode injection on LivingEntity
 * while preserving vanilla visibility behavior unchanged.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @ModifyReturnValue(method = "getVisibilityPercent", at = @At("RETURN"))
    private double mycelialmind$probeVisibilityPercent(double original) {
        MixinRuntimeProbe.markLivingEntityHookInvoked();
        return original;
    }
}
