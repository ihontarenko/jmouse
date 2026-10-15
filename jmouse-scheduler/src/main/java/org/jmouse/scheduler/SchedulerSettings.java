package org.jmouse.scheduler;

import java.time.Duration;
import java.util.Objects;

/**
 * How the runner polls.
 *
 * @param pollInterval     the pause between two passes
 * @param maximumJitter    a random extra pause in {@code [0, maximumJitter]} added to each, so runners
 *                         started together do not poll in lockstep; at most {@code pollInterval}
 * @param misfireThreshold how late an occurrence may be found and still count as on time; must exceed
 *                         {@code pollInterval + maximumJitter}, or ordinary polling would misfire
 * @param batchSize        the most due triggers one pass takes from the store
 * @param leaseDuration    how long a claim holds a trigger's occurrences in flight before another runner
 *                         may take them over and dispatch them again; it must comfortably exceed the time
 *                         the gate and the dispatcher take, or healthy runners will duplicate each other
 */
public record SchedulerSettings(Duration pollInterval, Duration maximumJitter, Duration misfireThreshold, int batchSize,
                                Duration leaseDuration) {

    /**
     * Five seconds, up to one second of jitter, a one-minute threshold, a hundred triggers a pass, a
     * thirty-second lease.
     */
    public static final SchedulerSettings DEFAULTS = new SchedulerSettings(
            Duration.ofSeconds(5), Duration.ofSeconds(1), Duration.ofMinutes(1), 100, Duration.ofSeconds(30));

    public SchedulerSettings {
        Objects.requireNonNull(pollInterval, "pollInterval");
        Objects.requireNonNull(maximumJitter, "maximumJitter");
        Objects.requireNonNull(misfireThreshold, "misfireThreshold");
        Objects.requireNonNull(leaseDuration, "leaseDuration");

        if (pollInterval.compareTo(Duration.ofMillis(10)) < 0) {
            throw new IllegalArgumentException("The poll interval must be at least 10 ms, not " + pollInterval + ".");
        }

        if (maximumJitter.isNegative() || maximumJitter.compareTo(pollInterval) > 0) {
            throw new IllegalArgumentException("The jitter must be between zero and the poll interval, not "
                    + maximumJitter + ".");
        }

        if (misfireThreshold.compareTo(pollInterval.plus(maximumJitter)) <= 0) {
            throw new IllegalArgumentException("The misfire threshold (" + misfireThreshold
                    + ") must exceed the poll interval plus the jitter (" + pollInterval.plus(maximumJitter)
                    + "), or every ordinary pass would find occurrences late.");
        }

        if (batchSize < 1 || batchSize > 10_000) {
            throw new IllegalArgumentException("The batch size must be between 1 and 10000, not " + batchSize + ".");
        }

        if (leaseDuration.compareTo(Duration.ofMillis(10)) < 0) {
            throw new IllegalArgumentException("The lease must be at least 10 ms, not " + leaseDuration + ".");
        }
    }
}
