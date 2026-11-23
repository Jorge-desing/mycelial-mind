package com.alex.mycelialmind.client.screen;

import net.minecraft.client.Minecraft;

/**
 * Client-only bridge to open SovereignNexusScreen safely without loading Screen classes on dedicated servers.
 */
public final class ClientColonyOpener {
    private ClientColonyOpener() {
    }

    public static void openSovereignNexusScreen() {
        Minecraft.getInstance().setScreen(new SovereignNexusScreen());
    }
}
