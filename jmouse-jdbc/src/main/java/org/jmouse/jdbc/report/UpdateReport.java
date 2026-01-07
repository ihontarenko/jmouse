package org.jmouse.jdbc.report;

import java.time.Duration;

public record UpdateReport(
        String sql,
        int updateCount,
        Duration elapsed,
        Throwable error
) implements ExecutionReport {
}