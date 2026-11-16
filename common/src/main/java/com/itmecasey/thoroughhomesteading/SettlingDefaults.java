package com.itmecasey.thoroughhomesteading;

import java.util.ArrayList;
import java.util.List;

/**
 * The settling recipes the mod ships (one JSON file each under data/thoroughhomesteading/recipe). A test keeps this table and those
 * files identical. Viewers fall back to it when the server has not sent the live recipes, and the self test uses it.
 */
public final class SettlingDefaults {
    private static final String[][] TABLE = {
        {"minecraft:composter", "minecraft:farmer"}, {"minecraft:barrel", "minecraft:fisherman"},
        {"minecraft:blast_furnace", "minecraft:armorer"}, {"minecraft:brewing_stand", "minecraft:cleric"},
        {"minecraft:cartography_table", "minecraft:cartographer"}, {"minecraft:fletching_table", "minecraft:fletcher"},
        {"minecraft:grindstone", "minecraft:weaponsmith"}, {"minecraft:lectern", "minecraft:librarian"},
        {"minecraft:loom", "minecraft:shepherd"}, {"minecraft:smithing_table", "minecraft:toolsmith"},
        {"minecraft:smoker", "minecraft:butcher"}, {"minecraft:stonecutter", "minecraft:mason"},
        {"minecraft:cauldron", "minecraft:leatherworker"},
    };

    private SettlingDefaults() {
    }

    public static List<SettlingEntry> entries() {
        List<SettlingEntry> out = new ArrayList<>();
        for (String[] row : TABLE) {
            out.add(new SettlingEntry(row[0], row[1], 1, 0, List.of()));
        }
        return out;
    }

    /** The file name (without .json) of the shipped recipe for a workstation, such as settle_composter. */
    public static String fileName(SettlingEntry entry) {
        String ws = entry.workstation();
        return "settle_" + ws.substring(ws.indexOf(':') + 1);
    }
}
