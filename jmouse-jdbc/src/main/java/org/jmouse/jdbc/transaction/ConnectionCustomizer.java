package org.jmouse.jdbc.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import org.jmouse.transaction.TransactionDefinition;

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
                } catch (SQLException | RuntimeException | Error failure) {
                    try {
                        next.restore();
                    } catch (SQLException | RuntimeException | Error cleanupFailure) {
                        if (failure != cleanupFailure) {
                            failure.addSuppressed(cleanupFailure);
                        }
                    }
                    throw failure;
                }
                next.restore();
            };
        }
    }
}
