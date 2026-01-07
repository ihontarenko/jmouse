package org.jmouse.jdbc.report;

import java.time.Duration;

/**
 * Mutable context used by statement handlers to record execution metadata.
 * Built into an immutable {@link ExecutionReport} at the end of a call.
 */
public final class ExecutionReportContext {

    private Duration  elapsed;
    private Throwable error;
    private Integer   updateCount;
    private int[]     batchCounts;

    public void elapsed(Duration elapsed) {
        this.elapsed = elapsed;
    }

    public void error(Throwable error) {
        this.error = error;
    }

    public void updateCount(int updateCount) {
        this.updateCount = updateCount;
    }

    public void batchCounts(int[] counts) {
        this.batchCounts = (counts == null) ? null : counts.clone();
    }

    Duration elapsed() {
        return elapsed;
    }

    Throwable error() {
        return error;
    }

    Integer updateCount() {
        return updateCount;
    }

    int[] batchCounts() {
        return batchCounts == null ? null : batchCounts.clone();
    }

    public void clear() {
        elapsed = null;
        error = null;
        updateCount = null;
        batchCounts = null;
    }
}
