package org.jmouse.crawler.runtime;

import org.jmouse.core.Verify;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

/**
 * Base class for {@link CrawlRunner} implementations that are driven by a {@link JobScheduler}. 🧭
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Provide access to shared scheduling dependencies ({@link #scheduler}, {@link #clock})</li>
 *   <li>Provide a small, bounded "park" primitive used to respect scheduler backoff decisions</li>
 *   <li>Offer shared validation helpers for runner implementations</li>
 * </ul>
 *
 * <p>The parking strategy is intentionally conservative:</p>
 * <ul>
 *   <li>Very small durations use {@link LockSupport#parkNanos(long)} for precision</li>
 *   <li>Larger durations use short sleeps capped at 50ms to remain responsive</li>
 *   <li>Zero/negative durations yield to avoid busy spinning</li>
 * </ul>
 */
public abstract class AbstractSchedulerRunner implements CrawlRunner {

    /**
     * Scheduler that produces {@link ScheduleDecision}s for the runner loop.
     */
    protected final JobScheduler scheduler;

    /**
     * Clock used for consistent time measurements in scheduling and applying task results.
     */
    protected final Clock clock;

    /**
     * @param scheduler scheduler decision source
     * @param clock     time source (must be non-null)
     */
    protected AbstractSchedulerRunner(JobScheduler scheduler, Clock clock) {
        this.scheduler = Verify.nonNull(scheduler, "scheduler");
        this.clock = Verify.nonNull(clock, "clock");
    }

    /**
     * Park the current thread for the given duration while remaining responsive. ⏸️
     *
     * <p>Semantics:</p>
     * <ul>
     *   <li>If {@code duration} is {@code null}, zero, or negative -> {@link Thread#yield()}</li>
     *   <li>If duration &lt; 1ms -> {@link LockSupport#parkNanos(long)}</li>
     *   <li>Otherwise -> {@link Thread#sleep(long)} capped at 50ms</li>
     * </ul>
     *
     * <p>This method restores the interrupt flag if interrupted.</p>
     *
     * @param duration requested park duration; may be {@code null}
     */
    protected final void park(Duration duration) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            // Yield is a good "do not busy spin" default when the scheduler indicates no wait is required.
            Thread.yield();
            return;
        }

        long nanos = duration.toNanos();

        // Sub-millisecond waits: prefer a precise park rather than a sleep.
        if (nanos < 1_000_000L) {
            LockSupport.parkNanos(nanos);
            return;
        }

        // Longer waits: use bounded sleep to keep the loop responsive to external events.
        try {
            Thread.sleep(Math.min(duration.toMillis(), 50L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Ensure the given engine is suitable for a runner that expects parallel execution semantics.
     *
     * <p>Depending on your type hierarchy, you may want this to validate a more specific subtype
     * (e.g. {@code ParallelProcessingEngine}) rather than {@link ProcessingEngine} itself.</p>
     *
     * @param engine engine instance
     * @return validated engine
     */
    protected final ProcessingEngine requireEngine(ProcessingEngine engine) {
        return Verify.instanceOf(engine, ProcessingEngine.class, "engine");
    }
}
