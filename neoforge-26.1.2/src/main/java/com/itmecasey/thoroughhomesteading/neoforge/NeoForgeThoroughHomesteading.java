package com.itmecasey.thoroughhomesteading.neoforge;

import com.itmecasey.thoroughhomesteading.Dev;
import com.itmecasey.thoroughhomesteading.ModContent;
import com.itmecasey.thoroughhomesteading.ThoroughHomesteading;
import com.itmecasey.thoroughhomesteading.script.Bridges;
import com.itmecasey.thoroughhomesteading.script.ScriptHost;
import net.minecraft.core.registries.Registries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** NeoForge entry point for Minecraft 26.x. Registers the content and sends the settling recipes to clients. */
@Mod(ThoroughHomesteading.MODID)
public final class NeoForgeThoroughHomesteading {
    public NeoForgeThoroughHomesteading(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, HomesteadingConfig.SPEC, "thoroughhomesteading-common.toml");
        modBus.addListener((ModConfigEvent.Loading e) -> onConfig(e.getConfig()));
        modBus.addListener((ModConfigEvent.Reloading e) -> onConfig(e.getConfig()));
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            ClientConfigScreen.register(container);
            NeoForgeClient.install();
        }
        Bridges.install(FMLPaths.GAMEDIR.get());
        modBus.addListener(NeoForgeThoroughHomesteading::register);
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> e.sendRecipes(ModContent.SETTLING_TYPE));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> ScriptHost.start(e.getServer()));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> ScriptHost.stop());
        Dev.call("com.itmecasey.thoroughhomesteading.neoforge.dev.NeoForgeDev", "install", modBus);
    }

    private static void onConfig(ModConfig config) {
        if (config.getSpec() == HomesteadingConfig.SPEC) {
            HomesteadingConfig.push();
        }
    }

    private static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> ModContent.registerBlocks((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.ITEM, helper -> ModContent.registerItems((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> ModContent.registerBlockEntities((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.RECIPE_TYPE, helper -> ModContent.registerRecipeTypes((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.RECIPE_SERIALIZER, helper -> ModContent.registerRecipeSerializers((name, v) -> helper.register(ModContent.id(name), v)));
        event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(ModContent.id("main"), ModContent.creativeTab()));
    }
}
