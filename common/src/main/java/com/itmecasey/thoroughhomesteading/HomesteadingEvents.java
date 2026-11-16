package com.itmecasey.thoroughhomesteading;

import java.util.function.Consumer;

/**
 * The {@code HomesteadingEvents} global of ModJS and KubeJS scripts: one method per event, each taking a function that gets the event.
 * {@code beforeSettle} listeners can call {@code event.cancel()} to stop an arrival.
 */
public final class HomesteadingEvents {
    public static final HomesteadingEvents INSTANCE = new HomesteadingEvents();

    private HomesteadingEvents() {
    }

    /** A Hearth was lit: x, y, z, dimension, tier. */
    public void hearthLit(Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on("hearth_lit", listener);
    }

    /** A Hearth went out: x, y, z, dimension, tier. */
    public void hearthExtinguished(Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on("hearth_extinguished", listener);
    }

    /** A settler walked in: settler (UUID text), hearth (x, y, z), profession, tier, plus x, y, z and dimension of the Hearth. */
    public void settlerArrived(Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on("settler_arrived", listener);
    }

    /** A scan found the home incomplete: reason is beds, workstations, light, food or population. Fires when the reason changes. */
    public void settlingBlocked(Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on("settling_blocked", listener);
    }

    /** Before an arrival; call event.cancel() to stop it. Has profession, tier, x, y, z and dimension. */
    public void beforeSettle(Consumer<ScriptEvents.Event> listener) {
        ScriptEvents.on("before_settle", listener);
    }
}
