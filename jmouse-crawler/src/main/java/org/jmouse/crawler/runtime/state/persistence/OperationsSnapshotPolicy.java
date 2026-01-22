package org.jmouse.crawler.runtime.state.persistence;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

final class OperationsSnapshotPolicy implements SnapshotPolicy {

    private final long     everyOperations;
    private final Duration minInterval;

    private final AtomicLong operations = new AtomicLong();
    private volatile long    lastNanos  = System.nanoTime();

    OperationsSnapshotPolicy(long everyOperations, Duration minInterval) {
        this.everyOperations = Math.max(1, everyOperations);
        this.minInterval = (minInterval == null) ? Duration.ZERO : minInterval;
    }

    @Override
    public boolean shouldCheckpoint() {
        long now = System.nanoTime();

        if (operations.incrementAndGet() < everyOperations) {
            return false;
        }
        if (now - lastNanos < minInterval.toNanos()) {
            return false;
        }

        operations.set(0);
        lastNanos = now;
        return true;
    }
}