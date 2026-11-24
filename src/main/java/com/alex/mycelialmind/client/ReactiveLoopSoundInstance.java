package com.alex.mycelialmind.client;

import com.alex.mycelialmind.hive.EvolutionRank;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Smoothly fading, optionally positional looping SoundInstance driven by {@link ClientSoundscapeDirector}.
 */
public final class ReactiveLoopSoundInstance extends AbstractTickableSoundInstance {
    private float targetVolume;
    private float targetPitch;
    private final float maxStepPerTick;
    private boolean fadeOutAndStop = false;

    public ReactiveLoopSoundInstance(
            SoundEvent soundEvent,
            SoundSource source,
            boolean positional,
            float initialPitch,
            float maxStepPerTick
    ) {
        super(soundEvent, source, SoundInstance.createUnseededRandom());
        this.looping = true;
        this.delay = 0;
        this.attenuation = positional ? Attenuation.LINEAR : Attenuation.NONE;
        this.relative = !positional;
        this.volume = 0.008F;
        this.targetVolume = 0.008F;
        this.pitch = initialPitch;
        this.targetPitch = initialPitch;
        this.maxStepPerTick = Math.max(0.002F, maxStepPerTick);
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void updateTarget(float targetVol, float targetP) {
        this.targetVolume = Math.max(0.0F, Math.min(1.5F, targetVol));
        this.targetPitch = Math.max(0.5F, Math.min(2.0F, targetP));
        this.fadeOutAndStop = this.targetVolume <= 0.004F;
    }

    public void requestStop() {
        this.targetVolume = 0.0F;
        this.fadeOutAndStop = true;
    }

    public void forceStopImmediate() {
        this.volume = 0.0F;
        this.targetVolume = 0.0F;
        this.fadeOutAndStop = true;
        this.stop();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        this.volume = EvolutionRank.computeSmoothFadeVolume(this.volume, this.targetVolume, this.maxStepPerTick);
        this.pitch = EvolutionRank.computeSmoothFadeVolume(this.pitch, this.targetPitch, 0.02F);
        if (this.fadeOutAndStop && this.volume <= 0.004F) {
            this.stop();
        }
    }
}
