package com.alex.mycelialmind.client.screen;

import net.minecraft.client.Minecraft;

/**
 * Client-only bridge to open BioScannerCodexScreen without loading Screen classes on dedicated servers.
 */
public final class ClientCodexOpener {
    private ClientCodexOpener() {
    }

    public static void openBioCodex() {
        Minecraft.getInstance().setScreen(new BioScannerCodexScreen());
    }

    public static void openBookCodex() {
        Minecraft.getInstance().setScreen(new MycelialCodexBookScreen());
    }

    public static void openCycleProtocolScreen() {
        Minecraft.getInstance().setScreen(new CycleProtocolScreen());
    }
}
