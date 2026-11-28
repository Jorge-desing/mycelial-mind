package com.alex.mycelialmind.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.sensing.Sensing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Typed Mixin accessor exposing Mob AI selectors, Sensing cache, and ambient timer reset without reflection.
 */
@Mixin(Mob.class)
public interface MobAccessor {

    @Accessor("goalSelector")
    GoalSelector mycelialmind$getGoalSelector();

    @Accessor("targetSelector")
    GoalSelector mycelialmind$getTargetSelector();

    @Accessor("sensing")
    Sensing mycelialmind$getSensing();

    @Invoker("resetAmbientSoundTime")
    void mycelialmind$invokeResetAmbientSoundTime();
}
