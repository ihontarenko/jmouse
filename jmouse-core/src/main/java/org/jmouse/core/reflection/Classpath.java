package org.jmouse.core.reflection;

public final class Classpath {

    private Classpath() {
    }

    public static boolean present(String className) {
        try {
            Class.forName(className, false, Classpath.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}