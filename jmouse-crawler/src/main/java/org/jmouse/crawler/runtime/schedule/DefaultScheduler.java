package org.jmouse.crawler.runtime.schedule;

import org.jmouse.crawler.api.*;
import org.jmouse.crawler.api.Frontier;
import org.jmouse.crawler.api.RetryBuffer;
import org.jmouse.crawler.api.PolitenessPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static java.lang.Math.max;
import static org.jmouse.core.Verify.nonNull;

/**
 * Default implementation of {@link JobScheduler}. 🧭
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>Move retry-ready tasks back into the {@link Frontier}</li>
 *   <li>Poll tasks from the frontier in bounded batches</li>
 *   <li>Enforce {@link PolitenessPolicy} by deferring tasks into {@link RetryBuffer}</li>
 *   <li>When no task is ready, emit either {@link ScheduleDecision.Park} (with a computed wake-up)
 *       or {@link ScheduleDecision.Drained} when the system is fully drained</li>
 * </ul>
 *
 * <p>Design notes:</p>
 * <ul>
 *   <li>Batch sizes are fixed to reduce contention and make scheduling cost predictable.</li>
 *   <li>Park duration is clamped to {@code [0, maxDuration]} to avoid both busy spinning and
 *       excessive sleep times.</li>
 * </ul>
 */
public final class DefaultScheduler implements JobScheduler {

    /**
     * Retry scheduling reason used when a task is deferred due to politeness.
     */
    private static final String REASON_POLITENESS = "politeness";

    /**
     * Small fallback park used when we have work in the system but no concrete wake-up time
     * can be computed (e.g. retry buffer has no not-before). ⚠️
     */
    private static final Duration FALLBACK_PARK = Duration.ofMillis(10);

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);

    private final Frontier         frontier;
    private final PolitenessPolicy politeness;
    private final RetryBuffer      retryBuffer;
    private final Clock            clock;

    private final int      retryDrainBatch;
    private final Duration maxDuration;

    /**
     * Minimum park duration returned by this scheduler.
     * Keeping it as {@link Duration#ZERO} avoids negative durations and simplifies runners.
     */
    private final Duration minDuration = Duration.ZERO;

    /**
     * Max number of frontier polls per scheduling step.
     */
    private final int scanFrontierBatch = 128;

    /**
     * @param frontier         source of tasks ready to be considered for execution
     * @param politeness       policy defining per-target backoff / "not before" instants
     * @param retryBuffer      buffer holding deferred tasks (politeness, retries, etc.)
     * @param clock            time source for deterministic scheduling decisions
     * @param retryDrainBatch  max number of ready retry items to move back to frontier per step (min 1)
     * @param maxDuration      upper bound for park durations (must be non-null)
     */
    public DefaultScheduler(
            Frontier frontier,
            PolitenessPolicy politeness,
            RetryBuffer retryBuffer,
            Clock clock,
            int retryDrainBatch,
            Duration maxDuration
    ) {
        this.frontier = nonNull(frontier, "frontier");
        this.retryBuffer = nonNull(retryBuffer, "retryBuffer");
        this.politeness = nonNull(politeness, "politeness");
        this.clock = nonNull(clock, "clock");
        this.retryDrainBatch = max(1, retryDrainBatch);
        this.maxDuration = nonNull(maxDuration, "maxParkDuration");
    }

    /**
     * Produce the next scheduler decision.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Move retry-ready tasks into the frontier</li>
     *   <li>Poll up to {@code scanFrontierBatch} tasks from frontier and check politeness</li>
     *   <li>If a task is allowed now -> {@link ScheduleDecision.TaskReady}</li>
     *   <li>If a task is not allowed -> defer into retry buffer and continue scanning</li>
     *   <li>If no task is ready -> return {@link ScheduleDecision.Park} or {@link ScheduleDecision.Drained}</li>
     * </ol>
     */
    @Override
    public ScheduleDecision nextDecision() {
        Instant now = clock.instant();

        moveReadyRetries(now);

        for (int i = 0; i < scanFrontierBatch; i++) {
            ProcessingTask task = frontier.poll();
            if (task == null) {
                break;
            }

            Instant eligibleAt = politeness.eligibleAt(task, now);

            // Politeness deferral: push back into retry buffer with a specific reason.
            if (eligibleAt != null && eligibleAt.isAfter(now)) {
                Duration delay = Duration.between(now, eligibleAt);

                Object key = (politeness instanceof KeyAwarePolitenessPolicy<?> keyAware)
                        ? keyAware.keyOf(task) : null;

                LOGGER.debug("scheduler.defer reason=politeness delay={} task={} key={}",
                             delay, task.url(), key);

                retryBuffer.schedule(task.deferred(eligibleAt), eligibleAt, REASON_POLITENESS, null);
                continue;
            }

            LOGGER.debug(
                    "scheduler.dispatch task={} lane={}",
                    task.url(),
                    task.hint() // або lane
            );

            return new ScheduleDecision.TaskReady(task);
        }

        if (frontier.size() > 0) {
            LOGGER.warn(
                    "scheduler.park.with-frontier size={} retry={}",
                    frontier.size(),
                    retryBuffer.size()
            );
        }

        return decideWhenNoTaskReady(now);
    }

    /**
     * Move tasks whose not-before has elapsed from retry buffer into frontier.
     *
     * <p>Bounded by {@code retryDrainBatch} to keep a single scheduling step predictable.</p>
     */
    private void moveReadyRetries(Instant now) {
        for (ProcessingTask ready : retryBuffer.drainReady(now, retryDrainBatch)) {
            frontier.offer(ready);
        }
    }

    /**
     * Decide between {@link ScheduleDecision.Drained} and {@link ScheduleDecision.Park}
     * after a scan step finds no runnable task.
     */
    private ScheduleDecision decideWhenNoTaskReady(Instant now) {
        // Fully drained only if both frontier and retry buffer are empty.
        if (frontier.size() == 0 && retryBuffer.size() == 0) {
            return ScheduleDecision.Drained.INSTANCE;
        }
        return computeParkDecision(now);
    }

    /**
     * Compute a park decision based on the earliest retry "not before" time.
     * The computed duration is clamped to {@code [0, maxDuration]}.
     */
    private ScheduleDecision computeParkDecision(Instant now) {
        Instant nextEligibleAt = retryBuffer.peekEligibleAt();

        if (nextEligibleAt == null) {
            // We have work somewhere (frontier/retryBuffer size > 0), but cannot compute a wake-up hint.
            return new ScheduleDecision.Park(FALLBACK_PARK, now.plus(FALLBACK_PARK));
        }

        Duration duration = Duration.between(now, nextEligibleAt);

        if (duration.isNegative()) {
            duration = minDuration;
        } else if (duration.compareTo(maxDuration) > 0) {
            duration = maxDuration;
        }

        // wakeUpAt remains the actual "nextEligibleAt" (useful for observability),
        // while duration is clamped for runners. 📌
        return new ScheduleDecision.Park(duration, nextEligibleAt);
    }
}
