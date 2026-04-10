package org.jmouse.jdbc.statement;

/**
 * Provides a custom update {@link StatementHandler} for a SQL operation.
 *
 * <p>This contract is optional and may be implemented by update operations that
 * want to observe or customize statement handling for insert/update/delete
 * executions.</p>
 *
 * @author Ivan Hontarenko
 */
public interface UpdateStatementHandlerProvider {

    /**
     * Returns update statement handler for this operation.
     *
     * @return update statement handler, never {@code null}
     */
    default StatementHandler<Integer> updateStatementHandler() {
        return StatementHandler.noop();
    }

}