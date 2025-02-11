package org.jmouse.testing_ground.beans;

public class Globals {

    public static final  String              DEFAULT_STATE = "_default";
    private static final ThreadLocal<String> THREAD_LOCAL  = ThreadLocal.withInitial(() -> DEFAULT_STATE);

    public static void set(String value) {
        THREAD_LOCAL.set(value);
    }

    public static String get() {
        return THREAD_LOCAL.get();
    }

}
