package org.jmouse.jdbc.intercept;

import org.jmouse.transaction.infrastructure.MutableTransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;

import static org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport.current;

public final class QueryTimeoutSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryTimeoutSupport.class);

    private QueryTimeoutSupport() { }

    public static void applyIfPresent(Statement statement) throws SQLException {
        if (statement != null && current().getContext() instanceof MutableTransactionContext mutable) {
            mutable.getEffectiveTimeoutSeconds().ifPresent(seconds -> {
                try {
                    statement.setQueryTimeout(seconds);
                } catch (Exception e) {
                    LOGGER.error("Failed to set query timeout to: {}s", seconds, e);
                }
            });
        }
    }
}