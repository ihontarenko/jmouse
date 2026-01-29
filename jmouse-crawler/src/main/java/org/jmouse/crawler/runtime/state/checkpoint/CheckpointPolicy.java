package org.jmouse.crawler.runtime.state.checkpoint;

import java.time.Duration;

public final class CheckpointPolicy {

    private final long     everyOperations;
    private final Duration minimumInterval;

    private long operations;
    private long lastNanos = System.nanoTime();

    private CheckpointPolicy(long everyOperations, Duration minimumInterval) {
        this.everyOperations = Math.max(1, everyOperations);
        this.minimumInterval = (minimumInterval == null) ? Duration.ZERO : minimumInterval;
    }

    public static CheckpointPolicy every(long operations) {
        return new CheckpointPolicy(operations, Duration.ZERO);
    }

    public static CheckpointPolicy every(long operations, Duration minimumInterval) {
        return new CheckpointPolicy(operations, minimumInterval);
    }

    public synchronized boolean shouldCheckpoint() {
        operations++;

        if (operations < everyOperations) {
            return false;
        }

        long now = System.nanoTime();

        if (now - lastNanos < minimumInterval.toNanos()) {
            return false;
        }

        operations = 0;
        lastNanos = now;

        return true;
    }

}
