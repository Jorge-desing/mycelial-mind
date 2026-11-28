package com.alex.mycelialmind.mixin.client;

import com.alex.mycelialmind.client.MycelialClientMixinHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client-only hook giving local Synaptic Vision users bounded glowing outlines for nearby Mycelial mobs
 * while preserving Synaptic Mimic's camouflage counterplay beyond 6 blocks.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @ModifyReturnValue(method = "shouldEntityAppearGlowing", at = @At("RETURN"))
    private boolean mycelialmind$applySynapticVisionGlow(boolean original, Entity entity) {
        return MycelialClientMixinHooks.shouldEntityAppearGlowing((Minecraft) (Object) this, entity, original);
    }
}
