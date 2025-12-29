package org.jmouse.jdbc.transaction;

import org.jmouse.transaction.TransactionDefinition;

import java.sql.Connection;
import java.sql.SQLException;

@FunctionalInterface
public interface ConnectionCustomizer {

    RestoreAction apply(Connection connection, TransactionDefinition definition) throws SQLException;

    interface RestoreAction {
        void restore() throws SQLException;

        static RestoreAction noop() {
            return () -> {};
        }

        default RestoreAction andThen(RestoreAction next) {
            return () -> {
                try {
                    this.restore();
                } finally {
                    next.restore();
                }
            };
        }
    }
}
