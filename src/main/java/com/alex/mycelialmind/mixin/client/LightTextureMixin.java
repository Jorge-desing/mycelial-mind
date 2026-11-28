package com.alex.mycelialmind.mixin.client;

import com.alex.mycelialmind.client.MycelialClientMixinHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client-only lightmap clarity under Synaptic Vision: enters the night-vision branch with a subtle
 * 0.35F floor and linear 100-tick fade-out while preserving stronger vanilla Night Vision.
 */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {

    @WrapOperation(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;hasEffect(Lnet/minecraft/core/Holder;)Z",
                    ordinal = 0))
    private boolean mycelialmind$hasNightVisionOrSynapticVision(
            LocalPlayer player, Holder<MobEffect> effect, Operation<Boolean> original) {
        boolean vanillaHasEffect = original.call(player, effect);
        if (!MobEffects.NIGHT_VISION.equals(effect)) {
            return vanillaHasEffect;
        }
        return MycelialClientMixinHooks.hasNightVisionOrSynapticVision(player, vanillaHasEffect);
    }

    @WrapOperation(
            method = "updateLightTexture",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;getNightVisionScale(Lnet/minecraft/world/entity/LivingEntity;F)F"))
    private float mycelialmind$adjustSynapticNightVisionScale(
            LivingEntity entity, float partialTicks, Operation<Float> original) {
        float vanillaScale = entity != null && entity.hasEffect(MobEffects.NIGHT_VISION)
                ? original.call(entity, partialTicks)
                : 0.0F;
        return MycelialClientMixinHooks.resolveNightVisionScale(entity, partialTicks, vanillaScale);
    }
}
