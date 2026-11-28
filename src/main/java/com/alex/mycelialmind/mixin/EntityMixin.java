package com.alex.mycelialmind.mixin;

import com.alex.mycelialmind.util.MycelialMixinHooks;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * MixinExtras hook on Entity providing bounded mycelial traction across colonized blocks.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @ModifyReturnValue(method = "getBlockSpeedFactor", at = @At("RETURN"))
    private float mycelialmind$applyMycelialTraction(float original) {
        return MycelialMixinHooks.adjustBlockSpeedFactor((Entity) (Object) this, original);
    }
}
