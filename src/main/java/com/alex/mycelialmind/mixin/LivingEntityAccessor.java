package com.alex.mycelialmind.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Typed Mixin accessor exposing LivingEntity combat timestamps and block speed evaluation without reflection.
 */
@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {

    @Accessor("lastHurtByMobTimestamp")
    int mycelialmind$getLastHurtByMobTimestamp();

    @Accessor("lastHurtMobTimestamp")
    int mycelialmind$getLastHurtMobTimestamp();

    @Invoker("getBlockSpeedFactor")
    float mycelialmind$invokeGetBlockSpeedFactor();

    @Invoker("decreaseAirSupply")
    int mycelialmind$invokeDecreaseAirSupply(int currentAir);
}
