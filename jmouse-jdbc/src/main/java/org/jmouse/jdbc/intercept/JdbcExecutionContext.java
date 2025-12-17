package org.jmouse.jdbc.intercept;

import org.jmouse.jdbc.core.JdbcExecutor;
import org.jmouse.jdbc.dialect.SqlDialect;

public record JdbcExecutionContext(JdbcExecutor delegate, SqlDialect dialect) {}