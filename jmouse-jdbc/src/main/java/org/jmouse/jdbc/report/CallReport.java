package org.jmouse.jdbc.report;

import java.time.Duration;

public record CallReport<T>(
        String sql,
        T result,
        Duration elapsed,
        Throwable error
) implements ExecutionReport {
}
