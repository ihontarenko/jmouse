package org.jmouse.jdbc.tx;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * 🪢 Default thread-local binding.
 */
public final class ThreadLocalConnectionBinding implements ConnectionBinding {

    private static final ThreadLocal<Connection> THREAD_LOCAL = new ThreadLocal<>();

    public static void ensureTxMode(Connection c) throws SQLException {
        if (c.getAutoCommit()) c.setAutoCommit(false);
    }

    @Override
    public void bind(Connection connection) {
        THREAD_LOCAL.set(Objects.requireNonNull(connection));
    }

    @Override
    public void unbind() {
        THREAD_LOCAL.remove();
    }

    @Override
    public Connection currentConnection() {
        return THREAD_LOCAL.get();
    }

}