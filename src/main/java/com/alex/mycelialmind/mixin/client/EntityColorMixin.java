package com.alex.mycelialmind.mixin.client;

import com.alex.mycelialmind.client.MycelialClientMixinHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client-only hook tinting Synaptic Vision glow outlines by threat tier and faction role
 * while preserving explicit vanilla scoreboard teams and disguised Synaptic Mimic counterplay.
 */
@Mixin(Entity.class)
public abstract class EntityColorMixin {

    @ModifyReturnValue(method = "getTeamColor", at = @At("RETURN"))
    private int mycelialmind$applySynapticVisionTeamColor(int original) {
        return MycelialClientMixinHooks.resolveEntityTeamColor((Entity) (Object) this, original);
    }
}
