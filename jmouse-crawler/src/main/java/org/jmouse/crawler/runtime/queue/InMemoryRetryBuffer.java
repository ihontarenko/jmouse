package org.jmouse.crawler.runtime.queue;

import org.jmouse.crawler.api.ProcessingTask;
import org.jmouse.crawler.api.RetryBuffer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * In-memory {@link RetryBuffer} backed by a time-ordered priority queue. ⏳
 *
 * <p>This implementation stores deferred {@link ProcessingTask}s along with
 * an eligibility timestamp (the earliest instant at which a task may be
 * retried). Tasks are ordered by that timestamp and drained in chronological order.</p>
 *
 * <p>Thread-safety: backed by {@link PriorityBlockingQueue}; operations are safe for
 * concurrent producers/consumers. Note that the queue provides no strict fairness
 * guarantees beyond priority ordering.</p>
 *
 * <p>⚠️ Operational note:
 * this buffer is memory-only; scheduled retries are lost on process restart.</p>
 */
public final class InMemoryRetryBuffer implements RetryBuffer {

    /**
     * Retry items ordered by eligibility instant (earliest first).
     *
     * <p>Naming note: internally we keep "eligibleAt", even if legacy code used "eligibleAt".</p>
     */
    private final PriorityBlockingQueue<Item> queue =
            new PriorityBlockingQueue<>(128, Comparator.comparing(Item::eligibleAt));

    /**
     * Schedule a task into the retry buffer until the provided eligibility instant.
     *
     * <p>Null task or time is ignored to keep the buffer consistent.</p>
     *
     * @param task       deferred task
     * @param eligibleAt earliest instant at which the task may be retried
     * @param reason     symbolic reason for deferral (e.g. "politeness", "retry")
     * @param error      optional error cause; may be {@code null}
     */
    @Override
    public void schedule(ProcessingTask task, Instant eligibleAt, String reason, Throwable error) {
        if (task != null && eligibleAt != null) {
            queue.offer(new Item(eligibleAt, task, reason, error));
        }
    }

    /**
     * Drain tasks whose eligibility time has elapsed.
     *
     * <p>This method repeatedly peeks the head item (earliest eligibleAt) and
     * drains it if {@code eligibleAt <= now}. Draining stops as soon as the head
     * is not yet eligible, ensuring O(k log n) behavior for k drained items.</p>
     */
    @Override
    public List<ProcessingTask> drainReady(Instant now, int max) {
        if (now == null || max <= 0) {
            return List.of();
        }

        List<ProcessingTask> ready = new ArrayList<>(Math.min(max, 64));

        while (ready.size() < max) {
            Item head = queue.peek();

            // Nothing queued, or earliest task is still not eligible.
            if (head == null || head.eligibleAt().isAfter(now)) {
                break;
            }

            // Remove the head and publish its task.
            queue.poll();
            ready.add(head.task());
        }

        return ready;
    }

    /**
     * Return the current number of scheduled retry items.
     *
     * <p>Because the underlying structure is concurrent, this value is an approximation.</p>
     */
    @Override
    public int size() {
        return queue.size();
    }

    /**
     * Peek the earliest eligibility instant among all scheduled retry items.
     *
     * @return earliest {@code eligibleAt}, or {@code null} if empty
     */
    @Override
    public Instant peekEligibleAt() {
        Item head = queue.peek();
        return (head == null) ? null : head.eligibleAt();
    }

    /**
     * Internal buffer item.
     *
     * <p>Kept as a record to ensure immutability and simplify ordering.</p>
     *
     * @param eligibleAt earliest instant at which the task becomes eligible
     * @param task       the deferred task
     * @param reason     symbolic reason for deferral
     * @param error      optional error cause
     */
    private record Item(Instant eligibleAt, ProcessingTask task, String reason, Throwable error) { }
}
