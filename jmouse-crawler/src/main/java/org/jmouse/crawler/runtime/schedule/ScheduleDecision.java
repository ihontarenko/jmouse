package org.jmouse.crawler.runtime.schedule;

import org.jmouse.crawler.api.ProcessingTask;

import java.time.Duration;
import java.time.Instant;

/**
 * Result of a scheduling step produced by a {@link JobScheduler}. 🧭
 *
 * <p>A {@code ScheduleDecision} expresses the scheduler’s current view of the system:
 * whether a task is ready for execution, execution should be temporarily paused
 * (parked), or the scheduler has no more work to offer.</p>
 *
 * <p>This sealed hierarchy makes scheduler decisions explicit and exhaustively
 * matchable, which is particularly important for deterministic crawler behavior
 * and clear control flow in runners.</p>
 */
public sealed interface ScheduleDecision permits
        ScheduleDecision.TaskReady,
        ScheduleDecision.Park,
        ScheduleDecision.Drained {

    /**
     * Indicates that a {@link ProcessingTask} is ready to be executed immediately.
     *
     * <p>Runners receiving this decision may submit the task to an executor
     * (subject to capacity limits).</p>
     *
     * @param task the task ready for execution
     */
    record TaskReady(ProcessingTask task) implements ScheduleDecision {}

    /**
     * Indicates that no task can be executed right now and the runner should
     * temporarily suspend scheduling activity. ⏸️
     *
     * <p>The {@code duration} represents the minimum amount of time the runner
     * should remain parked. The {@code wakeUpAt} timestamp is an absolute hint
     * that may be used for diagnostics, logging, or coordination with other
     * scheduling components.</p>
     *
     * <p>Runners are free to wake up earlier if external events occur
     * (for example, task completions), but should avoid busy-spinning.</p>
     *
     * @param duration minimum time to remain parked; may be {@code null} or zero
     *                 to indicate a very short or opportunistic park
     * @param wakeUpAt absolute time at which the scheduler expects to be polled again
     */
    record Park(Duration duration, Instant wakeUpAt) implements ScheduleDecision {}

    /**
     * Indicates that the scheduler has no remaining work to offer. 🏁
     *
     * <p>This does <em>not</em> necessarily mean that all tasks have completed execution.
     * Runners should typically terminate only after receiving {@code Drained}
     * <strong>and</strong> observing that no tasks are still in flight.</p>
     */
    enum Drained implements ScheduleDecision {
        /** Singleton drained decision. */
        INSTANCE
    }
}
