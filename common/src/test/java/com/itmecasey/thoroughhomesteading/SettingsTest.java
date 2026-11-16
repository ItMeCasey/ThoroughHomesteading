package com.itmecasey.thoroughhomesteading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SettingsTest {
    @AfterEach
    void reset() {
        Settings.reset();
    }

    @Test
    void defaultsAreTheDesignNumbers() {
        assertEquals(200, Settings.getInt("scan_interval"));
        assertEquals(6000, Settings.getInt("settle_ticks.stone"));
        assertEquals(4000, Settings.getInt("settle_ticks.iron"));
        assertEquals(2400, Settings.getInt("settle_ticks.golden"));
        assertEquals(8, Settings.getInt("radius.stone"));
        assertEquals(12, Settings.getInt("radius.iron"));
        assertEquals(16, Settings.getInt("radius.golden"));
        assertEquals(2, Settings.getInt("population.stone"));
        assertEquals(4, Settings.getInt("population.iron"));
        assertEquals(8, Settings.getInt("population.golden"));
        assertEquals(8, Settings.getInt("light_min"));
        assertEquals(20, Settings.getInt("food_required"));
        assertTrue(Settings.getBool("check_standing_room"));
        assertFalse(Settings.getBool("allow_unlit_hearth_scan"));
    }

    @Test
    void tierAccessorsReadTheSettings() {
        assertEquals(8, HearthTier.STONE.radius());
        assertEquals(8, HearthTier.GOLDEN.population());
        assertEquals(4000, HearthTier.IRON.settleTicks());
        Settings.set("radius.stone", 10);
        assertEquals(10, HearthTier.STONE.radius());
    }

    @Test
    void datapackOverridesWinOverTheConfigUntilReset() {
        Settings.set("radius.stone", 10);
        Settings.setPackOverrides(Map.of("radius.stone", 5));
        assertEquals(5, HearthTier.STONE.radius());
        Settings.reset();
        assertEquals(8, HearthTier.STONE.radius());
    }

    @Test
    void everyKeyHasADefinitionWithACommentAndAValidDefault() {
        for (Settings.Def d : Settings.defs()) {
            assertFalse(d.comment().isBlank(), d.key());
            if (d.kind() != Settings.Kind.BOOL) {
                double v = Double.parseDouble(d.fallback());
                assertTrue(v >= d.min() && v <= d.max(), d.key() + " default outside its range");
            }
        }
        assertEquals(Settings.defs().size(), Settings.keys().size());
    }

    @Test
    void unknownKeysThrow() {
        assertThrows(IllegalArgumentException.class, () -> Settings.value("nope"));
        assertThrows(IllegalArgumentException.class, () -> Settings.set("nope", 1));
    }

    @Test
    void recipeFoodFallsBackToTheSetting() {
        SettlingEntry plain = new SettlingEntry("minecraft:composter", "minecraft:farmer", 1, 0, List.of());
        SettlingEntry set = new SettlingEntry("minecraft:composter", "minecraft:farmer", 1, 35, List.of());
        assertEquals(20, plain.foodNeeded());
        Settings.set("food_required", 30);
        assertEquals(30, plain.foodNeeded());
        assertEquals(35, set.foodNeeded());
        assertTrue(plain.allows(HearthTier.STONE));
        assertFalse(new SettlingEntry("a:b", "c:d", 2, 0, List.of()).allows(HearthTier.STONE));
        assertTrue(new SettlingEntry("a:b", "c:d", 2, 0, List.of()).allows(HearthTier.GOLDEN));
    }

    @Test
    void tiersChainOneLevelAtATime() {
        assertTrue(HearthTier.IRON.upgradesFrom(HearthTier.STONE));
        assertTrue(HearthTier.GOLDEN.upgradesFrom(HearthTier.IRON));
        assertFalse(HearthTier.GOLDEN.upgradesFrom(HearthTier.STONE));
        assertFalse(HearthTier.STONE.upgradesFrom(null));
        assertEquals("iron_hearth_upgrade", HearthTier.IRON.upgradeName());
        assertEquals(2, HearthTier.GOLDEN.startLevel());
        assertEquals(1, HearthTier.IRON.startLevel());
        assertEquals(HearthTier.GOLDEN, HearthTier.byId("golden"));
    }
}
