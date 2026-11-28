package com.alex.mycelialmind.mixin.client;

import com.alex.mycelialmind.client.MycelialClientMixinHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.gui.Gui;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client-only diegetic heart styling: replaces vanilla NORMAL hearts with WITHERED under SPORE_ROT
 * and POISIONED under MYCELIAL_INFECTION without overriding absorbing, frozen, or already special states.
 */
@Mixin(Gui.HeartType.class)
public abstract class GuiHeartTypeMixin {

    @ModifyReturnValue(method = "forPlayer", at = @At("RETURN"))
    private static Gui.HeartType mycelialmind$applyMycelialHeartType(Gui.HeartType original, Player player) {
        return MycelialClientMixinHooks.resolveHeartType(player, original);
    }
}
