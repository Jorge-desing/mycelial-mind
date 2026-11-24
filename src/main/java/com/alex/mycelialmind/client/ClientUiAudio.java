package com.alex.mycelialmind.client;

import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

/**
 * Subtle client-side UI sound cues for screen navigation and confirmations.
 */
public final class ClientUiAudio {
    private ClientUiAudio() {
    }

    public static void playTabClick() {
        playPing(1.45F, 0.25F);
    }

    public static void playActionConfirm() {
        playPing(1.75F, 0.30F);
    }

    private static void playPing(float pitch, float volume) {
        if (!MycelialClientConfig.uiAudioFeedbackEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.BIO_SCANNER_PING.get(), pitch, volume));
        }
    }
}
