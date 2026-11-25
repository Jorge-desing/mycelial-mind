package com.alex.mycelialmind.client.audio;

import com.alex.mycelialmind.client.ClientSoundscapeDirector;
import com.alex.mycelialmind.config.MycelialClientConfig;

import java.util.Locale;

/**
 * Isolated client-side telemetry formatter invoked lazily from {@code /mycelial audio status}
 * only when running on a physical client distribution.
 */
public final class ClientAudioDiagnostics {
    private ClientAudioDiagnostics() {
    }

    public static String formatClientTelemetry() {
        return String.format(
                Locale.ROOT,
                "EFX=%s (%s, slots=%d, applied=%d, dynUpdates=%d, tracked=%d, vanillaZones=%s) | Env=%s (enc=%.2f, wall=%.1fm, sinkhole=%s, submersion=%s, occ=%.2f) | VoiceBudget=%s (acc=%d, sup=%d, win=%d) | ActiveLoops=%d",
                OpenAlEfxBridge.isEfxSupported() && MycelialClientConfig.openAlEfxAcousticsEnabled(),
                OpenAlEfxBridge.getStatusReason(),
                OpenAlEfxBridge.getActiveSlotCount(),
                OpenAlEfxBridge.getAppliedSourcesCount(),
                OpenAlEfxBridge.getDynamicUpdatesCount(),
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
                ClientAcousticController.getActiveVoiceWindowCount(),
                ClientSoundscapeDirector.getActiveLoopCount()
        );
    }
}
