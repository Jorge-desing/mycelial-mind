package com.alex.mycelialmind.client;

import com.alex.mycelialmind.MycelialMind;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side KeyMappings for quick Codex access and compact HUD toggling.
 */
@EventBusSubscriber(modid = MycelialMind.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class MycelialKeyBindings {
    public static final KeyMapping OPEN_CODEX = new KeyMapping(
            "key.mycelialmind.open_codex",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.mycelialmind"
    );

    public static final KeyMapping TOGGLE_HUD_COMPACT = new KeyMapping(
            "key.mycelialmind.toggle_hud_compact",
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            "key.categories.mycelialmind"
    );

    private MycelialKeyBindings() {
    }

    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_CODEX);
        event.register(TOGGLE_HUD_COMPACT);
    }
}
