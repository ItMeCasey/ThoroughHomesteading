package com.itmecasey.thoroughhomesteading;

import java.lang.reflect.Method;

/**
 * Development switches. The self test lives in src/dev folders that only build with {@code -PdevTools}, so release jars never
 * contain it; this class reaches it by name when it is there.
 */
public final class Dev {
    /** True in dev runs (the Gradle run tasks set the property when -PdevTools is given). */
    public static final boolean ENABLED = System.getProperty("thoroughhomesteading.dev") != null;

    private Dev() {
    }

    /** Calls a static method of a dev class by name if the class exists in this build; does nothing in a release jar. */
    public static void call(String className, String method, Object... args) {
        if (!ENABLED) {
            return;
        }
        try {
            Class<?> type = Class.forName(className);
            for (Method m : type.getMethods()) {
                if (m.getName().equals(method) && m.getParameterCount() == args.length) {
                    m.invoke(null, args);
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            // A build without -PdevTools: nothing to do.
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Dev hook " + className + "." + method + " failed", e);
        }
    }
}
