package org.jmouse.jdbc.statement;

import java.sql.ResultSet;

/**
 * Provides a custom query {@link StatementHandler} for a SQL operation.
 *
 * <p>This contract is optional and may be implemented by query operations that
 * want to observe or customize statement handling for select-like executions.</p>
 *
 * @author Ivan Hontarenko
 */
public interface QueryStatementHandlerProvider {

    /**
     * Returns query statement handler for this operation.
     *
     * @return query statement handler, never {@code null}
     */
    default StatementHandler<ResultSet> queryStatementHandler() {
        return StatementHandler.noop();
    }

}