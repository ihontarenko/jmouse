package org.jmouse.jdbc.statement;

import java.sql.SQLWarning;

public record StatementWarning(String message, String sqlState, SQLWarning warning) {}

