package com.alex.mycelialmind.client;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * Registers NeoForge's built-in configuration screen for Mycelial Mind on the client dist.
 * Kept in its own class so dedicated servers and headless unit tests never touch Screen classes.
 */
public final class ClientConfigScreenRegistrar {
    private ClientConfigScreenRegistrar() {
    }

    public static void register(ModContainer modContainer) {
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
