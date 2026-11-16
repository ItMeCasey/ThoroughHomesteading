package com.itmecasey.thoroughhomesteading;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * How many settlers each player's Hearths have brought, by profession. Quests ask this (the Barely Questing {@code settler} task), so the
 * numbers only ever go up. Saved as a small text file in the world folder; Minecraft-free so it can be tested.
 */
public final class Ledger {
    private final Map<UUID, Map<String, Integer>> counts = new LinkedHashMap<>();

    /** Adds one arrival of the profession (such as minecraft:farmer) for the player. */
    public synchronized void record(UUID player, String profession) {
        counts.computeIfAbsent(player, k -> new LinkedHashMap<>()).merge(profession, 1, Integer::sum);
    }

    /** Arrivals for the player of that profession; an empty profession counts all of them. */
    public synchronized int count(UUID player, String profession) {
        Map<String, Integer> mine = counts.get(player);
        if (mine == null) {
            return 0;
        }
        if (profession == null || profession.isEmpty()) {
            int sum = 0;
            for (int n : mine.values()) {
                sum += n;
            }
            return sum;
        }
        return mine.getOrDefault(profession, 0);
    }

    public synchronized void clear() {
        counts.clear();
    }

    public synchronized void save(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<UUID, Map<String, Integer>> p : counts.entrySet()) {
            for (Map.Entry<String, Integer> e : p.getValue().entrySet()) {
                lines.add(p.getKey() + "\t" + e.getKey() + "\t" + e.getValue());
            }
        }
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.write(file, lines, StandardCharsets.UTF_8);
    }

    /** Replaces the contents with the file's; a missing file leaves it empty and a bad line is skipped. */
    public synchronized void load(Path file) throws IOException {
        counts.clear();
        if (!Files.isRegularFile(file)) {
            return;
        }
        for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String[] parts = line.split("\t");
            if (parts.length != 3) {
                continue;
            }
            try {
                counts.computeIfAbsent(UUID.fromString(parts[0]), k -> new LinkedHashMap<>()).put(parts[1], Integer.parseInt(parts[2]));
            } catch (IllegalArgumentException e) {
                // skip a damaged line
            }
        }
    }
}
