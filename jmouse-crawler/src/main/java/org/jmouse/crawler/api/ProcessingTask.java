package org.jmouse.crawler.api;

import org.jmouse.core.trace.TraceContext;

import java.net.URI;
import java.time.Instant;

/**
 * Immutable representation of a crawl task scheduled for processing. 🧩
 *
 * <p>{@code ProcessingTask} is the core unit of work in the crawler.
 * It combines identity, tracing, discovery context, scheduling metadata,
 * and routing hints into a single immutable value object.</p>
 *
 * <p>The task is immutable and therefore thread-safe.
 * Any state transition (attempt increment, deferral, rescheduling)
 * produces a new {@code ProcessingTask} instance.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>Identify a crawl operation via {@link TaskId}</li>
 *   <li>Carry distributed tracing context via {@link TraceContext}</li>
 *   <li>Describe discovery provenance via {@link TaskOrigin}</li>
 *   <li>Encode scheduling and retry metadata</li>
 *   <li>Provide stable input for routing and pipeline execution</li>
 * </ul>
 *
 * <h3>Typical lifecycle</h3>
 * <ol>
 *   <li>A task is created when a URL is discovered</li>
 *   <li>The task is scheduled for execution at {@link #scheduledAt}</li>
 *   <li>When execution starts, {@link #attempt(Instant)} is called</li>
 *   <li>If execution must be delayed, {@link #deferred(Instant)} is called</li>
 *   <li>The task may be retried, routed, or dead-lettered based on outcomes</li>
 * </ol>
 *
 * <p>Note: this type intentionally contains no mutable execution state.
 * All execution outcomes are recorded externally (e.g. in logs, decision logs,
 * retry buffers, or dead-letter queues).</p>
 *
 * @param id           stable unique identifier of the task
 * @param trace        trace context used for correlation across subsystems
 * @param url          target URL to be processed
 * @param depth        crawl depth (distance from the seed)
 * @param parent       parent URL from which this task was discovered (may be {@code null})
 * @param origin       origin metadata describing how and why the task was discovered
 * @param priority     task priority (higher values indicate higher priority)
 * @param scheduledAt  instant at which the task is scheduled to run
 * @param attempt      execution attempt counter (starting from 0)
 * @param hint         routing or execution hint used by downstream components
 */
public record ProcessingTask(
        TaskId id,
        TraceContext trace,
        URI url,
        int depth,
        URI parent,
        TaskOrigin origin,
        int priority,
        Instant scheduledAt,
        int attempt,
        RoutingHint hint
) {

    /**
     * Create a new task instance representing a new execution attempt.
     *
     * <p>This method is typically called immediately before a task
     * is handed off to a {@code ProcessingEngine} for execution.</p>
     *
     * <p>The returned instance has:</p>
     * <ul>
     *   <li>{@code scheduledAt} set to {@code now}</li>
     *   <li>{@code attempt} incremented by one</li>
     * </ul>
     *
     * @param now instant at which the attempt begins
     * @return new {@code ProcessingTask} instance with incremented attempt count
     */
    public ProcessingTask attempt(Instant now) {
        return new ProcessingTask(
                id,
                trace,
                url,
                depth,
                parent,
                origin,
                priority,
                now,
                attempt + 1,
                hint
        );
    }

    /**
     * Create a new task instance deferred for execution at the given instant.
     *
     * <p>This method is used when a task cannot be executed immediately
     * (for example, due to politeness constraints or retry backoff).</p>
     *
     * <p>The returned instance has:</p>
     * <ul>
     *   <li>{@code scheduledAt} updated to the provided instant</li>
     *   <li>{@code attempt} left unchanged</li>
     * </ul>
     *
     * @param scheduledAt new scheduled execution instant
     * @return new {@code ProcessingTask} instance with updated schedule
     */
    public ProcessingTask deferred(Instant scheduledAt) {
        return new ProcessingTask(
                id,
                trace,
                url,
                depth,
                parent,
                origin,
                priority,
                scheduledAt,
                attempt,
                hint
        );
    }
}
