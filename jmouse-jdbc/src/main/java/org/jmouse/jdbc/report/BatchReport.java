package org.jmouse.jdbc.report;

import java.time.Duration;

public record BatchReport(
        String sql,
        int[] batchCounts,
        Duration elapsed,
        Throwable error
) implements ExecutionReport {

    public BatchReport {
        batchCounts = (batchCounts == null) ? null : batchCounts.clone();
    }

    @Override
    public int[] batchCounts() {
        return batchCounts == null ? null : batchCounts.clone();
    }

}
