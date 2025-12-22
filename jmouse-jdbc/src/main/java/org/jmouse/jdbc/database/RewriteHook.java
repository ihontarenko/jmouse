package org.jmouse.jdbc.database;

@FunctionalInterface
public interface RewriteHook {

    String rewrite(String sql);

    static RewriteHook noop() {
        return sql -> sql;
    }

}
