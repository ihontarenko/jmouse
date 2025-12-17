package org.jmouse.jdbc;

final public class JdbcSupport {

    public static void closeQuietly(AutoCloseable... closeable) {
        if (closeable != null) {
            for (AutoCloseable autoCloseable : closeable) {
                try {
                    autoCloseable.close();
                } catch (Exception ignored) { }
            }
        }
    }

}
