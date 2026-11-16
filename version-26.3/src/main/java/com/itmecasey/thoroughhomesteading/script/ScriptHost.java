package com.itmecasey.thoroughhomesteading.script;

import com.itmecasey.thoroughhomesteading.Ledger;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Logger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

/** The running server as scripts and the Barely Questing bridge see it, plus the saved arrival ledger. */
public final class ScriptHost {
    private static final Logger LOG = Logger.getLogger("thoroughhomesteading");
    private static volatile MinecraftServer server;
    private static volatile Ledger ledger = new Ledger();

    private ScriptHost() {
    }

    /** Called by the loader layer when a server has started: loads this world's ledger. */
    public static void start(MinecraftServer started) {
        server = started;
        Ledger fresh = new Ledger();
        try {
            fresh.load(ledgerFile(started));
        } catch (IOException e) {
            LOG.warning("ThoroughHomesteading: could not read the settler ledger: " + e);
        }
        ledger = fresh;
    }

    public static void stop() {
        server = null;
        ledger = new Ledger();
    }

    public static void touch(Level level) {
        if (server == null && level.getServer() != null) {
            start(level.getServer());
        }
    }

    private static Path ledgerFile(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve("thoroughhomesteading").resolve("ledger.txt");
    }

    /** The ledger of settlers brought, per player and profession. */
    public static Ledger ledger() {
        return ledger;
    }

    /** Adds an arrival and saves the file (it is tiny). */
    public static void record(java.util.UUID player, String profession) {
        MinecraftServer s = server;
        if (player == null || s == null) {
            return;
        }
        ledger.record(player, profession);
        try {
            ledger.save(ledgerFile(s));
        } catch (IOException e) {
            LOG.warning("ThoroughHomesteading: could not save the settler ledger: " + e);
        }
    }

    @Nullable
    public static MinecraftServer server() {
        return server;
    }

    /** The name of a level's dimension, such as minecraft:overworld. */
    public static String dimension(Level level) {
        return level.dimension().identifier().toString();
    }

    @Nullable
    public static ServerLevel level(String dimension) {
        MinecraftServer s = server;
        if (s == null) {
            return null;
        }
        for (ServerLevel l : s.getAllLevels()) {
            if (dimension(l).equals(dimension)) {
                return l;
            }
        }
        return null;
    }
}
