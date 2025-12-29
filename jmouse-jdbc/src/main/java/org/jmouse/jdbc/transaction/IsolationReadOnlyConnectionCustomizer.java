package org.jmouse.jdbc.transaction;

import org.jmouse.transaction.TransactionDefinition;
import org.jmouse.transaction.TransactionIsolation;

import java.sql.Connection;
import java.sql.SQLException;

public final class IsolationReadOnlyConnectionCustomizer implements ConnectionCustomizer {

    @Override
    public RestoreAction apply(Connection connection, TransactionDefinition definition) throws SQLException {
        ConnectionState before = ConnectionState.capture(connection);

        // readOnly
        if (definition.isReadOnly() != before.readOnly()) {
            connection.setReadOnly(definition.isReadOnly());
        }

        // isolation
        TransactionIsolation isolation = definition.getIsolation();
        if (!JdbcIsolation.isDefault(isolation)) {
            int desired = JdbcIsolation.toJdbc(isolation);
            if (!JdbcIsolation.isNone(desired) && desired != before.isolation()) {
                connection.setTransactionIsolation(desired);
            }
        }

        return () -> {
            if (connection.isReadOnly() != before.readOnly()) {
                connection.setReadOnly(before.readOnly());
            }

            if (connection.getTransactionIsolation() != before.isolation()) {
                connection.setTransactionIsolation(before.isolation());
            }

            if (connection.getAutoCommit() != before.autoCommit()) {
                connection.setAutoCommit(before.autoCommit());
            }
        };
    }
}
