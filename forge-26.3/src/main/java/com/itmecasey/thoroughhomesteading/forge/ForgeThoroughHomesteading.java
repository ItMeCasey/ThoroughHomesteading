package com.itmecasey.thoroughhomesteading.forge;

import com.itmecasey.thoroughhomesteading.Dev;
import com.itmecasey.thoroughhomesteading.ModContent;
import com.itmecasey.thoroughhomesteading.ThoroughHomesteading;
import com.itmecasey.thoroughhomesteading.script.Bridges;
import com.itmecasey.thoroughhomesteading.script.ScriptHost;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.RegisterEvent;

/** Forge entry point for Minecraft 26.x. Registers the content, the config and the script hooks. */
@Mod(ThoroughHomesteading.MODID)
public final class ForgeThoroughHomesteading {
    public ForgeThoroughHomesteading(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, HomesteadingConfig.SPEC, "thoroughhomesteading-common.toml");
        ModConfigEvent.Loading.getBus(context.getModBusGroup()).addListener(e -> onConfig(e.getConfig()));
        ModConfigEvent.Reloading.getBus(context.getModBusGroup()).addListener(e -> onConfig(e.getConfig()));
        Bridges.install(FMLPaths.GAMEDIR.get());
        RegisterEvent.getBus(context.getModBusGroup()).addListener(ForgeThoroughHomesteading::register);
        ServerStartedEvent.BUS.addListener((ServerStartedEvent e) -> ScriptHost.start(e.getServer()));
        ServerStoppedEvent.BUS.addListener((ServerStoppedEvent e) -> ScriptHost.stop());
        Dev.call("com.itmecasey.thoroughhomesteading.forge.dev.ForgeDev", "install", context);
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
