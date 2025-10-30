package org.jmouse.jdbc.tx;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/**
 * 🪢 Default thread-local binding.
 */
public final class ThreadLocalConnectionBinding implements ConnectionBinding {

    private static final ThreadLocal<Connection> THREAD_LOCAL = new ThreadLocal<>();

    public static void autoCommitDisable(Connection connection) throws SQLException {
        if (connection.getAutoCommit()) {
            connection.setAutoCommit(false);
        }
    }

    public static void autoCommitEnable(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            connection.setAutoCommit(true);
        }
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