package org.jmouse.jdbc.intercept;

import org.jmouse.core.Contract;
import org.jmouse.jdbc.core.JdbcExecutor;
import org.jmouse.jdbc.database.DatabasePlatform;

public record JdbcExecutionContext(JdbcExecutor delegate, DatabasePlatform platform) {

    public JdbcExecutionContext {
        Contract.nonNull(delegate, "delegate");
        Contract.nonNull(platform, "platform");
    }
}
