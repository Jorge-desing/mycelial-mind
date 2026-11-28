package com.alex.mycelialmind.mixin.client;

import com.mojang.blaze3d.audio.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Client-only typed Mixin accessor exposing the native OpenAL AL source handle from Channel without reflection.
 */
@Mixin(Channel.class)
public interface ChannelAccessor {

    @Accessor("source")
    int mycelialmind$getSource();
}
