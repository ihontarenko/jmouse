package org.jmouse.jdbc.transaction;

import java.sql.Connection;
import java.sql.SQLException;

/** Original JDBC state; restoration attempts each property and preserves all failures. */
public record ConnectionState(boolean autoCommit, boolean readOnly, int isolation) {
    public static ConnectionState capture(Connection connection) throws SQLException {
        return new ConnectionState(
            connection.getAutoCommit(),
            connection.isReadOnly(),
            connection.getTransactionIsolation()
        );
    }

    public void restore(Connection connection) throws SQLException {
        ConnectionCustomizer.RestoreAction restoreIsolation = () -> {
            if (connection.getTransactionIsolation() != isolation) {
                connection.setTransactionIsolation(isolation);
            }
        };
        ConnectionCustomizer.RestoreAction restoreReadOnly = () -> {
            if (connection.isReadOnly() != readOnly) {
                connection.setReadOnly(readOnly);
            }
        };
        ConnectionCustomizer.RestoreAction restoreAutoCommit = () -> {
            if (connection.getAutoCommit() != autoCommit) {
                connection.setAutoCommit(autoCommit);
            }
        };
        restoreIsolation.andThen(restoreReadOnly).andThen(restoreAutoCommit).restore();
    }
}
