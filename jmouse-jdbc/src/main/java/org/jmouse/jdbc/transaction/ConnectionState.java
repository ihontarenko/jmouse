package org.jmouse.jdbc.transaction;

import java.sql.Connection;
import java.sql.SQLException;

public record ConnectionState(boolean autoCommit, boolean readOnly, int isolation) {
    public static ConnectionState capture(Connection connection) throws SQLException {
        return new ConnectionState(
                connection.getAutoCommit(),
                connection.isReadOnly(),
                connection.getTransactionIsolation()
        );
    }

    public void restore(Connection connection) throws SQLException {
        // restore in a safe order (best-effort callers handle exceptions)
        connection.setTransactionIsolation(isolation);
        connection.setReadOnly(readOnly);
        connection.setAutoCommit(autoCommit);
    }

}
