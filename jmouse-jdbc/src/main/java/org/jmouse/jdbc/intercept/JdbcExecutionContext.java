package org.jmouse.jdbc.intercept;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.core.JdbcExecutor;
import org.jmouse.jdbc.database.DatabasePlatform;

public record JdbcExecutionContext(JdbcExecutor delegate) {

    public JdbcExecutionContext {
        Contract.nonNull(delegate, "delegate");
    }
}
