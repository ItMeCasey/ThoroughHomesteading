package com.itmecasey.thoroughhomesteading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ScriptEventsTest {
    @AfterEach
    void clear() {
        ScriptEvents.clear();
    }

    @Test
    void listenersGetTheEventData() {
        List<String> seen = new ArrayList<>();
        HomesteadingEvents.INSTANCE.settlerArrived(e -> seen.add(e.string("profession") + e.number("x")));
        assertFalse(ScriptEvents.fire("settler_arrived", "profession", "minecraft:farmer", "x", 4));
        assertEquals(List.of("minecraft:farmer4.0"), seen);
    }

    @Test
    void beforeSettleCanBeCancelled() {
        HomesteadingEvents.INSTANCE.beforeSettle(e -> e.cancel());
        assertTrue(ScriptEvents.fire("before_settle", "tier", "stone"));
    }

    @Test
    void aThrowingListenerIsSkippedAndTheRestStillRun() {
        List<String> seen = new ArrayList<>();
        HomesteadingEvents.INSTANCE.settlingBlocked(e -> {
            throw new IllegalStateException("script bug");
        });
        HomesteadingEvents.INSTANCE.settlingBlocked(e -> seen.add(e.string("reason")));
        ScriptEvents.fire("settling_blocked", "reason", "beds");
        assertEquals(List.of("beds"), seen);
    }

    @Test
    void unknownEventsAreRefusedAndFiringWithoutListenersIsFree() {
        assertThrows(IllegalArgumentException.class, () -> ScriptEvents.on("nope", e -> { }));
        assertFalse(ScriptEvents.hasListeners("hearth_lit"));
        assertFalse(ScriptEvents.fire("hearth_lit", "x", 1));
        assertEquals(5, ScriptEvents.TYPES.size());
    }
}
