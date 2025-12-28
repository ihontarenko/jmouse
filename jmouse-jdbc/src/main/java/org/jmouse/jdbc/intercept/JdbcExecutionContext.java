package org.jmouse.jdbc.intercept;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.JdbcExecutor;

public record JdbcExecutionContext(JdbcExecutor delegate) {

    public JdbcExecutionContext {
        Contract.nonNull(delegate, "delegate");
    }
}
