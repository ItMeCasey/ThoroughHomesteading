package com.itmecasey.thoroughhomesteading;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * The optional Barely Questing side, all by reflection so the mod never needs it:
 * <ul>
 *   <li>the {@code settler} quest task type ({@code {"type": "settler", "count": 3, "profession": "minecraft:farmer"}}, profession optional),
 *       which reads the {@link Ledger} of arrivals for the player;</li>
 *   <li>{@link #sameTeam}, which makes the owner's Barely Questing team share the Hearth.</li>
 * </ul>
 */
public final class BarelyQuestingBridge {
    private static final Logger LOG = Logger.getLogger("thoroughhomesteading");
    private static final String API = "com.itmecasey.barelyquesting.core.script.Bridges";

    private BarelyQuestingBridge() {
    }

    /** True when Barely Questing's classes are visible. */
    public static boolean present() {
        try {
            Class.forName(API, false, BarelyQuestingBridge.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException | LinkageError e) {
            return false;
        }
    }

    /** Registers the {@code settler} task type if Barely Questing is installed; returns whether it did. */
    public static boolean install(Supplier<Ledger> ledger) {
        if (!present()) {
            return false;
        }
        try {
            ClassLoader loader = Class.forName(API).getClassLoader();
            Class<?> taskType = Class.forName("com.itmecasey.barelyquesting.core.task.TaskType", false, loader);
            Class<?> taskTypes = Class.forName("com.itmecasey.barelyquesting.core.task.TaskTypes", false, loader);
            Class<?> def = Class.forName("com.itmecasey.barelyquesting.core.model.Def", false, loader);
            Class<?> view = Class.forName("com.itmecasey.barelyquesting.core.progress.PlayerView", false, loader);
            Method num = def.getMethod("num", String.class, long.class);
            Method str = def.getMethod("str", String.class, String.class);
            Method uuid = view.getMethod("uuid");
            InvocationHandler handler = (proxy, method, args) -> {
                switch (method.getName()) {
                    case "id":
                        return "settler";
                    case "target":
                        return Math.max(1L, (long) num.invoke(args[0], "count", 1L));
                    case "polled":
                        return true;
                    case "poll": {
                        String profession = (String) str.invoke(args[0], "profession", "");
                        UUID player = (UUID) uuid.invoke(args[1]);
                        long now = ledger.get().count(player, profession);
                        return Math.max((long) args[2], now);
                    }
                    case "describe": {
                        String profession = (String) str.invoke(args[0], "profession", "");
                        long count = Math.max(1L, (long) num.invoke(args[0], "count", 1L));
                        return "Settlers " + count + (profession.isEmpty() ? "" : " " + profession);
                    }
                    case "toString":
                        return "settler task type";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        return InvocationHandler.invokeDefault(proxy, method, args);
                }
            };
            Object type = Proxy.newProxyInstance(loader, new Class<?>[] {taskType}, handler);
            taskTypes.getMethod("register", taskType).invoke(null, type);
            LOG.info("ThoroughHomesteading: Barely Questing quests can use the settler task type");
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warning("ThoroughHomesteading: could not add the settler task type to Barely Questing: " + e);
            return false;
        }
    }

    /** True when both players are in the same Barely Questing team (always false without Barely Questing or before its server is up). */
    public static boolean sameTeam(UUID a, UUID b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        if (!present()) {
            return false;
        }
        try {
            Class<?> api = Class.forName(API);
            Object instance = api.getField("API").get(null);
            Method ready = instance.getClass().getMethod("isReady");
            if (!(boolean) ready.invoke(instance)) {
                return false;
            }
            Method teamName = instance.getClass().getMethod("teamName", String.class);
            String ta = (String) teamName.invoke(instance, a.toString());
            String tb = (String) teamName.invoke(instance, b.toString());
            return ta != null && !ta.isEmpty() && ta.equals(tb);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }
}
