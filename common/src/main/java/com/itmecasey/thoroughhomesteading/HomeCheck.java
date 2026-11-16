package com.itmecasey.thoroughhomesteading;

import java.util.Locale;

/**
 * The requirement logic of the home check, free of Minecraft types so it can be tested: given what the scan found, which single
 * requirement blocks the next settler, and how the countdown moves. The Hearth does the scanning; this decides.
 */
public final class HomeCheck {
    /** The requirement that blocks the arrival, in the order they are checked. */
    public enum Reason {
        NONE, POPULATION, BEDS, WORKSTATIONS, LIGHT, FOOD;

        /** The id scripts see in events (beds, workstations, light, food, population); empty for NONE. */
        public String id() {
            return this == NONE ? "" : name().toLowerCase(Locale.ROOT);
        }
    }

    /** What the scan found. {@code litBeds} counts the free beds whose head block has enough light. */
    public record Inputs(int settlers, int population, int freeBeds, int litBeds, int bestLight, int lightMin, int freeWorkstations, int food,
            int foodRequired) {
    }

    /** The outcome: every number the look-at lines and scripts show, and the first blocking reason. */
    public record Report(Reason reason, Inputs in) {
        public boolean passes() {
            return reason == Reason.NONE;
        }

        /** Have and need of the blocking requirement, for the single look-at line; {0, 0} when nothing blocks. */
        public int[] missing() {
            return switch (reason) {
                case NONE -> new int[] {0, 0};
                case POPULATION -> new int[] {in.settlers(), in.population()};
                case BEDS -> new int[] {in.freeBeds(), 1};
                case WORKSTATIONS -> new int[] {in.freeWorkstations(), 1};
                case LIGHT -> new int[] {in.bestLight(), in.lightMin()};
                case FOOD -> new int[] {in.food(), in.foodRequired()};
            };
        }
    }

    private HomeCheck() {
    }

    public static Report evaluate(Inputs in) {
        Reason reason = Reason.NONE;
        if (in.settlers() >= in.population()) {
            reason = Reason.POPULATION;
        } else if (in.freeBeds() < 1) {
            reason = Reason.BEDS;
        } else if (in.freeWorkstations() < 1) {
            reason = Reason.WORKSTATIONS;
        } else if (in.litBeds() < 1) {
            reason = Reason.LIGHT;
        } else if (in.food() < in.foodRequired()) {
            reason = Reason.FOOD;
        }
        return new Report(reason, in);
    }

    /** The countdown after one scan: it moves up by {@code step} while the check passes and stands still otherwise. */
    public static int advance(int progress, boolean passes, int step) {
        return passes ? progress + Math.max(0, step) : progress;
    }

    public static boolean done(int progress, int target) {
        return progress >= target;
    }
}
