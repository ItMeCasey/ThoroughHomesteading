package com.itmecasey.thoroughhomesteading;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * The settling recipes the running server has, and (on a client) the ones the server sent or, failing that, the shipped defaults.
 * Server side this reads the recipe manager every time, so a datapack reload or a script that changes recipes is seen at once.
 */
public final class SettlingRecipes {
    private static volatile List<SettlingEntry> client = List.of();
    private static volatile boolean clientSynced;

    private SettlingRecipes() {
    }

    /** Every settling recipe of the server, ordered by recipe id so the result never depends on load order. */
    public static List<SettlingEntry> entries(MinecraftServer server) {
        List<RecipeHolder<?>> holders = new ArrayList<>();
        for (RecipeHolder<?> h : server.getRecipeManager().getRecipes()) {
            if (h.value() instanceof SettlingRecipe) {
                holders.add(h);
            }
        }
        holders.sort(Comparator.comparing(h -> h.id().identifier().toString()));
        List<SettlingEntry> out = new ArrayList<>();
        for (RecipeHolder<?> h : holders) {
            out.add(((SettlingRecipe) h.value()).toEntry());
        }
        return out;
    }

    /** The first recipe for each workstation block id. */
    public static Map<String, SettlingEntry> byWorkstation(MinecraftServer server) {
        Map<String, SettlingEntry> map = new LinkedHashMap<>();
        for (SettlingEntry e : entries(server)) {
            map.putIfAbsent(e.workstation(), e);
        }
        return map;
    }

    /** Item ids the recipes spend besides food; the Hearth accepts these as well. */
    public static Set<String> extraItems(MinecraftServer server) {
        Set<String> out = new TreeSet<>();
        for (SettlingEntry e : entries(server)) {
            for (SettlingEntry.Extra x : e.extra()) {
                out.add(x.item());
            }
        }
        return out;
    }

    // ---- client ----

    /** Called by the loader layer on a client when the server's recipes arrive. */
    public static void setClient(List<SettlingEntry> entries) {
        client = List.copyOf(entries);
        clientSynced = true;
    }

    public static void clearClient() {
        client = List.of();
        clientSynced = false;
    }

    /** What the viewers show: the recipes the server sent, or the shipped defaults when none arrived (Forge, or before joining a world). */
    public static List<SettlingEntry> forViewers() {
        return clientSynced ? client : SettlingDefaults.entries();
    }
}
