package org.jmouse.crawler.runtime.state.persistence;

import java.time.Duration;
import java.time.Instant;

public interface SnapshotPolicy {

    boolean shouldSnapshot(long opsSinceLastSnapshot, Instant now, Instant lastSnapshotAt);

    static SnapshotPolicy every(long operations) {
        if (operations < 1) {
            throw new IllegalArgumentException("operations must be >= 1");
        }
        return (operationsSince, now, lastAt) -> operationsSince >= operations;
    }

    static SnapshotPolicy every(Duration interval) {
        if (interval == null || interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("interval must be > 0");
        }
        return (opsSince, now, lastAt) -> lastAt == null || now.isAfter(lastAt.plus(interval));
    }

    default SnapshotPolicy or(SnapshotPolicy other) {
        return (operationsSince, now, lastAt) ->
                this.shouldSnapshot(operationsSince, now, lastAt) || other.shouldSnapshot(operationsSince, now, lastAt);
    }
}
