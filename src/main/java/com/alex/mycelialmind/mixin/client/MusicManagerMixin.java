package com.alex.mycelialmind.mixin.client;

import com.alex.mycelialmind.client.MycelialClientMixinHooks;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Client-only hook cleanly deferring vanilla music tracks during high-tension Mycelial soundscape peaks
 * (nearby bosses, active Fungal Tide outside Pylons, or elevated Deep Mycelium homeostasis).
 */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @WrapOperation(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ClientHooks;selectMusic(Lnet/minecraft/sounds/Music;Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/sounds/Music;"))
    private Music mycelialmind$suppressVanillaMusicDuringTenseSoundscape(
            Music music, SoundInstance currentMusic, Operation<Music> original) {
        if (MycelialClientMixinHooks.shouldSuppressVanillaMusic(this.minecraft)) {
            return null;
        }
        return original.call(music, currentMusic);
    }
}
