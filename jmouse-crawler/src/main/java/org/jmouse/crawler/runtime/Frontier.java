package org.jmouse.crawler.runtime;

/**
 * Frontier of pending {@link ProcessingTask}s. 🧭
 *
 * <p>The {@code Frontier} represents the primary queue of tasks that are
 * candidates for execution. Tasks enter the frontier when they are initially
 * discovered or when they become eligible again after being deferred
 * (for example, via {@link RetryBuffer}).</p>
 *
 * <p>The frontier does <em>not</em> enforce politeness, retry backoff,
 * or error semantics by itself. Those concerns are handled by the scheduler
 * and related components.</p>
 *
 * <p>Typical flow:</p>
 * <ol>
 *   <li>Tasks are {@link #offer(ProcessingTask) offered} to the frontier</li>
 *   <li>The scheduler {@link #poll() polls} tasks for evaluation</li>
 *   <li>Eligible tasks are handed off for execution</li>
 * </ol>
 */
public interface Frontier {

    /**
     * Offer a task to the frontier.
     *
     * <p>Implementations may apply ordering, prioritization, or de-duplication
     * strategies, but should not block the caller.</p>
     *
     * @param task the task to add to the frontier
     */
    void offer(ProcessingTask task);

    /**
     * Poll the next task from the frontier.
     *
     * <p>Returns {@code null} if the frontier is currently empty.</p>
     *
     * @return the next {@link ProcessingTask}, or {@code null} if none available
     */
    ProcessingTask poll();

    /**
     * Return the number of tasks currently held in the frontier.
     *
     * <p>Because implementations may be concurrent, the returned value
     * is typically an approximation and intended for monitoring
     * and scheduling heuristics.</p>
     */
    int size();
}
