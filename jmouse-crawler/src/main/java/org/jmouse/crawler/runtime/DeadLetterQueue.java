package org.jmouse.crawler.runtime;

import java.util.List;

/**
 * Dead-letter queue for {@link ProcessingTask}s that can no longer be processed. ☠️
 *
 * <p>A {@code DeadLetterQueue} is the terminal sink in the crawler lifecycle.
 * Tasks are placed here when they permanently fail and should not be retried
 * (for example, due to repeated errors, invalid input, or unrecoverable conditions).</p>
 *
 * <p>This abstraction allows different implementations:
 * in-memory, persistent (DB), log-backed, or external systems.</p>
 *
 * <p>Typical usage:</p>
 * <ol>
 *   <li>A task fails irrecoverably</li>
 *   <li>A {@link DeadLetterItem} is created with diagnostic information</li>
 *   <li>The task and item are stored via {@link #put}</li>
 *   <li>An operator or background job polls entries for inspection or export</li>
 * </ol>
 */
public interface DeadLetterQueue {

    /**
     * Put a task into the dead-letter queue.
     *
     * <p>Implementations should treat this operation as terminal for the task:
     * once added, the task is not expected to re-enter the scheduling pipeline.</p>
     *
     * @param task the task that failed permanently
     * @param item metadata describing the failure reason and context
     */
    void put(ProcessingTask task, DeadLetterItem item);

    /**
     * Poll up to {@code max} dead-letter entries from the queue.
     *
     * <p>The returned entries are removed from the queue.
     * If fewer than {@code max} entries are available, all available
     * entries are returned.</p>
     *
     * @param max maximum number of entries to poll
     * @return list of polled {@link DeadLetterEntry} instances, possibly empty
     */
    List<DeadLetterEntry> pollBatch(int max);

    /**
     * Return the number of entries currently stored in the queue.
     *
     * <p>Primarily intended for monitoring and diagnostics.</p>
     */
    int size();
}
