package org.jmouse.crawler.api;

import java.time.Instant;
import java.util.List;

/**
 * Buffer for temporarily deferred {@link ProcessingTask}s. ⏳
 *
 * <p>{@code RetryBuffer} stores tasks that cannot be executed immediately
 * (for example, due to politeness constraints, backoff, or transient errors)
 * and releases them back to the scheduler once they become eligible again.</p>
 *
 * <p>The buffer is time-aware: every task is associated with an {@code eligibleAt}
 * timestamp that defines the earliest moment it may be retried.</p>
 *
 * <p>Typical lifecycle:</p>
 * <ol>
 *   <li>Task is scheduled into the buffer with an eligibility timestamp</li>
 *   <li>The scheduler periodically checks for ready tasks</li>
 *   <li>Eligible tasks are drained and returned to the frontier</li>
 * </ol>
 */
public interface RetryBuffer {

    /**
     * Schedule a task for retry at or after the given eligibility instant.
     *
     * <p>The task will not be returned by {@link #drainReady(Instant, int)}
     * until {@code eligibleAt} has been reached.</p>
     *
     * @param task        the task to defer
     * @param eligibleAt  the earliest instant at which the task becomes runnable
     * @param reason      symbolic reason for deferral (e.g. "politeness", "retry")
     * @param error       optional error that caused the deferral; may be {@code null}
     */
    void schedule(ProcessingTask task, Instant eligibleAt, String reason, Throwable error);

    /**
     * Drain tasks whose eligibility instant has elapsed.
     *
     * <p>Returns at most {@code max} tasks whose {@code eligibleAt <= now}.
     * The returned tasks are removed from the buffer.</p>
     *
     * @param now  current time used to evaluate eligibility
     * @param max  maximum number of tasks to return
     * @return list of ready-to-retry tasks, possibly empty
     */
    List<ProcessingTask> drainReady(Instant now, int max);

    /**
     * Return the number of tasks currently stored in this buffer.
     *
     * <p>Used for observability and for determining whether the system
     * is fully drained.</p>
     */
    int size();

    /**
     * Peek the earliest eligibility instant among all buffered tasks.
     *
     * <p>This method does <em>not</em> remove any task from the buffer.</p>
     *
     * @return the earliest {@code eligibleAt} instant, or {@code null}
     *         if the buffer is empty
     */
    Instant peekEligibleAt();
}
