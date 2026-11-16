package com.itmecasey.thoroughhomesteading;

import java.util.List;

/**
 * One settling recipe as plain data: the workstation block, the profession it gives, the lowest Hearth tier that may use it, the
 * nutrition of food it spends (0 means the {@code food_required} setting) and the extra items spent with the food.
 */
public record SettlingEntry(String workstation, String profession, int minTier, int food, List<Extra> extra) {
    /** An extra item spent on arrival. */
    public record Extra(String item, int count) {
    }

    public SettlingEntry {
        extra = List.copyOf(extra);
    }

    /** Nutrition this recipe needs under the current settings. */
    public int foodNeeded() {
        return food > 0 ? food : Settings.getInt("food_required");
    }

    public boolean allows(HearthTier tier) {
        return tier.level() >= minTier;
    }
}
