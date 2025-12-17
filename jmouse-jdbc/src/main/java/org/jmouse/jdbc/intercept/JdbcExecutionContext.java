package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.core.JdbcExecutor;

public record JdbcExecutionContext(JdbcExecutor delegate) {}