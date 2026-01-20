package org.jmouse.crawler.runtime;

import java.time.Instant;

/**
 * Core execution engine for {@link ProcessingTask}s. ⚙️
 *
 * <p>{@code ProcessingEngine} defines the contract between the crawler runtime
 * (schedulers, runners) and the task execution logic.</p>
 *
 * <p>The engine operates in two distinct phases:</p>
 * <ol>
 *   <li><strong>Execute</strong> — perform the task’s work and compute a {@link TaskDisposition}</li>
 *   <li><strong>Apply</strong> — apply the resulting disposition to the crawler state</li>
 * </ol>
 *
 * <p>This split allows execution to be performed asynchronously or in parallel,
 * while state mutation remains centralized and controlled.</p>
 */
public interface ProcessingEngine {

    /**
     * Execute the given task and determine its disposition.
     *
     * <p>This method should perform the core processing logic
     * (fetching, parsing, routing, etc.) but must <em>not</em> mutate
     * shared crawler state directly.</p>
     *
     * <p>The returned {@link TaskDisposition} describes what should happen
     * to the task next (retry, enqueue children, drop, dead-letter, etc.).</p>
     *
     * @param task task to execute
     * @return disposition describing the outcome of execution
     */
    TaskDisposition execute(ProcessingTask task);

    /**
     * Apply the outcome of task execution to the crawler state.
     *
     * <p>This method is responsible for performing side effects such as:</p>
     * <ul>
     *   <li>enqueueing new tasks</li>
     *   <li>scheduling retries</li>
     *   <li>recording dead-letter entries</li>
     *   <li>updating metrics or logs</li>
     * </ul>
     *
     * <p>It is typically called by the runner after execution completes,
     * potentially on a different thread.</p>
     *
     * @param task        the task that was executed
     * @param disposition the outcome of execution
     * @param now         current time used for scheduling and bookkeeping
     */
    void apply(ProcessingTask task, TaskDisposition disposition, Instant now);
}
