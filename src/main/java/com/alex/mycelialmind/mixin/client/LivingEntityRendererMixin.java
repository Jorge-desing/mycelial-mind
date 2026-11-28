package com.alex.mycelialmind.mixin.client;

import com.alex.mycelialmind.client.MycelialClientMixinHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client-only hook adding diegetic entity shiver under Spore Rot or for non-boss Mycelial mobs
 * while the Hive Mind is under Synaptic Suppression.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @ModifyReturnValue(method = "isShaking", at = @At("RETURN"))
    private boolean mycelialmind$applyDiegeticShake(boolean original, LivingEntity entity) {
        return MycelialClientMixinHooks.shouldEntityShake(entity, original);
    }
}
