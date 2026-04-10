package org.jmouse.jdbc.parameters.named;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.statement.StatementBinder;

/**
 * Prepared SQL execution descriptor.
 *
 * <p>Encapsulates compiled SQL and corresponding {@link StatementBinder}.</p>
 *
 * @author Ivan Hontarenko
 */
public record NamedSqlPreparedExecution(String sql, StatementBinder binder) {

    public NamedSqlPreparedExecution {
        Verify.nonNull(sql, "sql");
        Verify.nonNull(binder, "binder");
    }

}