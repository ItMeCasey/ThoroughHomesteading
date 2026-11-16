package com.itmecasey.thoroughhomesteading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Checks the data files, models and textures every Hearth and upgrade item needs, and that the shipped recipes equal the defaults table. */
class AssetTest {
    private static final String BASE = "/assets/thoroughhomesteading/";
    private static final String DATA = "/data/thoroughhomesteading/";

    private static String text(String path) throws IOException {
        try (InputStream in = AssetTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "missing " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static BufferedImage image(String path) throws IOException {
        try (InputStream in = AssetTest.class.getResourceAsStream(BASE + path)) {
            assertNotNull(in, "missing " + path);
            return ImageIO.read(in);
        }
    }

    private static boolean exists(String path) {
        return AssetTest.class.getResource(path) != null;
    }

    private static String field(String json, String name) {
        Matcher m = Pattern.compile("\"" + name + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
        assertTrue(m.find(), name + " missing in " + json);
        return m.group(1);
    }

    @Test
    void everyHearthHasBlockstateModelsItemAndTextures() throws IOException {
        for (HearthTier t : HearthTier.values()) {
            String id = t.blockName();
            assertTrue(exists(BASE + "blockstates/" + id + ".json"), id);
            assertTrue(exists(BASE + "models/block/" + id + ".json"), id);
            assertTrue(exists(BASE + "models/block/" + id + "_lit.json"), id);
            assertTrue(exists(BASE + "models/item/" + id + ".json"), id);
            assertTrue(exists(BASE + "items/" + id + ".json"), id);
            for (String face : List.of("side", "bottom", "top", "top_lit")) {
                BufferedImage img = image("textures/block/" + id + "_" + face + ".png");
                assertEquals(16, img.getWidth(), id + face);
                assertEquals(16, img.getHeight(), id + face);
            }
        }
    }

    @Test
    void everyUpgradeItemHasAnIconAndModels() throws IOException {
        for (HearthTier t : HearthTier.values()) {
            if (t.upgradeName() == null) {
                continue;
            }
            BufferedImage img = image("textures/item/" + t.upgradeName() + ".png");
            assertEquals(16, img.getWidth());
            assertEquals(16, img.getHeight());
            int opaque = 0;
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 16; y++) {
                    if (((img.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                        opaque++;
                    }
                }
            }
            assertTrue(opaque > 40, "icon is nearly empty: " + t.upgradeName());
            assertTrue(exists(BASE + "models/item/" + t.upgradeName() + ".json"));
            assertTrue(exists(BASE + "items/" + t.upgradeName() + ".json"));
        }
    }

    @Test
    void everyNameHasAnEnglishTranslation() throws IOException {
        String lang = text(BASE + "lang/en_us.json");
        for (HearthTier t : HearthTier.values()) {
            assertTrue(lang.contains("\"block.thoroughhomesteading." + t.blockName() + "\""), t.id());
            if (t.upgradeName() != null) {
                assertTrue(lang.contains("\"item.thoroughhomesteading." + t.upgradeName() + "\""), t.id());
            }
        }
        for (String key : List.of("itemGroup.thoroughhomesteading", "smwti.thoroughhomesteading.settlers", "smwti.thoroughhomesteading.beds",
                "smwti.thoroughhomesteading.workstations", "smwti.thoroughhomesteading.food", "smwti.thoroughhomesteading.need.beds",
                "smwti.thoroughhomesteading.need.workstations", "smwti.thoroughhomesteading.need.light", "viewer.thoroughhomesteading.settling",
                "viewer.thoroughhomesteading.hearth_upgrade", "viewer.thoroughhomesteading.info")) {
            assertTrue(lang.contains("\"" + key + "\""), key);
        }
    }

    @Test
    void theLookAtLinesAreNamesAndValuesOnly() throws IOException {
        String lang = text(BASE + "lang/en_us.json");
        Matcher m = Pattern.compile("\"smwti\\.thoroughhomesteading\\.[a-z_.]+\"\\s*:\\s*\"([^\"]*)\"").matcher(lang);
        int n = 0;
        while (m.find()) {
            n++;
            String line = m.group(1);
            assertTrue(line.matches("[A-Za-z ]+ %s( of %s)?"), "not a name and values line: " + line);
        }
        assertTrue(n >= 8);
    }

    @Test
    void shippedSettlingRecipesEqualTheDefaultsTable() throws IOException {
        for (SettlingEntry e : SettlingDefaults.entries()) {
            String json = text(DATA + "recipe/" + SettlingDefaults.fileName(e) + ".json");
            assertEquals("thoroughhomesteading:settling", field(json, "type"));
            assertEquals(e.workstation(), field(json, "workstation"));
            assertEquals(e.profession(), field(json, "profession"));
            assertTrue(json.contains("\"min_tier\": " + e.minTier()), json);
        }
        assertEquals(13, SettlingDefaults.entries().size());
    }

    @Test
    void craftingRecipesAndTagsExist() throws IOException {
        for (String name : List.of("stone_hearth", "iron_hearth_upgrade", "golden_hearth_upgrade")) {
            String json = text(DATA + "recipe/" + name + ".json");
            assertEquals("thoroughhomesteading:" + name, field(json, "id"));
        }
        String tag = text("/data/minecraft/tags/block/mineable/pickaxe.json");
        for (HearthTier t : HearthTier.values()) {
            assertTrue(tag.contains("thoroughhomesteading:" + t.blockName()));
        }
    }
}
