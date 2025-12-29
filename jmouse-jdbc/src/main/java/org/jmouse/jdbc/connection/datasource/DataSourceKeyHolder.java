package org.jmouse.jdbc.connection.datasource;

public class DataSourceKeyHolder {

    private static final ThreadLocal<String> KEY = new ThreadLocal<>();

    private DataSourceKeyHolder() {}

    public static void use(String key) { KEY.set(key); }

    public static String current() { return KEY.get(); }

    public static void clear() { KEY.remove(); }

}
