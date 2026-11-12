package org.jmouse.jdbc.intercept;

import static org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport.current;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import org.jmouse.transaction.infrastructure.MutableTransactionContext;
import org.jmouse.transaction.infrastructure.TransactionAttributes;
import org.jmouse.transaction.infrastructure.TransactionContext;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;

/** Applies the remaining transaction deadline, preserving any stricter statement timeout. */
public final class QueryTimeoutSupport {

    private QueryTimeoutSupport() {}

    public static void applyIfPresent(Statement statement) throws SQLException {
        applyIfPresent(statement, current().getContext());
    }

    public static void applyIfPresent(Statement statement, TransactionContext transactionContext)
        throws SQLException {
        if (statement == null || !(transactionContext instanceof MutableTransactionContext context)) {
            return;
        }
        TransactionAttributes attributes = context.getAttributes().orElse(null);
        if (attributes == null || !attributes.hasTimeout()) {
            return;
        }
        long remainingNanos = attributes.deadlineNanos() - System.nanoTime();
        if (remainingNanos <= 0) {
            context.setRollbackOnly();
            throw new SQLTimeoutException("Transaction deadline expired", "HYT00");
        }
        long seconds = 1 + (remainingNanos - 1) / TimeUnit.SECONDS.toNanos(1);
        int timeout = (int) Math.min(Integer.MAX_VALUE, seconds);
        int configured = statement.getQueryTimeout();
        if (configured == 0 || timeout < configured) {
            statement.setQueryTimeout(timeout);
        }
    }
}
