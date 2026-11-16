package com.itmecasey.thoroughhomesteading.neoforge;

import com.itmecasey.thoroughhomesteading.ModContent;
import com.itmecasey.thoroughhomesteading.SettlingEntry;
import com.itmecasey.thoroughhomesteading.SettlingRecipe;
import com.itmecasey.thoroughhomesteading.SettlingRecipes;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Client side: remembers the settling recipes the server sent, so the recipe viewers show the live ones. Only loaded on a client. */
final class NeoForgeClient {
    private NeoForgeClient() {
    }

    static void install() {
        NeoForge.EVENT_BUS.addListener((RecipesReceivedEvent e) -> {
            List<SettlingEntry> entries = new ArrayList<>();
            for (RecipeHolder<SettlingRecipe> holder : e.getRecipeMap().byType(ModContent.SETTLING_TYPE)) {
                entries.add(holder.value().toEntry());
            }
            SettlingRecipes.setClient(entries);
        });
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> SettlingRecipes.clearClient());
    }
}
