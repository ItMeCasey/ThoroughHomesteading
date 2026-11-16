package com.itmecasey.thoroughhomesteading;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HomeCheckTest {
    private static HomeCheck.Inputs ok() {
        return new HomeCheck.Inputs(0, 2, 1, 1, 12, 8, 1, 20, 20);
    }

    private static HomeCheck.Inputs with(int settlers, int population, int freeBeds, int litBeds, int bestLight, int workstations, int food) {
        return new HomeCheck.Inputs(settlers, population, freeBeds, litBeds, bestLight, 8, workstations, food, 20);
    }

    @Test
    void everythingPresentPasses() {
        HomeCheck.Report r = HomeCheck.evaluate(ok());
        assertTrue(r.passes());
        assertEquals("", r.reason().id());
        assertArrayEquals(new int[] {0, 0}, r.missing());
    }

    @Test
    void eachRequirementBlocksAndNamesItself() {
        assertEquals(HomeCheck.Reason.POPULATION, HomeCheck.evaluate(with(2, 2, 1, 1, 12, 1, 20)).reason());
        assertEquals(HomeCheck.Reason.BEDS, HomeCheck.evaluate(with(0, 2, 0, 0, 0, 1, 20)).reason());
        assertEquals(HomeCheck.Reason.WORKSTATIONS, HomeCheck.evaluate(with(0, 2, 1, 1, 12, 0, 20)).reason());
        assertEquals(HomeCheck.Reason.LIGHT, HomeCheck.evaluate(with(0, 2, 1, 0, 5, 1, 20)).reason());
        assertEquals(HomeCheck.Reason.FOOD, HomeCheck.evaluate(with(0, 2, 1, 1, 12, 1, 19)).reason());
    }

    @Test
    void reasonIdsMatchTheScriptingNames() {
        assertEquals("beds", HomeCheck.Reason.BEDS.id());
        assertEquals("workstations", HomeCheck.Reason.WORKSTATIONS.id());
        assertEquals("light", HomeCheck.Reason.LIGHT.id());
        assertEquals("food", HomeCheck.Reason.FOOD.id());
        assertEquals("population", HomeCheck.Reason.POPULATION.id());
    }

    @Test
    void theFirstMissingRequirementWinsInOrder() {
        assertEquals(HomeCheck.Reason.BEDS, HomeCheck.evaluate(with(0, 2, 0, 0, 0, 0, 0)).reason());
        assertEquals(HomeCheck.Reason.POPULATION, HomeCheck.evaluate(with(4, 4, 0, 0, 0, 0, 0)).reason());
    }

    @Test
    void missingGivesTheHaveAndNeedOfTheBlockingRequirement() {
        assertArrayEquals(new int[] {0, 1}, HomeCheck.evaluate(with(0, 2, 1, 1, 12, 0, 20)).missing());
        assertArrayEquals(new int[] {5, 8}, HomeCheck.evaluate(with(0, 2, 1, 0, 5, 1, 20)).missing());
        assertArrayEquals(new int[] {12, 20}, HomeCheck.evaluate(with(0, 2, 1, 1, 12, 1, 12)).missing());
        assertArrayEquals(new int[] {2, 2}, HomeCheck.evaluate(with(2, 2, 1, 1, 12, 1, 20)).missing());
    }

    @Test
    void theCountdownMovesWhileTheCheckPassesAndWaitsOtherwise() {
        int progress = 0;
        progress = HomeCheck.advance(progress, true, 200);
        progress = HomeCheck.advance(progress, true, 200);
        assertEquals(400, progress);
        progress = HomeCheck.advance(progress, false, 200);
        assertEquals(400, progress);
        assertFalse(HomeCheck.done(5999, 6000));
        assertTrue(HomeCheck.done(6000, 6000));
        assertEquals(0, HomeCheck.advance(0, true, -5));
    }
}
