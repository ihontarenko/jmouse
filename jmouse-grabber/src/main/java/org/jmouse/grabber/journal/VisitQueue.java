package org.jmouse.grabber.journal;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.jmouse.grabber.Visit;

/**
 * 🚦 The frontier — what is waiting to be visited.
 *
 * <p>An interface rather than a class because the order pages come off it is a real choice: breadth
 * first walks a site level by level and finds the shallow pages quickly; depth first drives straight
 * down one branch and finishes a category before starting the next. Both are one line to pick.</p>
 *
 * <p>⚠️ It holds no opinion about whether a visit <i>should</i> be made. Deduplication is the
 * journal's, caps are the policy's, and a queue that also decided those would be a third place to
 * look when a page is missing.</p>
 */
public interface VisitQueue {

    /**
     * 🌊 Breadth first — the default. Shallow pages before deep ones, which is what a listing-then-
     * detail crawl wants and what keeps a run's early output representative.
     */
    static VisitQueue breadthFirst() {
        return new DequeVisitQueue(false);
    }

    /**
     * 🪜 Depth first — finishes a branch before starting the next. Useful when each branch produces
     * output somebody consumes as it arrives.
     */
    static VisitQueue depthFirst() {
        return new DequeVisitQueue(true);
    }

    /**
     * 🎚️ Shallowest first, strictly — a heap on depth rather than an insertion-ordered queue.
     *
     * <p>Different from {@link #breadthFirst()} once retries and discovered links interleave: this
     * one re-sorts, so a depth-1 page discovered late still goes before the depth-4 pages already
     * waiting.</p>
     */
    static VisitQueue shallowestFirst() {
        return new PriorityVisitQueue();
    }

    void offer(Visit visit);

    Optional<Visit> poll();

    boolean isEmpty();

    int size();

    /**
     * 📊 How many visits this queue has handed out over its life — the run's "pages started" figure.
     */
    long dispensed();

    final class DequeVisitQueue implements VisitQueue {

        private final ConcurrentLinkedDeque<Visit> visits    = new ConcurrentLinkedDeque<>();
        private final AtomicLong                   dispensed = new AtomicLong();
        private final boolean                      fromTheEnd;

        private DequeVisitQueue(boolean fromTheEnd) {
            this.fromTheEnd = fromTheEnd;
        }

        @Override
        public void offer(Visit visit) {
            visits.addLast(visit);
        }

        @Override
        public Optional<Visit> poll() {
            Visit visit = fromTheEnd ? visits.pollLast() : visits.pollFirst();

            if (visit != null) {
                dispensed.incrementAndGet();
            }

            return Optional.ofNullable(visit);
        }

        @Override
        public boolean isEmpty() {
            return visits.isEmpty();
        }

        @Override
        public int size() {
            return visits.size();
        }

        @Override
        public long dispensed() {
            return dispensed.get();
        }
    }

    final class PriorityVisitQueue implements VisitQueue {

        private final PriorityBlockingQueue<Visit> visits = new PriorityBlockingQueue<>(
                64, Comparator.comparingInt(Visit::depth).thenComparingInt(Visit::attempt));

        private final AtomicLong dispensed = new AtomicLong();

        private PriorityVisitQueue() {
        }

        @Override
        public void offer(Visit visit) {
            visits.offer(visit);
        }

        @Override
        public Optional<Visit> poll() {
            Visit visit = visits.poll();

            if (visit != null) {
                dispensed.incrementAndGet();
            }

            return Optional.ofNullable(visit);
        }

        @Override
        public boolean isEmpty() {
            return visits.isEmpty();
        }

        @Override
        public int size() {
            return visits.size();
        }

        @Override
        public long dispensed() {
            return dispensed.get();
        }
    }

}
