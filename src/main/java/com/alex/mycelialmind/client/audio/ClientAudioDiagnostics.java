package com.alex.mycelialmind.client.audio;

import com.alex.mycelialmind.client.ClientSoundscapeDirector;
import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.mixin.client.SoundEngineAccessor;
import com.alex.mycelialmind.mixin.client.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;

import java.util.Locale;
import java.util.Map;

/**
 * Isolated client-side telemetry formatter invoked lazily from {@code /mycelial audio status}
 * only when running on a physical client distribution.
 */
public final class ClientAudioDiagnostics {
    private ClientAudioDiagnostics() {
    }

    public static String formatClientTelemetry() {
        Minecraft minecraft = Minecraft.getInstance();
        boolean soundEngineLoaded = false;
        int activeSoundChannels = 0;
        if (minecraft != null && minecraft.getSoundManager() != null) {
            SoundEngine soundEngine = ((SoundManagerAccessor) minecraft.getSoundManager()).mycelialmind$getSoundEngine();
            if (soundEngine != null) {
                SoundEngineAccessor engineAccessor = (SoundEngineAccessor) soundEngine;
                soundEngineLoaded = engineAccessor.mycelialmind$isLoaded();
                Map<SoundInstance, ChannelAccess.ChannelHandle> channels = engineAccessor.mycelialmind$getInstanceToChannel();
                if (channels != null) {
                    activeSoundChannels = channels.size();
                }
            }
        }
        return String.format(
                Locale.ROOT,
                "EFX=%s (%s, slots=%d, lpf=%d, applied=%d, humanized=%d, dynUpdates=%d, revUpd=%d, revScale=%.2f, occScale=%.2f, tracked=%d, vanillaZones=%s) | Env=%s (enc=%.2f, wall=%.1fm, sinkhole=%s, submersion=%s, occ=%.2f) | VoiceBudget=%s (acc=%d, sup=%d, stolen=%d, win=%d) | ActiveLoops=%d | SoundEngine=(loaded=%s, channels=%d)",
                OpenAlEfxBridge.isEfxSupported() && MycelialClientConfig.openAlEfxAcousticsEnabled(),
                OpenAlEfxBridge.getStatusReason(),
                OpenAlEfxBridge.getActiveSlotCount(),
                OpenAlEfxBridge.getOcclusionBucketCount(),
                OpenAlEfxBridge.getAppliedSourcesCount(),
                ClientAcousticController.getHumanizedPlaybackCount(),
                OpenAlEfxBridge.getDynamicUpdatesCount(),
                OpenAlEfxBridge.getReverbGainUpdatesCount(),
                MycelialClientConfig.openAlEfxReverbScale(),
                MycelialClientConfig.openAlEfxOcclusionScale(),
                ClientAcousticController.getTrackedChannelCount(),
                MycelialClientConfig.openAlEfxVanillaInMycelialZones(),
                ClientAcousticController.getCurrentEnvironment().name(),
                ClientAcousticController.getLastEnclosureRatio(),
                ClientAcousticController.getLastAverageWallDistance(),
                ClientAcousticController.isPlayerNearSinkhole(),
                ClientAcousticController.getCurrentSubmersionState().name(),
                ClientAcousticController.getLastComputedOcclusion(),
                MycelialClientConfig.swarmVoiceLimitingEnabled(),
                ClientAcousticController.getVoiceAcceptedCount(),
                ClientAcousticController.getVoiceSuppressedCount(),
                ClientAcousticController.getVoiceStolenCount(),
                ClientAcousticController.getActiveVoiceWindowCount(),
                ClientSoundscapeDirector.getActiveLoopCount(),
                soundEngineLoaded,
                activeSoundChannels
        );
    }
}
