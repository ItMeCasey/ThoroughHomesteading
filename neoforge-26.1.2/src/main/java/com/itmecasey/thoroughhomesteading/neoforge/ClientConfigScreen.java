package com.itmecasey.thoroughhomesteading.neoforge;

import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Registers NeoForge's own configuration screen for the mod. Only loaded on a client. */
final class ClientConfigScreen {
    private ClientConfigScreen() {
    }

    static void register(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
