package com.alex.mycelialmind.client;

import com.alex.mycelialmind.config.MycelialClientConfig;
import com.alex.mycelialmind.registry.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;

/**
 * Subtle client-side UI sound cues for screen navigation and confirmations.
 */
public final class ClientUiAudio {
    private ClientUiAudio() {
    }

    public static void playTabClick() {
        playUiCue(ModSounds.UI_CODEX_TAB.get(), 1.0F, 0.34F);
    }

    public static void playActionConfirm() {
        playUiCue(ModSounds.UI_ACTION_CONFIRM.get(), 1.0F, 0.38F);
    }

    private static void playUiCue(SoundEvent sound, float pitch, float volume) {
        if (!MycelialClientConfig.uiAudioFeedbackEnabled()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }
}
