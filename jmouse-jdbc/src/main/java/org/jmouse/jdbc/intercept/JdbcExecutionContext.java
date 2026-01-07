package org.jmouse.jdbc.intercept;

import org.jmouse.core.Verify;
import org.jmouse.jdbc.JdbcExecutor;

public record JdbcExecutionContext(JdbcExecutor executor) {

    public JdbcExecutionContext {
        Verify.nonNull(executor, "executor");
    }
}
