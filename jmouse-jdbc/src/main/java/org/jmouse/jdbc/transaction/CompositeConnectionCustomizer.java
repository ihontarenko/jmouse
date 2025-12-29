package org.jmouse.jdbc.transaction;

import org.jmouse.core.Contract;
import org.jmouse.transaction.TransactionDefinition;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public final class CompositeConnectionCustomizer implements ConnectionCustomizer {

    private final List<ConnectionCustomizer> delegates;

    public CompositeConnectionCustomizer(List<ConnectionCustomizer> delegates) {
        this.delegates = List.copyOf(Contract.nonNull(delegates, "delegates"));
    }

    @Override
    public RestoreAction apply(Connection connection, TransactionDefinition definition) throws SQLException {
        RestoreAction restore = RestoreAction.noop();
        for (ConnectionCustomizer customizer : delegates) {
            restore = restore.andThen(customizer.apply(connection, definition));
        }
        return restore;
    }
}
