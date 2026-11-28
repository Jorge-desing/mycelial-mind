package com.alex.mycelialmind.mixin.client;

import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

/**
 * Client-only typed Mixin accessor exposing SoundEngine readiness and active channel handles without reflection.
 */
@Mixin(SoundEngine.class)
public interface SoundEngineAccessor {

    @Accessor("loaded")
    boolean mycelialmind$isLoaded();

    @Accessor("instanceToChannel")
    Map<SoundInstance, ChannelAccess.ChannelHandle> mycelialmind$getInstanceToChannel();
}
