package com.itmecasey.thoroughhomesteading;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Every tunable number in the mod: the single list of keys, defaults, ranges and descriptions. Each loader layer builds its config
 * spec (NeoForge {@code ModConfigSpec}, Forge {@code ForgeConfigSpec}, file {@code config/thoroughhomesteading-common.toml}) from
 * {@link #defs()} and pushes the loaded values in with {@link #set}, so common code and tests only ever read from here. Datapack
 * hearth files can override the per-tier numbers on top of the config ({@link #setPackOverrides}). This class reads and writes no file.
 */
public final class Settings {
    /** The kind of value a setting holds. */
    public enum Kind { INT, DOUBLE, BOOL }

    /** One setting: its key, kind, default, allowed range (numbers only) and description. */
    public record Def(String key, Kind kind, String fallback, double min, double max, String comment) {
    }

    private static final Map<String, Def> DEFS = new LinkedHashMap<>();
    private static final Map<String, String> VALUES = new LinkedHashMap<>();
    private static volatile Map<String, Integer> packOverrides = Map.of();

    private static void def(String key, Object fallback, double min, double max, String comment) {
        Kind kind = fallback instanceof Boolean ? Kind.BOOL : fallback instanceof Integer ? Kind.INT : Kind.DOUBLE;
        DEFS.put(key, new Def(key, kind, String.valueOf(fallback), min, max, comment));
    }

    static {
        def("scan_interval", 200, 20, 24000, "Ticks between home checks of a lit Hearth.");
        for (HearthTier t : HearthTier.values()) {
            def("settle_ticks." + t.id(), t.defaultSettleTicks(), 20, 1728000, "Ticks the countdown of a " + t.title() + " Hearth runs while the home check passes.");
        }
        for (HearthTier t : HearthTier.values()) {
            def("radius." + t.id(), t.defaultRadius(), 2, 64, "Horizontal radius in blocks a " + t.title() + " Hearth searches for beds and workstations (6 blocks up and down).");
        }
        for (HearthTier t : HearthTier.values()) {
            def("population." + t.id(), t.defaultPopulation(), 1, 64, "Most living settlers a " + t.title() + " Hearth keeps.");
        }
        def("light_min", 8, 0, 15, "Light level needed at the head of a bed.");
        def("food_required", 20, 1, 10000, "Nutrition of food a settling recipe uses when it names no amount.");
        def("check_standing_room", true, 0, 0, "true makes the Hearth check that a settler can walk from the arrival point to the bed.");
        def("allow_unlit_hearth_scan", false, 0, 0, "true lets an unlit Hearth run the home check (the look-at lines update); the countdown only runs while lit.");
    }

    private Settings() {
    }

    /** Restores every default (and drops datapack overrides). */
    public static synchronized void reset() {
        VALUES.clear();
        packOverrides = Map.of();
    }

    /** Replaces the datapack overrides of {@code radius.<tier>}, {@code population.<tier>} and {@code settle_ticks.<tier>}. */
    public static void setPackOverrides(Map<String, Integer> overrides) {
        packOverrides = Map.copyOf(overrides);
    }

    private static synchronized String raw(String key) {
        Def e = def(key);
        return VALUES.getOrDefault(key, e.fallback());
    }

    /** Every setting key, sorted. */
    public static List<String> keys() {
        return new ArrayList<>(new TreeSet<>(DEFS.keySet()));
    }

    /** The current value of a setting as text. */
    public static String value(String key) {
        Integer pack = packOverrides.get(key);
        return pack != null ? String.valueOf(pack) : raw(key);
    }

    /** Overrides one setting (tests and scripts; not saved to the file). */
    public static synchronized void set(String key, Object value) {
        raw(key);
        VALUES.put(key, String.valueOf(value));
    }

    public static double getDouble(String key) {
        try {
            return Double.parseDouble(value(key).trim());
        } catch (NumberFormatException ex) {
            return Double.parseDouble(DEFS.get(key).fallback());
        }
    }

    public static int getInt(String key) {
        return (int) Math.round(getDouble(key));
    }

    public static boolean getBool(String key) {
        return Boolean.parseBoolean(value(key).trim());
    }

    /** Every setting definition, in the order the config files list them. */
    public static List<Def> defs() {
        return new ArrayList<>(DEFS.values());
    }

    public static Def def(String key) {
        Def d = DEFS.get(key);
        if (d == null) {
            throw new IllegalArgumentException("Unknown setting " + key);
        }
        return d;
    }
}
