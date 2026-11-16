package com.itmecasey.thoroughhomesteading;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LedgerTest {
    @Test
    void countsPerPlayerAndProfession() {
        Ledger l = new Ledger();
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        l.record(a, "minecraft:farmer");
        l.record(a, "minecraft:farmer");
        l.record(a, "minecraft:librarian");
        l.record(b, "minecraft:farmer");
        assertEquals(2, l.count(a, "minecraft:farmer"));
        assertEquals(3, l.count(a, ""));
        assertEquals(1, l.count(b, "minecraft:farmer"));
        assertEquals(0, l.count(b, "minecraft:librarian"));
        assertEquals(0, l.count(UUID.randomUUID(), ""));
    }

    @Test
    void savesAndLoadsAndSkipsDamagedLines(@TempDir Path dir) throws IOException {
        Ledger l = new Ledger();
        UUID a = UUID.randomUUID();
        l.record(a, "minecraft:mason");
        l.record(a, "minecraft:mason");
        Path file = dir.resolve("deep").resolve("ledger.txt");
        l.save(file);
        Files.writeString(file, Files.readString(file) + "garbage line\nnot-a-uuid\tx\t1\n");
        Ledger loaded = new Ledger();
        loaded.load(file);
        assertEquals(2, loaded.count(a, "minecraft:mason"));
        Ledger empty = new Ledger();
        empty.load(dir.resolve("missing.txt"));
        assertEquals(0, empty.count(a, ""));
    }
}
