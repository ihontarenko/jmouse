package org.jmouse.jdbc.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import org.jmouse.core.Verify;
import org.jmouse.transaction.TransactionDefinition;

public final class CompositeConnectionCustomizer implements ConnectionCustomizer {

    private final List<ConnectionCustomizer> delegates;

    public CompositeConnectionCustomizer(List<ConnectionCustomizer> delegates) {
        this.delegates = List.copyOf(Verify.nonNull(delegates, "delegates"));
    }

    @Override
    public RestoreAction apply(Connection connection, TransactionDefinition definition) throws SQLException {
        RestoreAction restore = RestoreAction.noop();
        try {
            for (ConnectionCustomizer customizer : delegates) {
                restore = java.util.Objects.requireNonNull(
                    customizer.apply(connection, definition),
                    "ConnectionCustomizer returned null"
                ).andThen(restore);
            }
            return restore;
        } catch (SQLException | RuntimeException | Error failure) {
            try {
                restore.restore();
            } catch (SQLException | RuntimeException | Error cleanupFailure) {
                if (failure != cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }
}
