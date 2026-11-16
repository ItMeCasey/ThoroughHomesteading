package com.itmecasey.thoroughhomesteading.script;

import com.itmecasey.thoroughhomesteading.HomesteadingEvents;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * Gives ModJS server scripts the globals Homesteading and HomesteadingEvents. ModJS is optional, so everything goes through reflection: ModJS
 * offers ExtraBindings.register(BindingProvider), and the provider's install(type, cx, scope) adds globals to every script scope.
 * Without ModJS nothing is loaded and nothing fails.
 */
public final class ScriptBridge {
    private static final Logger LOG = Logger.getLogger("thoroughhomesteading");

    private ScriptBridge() {
    }

    /** Whether the globals belong in a scope of this ModJS script type (Hearths live on the server, so not for client scripts). */
    static boolean wanted(Object scriptType) {
        return !String.valueOf(scriptType).equalsIgnoreCase("CLIENT");
    }

    /** Called once from each loader's mod constructor; returns whether the binding was registered. */
    public static boolean install() {
        try {
            Class<?> provider = Class.forName("com.itmecasey.modjs.script.BindingProvider");
            Class<?> extra = Class.forName("com.itmecasey.modjs.script.ExtraBindings");
            Class<?> context = Class.forName("org.mozilla.javascript.Context");
            Class<?> scriptable = Class.forName("org.mozilla.javascript.Scriptable");
            Class<?> scriptableObject = Class.forName("org.mozilla.javascript.ScriptableObject");
            Method javaToJs = context.getMethod("javaToJS", Object.class, scriptable);
            Method put = scriptableObject.getMethod("putProperty", scriptable, String.class, Object.class);
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("install") && args != null && args.length == 3) {
                    if (wanted(args[0])) {
                        put.invoke(null, args[2], "Homesteading", javaToJs.invoke(null, Bridges.API, args[2]));
                        put.invoke(null, args[2], "HomesteadingEvents", javaToJs.invoke(null, HomesteadingEvents.INSTANCE, args[2]));
                    }
                    return null;
                }
                return switch (method.getName()) {
                    case "toString" -> "Homesteading binding";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> null;
                };
            };
            Object binding = Proxy.newProxyInstance(provider.getClassLoader(), new Class<?>[] {provider}, handler);
            extra.getMethod("register", provider).invoke(null, binding);
            LOG.info("ThoroughHomesteading: ModJS server scripts can use the Homesteading and HomesteadingEvents globals");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warning("ThoroughHomesteading: could not add the Homesteading globals to ModJS: " + e);
            return false;
        }
    }
}
