package org.jmouse.jdbc.intercept;

import org.jmouse.transaction.infrastructure.MutableTransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.sql.Statement;

import static org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport.current;

/**
 * Helper for applying transaction-scoped query timeouts to JDBC statements.
 * <p>
 * {@code QueryTimeoutSupport} bridges the transaction layer and JDBC execution by
 * propagating an <b>effective transaction timeout</b> into
 * {@link Statement#setQueryTimeout(int)}.
 *
 * <h3>How it works</h3>
 * <ul>
 *     <li>Reads the current transaction context via {@link org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport#current()}</li>
 *     <li>Checks whether the context is a {@link MutableTransactionContext}</li>
 *     <li>Extracts the effective timeout (if present)</li>
 *     <li>Applies it to the given JDBC {@link Statement}</li>
 * </ul>
 *
 * <h3>Typical integration point</h3>
 * <pre>{@code
 * StatementConfigurer cfg = stmt -> {
 *     QueryTimeoutSupport.applyIfPresent(stmt);
 * };
 * }</pre>
 *
 * <p>
 * ⚠️ Any exception thrown while setting the timeout is caught and logged.
 * Query execution proceeds even if timeout propagation fails.
 *
 * @author jMouse
 */
public final class QueryTimeoutSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(QueryTimeoutSupport.class);

    private QueryTimeoutSupport() { }

    /**
     * Applies the effective transaction timeout (if present) to the given JDBC statement.
     * <p>
     * This method is a best-effort operation:
     * <ul>
     *     <li>ignored if {@code statement} is {@code null}</li>
     *     <li>ignored if there is no active transaction context</li>
     *     <li>ignored if the context does not expose a timeout</li>
     * </ul>
     *
     * @param statement JDBC statement to configure
     * @throws SQLException never propagated; declared for signature compatibility
     */
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
