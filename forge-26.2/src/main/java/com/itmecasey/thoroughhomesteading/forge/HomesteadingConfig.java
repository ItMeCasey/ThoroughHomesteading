package com.itmecasey.thoroughhomesteading.forge;

import com.itmecasey.thoroughhomesteading.Settings;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraftforge.common.ForgeConfigSpec;

/**
 * The Forge config file {@code config/thoroughhomesteading-common.toml}, built from the single list of settings in common. Its values are
 * pushed into {@link Settings} when the file loads or reloads.
 */
final class HomesteadingConfig {
    static final ForgeConfigSpec SPEC;
    private static final Map<String, ForgeConfigSpec.ConfigValue<?>> VALUES = new LinkedHashMap<>();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        for (Settings.Def d : Settings.defs()) {
            String start = d.fallback();
            builder.comment(d.comment());
            switch (d.kind()) {
                case INT -> VALUES.put(d.key(), builder.defineInRange(d.key(), clampInt(start, d), (int) d.min(), (int) d.max()));
                case DOUBLE -> VALUES.put(d.key(), builder.defineInRange(d.key(), clampDouble(start, d), d.min(), d.max()));
                case BOOL -> VALUES.put(d.key(), builder.define(d.key(), Boolean.parseBoolean(start)));
            }
        }
        SPEC = builder.build();
    }

    private HomesteadingConfig() {
    }

    private static int clampInt(String text, Settings.Def d) {
        try {
            return (int) Math.max(d.min(), Math.min(d.max(), Math.round(Double.parseDouble(text))));
        } catch (NumberFormatException e) {
            return (int) Math.round(Double.parseDouble(d.fallback()));
        }
    }

    private static double clampDouble(String text, Settings.Def d) {
        try {
            return Math.max(d.min(), Math.min(d.max(), Double.parseDouble(text)));
        } catch (NumberFormatException e) {
            return Double.parseDouble(d.fallback());
        }
    }

    /** Copies the loaded file values into {@link Settings}. */
    static void push() {
        for (Map.Entry<String, ForgeConfigSpec.ConfigValue<?>> e : VALUES.entrySet()) {
            Settings.set(e.getKey(), e.getValue().get());
        }
    }
}
