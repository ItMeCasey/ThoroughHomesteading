package com.itmecasey.thoroughhomesteading;

/** The three Hearth tiers. The numbers here are the defaults; {@link Settings} holds the live values (config file or datapack). */
public enum HearthTier {
    STONE("stone", "Stone", 1, 8, 2, 6000, 1),
    IRON("iron", "Iron", 2, 12, 4, 4000, 1),
    GOLDEN("golden", "Golden", 3, 16, 8, 2400, 2);

    private final String id;
    private final String title;
    private final int level;
    private final int radius;
    private final int population;
    private final int settleTicks;
    private final int startLevel;

    HearthTier(String id, String title, int level, int radius, int population, int settleTicks, int startLevel) {
        this.id = id;
        this.title = title;
        this.level = level;
        this.radius = radius;
        this.population = population;
        this.settleTicks = settleTicks;
        this.startLevel = startLevel;
    }

    public String id() {
        return id;
    }

    public String title() {
        return title;
    }

    /** 1 for Stone, 2 for Iron, 3 for Golden. Settling recipes name the lowest tier that may use them. */
    public int level() {
        return level;
    }

    public int defaultRadius() {
        return radius;
    }

    public int defaultPopulation() {
        return population;
    }

    public int defaultSettleTicks() {
        return settleTicks;
    }

    /** The trade level a new settler starts at (2 for Golden, 1 otherwise). */
    public int startLevel() {
        return startLevel;
    }

    public String blockName() {
        return id + "_hearth";
    }

    /** The upgrade item that turns the tier below into this one, or null for Stone. */
    public String upgradeName() {
        return level == 1 ? null : id + "_hearth_upgrade";
    }

    public boolean upgradesFrom(HearthTier below) {
        return below != null && below.level + 1 == level;
    }

    public static HearthTier byId(String id) {
        for (HearthTier t : values()) {
            if (t.id.equals(id)) {
                return t;
            }
        }
        return null;
    }

    /** The live radius (config, or a datapack tier file). */
    public int radius() {
        return Settings.getInt("radius." + id);
    }

    public int population() {
        return Settings.getInt("population." + id);
    }

    public int settleTicks() {
        return Settings.getInt("settle_ticks." + id);
    }
}
