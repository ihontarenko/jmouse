package org.jmouse.crawler.runtime.state.persistence;

import java.time.Duration;

@FunctionalInterface
public interface SnapshotPolicy {

    boolean shouldCheckpoint();

    static SnapshotPolicy or(SnapshotPolicy p0, SnapshotPolicy p1) {
        return () -> p0.shouldCheckpoint() || p1.shouldCheckpoint();
    }

    static SnapshotPolicy and(SnapshotPolicy p0, SnapshotPolicy p1) {
        return () -> p0.shouldCheckpoint() && p1.shouldCheckpoint();
    }

    static SnapshotPolicy every(long operations) {
        return new OperationsSnapshotPolicy(operations, Duration.ZERO);
    }

    static SnapshotPolicy every(Duration minInterval) {
        return new OperationsSnapshotPolicy(1, minInterval);
    }

    static SnapshotPolicy every(long operations, Duration minInterval) {
        return new OperationsSnapshotPolicy(operations, minInterval);
    }
}
