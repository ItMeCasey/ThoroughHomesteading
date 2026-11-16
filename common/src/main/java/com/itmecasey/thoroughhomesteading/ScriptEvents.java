package com.itmecasey.thoroughhomesteading;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Listeners that ModJS and KubeJS scripts register. Minecraft-free: the game code fires events with plain data, and a script that
 * throws is logged on one line and skipped, so a script can never break the server tick.
 */
public final class ScriptEvents {
    private static final Logger LOG = Logger.getLogger("thoroughhomesteading");

    /** The events scripts can listen to. */
    public static final List<String> TYPES = List.of("hearth_lit", "hearth_extinguished", "settler_arrived", "settling_blocked", "before_settle");

    /** What a script receives: the event name and its values by key. {@code before_settle} events can be cancelled. */
    public static final class Event {
        private final String type;
        private final Map<String, Object> data;
        private boolean cancelled;

        Event(String type, Map<String, Object> data) {
            this.type = type;
            this.data = data;
        }

        public String type() {
            return type;
        }

        public Object get(String key) {
            return data.get(key);
        }

        public String string(String key) {
            Object v = data.get(key);
            return v == null ? "" : String.valueOf(v);
        }

        public double number(String key) {
            Object v = data.get(key);
            return v instanceof Number n ? n.doubleValue() : 0;
        }

        public List<String> keys() {
            return new ArrayList<>(data.keySet());
        }

        /** Stops the arrival (only {@code before_settle}; other events ignore it). */
        public void cancel() {
            cancelled = true;
        }

        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public String toString() {
            return type + data;
        }
    }

    private static final Map<String, List<Consumer<Event>>> LISTENERS = new ConcurrentHashMap<>();

    private ScriptEvents() {
    }

    public static void on(String type, Consumer<Event> listener) {
        if (!TYPES.contains(type)) {
            throw new IllegalArgumentException("Unknown Thorough Homesteading event " + type + ". Events: " + TYPES);
        }
        LISTENERS.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    public static boolean hasListeners(String type) {
        List<Consumer<Event>> l = LISTENERS.get(type);
        return l != null && !l.isEmpty();
    }

    /** Tells every listener; nothing is built when nobody listens. Arguments are key, value, key, value... Returns true if a listener cancelled. */
    public static boolean fire(String type, Object... keyValues) {
        List<Consumer<Event>> listeners = LISTENERS.get(type);
        if (listeners == null || listeners.isEmpty()) {
            return false;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            data.put(String.valueOf(keyValues[i]), keyValues[i + 1]);
        }
        Event event = new Event(type, data);
        for (Consumer<Event> l : listeners) {
            try {
                l.accept(event);
            } catch (RuntimeException | LinkageError e) {
                LOG.warning("A script listener for " + type + " failed: " + e);
            }
        }
        return event.cancelled;
    }

    /** Removes every listener (tests, script reload). */
    public static void clear() {
        LISTENERS.clear();
    }

    public static Set<String> listened() {
        return LISTENERS.keySet();
    }
}
