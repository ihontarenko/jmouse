package org.jmouse.jdbc.intercept.guard;

public record SQLScan(
        boolean hasStatementSeparator,
        boolean isDDL,
        boolean isUD,
        boolean hasWhere
) { }
