package com.itmecasey.thoroughhomesteading.script;

import com.itmecasey.thoroughhomesteading.HearthTier;
import com.itmecasey.thoroughhomesteading.ScriptEvents;
import com.itmecasey.thoroughhomesteading.Settings;
import com.itmecasey.thoroughhomesteading.SettlingEntry;
import com.itmecasey.thoroughhomesteading.SettlingRecipes;
import com.itmecasey.thoroughhomesteading.block.HearthBlockEntity;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * The Homesteading global of ModJS server scripts (and KubeJS through its helper script): read access to Hearths, settings and recipes, and
 * the one action. Events are on the HomesteadingEvents global. Everything uses strings, numbers, lists and maps so a script engine can call it.
 */
public final class HomesteadingApi {
    /** The tiers, from Stone to Golden. */
    public List<String> tiers() {
        List<String> out = new ArrayList<>();
        for (HearthTier t : HearthTier.values()) {
            out.add(t.id());
        }
        return out;
    }

    /** The events HomesteadingEvents offers, by their internal names. */
    public List<String> events() {
        return ScriptEvents.TYPES;
    }

    /** What every Hearth settling recipe says, as maps with workstation, profession, minTier, food (nutrition) and extra (item to count). Empty before a server runs. */
    public List<Map<String, Object>> recipes() {
        MinecraftServer server = ScriptHost.server();
        List<Map<String, Object>> out = new ArrayList<>();
        if (server == null) {
            return out;
        }
        for (SettlingEntry e : SettlingRecipes.entries(server)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("workstation", e.workstation());
            m.put("profession", e.profession());
            m.put("minTier", e.minTier());
            m.put("food", e.foodNeeded());
            Map<String, Object> extra = new LinkedHashMap<>();
            for (SettlingEntry.Extra x : e.extra()) {
                extra.put(x.item(), x.count());
            }
            m.put("extra", extra);
            out.add(m);
        }
        return out;
    }

    private static HearthBlockEntity find(String dimension, int x, int y, int z) {
        MinecraftServer server = ScriptHost.server();
        if (server == null) {
            return null;
        }
        BlockPos pos = new BlockPos(x, y, z);
        if (dimension != null && !dimension.isEmpty()) {
            ServerLevel level = ScriptHost.level(dimension);
            return level != null && level.getBlockEntity(pos) instanceof HearthBlockEntity h ? h : null;
        }
        ServerLevel overworld = server.overworld();
        if (overworld.getBlockEntity(pos) instanceof HearthBlockEntity h) {
            return h;
        }
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getBlockEntity(pos) instanceof HearthBlockEntity h) {
                return h;
            }
        }
        return null;
    }

    /** Every requirement of the Hearth at a position with its current value, the timer and the settlers brought; null when there is no Hearth. */
    public Map<String, Object> report(int x, int y, int z) {
        return report("", x, y, z);
    }

    public Map<String, Object> report(String dimension, int x, int y, int z) {
        HearthBlockEntity hearth = find(dimension, x, y, z);
        return hearth == null || !(hearth.getLevel() instanceof ServerLevel level) ? null : hearth.report(level);
    }

    /** The UUIDs (as text) of the settlers the Hearth at a position has brought and still counts; empty when there is no Hearth. */
    public List<String> settlersOf(int x, int y, int z) {
        HearthBlockEntity hearth = find("", x, y, z);
        return hearth == null ? new ArrayList<>() : hearth.settlerIds();
    }

    /** Skips the timer and brings a settler if the home check passes. The profession (such as minecraft:farmer) may be empty to take the nearest workstation. */
    public boolean settleNow(int x, int y, int z, String profession) {
        HearthBlockEntity hearth = find("", x, y, z);
        return hearth != null && hearth.getLevel() instanceof ServerLevel level && hearth.settleNow(level, profession == null ? "" : profession);
    }

    public List<String> settingKeys() {
        return Settings.keys();
    }

    /** A setting as text, for example radius.stone. */
    public String setting(String key) {
        return Settings.value(key);
    }

    /** Changes a setting until the game closes (the config file is not touched). Unknown keys throw. */
    public void setSetting(String key, String value) {
        Settings.value(key);
        Settings.set(key, value);
    }
}
