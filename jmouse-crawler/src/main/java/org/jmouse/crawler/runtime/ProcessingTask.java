package org.jmouse.crawler.runtime;

import java.net.URI;
import java.time.Instant;

/**
 * Immutable representation of a crawl unit scheduled for processing. 🧩
 *
 * <p>{@code ProcessingTask} captures both the <em>what</em> (target URL, depth,
 * discovery context) and the <em>when/how</em> (scheduling time, attempts,
 * priority, routing hints) of a single crawl operation.</p>
 *
 * <p>Instances are immutable and therefore thread-safe. Any state transition
 * (retry, reschedule, attempt increment) is expressed by creating a new instance.</p>
 *
 * <h3>Lifecycle overview</h3>
 * <ol>
 *   <li>Task is created when a URL is discovered</li>
 *   <li>Task is scheduled for execution at {@link #scheduledAt}</li>
 *   <li>On execution, {@link #attempt(Instant)} is called</li>
 *   <li>On deferral, {@link #schedule(Instant)} is called</li>
 * </ol>
 *
 * @param url           target URL to be processed
 * @param depth         crawl depth (distance from the seed)
 * @param parent        parent URL from which this task was discovered (may be {@code null})
 * @param discoveredBy  symbolic identifier of the discovery source (e.g. parser, extractor)
 * @param priority      task priority (higher values indicate higher priority)
 * @param scheduledAt   instant at which the task is scheduled to run
 * @param attempt       execution attempt counter (starting from 0)
 * @param hint          routing or execution hint used by downstream components
 */
public record ProcessingTask(
        URI url,
        int depth,
        URI parent,
        String discoveredBy,
        int priority,
        Instant scheduledAt,
        int attempt,
        RoutingHint hint
) {

    /**
     * Create a new task instance representing a new execution attempt.
     *
     * <p>This method updates the {@code scheduledAt} timestamp to {@code now}
     * and increments the {@code attempt} counter.</p>
     *
     * @param now instant at which the attempt occurs
     * @return new {@code ProcessingTask} instance with incremented attempt count
     */
    public ProcessingTask attempt(Instant now) {
        return new ProcessingTask(
                url,
                depth,
                parent,
                discoveredBy,
                priority,
                now,
                attempt + 1,
                hint
        );
    }

    /**
     * Create a new task instance scheduled for execution at the given instant.
     *
     * <p>The attempt counter is left unchanged.</p>
     *
     * @param scheduledAt new scheduled execution instant
     * @return new {@code ProcessingTask} instance with updated schedule
     */
    public ProcessingTask schedule(Instant scheduledAt) {
        return new ProcessingTask(
                url,
                depth,
                parent,
                discoveredBy,
                priority,
                scheduledAt,
                attempt,
                hint
        );
    }
}
