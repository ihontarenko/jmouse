package org.jmouse.jdbc.report;

import java.time.Duration;

public record QueryReport(
        String sql,
        Duration elapsed,
        Throwable error
) implements ExecutionReport {
}
