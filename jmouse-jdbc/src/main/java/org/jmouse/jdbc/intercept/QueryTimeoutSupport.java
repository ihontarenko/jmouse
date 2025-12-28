package org.jmouse.jdbc.intercept;

import org.jmouse.transaction.infrastructure.TransactionContext;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;

import java.sql.SQLException;
import java.sql.Statement;

public final class QueryTimeoutSupport {

    private QueryTimeoutSupport() { }

    public static void applyIfPresent(Statement statement) throws SQLException {
        if (statement == null) return;

        TransactionContext context = TransactionContextAccessSupport.current().getContext();

        if (context == null || context.getDefinition() == null) {
            return;
        }

        int timeout = context.getDefinition().getTimeoutSeconds();

        if (timeout > 0) {
            statement.setQueryTimeout(timeout);
        }
    }
}