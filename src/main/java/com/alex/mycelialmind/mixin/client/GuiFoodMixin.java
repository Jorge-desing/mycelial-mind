package com.alex.mycelialmind.mixin.client;

import com.alex.mycelialmind.client.MycelialClientMixinHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Gui;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client-only diegetic HUD feedback mirroring Spore Rot's natural healing penalty onto food bar sprites
 * outside active Purification Pylons.
 */
@Mixin(Gui.class)
public abstract class GuiFoodMixin {

    @WrapOperation(
            method = "renderFood",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;hasEffect(Lnet/minecraft/core/Holder;)Z"))
    private boolean mycelialmind$showSporeRotFoodSprites(
            Player player, Holder<MobEffect> effect, Operation<Boolean> original) {
        boolean vanillaHasEffect = original.call(player, effect);
        if (!MobEffects.HUNGER.equals(effect)) {
            return vanillaHasEffect;
        }
        return MycelialClientMixinHooks.hasHungerOrSporeRotFoodPenalty(player, vanillaHasEffect);
    }
}
