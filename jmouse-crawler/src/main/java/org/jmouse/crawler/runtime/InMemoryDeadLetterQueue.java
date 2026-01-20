package org.jmouse.crawler.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory implementation of {@link DeadLetterQueue}. 🧺
 *
 * <p>This implementation is thread-safe and non-blocking, backed by
 * a {@link ConcurrentLinkedQueue}. It is suitable for:</p>
 * <ul>
 *   <li>development and testing</li>
 *   <li>small to medium crawls</li>
 *   <li>scenarios where dead-letter data does not need to survive restarts</li>
 * </ul>
 *
 * <p>⚠️ Note: entries are stored only in memory and will be lost
 * if the process terminates.</p>
 */
public final class InMemoryDeadLetterQueue implements DeadLetterQueue {

    private final ConcurrentLinkedQueue<DeadLetterEntry> queue = new ConcurrentLinkedQueue<>();

    /**
     * Put a task into the dead-letter queue.
     *
     * <p>Null values are ignored to keep the queue free of incomplete entries.
     * Callers are expected to validate inputs beforehand.</p>
     */
    @Override
    public void put(ProcessingTask task, DeadLetterItem item) {
        if (task != null && item != null) {
            queue.offer(new DeadLetterEntry(task, item));
        }
    }

    /**
     * Poll a batch of dead-letter entries.
     *
     * <p>This method performs repeated non-blocking polls until either
     * {@code max} entries have been collected or the queue becomes empty.</p>
     *
     * <p>The initial capacity of the returned list is capped to avoid
     * over-allocation for large {@code max} values.</p>
     */
    @Override
    public List<DeadLetterEntry> pollBatch(int max) {
        if (max <= 0) {
            return List.of();
        }

        List<DeadLetterEntry> batch = new ArrayList<>(Math.min(max, 64));

        while (batch.size() < max) {
            DeadLetterEntry entry = queue.poll();
            if (entry == null) {
                break;
            }
            batch.add(entry);
        }

        return batch;
    }

    /**
     * Return the current queue size.
     *
     * <p>Because the underlying queue is concurrent, the returned value
     * is an approximation and should be used for monitoring purposes only.</p>
     */
    @Override
    public int size() {
        return queue.size();
    }
}
