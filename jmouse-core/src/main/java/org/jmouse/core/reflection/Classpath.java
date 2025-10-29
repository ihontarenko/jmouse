package org.jmouse.core.reflection;

public final class Classpath {

    private Classpath() {
    }

    public static boolean present(String fqcn) {
        try {
            Class.forName(fqcn, false, Classpath.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}