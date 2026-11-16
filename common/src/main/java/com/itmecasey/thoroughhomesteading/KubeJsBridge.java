package com.itmecasey.thoroughhomesteading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

/**
 * KubeJS support without a compile-time dependency on any KubeJS version: scripts reach the API through KubeJS's own
 * {@code Java.loadClass}. When KubeJS is installed, a small startup script is written once to
 * {@code kubejs/startup_scripts/thoroughhomesteading.js}; it puts the API in {@code global.Homesteading} and {@code global.HomesteadingEvents}
 * so every script type can use them. An existing file is never overwritten. KubeJS is optional; without it nothing happens.
 */
public final class KubeJsBridge {
    private static final Logger LOG = Logger.getLogger("thoroughhomesteading");

    static final String SCRIPT = "// Written by Thorough Homesteading. An existing file is never overwritten.\n"
            + "// Use global.Homesteading and global.HomesteadingEvents from any script, for example\n"
            + "// global.HomesteadingEvents.settlerArrived(e => console.log(e.string('profession')))\n"
            + "global.Homesteading = Java.loadClass('com.itmecasey.thoroughhomesteading.script.Bridges').API\n"
            + "global.HomesteadingEvents = Java.loadClass('com.itmecasey.thoroughhomesteading.HomesteadingEvents').INSTANCE\n";

    private KubeJsBridge() {
    }

    /** True when a KubeJS class is visible. */
    public static boolean present() {
        for (String name : new String[] {"dev.latvian.mods.kubejs.KubeJS", "dev.latvian.mods.kubejs.KubeJSPlugin"}) {
            try {
                Class.forName(name, false, KubeJsBridge.class.getClassLoader());
                return true;
            } catch (ClassNotFoundException | LinkageError e) {
                // try the next name
            }
        }
        return false;
    }

    /** Writes the helper script into the game folder's kubejs when KubeJS is installed; returns whether KubeJS was found. */
    public static boolean install(Path gameDir) {
        return present() && writeHelper(gameDir);
    }

    /** Writes the helper script if it is missing. Returns whether the file exists afterwards. */
    static boolean writeHelper(Path gameDir) {
        Path file = gameDir.resolve("kubejs").resolve("startup_scripts").resolve("thoroughhomesteading.js");
        try {
            if (Files.exists(file)) {
                return Files.isRegularFile(file);
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, SCRIPT, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            LOG.info("ThoroughHomesteading: wrote " + file + " so KubeJS scripts get the Homesteading globals");
        } catch (FileAlreadyExistsException e) {
            return Files.isRegularFile(file);
        } catch (IOException e) {
            LOG.warning("ThoroughHomesteading: could not write the KubeJS helper script: " + e);
            return false;
        }
        return true;
    }
}
