package org.jmouse.crawler.runtime;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * FIFO (first-in, first-out) implementation of {@link Frontier}. 📥📤
 *
 * <p>{@code FifoFrontier} maintains tasks in insertion order, making it the
 * simplest possible frontier strategy. It is suitable for:</p>
 * <ul>
 *   <li>basic crawlers</li>
 *   <li>testing and debugging</li>
 *   <li>scenarios where no prioritization or host-based ordering is required</li>
 * </ul>
 *
 * <p>This implementation is thread-safe and non-blocking, backed by
 * {@link ConcurrentLinkedQueue}.</p>
 *
 * <p>⚠️ Note: no deduplication, prioritization, or fairness guarantees
 * beyond FIFO ordering are provided.</p>
 */
public final class FifoFrontier implements Frontier {

    private final ConcurrentLinkedQueue<ProcessingTask> queue = new ConcurrentLinkedQueue<>();

    /**
     * Offer a task to the frontier.
     *
     * <p>Null tasks are ignored to keep the frontier free of invalid entries.
     * Callers are expected to validate tasks beforehand.</p>
     *
     * @param task the task to enqueue
     */
    @Override
    public void offer(ProcessingTask task) {
        if (task != null) {
            queue.offer(task);
        }
    }

    /**
     * Poll the next task from the frontier.
     *
     * <p>Returns {@code null} if the frontier is currently empty.</p>
     *
     * @return the next {@link ProcessingTask}, or {@code null} if none available
     */
    @Override
    public ProcessingTask poll() {
        return queue.poll();
    }

    /**
     * Return the current size of the frontier.
     *
     * <p>Because the underlying queue is concurrent, the returned value
     * is an approximation and intended for monitoring purposes only.</p>
     */
    @Override
    public int size() {
        return queue.size();
    }
}
